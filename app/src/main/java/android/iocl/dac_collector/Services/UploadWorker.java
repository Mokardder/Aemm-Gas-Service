package android.iocl.dac_collector.Services;

import android.content.Context;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.Utility.Utility;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class UploadWorker extends Worker {
    Context context;

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {

        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        String data = Utility.getUnsentDAC(context);
        if (data != null && isNetworkAvailable(context)) {
            boolean success = sendToServer(data);
            if (success) {

                return Result.success();
            }
        }
        return Result.retry();
    }

    private boolean sendToServer(String data) {

        FirebaseDBClient.syncDac(context,data, "", Utility.getCurrentTime());

        Log.d("WorkerUpload", "sendToServer: Sent To Server");
        return true; // Return true if successful
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities capabilities = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        }
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }
}