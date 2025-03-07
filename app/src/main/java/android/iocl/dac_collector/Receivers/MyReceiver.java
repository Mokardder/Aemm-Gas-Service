package android.iocl.dac_collector.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Services.MyWorker;
import android.iocl.dac_collector.Utility.WakeupHelper;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class MyReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {

            WakeupHelper.wakeupAppService(context);
            // Handle user unlock event (e.g., start a service, activity, or log)
            Log.d("UserUnlockReceiver", "Device unlocked!");
        }


    }
}
