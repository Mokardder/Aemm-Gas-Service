package android.iocl.dac_collector.Interface;

public interface ResponseListener {
    /**
     * Called on the OkHttp thread whenever a response comes back.
     * @param url the request URL
     * @param body the full response body as a String
     */
    void onResponse(String url, String body);
}
