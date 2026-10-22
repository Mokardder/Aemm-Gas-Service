package android.iocl.keepAlive;


import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import android.iocl.dac_collector.Utility.NotificationHelper;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class ServiceA extends Service {
    private static final String TAG = "ServiceA";
    private static final String CHANNEL_ID = NotificationHelper.BACKGROUND_SERVICE_CHANNEL_ID;
    private static final int NOTIFICATION_ID = 1101;

    /** Starts ServiceA as a foreground service on Android 8 and later. */
    public static void start(Context context) {
        Intent intent = new Intent(context, ServiceA.class);
        try {
            ContextCompat.startForegroundService(context, intent);
        } catch (RuntimeException e) {
            Log.e(TAG, "Android did not allow the KeepAlive service to start", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ServiceA created");

        // Start as foreground
        startForegroundService();

        // Start native watcher
        int pid = Process.myPid();
        new NativeDaemon().startWatch(pid, getPackageName());

        // Ensure ServiceB is running
        startService(new Intent(this, ServiceB.class));
    }

    private void startForegroundService() {
        NotificationHelper.initializeNotificationChannels(this);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("App running")
                .setContentText("Background service active")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ServiceA running, keeping ServiceB alive");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ServiceA destroyed, restarting ServiceB");
        startService(new Intent(this, ServiceB.class));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
