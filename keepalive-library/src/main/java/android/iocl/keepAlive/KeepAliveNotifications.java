package android.iocl.keepAlive;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

final class KeepAliveNotifications {
    static final String CHANNEL_ID = "keepalive_service";

    private KeepAliveNotifications() {
    }

    static Notification build(Context context, String title, String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);
            if (manager == null) {
                throw new IllegalStateException("Notification manager is unavailable");
            }
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "KeepAlive service",
                    NotificationManager.IMPORTANCE_LOW
            );
            manager.createNotificationChannel(channel);
            return new Notification.Builder(context, CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setOngoing(true)
                    .build();
        }
        return new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build();
    }

    static void startService(Context context, Class<?> serviceClass) {
        android.content.Intent intent = new android.content.Intent(context, serviceClass);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}
