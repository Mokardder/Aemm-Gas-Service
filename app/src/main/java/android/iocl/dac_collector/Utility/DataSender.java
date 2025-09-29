package android.iocl.dac_collector.Utility;

import android.content.Context;
import android.iocl.dac_collector.Services.UploadWorker;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class DataSender {

    public static void sendData(Context context, String dac, String cashmemo) {
            scheduleUpload(context);
    }

    private static void scheduleUpload(Context context) {
        Log.d("WorkerUpload", "scheduleUpload: Scheduled Successfully");
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest uploadWork =
                new OneTimeWorkRequest.Builder(UploadWorker.class)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueue(uploadWork);
    }


}