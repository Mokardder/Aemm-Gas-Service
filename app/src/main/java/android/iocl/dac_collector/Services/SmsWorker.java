package android.iocl.dac_collector.Services;

import android.content.Context;
import android.iocl.dac_collector.Utility.SmsOtpPopup;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.ModelData.RegexModel;
import android.iocl.dac_collector.Utility.DataSender;
import android.iocl.dac_collector.Utility.Utility;
import android.iocl.dac_collector.Utility.WakeupHelper;


public class SmsWorker extends Worker {
    private static final String TAG = "SmsWorker";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public SmsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        // Acquire wake lock
        PowerManager.WakeLock mainWakeLock = acquireWakeLock(context);
        try {
            String sender = getInputData().getString("sender");
            String body = getInputData().getString("body");
            Log.d(TAG, "Worker processing SMS from: " + sender + ", body: " + body);

            if (body == null || !body.toUpperCase().contains("INDANE")) {
                return Result.success();
            }

            List<RegexModel> details = Utility.checkDACRegex(body, context);
            String timestamp = Utility.getCurrentTime();
            if (details == null || details.isEmpty()) {
                Log.e(TAG, "No valid details extracted");
                return Result.success();
            }

            // Schedule fallback sender
            WakeupHelper.scheduleAlarm(context, SmsSenderJOBService.class);

            boolean isDAC = details.get(0).getCaptures().size() > 1;
            String dacValue = isDAC ? details.get(0).getCaptures().get(1) : details.get(0).getCaptures().get(0);
            String message = isDAC ? details.get(0).getCaptures().toString() : "Indian Oil OTP";

            // Process details on background thread
            executorService.execute(() -> handleDacProcessing(context, details, dacValue, message, timestamp));

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in SmsWorker", e);
            return Result.retry();
        } finally {
            if (mainWakeLock != null && mainWakeLock.isHeld()) {
                mainWakeLock.release();
            }
            executorService.shutdownNow();
        }
    }

    public void handleDacProcessing(Context context, List<RegexModel> details,
                                     String dac, String message, String timestamp) {
        PowerManager.WakeLock taskLock = acquireWakeLock(context);
        Utility.showDACNotification(context, dac);
        SmsOtpPopup.with(context).show(dac);
        try {
            FirebaseDBClient fireDb = new FirebaseDBClient(context);
            boolean hasInternet = Utility.isInternetAvailable(context);


            if (hasInternet) {
                if (details.get(0).getId().equals("DAC_SYNC")) {
                    fireDb.syncDac(dac, message, timestamp);
                } else {
                    fireDb.addToDb(dac, message, timestamp);
                }
            } else {
                handleOfflineScenario(context, dac, message);
            }
        } finally {
            if (taskLock != null && taskLock.isHeld()) {
                taskLock.release();
            }
        }
    }

    private void handleOfflineScenario(Context context, String dac, String message) {
        String userName = getStringPref("user_name", "not_found", context);
        Utility.sendSms(message, dac, userName, context);
        Utility.saveUnsentDAC(dac, context);
        DataSender.sendData(context, dac, message);
    }

    private PowerManager.WakeLock acquireWakeLock(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm == null) return null;

        PowerManager.WakeLock wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "DACCollector::ProcessingLock"
        );
        wakeLock.acquire(TimeUnit.MINUTES.toMillis(5));
        return wakeLock;
    }



    private String getStringPref(String key, String defaultValue, Context context) {
        return context.getSharedPreferences("AppsData", Context.MODE_PRIVATE)
                .getString(key, defaultValue);
    }
}