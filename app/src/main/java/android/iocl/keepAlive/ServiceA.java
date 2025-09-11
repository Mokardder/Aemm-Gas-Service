package android.iocl.keepAlive;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class ServiceA extends Service {
    private static final String TAG = "ServiceA";
    private static final String CHANNEL_ID = "keepalive_channel";

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Block this | Keep Alive",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps the app alive in background");
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("App running")
                .setContentText("Background service active")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);
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
