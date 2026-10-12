package android.iocl.dac_collector.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Plugin.PluginDataBridge;
import android.iocl.dac_collector.Utility.IndaneNotificationMatcher;
import android.iocl.dac_collector.Utility.NotificationHelper;
import android.iocl.dac_collector.Utility.RoleHelper;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.SmsMessageStore;
import android.iocl.dac_collector.Utility.SmsWorkUtil;
import android.iocl.dac_collector.Utility.Utility;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

public class smsReceivers extends BroadcastReceiver {

    // ADD THIS
    public static final String ACTION_CLEAR_INDANE_NOTIFICATION =
            "android.iocl.dac_collector.CLEAR_INDANE_NOTIFICATION";
    private static final String TAG = "smsReceivers";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            Log.w(TAG, "Ignoring SMS broadcast with no action");
            return;
        }

        boolean isDeliver = Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction());
        boolean isReceived = Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction());
        if (!isDeliver && !isReceived) {
            Log.w(TAG, "Ignoring unsupported SMS action: " + intent.getAction());
            return;
        }

        boolean isDefaultSmsApp = RoleHelper.isDefault(context);
        if ((isDeliver && !isDefaultSmsApp) || (isReceived && isDefaultSmsApp)) {
            return;
        }

        SmsMessage[] messages;
        try {
            messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not decode incoming SMS broadcast", e);
            return;
        }
        if (messages == null || messages.length == 0 || messages[0] == null) {
            Log.w(TAG, "Incoming SMS broadcast contained no decodable message parts");
            return;
        }

        SmsMessage firstMessage = messages[0];
        String sender = firstMessage.getOriginatingAddress();
        StringBuilder bodyBuilder = new StringBuilder();
        for (SmsMessage message : messages) {
            if (message != null && message.getMessageBody() != null) {
                bodyBuilder.append(message.getMessageBody());
            }
        }
        String body = bodyBuilder.toString();
        if (sender == null || sender.trim().isEmpty() || body.isEmpty()) {
            Log.w(TAG, "Ignoring incoming SMS with an empty sender or body");
            return;
        }

        if (isDeliver) {
            SmsMessageStore.saveIncoming(context, sender, body, firstMessage.getTimestampMillis());
        }

        if (isDefaultSmsApp) {
            NotificationHelper.sendNotification(
                    context,
                    sender,
                    body,
                    null
            );
        }

        String dateMillis = Utility.getStandardDatenTime();


        boolean pluginHandled = PluginDataBridge.dispatchSmsReceived(
                context,
                sender,
                body,
                dateMillis
        );
        if (pluginHandled) {
            Log.d(TAG, "SMS handled by plugin");
            return;
        }


        // ============================================================
        // ADDITION ONLY
        // ============================================================

        if (body != null) {

            String lowerBody = body.toLowerCase();

            boolean isIndane =
                    lowerBody.contains("indane") ||
                            lowerBody.contains("indianoil") ||
                            lowerBody.contains("indian oil");

            if (isIndane) {

                Log.d(TAG, "Indane SMS detected");

                                SharedPrefs.setLastIndaneSmsBody(body);
                                SharedPrefs.setLastIndaneSender(sender);

                Intent clearIntent = new Intent(
                        ACTION_CLEAR_INDANE_NOTIFICATION
                );

                // Keep broadcast inside your application
                clearIntent.setPackage(
                        context.getPackageName()
                );

                context.sendBroadcast(clearIntent);

                Log.d(TAG,
                        "Clear Indane notification broadcast sent");
            }
        }

        // ============================================================
        // YOUR EXISTING CODE - UNCHANGED
        // ============================================================

        SmsWorkUtil.enqueueSmsWorker(
                context.getApplicationContext(),
                sender,
                body,
                dateMillis
        );

        /*
        // Defer to WorkManager
        Data data = new Data.Builder()
                .putString("sender", message.getOriginatingAddress())
                .putString("body", message.getMessageBody())
                .build();

        WorkRequest request = new OneTimeWorkRequest.Builder(SmsWorker.class)
                .setInputData(data)
                .build();

        String sender = message.getOriginatingAddress();
        String body = message.getMessageBody();

        // Stable unique key per SMS
        String uniqueWorkName =
                "SMS_" + sender + "_" + body.hashCode();

        WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.KEEP,
                (OneTimeWorkRequest) request
        );
        */
    }

}