package android.iocl.dac_collector.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.MainActivity;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FixOppoAutoKill extends Service {
    private static final String CHANNEL_ID = "TimeUpdateChannel";
    private static final String CHANNEL_DESC = "Updating Channel";
    private static final int NOTIFICATION_ID = 1;
    private Handler handler = new Handler();
    private Runnable timeUpdater;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, sendTimeNotification("Initializing...", getApplicationContext()));

            timeUpdater = new Runnable() {
                @Override
                public void run() {
                    updateNotification();
                    handler.postDelayed(this,  60 * 1000); // Update every second
                }
            };
            handler.post(timeUpdater);
        } catch (Exception e) {
            Log.e("FixOppoAutoKill", "Error during service creation: " + e.getMessage(), e);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(timeUpdater);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Context context = getApplicationContext(); // Ensure the context is valid
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (manager == null) {
                Log.e("FixOppoAutoKill", "NotificationManager is null. Delaying channel creation.");
                new Handler().postDelayed(this::createNotificationChannel,  60*1000); // Retry after 1 second
                return;
            }

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Time Update Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for updating time in notification.");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);


            try {
                manager.createNotificationChannel(channel);
                Log.d("FixOppoAutoKill", "Notification channel created successfully.");
            } catch (Exception e) {
                Log.e("FixOppoAutoKill", "Error creating notification channel: " + e.getMessage(), e);
            }
        }
    }



    public static Notification sendTimeNotification(String messageBody, Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        String channelId = "fcm_default_channel";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.clock_time)
                .setContentTitle("Time Updater")
                .setContentText("Current Time -> " + messageBody)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSilent(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Channel Title", NotificationManager.IMPORTANCE_DEFAULT);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Return the built notification
        return notificationBuilder.build();
    }


    public static Notification updateTimerNotification(Context context, String OTP) {
        final int NOTIFY_ID = 1003;
        String DAC_CUSTOM_NOTIFY_ID = "timer_sms_notify";

        RemoteViews customLayout = new RemoteViews(context.getPackageName(), R.layout.fix_oppo_kill_layout);
        customLayout.setTextViewText(R.id.tvTime, OTP);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DAC_CUSTOM_NOTIFY_ID)
                .setSmallIcon(R.drawable.clock_time)
                .setCustomContentView(customLayout)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setAutoCancel(true)
                .setColor(Color.parseColor("#0Fffc107"))
                .setColorized(true)
                .setSilent(true)
//                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    DAC_CUSTOM_NOTIFY_ID, "Show Timer", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Show Timer Notify");
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Return the built notification
        return builder.build();
    }



    private void updateNotification() {
        String currentTime = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(new Date());
        Notification notification = updateTimerNotification(getApplicationContext(), currentTime);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }
}
