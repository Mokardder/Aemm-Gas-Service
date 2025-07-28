// android/iocl/dac_collector/RetrofitClient/TelegramApi.java

package android.iocl.dac_collector.RetrofitClient;

import android.iocl.dac_collector.ModelData.TelegramResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

/**
 * Retrofit interface for Telegram Bot API methods.
 */
public interface TelegramApi {
    @POST("sendMessage")
    Call<TelegramResponse> sendMessage(
            @Query("chat_id") String chatId,
            @Query("text") String text
    );


    @Multipart
    @POST("sendPhoto")
    Call<TelegramResponse> sendPhoto(
            @Part("chat_id") RequestBody chatId,
            @Part MultipartBody.Part photo,
            @Part("caption") RequestBody caption
    );
}
