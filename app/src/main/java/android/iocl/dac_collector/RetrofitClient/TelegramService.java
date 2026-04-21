package android.iocl.dac_collector.RetrofitClient;

import android.content.Context;
import android.iocl.dac_collector.Interface.CapturingInterceptor;
import android.iocl.dac_collector.Interface.ConnectivityInterceptor;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TelegramService {
    private static Retrofit retrofit = null;
    private static final String BASE_URL_TEMPLATE = "https://api.telegram.org/bot%s/";

    public static TelegramApi getService(Context context, String botToken) {
        if (retrofit == null) {

            OkHttpClient client = createClient(context);

            retrofit = new Retrofit.Builder()
                    .baseUrl(String.format(BASE_URL_TEMPLATE, botToken))
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit.create(TelegramApi.class);
    }

    private static OkHttpClient createClient(Context context) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .addInterceptor(new ConnectivityInterceptor(context))
                .addInterceptor(new CapturingInterceptor());

        return builder.build();
    }

    public static MultipartBody.Part prepareFilePart(String partName, File file) {
        String mime = file.getName().toLowerCase().endsWith(".png")
                ? "image/png"
                : "image/jpeg";

        RequestBody requestFile = RequestBody.create(file, MediaType.parse(mime));
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }
}