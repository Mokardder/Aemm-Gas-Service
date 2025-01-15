package android.iocl.dac_collector.Receivers;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.Interface.onIOCLMessageReceived;
import android.iocl.dac_collector.ModelData.RegexModel;
import android.iocl.dac_collector.Services.FloatingBallService;
import android.iocl.dac_collector.Services.JobSchedulerUtil;
import android.iocl.dac_collector.Services.SmsSenderJOBService;
import android.iocl.dac_collector.Utility.Utility;
import android.iocl.dac_collector.Utility.WakeupHelper;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class smsReceivers extends BroadcastReceiver {

    private static final String TAG = "smsReceivers";

    static onIOCLMessageReceived ioclMessage = null;

    @Override
    public void onReceive(Context context, Intent intent) {

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MyApp::PhoneWakeLock");
        wakeLock.acquire();

        FirebaseDBClient fireDb = new FirebaseDBClient(context);


        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
                for (Object pdu : pdus) {
                    SmsMessage smsMessage = createSmsMessage(pdu, bundle);
                    String messageBody = smsMessage.getMessageBody();


                    WakeupHelper.wakeupAppService(context);




                    if (messageBody.isEmpty()) {
                        return;
                    }

                    if (!messageBody.toUpperCase().contains("INDANE")) {
                        Log.d(TAG, "None of our business: ");
                        return;
                    }

                    List<RegexModel> details = Utility.checkDACRegex(messageBody, context);

                    String currentTime = Utility.getCurrentTime();


                    if (details != null && details.size() > 0) {

                        WakeupHelper.scheduleAlarm(context, SmsSenderJOBService.class);

                        if (!Utility.isJobSchedulerActive(context, JobSchedulerUtil.SMS_CALL_ID)) {
                            JobSchedulerUtil.Sms_and_Call_sender(context);
                            JobSchedulerUtil.fetch_profile_info(context);
                        }


                        boolean isDAC = details.get(0).getCaptures().size() > 1 ? true : false;
//                        boolean isDAC = details.get(0).getCaptures().size() > 1 ? details.get(0).getCaptures().get(1) : details.get(0).getCaptures().get(0)
                        String DAC = isDAC ? details.get(0).getCaptures().get(1) : details.get(0).getCaptures().get(0);

                        String message = isDAC ? details.get(0).getCaptures().toString()  : "Indian Oil OTP";
                            Utility.showDACNotification(context, DAC);

                        if (Utility.isConnectedToInternet(context)) {
                            if (!details.get(0).getId().equals("DAC_SYNC")) {
                                fireDb.addToDb(DAC, message, currentTime);
                                return;
                            }
                            fireDb.syncDac(DAC, message, currentTime);

                        } else {
                            Utility.saveUnsentDAC(DAC, context);
                            Utility.sendSms(message, DAC, getString("user_name", "not_found", context));
                        }
                    } else {
                        Log.e(TAG, "Details array is empty or invalid");
                    }
                }
            }
        }
        wakeLock.release();
    }

    private SmsMessage createSmsMessage(Object pdu, Bundle bundle) {
        SmsMessage smsMessage;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String format = bundle.getString("format");
            smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
        } else {
            smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
        }
        return smsMessage;
    }


    private String getString(String key, String defaultValue, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AppsData", Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, defaultValue);
    }





}