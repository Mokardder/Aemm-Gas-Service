package android.iocl.dac_collector.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Utility.WakeupHelper;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {

            WakeupHelper.wakeupAppService(context);
            // Handle user unlock event (e.g., start a service, activity, or log)
            Log.d("UserUnlockReceiver", "Device unlocked!");


        }
        if (intent == null) return;
        String action = intent.getAction();
//        if (PersistentVpnService.ACTION_RESTART.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
//            PersistentVpnServiceUtil.startService(context);
//        }


    }
}
