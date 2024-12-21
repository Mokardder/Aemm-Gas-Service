package android.iocl.dac_collector.Utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.Key;
import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefs {

    private static final String PREF_NAME = "AppsData";  // Name of the SharedPreferences file
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    public SharedPrefs(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Setter method to store string value
    public static void setString(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    // Getter method to retrieve string value
    public static String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    // Setter method to store integer value
    public static void setInt(String key, int value) {
        editor.putInt(key, value);
        editor.apply();
    }

    // Getter method to retrieve integer value
    public static int getInt(String key, int defaultValue) {

        return sharedPreferences.getInt(key, defaultValue);
    }

    // Setter method to store boolean value
    public static void setBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    // Getter method to retrieve boolean value
    public static boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    // Remove a specific key-value pair
    public void remove(String key) {
        editor.remove(key);
        editor.apply();
    }

    // Clear all preferences
    public void clear() {
        editor.clear();
        editor.apply();
    }
}
