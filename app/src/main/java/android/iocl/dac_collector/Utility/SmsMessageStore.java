package android.iocl.dac_collector.Utility;

import android.content.ContentValues;
import android.content.Context;
import android.provider.Telephony;
import android.util.Log;

public final class SmsMessageStore {
    private static final String TAG = "SmsMessageStore";

    private SmsMessageStore() {
    }

    public static boolean saveIncoming(Context context, String sender, String body, long timestamp) {
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.ADDRESS, sender);
        values.put(Telephony.Sms.BODY, body);
        values.put(Telephony.Sms.DATE, timestamp);
        values.put(Telephony.Sms.READ, 0);
        values.put(Telephony.Sms.SEEN, 0);
        values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);
        values.put(Telephony.Sms.CREATOR, context.getPackageName());
        return insert(context, Telephony.Sms.Inbox.CONTENT_URI, values);
    }

    public static boolean saveSent(Context context, String recipient, String body) {
        long timestamp = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.ADDRESS, recipient);
        values.put(Telephony.Sms.BODY, body);
        values.put(Telephony.Sms.DATE, timestamp);
        values.put(Telephony.Sms.DATE_SENT, timestamp);
        values.put(Telephony.Sms.READ, 1);
        values.put(Telephony.Sms.SEEN, 1);
        values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT);
        values.put(Telephony.Sms.CREATOR, context.getPackageName());
        return insert(context, Telephony.Sms.Sent.CONTENT_URI, values);
    }

    private static boolean insert(Context context, android.net.Uri uri, ContentValues values) {
        if (context == null) {
            Log.e(TAG, "Could not save SMS because context is null");
            return false;
        }

        try {
            if (context.getContentResolver().insert(uri, values) == null) {
                Log.e(TAG, "SMS provider returned no row for " + uri);
                return false;
            }
            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "SMS provider write denied; verify the app holds the default SMS role", e);
            return false;
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not save SMS to the system provider", e);
            return false;
        }
    }
}
