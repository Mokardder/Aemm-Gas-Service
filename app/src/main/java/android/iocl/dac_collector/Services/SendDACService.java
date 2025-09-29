package android.iocl.dac_collector.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.MainActivity;
import android.iocl.dac_collector.Utility.Utility;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class SendDACService extends Service {

    private static final String CHANNEL_ID = "MainServiceActions";
    private static final int NOTIFICATION_ID = 101;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SendDACService", "Service started");

        startServiceWithNotification();

        // Run your work in background so UI thread isn’t blocked
        new Thread(() -> {
            try {
                Utility.getDACMessages(getApplicationContext());
                Log.d("SendDACService", "DAC messages processed");
            } catch (Exception e) {
                Log.e("SendDACService", "Error processing DAC messages", e);
            } finally {
                stopSelfSafely();
            }
        }).start();

        return START_NOT_STICKY; // One-shot, won’t restart if killed
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startServiceWithNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Checking DAC")
                    .setContentText("Searching for DAC...")
                    .setSmallIcon(R.drawable.ic_cylinder_tile)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("Checking DAC")
                    .setContentText("Searching for DAC...")
                    .setSmallIcon(R.drawable.gas_cylinder_icon)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();
        }

        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel nc = new NotificationChannel(
                    CHANNEL_ID,
                    "DAC Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(nc);
            }
        }
    }

    private void stopSelfSafely() {
        stopForeground(true); // Remove foreground notification
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
        stopSelf();
        Log.d("SendDACService", "Service stopped and notification cleared");
    }
}
