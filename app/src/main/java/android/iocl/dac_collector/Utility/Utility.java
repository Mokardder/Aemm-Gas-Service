package android.iocl.dac_collector.Utility;


import android.annotation.SuppressLint;
import android.app.ActivityManager;
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
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.ModelData.RegexModel;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Services.AcessibilitySettings;
import android.iocl.dac_collector.Services.FixOppoAutoKill;
import android.iocl.dac_collector.Ui.MainActivity;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility {

    public static final String TAG = "Utility_Mokardder";
    public static final int DEFAULT_SUBSCRIPTION_ID = 1;


    public static String getConsID(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AppsData", Context.MODE_PRIVATE);
        return sharedPreferences.getString("cons_id", "N");
    }

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

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            sendNotification(OTP, context);
            return;
        }
        final int NOTIFY_ID = 10;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            customLayout.setViewVisibility(R.id.call_Aemm, View.VISIBLE);
            customLayout.setOnClickResponse(R.id.call_Aemm, makeCall("+919231902703", context));

        } else {
            customLayout.setViewVisibility(R.id.call_Aemm, View.GONE);
        }
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
                .setCustomBigContentView(customLayout)
                .setCustomHeadsUpContentView(customLayout)

                .setColorized(true)
                .setColor(Color.WHITE)
                .setContentIntent(pendingIntent)

                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);


        // Show the notification
        Notification notification = builder.build();
        notificationManager.notify(NOTIFY_ID, notification);
    }

    public static void startForegroundService(Context context) {
        Intent serviceIntent = new Intent(context, FixOppoAutoKill.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
           context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    public static void showRechargeNotification(Context context, String OTP) {
        if (context == null) {
            Log.e("NotificationError", "Context is null");
            return;
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            sendNotification(OTP, context);
            return;
        }

        final int NOTIFY_ID = 1004;
        String DAC_CUSTOM_NOTIFY_ID = "recharge_sms_notify";
        String DAC_CUSTOM_NOTIFY_NAME = "recharge_sms_notify";
        String DAC_CUSTOM_NOTIFY_DESC = "DAC -> " + OTP;

        // NotificationManager
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            Log.e("NotificationError", "NotificationManager is null");
            return;
        }


        // Custom layout
        RemoteViews customLayout = new RemoteViews(context.getPackageName(), R.layout.annual_end_notificationbar);
        customLayout.setTextViewText(R.id.tvTime, OTP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            customLayout.setViewVisibility(R.id.call_Aemm, View.VISIBLE);
            customLayout.setOnClickResponse(R.id.call_Aemm, makeCall("+919932896502", context));

        } else {
            customLayout.setViewVisibility(R.id.call_Aemm, View.GONE);

        }


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
                .setColor(Color.parseColor("#14A44D"))
                .setColorized(true)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Show the notification
        Notification notification = builder.build();
        notificationManager.notify(NOTIFY_ID, notification);
    }

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }


    public static void sendNotification(String messageBody, Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, intent,
                PendingIntent.FLAG_IMMUTABLE);

        String channelId = "fcm_default_channel";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.gas_cylinder_icon)
                        .setContentTitle("GAS MESSAGE")
                        .setContentText("DAC -> " + messageBody)
                        .setAutoCancel(true)
                        .setAllowSystemGeneratedContextualActions(false)

                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }


    @SuppressLint("NewApi")
    private static RemoteViews.RemoteResponse makeCall(String phoneNumber, Context context) {
        Intent intent = new Intent(Intent.ACTION_DIAL); // ACTION_CALL requires permissions
        intent.setData(Uri.parse("tel:" + phoneNumber));
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return RemoteViews.RemoteResponse.fromPendingIntent(pendingIntent);

    }


    public static String getMyPhoneNumber(Context context) {
        if (context == null) return null;

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (telephonyManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ||
                        context.checkSelfPermission(android.Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED) {

                    String phoneNumber = telephonyManager.getLine1Number();
                    return (phoneNumber != null && !phoneNumber.isEmpty()) ? phoneNumber : "Phone number unavailable";
                } else {
                    return "Permission not granted";
                }
            } else {
                // For pre-Marshmallow versions
                String phoneNumber = telephonyManager.getLine1Number();
                return (phoneNumber != null && !phoneNumber.isEmpty()) ? phoneNumber : "Phone number unavailable";
            }
        }
        return "TelephonyManager unavailable";
    }

    public static boolean isAccessibilityServiceEnabled(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = mContext.getPackageName() + "/" + AcessibilitySettings.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
            );
            Log.v(TAG, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error finding setting, default accessibility to not found: " + e.getMessage());
        }

        if (accessibilityEnabled == 1) {
            Log.v(TAG, "Accessibility Is Enabled");
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessibilityService = splitter.next();
                    Log.v(TAG, "AccessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        Log.v(TAG, "accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            Log.v(TAG, "accessibility is disabled");
        }
        return false;
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
                            long twentyFourHoursInMillis = TimeUnit.HOURS.toMillis(10);
                            if (currentTime - time < twentyFourHoursInMillis) {
                                String body = cursor.getString(bodyColumnIndex);
                                Long date = cursor.getLong(dateColumnIndex);
                                String formattedDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale.getDefault())
                                        .format(new java.util.Date(date));

                                List<RegexModel> details = checkDACRegex(body, c);


                                if (details != null && details.size() > 0) {

                                    boolean isDAC = details.get(0).getCaptures().size() > 1 ? true : false;
                                    String DAC = isDAC ? details.get(0).getCaptures().get(1) : details.get(0).getCaptures().get(0);

                                    String message = isDAC ? details.get(0).getCaptures().get(0) : "Indian Oil OTP";

                                    if (isDAC) {
                                        if (!details.get(0).getId().equals("DAC_SYNC")) {
                                            fireDb.addToDb(DAC, message, formattedDate);
                                            return;
                                        }
                                        fireDb.syncDac(DAC, message, formattedDate);
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
        JSONObject jsonObject;
        JSONArray patterns = null;
        try {
            jsonObject = new JSONObject(jsonString);
            patterns = jsonObject.getJSONArray("patterns");
        } catch (JSONException e) {

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


        return matches;
    }

    public static String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String formattedDateTime = sdf.format(calendar.getTime());
        return formattedDateTime;
    }

    public static void sendSms(String Cashmemo, String DAC, String name, Context context) {

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String formattedDateTime = sdf.format(calendar.getTime());
        System.out.println("Current Date and Time: " + formattedDateTime);

        String msg = "CM - " + Cashmemo + " DAC - " + DAC + " Name " + name + " Time " + formattedDateTime;
        String phoneNumber = getPhoneNumber();
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        if (subscriptionManager == null) {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNumber, null, msg, null, null);
            return;
        }
        // Get active subscription list
        @SuppressLint("MissingPermission") List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptionInfos == null || subscriptionInfos.isEmpty()) {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNumber, null, msg, null, null);
            return;
        }
        // Send SMS from each SIM
        for (SubscriptionInfo subInfo : subscriptionInfos) {
            int subId = subInfo.getSubscriptionId();

            // Get SmsManager for this subscription ID
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
            } else {
                smsManager = SmsManager.getDefault();
            }
            try {
                // Send SMS
                smsManager.sendTextMessage(phoneNumber, null, msg, null, null);


            } catch (Exception e) {
                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(phoneNumber, null, msg, null, null);
                e.printStackTrace();

            }
        }

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

    public static String encodeB64(String input) {
        try {
            return Base64.encodeToString(input.getBytes("UTF-8"), Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.e(TAG, "Error encoding Base64: " + e.getMessage());
            return null; // Return null if encoding fails
        }
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


    public static void updateMessagePattern(String message, Context c) {


        // Obtain the SharedPreferences object
        SharedPreferences sharedPreferences = c.getSharedPreferences("OTP_Pattern", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("pattern", message);
        editor.apply();

    }


    public static void updateSenderNumbers(String message, Context c) {

        // Obtain the SharedPreferences object
        SharedPreferences sharedPreferences = c.getSharedPreferences("senders_number", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("numbers", message);
        editor.apply();

    }

    public static void updateProfile(String message, Context c) {

        // Obtain the SharedPreferences object
        SharedPreferences sharedPreferences = c.getSharedPreferences("profile_enc", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("profile", message);
        editor.apply();

    }

    public static void saveUnsentDAC(String DAC, Context c) {

        // Obtain the SharedPreferences object
        SharedPreferences sharedPreferences = c.getSharedPreferences("unsent_dac", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("unsent_dac", DAC);
        editor.putLong("saved_timestamp", System.currentTimeMillis());
        editor.apply();

    }

    public static String getUnsentDAC(Context c) {
        // Obtain the SharedPreferences object
        SharedPreferences sharedPreferences = c.getSharedPreferences("unsent_dac", Context.MODE_PRIVATE);

        return sharedPreferences.getString("unsent_dac", "not_found");

    }

    public static void clearUnsentDAC(Context c) {
        // Obtain the SharedPreferences object
        SharedPreferences sharedPreferences = c.getSharedPreferences("unsent_dac", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();  // Clear all values
        editor.apply();  // Save changes asynchronously


    }

    public static void sendAnyUnsentDAC(Context c) {
        FirebaseDBClient db = new FirebaseDBClient(c);
        SharedPreferences sharedPreferences = c.getSharedPreferences("unsent_dac", Context.MODE_PRIVATE);

        String DAC = sharedPreferences.getString("unsent_dac", "not_found");
        if (DAC.isEmpty()) {
            Log.d(Utility.TAG, "Connefcted to Internet but Already Sent Last DAC");
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        long savedTime = sharedPreferences.getLong("saved_timestamp", 0);
        boolean isSendAble = System.currentTimeMillis() - savedTime <= 14 * 60 * 60 * 1000;

        Log.d(Utility.TAG, "Connected to Internet Unsent DAC DETAILS  -> " + DAC + " savedTime " + savedTime + " isAbleToSend ? -> " + isSendAble);


        if (isSendAble) {


            String formattedDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date(savedTime));
            db.syncDac(DAC, "unsent_dac", formattedDate);
            editor.putString("unsent_dac", "");
            editor.apply();


        }


    }

    public static String getProfile(Context c) {
        // Obtain the SharedPreferences object
        SharedPreferences sharedPreferences = c.getSharedPreferences("profile_enc", Context.MODE_PRIVATE);
        return sharedPreferences.getString("profile", "N");
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
