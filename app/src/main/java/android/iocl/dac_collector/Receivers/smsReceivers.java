package android.iocl.dac_collector.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Services.SmsWorker;
import android.iocl.dac_collector.Utility.NotificationHelper;
import android.iocl.dac_collector.Utility.RoleHelper;
import android.iocl.dac_collector.Utility.SmsWorkUtil;
import android.iocl.dac_collector.Utility.Utility;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class smsReceivers extends BroadcastReceiver {

    private static final String TAG = "smsReceivers";

    @Override
    public void onReceive(Context context, Intent intent) {


        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
        SmsMessage message = SmsMessage.createFromPdu((byte[]) pdus[0]);

        if (RoleHelper.isDefault(context)){
            NotificationHelper.sendNotification(context, message.getOriginatingAddress(),  message.getMessageBody(), null);
        }



        String sender = message.getOriginatingAddress();
        String body = message.getMessageBody();
        String dateMillis = Utility.getStandardDatenTime(); // stable timestamp from SMS

// old code removed. Use helper:
        SmsWorkUtil.enqueueSmsWorker(context.getApplicationContext(), sender, body, dateMillis);
        Log.d(TAG, "is Calling Twice ? :smsReceivers():onReceive");


      /*  // Defer to WorkManager
        Data data = new Data.Builder()
                .putString("sender", message.getOriginatingAddress())
                .putString("body", message.getMessageBody())
                .build();

        WorkRequest request = new OneTimeWorkRequest.Builder(SmsWorker.class)
                .setInputData(data)
                .build();

        String sender = message.getOriginatingAddress();
        String body = message.getMessageBody();

// Stable unique key per SMS
        String uniqueWorkName = "SMS_" + sender + "_" + body.hashCode();

        WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.KEEP,   // ignore duplicates
                (OneTimeWorkRequest) request
        );


       */


    }
}