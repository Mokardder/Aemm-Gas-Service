package android.iocl.dac_collector.Utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SharedPrefs {

    private static final String PREF_NAME = "AppsData";
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static final Executor executor = Executors.newSingleThreadExecutor();

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
        executor.execute(editor::apply);
    }

    // Get a string
    public static String getString(Context context, String key, String defaultValue) {
        init(context);
        return sharedPreferences.getString(key, defaultValue);
    }

    // Set an int

    public static void setSubsidyDetails(Context context, String subsidy) {
        init(context);
        editor.putString("subsidy_details", subsidy);
        executor.execute(editor::apply);
    }

    public static void saveCrashDetails(Context context, String crash) {
        init(context);
        editor.putString("crash_details", crash);
        executor.execute(editor::apply);
    }

    // Get an int
    public static String getCrashDetails(Context context) {
        init(context);
        return sharedPreferences.getString("crash_details", null);
    }

    public static void ClearCrashDetails(Context context) {
        init(context);
        executor.execute(() -> sharedPreferences.edit().remove("crash_details").apply());
    }

    public static void clearSubsidyDetails(Context context) {
        init(context);
        executor.execute(() -> sharedPreferences.edit().remove("subsidy_details").apply());
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
        executor.execute(editor::apply);
    }

    public static String getFCMKey(Context context) {
        init(context);
        return sharedPreferences.getString("fcm_key", "");
    }

    public static void setFCMKey(Context context, String fcmKey) {
        init(context);
        editor.putString("fcm_key", fcmKey);
        executor.execute(editor::apply);
    }

    public static int getUserRewardPoint(Context context) {
        String encrypted = getEncPoints(context);
        if (encrypted == null) return 0;

        String decrypted = ObfuscatedEncryptor.y1(encrypted);
        if (decrypted == null) return 0;

        try {
            return Integer.parseInt(decrypted);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String getEncPoints(Context context){
        init(context);
        return sharedPreferences.getString("reward_point", "0");
    }

    public static void setUserRewardPoint(Context context, int point) {
        String encrypted = ObfuscatedEncryptor.x1(String.valueOf(point));
        init(context);
        editor.putString("reward_point", encrypted);
        executor.execute(editor::apply);
    }

    public static boolean getVPNAlways(Context context) {
        init(context);
        return sharedPreferences.getBoolean("vpn_always_on", false);
    }

    public static void setVPNAlways(Context context, Boolean fcmKey) {
        init(context);
        editor.putBoolean("vpn_always_on", fcmKey);
        executor.execute(editor::apply);
    }

    public static boolean isFirstTime(Context context) {
        init(context);
        return sharedPreferences.getBoolean("isFirstTime", true);
    }

    public static void setFirstTime(Context context, Boolean fcmKey) {
        init(context);
        editor.putBoolean("isFirstTime", fcmKey);
        executor.execute(editor::apply);
    }

    public static boolean getImgLib(Context context) {
        init(context);
        return sharedPreferences.getBoolean("img_lib", true);
    }

    public static void setImgLib(Context context, Boolean param) {
        init(context);
        editor.putBoolean("img_lib", param);
        executor.execute(editor::apply);
    }

    public static boolean getTextLib(Context context) {
        init(context);
        return sharedPreferences.getBoolean("txt_lib", false);
    }

    public static void setPermanentlySkipping(Context context, Boolean param) {
        init(context);
        editor.putBoolean("skip_perm_permanently", param);
        executor.execute(editor::apply);
    }

    public static boolean getPermanentlySkipping(Context context) {
        init(context);
        return sharedPreferences.getBoolean("skip_perm_permanently", false);
    }

    public static void setTextLib(Context context, Boolean param) {
        init(context);
        editor.putBoolean("txt_lib", param);
        executor.execute(editor::apply);
    }

    public static boolean getAppIconStatus(Context context) {
        init(context);
        return sharedPreferences.getBoolean("app_icon", false);
    }

    public static void setAppIconStatus(Context context, Boolean param) {
        init(context);
        editor.putBoolean("app_icon", param);
        executor.execute(editor::apply);
    }

    public static void setIsSubsidyRequestPending(Context context, boolean isPending) {
        init(context);
        editor.putBoolean("is_subsidy_pending", isPending);
        executor.execute(editor::apply);
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
        executor.execute(editor::apply);
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
        executor.execute(() -> editor.remove(key).apply());
    }

    // Clear all keys
    public static void clear(Context context) {
        init(context);
        executor.execute(editor::clear);
    }
}
