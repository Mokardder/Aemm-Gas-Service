package android.iocl.keepAlive;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import android.iocl.dac_collector.Utility.NotificationHelper;

import androidx.core.app.NotificationCompat;

public class ServiceB extends Service {
    private static final String TAG = "ServiceB";
    private static final String CHANNEL_ID = NotificationHelper.BACKGROUND_SERVICE_CHANNEL_ID;
    private static final int NOTIFICATION_ID = 1102;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ServiceB created");

        NotificationHelper.initializeNotificationChannels(this);

        // Build notification
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ServiceB Running")
                .setContentText("Keeping ServiceA alive")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        // Start foreground
        startForeground(NOTIFICATION_ID, notification);

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
}
