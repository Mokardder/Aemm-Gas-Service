package android.iocl.dac_collector.Firebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.iocl.dac_collector.ModelData.dacPayload;
import android.iocl.dac_collector.Utility.SharedPrefs;
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

    static String username = "";
   static String consumerID = "";

    public FirebaseDBClient(Context context) {
        mContext = context;
        this.username = SharedPrefs.getUsername(context);
        this.consumerID = SharedPrefs.getConsumerId(context);
    }


    public static void syncDac(String dac, String cashmemo, String smsTime) {


        db = FirebaseDatabase.getInstance();

        dbRef = db.getReference(PATH_SYNC);

        dacPayload payload = new dacPayload(dac, cashmemo, username, consumerID, smsTime);

//        isAlreadyAvailable(isAvailable -> {
//            if (!isAvailable) {
                dbRef.push().setValue(payload).addOnSuccessListener(unused -> {
                        })
                        .addOnFailureListener(e -> {
//                        });
//            }
            Utility.clearUnsentDAC(mContext);

        });

    }

    public static void addToDb(String dac, String cashmemo, String SmsReceivedTime) {

        String cons_id = consumerID;
        String name = username;

        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference(PATH_OFFLINE);
        dacPayload payload = new dacPayload(dac, cashmemo, name, cons_id, SmsReceivedTime);

//        isAlreadyAvailable(isAvailable -> {
//            if (!isAvailable) {
                dbRef.push().setValue(payload).addOnSuccessListener(unused -> {
                        })
                        .addOnFailureListener(e -> {
                        });
//            }
//        });


    }




}