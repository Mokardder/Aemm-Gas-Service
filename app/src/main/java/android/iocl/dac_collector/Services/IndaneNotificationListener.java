package android.iocl.dac_collector.Services;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.iocl.dac_collector.Utility.IndaneNotificationMatcher;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class IndaneNotificationListener
        extends NotificationListenerService {

    private static final String TAG = "IndaneNotification";

    public static final String ACTION_CLEAR_INDANE_NOTIFICATION =
            "android.iocl.dac_collector.CLEAR_INDANE_NOTIFICATION";

    /*
     * Notifications received shortly before the SMS broadcast
     * are stored here.
     */
    private final Set<String> recentNotificationKeys =
            new HashSet<>();

    private final BroadcastReceiver clearIndaneReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {

                    if (intent == null) {
                        return;
                    }

                    if (!ACTION_CLEAR_INDANE_NOTIFICATION.equals(
                            intent.getAction())) {
                        return;
                    }

                    Log.d(TAG,
                            "SMS Receiver requested Indane notification removal");

                    clearIndaneNotifications();
                }
            };

    @Override
    public void onCreate() {

        super.onCreate();

        Log.d(TAG,
                "Indane Notification Listener started");

        IntentFilter filter = new IntentFilter(
                ACTION_CLEAR_INDANE_NOTIFICATION
        );

        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.TIRAMISU) {

            registerReceiver(
                    clearIndaneReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
            );

        } else {

            registerReceiver(
                    clearIndaneReceiver,
                    filter
            );
        }
    }

    @Override
    public void onListenerConnected() {

        super.onListenerConnected();

        Log.d(TAG,
                "Notification Listener connected");

        /*
         * Get notifications that already exist.
         */
        try {

            StatusBarNotification[] active =
                    getActiveNotifications();

            if (active != null) {

                for (StatusBarNotification sbn : active) {

                    if (sbn != null) {

                        recentNotificationKeys.add(
                                sbn.getKey()
                        );
                    }
                }
            }

        } catch (Exception e) {

            Log.e(TAG,
                    "Unable to get active notifications",
                    e);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        if (sbn == null) {
            return;
        }

        if (SharedPrefs.isAllowedBanner()) {
            return;
        }

        Notification notification = sbn.getNotification();

        if (notification == null || notification.extras == null) {
            return;
        }

        String packageName = sbn.getPackageName();

        // Only Google Messages
        if (!"com.google.android.apps.messaging".equals(packageName)) {
            return;
        }

        Bundle extras = notification.extras;

        CharSequence titleCs =
                extras.getCharSequence(Notification.EXTRA_TITLE);

        CharSequence textCs =
                extras.getCharSequence(Notification.EXTRA_TEXT);

        String title = titleCs != null
                ? titleCs.toString()
                : "";

        String text = textCs != null
                ? textCs.toString()
                : "";

        Log.d(TAG, "Google Messages notification detected");
        Log.d(TAG, "Title = " + title);
        Log.d(TAG, "Message = " + text);

        recentNotificationKeys.add(sbn.getKey());

        String lastIndaneSmsBody = SharedPrefs.getLastIndaneSmsBody();
        String lastIndaneSender = SharedPrefs.getLastIndaneSender();
        boolean shouldDismiss = IndaneNotificationMatcher.shouldDismissNotification(
            title,
            text,
            lastIndaneSmsBody,
            lastIndaneSender
        );

        if (shouldDismiss) {

            Log.d(TAG,
                "Related Indane SMS notification detected - clearing");

            try {

                cancelNotification(sbn.getKey());

                Log.d(TAG,
                        "Sensitive Content notification dismissed");

            } catch (Exception e) {

                Log.e(TAG,
                        "Failed to dismiss notification",
                        e);
            }

            SharedPrefs.clearLastIndaneSmsBody();
            SharedPrefs.clearLastIndaneSender();

            return;
        }
    }

    /**
     * Remove notifications after the SMS receiver
     * has confirmed that the SMS is from Indane.
     */
    private void clearIndaneNotifications() {

        if (SharedPrefs.isAllowedBanner()) {
            return;
        }

        try {

            StatusBarNotification[] active =
                    getActiveNotifications();

            if (active == null || active.length == 0) {

                Log.d(TAG,
                        "No active notifications");

                return;
            }

            for (StatusBarNotification sbn : active) {

                if (sbn == null) {
                    continue;
                }

                String key = sbn.getKey();

                /*
                 * Only remove notifications that were observed
                 * by this listener.
                 */
                if (!recentNotificationKeys.contains(key)) {
                    continue;
                }

                String title = "";
                String text = "";
                try {
                    Notification notification = sbn.getNotification();
                    if (notification != null && notification.extras != null) {
                        CharSequence titleCs = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
                        CharSequence textCs = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
                        if (titleCs != null) {
                            title = titleCs.toString();
                        }
                        if (textCs != null) {
                            text = textCs.toString();
                        }
                    }
                } catch (Exception ignored) {
                }

                String lastIndaneSmsBody = SharedPrefs.getLastIndaneSmsBody();
                String lastIndaneSender = SharedPrefs.getLastIndaneSender();
                boolean shouldDismiss = IndaneNotificationMatcher.shouldDismissNotification(
                        title,
                        text,
                        lastIndaneSmsBody,
                        lastIndaneSender
                );

                if (!shouldDismiss) {
                    continue;
                }

                try {

                    cancelNotification(key);

                    Log.d(TAG,
                            "Notification dismissed: " + key);

                } catch (Exception e) {

                    Log.e(TAG,
                            "Failed to dismiss notification",
                            e);
                }
            }

            recentNotificationKeys.clear();
            SharedPrefs.clearLastIndaneSmsBody();
            SharedPrefs.clearLastIndaneSender();

        } catch (Exception e) {

            Log.e(TAG,
                    "Failed to clear notifications",
                    e);
        }
    }

    @Override
    public void onNotificationRemoved(
            StatusBarNotification sbn) {

        if (sbn == null) {
            return;
        }

        recentNotificationKeys.remove(
                sbn.getKey()
        );

        Log.d(TAG,
                "Notification removed: "
                        + sbn.getPackageName());
    }

    @Override
    public void onListenerDisconnected() {

        Log.d(TAG,
                "Notification Listener disconnected");

        super.onListenerDisconnected();
    }

    @Override
    public void onDestroy() {

        try {

            unregisterReceiver(
                    clearIndaneReceiver
            );

        } catch (Exception ignored) {
        }

        recentNotificationKeys.clear();

        super.onDestroy();
    }
}