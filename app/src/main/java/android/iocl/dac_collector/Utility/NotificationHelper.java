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
import android.iocl.dac_collector.Ui.FloatingWebActivity;
import android.iocl.dac_collector.Ui.MainActivity;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public final class NotificationHelper {

    // Public IDs (adjust if you want different IDs)
    public static final int NOTIFICATION_ID_DAC = 1005;
    public static final int NOTIFICATION_ID_RECHARGE = 1004;
    private static final String CHANNEL_DAC_ID = "DAC Notification Channel";
    private static final String CHANNEL_RECHARGE_ID = "recharge_sms_notify";
    private static final String CHANNEL_DEFAULT_ID = "fcm_default_channel";
    private static final String SMS_NOTIFICATION_ID = "sms_app_default";

    private NotificationHelper() {
        // no instances
    }

    /**
     * Show custom DAC notification.
     */
    public static void showDACNotification(Context context, String otp, String otpType) {

        if (!SharedPrefs.isAllowedBanner()) return;
        if (context == null) return;

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) return;

        // Remote View
        RemoteViews customLayout =
                new RemoteViews(context.getPackageName(), R.layout.dac_notification_bar);

        customLayout.setTextViewText(R.id.dac_val_eng, otp);
        customLayout.setTextViewText(R.id.tv_generated, otpType);

        // DARK MODE CHECK
        boolean dark =
                (context.getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;

        // COLORS
        int cardBg = dark
                ? Color.parseColor("#1E1E1E")
                : Color.parseColor("#FFFFFF");

        int primaryText = dark
                ? Color.WHITE
                : Color.parseColor("#212121");

        int secondaryText = dark
                ? Color.parseColor("#B0BEC5")
                : Color.parseColor("#757575");

        int otpColor = dark
                ? Color.parseColor("#FF8A65")
                : Color.parseColor("#D84315");

        int dividerColor = dark
                ? Color.parseColor("#3A3A3A")
                : Color.parseColor("#E0E0E0");

        // APPLY COLORS
//        customLayout.setInt(R.id.root_card, "setBackgroundColor", cardBg);

        customLayout.setTextColor(R.id.dac_val_eng, otpColor);

        customLayout.setTextColor(R.id.tv_generated, secondaryText);

        customLayout.setTextColor(R.id.tv_share, secondaryText);

        customLayout.setInt(R.id.left_divider, "setBackgroundColor", dividerColor);

        customLayout.setInt(R.id.bottom_divider, "setBackgroundColor", dividerColor);

        // Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    notificationManager.getNotificationChannel(CHANNEL_DAC_ID);

            if (channel == null) {

                channel = new NotificationChannel(
                        CHANNEL_DAC_ID,
                        "LPG OTP",
                        NotificationManager.IMPORTANCE_HIGH
                );

                channel.setDescription("LPG OTP Notification");
                channel.enableVibration(true);

                notificationManager.createNotificationChannel(channel);
            }
        }

        Intent intent = new Intent(context, MainActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                getPendingIntentFlags()
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_DAC_ID)
                        .setSmallIcon(R.drawable.ic_cylinder_tile)
                        .setCustomContentView(customLayout)
                        .setCustomBigContentView(customLayout)
                        .setCustomHeadsUpContentView(customLayout)
                        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        Notification notification = builder.build();

        notificationManager.notify(NOTIFICATION_ID_DAC, notification);
    }


    @SuppressLint("MissingPermission")
    public static void showFloatingWeb(Context context,
                                       String title,
                                       String body,
                                       String url) {

        String channelId = "server_popup";

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Server Popup",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Server controlled popup notifications");
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, FloatingWebActivity.class);
        intent.putExtra("url", url);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        ;

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_cylinder_tile)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
    }


    public static void showOtpNotification(Context context, String otp) {
        if (!SharedPrefs.isAllowedBanner()) return;
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
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification notification = builder.build();
        notificationManager.notify(NOTIFICATION_ID_RECHARGE, notification);
    }

    /**
     * Legacy simple notification (used for pre-M behavior).
     */
    public static void sendNotification(
            Context context,
            String title,
            String messageBody,
            @Nullable Intent intent
    ) {
        if (context == null) return;

        PendingIntent pendingIntent = null;

        if (intent != null) {
            pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    getPendingIntentFlags()
            );
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, CHANNEL_DEFAULT_ID)
                        .setSmallIcon(R.drawable.ic_cylinder_tile)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody)) // show full message
                        .setPriority(NotificationCompat.PRIORITY_HIGH) // for heads-up
                        .setDefaults(NotificationCompat.DEFAULT_ALL) // vibration + sound
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setAutoCancel(true);
        if (pendingIntent != null) {
            notificationBuilder.setContentIntent(pendingIntent);
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_DEFAULT_ID,
                    "SMS Default Notifier",
                    NotificationManager.IMPORTANCE_HIGH // heads-up for Android O+
            );
            channel.enableVibration(true);
            channel.setSound(defaultSoundUri, Notification.AUDIO_ATTRIBUTES_DEFAULT);
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
