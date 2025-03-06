package android.iocl.dac_collector.Services;


import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.DialogActivity;
import android.iocl.dac_collector.Ui.MainActivity;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.WakeupHelper;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.app.NotificationCompat;

import java.util.List;

public class AcessibilitySettings extends AccessibilityService {

    public static final String CUSTOM_ACTION = "com.example.mybroadcastapp.CUSTOM_ACTION";


    private static final String CHANNEL_ID = "AccessibilitySettingsID";
    private static final String TAG = "AccessibilitySettingsID";
    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (CUSTOM_ACTION.equals(intent.getAction())) {
                clickBackButton();
                // Handle the data as needed...
            }
        }
    };


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        SharedPrefs sharedPrefs = new SharedPrefs(getApplicationContext());

        boolean isRestrictionEnabled = sharedPrefs.getRestrictionEnabled();


        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            String className = event.getClassName() != null ? event.getClassName().toString() : "";
            String Text = !event.getText().isEmpty() ? event.getText().toString() : "";

            Log.d(TAG, "Event -> " + event);

            Intent startDialog = new Intent(getApplicationContext(), DialogActivity.class);
            startDialog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startDialog.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startDialog.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            String appname = getString(R.string.app_name);

            if (isRestrictionEnabled) {
                if (Text.contains(appname) || Text.toLowerCase().contains("accessibility")) {
                    if (!Text.toLowerCase().contains("notification")) {

                        startActivity(startDialog);
                    }

                } else if (Text.contains("Force stop")) {

                    startActivity(startDialog);
                } else if (className.toLowerCase().contains("admin")) {

                    startActivity(startDialog);
                }
            }


        }

    }


    public void clickBackButton() {
        performGlobalAction(GLOBAL_ACTION_BACK); // Simulate Back button press
    }


    @Override
    public void onInterrupt() {

        createNotificationChannel();
        sendTimeNotification("Bhosdiwala", getApplicationContext());
        unregisterReceiver(receiver);
    }


    @Override
    protected void onServiceConnected() {
        WakeupHelper.wakeupAppService(getApplicationContext());

        createNotificationChannel();
        sendTimeNotification("Baraaa", getApplicationContext());

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.notificationTimeout = 100;
        info.packageNames = null;
        setServiceInfo(info);
        IntentFilter filter = new IntentFilter(CUSTOM_ACTION);
        registerReceiver(receiver, filter);
        Log.d(TAG, "BroadcastReceiver registered");
    }


    private void extractTextFromNode(AccessibilityNodeInfo node, List<String> texts) {
        if (node == null) return;

        // Get text from the current node
        CharSequence text = node.getText();
        if (text != null) {
            texts.add(text.toString());
        }

        // Recursively process child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            extractTextFromNode(node.getChild(i), texts);
        }
    }

    public static Notification sendTimeNotification(String messageBody, Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        String channelId = CHANNEL_ID;
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.clock_time)
                .setContentTitle("Accessibility")
                .setContentText("Current Time -> " + messageBody)
                .setAutoCancel(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setSilent(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Hate Yut Bitchhh", NotificationManager.IMPORTANCE_DEFAULT);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Return the built notification
        return notificationBuilder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Context context = getApplicationContext(); // Ensure the context is valid
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (manager == null) {
                Log.e("FixOppoAutoKill", "NotificationManager is null. Delaying channel creation.");
                new Handler().postDelayed(this::createNotificationChannel, 60 * 1000); // Retry after 1 second
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


}