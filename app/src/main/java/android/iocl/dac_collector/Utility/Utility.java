package android.iocl.dac_collector.Utility;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.iocl.dac_collector.ModelData.RegexModel;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.MainActivity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility {
    
    private static String TAG = "Utility_Mokardder";


    public static Boolean isOldDac(String timestamp) {

        long converted = Long.parseLong(timestamp);

        long currentMillis = System.currentTimeMillis();
        long differenceMillis = currentMillis - converted;


        long differenceInSeconds = 50 * 1000;

        return Math.abs(differenceMillis) >= differenceInSeconds;

    }

    public static void showDACNotification(Context context, String OTP) {
        // Create a NotificationManager
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create the notification channel (required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "dac_sms_notify",
                    "Custom Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Create a RemoteViews object
        RemoteViews customLayout = new RemoteViews(context.getPackageName(), R.layout.dac_notification_bar);
        customLayout.setTextViewText(R.id.dac_val_eng, OTP);
        customLayout.setTextViewText(R.id.dac_val_beng, OTP);

        // Set up a pending intent (optional)
        Intent intent = new Intent(context, MainActivity.class); // Replace with your target activity
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Create the notification
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(context, "dac_sms_notify")
                    .setSmallIcon(R.drawable.gas_cylinder_icon) // Replace with your icon
                    .setContentIntent(pendingIntent)
                    .setCustomContentView(customLayout) // Set the custom layout
                    .setAutoCancel(false)
                    .setOngoing(false)
                    .build();
        }

        // Show the notification
        notificationManager.notify(1, notification);
    }

    public static boolean isConnectedToInternet(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    /**
     * 获取自启动管理页面的Intent * @param context context * @return 返回自启动管理页面的Intent *
     */
    public static void getAutostartSettingIntent(Context context) {
        ComponentName componentName = null;
        String brand = Build.MANUFACTURER;
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        switch (brand.toLowerCase()) {
            case "samsung":
                componentName = new ComponentName("com.samsung.android.sm", "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity");
                break;
            case "huawei":
//                componentName = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity");
                componentName = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity");
                break;
            case "xiaomi":
                componentName = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
                break;
            case "vivo":
//                componentName = new ComponentName("com.iqoo.secure", "com.iqoo.secure.safaguard.PurviewTabActivity");
                componentName = new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity");
                break;
            case "oppo":
//                componentName = new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity");
                componentName = new ComponentName("com.oplus.battery", "com.oplus.powermanager.fuelgaue.PowerConntrolActivity");
                break;

            case "oneplus":
                componentName = new ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity");
                break;
            case "letv":
                intent.setAction("com.letv.android.permissionautoboot");
            default:
                intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                break;
        }
        intent.setComponent(componentName);
        context.startActivity(intent);
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

    public static void sendSms(String Cashmemo, String DAC) {

        String msg = "CM - " + Cashmemo + " DAC - " + DAC;

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

    // Assuming getMessagepattern is implemented elsewhere


    public static void updateMessagePattern(String message, Context c) {

        Log.d(TAG, "Updatedt Pattern");
        // Obtain the SharedPreferences object
        SharedPreferences sharedPreferences = c.getSharedPreferences("OTP_Pattern", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("pattern", message);
        editor.apply();

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
