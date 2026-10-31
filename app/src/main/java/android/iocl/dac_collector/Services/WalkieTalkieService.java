package android.iocl.dac_collector.Services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.iocl.dac_collector.Firebase.WalkieTalkieFirebase;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.WalkieTalkieActivity;
import android.iocl.dac_collector.Utility.NotificationHelper;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;

public final class WalkieTalkieService extends Service
        implements WebRtcWalkieTalkieClient.Listener {

    public static final String ACTION_START_CALL = "android.iocl.dac_collector.walkie.START";
    public static final String ACTION_TALK = "android.iocl.dac_collector.walkie.TALK";
    public static final String ACTION_MUTE = "android.iocl.dac_collector.walkie.MUTE";
    public static final String ACTION_END_CALL = "android.iocl.dac_collector.walkie.END";
    public static final String EXTRA_CONSUMER_ID = "consumer_id";
    public static final String EXTRA_DASHBOARD_REQUEST = "dashboard_request";
    public static final String EXTRA_ENABLE_VIDEO = "enable_video";

    private static final String CHANNEL_ID = NotificationHelper.BACKGROUND_SERVICE_CHANNEL_ID;
    private static final int NOTIFICATION_ID = 1301;
    private static final String TAG = "WalkieTalkieService";
    private static final String RECOVERY_PREFERENCES = "walkie_call_recovery";
    private static final long RECOVERY_MAX_AGE_MS = 5 * 60 * 1000L;
    private static final int MAX_AUTOMATIC_RECOVERIES = 1;

    private final IBinder binder = new LocalBinder();
    private WebRtcWalkieTalkieClient callClient;
    private WebRtcWalkieTalkieClient.Listener listener;
    private String callStatus = "Ready to call FCM_sender.";
    private String roomCode;
    private boolean connected;
    private boolean transmitting;
    private boolean muteActionSent;
    private boolean videoEnabled;
    private AudioManager audioManager;
    private PowerManager.WakeLock callWakeLock;

    private final AudioManager.AudioRecordingCallback recordingCallback =
            new AudioManager.AudioRecordingCallback() {
                @Override
                public void onRecordingConfigChanged(
                        List<AudioRecordingConfiguration> configurations
                ) {
                    if (callClient == null) {
                        return;
                    }
                    int configurationCount = configurations == null ? 0 : configurations.size();
                    boolean silenced = hasSilencedRecording(configurations);
                    Log.i(TAG, "Audio recording configuration changed during call: count="
                            + configurationCount + ", anyClientSilenced=" + silenced);
                    if (configurations != null) {
                        for (AudioRecordingConfiguration configuration : configurations) {
                            Log.i(TAG, "Recording configuration: clientAudioSource="
                                    + configuration.getClientAudioSource()
                                    + ", clientSilenced=" + configuration.isClientSilenced());
                        }
                    }
                    if (silenced) {
                        Log.w(TAG, "Android reports a silenced recording configuration; "
                                + "keeping the WebRTC call active");
                    }
                }
            };

    public final class LocalBinder extends Binder {
        public WalkieTalkieService getService() {
            return WalkieTalkieService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.initializeBlockNotificationChannel(this);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            audioManager.registerAudioRecordingCallback(
                    recordingCallback,
                    new Handler(Looper.getMainLooper())
            );
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            if (!recoverInterruptedCall()) {
                stopSelf(startId);
                return START_NOT_STICKY;
            }
        } else {
            String action = intent.getAction();
            if (ACTION_START_CALL.equals(action)) {
                String consumerId = intent.getStringExtra(EXTRA_CONSUMER_ID);
                boolean dashboardRequested =
                        intent.getBooleanExtra(EXTRA_DASHBOARD_REQUEST, false);
                boolean videoRequested = intent.getBooleanExtra(EXTRA_ENABLE_VIDEO, false);
                startCall(consumerId, dashboardRequested, videoRequested, false);
            } else if (ACTION_TALK.equals(action)) {
                setTransmitting(true);
            } else if (ACTION_MUTE.equals(action)) {
                setTransmitting(false);
            } else if (ACTION_END_CALL.equals(action)) {
                endCall("Room ended");
            }
        }
        return callClient == null ? START_NOT_STICKY : START_STICKY;
    }

    private void startCall(
            String consumerId,
            boolean dashboardRequested,
            boolean videoRequested,
            boolean recovering
    ) {
        if (callClient != null) {
            return;
        }
        boolean videoEnabled = videoRequested
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        this.videoEnabled = videoEnabled;
        try {
            startCallForeground(videoEnabled);
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not promote walkie-talkie service to foreground", error);
            callStatus = "Could not start the background call. Check microphone and camera permissions.";
            notifyListenerError(callStatus);
            if (dashboardRequested) {
                showDashboardCallRequestFallback();
            }
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return;
        }
        if (consumerId == null || consumerId.trim().isEmpty()
                || consumerId.contains("not_found")) {
            callStatus = "Set up the consumer profile before calling.";
            notifyListenerError(callStatus);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            callStatus = "Microphone permission is required to use push-to-talk.";
            notifyListenerError(callStatus);
            stopForeground(STOP_FOREGROUND_REMOVE);
            if (dashboardRequested) {
                showDashboardCallRequestFallback();
            }
            stopSelf();
            return;
        }
        if (videoRequested && !videoEnabled) {
            Log.w(TAG, "Camera permission is unavailable; continuing with an audio-only call");
        }
        List<AudioRecordingConfiguration> activeRecordings = Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.N
                ? audioManager.getActiveRecordingConfigurations()
                : null;
        Log.i(TAG, "Checking active recordings before WebRTC setup: count="
                + (activeRecordings == null ? 0 : activeRecordings.size()));
        if (activeRecordings != null && !activeRecordings.isEmpty()) {
            for (AudioRecordingConfiguration configuration : activeRecordings) {
                Log.w(TAG, "Active recording detected before WebRTC setup: clientAudioSource="
                        + configuration.getClientAudioSource()
                        + ", clientSilenced=" + configuration.isClientSilenced());
            }
            Log.w(TAG, "Continuing the walkie-talkie call despite another active recording; "
                    + "Android may limit simultaneous microphone capture.");
        }

        try {
            if (recovering) {
                callStatus = "Recovering interrupted call in audio-only mode...";
            } else if (videoEnabled) {
                callStatus = "Setting up WebRTC audio and video...";
            } else {
                callStatus = "Camera permission is off. Setting up an audio-only call.";
            }
            updateNotification();
            saveCallForRecovery(consumerId.trim(), recovering);
            acquireCallWakeLock();
            String participantId = consumerId.trim();
            callClient = new WebRtcWalkieTalkieClient(
                    this,
                    participantId,
                    participantId,
                    videoEnabled,
                    this
            );
            callClient.createRoom();
            notifyStatus(callStatus);
        } catch (SecurityException error) {
            Log.e(TAG, "Could not start microphone or camera foreground service", error);
            endCall("Could not start the background call. Check microphone and camera permissions.");
            if (dashboardRequested) {
                showDashboardCallRequestFallback();
            }
            notifyListenerError(callStatus);
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not initialize walkie-talkie call", error);
            recordCallFailure("call_initialization", error);
            endCall("Could not start the walkie-talkie call.");
            if (dashboardRequested) {
                showDashboardCallRequestFallback();
            }
            notifyListenerError(callStatus);
        }
    }

    private void showDashboardCallRequestFallback() {
        Intent openCallScreen = new Intent(this, WalkieTalkieActivity.class)
                .putExtra(WalkieTalkieActivity.EXTRA_DASHBOARD_CALL_REQUEST, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        NotificationHelper.showDashboardCallRequest(
                this,
                "Dashboard call request",
                "Tap to open the app and enable the dashboard call.",
                openCallScreen
        );
    }

    private void startCallForeground(boolean videoEnabled) {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            if (videoEnabled) {
                serviceType |= ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
            }
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    serviceType
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    public void setListener(@Nullable WebRtcWalkieTalkieClient.Listener listener) {
        this.listener = listener;
        if (listener == null) {
            return;
        }
        listener.onStatus(callStatus);
        if (roomCode != null) {
            listener.onRoomCreated(roomCode);
        }
        if (connected) {
            listener.onConnected();
        }
    }

    public boolean isCallActive() {
        return callClient != null;
    }

    public boolean isVideoEnabled() {
        return videoEnabled;
    }

    public void setTransmitting(boolean enabled) {
        transmitting = enabled && callClient != null;
        if (callClient != null) {
            callClient.setTransmitting(transmitting);
            if (connected) {
                callClient.publishTransmitState(transmitting);
            }
            updateNotification();
        }
    }

    private void applyRemoteTransmitState(boolean enabled) {
        transmitting = enabled && callClient != null;
        if (callClient != null) {
            callClient.setTransmitting(transmitting);
            updateNotification();
        }
    }

    public void endCall(String message) {
        Log.i(TAG, "endCall requested: " + message);
        finishCall(message, true);
    }

    private void finishCall(String message, boolean notifyEnded) {
        WebRtcWalkieTalkieClient client = callClient;
        Log.i(TAG, "Finishing walkie-talkie call: reason=" + message
                + ", notifyEnded=" + notifyEnded
                + ", wasConnected=" + connected
                + ", wasTransmitting=" + transmitting
                + ", hadClient=" + (client != null));
        callClient = null;
        transmitting = false;
        connected = false;
        roomCode = null;
        callStatus = message;
        clearCallRecoveryState();
        releaseCallWakeLock();
        if (client != null) {
            closeCallClient(client);
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        if (notifyEnded && listener != null) {
            listener.onRoomEnded(message);
        }
        stopSelf();
    }

    private void notifyListenerError(String message) {
        if (listener != null) {
            listener.onError(message);
        }
    }

    private void notifyStatus(String status) {
        if (listener != null) {
            listener.onStatus(status);
        }
    }

    private boolean hasSilencedRecording(
            List<AudioRecordingConfiguration> configurations
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || configurations == null) {
            return false;
        }
        for (AudioRecordingConfiguration configuration : configurations) {
            if (configuration.isClientSilenced()) {
                return true;
            }
        }
        return false;
    }

    private void updateNotification() {
        if (callClient != null) {
            getSystemService(android.app.NotificationManager.class)
                    .notify(NOTIFICATION_ID, buildNotification());
            if (!muteActionSent) {
                muteActionSent = true;
                try {
                    serviceAction(ACTION_MUTE, 3).send();
                } catch (PendingIntent.CanceledException error) {
                    Log.w(TAG, "Could not send initial mute action", error);
                }
            }
        }
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, WalkieTalkieActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this,
                1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cylinder_tile)
                .setContentTitle("Walkie-talkie call active")
                .setContentText(notificationMessage())
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openPendingIntent);
        if (connected) {
            builder.addAction(0, "Talk", serviceAction(ACTION_TALK, 2))
                    .addAction(0, "Mute", serviceAction(ACTION_MUTE, 3));
        }
        builder.addAction(0, "End call", serviceAction(ACTION_END_CALL, 4));
        return builder.build();
    }

    private String notificationMessage() {
        if (transmitting) {
            return videoEnabled
                    ? "Transmitting audio and video. Use the dashboard or Mute to stop."
                    : "Transmitting audio. Use the dashboard or Mute to stop.";
        }
        return videoEnabled
                ? "Camera is off until transmission starts. Resume from FCM_sender."
                : "Call stays active. Accept or resume from FCM_sender.";
    }

    private PendingIntent serviceAction(String action, int requestCode) {
        Intent intent = new Intent(this, WalkieTalkieService.class).setAction(action);
        return PendingIntent.getService(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    @Override
    public void onRoomCreated(String roomCode) {
        this.roomCode = roomCode;
        getSharedPreferences(RECOVERY_PREFERENCES, MODE_PRIVATE)
                .edit()
                .putString("room_code", roomCode)
                .apply();
        callStatus = "Ringing FCM_sender. Waiting for receiver...";
        updateNotification();
        if (listener != null) {
            listener.onRoomCreated(roomCode);
        }
    }

    @Override
    public void onStatus(String status) {
        callStatus = status;
        updateNotification();
        notifyStatus(status);
    }

    @Override
    public void onConnected() {
        connected = true;
        callStatus = "Connected. FCM_sender controls transmission.";
        updateNotification();
        if (listener != null) {
            listener.onConnected();
        }
    }

    @Override
    public void onTransmitChanged(boolean transmitting) {
        applyRemoteTransmitState(transmitting);
        if (transmitting) {
            callStatus = videoEnabled
                    ? "Transmitting audio and video to FCM_sender."
                    : "Transmitting audio to FCM_sender.";
        } else if (connected) {
            callStatus = videoEnabled
                    ? "Connected. FCM_sender can start audio and video transmission."
                    : "Connected. FCM_sender can start audio transmission.";
        }
        updateNotification();
        notifyStatus(callStatus);
        if (listener != null) {
            listener.onTransmitChanged(transmitting);
        }
    }

    @Override
    public void onError(String message) {
        callStatus = message;
        notifyListenerError(message);
        finishCall(message, false);
    }

    @Override
    public void onRoomEnded(String message) {
        finishCall(message, true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        WebRtcWalkieTalkieClient client = callClient;
        if (client != null) {
            Log.w(TAG, "Service destroyed while walkie-talkie client is still active");
        }
        callClient = null;
        releaseCallWakeLock();
        if (client != null) {
            closeCallClient(client);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && audioManager != null) {
            audioManager.unregisterAudioRecordingCallback(recordingCallback);
        }
        super.onDestroy();
    }

    private boolean recoverInterruptedCall() {
        android.content.SharedPreferences preferences =
                getSharedPreferences(RECOVERY_PREFERENCES, MODE_PRIVATE);
        String consumerId = preferences.getString("consumer_id", null);
        long savedAt = preferences.getLong("saved_at", 0L);
        int recoveryCount = preferences.getInt("recovery_count", 0);
        long ageMs = System.currentTimeMillis() - savedAt;
        if (consumerId == null || consumerId.trim().isEmpty()
                || savedAt <= 0L || ageMs < 0L || ageMs > RECOVERY_MAX_AGE_MS
                || recoveryCount >= MAX_AUTOMATIC_RECOVERIES) {
            clearCallRecoveryState();
            return false;
        }

        if (!preferences.edit().putInt("recovery_count", recoveryCount + 1).commit()) {
            Log.e(TAG, "Could not persist walkie-talkie recovery attempt; skipping auto-recovery.");
            return false;
        }
        removeInterruptedRoom(preferences.getString("room_code", null));
        Log.w(TAG, "Recovering interrupted walkie-talkie call once after process restart.");
        startCall(
                consumerId,
                false,
                false,
                true
        );
        return callClient != null;
    }

    private void saveCallForRecovery(
            String consumerId,
            boolean recovering
    ) {
        android.content.SharedPreferences.Editor editor =
                getSharedPreferences(RECOVERY_PREFERENCES, MODE_PRIVATE)
                        .edit()
                        .putString("consumer_id", consumerId)
                        .putLong("saved_at", System.currentTimeMillis());
        if (!recovering) {
            editor.putInt("recovery_count", 0).remove("room_code");
        }
        if (!editor.commit()) {
            Log.e(TAG, "Could not save walkie-talkie recovery state.");
        }
    }

    private void clearCallRecoveryState() {
        getSharedPreferences(RECOVERY_PREFERENCES, MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    private void removeInterruptedRoom(String staleRoomCode) {
        if (staleRoomCode == null || staleRoomCode.trim().isEmpty()) {
            return;
        }
        try {
            WalkieTalkieFirebase.getDatabase(this)
                    .getReference("walkie_talkie/rooms")
                    .child(staleRoomCode)
                    .removeValue()
                    .addOnFailureListener(error ->
                            Log.e(TAG, "Could not remove interrupted call room "
                                    + staleRoomCode, error));
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not clean up interrupted call room " + staleRoomCode, error);
            recordCallFailure("interrupted_room_cleanup", error);
        }
    }

    private void recordCallFailure(String stage, RuntimeException error) {
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCustomKey("walkie_talkie_stage", stage);
            crashlytics.recordException(error);
        } catch (RuntimeException reportingError) {
            Log.e(TAG, "Could not report walkie-talkie failure to Crashlytics.", reportingError);
        }
    }

    private void closeCallClient(WebRtcWalkieTalkieClient client) {
        try {
            client.close();
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not cleanly close the WebRTC call.", error);
            recordCallFailure("call_cleanup", error);
        }
    }

    private void acquireCallWakeLock() {
        if (callWakeLock == null) {
            PowerManager powerManager = getSystemService(PowerManager.class);
            if (powerManager == null) {
                throw new IllegalStateException("Power manager is unavailable.");
            }
            callWakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    getPackageName() + ":WalkieTalkieCall"
            );
            callWakeLock.setReferenceCounted(false);
        }
        if (!callWakeLock.isHeld()) {
            callWakeLock.acquire();
        }
    }

    private void releaseCallWakeLock() {
        if (callWakeLock != null && callWakeLock.isHeld()) {
            callWakeLock.release();
        }
    }
}
