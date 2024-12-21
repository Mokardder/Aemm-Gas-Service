package android.iocl.dac_collector.Utility;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.ModelData.RegexModel;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.MainActivity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility {

    public static final String TAG = "Utility_Mokardder";


    public static boolean isJobSchedulerActive(Context context, int jobId) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
                if (jobInfo.getId() == jobId) {
                    return true; // Job with this ID is active
                }
            }
        }
        return false; // Job is not active
    }
    public static void showDACNotification(Context context, String OTP) {
        if (context == null) {
            Log.e("NotificationError", "Context is null");
            return;
        }

        final int NOTIFY_ID = 1003;
        String DAC_CUSTOM_NOTIFY_ID = "dac_sms_notify";
        String DAC_CUSTOM_NOTIFY_NAME = "dac_sms_notify";
        String DAC_CUSTOM_NOTIFY_DESC = "DAC -> " + OTP;

        // NotificationManager
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            Log.e("NotificationError", "NotificationManager is null");
            return;
        }

        // Custom layout
        RemoteViews customLayout = new RemoteViews(context.getPackageName(), R.layout.dac_notification_bar);
        customLayout.setTextViewText(R.id.dac_val_eng, OTP);
        customLayout.setTextViewText(R.id.dac_val_beng, OTP);

        // Notification Channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = notificationManager.getNotificationChannel(DAC_CUSTOM_NOTIFY_ID);
            if (mChannel == null) {
                mChannel = new NotificationChannel(DAC_CUSTOM_NOTIFY_ID, DAC_CUSTOM_NOTIFY_NAME, NotificationManager.IMPORTANCE_HIGH);
                mChannel.setDescription(DAC_CUSTOM_NOTIFY_DESC);
                mChannel.enableVibration(true);
                notificationManager.createNotificationChannel(mChannel);
            }
        }

        // Notification Builder
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DAC_CUSTOM_NOTIFY_ID)
                .setSmallIcon(R.drawable.gas_cylinder_icon)
                .setCustomContentView(customLayout)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Show the notification
        Notification notification = builder.build();
        notificationManager.notify(NOTIFY_ID, notification);
    }


    public static boolean isConnectedToInternet(Context context) {

        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void getDACMessages(Context c) {
        FirebaseDBClient fireDb = new FirebaseDBClient(c);

        if (ContextCompat.checkSelfPermission(c, "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            ContentResolver contentResolver = c.getContentResolver();
            Uri uri = Uri.parse("content://sms/inbox");
            String[] projection = {"_id", "address", "body", "date", "read", "type"};
            String sortOrder = "date DESC";
            Cursor cursor = null;

            try {
                cursor = contentResolver.query(uri, projection, null, null, sortOrder);
                if (cursor != null && cursor.moveToFirst()) {
                    int count = 0;
                    do {
                        int bodyColumnIndex = cursor.getColumnIndex("body");
                        int dateColumnIndex = cursor.getColumnIndex("date");

                        if (bodyColumnIndex != -1 && dateColumnIndex != -1) {
                            Long time = cursor.getLong(dateColumnIndex);
                            long currentTime = System.currentTimeMillis();
                            long twentyFourHoursInMillis = 24 * 60 * 60 * 1000;
                            if (currentTime - time < twentyFourHoursInMillis) {
                                String body = cursor.getString(bodyColumnIndex);

                                List<RegexModel> details = checkDACRegex(body, c);


                                if (details != null && details.size() > 0) {

                                    boolean isDAC = details.get(0).getCaptures().size() > 1 ? true : false;
                                    String DAC = isDAC ? details.get(0).getCaptures().get(1) : details.get(0).getCaptures().get(0);

                                    String message = isDAC ? details.get(0).getCaptures().get(0) : "Indian Oil OTP";

                                    if (isDAC) {
                                        if (!details.get(0).getId().equals("DAC_SYNC")) {
                                            fireDb.addToDb(DAC, message);
                                            return;
                                        }
                                        fireDb.syncDac(DAC, message);
                                    }


                                }

                            }


                        }

                        count++;
                    } while (cursor.moveToNext() && count < 40);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

    }


    public static List<RegexModel> checkDACRegex(String message, Context c) {

        String jsonString = getMessagepattern(c);
        Log.d(TAG, message);

        JSONObject jsonObject;
        JSONArray patterns = null;
        try {
            jsonObject = new JSONObject(jsonString);
            patterns = jsonObject.getJSONArray("patterns");
        } catch (JSONException e) {
            Log.d(TAG, "checkDACRegex: JSONException occurred");
            return new ArrayList<>();
        }

        List<RegexModel> matches = new ArrayList<>();
        // Loop through patterns
        for (int i = 0; i < patterns.length(); i++) {
            JSONObject patternObject = null;
            String regex = null;
            String id = null;
            try {
                patternObject = patterns.getJSONObject(i);
                regex = patternObject.getString("regex");
                id = patternObject.getString("id");
            } catch (JSONException e) {
                Log.d(TAG, "checkDACRegex: JSONException occurred in pattern parsing");
            }

            // Compile and match regex
            if (regex != null && id != null) {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(message);

                if (matcher.find()) {
                    List<String> captures = new ArrayList<>();
                    for (int j = 1; j <= matcher.groupCount(); j++) {
                        captures.add((matcher.group(j)));
                    }

                    // Create RegexModel and add it to matches
                    RegexModel matchData = new RegexModel(regex, captures, id);
                    matches.add(matchData);
                }
            }
        }

        Log.d(TAG, matches.toString());
        return matches;
    }

    public static void sendSms(String Cashmemo, String DAC, String name) {

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String formattedDateTime = sdf.format(calendar.getTime());
        System.out.println("Current Date and Time: " + formattedDateTime);

        String msg = "CM - " + Cashmemo + " DAC - " + DAC + " Name " + name + " Time " + formattedDateTime;

        String phoneNumber = getPhoneNumber();

        SmsManager sms = SmsManager.getDefault();


        Log.d(TAG, "Sent to  -> " + phoneNumber);


        sms.sendTextMessage(phoneNumber, null, msg, null, null);

    }


    public static String getPhoneNumber() {

        String[] array = {"+919123386785", "+919932896502", "+919231902703"};

        // Create an instance of Random
        long timeStamp = System.currentTimeMillis();

        // Get the last digit of the timestamp
        int lastDigit = (int) (timeStamp % 10);

        // Use the last digit to generate an index within the range of the array
        int randomIndex = lastDigit % array.length;

        // Get the random value from the array
        String randomValue = array[randomIndex];

        return randomValue;
    }

    public static <T> Object decodeApiResponse(String base64String, Class<T> modelClass) {
        try {
            // Decode the Base64 string to a JSON string
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            String decodedJson = new String(decodedBytes);

            // Use Gson to check JSON type
            Gson gson = new Gson();
            JsonElement jsonElement = JsonParser.parseString(decodedJson);

            // If JSON is an array, return List<T>
            if (jsonElement.isJsonArray()) {
                Type listType = TypeToken.getParameterized(List.class, modelClass).getType();
                return gson.fromJson(jsonElement, listType);
            }

            // If JSON is an object, return T
            if (jsonElement.isJsonObject()) {
                return gson.fromJson(jsonElement, modelClass);
            }

            throw new IllegalStateException("Unexpected JSON type: " + jsonElement);
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Or handle error as needed
        }
    }

    // Assuming getMessagepattern is implemented elsewhere


    public static void updateMessagePattern(String message, Context c) {

        Log.d(TAG, "Updatedt Pattern");
        // Obtain the SharedPreferences object
        SharedPreferences sharedPreferences = c.getSharedPreferences("OTP_Pattern", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("pattern", message);
        editor.apply();

    }

    public static int getVersionCode(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String getVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void updateSenderNumbers(String message, Context c) {

        Log.d(TAG, "Updatedt Pattern");
        // Obtain the SharedPreferences object
        SharedPreferences sharedPreferences = c.getSharedPreferences("senders_number", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("numbers", message);
        editor.apply();

    }

    public static String getMessagepattern(Context c) {
        // Obtain the SharedPreferences object
        SharedPreferences sharedPreferences = c.getSharedPreferences("OTP_Pattern", Context.MODE_PRIVATE);
        return sharedPreferences.getString("pattern", "N");
    }

    public static String getSendersNumbers(Context c) {
        // Obtain the SharedPreferences object
        SharedPreferences sharedPreferences = c.getSharedPreferences("senders_number", Context.MODE_PRIVATE);
        return sharedPreferences.getString("numbers", "+919231902703");
    }

}
