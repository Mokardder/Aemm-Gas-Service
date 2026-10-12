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
    private static final String CHANNEL_ID = NotificationHelper.BLOCK_NOTIFICATION_CHANNEL_ID;

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

        startAsForegroundService();
        new NativeDaemon().startWatch(Process.myPid(), getPackageName());
    }


    private void startAsForegroundService() {
        NotificationHelper.initializeNotificationChannels(this);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("KeepAlive active")
                .setContentText("Background recovery service is running")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "KeepAlive foreground service running");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "KeepAlive service stopped; Android may recreate it because it is sticky");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}