package android.iocl.keepAlive;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class ServiceB extends Service {
    private static final String TAG = "ServiceB";
    private static final String CHANNEL_ID = "ServiceBChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ServiceB created");

        // Create notification channel for foreground service (Android 8+)
        createNotificationChannel();

        // Build notification
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ServiceB Running")
                .setContentText("Keeping ServiceA alive")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        // Start foreground
        startForeground(1, notification);

        // Ensure ServiceA is running
        startService(new Intent(this, ServiceA.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ServiceB running, keeping ServiceA alive");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ServiceB destroyed, restarting ServiceA");
        startService(new Intent(this, ServiceA.class));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Block this | Keep Alive B",
                    NotificationManager.IMPORTANCE_MIN
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
