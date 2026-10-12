package android.iocl.dac_collector.Services;



import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

public class SmsStreamingJobService extends JobService {

    private static volatile boolean running = false;

    public static boolean isRunning() {
        return running;
    }

    private static final String TAG =
            "SmsStreamingJobService";

    public static final int JOB_ID = 2001;

    private WebSocketConnectionManager
            connectionManager;

    @Override
    public boolean onStartJob(
            JobParameters params) {

        running = true;

        try {

            Log.d(
                    TAG,
                    "SMS Streaming Job started"
            );

            String path =
                    params.getExtras().getString(
                            "path"
                    );

            if (path == null ||
                    path.trim().isEmpty()) {

                path = "/websocket";
            }

            connectionManager =
                    WebSocketConnectionManager
                            .getInstance(this);

            connectionManager.start(path);

            Log.d(
                    TAG,
                    "WebSocket started: " + path
            );

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Job start crash",
                    e
            );
        }

        /*
         * Return false because the JobService
         * does not have a separate job running
         * that needs jobFinished().
         */
        return false;
    }

    @Override
    public boolean onStopJob(
            JobParameters params) {

        running = false;

        try {

            Log.d(
                    TAG,
                    "SMS Streaming Job stopped"
            );

            if (connectionManager != null) {

                connectionManager.stop();

                connectionManager = null;
            }

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Job stop crash",
                    e
            );
        }

        /*
         * true = Android should reschedule
         * the job.
         */
        return true;
    }
}