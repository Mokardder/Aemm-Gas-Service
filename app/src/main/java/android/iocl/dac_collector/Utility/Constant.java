package android.iocl.dac_collector.Utility;

public class Constant {
    public static final String InterstitialSmsReceiver = "ca-app-pub-1053541101126785/1768156123";
    public static final String RewardedVideoAnnual = "ca-app-pub-1053541101126785/7776867564";
    public static final String BannerAdHome = "ca-app-pub-1053541101126785/9217840111";
    public static final String DISABLE_COLOR = "#6c757d";
    public static final String API_FAILURE = "Unexpectedly Error Occured ! Check Your Internet";
    public static final String Pref_AnnualPoint = "AnnualPoints";

    public static final String Pref_AdsWatched = "AdsWatched";
    public static final String DefaultRegex = "{\r\n  \"patterns\": [\r\n    {\r\n      \"id\": \"DAC\",\r\n      \"regex\": \"Invoice Number # (\\\\d+-\\\\d+) is (\\\\d{4})\",\r\n      \"capture\": \"1, 2\"\r\n    },\r\n    {\r\n      \"id\": \"OTP\",\r\n      \"regex\": \"Your IOCL one time password is :(\\\\d{4})\",\r\n      \"capture\": \"1\"\r\n    },\r\n    {\r\n      \"id\": \"GeneratedDAC\",\r\n      \"regex\": \"Invoice generated for Rs. (\\\\d{3}).Share DAC (\\\\d{4})\",\r\n      \"capture\": \"2\"\r\n    }\r\n  ]\r\n}";
    ;
    public static final String ENABLE_COLOR = "#007bff";
}
