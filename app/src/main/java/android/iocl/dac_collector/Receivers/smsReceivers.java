package android.iocl.dac_collector.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.ModelData.RegexModel;
import android.iocl.dac_collector.Services.SmsSenderJOBService;
import android.iocl.dac_collector.Utility.DataSender;
import android.iocl.dac_collector.Utility.Utility;
import android.iocl.dac_collector.Utility.WakeupHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.telephony.SmsMessage;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class smsReceivers extends BroadcastReceiver {

    private static final String TAG = "smsReceivers";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager.WakeLock mainWakeLock = acquireWakeLock(context);
        try {
            processIncomingSms(context, intent);
        } finally {
            if (mainWakeLock != null && mainWakeLock.isHeld()) {
                mainWakeLock.release();
            }
        }
    }

    private void processIncomingSms(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) return;

        String fullMessage = buildMessageFromPdus(context, bundle, pdus);
        if (!fullMessage.toUpperCase().contains("INDANE")) return;

        processValidMessage(context, fullMessage);
    }

    private String buildMessageFromPdus(Context context, Bundle bundle, Object[] pdus) {
        StringBuilder messageBuilder = new StringBuilder();
        for (Object pdu : pdus) {
            SmsMessage sms = createSmsMessage(pdu, context, bundle);
            messageBuilder.append(sms.getMessageBody());
        }
        return messageBuilder.toString();
    }

    private void processValidMessage(Context context, String fullMessage) {
        WakeupHelper.wakeupAppService(context);
        List<RegexModel> details = Utility.checkDACRegex(fullMessage, context);
        String timestamp = Utility.getCurrentTime();

        if (details == null || details.isEmpty()) {
            Log.e(TAG, "No valid details extracted");
            return;
        }

        processExtractedDetails(context, details, timestamp);
    }

    private void processExtractedDetails(Context context, List<RegexModel> details, String timestamp) {
        WakeupHelper.scheduleAlarm(context, SmsSenderJOBService.class);

        boolean isDAC = details.get(0).getCaptures().size() > 1;
        String dacValue = isDAC ? details.get(0).getCaptures().get(1) : details.get(0).getCaptures().get(0);
        String message = isDAC ? details.get(0).getCaptures().toString() : "Indian Oil OTP";


        executorService.execute(() -> handleDacProcessing(context, details, dacValue, message, timestamp));
    }

    private void handleDacProcessing(Context context, List<RegexModel> details,
                                     String dac, String message, String timestamp) {
        PowerManager.WakeLock taskLock = acquireWakeLock(context);
        Utility.showDACNotification(context, dac);
        try {
            FirebaseDBClient fireDb = new FirebaseDBClient(context);
            boolean hasInternet = isInternetAvailable(context);
            //Todo: Internet is Avaialble is not working properly

            Log.d(TAG, "handleDacProcessing: isInterner " + hasInternet);



            if (hasInternet) {
                updateFirebase(fireDb, details, dac, message, timestamp);
            } else {
                handleOfflineScenario(context, dac, message);
            }
        } finally {
            if (taskLock != null && taskLock.isHeld()) {
                taskLock.release();
            }
        }
    }

    private void updateFirebase(FirebaseDBClient fireDb, List<RegexModel> details,
                                String dac, String message, String timestamp) {
        if (details.get(0).getId().equals("DAC_SYNC")) {
            fireDb.syncDac(dac, message, timestamp);
        } else {
            fireDb.addToDb(dac, message, timestamp);
        }
    }

    private void handleOfflineScenario(Context context, String dac, String message) {
        String userName = getString("user_name", "not_found", context);
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
        wakeLock.acquire(TimeUnit.MINUTES.toMillis(5)); // 5-minute timeout
        return wakeLock;
    }

    private boolean isInternetAvailable(Context context) {
        if (!isNetworkConnected(context)) return false;
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Future<Boolean> check = executorService.submit(() -> {
            HttpURLConnection conn = null;
            try {
                // Use a reliable URL that responds to HEAD requests
                conn = (HttpURLConnection) new URL("https://www.google.com").openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setRequestMethod("HEAD");

                // Add User-Agent to mimic a browser request
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                int responseCode = conn.getResponseCode();

                Log.d("MainActivity_Mokardder", "isInternetAvailable: " + responseCode);

                // Accept 200 (OK) or 3xx (redirects) if following them
                return (responseCode == HttpURLConnection.HTTP_OK ||
                        (responseCode >= HttpURLConnection.HTTP_MULT_CHOICE &&
                                responseCode < HttpURLConnection.HTTP_BAD_REQUEST));
            } catch (Exception e) {
                Log.e("MainActivity_Mokardder", "Error checking internet", e); // Add logging
                return false;
            } finally {
                if (conn != null) conn.disconnect();
            }
        });

        try {
            return check.get(5, TimeUnit.SECONDS); // Increased timeout slightly
        } catch (TimeoutException e) {
            Log.e("MainActivity_Mokardder", "Timeout checking internet");
            check.cancel(true);
            return false;
        } catch (Exception e) {
            Log.e("MainActivity_Mokardder", "Exception in future task", e);
            return false;
        }
    }

    private boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm != null ? cm.getActiveNetworkInfo() : null;
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private SmsMessage createSmsMessage(Object pdu, Context context, Bundle bundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return SmsMessage.createFromPdu((byte[]) pdu, bundle.getString("format"));
        }
        return SmsMessage.createFromPdu((byte[]) pdu);
    }

    private String getString(String key, String defaultValue, Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppsData", Context.MODE_PRIVATE);
        return prefs.getString(key, defaultValue);
    }
}