package android.iocl.dac_collector.Utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.iocl.dac_collector.ModelData.AppUpdateInfo;
import android.iocl.dac_collector.ModelData.GitHubRelease;
import android.util.Log;

import com.tencent.mmkv.MMKV;

import java.util.Map;

public class SharedPrefs {

    private static final String MIGRATION_DONE = "mmkv_migrated";

    private static MMKV kv;

    // 🔥 Init ONLY ONCE (Application class)
    public static void init(Context context) {
        if (kv == null) {
            MMKV.initialize(context);
            kv = MMKV.defaultMMKV();
        }
    }

    private static MMKV getKV() {
        if (kv == null) {
            throw new IllegalStateException("SharedPrefs not initialized. Call init() in Application");
        }
        return kv;
    }

    // ================= STRING =================

    public static void setString(String key, String value) {
        getKV().encode(key, value);
    }

    public static String getString(String key, String def) {
        return getKV().decodeString(key, def);
    }

    // ================= BOOLEAN =================

    public static void setBoolean(String key, boolean val) {
        getKV().encode(key, val);
    }

    public static boolean getBoolean(String key, boolean def) {
        return getKV().decodeBool(key, def);
    }

    // ================= CUSTOM =================

    public static void setSubsidyDetails(String subsidy) {
        setString("subsidy_details", subsidy);

    }

    public static String getSubsidyDetails() {
        return getString("subsidy_details", "");
    }

    public static void clearSubsidyDetails() {
        getKV().removeValueForKey("subsidy_details");
    }

    public static void saveCrashDetails(String crash) {
        setString("crash_details", crash);
    }

    public static String getCrashDetails() {
        return getString("crash_details", null);
    }

    public static void clearCrashDetails() {
        getKV().removeValueForKey("crash_details");
    }

    public static void setLastSubsidyDate(String date) {
        setString("last_subsidy_date", date);
    }

    public static String getLastSubsidyDate() {
        return getString("last_subsidy_date", "");
    }

    public static void setFCMKey(String key) {
        setString("fcm_key", key);
    }

    public static String getFCMKey() {
        return getString("fcm_key", "");
    }

    // ================= USER =================

    public static void setUsername(String username) {
        setString("user_name", username);
    }

    public static String getUsername() {
        return getString("user_name", "not_found");
    }

    public static void setConsumerId(String id) {
        setString("cons_id", id);
    }

    public static String getConsumerId() {
        return getString("cons_id", "");
    }

    public static String getLastUploadedImage() {
        return getString("last_img", "");
    }

    // ================= FLAGS =================

    public static void setVPNAlways(boolean val) {
        setBoolean("vpn_always_on", val);
    }

    public static boolean getVPNAlways() {
        return getBoolean("vpn_always_on", false);
    }

    public static void setFirstTime(boolean val) {
        setBoolean("isFirstTime", val);
    }

    public static boolean isFirstTime() {
        return getBoolean("isFirstTime", true);
    }

    public static void setImgLib(boolean val) {
        setBoolean("img_lib", val);
    }

    public static boolean getImgLib() {
        return getBoolean("img_lib", false);
    }

    public static void setPermanentlySkipping(boolean val) {
        setBoolean("skip_perm_permanently", val);
    }

    public static boolean getPermanentlySkipping() {
        return getBoolean("skip_perm_permanently", false);
    }

    public static void setAppIconStatus(boolean val) {
        setBoolean("app_icon", val);
    }

    public static void setIsSubsidyRequestPending(boolean val) {
        setBoolean("is_subsidy_pending", val);
        getKV().encode("subsidy_request_time", System.currentTimeMillis());

    }

    public static boolean isSubsidyRequestPending() {
        boolean isPending = getBoolean("is_subsidy_pending", false);
        if (!isPending) return false;
        long savedTime = getKV().decodeLong("subsidy_request_time", 0);
        if (savedTime == 0) return false;
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - savedTime;
        long twoDaysMillis = 2L * 24 * 60 * 60 * 1000;
        return diff <= twoDaysMillis;
    }

    public static void setRestrictionEnabled() {
        setBoolean("restriction", true);
    }

    public static void setRestrictionDisabled() {
        setBoolean("restriction", false);
    }

    public static boolean isRestrictionEnabled() {
        return getBoolean("restriction", false);
    }

    public static void setTileAdded() {
        setBoolean("isTileAdded", true);
    }

    public static boolean isTileAdded() {
        return getBoolean("isTileAdded", false);
    }


    // ================= OTP PATTERN =================

    public static void setPattern(String pattern) {
        setString("pattern", pattern);
    }

    public static String getPattern() {
        return getString("pattern", "N");
    }

// ================= SENDER NUMBERS =================

    public static void setSenderNumbers(String numbers) {
        setString("numbers", numbers);
    }

    public static String getSenderNumbers() {
        return getString("numbers", "N");
    }

// ================= PROFILE =================

    public static void setProfile(String profile) {
        setString("profile", profile);
    }

    public static String getProfile() {
        return getString("profile", "N");
    }

// ================= UNSENT DAC =================

    public static void saveUnsentDAC(String dac) {
        getKV().encode("unsent_dac", dac);
        getKV().encode("saved_timestamp", System.currentTimeMillis());
    }

    public static String getUnsentDAC() {
        return getString("unsent_dac", "");
    }

    public static long getUnsentDACTime() {
        return getKV().decodeLong("saved_timestamp", 0);
    }

    public static void clearUnsentDAC() {
        getKV().removeValueForKey("unsent_dac");
        getKV().removeValueForKey("saved_timestamp");
    }


    public static void saveAppUpdateInfo(GitHubRelease release, String apkUrl, int serverVersion) {
        getKV().encode("update_tag", release.getTag_name());
        getKV().encode("update_apk_url", apkUrl);
        getKV().encode("update_body", release.getBody());
        getKV().encode("server_version_code", serverVersion);

        Log.d("MMKV", "saveAppUpdateInfo:Saved " +release + apkUrl + release.getBody() + serverVersion);
    }


    public static AppUpdateInfo getUpdateInfo() {
        String tag = getString("update_tag", null);
        String apkUrl = getString("update_apk_url", null);
        String body = getString("update_body", "");
        int versionCode = getKV().decodeInt("server_version_code", 0);


        // ✅ prevent crash
        if (tag == null || apkUrl == null) {
            return null;
        }

        return new AppUpdateInfo(tag, versionCode, apkUrl, body);
    }

    public static void clearAppUpdateInfo () {
        getKV().removeValueForKey("update_tag");
        getKV().removeValueForKey("update_apk_url");
        getKV().removeValueForKey("update_body");
        getKV().removeValueForKey("server_version_code");
    }


    public static void migrateFromSharedPrefs(Context context) {
        MMKV.initialize(context);
        MMKV kv = MMKV.defaultMMKV();

        // If already migrated → skip
        if (kv.decodeBool(MIGRATION_DONE, false)) {
            return;
        }

        // List all your old SharedPreferences names
        String[] prefsNames = {
                "AppsData",
                "OTP_Pattern",
                "senders_number",
                "profile_enc",
                "unsent_dac"
        };

        for (String prefName : prefsNames) {
            SharedPreferences prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
            Map<String, ?> allEntries = prefs.getAll();

            if (allEntries != null) {
                for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                    String key = prefName + "_" + entry.getKey(); // avoid key conflict
                    Object value = entry.getValue();

                    if (value instanceof String) {
                        kv.encode(key, (String) value);
                    } else if (value instanceof Integer) {
                        kv.encode(key, (Integer) value);
                    } else if (value instanceof Boolean) {
                        kv.encode(key, (Boolean) value);
                    } else if (value instanceof Float) {
                        kv.encode(key, (Float) value);
                    } else if (value instanceof Long) {
                        kv.encode(key, (Long) value);
                    }
                }
            }
        }

        // Mark migration complete
        kv.encode(MIGRATION_DONE, true);
    }

    // ================= REMOVE / CLEAR =================

    public static void remove(String key) {
        getKV().removeValueForKey(key);
    }

    public static void clearAll() {
        getKV().clearAll();
    }
}