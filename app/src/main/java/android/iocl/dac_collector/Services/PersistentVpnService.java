package android.iocl.dac_collector.Services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.MainActivity;
import android.iocl.dac_collector.Utility.Utility;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

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
    private static final String NOTIFICATION_CHANNEL_ID = "Block this too (VPN)";
    private static final int NOTIFICATION_ID = 0xC0FFEE;
    private static final long RESTART_DELAY_MS = 6_000L; // 6 seconds

    private ParcelFileDescriptor vpnInterface;
    private volatile boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
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
        String action = intent != null ? intent.getAction() : ACTION_START;
        Log.d(TAG, "onStartCommand action=" + action);

        if (ACTION_DISCONNECT.equals(action)) {
            // explicit user-initiated stop
            markShouldNotRestart();
            disconnect();
            return START_NOT_STICKY;
        } else if (ACTION_RESTART.equals(action)) {
            // restart requested externally
            startVpnInternal(intent);
            return START_STICKY;
        } else { // ACTION_START or default
            startVpnInternal(intent);
            return START_STICKY;
        }
    }

    private void startVpnInternal(Intent intent) {
        if (isRunning) {
            // already running -- refresh foreground notification
            try {
                startForeground(NOTIFICATION_ID, createNotification());
            } catch (Exception ignored) {
            }
            broadcastStatus("VPN Already Running");
            return;
        }

        final String mode = (intent != null) ? intent.getStringExtra(EXTRA_MODE) : MODE_DUMMY;
        final boolean dummy = MODE_DUMMY.equals(mode);

        Log.d(TAG, "Starting VPN (dummy=" + dummy + ")");

        createNotificationChannel();
        // move to foreground immediately to reduce chance of kill
        startForeground(NOTIFICATION_ID, createNotification());

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
            throw new RuntimeException(e);
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


            executor.execute(() -> {
                if (!Utility.isInternetAvailable(this)) {
                    Toast.makeText(this, "Device appears offline after establishing dummy VPN — rolling back", Toast.LENGTH_SHORT).show();
                    disconnect();
                    return;
                }
            });


            Log.d(TAG, "VPN established (fd=" + vpnInterface.getFileDescriptor() + ")");
        } catch (Exception e) {
            Log.e(TAG, "Failed to establish VPN", e);
            scheduleRestart();
            stopSelf();
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Block this notification too (For VPN)",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for persistent VPN service | block this");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
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

        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Dummy VPN Active")
                .setContentText("VPN running (no-route dummy mode)")
                .setSmallIcon(R.drawable.ic_cylinder_tile)

                .addAction(new NotificationCompat.Action(0, "Disconnect", disconnectPending))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void broadcastStatus(String status) {
        try {
            Intent intent = new Intent(BROADCAST_ACTION);
            intent.putExtra(STATUS_EXTRA, status);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } catch (Exception ignored) {
        }
    }

    /**
     * Quick connectivity check. If the device has no active network after establishing the TUN,
     * many OEMs treat TUN creation as a sink and connectivity can break. We detect that and roll back.
     */


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
