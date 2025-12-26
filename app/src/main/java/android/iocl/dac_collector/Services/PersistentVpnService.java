package android.iocl.dac_collector.Services;

import static androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.MainActivity;
import android.iocl.dac_collector.Utility.Utility;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PersistentVpnService extends VpnService {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String TAG = "PersistentVpnService";

    public static final String ACTION_START = "android.iocl.dac_collector.action.START_VPN";
    public static final String ACTION_DISCONNECT = "android.iocl.dac_collector.action.DISCONNECT_VPN";
    public static final String ACTION_RESTART = "android.iocl.dac_collector.action.RESTART_VPN";

    public static final String BROADCAST_ACTION = "VPN_STATUS_UPDATE";
    public static final String STATUS_EXTRA = "status";

    public static final String PREFS = "persistent_vpn_prefs";
    private static final String PREF_SHOULD_RESTART = "vpn_service_should_restart";
    private static final String PREF_MARKED_RUNNING = "vpn_service_running";

    // Modes
    public static final String EXTRA_MODE = "extra_mode";
    public static final String MODE_DUMMY = "mode_dummy";
    public static final String MODE_NORMAL = "mode_normal"; // for future use

    // Notification / restart
    private static final String NOTIFICATION_CHANNEL_ID = "vpn_channel"; // no spaces
    private static final String EMERGENCY_CHANNEL_ID = "emergency_vpn_channel";
    private static final int NOTIFICATION_ID = 0xC0FFEE;
    private static final long RESTART_DELAY_MS = 6_000L; // 6 seconds

    private ParcelFileDescriptor vpnInterface;
    private volatile boolean isRunning = false;
    private volatile boolean foregroundStarted = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        // Create notification channel immediately
        createNotificationChannel();

        // default: allow restart
        getPrefs().edit().putBoolean(PREF_SHOULD_RESTART, true).apply();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Not intended for binding
        return null;
    }

    /**
     * Expect callers to call startForegroundService(...) on O+.
     * Intent extras:
     * - EXTRA_MODE = MODE_DUMMY (recommended) or MODE_NORMAL
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // First ensure notification channel exists
        ensureNotificationChannel();

        // Try to start foreground with primary notification
        try {
            Notification notification = createMinimalNotification();
            startForeground(NOTIFICATION_ID, notification);
            foregroundStarted = true;
            Log.d(TAG, "Foreground successfully started with primary notification");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground with primary notification", e);

            // Try emergency fallback
            try {
                createEmergencyChannel();
                Notification emergencyNotification = createEmergencyNotification();
                startForeground(NOTIFICATION_ID, emergencyNotification);
                foregroundStarted = true;
                Log.d(TAG, "Foreground started with emergency notification");
            } catch (Exception e2) {
                Log.e(TAG, "Emergency foreground also failed", e2);
                // Stop service to avoid crash
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        // Then handle the actual command
        String action = intent != null ? intent.getAction() : ACTION_START;

        if (ACTION_DISCONNECT.equals(action)) {
            markShouldNotRestart();
            disconnect();
            return START_NOT_STICKY;
        } else {
            startVpnInternal(intent);
            return START_STICKY;
        }
    }

    private Notification createMinimalNotification() {
        // Ensure channel exists
        String channelId = getValidChannelId();

        // Ultra-minimal notification that should never fail
        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("VPN Service")
                .setContentText("Running")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // System icon - guaranteed to exist
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW) // Changed from MIN to LOW
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setShowWhen(false)
                .setSilent(true)
                .build();
    }

    private Notification createEmergencyNotification() {
        String channelId = EMERGENCY_CHANNEL_ID;

        // For Android O and above, use the emergency channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new NotificationCompat.Builder(this, channelId)
                    .setContentTitle("Service")
                    .setContentText("Running")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();
        } else {
            // Pre-Oreo doesn't need channels
            return new NotificationCompat.Builder(this)
                    .setContentTitle("Service")
                    .setContentText("Running")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();
        }
    }

    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Check if main channel exists
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                NotificationChannel channel = nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID);
                if (channel == null) {
                    // Channel doesn't exist, create it
                    createNotificationChannel();
                }
            }
        }
    }

    private String getValidChannelId() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                // Check if main channel exists
                NotificationChannel channel = nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID);
                if (channel != null) {
                    return NOTIFICATION_CHANNEL_ID;
                }

                // Check if emergency channel exists
                channel = nm.getNotificationChannel(EMERGENCY_CHANNEL_ID);
                if (channel != null) {
                    return EMERGENCY_CHANNEL_ID;
                }
            }
        }
        // For pre-Oreo or if no channel exists
        return "";
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        "Block this channel too (VPN)",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Channel for persistent VPN service");
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setSound(null, null);
                channel.setShowBadge(false);
                channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    // Delete existing channel first to avoid any issues
                    nm.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID);
                    nm.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel created: " + NOTIFICATION_CHANNEL_ID);
                }
            } catch (Exception e) {
                Log.e(TAG, "createNotificationChannel failed", e);
            }
        }
    }

    private void createEmergencyChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        EMERGENCY_CHANNEL_ID,
                        "VPN Service",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Emergency VPN service channel");
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setSound(null, null);
                channel.setShowBadge(false);
                channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    nm.createNotificationChannel(channel);
                    Log.d(TAG, "Emergency notification channel created");
                }
            } catch (Exception e) {
                Log.e(TAG, "createEmergencyChannel failed", e);
            }
        }
    }

    private void startVpnInternal(Intent intent) {
        if (isRunning) {
            // already running -- refresh foreground notification
            try {
                startForegroundSafe(createNotification());
            } catch (Exception ignored) {
            }
            broadcastStatus("VPN Already Running");
            return;
        }

        final String mode = (intent != null) ? intent.getStringExtra(EXTRA_MODE) : MODE_DUMMY;
        final boolean dummy = MODE_DUMMY.equals(mode);

        Log.d(TAG, "Starting VPN (dummy=" + dummy + ")");

        // Update notification with VPN status
        updateNotificationWithVPNStatus();

        // Build VPN interface
        Builder builder = new Builder();
        builder.setSession("Persistent Dummy VPN");
        builder.setMtu(1500);

        // Minimal TUN address required by certain devices
        // Use a /32 so we don't influence device routes
        builder.addAddress("10.0.0.2", 32);

        // DNS optional; doesn't affect routing if no route is added
        builder.addDnsServer("8.8.8.8");

        // IMPORTANT: Do NOT add default route in dummy mode.
        if (!dummy) {
            // If you later want interception, uncomment:
            // builder.addRoute("0.0.0.0", 0);
        }

        // Optional: restrict VPN to your app only if you planned to process app traffic.
        try {
            builder.addAllowedApplication(getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Package not found when adding allowed application", e);
            // not fatal; continue
        }

        try {
            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                Log.e(TAG, "builder.establish() returned null; cannot start VPN");
                stopSelf();
                return;
            }

            isRunning = true;
            getPrefs().edit().putBoolean(PREF_MARKED_RUNNING, true).apply();
            broadcastStatus("VPN Connected (dummy=" + dummy + ")");

            Log.d(TAG, "VPN established (fd=" + vpnInterface.getFileDescriptor() + ")");
        } catch (Exception e) {
            Log.e(TAG, "Failed to establish VPN", e);
            scheduleRestart();
            stopSelf();
        }
    }

    private void updateNotificationWithVPNStatus() {
        try {
            Notification notification = createNotification();
            if (foregroundStarted && notification != null) {
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    nm.notify(NOTIFICATION_ID, notification);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update notification with VPN status", e);
        }
    }

    /**
     * Cleanly disconnect VPN.
     * If the user explicitly requested stop (markShouldNotRestart), this will avoid scheduling a restart.
     */
    public void disconnect() {
        if (!isRunning) {
            Log.d(TAG, "disconnect: not running");
            return;
        }

        isRunning = false;
        getPrefs().edit().putBoolean(PREF_MARKED_RUNNING, false).apply();

        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (Exception e) {
                Log.w(TAG, "Error closing VPN interface: " + e.getMessage());
            }
            vpnInterface = null;
        }

        broadcastStatus("VPN Disconnected");
        try {
            stopForeground(true);
        } catch (Exception ignored) {
        }

        // schedule restart unless user explicitly stopped it
        scheduleRestart();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        disconnect();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // if app removed from recents, try scheduling restart to retain persistence (subject to OEM).
        scheduleRestart();
    }

    /* ----------------- Helpers ----------------- */

    private Notification createNotification() {
        try {
            Intent openApp = new Intent(this, MainActivity.class);
            PendingIntent openPending = PendingIntent.getActivity(
                    this, 0, openApp,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            // Disconnect action
            Intent disconnectInt = new Intent(this, PersistentVpnService.class);
            disconnectInt.setAction(ACTION_DISCONNECT);
            PendingIntent disconnectPending = PendingIntent.getService(
                    this, 0, disconnectInt,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            int smallIconRes = R.drawable.ic_cylinder_tile;
            // Fallback to system icon if your drawable doesn't exist on some OEM builds
            try {
                getResources().getResourceName(smallIconRes);
            } catch (Exception e) {
                smallIconRes = android.R.drawable.ic_dialog_info;
            }

            // Use valid channel ID
            String channelId = getValidChannelId();
            if (channelId.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channelId = NOTIFICATION_CHANNEL_ID;
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle("VPN Connected")
                    .setContentText("Vpn Status - Active")
                    .setSmallIcon(smallIconRes)
                    .addAction(new NotificationCompat.Action(0, "Disconnect", disconnectPending))
                    .setSilent(true)
                    .setShowWhen(false)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW); // Changed from MIN to LOW

            // For older API levels, ensure notification is valid
            Notification notification = builder.build();
            return notification;
        } catch (Exception e) {
            Log.e(TAG, "createNotification failed, returning fallback notification", e);
            // final fallback
            String channelId = getValidChannelId();
            if (channelId.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channelId = NOTIFICATION_CHANNEL_ID;
            }

            NotificationCompat.Builder fallback = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle("VPN Active")
                    .setContentText("Running")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW); // Changed from MIN to LOW
            return fallback.build();
        }
    }

    private void startForegroundSafe(Notification notification) {
        try {
            if (!foregroundStarted) {
                startForeground(NOTIFICATION_ID, notification);
                foregroundStarted = true;
                Log.d(TAG, "startForeground invoked");
            } else {
                // update existing notification
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null && notification != null) {
                    nm.notify(NOTIFICATION_ID, notification);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "startForegroundSafe failed", e);
        }
    }

    private void broadcastStatus(String status) {
        try {
            Intent intent = new Intent(BROADCAST_ACTION);
            intent.putExtra(STATUS_EXTRA, status);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } catch (Exception ignored) {
        }
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private void markShouldNotRestart() {
        getPrefs().edit().putBoolean(PREF_SHOULD_RESTART, false).apply();
    }

    private void scheduleRestart() {
        boolean shouldRestart = getPrefs().getBoolean(PREF_SHOULD_RESTART, true);
        if (!shouldRestart) {
            Log.d(TAG, "scheduleRestart: restart disabled in prefs");
            return;
        }

        Intent restartIntent = new Intent(this, PersistentVpnService.class);
        restartIntent.setAction(ACTION_RESTART);
        // Preserve mode: start again as dummy
        restartIntent.putExtra(EXTRA_MODE, MODE_DUMMY);

        PendingIntent pi = PendingIntent.getService(
                this,
                0,
                restartIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long triggerAt = System.currentTimeMillis() + RESTART_DELAY_MS;
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
            Log.d(TAG, "Scheduled restart in " + RESTART_DELAY_MS + "ms");
        } else {
            Log.w(TAG, "AlarmManager unavailable; cannot schedule restart");
        }
    }
}