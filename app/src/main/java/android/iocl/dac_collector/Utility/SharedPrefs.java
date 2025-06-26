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

    // Get an int
    public static int getAppVersion(Context context) {
        init(context);
        return sharedPreferences.getInt("appVersion", 0);
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
