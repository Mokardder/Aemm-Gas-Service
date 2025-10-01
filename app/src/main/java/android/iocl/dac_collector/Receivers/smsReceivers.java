package android.iocl.dac_collector.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Services.SmsWorker;
import android.iocl.dac_collector.Utility.NotificationHelper;
import android.iocl.dac_collector.Utility.RoleHelper;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.work.Data;
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
            NotificationHelper.sendNotification(context, message.getOriginatingAddress(),  message.getMessageBody());
        }
        // Defer to WorkManager
        Data data = new Data.Builder()
                .putString("sender", message.getOriginatingAddress())
                .putString("body", message.getMessageBody())
                .build();
        WorkRequest request = new OneTimeWorkRequest.Builder(SmsWorker.class)
                .setInputData(data)
                .build();

        WorkManager.getInstance(context).enqueue(request);
    }
}