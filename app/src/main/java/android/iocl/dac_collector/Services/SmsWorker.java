package android.iocl.dac_collector.Services;

import android.content.Context;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.MainApplication.MyApp;
import android.iocl.dac_collector.ModelData.RegexModel;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Utility.DataSender;
import android.iocl.dac_collector.Utility.InternetCheckerSimple;
import android.iocl.dac_collector.Utility.NotificationHelper;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.SmsOtpPopup;
import android.iocl.dac_collector.Utility.Utility;
import android.iocl.dac_collector.Utility.WakeupHelper;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

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


        Context context = getApplicationContext();

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

            String s = sender.toLowerCase();

            if (!(s.contains("indane") ||
                    s.contains("9932896502") ||
                    s.contains("9123386785"))) {
                NotificationHelper.sendNotification(MyApp.getContext(), "Invalid Sender", "Your Recent Otp Matched, but not from allowed senders.", null);

                return Result.success();
            }

            WakeupHelper.scheduleAlarm(context, SmsSenderJOBService.class);


            RegexModel otpModel = details.get(0);

            String codeType = otpModel.getId();

            // Check kore hocche eta normal IndianOil Otp naki, DeliveryDAC
            boolean isOTP = codeType.equals("OTP");


            // Capturing full code value
            String fullCodeValue = otpModel.getCaptures().toString();
            String message = isOTP ? "Indian Oil OTP" : otpModel.getCaptures().toString();


            // Popup ba Notification bar e DAC show koranor jonno OTP extract kore hocche

            String onlyCode = "";


            Log.d(TAG, "CodeType : " + codeType + " Code Value : " + fullCodeValue + " Message : " + message);


            switch (codeType) {
                case "OTP":
                    onlyCode = otpModel.getCaptures().get(0);
                    break;
                case "DAC":
                    onlyCode = otpModel.getCaptures().get(1);
                    break;
                case "GeneratedDAC":
                    onlyCode = otpModel.getCaptures().get(1);
                    break;
                default:
                    // code block
            }

            String finalOnlyCode = onlyCode;
            executorService.execute(() -> handleDacProcessing(context, otpModel, finalOnlyCode, message, timestamp, codeType));

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

    public void handleDacProcessing(Context context, RegexModel otpModel,
                                    String dac, String message, String timestamp, String codeType) {
        PowerManager.WakeLock taskLock = acquireWakeLock(context);

        try {

            // Kichu kichu android version e RemoteViews support korena tar karone, Android version check kore hocche



            // Jodi Truecaller install thake tahole 3 second delay diye PopUp show hobe,
            // nahole Truecaller er popup ei app er upore show koriye dei
            String headerText;

            switch (codeType) {
                case "OTP":
                    headerText = "General OTP";
                    break;

                case "DAC":
                    headerText = "গ্যাসের কোড (DAC)";
                    break;

                case "GeneratedDAC":
                    headerText = "Generated DAC";
                    break;

                default:
                    headerText = "কোড";
                    break;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                NotificationHelper.showDACNotification(context, dac, headerText);   // RemoteViews version
            } else {
                NotificationHelper.showOtpNotification(context, dac);   // Normal notification
            }

            if (Utility.isTruecallerInstalled(context)) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                SmsOtpPopup.with(context)
                        .setImage(R.drawable.gas_cylinder_icon)
                        .enableVerified(false)
                        .setCustomHeader(headerText)
                        .show(dac);

            } else {

                SmsOtpPopup.with(context)
                        .setImage(R.drawable.gas_cylinder_icon)
                        .enableVerified(false)
                        .setCustomHeader(headerText)
                        .show(dac);
            }


            new InternetCheckerSimple(context).check((isConnected, reason) -> {

                if (isConnected) {
                    FirebaseDBClient.addToDb(context, dac, message, timestamp);

                } else {
                    handleOfflineScenario(context, dac, message, codeType);
                }

            });


        } finally {
            if (taskLock != null && taskLock.isHeld()) {
                taskLock.release();
            }
        }
    }

    private void handleOfflineScenario(Context context, String dac, String message, String codetype) {
        String userName = SharedPrefs.getUsername();
        String consumerID = SharedPrefs.getConsumerId();
        Utility.sendSms(message, dac, userName, consumerID, context, codetype);
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