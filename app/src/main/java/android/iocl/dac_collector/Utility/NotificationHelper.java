package android.iocl.dac_collector.Utility;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.Manifest;
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
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public final class NotificationHelper {

        private static final String TAG = "NotificationHelper";

    // Public IDs (adjust if you want different IDs)
    public static final int NOTIFICATION_ID_DAC = 1005;
        public static final int NOTIFICATION_ID_RECHARGE = 1004;
        public static final String BLOCK_NOTIFICATION_CHANNEL_ID = "block_notifications";
    public static final String FCM_NOTIFICATION_CHANNEL_ID = "fcm_notifications";
    public static final String OTP_NOTIFICATION_CHANNEL_ID = "otp_notifications";
        private static final String UPDATE_NOTIFICATION_CHANNEL_ID = "app_updates";
        private static final int NOTIFICATION_ID_UPDATE = 1201;
    private static final String CHANNEL_DAC_ID = OTP_NOTIFICATION_CHANNEL_ID;
    private static final String CHANNEL_OTP_ID = OTP_NOTIFICATION_CHANNEL_ID;
    private static final String CHANNEL_RECHARGE_ID = FCM_NOTIFICATION_CHANNEL_ID;
    private static final String CHANNEL_DEFAULT_ID = BLOCK_NOTIFICATION_CHANNEL_ID;
    private static final String CHANNEL_SERVER_POPUP_ID = FCM_NOTIFICATION_CHANNEL_ID;

    private NotificationHelper() {
        // no instances
    }

    public static void initializeBlockNotificationChannel(Context context) {
        initializeNotificationChannels(context);
    }

    public static void initializeNotificationChannels(Context context) {
        ensureNotificationChannel(
                context,
                FCM_NOTIFICATION_CHANNEL_ID,
                "FCM Notifications",
                NotificationManager.IMPORTANCE_HIGH
        );
        ensureNotificationChannel(
                context,
                OTP_NOTIFICATION_CHANNEL_ID,
                "OTP Notifications",
                NotificationManager.IMPORTANCE_HIGH
        );
        ensureNotificationChannel(
                context,
                BLOCK_NOTIFICATION_CHANNEL_ID,
                "Block This",
                NotificationManager.IMPORTANCE_LOW
        );
        ensureNotificationChannel(
                context,
                UPDATE_NOTIFICATION_CHANNEL_ID,
                "App updates",
                NotificationManager.IMPORTANCE_DEFAULT
        );
    }

    private static void ensureNotificationChannel(
            Context context,
            String channelId,
            String channelName,
            int importance
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context == null) return;

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null
                || notificationManager.getNotificationChannel(channelId) != null) {
            return;
        }

        notificationManager.createNotificationChannel(
                new NotificationChannel(channelId, channelName, importance)
        );
    }

    public static void showUpdateAvailable(Context context, String version) {
        if (context == null) {
            Log.w(TAG, "Update notification skipped: context is null");
            return;
        }

        ensureNotificationChannel(
                context,
                UPDATE_NOTIFICATION_CHANNEL_ID,
                "App updates",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        Intent intent = new Intent(context, MainActivity.class)
                .putExtra("open_app_update", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_UPDATE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | getPendingIntentImmutableFlag()
        );

        String releaseVersion = version == null || version.trim().isEmpty()
                ? "new version"
                : version;
        Notification notification = new NotificationCompat.Builder(
                context,
                UPDATE_NOTIFICATION_CHANNEL_ID
        )
                .setSmallIcon(R.drawable.ic_cylinder_tile)
                .setContentTitle("Update downloaded")
                .setContentText("Version " + releaseVersion + " is ready to install.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Version " + releaseVersion
                                + " is ready. Tap to review and install the update."))
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_UPDATE, notification);
        } catch (SecurityException e) {
            Log.w(TAG, "Update notification permission is unavailable", e);
        }
    }

    /**
     * Show custom DAC notification.
     */
    public static void showDACNotification(Context context, String otp, String otpType) {

                if (context == null) {
                        Log.w(TAG, "DAC notification skipped: context is null");
                        return;
                }

                if (!SharedPrefs.isAllowedBanner()) {
                        Log.w(TAG, "DAC notification skipped: sms_banner is disabled");
                        return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        Log.w(TAG, "DAC notification skipped: POST_NOTIFICATIONS permission is denied");
                        return;
                }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (notificationManager == null) {
                        Log.w(TAG, "DAC notification skipped: NotificationManager is unavailable");
                        return;
                }

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

        ensureNotificationChannel(context, CHANNEL_DAC_ID, "LPG OTP", NotificationManager.IMPORTANCE_HIGH);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_DAC_ID);
                        if (channel == null || channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                                Log.w(TAG, "DAC notification skipped: LPG OTP channel is blocked");
                                return;
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

                if (context == null || url == null || url.trim().isEmpty()) {
                        Log.w(TAG, "Web notification skipped: URL is empty");
                        return;
                }

                Uri parsedUrl = Uri.parse(url.trim());
                String scheme = parsedUrl.getScheme();
                if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                        Log.w(TAG, "Web notification skipped: unsupported URL " + url);
                        return;
                }

                if (title == null || title.trim().isEmpty()) {
                        title = "New notification";
                }
                if (body == null) {
                        body = "";
                }

        String channelId = CHANNEL_SERVER_POPUP_ID;

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        ensureNotificationChannel(context, channelId, "FCM Notifications", NotificationManager.IMPORTANCE_HIGH);

        Intent intent = new Intent(context, FloatingWebActivity.class);
        intent.putExtra("url", parsedUrl.toString());
        intent.setData(parsedUrl);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        (int) (System.currentTimeMillis() & 0x7FFFFFFF),
                        intent,
                        PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_cylinder_tile)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
    }


    public static void showOtpNotification(Context context, String otp) {
        if (!SharedPrefs.isAllowedBanner()) return;
        String channelId = CHANNEL_OTP_ID;

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        ensureNotificationChannel(context, channelId, "OTP Notifications", NotificationManager.IMPORTANCE_HIGH);

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
                            ensureNotificationChannel(context, CHANNEL_RECHARGE_ID, "FCM Notifications", NotificationManager.IMPORTANCE_HIGH);
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

        ensureNotificationChannel(context, CHANNEL_DEFAULT_ID, "Block This", NotificationManager.IMPORTANCE_LOW);

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
