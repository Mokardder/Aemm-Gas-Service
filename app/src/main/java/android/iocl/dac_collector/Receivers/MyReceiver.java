package android.iocl.dac_collector.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Services.MyWorker;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class MyReceiver extends BroadcastReceiver {
    private String TAG = "MyReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("MyReceiver", "BOOT_COMPLETED received!");
        } else if ("android.iocl.dac_collector.TEST_BOOT".equals(intent.getAction())) {
            Log.d("MyReceiver", "Test boot action received!");
        }
    }
}
