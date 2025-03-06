package android.iocl.dac_collector.Firebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.iocl.dac_collector.ModelData.dacPayload;
import android.iocl.dac_collector.Utility.Utility;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

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


    public static void syncDac(String dac, String cashmemo, String smsTime) {


        db = FirebaseDatabase.getInstance();

        dbRef = db.getReference(PATH_SYNC);

        dacPayload payload = new dacPayload(dac, cashmemo, getString("user_name", "not_found"), getString("cons_id", "not_found"), smsTime);

        isAlreadyAvailable(isAvailable -> {
            if (!isAvailable) {
                dbRef.push().setValue(payload).addOnSuccessListener(unused -> {
                        })
                        .addOnFailureListener(e -> {
                        });
            }
            Utility.clearUnsentDAC(mContext);

        });

    }

    public static void addToDb(String dac, String cashmemo, String SmsReceivedTime) {

        String cons_id = getString("cons_id", "not_found");
        String name = getString("user_name", "not_found");

        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference(PATH_OFFLINE);
        dacPayload payload = new dacPayload(dac, cashmemo, name, cons_id, SmsReceivedTime);

        isAlreadyAvailable(isAvailable -> {
            if (!isAvailable) {
                dbRef.push().setValue(payload).addOnSuccessListener(unused -> {
                        })
                        .addOnFailureListener(e -> {
                        });
            }
        });


    }

    public interface isExistinDB {
        void onResult(boolean isAvailable);
    }


    public static void isAlreadyAvailable(isExistinDB callback) {
        String consumerID = getString("cons_id", "not_found");
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(PATH_OFFLINE);
        Query query = dbRef.orderByChild("consID").equalTo(consumerID);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean isAvailable = dataSnapshot.exists();
                Log.d(TAG, "onDataChange: isAvailable ? " + isAvailable);
                callback.onResult(isAvailable); // Pass result to callback
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                callback.onResult(false); // Pass false on error
            }
        });
    }


    public static String getString(String key, String defaultValue) {
        if (mContext != null) {
            SharedPreferences sharedPreferences = mContext.getSharedPreferences("AppsData", Context.MODE_PRIVATE);
            return sharedPreferences.getString(key, defaultValue);
        }
        return "";
    }
}