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
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.WalkieTalkieActivity;
import android.iocl.dac_collector.Utility.NotificationHelper;

import java.util.List;

public final class WalkieTalkieService extends Service
        implements WebRtcWalkieTalkieClient.Listener {

    public static final String ACTION_START_CALL = "android.iocl.dac_collector.walkie.START";
    public static final String ACTION_TALK = "android.iocl.dac_collector.walkie.TALK";
    public static final String ACTION_MUTE = "android.iocl.dac_collector.walkie.MUTE";
    public static final String ACTION_END_CALL = "android.iocl.dac_collector.walkie.END";
    public static final String EXTRA_CONSUMER_ID = "consumer_id";
    public static final String EXTRA_DASHBOARD_REQUEST = "dashboard_request";

    private static final String CHANNEL_ID = NotificationHelper.BACKGROUND_SERVICE_CHANNEL_ID;
    private static final int NOTIFICATION_ID = 1301;
    private static final String TAG = "WalkieTalkieService";

    private final IBinder binder = new LocalBinder();
    private WebRtcWalkieTalkieClient callClient;
    private WebRtcWalkieTalkieClient.Listener listener;
    private String callStatus = "Ready to call FCM_sender.";
    private String roomCode;
    private boolean connected;
    private boolean transmitting;
    private AudioManager audioManager;

    private final AudioManager.AudioRecordingCallback recordingCallback =
            new AudioManager.AudioRecordingCallback() {
                @Override
                public void onRecordingConfigChanged(
                        List<AudioRecordingConfiguration> configurations
                ) {
                    if (callClient != null && hasSilencedRecording(configurations)) {
                        endCall("Another app started using the microphone. The call was ended.");
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
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_START_CALL.equals(action)) {
            String consumerId = intent.getStringExtra(EXTRA_CONSUMER_ID);
            boolean dashboardRequested = intent.getBooleanExtra(EXTRA_DASHBOARD_REQUEST, false);
            startCall(consumerId, dashboardRequested);
        } else if (ACTION_TALK.equals(action)) {
            setTransmitting(true);
        } else if (ACTION_MUTE.equals(action)) {
            setTransmitting(false);
        } else if (ACTION_END_CALL.equals(action)) {
            endCall("Room ended");
        }
        return START_NOT_STICKY;
    }

    private void startCall(String consumerId, boolean dashboardRequested) {
        if (callClient != null) {
            return;
        }
        if (consumerId == null || consumerId.trim().isEmpty()
                || consumerId.contains("not_found")) {
            callStatus = "Set up the consumer profile before calling.";
            notifyListenerError(callStatus);
            stopSelf();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            callStatus = "Microphone permission is required to use push-to-talk.";
            notifyListenerError(callStatus);
            stopSelf();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !audioManager.getActiveRecordingConfigurations().isEmpty()) {
            callStatus = "Another app is using the microphone. End its recording before calling.";
            notifyListenerError(callStatus);
            stopSelf();
            return;
        }

        try {
            startCallForeground();
            callStatus = "Setting up WebRTC audio...";
            updateNotification();
            String participantId = consumerId.trim();
            callClient = new WebRtcWalkieTalkieClient(
                    this,
                    participantId,
                    participantId,
                    this
            );
            callClient.createRoom();
            notifyStatus(callStatus);
        } catch (SecurityException error) {
            Log.e(TAG, "Could not start microphone foreground service", error);
            endCall("Could not start the background call. Check microphone permission.");
            if (dashboardRequested) {
                showDashboardCallRequestFallback();
            }
            notifyListenerError(callStatus);
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not initialize walkie-talkie call", error);
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

    private void startCallForeground() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
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
        finishCall(message, true);
    }

    private void finishCall(String message, boolean notifyEnded) {
        WebRtcWalkieTalkieClient client = callClient;
        callClient = null;
        transmitting = false;
        connected = false;
        roomCode = null;
        callStatus = message;
        if (client != null) {
            client.close();
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
                .setContentText(transmitting
                        ? "Transmitting audio. Use the dashboard or Mute to stop."
                        : "Call stays active. Accept or resume from FCM_sender.")
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
        callStatus = transmitting
                ? "Transmitting audio to FCM_sender."
                : connected ? "Connected. FCM_sender can start transmission."
                : callStatus;
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
        callClient = null;
        if (client != null) {
            client.close();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && audioManager != null) {
            audioManager.unregisterAudioRecordingCallback(recordingCallback);
        }
        super.onDestroy();
    }
}
