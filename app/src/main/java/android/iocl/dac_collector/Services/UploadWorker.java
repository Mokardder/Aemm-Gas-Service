package android.iocl.dac_collector.Services;

import android.content.Context;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.Utility.PermissionUtility;
import android.iocl.dac_collector.Utility.Utility;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


import java.util.List;

public class UploadWorker extends Worker {
    private static final String TAG = "UploadWorker";
    private final Context context;

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {

        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        String type = getInputData().getString("type");
        try {
            Log.d(TAG, "doWork: " + type);
            if ("DAC_OUTBOX".equals(type)) {
                while (FirebaseDBClient.uploadNextPendingDac()) {
                    if (isStopped()) {
                        return Result.retry();
                    }
                }
                return Result.success();
            } else if ("DAC".equals(type)) {
                Utility.sendAnyUnsentDAC(context);
                return Result.success();
            } else if ("PERM".equals(type)) {
                List<String> perms =  PermissionUtility.getMissingPermissions(context, false);
                FirebaseDBClient.updateAppPermissions(perms);
                return Result.success();
            } else {
                Log.e(TAG, "Unknown upload type: " + type);
                return Result.failure();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "DAC upload interrupted; it will be retried", e);
            return Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "Upload worker failed for type " + type, e);
            return Result.retry();
        }
    }
}