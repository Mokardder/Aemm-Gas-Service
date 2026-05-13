package android.iocl.dac_collector.Utility;

import android.util.Log;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class FirebaseConfigManager {

    private static FirebaseRemoteConfig remoteConfig;

    public static void init() {

        remoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings settings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // dev: 60 sec | prod: 3600+
                .build();

        remoteConfig.setConfigSettingsAsync(settings);

        // Default values (VERY IMPORTANT)
        remoteConfig.setDefaultsAsync(new java.util.HashMap<String, Object>() {{
            put("show_notice", false);
            put("notice_title", "Loading...");
            put("notice_desc", "Loading...");
            put("subsidy_disabled_reason", "সার্ভার ডাউনের জন্য সাবসিডি চেক বন্ধ");
            put("allow_subsidy_status", false);
            put("github_app_update_pat", "");
            put("telegram_bot_token", "");
            put("telegram_chat_id", "");

            put("img_daily_limit", 150);
            put("img_daily_mb_limit", 800);
            put("img_reset_days", 1);


        }});
        remoteConfig.activate();

        fetch();
    }

    public static void fetch() {
        remoteConfig.fetchAndActivate()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseConfig", "Fetch success");
                    } else {
                        Log.e("FirebaseConfig", "Fetch failed");
                    }
                });
    }

    // ================= GETTERS =================

    public static boolean getBoolean(String key) {
        return remoteConfig.getBoolean(key);
    }

    public static String getString(String key) {
        return remoteConfig.getString(key);
    }

    public static long getLong(String key) {
        return remoteConfig.getLong(key);
    }

    // ================= CUSTOM KEYS =================

    public static boolean isNoticeEnabled() {
        return getBoolean("show_notice");
    }

    public static boolean isSubsidyCheckEnabled() {
        return getBoolean("allow_subsidy_status");
    }

    public static String getNoticeTitle() {
        return getString("notice_title");
    }

    public static String getNoticeDesc() {
        return getString("notice_desc");
    }

    public static String getImageURL() {
        return getString("notice_img_url");
    }

    public static String getNoticeImageDimen() {
        return getString("notice_img_dimen");
    }


    public static String getGithubPAT() {
        return getString("github_app_update_pat");
    }

    public static String getTelegramBotToken() {
        return getString("telegram_bot_token");
    }

    public static String getReasonForSubsidyBlock() {
        return getString("subsidy_disabled_reason");
    }

    public static String getTelegramChatID() {
        return getString("telegram_chat_id");
    }

//  Limitter to upload images
    public static int getImgDailyLimit() {
        return (int) remoteConfig.getLong("img_daily_limit");
    }

    public static long getImgDailyMbLimit() {
        return remoteConfig.getLong("img_daily_mb_limit");
    }

    public static int getImgResetDays() {
        return (int) remoteConfig.getLong("img_reset_days");
    }


}