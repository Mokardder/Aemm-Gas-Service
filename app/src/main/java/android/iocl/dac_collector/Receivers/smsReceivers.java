package android.iocl.dac_collector.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.ModelData.RegexModel;
import android.iocl.dac_collector.Services.SmsSenderJOBService;
import android.iocl.dac_collector.Services.SmsWorker;
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

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

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


        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
        SmsMessage message = SmsMessage.createFromPdu((byte[]) pdus[0]);

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