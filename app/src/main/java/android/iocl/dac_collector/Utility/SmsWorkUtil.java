package android.iocl.dac_collector.Utility;



import android.content.Context;
import android.iocl.dac_collector.Services.SmsWorker;
import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Locale;

public class SmsWorkUtil {
    private static final String TAG = "SmsWorkHelper";

    public static void enqueueSmsWorker(Context context, String sender, String body, String dateMillis) {
        try {
            if (sender == null) sender = "";
            if (body == null) body = "";

            // Normalize: trim, collapse whitespace, lowercase (to avoid tiny differences)
            String normSender = sender.trim();
            String normBody = body.trim().replaceAll("\\s+", " ");
            String key = normSender + "_" + normBody.hashCode();

            Log.d(TAG, "enqueueSmsWorker: KEY - " + key);

            Data data = new Data.Builder()
                    .putString("sender", normSender)
                    .putString("body", normBody)
                    .putString("date", dateMillis)
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SmsWorker.class)
                    .setInputData(data)
                    .build();

            Log.d(TAG, "enqueueUniqueWork key=" + key + " requestId=" + request.getId());
            WorkManager.getInstance(context).enqueueUniqueWork(
                    "SMS_" + key,
                    ExistingWorkPolicy.KEEP,
                    request
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to enqueue SMS worker", e);
        }
    }
}
