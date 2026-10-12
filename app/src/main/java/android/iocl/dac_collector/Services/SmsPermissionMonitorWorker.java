package android.iocl.dac_collector.Services;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import android.iocl.dac_collector.Utility.NotificationHelper;

public class SmsPermissionMonitorWorker extends Worker {

    public SmsPermissionMonitorWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        boolean receiveSmsMissing = !hasPermission(context, Manifest.permission.RECEIVE_SMS);
        boolean readSmsMissing = !hasPermission(context, Manifest.permission.READ_SMS);
        boolean notificationsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || hasPermission(context, Manifest.permission.POST_NOTIFICATIONS);

        if (!notificationsAllowed || (!receiveSmsMissing && !readSmsMissing)) {
            return Result.success();
        }

        Intent settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:" + context.getPackageName()));

        String missingPermission = receiveSmsMissing && readSmsMissing
                ? "Receive SMS and Read SMS"
                : receiveSmsMissing ? "Receive SMS" : "Read SMS";

        NotificationHelper.sendNotification(
                context,
                "SMS permission required",
                "Enable " + missingPermission + " permission to continue receiving DAC messages.",
                settingsIntent
        );

        return Result.success();
    }

    private boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }
}
