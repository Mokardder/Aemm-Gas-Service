package android.iocl.dac_collector.Services;

import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Utility.NotificationHelper;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.SmsOtpPopup;
import android.iocl.dac_collector.Utility.Utility;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.util.Log;

import androidx.annotation.RequiresApi;


@RequiresApi(api = Build.VERSION_CODES.N)
public class CallerIdService extends CallScreeningService {
    private static final String TAG = "CallerIdService";
    private Handler dacNotifHandler;
    private Runnable dacNotifRunnable;
    private int notifyCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");

//        try {
//            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//            registerPhoneStateListener();
//        } catch (Exception e) {
//            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
//        }
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onScreenCall(Call.Details callDetails) {

        Log.d(TAG, "onScreenCall: " + callDetails);

        if (callDetails == null) {
            Log.w(TAG, "callDetails is null");
            return;
        }

        try {
            String phoneNumber = getPhoneNumber(callDetails);
            Log.d(TAG, "Incoming Phone Number: " + phoneNumber);

            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Log.w(TAG, "Phone number is null or empty");
                return;
            }

            String verifiedName = getVerifiedName(phoneNumber);
            Log.d(TAG, "Verified caller name: " + verifiedName);

            // *** MAIN NEW CONDITION HERE ***


            if (verifiedName != null) {
                showCallerIdPopup();   // only show popup for verified numbers
            }


        } catch (Exception e) {
            Log.e(TAG, "Error in onScreenCall: " + e.getMessage(), e);
        }
    }


    private void showCallerIdPopup() {

        String dac = Utility.getReturValidDAC(getApplicationContext());
        if (dac == null) return;
        try {
            Context context = getApplicationContext();



            Utility.sendSms("received_for_call", dac, SharedPrefs.getUsername(context), SharedPrefs.getUserID(context), context);

            FirebaseDBClient.addToDb(context, dac, "Received for call", Utility.getStandardDatenTime());
            if (Settings.canDrawOverlays(context)) {
                SmsOtpPopup.with(context)
                        .setCustomHeader("গ্যাসের OTP")
                        .enableVerified(true)
                        .setImage(R.drawable.gas_cylinder_icon)
                        .show(dac);
            } else {
                startDACNotificationLoop(context);

            }



        } catch (Exception e) {
            Log.e(TAG, "Error showing caller ID popup: " + e.getMessage(), e);
        }
    }

    private void startDACNotificationLoop(Context context) {

        notifyCount = 0;
        dacNotifHandler = new Handler(Looper.getMainLooper());

        dacNotifRunnable = new Runnable() {
            @Override
            public void run() {

                if (notifyCount >= 5) {
                    stopDACNotificationLoop();
                    return;
                }

                String dac = Utility.getReturValidDAC(context);
                if (dac != null) {
                    NotificationHelper.showDACNotification(context, dac);
                }

                notifyCount++;
                dacNotifHandler.postDelayed(this, 3000); // 3 seconds
            }
        };

        dacNotifHandler.post(dacNotifRunnable);
    }
    private void stopDACNotificationLoop() {
        if (dacNotifHandler != null && dacNotifRunnable != null) {
            dacNotifHandler.removeCallbacks(dacNotifRunnable);
        }
    }




    @RequiresApi(api = Build.VERSION_CODES.M)
    private String getPhoneNumber(Call.Details callDetails) {
        try {
            if (callDetails == null || callDetails.getHandle() == null) {
                return null;
            }

            String number = callDetails.getHandle().getSchemeSpecificPart();

            // Clean the phone number safely
            if (number != null) {
                return number.replaceAll("[^+0-9]", "");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting phone number: " + e.getMessage(), e);
        }
        return null;
    }

//    private Uri getContactPhotoUri(Context context, String phoneNumber) {
//        if (context == null || phoneNumber == null || phoneNumber.isEmpty()) {
//            return null;
//        }
//
//        Cursor cursor = null;
//        try {
//            Uri lookupUri = Uri.withAppendedPath(
//                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
//                    Uri.encode(phoneNumber)
//            );
//
//            String[] projection = {
//                    ContactsContract.PhoneLookup._ID,
//                    ContactsContract.PhoneLookup.PHOTO_URI
//            };
//
//            cursor = context.getContentResolver().query(
//                    lookupUri,
//                    projection,
//                    null,
//                    null,
//                    null
//            );
//
//            if (cursor != null && cursor.moveToFirst()) {
//                int photoUriIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI);
//                if (photoUriIndex != -1) {
//                    String photo = cursor.getString(photoUriIndex);
//                    if (photo != null) {
//                        return Uri.parse(photo);
//                    }
//                }
//            }
//        } catch (SecurityException e) {
//            Log.e(TAG, "Permission denied accessing contacts: " + e.getMessage(), e);
//        } catch (Exception e) {
//            Log.e(TAG, "Error getting contact photo URI: " + e.getMessage(), e);
//        } finally {
//            if (cursor != null) {
//                try {
//                    cursor.close();
//                } catch (Exception e) {
//                    Log.e(TAG, "Error closing cursor: " + e.getMessage(), e);
//                }
//            }
//        }
//        return null;
//    }

    public String getVerifiedName(String phoneNumber) {
        try {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                return null;
            }

            java.util.Map<String, String> verifiedNumbers = new java.util.HashMap<>();
            verifiedNumbers.put("9932896502", "son1");
            verifiedNumbers.put("9123386785", "son2");
            verifiedNumbers.put("9231902703", "father");
            verifiedNumbers.put("9153504979", "mother");

            for (String key : verifiedNumbers.keySet()) {
                if (phoneNumber.contains(key)) {
                    return verifiedNumbers.get(key);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getVerifiedName: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        return START_STICKY; // Service will be restarted if killed by system
    }
}