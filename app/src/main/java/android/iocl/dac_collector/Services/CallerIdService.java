package android.iocl.dac_collector.Services;


import android.content.Context;
import android.database.Cursor;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Utility.SmsOtpPopup;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telecom.Call.Details;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class CallerIdService extends CallScreeningService {
    private static final String TAG = "CallerIdService";
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;

    @Override
    public void onCreate() {
        super.onCreate();

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);

                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE: // call ended
                        Log.d(TAG, "Call ended, dismissing popup");
                        SmsOtpPopup.with(getApplicationContext()).dismiss();
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK: // call picked
                        // could dismiss here if you only want popup while ringing
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        // already handled in onScreenCall()
                        break;
                }
            }
        };

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Override
    public void onScreenCall(Details callDetails) {

        /*
        if (callDetails == null) return;

        String phoneNumber = getPhoneNumber(callDetails);
        if (phoneNumber == null || phoneNumber.isEmpty()) return;

        Log.d(TAG, "onScreenCall: " + phoneNumber);
        String isVerified  = getVerifiedName(phoneNumber);

        Uri photoUri = getContactPhotoUri(getApplicationContext(), phoneNumber);

        Log.d(TAG, "onScreenCall: " + photoUri);

        if (isVerified != null){
            SmsOtpPopup.with(getApplicationContext())
                    .setImage(photoUri)
                    .setCustomHeader("Delivery Representative")
                    .enableVerified(true).show("Mokardder Hossain");
        }


         */
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private String getPhoneNumber(Details callDetails) {
        String number = callDetails.getHandle() != null ?
                callDetails.getHandle().getSchemeSpecificPart() : null;

        // Clean the phone number
        if (number != null) {
            return number.replaceAll("[^+0-9]", "");
        }
        return null;
    }

    private Uri getContactPhotoUri(Context context, String phoneNumber) {
        Uri photoUri = null;
        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
        );

        String[] projection = {
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.PHOTO_URI
        };

        try (Cursor cursor = context.getContentResolver().query(
                lookupUri,
                projection,
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                String photo = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI));
                if (photo != null) {
                    photoUri = Uri.parse(photo);
                }
            }
        }
        return photoUri;
    }




    public String getVerifiedName(String phoneNumber) {
        java.util.Map<String, String> verifiedNumbers = new java.util.HashMap<>();
        verifiedNumbers.put("9932896502", "son1");
        verifiedNumbers.put("9123386785", "son2");
        verifiedNumbers.put("9231902703", "father");

        for (String key : verifiedNumbers.keySet()) {
            if (phoneNumber.contains(key)) {  // check if phoneNumber contains the key
                return verifiedNumbers.get(key);
            }
        }
        return null; // not found
    }





}