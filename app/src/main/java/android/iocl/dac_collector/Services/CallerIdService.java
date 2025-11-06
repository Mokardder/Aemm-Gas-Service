package android.iocl.dac_collector.Services;

import android.content.Context;
import android.content.Intent;
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
    private boolean isPhoneStateListenerRegistered = false;

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

    private void registerPhoneStateListener() {
        try {
            if (telephonyManager == null) {
                Log.w(TAG, "TelephonyManager is null, cannot register listener");
                return;
            }

            if (phoneStateListener == null) {
                phoneStateListener = new PhoneStateListener() {
                    @Override
                    public void onCallStateChanged(int state, String incomingNumber) {
                        super.onCallStateChanged(state, incomingNumber);
                        handleCallStateChange(state, incomingNumber);
                    }
                };
            }

            // Check if we have READ_PHONE_STATE permission
            if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {

                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                isPhoneStateListenerRegistered = true;
                Log.d(TAG, "PhoneStateListener registered successfully");
            } else {
                Log.w(TAG, "READ_PHONE_STATE permission not granted");
            }

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException registering PhoneStateListener: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Error registering PhoneStateListener: " + e.getMessage(), e);
        }
    }

    private void handleCallStateChange(int state, String incomingNumber) {
        Log.d(TAG, "Call state changed: " + state + ", number: " + incomingNumber);

        try {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    Log.d(TAG, "Call ended, dismissing popup");
                    dismissPopupSafely();
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d(TAG, "Call picked up");
                    // You can add call picked up logic here
                    break;

                case TelephonyManager.CALL_STATE_RINGING:
                    Log.d(TAG, "Call ringing: " + incomingNumber);
                    // Already handled in onScreenCall for Android 10+
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in handleCallStateChange: " + e.getMessage(), e);
        }
    }

    private void dismissPopupSafely() {
        try {
            // Uncomment if you want to use SmsOtpPopup
            // SmsOtpPopup.with(getApplicationContext()).dismiss();
        } catch (Exception e) {
            Log.e(TAG, "Error dismissing popup: " + e.getMessage(), e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onScreenCall(Call.Details callDetails) {
      /*  Log.d(TAG, "onScreenCall triggered");


        if (callDetails == null) {
            Log.w(TAG, "callDetails is null");
            return;
        }

        try {
            String phoneNumber = getPhoneNumber(callDetails);
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Log.w(TAG, "Phone number is null or empty");
                return;
            }


            // Uncomment and use this section when you're ready to show the popup

            String verifiedName = getVerifiedName(phoneNumber);
            Uri photoUri = getContactPhotoUri(getApplicationContext(), phoneNumber);

            Log.d(TAG, "Verified name: " + verifiedName + ", Photo URI: " + photoUri);

            if (verifiedName != null) {
                showCallerIdPopup(verifiedName, photoUri);
            }


            // Always respond to the call to avoid system issues
//            respondToCallSafely(callDetails);

        } catch (Exception e) {
            Log.e(TAG, "Error in onScreenCall: " + e.getMessage(), e);
//            respondToCallSafely(callDetails);
        }

       */
    }

    private void respondToCallSafely(Call.Details callDetails) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                CallResponse response = new CallResponse.Builder()
                        .setDisallowCall(false)
                        .setRejectCall(false)
                        .setSkipCallLog(false)
                        .setSkipNotification(false)
                        .build();
                respondToCall(callDetails, response);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error responding to call: " + e.getMessage(), e);
        }
    }

    private void showCallerIdPopup(String verifiedName, Uri photoUri) {
        try {
            // Uncomment when you're ready to use SmsOtpPopup
            /*
            SmsOtpPopup.with(getApplicationContext())
                    .setImage(photoUri)
                    .setCustomHeader("Delivery Representative")
                    .enableVerified(true)
                    .show(verifiedName);
            */
        } catch (Exception e) {
            Log.e(TAG, "Error showing caller ID popup: " + e.getMessage(), e);
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

    private Uri getContactPhotoUri(Context context, String phoneNumber) {
        if (context == null || phoneNumber == null || phoneNumber.isEmpty()) {
            return null;
        }

        Cursor cursor = null;
        try {
            Uri lookupUri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(phoneNumber)
            );

            String[] projection = {
                    ContactsContract.PhoneLookup._ID,
                    ContactsContract.PhoneLookup.PHOTO_URI
            };

            cursor = context.getContentResolver().query(
                    lookupUri,
                    projection,
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int photoUriIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI);
                if (photoUriIndex != -1) {
                    String photo = cursor.getString(photoUriIndex);
                    if (photo != null) {
                        return Uri.parse(photo);
                    }
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied accessing contacts: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact photo URI: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing cursor: " + e.getMessage(), e);
                }
            }
        }
        return null;
    }

    public String getVerifiedName(String phoneNumber) {
        try {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                return null;
            }

            java.util.Map<String, String> verifiedNumbers = new java.util.HashMap<>();
            verifiedNumbers.put("9932896502", "son1");
            verifiedNumbers.put("9123386785", "son2");
            verifiedNumbers.put("9231902703", "father");

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
//        unregisterPhoneStateListener();
        super.onDestroy();
    }

    private void unregisterPhoneStateListener() {
        try {
            if (telephonyManager != null && phoneStateListener != null && isPhoneStateListenerRegistered) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
                isPhoneStateListenerRegistered = false;
                Log.d(TAG, "PhoneStateListener unregistered");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering PhoneStateListener: " + e.getMessage(), e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        return START_STICKY; // Service will be restarted if killed by system
    }
}