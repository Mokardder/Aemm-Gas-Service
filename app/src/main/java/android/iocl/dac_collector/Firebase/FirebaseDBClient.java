package android.iocl.dac_collector.Firebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.iocl.dac_collector.ModelData.dacPayload;
import android.iocl.dac_collector.Utility.Utility;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseDBClient {
    private static FirebaseDatabase db;
    private static DatabaseReference dbRef;
    private static String PATH_SYNC = "DAC_SYNC";
    private static String PATH_OFFLINE = "DAC_OFFLINE";
    private static String TAG = "Mokardder--->";

    private static Context mContext;

    public FirebaseDBClient(Context context) {
        mContext = context;
    }


    // Listener variable to hold the listener instance


    public static void syncDac(String dac, String cashmemo) {


        db = FirebaseDatabase.getInstance();

        dbRef = db.getReference(PATH_SYNC);
        dacPayload payload = new dacPayload(dac, cashmemo, getString("user_name", "not_found"), getString("cons_id", "not_found"), String.valueOf(System.currentTimeMillis()));

        dbRef.setValue(payload).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "DAC synchronized successfully");
            } else {
                Log.e(TAG, "DAC synchronization failed: " + task.getException());
            }
        });
    }

    public static void addToDb(String dac, String cashmemo) {

        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference(PATH_OFFLINE);
        dacPayload payload = new dacPayload(dac, cashmemo, getString("user_name", "not_found"), getString("cons_id", "not_found"), String.valueOf(System.currentTimeMillis()));

        dbRef.push().setValue(payload).addOnSuccessListener(unused -> {
                    Log.d(TAG, "Synced Offline ! ");
                })
                .addOnFailureListener(e -> {
                    Utility.sendSms(cashmemo, dac, getString("user_name", "not_found"));
                });
    }

    public static String getString(String key, String defaultValue) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("AppsData", Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, defaultValue);
    }
}