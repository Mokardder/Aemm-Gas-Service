package android.iocl.dac_collector.Utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SharedPrefs {

    private static final String PREF_NAME = "AppsData";
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    // Initialize SharedPreferences once
    private static void init(Context context) {
        if (sharedPreferences == null || editor == null) {
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();
        }
    }

    // Set a string
    public static void setString(Context context, String key, String value) {
        init(context);
        editor.putString(key, value);
        editor.apply();
    }

    // Get a string
    public static String getString(Context context, String key, String defaultValue) {
        init(context);
        return sharedPreferences.getString(key, defaultValue);
    }

    // Set an int
    public static void setAppVersion(Context context, int version) {
        init(context);
        editor.putInt("appVersion", version);
        editor.apply();
    }

    public static void setSubsidyDetails(Context context, String subsidy) {
        init(context);
        editor.putString("subsidy_details", subsidy);
        editor.apply();
    }

    public static void saveCrashDetails(Context context, String crash) {
        init(context);
        editor.putString("crash_details", crash);
        editor.apply();
    }


    // Get an int
    public static String getCrashDetails(Context context) {
        init(context);
        return sharedPreferences.getString("crash_details", null);
    }

    public static void ClearCrashDetails(Context context) {
        init(context);
        sharedPreferences.edit().remove("crash_details").apply();
    }

    public static void clearSubsidyDetails(Context context) {
        init(context);
        sharedPreferences.edit().remove("subsidy_details").apply();
    }

    public static String getSubsidyDetails(Context context) {
        init(context);
        return sharedPreferences.getString("subsidy_details", "");
    }
    public static String lastSubsidyDate(Context context) {
        init(context);
        return sharedPreferences.getString("last_subsidy_date", "");
    }

    public static void SetlastSubsidyDate(Context context, String crash) {
        init(context);
        editor.putString("last_subsidy_date", crash);
        editor.apply();
    }
    public static String getFCMKey(Context context) {
        init(context);
        return sharedPreferences.getString("fcm_key", "");
    }

    public static void setFCMKey(Context context, String fcmKey) {
        init(context);
        editor.putString("fcm_key", fcmKey);
        editor.apply();
    }
    public static void setIsSubsidyRequestPending(Context context, boolean isPending) {
        init(context);
        editor.putBoolean("is_subsidy_pending", isPending);
        editor.apply();
    }

    public static String getUserName(Context context) {
        init(context);
        return sharedPreferences.getString("user_name", "");
    }
    public static boolean isSubsidyRequestPending(Context context) {
        init(context);
        return sharedPreferences.getBoolean("is_subsidy_pending", false);
    }

    public static String getUserID(Context context) {
        init(context);
        return sharedPreferences.getString("cons_id", "");
    }

    // Set a boolean
    public static void setBoolean(Context context, String key, boolean value) {
        init(context);
        editor.putBoolean(key, value);
        editor.apply();
    }

    // Get a boolean
    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        init(context);
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public static void setRestrictionEnabled(Context context) {
        setBoolean(context, "restriction", true);
    }

    public static void setRestrictionDisabled(Context context) {
        setBoolean(context, "restriction", false);
    }

    public static void setUsername(Context context, String username) {
        setString(context, "user_name", username);
    }

    public static String getUsername(Context context) {
        return getString(context, "user_name", "not_found");
    }

    public static void setConsumerId(Context context, String username) {
        setString(context, "cons_id", username);
    }

    public static String getConsumerId(Context context) {
        return getString(context, "cons_id", "not_found");
    }


    public static String getLastUploadedImage(Context context) {
        return getString(context, "last_img", "");
    }

    public static boolean getRestrictionEnabled(Context context) {
        return getBoolean(context, "restriction", false);
    }

    public static void setTileAdded(Context context) {
        setBoolean(context, "isTileAdded", true);
        Log.d("Tiles", "setTileAdded: True");
    }

    public static boolean isTileAdded(Context context) {
        boolean isAdded = getBoolean(context, "isTileAdded", false);
        Log.d("Tiles", "getTileAdded: " + isAdded);
        return isAdded;
    }


    // Remove a key
    public static void remove(Context context, String key) {
        init(context);
        editor.remove(key);
        editor.apply();
    }

    // Clear all keys
    public static void clear(Context context) {
        init(context);
        editor.clear();
        editor.apply();
    }
}
