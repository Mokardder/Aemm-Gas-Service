package android.iocl.dac_collector.Firebase;

import android.iocl.dac_collector.ModelData.dacPayload;
import android.iocl.dac_collector.ModelData.dacPayload;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseDBClient {
    private static FirebaseDatabase db;
    private static DatabaseReference dbRef;
    private static String PATH_SYNC = "DAC_SYNC";
    private static String PATH_OFFLINE = "DAC_OFFLINE";
    private static String TAG = "Mokardder--->";

    // Listener variable to hold the listener instance


    public static void syncDac(String dac, String cashmemo) {

        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference(PATH_SYNC);
        dacPayload payload = new dacPayload(dac, cashmemo, String.valueOf(System.currentTimeMillis()));

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
        dacPayload payload = new dacPayload(dac, cashmemo, String.valueOf(System.currentTimeMillis()));

        dbRef.push().setValue(payload).addOnSuccessListener(unused -> {
            Log.d(TAG, "Synced Offline ! ");
        });
    }
}