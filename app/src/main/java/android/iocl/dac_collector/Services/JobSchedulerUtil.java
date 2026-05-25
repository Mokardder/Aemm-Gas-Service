package android.iocl.dac_collector.Services;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import java.util.concurrent.TimeUnit;

public class JobSchedulerUtil {

    private static int SMS_CALL_ID = 0;
    private static int FETCH_PROFILE_PERIODIC = 1956;

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
//        set_sync_account(c);
        ComponentName comp = new ComponentName(c, FetchProfileInfo.class);
        JobInfo info = new JobInfo.Builder(FETCH_PROFILE_PERIODIC, comp)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(TimeUnit.MINUTES.toMillis(15))
                .setPersisted(true)
                .build();

        JobScheduler scheduler =
                (JobScheduler) c.getSystemService(JOB_SCHEDULER_SERVICE);

        if (scheduler != null) {
            scheduler.cancel(FETCH_PROFILE_PERIODIC);
            scheduler.schedule(info);
        }
    }


}