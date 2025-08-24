package android.iocl.dac_collector.Utility;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.MainActivity;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public final class NotificationHelper {

    // Public IDs (adjust if you want different IDs)
    public static final int NOTIFICATION_ID_DAC = 1001;
    public static final int NOTIFICATION_ID_RECHARGE = 1004;
    private static final String CHANNEL_DAC_ID = "dac_sms_notify";
    private static final String CHANNEL_RECHARGE_ID = "recharge_sms_notify";
    private static final String CHANNEL_DEFAULT_ID = "fcm_default_channel";

    private NotificationHelper() {
        // no instances
    }

    /**
     * Show custom DAC notification.
     */
    public static void showDACNotification(Context context, String otp) {
        if (context == null) return;

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        // custom RemoteViews
        RemoteViews customLayout = new RemoteViews(context.getPackageName(), R.layout.dac_notification_bar);
        customLayout.setTextViewText(R.id.dac_val_eng, otp);
        if (isSystemInDarkMode(context)) {
            customLayout.setTextColor(R.id.tvTime, Color.WHITE);
            customLayout.setInt(R.id.clock_img, "setColorFilter", Color.WHITE);
        } else {
            customLayout.setTextColor(R.id.tvTime, Color.BLACK);
            customLayout.setInt(R.id.clock_img, "setColorFilter", Color.BLACK);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            customLayout.setViewVisibility(R.id.call_Aemm, View.VISIBLE);
            customLayout.setOnClickResponse(R.id.call_Aemm, makeCall("+919231902703", context));
        } else {
            customLayout.setViewVisibility(R.id.call_Aemm, View.GONE);
        }

        // Create channel for O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_DAC_ID);
            if (channel == null) {
                channel = new NotificationChannel(CHANNEL_DAC_ID, CHANNEL_DAC_ID, NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("DAC -> " + otp);
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, getPendingIntentFlags());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_DAC_ID)
                .setSmallIcon(R.drawable.gas_cylinder_icon)
                .setCustomContentView(customLayout)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomBigContentView(customLayout)
                .setCustomHeadsUpContentView(customLayout)
                .setColorized(true)
                .setColor(Color.WHITE)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Notification notification = builder.build();
        notificationManager.notify(NOTIFICATION_ID_DAC, notification);
    }

    /**
     * Show recharge notification (different layout & id).
     */
    public static void showRechargeNotification(Context context, String text) {

        if (context == null) return;
        Log.d("MyFirebaseMsgService", "showRechargeNotification: " + text);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        RemoteViews customLayout = new RemoteViews(context.getPackageName(), R.layout.annual_end_notificationbar);
        customLayout.setTextViewText(R.id.tvTime, text);


        if (isSystemInDarkMode(context)) {
            customLayout.setTextColor(R.id.tvTime, Color.WHITE);

            customLayout.setInt(R.id.clock_img, "setColorFilter", Color.WHITE);
        } else {
            customLayout.setTextColor(R.id.tvTime, Color.BLACK);
            customLayout.setInt(R.id.clock_img, "setColorFilter", Color.BLACK);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            customLayout.setViewVisibility(R.id.call_Aemm, View.VISIBLE);
            customLayout.setOnClickResponse(R.id.call_Aemm, makeCall("+919932896502", context));
        } else {
            customLayout.setViewVisibility(R.id.call_Aemm, View.GONE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_RECHARGE_ID);
            if (channel == null) {
                channel = new NotificationChannel(CHANNEL_RECHARGE_ID, CHANNEL_RECHARGE_ID, NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Recharge -> " + text);
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, getPendingIntentFlags());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_RECHARGE_ID)
                .setSmallIcon(R.drawable.gas_cylinder_icon)
                .setCustomContentView(customLayout)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setAutoCancel(true)
                .setColor(Color.parseColor("#14A44D"))
                .setColorized(true)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification notification = builder.build();
        notificationManager.notify(NOTIFICATION_ID_RECHARGE, notification);
    }

    public static boolean isSystemInDarkMode(Context ctx) {
        int uiMode = ctx.getResources().getConfiguration().uiMode;
        int nightModeFlags = uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Legacy simple notification (used for pre-M behavior).
     */
    public static void sendNotification(Context context, String messageBody) {
        if (context == null) return;

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, getPendingIntentFlags());

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, CHANNEL_DEFAULT_ID)
                        .setSmallIcon(R.drawable.gas_cylinder_icon)
                        .setContentTitle("GAS MESSAGE")
                        .setContentText("DAC -> " + messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_DEFAULT_ID,
                    "Default channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }

    /**
     * Returns true if there is an active notification with the provided id.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static boolean isNotificationActive(Context context, int notificationId) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return false;

        try {
            for (android.service.notification.StatusBarNotification sbn : nm.getActiveNotifications()) {
                if (sbn.getId() == notificationId) {
                    return true;
                }
            }
        } catch (Exception e) {
            // some devices may throw; swallow and return false
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Helper to create RemoteViews.PendingResponse for a phone dial action (API 31+ uses RemoteResponse).
     */
    @SuppressLint("NewApi")
    private static RemoteViews.RemoteResponse makeCall(String phoneNumber, Context context) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(android.net.Uri.parse("tel:" + phoneNumber));
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | getPendingIntentImmutableFlag()
        );
        return RemoteViews.RemoteResponse.fromPendingIntent(pendingIntent);
    }

    // Small helpers for PendingIntent flags to support different SDKs
    private static int getPendingIntentFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_IMMUTABLE;
        } else {
            return 0;
        }
    }

    private static int getPendingIntentImmutableFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_IMMUTABLE;
        } else {
            return 0;
        }
    }
}
