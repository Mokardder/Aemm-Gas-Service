package android.iocl.dac_collector.Interface;


import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CapturingInterceptor implements Interceptor {
    private static ResponseListener globalListener;

    public static void setGlobalListener(ResponseListener listener) {
        globalListener = listener;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        ResponseBody peeked = response.peekBody(Long.MAX_VALUE);
        String bodyString = peeked.string();

        if (globalListener != null) {
            globalListener.onResponse(request.url().toString(), bodyString);
        }

        return response;
    }
}
