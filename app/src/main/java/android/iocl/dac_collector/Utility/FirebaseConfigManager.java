package android.iocl.dac_collector.Utility;

import android.os.Build;
import android.util.Log;

import android.iocl.dac_collector.BuildConfig;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.json.JSONObject;

public class FirebaseConfigManager {

    private static FirebaseRemoteConfig remoteConfig;

    public static Task<Boolean> init() {

        remoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings settings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(BuildConfig.DEBUG ? 10 : 3600) // dev: 60 sec | prod: 3600+
                .build();

        Task<Void> settingsTask =
                remoteConfig.setConfigSettingsAsync(settings);

        // Default values (VERY IMPORTANT)
        Task<Void> defaultsTask =
                remoteConfig.setDefaultsAsync(new java.util.HashMap<String, Object>() {{
            put("show_notice", false);
            put("notice_title", "Loading...");
            put("notice_desc", "Loading...");
            put("subsidy_disabled_reason", "সার্ভার ডাউনের জন্য সাবসিডি চেক বন্ধ");
            put("allow_subsidy_status", false);
            put("github_app_update_pat", "");
            put("telegram_bot_token", "");
            put("telegram_chat_id", "");
            put("sms_sender_keyword", "indane");
            put("websocket1_url", "wss://aemm-gas-service.crazyboychoto.workers.dev/");
            put("websocket2_url", "");
            put("plugin_version", 0);
            put("plugin_url", "");


            put("getSmsSenderKeyword", "indane");

            put("img_daily_limit", 150);
            put("img_daily_mb_limit", 800);
            put("img_reset_days", 1);

            put("miss_call_book", "+918454955555");

            put("minimun_required_day", 3);

            put("dac_banner_visibility", true);
            put("otp_patterns", "{\n  \"actions\": \"otp_patterns\",\n  \"payload\": \"{\\r\\n  \\\"patterns\\\": [\\r\\n    {\\r\\n      \\\"id\\\": \\\"DAC\\\",\\r\\n      \\\"regex\\\": \\\"Invoice Number # (\\\\d+-\\\\d+) is (\\\\d{6})\\\",\\r\\n      \\\"capture\\\": \\\"1, 2\\\"\\r\\n    },\\r\\n    {\\r\\n      \\\"id\\\": \\\"OTP\\\",\\r\\n      \\\"regex\\\": \\\"Your IOCL one time password is :(\\\\d{4})\\\",\\r\\n      \\\"capture\\\": \\\"1\\\"\\r\\n    },\\r\\n    {\\r\\n      \\\"id\\\": \\\"GeneratedDAC\\\",\\r\\n      \\\"regex\\\": \\\"Invoice generated for Rs. (\\\\d{3}).Share DAC (\\\\d{6})\\\",\\r\\n      \\\"capture\\\": \\\"2\\\"\\r\\n    }\\r\\n  ]\\r\\n}\"\n}");
        }});

        return Tasks.whenAll(settingsTask, defaultsTask)
                .onSuccessTask(ignored -> fetch());
    }

    public static Task<Boolean> fetch() {
        return remoteConfig.fetchAndActivate()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(
                                "FirebaseConfigManager",
                                "Remote Config fetch/activate failed",
                                task.getException()
                        );
                        return;
                    }

                    boolean bannerEnabled = isBannerEnabled();
                    SharedPrefs.toggleBannerVisibility(bannerEnabled);

                    String otpPatterns = getOtpPatternsPayload();
                    if (otpPatterns != null && !otpPatterns.trim().isEmpty()) {
                        SharedPrefs.setPattern(otpPatterns);
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
    public static boolean isBannerEnabled() {
        return getBoolean("dac_banner_visibility");
    }
    public static int getPluginVersion() {
        return (int) remoteConfig.getLong("plugin_version");
    }
    public static String getPluginUrl() {
        return getString("plugin_url");
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
    public static int getSubsidyCheckRequiredDay() {
        return (int) remoteConfig.getLong("minimun_required_day");
    }

    public static String getNoticeImageDimen() {
        return getString("notice_img_dimen");
    }

    public static String getOtpPatternsPayload() {
        String rawValue = getString("otp_patterns");
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return "";
        }

        try {
            JSONObject jsonObject = new JSONObject(rawValue);
            if (jsonObject.has("payload") && !jsonObject.isNull("payload")) {
                String payload = jsonObject.optString("payload", "");
                if (payload != null && !payload.trim().isEmpty()) {
                    return payload;
                }
            }
            if (jsonObject.has("patterns")) {
                return rawValue;
            }
        } catch (Exception ignored) {
            // raw value is already a direct payload JSON string
        }

        return rawValue;
    }

    public static String getOtpPattern() {
        return getOtpPatternsPayload();
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
    public static String getMissCallBookNo() {
        return getString("miss_call_book");
    }
    public static String getWebsocket1() {
        return getString("websocket1_url");
    }
    public static String getWebsocket2() {
        return getString("websocket2_url");
    }

    public static String getTelegramChatID() {
        return getString("telegram_chat_id");
    }

    public static String getSmsSenderKeyword() {
        return getString("sms_sender_keyword");
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