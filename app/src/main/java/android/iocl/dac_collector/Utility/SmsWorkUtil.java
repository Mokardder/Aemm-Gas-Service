package android.iocl.dac_collector.Utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.iocl.dac_collector.Services.SmsWorker;
import android.iocl.dac_collector.Services.UploadWorker;
import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.HashSet;
import java.util.Set;

public class SmsWorkUtil {

    private static final String TAG = "SmsWorkHelper";

    // 🔒 DEDUPE STORAGE
    private static final String PREF = "sms_dedup_store";
    private static final String KEY_SET = "processed_keys";

    // ================== DEDUPE HELPERS ==================

    private static synchronized boolean isProcessed(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        Set<String> set = prefs.getStringSet(KEY_SET, new HashSet<>());
        return set.contains(key);
    }

    private static synchronized void markProcessed(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY_SET, new HashSet<>()));
        set.add(key);
        prefs.edit().putStringSet(KEY_SET, set).apply();
    }

    // ================== SMS WORK ==================

    public static void enqueueSmsWorker(Context context, String sender, String body, String dateMillis) {

        Log.d(TAG, "enqueueSmsWorker() called");

        try {
            if (sender == null) sender = "";
            if (body == null) body = "";

            String normSender = sender.trim();
            String normBody = body.trim().replaceAll("\\s+", " ");
            String hashKey = normSender + "_" + normBody.hashCode();

            // 🚫 HARD DUPLICATE BLOCK
            if (isProcessed(context, hashKey)) {
                Log.d(TAG, "Duplicate SMS ignored: " + hashKey);
                return;
            }

            if (normSender.equals("TEST-001")) {
                Utility.sendSms(
                        "TEST-001",
                        "0000",
                        SharedPrefs.getUsername(context),
                        SharedPrefs.getUserID(context),
                        context
                );
                return;
            }

            Data data = new Data.Builder()
                    .putString("sender", normSender)
                    .putString("body", normBody)
                    .putString("date", dateMillis)
                    .putString("hashKey", hashKey)
                    .build();

            OneTimeWorkRequest request =
                    new OneTimeWorkRequest.Builder(SmsWorker.class)
                            .setInputData(data)
                            .build();

            WorkManager.getInstance(context).enqueue(request);

            // ✅ Mark immediately to block re-entry
            markProcessed(context, hashKey);

        } catch (Exception e) {
            Log.e(TAG, "Failed to enqueue SMS worker", e);
        }
    }

    // ================== UPLOAD WORK ==================

    public static void enqueueUploadWorker(Context context, String type) {
        try {
            String key = String.valueOf(System.currentTimeMillis());

            Data data = new Data.Builder()
                    .putString("type", type)
                    .build();

            OneTimeWorkRequest request =
                    new OneTimeWorkRequest.Builder(UploadWorker.class)
                            .setInputData(data)
                            .build();

            WorkManager.getInstance(context).enqueue(request);

        } catch (Exception e) {
            Log.e(TAG, "Failed to enqueue Upload worker", e);
        }
    }
}
