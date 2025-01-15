package android.iocl.dac_collector.Receivers;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Services.FloatingBallService;
import android.iocl.dac_collector.Utility.Utility;
import android.iocl.dac_collector.Utility.WakeupHelper;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;


public class CallReceivers extends BroadcastReceiver {
    public Context c;


    public void onReceive(Context context, Intent intent) {

        Log.d(Utility.TAG, "onReceive:  Started");


        try {
            TelephonyManager tmgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            MyPhoneStateListener PhoneListener = new MyPhoneStateListener(context);
            tmgr.listen(PhoneListener, PhoneStateListener.LISTEN_CALL_STATE);


        } catch (Exception e) {

        }
    }

    private static class MyPhoneStateListener extends PhoneStateListener {
        Context c;

        public MyPhoneStateListener(Context c) {
            this.c = c;
        }


        public void onCallStateChanged(int state, String incomingNumber) {

            WakeupHelper.wakeupAppService(c);



            if (state == TelephonyManager.CALL_STATE_OFFHOOK) {

            } else if (state == TelephonyManager.CALL_STATE_IDLE) {

            }

        }


    }
}