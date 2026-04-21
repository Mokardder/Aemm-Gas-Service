package android.iocl.dac_collector.RetrofitClient;

import android.content.Context;
import android.iocl.dac_collector.Interface.CapturingInterceptor;
import android.iocl.dac_collector.Interface.ConnectivityInterceptor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit;
    private static final String BASE_URL = "https://script.google.com/";

    public static Retrofit retrofit_spreadsheet(Context context) {

        CapturingInterceptor capturer = new CapturingInterceptor();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .addInterceptor(new ConnectivityInterceptor(context))
                .addInterceptor(capturer)
                .build();

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }

        return retrofit;
    }
}