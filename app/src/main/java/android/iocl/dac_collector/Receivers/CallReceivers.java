package android.iocl.dac_collector.Receivers;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Ui.MainActivity;
import android.iocl.dac_collector.Utility.WakeupHelper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;


public class CallReceivers extends BroadcastReceiver {
    public Context c;

    String TAG = "CallReceiver";


    public void onReceive(Context context, Intent intent) {


        /*
        Log.d(TAG, "onReceive:  Started");



        try {
            TelephonyManager tmgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            MyPhoneStateListener PhoneListener = new MyPhoneStateListener(context);
            tmgr.listen(PhoneListener, PhoneStateListener.LISTEN_CALL_STATE);


        } catch (Exception e) {

        }

         */
    }

    /*
    private static class MyPhoneStateListener extends PhoneStateListener {
        Context c;

        public MyPhoneStateListener(Context c) {
            this.c = c;
        }


        public void onCallStateChanged(int state, String incomingNumber) {
            String TAG = "CallReceiver";

            WakeupHelper.wakeupAppService(c);

            Log.d(TAG, "onCallStateChanged: " + incomingNumber);

            if (incomingNumber.equals("*#786786*#")){
                Intent activity = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                c.startActivity(activity);
            }



            if (state == TelephonyManager.CALL_STATE_OFFHOOK) {

            } else if (state == TelephonyManager.CALL_STATE_IDLE) {

            }

        }


    }

     */
}