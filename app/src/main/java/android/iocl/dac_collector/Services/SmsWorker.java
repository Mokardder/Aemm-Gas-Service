package android.iocl.dac_collector.Services;

import android.content.Context;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.ModelData.RegexModel;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Utility.DataSender;
import android.iocl.dac_collector.Utility.InternetCheckerSimple;
import android.iocl.dac_collector.Utility.NotificationHelper;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.SmsOtpPopup;
import android.iocl.dac_collector.Utility.Utility;
import android.iocl.dac_collector.Utility.WakeupHelper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class SmsWorker extends Worker {
    private static final String TAG = "SmsWorker";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public SmsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        Log.d(TAG, "is Calling Twice ? :doWork()");

        Context context = getApplicationContext();
        // Acquire wake lock
        PowerManager.WakeLock mainWakeLock = acquireWakeLock(context);
        try {
            String sender = getInputData().getString("sender");
            String body = getInputData().getString("body");






            List<RegexModel> details = Utility.checkDACRegex(body, context);
            String timestamp = Utility.getCurrentTime();
            Log.d(TAG, "checkDACRegex: " + details);
            if (details == null || details.isEmpty()) {

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

            return Result.retry();
        } finally {
            if (mainWakeLock != null && mainWakeLock.isHeld()) {
                mainWakeLock.release();
            }
            executorService.shutdownNow();
        }
    }

    public void handleDacProcessing(Context context, List<RegexModel> detailsdetails,
                                    String dac, String message, String timestamp) {
        PowerManager.WakeLock taskLock = acquireWakeLock(context);


        Log.d(TAG, "is Calling Twice ?");





        try {
            NotificationHelper.showOtpNotification(context, dac);
            NotificationHelper.showDACNotification(context, dac);

            SmsOtpPopup.with(context).setImage(R.drawable.gas_cylinder_icon)
                    .enableVerified(false).setCustomHeader("গ্যাসের কোড (DAC)").show(dac);

            new InternetCheckerSimple(context).check((isConnected, reason) -> {

                Log.d(TAG, "handleDacProcessing: " + isConnected);
                if (isConnected) {

                    FirebaseDBClient.addToDb(context, dac, message, timestamp);

                } else {
                    Log.d(TAG, "handleDacProcessing: Handling Offline ");
                    handleOfflineScenario(context, dac, message);
                }

            });


        } finally {
            if (taskLock != null && taskLock.isHeld()) {
                taskLock.release();
            }
        }
    }

    private void handleOfflineScenario(Context context, String dac, String message) {
        String userName = SharedPrefs.getUsername(context);
        String consumerID = SharedPrefs.getUserID(context);
        Utility.sendSms(message, dac, userName, consumerID, context);
        Utility.saveUnsentDAC(dac, context);
        DataSender.sendData(context);
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



}