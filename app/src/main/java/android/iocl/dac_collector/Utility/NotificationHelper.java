package android.iocl.dac_collector.Utility;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.MainActivity;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public final class NotificationHelper {

    // Public IDs (adjust if you want different IDs)
    public static final int NOTIFICATION_ID_DAC = 1001;
    public static final int NOTIFICATION_ID_RECHARGE = 1004;
    private static final String CHANNEL_DAC_ID = "DAC Notification Channel";
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



        boolean dark = isDarkMode();

        Log.d("GGGGG", "showDACNotification: isDarkMode ? " + dark);

        int textColor = dark ? Color.WHITE : Color.BLACK;
        int text2 = dark ? Color.parseColor("#ABD7E6") : ContextCompat.getColor(context, R.color.red);


        customLayout.setTextColor(R.id.tv_DAC, textColor);
        customLayout.setTextColor(R.id.dac_val_eng, text2);


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
                .setSmallIcon(R.drawable.ic_cylinder_tile)
                .setCustomContentView(customLayout)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomBigContentView(customLayout)
                .setCustomHeadsUpContentView(customLayout)

                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification notification = builder.build();
        notificationManager.notify(NOTIFICATION_ID_DAC, notification);
    }


    public static void showOtpNotification(Context context, String otp) {
        String channelId = "otp_channel";
        String channelName = "OTP Notifications";

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create channel only once for O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Used for OTP alerts");
            channel.enableLights(true);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("GAS OTP")
                .setContentText("OTP: " + otp)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("OTP: " + otp))
                .setPriority(NotificationCompat.PRIORITY_HIGH)   // heads-up
                .setCategory(NotificationCompat.CATEGORY_MESSAGE) // treat like message
                .setAutoCancel(true);

        notificationManager.notify(1002, builder.build());
    }

    /**
     * Show recharge notification (different layout & id).
     */
    public static void showRechargeNotification(Context context, String text) {

        if (context == null) return;

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        RemoteViews customLayout = new RemoteViews(context.getPackageName(), R.layout.annual_end_notificationbar);
        customLayout.setTextViewText(R.id.alert_text, text);
        boolean dark = isDarkMode();

        int textColor = dark ? Color.WHITE : Color.BLACK;

        customLayout.setTextColor(R.id.alert_text, textColor);
        customLayout.setInt(R.id.speaker_img, "setColorFilter", textColor);


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
                .setSmallIcon(R.drawable.ic_cylinder_tile)
                .setCustomContentView(customLayout)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setAutoCancel(true)

                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification notification = builder.build();
        notificationManager.notify(NOTIFICATION_ID_RECHARGE, notification);
    }

    /**
     * Legacy simple notification (used for pre-M behavior).
     */
    public static void sendNotification(Context context, String title,  String messageBody) {
        if (context == null) return;

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, getPendingIntentFlags());

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, CHANNEL_DEFAULT_ID)
                        .setSmallIcon(R.drawable.gas_cylinder_icon)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_DEFAULT_ID,
                    "SMS Default Notifier",
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


    public static boolean isDarkMode() {
        int mode = AppCompatDelegate.getDefaultNightMode();
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) return true;
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) return false;

        // MODE_NIGHT_FOLLOW_SYSTEM or MODE_NIGHT_UNSPECIFIED → fall back to system
        int systemMode = Resources.getSystem().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return systemMode == Configuration.UI_MODE_NIGHT_YES;
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
