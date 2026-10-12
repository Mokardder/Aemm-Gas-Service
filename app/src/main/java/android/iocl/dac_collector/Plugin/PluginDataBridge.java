package android.iocl.dac_collector.Plugin;

import android.content.Context;
import android.os.Bundle;

public final class PluginDataBridge {

    public static final String TARGET_SMS_RECEIVED = "sms_received";
    public static final String TARGET_ACTIVITY_RESUMED = "activity_resumed";
    public static final String KEY_SENDER = "sender";
    public static final String KEY_BODY = "body";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_ACTIVITY_NAME = "activity_name";

    private PluginDataBridge() {
    }

    public static Object dispatch(
            String target,
            Context context,
            Bundle data
    ) {
        return PluginDispatcher.dispatch(target, context, data);
    }

    public static Bundle newData() {
        return new Bundle();
    }

    public static boolean dispatchSmsReceived(
            Context context,
            String sender,
            String body,
            String timestamp
    ) {
        Bundle data = new Bundle();
        data.putString(KEY_SENDER, sender);
        data.putString(KEY_BODY, body);
        data.putString(KEY_TIMESTAMP, timestamp);

        return PluginDispatcher.dispatchBoolean(
                TARGET_SMS_RECEIVED,
                context,
                data
        );
    }

    public static void dispatchActivityResumed(
            Context context,
            String activityName
    ) {
        Bundle data = newData();
        data.putString(KEY_ACTIVITY_NAME, activityName);
        dispatch(TARGET_ACTIVITY_RESUMED, context, data);
    }
}