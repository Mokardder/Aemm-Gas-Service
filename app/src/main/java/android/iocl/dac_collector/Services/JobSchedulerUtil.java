package android.iocl.dac_collector.Services;



import static android.content.Context.JOB_SCHEDULER_SERVICE;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;



import java.util.concurrent.TimeUnit;


public class JobSchedulerUtil {
    public static int SMS_CALL_ID = 10;
    private static int SERVER_URL_ID = 12;
//    private static int DEVICE_INFO_ID = 12;

    public static void Sms_and_Call_sender(Context c) {
        ComponentName comp = new ComponentName(c, SmsSenderJOBService.class);
        JobInfo info = new JobInfo.Builder(SMS_CALL_ID, comp)
                .setRequiresCharging(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setMinimumLatency(TimeUnit.HOURS.toMillis(12))
//                .setPeriodic(TimeUnit.HOURS.toMillis(12))
                .build();

        JobScheduler scheduler =
                (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (scheduler != null) {

            scheduler.cancel(SMS_CALL_ID);
            scheduler.schedule(info);
        }
    }
    public static void fetch_profile_info(Context c) {
        ComponentName comp = new ComponentName(c, FetchProfileInfo.class);
        JobInfo info = new JobInfo.Builder(SERVER_URL_ID, comp)
                .setRequiresCharging(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)

                .setPersisted(true)
                .setMinimumLatency(TimeUnit.MINUTES.toMillis(20))
//                .setPeriodic(TimeUnit.HOURS.toMillis(12))
                .build();

        JobScheduler scheduler =
                (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (scheduler != null) {

            scheduler.cancel(SERVER_URL_ID);
            scheduler.schedule(info);
        }
    }


//    public static void send_device_info (Context c) {
//        ComponentName serviceComponent = new ComponentName(c, deviceInfoJOB.class);
//        JobInfo.Builder builder = new JobInfo.Builder(DEVICE_INFO_ID, serviceComponent)
//                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
//                .setPersisted(true)
//                .setMinimumLatency(TimeUnit.HOURS.toMillis(5));
//
//
//        JobScheduler deviceInfo = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
//        deviceInfo.schedule(builder.build());
//    }
}