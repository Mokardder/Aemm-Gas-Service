package android.iocl.dac_collector.Utility;


import android.content.Context;
import android.iocl.dac_collector.Services.SmsWorker;
import android.iocl.dac_collector.Services.UploadWorker;
import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class SmsWorkUtil {
    private static final String TAG = "SmsWorkHelper";
    private static String LAST_KEY = "";

    public static void enqueueSmsWorker(Context context, String sender, String body, String dateMillis) {
        try {
            if (sender == null) sender = "";
            if (body == null) body = "";


            String normSender = sender.trim();
            String normBody = body.trim().replaceAll("\\s+", " ");
            String key = normSender + "_" + normBody.hashCode();


            Data data = new Data.Builder()
                    .putString("sender", normSender)
                    .putString("body", normBody)
                    .putString("date", dateMillis)
                    .build();


            if (normSender.equals("TEST-001")) {
                Utility.sendSms("TEST-001", "0000", SharedPrefs.getUsername(context), SharedPrefs.getUserID(context), context);
                return;
            }

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SmsWorker.class)
                    .setInputData(data)
                    .build();

            String workManagerKey = "SMS_" + key;


            Log.d(TAG, "last: " + LAST_KEY + "\n"
             + "current: " + workManagerKey);

            if (!LAST_KEY.isEmpty() && workManagerKey.equals(LAST_KEY)) {
                LAST_KEY = "";
                return;
            }


            WorkManager.getInstance(context).enqueueUniqueWork(
                    workManagerKey,
                    ExistingWorkPolicy.KEEP,
                    request
            );

            LAST_KEY = workManagerKey;
        } catch (Exception e) {
            Log.e(TAG, "Failed to enqueue SMS worker", e);
        }
    }
    public static void enqueueUploadWorker(Context context, String type) {
        try {

            String key = String.valueOf(System.currentTimeMillis());


            Data data = new Data.Builder()
                    .putString("type", type)
                    .build();



            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(UploadWorker.class)
                    .setInputData(data)
                    .build();

            String workManagerKey = "SMS_" + key;



            WorkManager.getInstance(context).enqueueUniqueWork(
                    workManagerKey,
                    ExistingWorkPolicy.KEEP,
                    request
            );

            LAST_KEY = workManagerKey;
        } catch (Exception e) {
            Log.e(TAG, "Failed to enqueue SMS worker", e);
        }
    }
}
