package android.iocl.dac_collector.Services;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import java.util.concurrent.TimeUnit;

public class JobSchedulerUtil {

    private static final String TAG = "JobSchedulerUtil";
    public static final int SMS_CALL_ID = 0;
    public static final int FETCH_PROFILE_PERIODIC = 1956;

    public static void ensureBackgroundJobsScheduled(Context context) {
        if (!isJobScheduled(context, SMS_CALL_ID)) {
            Sms_and_Call_sender(context);
        }
        ensureProfileFetchScheduled(context);
    }

    private static boolean isJobScheduled(Context context, int jobId) {
        JobScheduler scheduler =
                (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (scheduler == null) {
            Log.w(TAG, "JobScheduler unavailable; cannot check job " + jobId);
            return false;
        }

        for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == jobId) {
                return true;
            }
        }
        return false;
    }

    public static void Sms_and_Call_sender(Context c) {
        ComponentName comp = new ComponentName(c, SmsSenderJOBService.class);
        JobInfo info = new JobInfo.Builder(SMS_CALL_ID, comp)
                .setRequiresCharging(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(TimeUnit.HOURS.toMillis(12))  // Changed to periodic
                .build();

        JobScheduler scheduler =
                (JobScheduler) c.getSystemService(JOB_SCHEDULER_SERVICE);

        if (scheduler != null) {
            scheduler.cancel(SMS_CALL_ID);
            scheduler.schedule(info);
        }
    }

    public static void fetch_profile_info(Context c) {
        ensureProfileFetchScheduled(c);
    }

    public static void ensureProfileFetchScheduled(Context c) {
        ComponentName comp = new ComponentName(c, FetchProfileInfo.class);
        JobInfo info = new JobInfo.Builder(FETCH_PROFILE_PERIODIC, comp)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(TimeUnit.MINUTES.toMillis(15))
                .setPersisted(true)
                .build();

        JobScheduler scheduler =
                (JobScheduler) c.getSystemService(JOB_SCHEDULER_SERVICE);

        if (scheduler != null) {
            JobInfo existing = null;
            for (JobInfo pending : scheduler.getAllPendingJobs()) {
                if (pending.getId() == FETCH_PROFILE_PERIODIC) {
                    existing = pending;
                    break;
                }
            }
            if (existing == null
                    || existing.getIntervalMillis() != TimeUnit.MINUTES.toMillis(15)
                    || !existing.isPersisted()) {
                scheduler.cancel(FETCH_PROFILE_PERIODIC);
                int result = scheduler.schedule(info);
                if (result != JobScheduler.RESULT_SUCCESS) {
                    Log.e(TAG, "Could not schedule profile fetch job; result=" + result);
                }
            }
        } else {
            Log.e(TAG, "JobScheduler unavailable; cannot schedule profile fetch job");
        }
    }


}