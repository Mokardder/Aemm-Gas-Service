package android.iocl.dac_collector.Firebase;

import android.content.Context;
import android.iocl.dac_collector.ModelData.dacPayload;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.Utility;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FirebaseDBClient {

    private static final String PATH_SYNC = "DAC_SYNC";
    private static final String PATH_OFFLINE = "DAC_OFFLINE";

    private static FirebaseDatabase db;

    /**
     * Updates the app alive status in Firebase.
     */
    public static void updateAppAliveStatus(Context context) {
        String username = SharedPrefs.getConsumerId(context);

        db = FirebaseDatabase.getInstance();
        DatabaseReference statusRef = db.getReference("app_status");

        Map<String, Object> data = new HashMap<>();
        data.put("user", username);
        String time = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(new Date());
        data.put("time", time);

        statusRef.setValue(data);
        statusRef.onDisconnect().removeValue();
    }

    /**
     * Sync DAC payload to Firebase "DAC_SYNC".
     */
    public static void syncDac(Context context, String dac, String cashmemo, String smsTime) {
        String username = SharedPrefs.getUsername(context);
        String consumerID = SharedPrefs.getConsumerId(context);

        db = FirebaseDatabase.getInstance();
        DatabaseReference dbRef = db.getReference(PATH_SYNC);

        dacPayload payload = new dacPayload(dac, cashmemo, username, consumerID, smsTime);

        dbRef.push().setValue(payload)
                .addOnSuccessListener(unused -> {
                    // success
                })
                .addOnFailureListener(e -> {
                    // failure
                });

        Utility.clearUnsentDAC(context);
    }

    /**
     * Add DAC payload to Firebase "DAC_OFFLINE".
     */
    public static void addToDb(Context context, String dac, String cashmemo, String smsReceivedTime) {
        String username = SharedPrefs.getUsername(context);
        String consumerID = SharedPrefs.getConsumerId(context);

        db = FirebaseDatabase.getInstance();
        DatabaseReference dbRef = db.getReference(PATH_OFFLINE);

        dacPayload payload = new dacPayload(dac, cashmemo, username, consumerID, smsReceivedTime);

        dbRef.push().setValue(payload)
                .addOnSuccessListener(unused -> {
                    // success
                })
                .addOnFailureListener(e -> {
                    // failure
                });
    }
}
