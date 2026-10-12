package android.iocl.dac_collector.Firebase;

import android.content.Context;
import android.iocl.dac_collector.ModelData.dacPayload;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.SmsWorkUtil;
import android.iocl.dac_collector.Utility.Utility;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FirebaseDBClient {

    private static final String TAG = "FirebaseDBClient";
    private static final String PATH_SYNC = "DAC_SYNC";
    private static final String PATH_OFFLINE = "DAC_OFFLINE";

    private static FirebaseDatabase db;

    /**
     * Updates the app alive status in Firebase.
     */
    public static void updateAppAliveStatus() {
        String username = SharedPrefs.getConsumerId();

        db = FirebaseDatabase.getInstance();
        DatabaseReference statusRef = db.getReference("app_status");

        Map<String, Object> data = new HashMap<>();
        data.put("user", username);
        String time = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(new Date());
        data.put("time", time);

        statusRef.setValue(data)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Heartbeat acknowledged in Firebase"))
                .addOnFailureListener(error ->
                        Log.e(TAG, "Failed to publish app-alive status", error));

        statusRef.onDisconnect().removeValue()
                .addOnFailureListener(error ->
                        Log.e(TAG, "Failed to register app-alive disconnect cleanup", error));
    }
    public static void updateAppPermissions(List<String> permissions) {
        String username = SharedPrefs.getUsername() + " - " + SharedPrefs.getConsumerId();

        db = FirebaseDatabase.getInstance();
        DatabaseReference statusRef = db.getReference("app_perms");

        Map<String, Object> data = new HashMap<>();
        data.put("user", username);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < permissions.size(); i++) {
            sb.append(permissions.get(i));
            if (i < permissions.size() - 1) sb.append(", ");
        }
        String plainString = sb.toString();



        data.put("required_perms", plainString);
        String time = Utility.getStandardDatenTime();
        data.put("time", time);

        statusRef.setValue(data);
        statusRef.onDisconnect().removeValue();
    }

    /**
     * Sync DAC payload to Firebase "DAC_SYNC".
     */
    public static void syncDac(Context context, String dac, String cashmemo, String smsTime) {
        enqueueDacUpload(context, PATH_SYNC, dac, cashmemo, smsTime);

        if (dac != null && dac.equals(SharedPrefs.getUnsentDAC())) {
            Utility.clearUnsentDAC(context);
        }
    }

    /**
     * Add DAC payload to Firebase "DAC_OFFLINE".
     */
    public static void addToDb(Context context, String dac, String cashmemo, String smsReceivedTime) {
        enqueueDacUpload(context, PATH_OFFLINE, dac, cashmemo, smsReceivedTime);
    }

    private static void enqueueDacUpload(
            Context context,
            String path,
            String dac,
            String cashmemo,
            String smsTime
    ) {
        JSONObject upload = new JSONObject();
        String firebaseKey = UUID.randomUUID().toString().replace("-", "");

        try {
            upload.put("firebaseKey", firebaseKey);
            upload.put("path", path);
            upload.put("dac", dac);
            upload.put("cashmemo", cashmemo);
            upload.put("name", SharedPrefs.getUsername());
            upload.put("consID", SharedPrefs.getConsumerId());
            upload.put("timeStamp", smsTime);
        } catch (JSONException e) {
            Log.e(TAG, "Unable to create DAC upload record", e);
            throw new IllegalStateException("Unable to create DAC upload record", e);
        }

        SharedPrefs.enqueuePendingDacUpload(upload.toString());
        SmsWorkUtil.enqueueDacOutboxWorker(context);
    }

    public static void schedulePendingDacUploads(Context context) {
        if (SharedPrefs.hasPendingDacUploads()) {
            SmsWorkUtil.enqueueDacOutboxWorker(context);
        }
    }

    public static boolean uploadNextPendingDac() throws Exception {
        String serializedUpload = SharedPrefs.getNextPendingDacUpload();
        if (serializedUpload == null) {
            return false;
        }

        JSONObject upload = new JSONObject(serializedUpload);
        String firebaseKey = upload.getString("firebaseKey");
        String path = upload.getString("path");
        if (!PATH_SYNC.equals(path) && !PATH_OFFLINE.equals(path)) {
            throw new IllegalStateException("Invalid Firebase path in queued DAC upload");
        }

        dacPayload payload = new dacPayload(
                upload.optString("dac"),
                upload.optString("cashmemo"),
                upload.optString("name"),
                upload.optString("consID"),
                upload.optString("timeStamp")
        );

        DatabaseReference uploadRef = FirebaseDatabase.getInstance()
                .getReference(path)
                .child(firebaseKey);
        Tasks.await(uploadRef.setValue(payload), 30, TimeUnit.SECONDS);
        SharedPrefs.removePendingDacUpload(firebaseKey);
        return true;
    }

    public static void updateUploadStatus(String stats) {
        String username = SharedPrefs.getUsername() + " - " + SharedPrefs.getConsumerId();

        db = FirebaseDatabase.getInstance();
        DatabaseReference statsref = db.getReference("upload_stats");

        Map<String, Object> data = new HashMap<>();
        data.put("user", username);

        data.put("stats", stats);
        String time = Utility.getStandardDatenTime();
        data.put("time", time);

        statsref.setValue(data);
        statsref.onDisconnect().removeValue();
    }
}
