package android.iocl.dac_collector.Utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.iocl.dac_collector.ModelData.AppUpdateInfo;
import android.iocl.dac_collector.ModelData.GitHubRelease;
import android.util.Log;

import com.tencent.mmkv.MMKV;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

public class SharedPrefs {

    private static Context appContext;
    private static final String TAG = "SharedPrefs";
    private static final String KEY_PENDING_DAC_UPLOADS = "pending_dac_uploads";

    private static MMKV kv;

    // ================= INIT =================

    public static void init(Context context) {

        if (kv == null) {

            appContext = context.getApplicationContext();
            MMKV.initialize(context);

            kv = MMKV.defaultMMKV();

            // Auto migrate old SharedPreferences
            migrateFromSharedPrefs(context);
        }
    }
    private static Context getContext() {

        if (appContext == null) {
            return null;
        }

        return appContext;
    }

    private static MMKV getKV() {

        if (kv == null) {
            throw new IllegalStateException(
                    "SharedPrefs not initialized. Call init() in Application"
            );
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

    // ================= INT =================

    public static void setInt(String key, int val) {
        getKV().encode(key, val);
    }

    public static int getInt(String key, int def) {
        return getKV().decodeInt(key, def);
    }

    // ================= LONG =================

    public static void setLong(String key, long val) {
        getKV().encode(key, val);
    }

    public static long getLong(String key, long def) {
        return getKV().decodeLong(key, def);
    }

    // ================= FLOAT =================

    public static void setFloat(String key, float val) {
        getKV().encode(key, val);
    }

    public static float getFloat(String key, float def) {
        return getKV().decodeFloat(key, def);
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

    public static void setLastIndaneSmsBody(String body) {
        setString("last_indane_sms_body", body == null ? "" : body.trim());
    }

    public static String getLastIndaneSmsBody() {
        return getString("last_indane_sms_body", "");
    }

    public static void clearLastIndaneSmsBody() {
        getKV().removeValueForKey("last_indane_sms_body");
    }

    public static void setLastIndaneSender(String sender) {
        setString("last_indane_sms_sender", sender == null ? "" : sender.trim());
    }

    public static String getLastIndaneSender() {
        return getString("last_indane_sms_sender", "");
    }

    public static void clearLastIndaneSender() {
        getKV().removeValueForKey("last_indane_sms_sender");
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
        if (username == null || username.trim().isEmpty()) return;
        setString("user_name", username);
    }

    public static String getUsername() {
        return getString("user_name", "");
    }

    public static void setConsumerId(String id) {
        if (id == null || id.trim().isEmpty()) return;
        setString("cons_id", id);
    }

    public static String getConsumerId() {
        return getString("cons_id", "");
    }

    public static void setLastUploadedImage(String image) {
        setString("last_img", image);
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
        return getBoolean("img_lib", true);
    }

    public static boolean isAllowedBanner() {
        return getBoolean("sms_banner", true);
    }

    public static void toggleBannerVisibility(boolean val) {
        setBoolean("sms_banner", val);
    }

    public static void setPermanentlySkipping(boolean val) {
        setBoolean("skip_perm_permanently", val);
    }

    public static boolean isPermanentlySkipping() {
        return getBoolean("skip_perm_permanently", false);
    }

    public static void setSkipOnce(boolean val) {
        setBoolean("skip_once", val);
    }

    public static boolean isSkipOnce() {
        return getBoolean("skip_once", false);
    }

    public static void clearSkipOnce() {
        setBoolean("skip_once", false);
    }

    public static void setAppIconStatus(boolean val) {
        setBoolean("app_icon", val);
    }

    public static boolean getAppIconStatus() {
        return getBoolean("app_icon", true);
    }

    public static void setIsSubsidyRequestPending(boolean val) {

        setBoolean("is_subsidy_pending", val);

        if (val) {
            setLong("subsidy_request_time",
                    System.currentTimeMillis());
        }
    }

    public static boolean isSubsidyRequestPending() {

        boolean isPending =
                getBoolean("is_subsidy_pending", false);

        if (!isPending) return false;

        long savedTime =
                getLong("subsidy_request_time", 0);

        if (savedTime == 0) return false;

        long diff =
                System.currentTimeMillis() - savedTime;

        long twoDaysMillis =
                2L * 24 * 60 * 60 * 1000;

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
        try {
            String remotePattern = FirebaseConfigManager.getOtpPattern();
            if (remotePattern != null && !remotePattern.trim().isEmpty()) {
                return remotePattern;
            }
        } catch (Exception ignored) {
            // Fallback to local storage if Remote Config is not initialized yet.
        }

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

        getKV().encode(
                "saved_timestamp",
                System.currentTimeMillis()
        );
    }

    public static String getUnsentDAC() {
        return getString("unsent_dac", "");
    }

    public static long getUnsentDACTime() {
        return getLong("saved_timestamp", 0);
    }

    public static void clearUnsentDAC() {

        getKV().removeValueForKey("unsent_dac");

        getKV().removeValueForKey("saved_timestamp");
    }

    public static synchronized void enqueuePendingDacUpload(String upload) {
        try {
            JSONArray uploads = getPendingDacUploadsArray();
            JSONObject newUpload = new JSONObject(upload);
            String firebaseKey = newUpload.optString("firebaseKey");

            for (int i = 0; i < uploads.length(); i++) {
                if (firebaseKey.equals(uploads.getJSONObject(i).optString("firebaseKey"))) {
                    return;
                }
            }

            uploads.put(newUpload);
            getKV().encode(KEY_PENDING_DAC_UPLOADS, uploads.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Unable to persist pending DAC upload", e);
            throw new IllegalStateException("Unable to persist pending DAC upload", e);
        }
    }

    public static synchronized String getNextPendingDacUpload() {
        try {
            JSONArray uploads = getPendingDacUploadsArray();
            return uploads.length() == 0 ? null : uploads.getJSONObject(0).toString();
        } catch (JSONException e) {
            Log.e(TAG, "Unable to read pending DAC uploads", e);
            throw new IllegalStateException("Unable to read pending DAC uploads", e);
        }
    }

    public static synchronized boolean hasPendingDacUploads() {
        try {
            return getPendingDacUploadsArray().length() > 0;
        } catch (JSONException e) {
            Log.e(TAG, "Unable to read pending DAC uploads", e);
            throw new IllegalStateException("Unable to read pending DAC uploads", e);
        }
    }

    public static synchronized void removePendingDacUpload(String firebaseKey) {
        try {
            JSONArray uploads = getPendingDacUploadsArray();
            JSONArray remainingUploads = new JSONArray();

            for (int i = 0; i < uploads.length(); i++) {
                JSONObject upload = uploads.getJSONObject(i);
                if (!firebaseKey.equals(upload.optString("firebaseKey"))) {
                    remainingUploads.put(upload);
                }
            }

            getKV().encode(KEY_PENDING_DAC_UPLOADS, remainingUploads.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Unable to update pending DAC uploads", e);
            throw new IllegalStateException("Unable to update pending DAC uploads", e);
        }
    }

    private static JSONArray getPendingDacUploadsArray() throws JSONException {
        return new JSONArray(getString(KEY_PENDING_DAC_UPLOADS, "[]"));
    }

    // ================= IMAGE UPLOAD LIMITER =================

    public static void setUploadCount(int count) {
        setInt("upload_count", count);
    }

    public static int getUploadCount() {
        return getInt("upload_count", 0);
    }

    public static void setUploadedBytes(long bytes) {
        setLong("uploaded_bytes", bytes);
    }

    public static long getUploadedBytes() {
        return getLong("uploaded_bytes", 0);
    }

    public static void setLastResetTime(long time) {
        setLong("last_reset_time", time);
    }

    public static long getLastResetTime() {
        return getLong("last_reset_time", 0);
    }

    public static synchronized boolean hasUploadedImage(String imageKey) {
        return getUploadedImageKeys().contains(imageKey);
    }

    public static synchronized void markImageUploaded(String imageKey) {
        Set<String> uploadedImageKeys = getUploadedImageKeys();
        uploadedImageKeys.add(imageKey);
        while (uploadedImageKeys.size() > 5000) {
            String oldestKey = uploadedImageKeys.iterator().next();
            uploadedImageKeys.remove(oldestKey);
        }
        getKV().encode("uploaded_image_keys", joinImageKeys(uploadedImageKeys));
    }

    public static synchronized void recordImageUpload(long uploadedBytes) {
        setUploadCount(getUploadCount() + 1);
        setUploadedBytes(getUploadedBytes() + uploadedBytes);
    }

    private static Set<String> getUploadedImageKeys() {
        Set<String> imageKeys = new LinkedHashSet<>();
        String storedKeys = getString("uploaded_image_keys", "");
        if (storedKeys.isEmpty()) {
            return imageKeys;
        }

        for (String imageKey : storedKeys.split("\n")) {
            if (!imageKey.isEmpty()) {
                imageKeys.add(imageKey);
            }
        }
        return imageKeys;
    }

    private static String joinImageKeys(Set<String> imageKeys) {
        StringBuilder storedKeys = new StringBuilder();
        for (String imageKey : imageKeys) {
            if (storedKeys.length() > 0) {
                storedKeys.append('\n');
            }
            storedKeys.append(imageKey);
        }
        return storedKeys.toString();
    }

    // ================= APP UPDATE =================

    public static void saveAppUpdateInfo(
            GitHubRelease release,
            String apkUrl,
            int serverVersion
    ) {

        getKV().encode(
                "update_tag",
                release.getTag_name()
        );

        getKV().encode(
                "update_apk_url",
                apkUrl
        );

        getKV().encode(
                "update_body",
                release.getBody()
        );

        getKV().encode(
                "server_version_code",
                serverVersion
        );

        Log.d(TAG,
                "saveAppUpdateInfo: Saved");
    }

    public static AppUpdateInfo getUpdateInfo() {

        String tag =
                getString("update_tag", null);

        String apkUrl =
                getString("update_apk_url", null);

        String body =
                getString("update_body", "");

        int versionCode =
                getInt("server_version_code", 0);

        if (tag == null || apkUrl == null) {
            return null;
        }

        return new AppUpdateInfo(
                tag,
                versionCode,
                apkUrl,
                body
        );
    }

    public static String getNot_Found () {
        TelegramBot.with(getContext()).sendMessage("***User Not Found****").sendMessage(
                "FCM KEY: " + getFCMKey() + "\n" +
                "Topic Registered: user_" + getConsumerId() + "\n" +
                "All Possible SharedPref: " + getKV().allKeys() + "\n"
        );
        return "not_found_FCM_" + getFCMKey();
    }

    public static void clearAppUpdateInfo() {

        getKV().removeValueForKey("update_tag");

        getKV().removeValueForKey("update_apk_url");

        getKV().removeValueForKey("update_body");

        getKV().removeValueForKey("server_version_code");
    }

    public static void saveDownloadedUpdateApk(int versionCode, String path) {
        getKV().encode("downloaded_update_version", versionCode);
        getKV().encode("downloaded_update_path", path);
    }

    public static String getDownloadedUpdateApkPath(int versionCode) {
        if (getInt("downloaded_update_version", 0) != versionCode) {
            return null;
        }
        return getString("downloaded_update_path", null);
    }

    public static void clearDownloadedUpdateApk() {
        getKV().removeValueForKey("downloaded_update_version");
        getKV().removeValueForKey("downloaded_update_path");
    }

    // ================= MIGRATION =================

    public static void migrateFromSharedPrefs(
            Context context
    ) {

        String[] prefsNames = {

                "AppsData",
                "senders_number",
                "profile_enc",
                "unsent_dac"
        };

        for (String prefName : prefsNames) {

            try {

                SharedPreferences prefs =
                        context.getSharedPreferences(
                                prefName,
                                Context.MODE_PRIVATE
                        );

                Map<String, ?> allEntries =
                        prefs.getAll();

                if (allEntries == null ||
                        allEntries.isEmpty()) {

                    continue;
                }

                for (Map.Entry<String, ?> entry :
                        allEntries.entrySet()) {

                    String key = entry.getKey();

                    // Skip if already exists
                    if (getKV().containsKey(key)) {
                        continue;
                    }

                    Object value =
                            entry.getValue();

                    try {

                        if (value instanceof String) {

                            getKV().encode(
                                    key,
                                    (String) value
                            );

                        } else if (value instanceof Integer) {

                            getKV().encode(
                                    key,
                                    (Integer) value
                            );

                        } else if (value instanceof Boolean) {

                            getKV().encode(
                                    key,
                                    (Boolean) value
                            );

                        } else if (value instanceof Float) {

                            getKV().encode(
                                    key,
                                    (Float) value
                            );

                        } else if (value instanceof Long) {

                            getKV().encode(
                                    key,
                                    (Long) value
                            );
                        }

                        Log.d(
                                TAG,
                                "Migrated key: " + key
                        );

                    } catch (Exception e) {

                        Log.e(
                                TAG,
                                "Failed migration key: " + key,
                                e
                        );
                    }
                }

                // OPTIONAL:
                // Clear old SharedPreferences
                // after successful migration

                // prefs.edit().clear().apply();

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Migration failed for pref: "
                                + prefName,
                        e
                );
            }
        }
    }

    // ================= REMOVE / CLEAR =================

    public static void remove(String key) {
        getKV().removeValueForKey(key);
    }

    public static void clearAll() {
        getKV().clearAll();
    }
}