package android.iocl.dac_collector.Utility;

public class Constant {

    public static final String API_FAILURE = "Check your internet !!";
    public static final String ACRA_REPORT_DB_API = "https://script.google.com/macros/s/AKfycbxDW7CuKtFuVZImkpbUTzyVn51YcQqS3Ta9p-fTZzQUF-kiPsByHmaPPU29shlK0Skt/exec";


    public static final String DefaultRegex = "{\r\n  \"patterns\": [\r\n    {\r\n      \"id\": \"DAC\",\r\n      \"regex\": \"Invoice Number # (\\\\d+-\\\\d+) is (\\\\d{6})\",\r\n      \"capture\": \"1, 2\"\r\n    },\r\n    {\r\n      \"id\": \"OTP\",\r\n      \"regex\": \"Your IOCL one time password is :(\\\\d{4})\",\r\n      \"capture\": \"1\"\r\n    },\r\n    {\r\n      \"id\": \"GeneratedDAC\",\r\n      \"regex\": \"Invoice generated for Rs. (\\\\d{3}).Share DAC (\\\\d{6})\",\r\n      \"capture\": \"2\"\r\n    }\r\n  ]\r\n}";

}
