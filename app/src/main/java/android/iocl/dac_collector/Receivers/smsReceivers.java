package android.iocl.dac_collector.Receivers;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.iocl.dac_collector.Interface.onIOCLMessageReceived;
import android.iocl.dac_collector.ModelData.RegexModel;
import android.os.Build;
import android.os.Bundle;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.Utility.Utility;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class smsReceivers extends BroadcastReceiver {

    private static final String TAG = "smsReceivers";

    static onIOCLMessageReceived ioclMessage = null;

    @Override
    public void onReceive(Context context, Intent intent) {

        FirebaseDBClient fireDb = new FirebaseDBClient(context);


        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
                for (Object pdu : pdus) {
                    SmsMessage smsMessage = createSmsMessage(pdu, bundle);
                    String messageBody = smsMessage.getMessageBody();


                    if (messageBody.isEmpty()){
                        return;
                    }

                    if (!messageBody.toUpperCase().contains("INDANE")){
                        Log.d(TAG, "None of our business: ");
                        return;
                    }

                    List<RegexModel> details = Utility.checkDACRegex(messageBody, context);



                    if (details != null && details.size() > 0) {

                        boolean isDAC = details.get(0).getCaptures().size() > 1 ? true : false;
//                        boolean isDAC = details.get(0).getCaptures().size() > 1 ? details.get(0).getCaptures().get(1) : details.get(0).getCaptures().get(0)
                        String DAC = isDAC ?  details.get(0).getCaptures().get(1) :  details.get(0).getCaptures().get(0);

                        String message = isDAC ? details.get(0).getCaptures().get(0)  : "Indian Oil OTP";

//                        ioclMessage.onMesageReceived(DAC, message);

                        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.M) {
                            Utility.showDACNotification(context, DAC);
                        }
                        // Android version is greater than Marshmallow (6.0.x)



                        if (Utility.isConnectedToInternet(context)) {
                            if (!details.get(0).getId().equals("DAC_SYNC")){
                                fireDb.addToDb(DAC, message);
                                return;
                            }

                            fireDb.syncDac(DAC, message);
                        } else {
                            Utility.sendSms(message, DAC, getString("user_name", "not_found", context));
                        }
                    }  else {
                        Log.e(TAG, "Details array is empty or invalid");
                    }
                }
            }
        }
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


    String[] extractOtp(String message) {

        String[] patterns = {
                "Invoice Number # (\\d+-\\d+) is (\\d{4})",
                "Your IOCL one time password is :(\\d{4})"
        };


        for (int i = 0; i <= patterns.length - 1; i++)
            if (Pattern.compile(patterns[i]).matcher(message).find()) {
                Log.d("HE", "Found Text " + i + " Array");
            }



        // Improved regex patterns
        Pattern pattern_dac = Pattern.compile("Invoice Number # (\\d+-\\d+) is (\\d{4})");
        Pattern pattern_otp = Pattern.compile("Your IOCL one time password is :(\\d{4})");

        // Matchers for the patterns
        Matcher matcher_dac = pattern_dac.matcher(message);
        Matcher matcher_otp = pattern_otp.matcher(message);

        // Check for DAC pattern
        if (matcher_dac.find()) {
            String otp = matcher_dac.group(2);
            String cashmemo = matcher_dac.group(1);
            return new String[]{otp, cashmemo};
        }

        // Check for OTP pattern
        if (matcher_otp.find()) {
            String otp = matcher_otp.group(1);
            return new String[]{otp};
        }

        // Return null if no matches found
        return null;
    }

    private String getString(String key, String defaultValue, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AppsData", Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, defaultValue);
    }



    public static void getDACType(onIOCLMessageReceived listener)  {
        ioclMessage = listener;
    }


}