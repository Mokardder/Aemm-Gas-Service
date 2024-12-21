package android.iocl.dac_collector.Services;


import static android.iocl.dac_collector.Services.JobSchedulerUtil.Sms_and_Call_sender;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.iocl.dac_collector.Utility.Utility;


public class SmsSenderJOBService extends JobService {


    private boolean jobCancelled = false;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {


        doTheThing(jobParameters);

        return true;
    }

    private void doTheThing(JobParameters jobParameters) {
        if (!jobCancelled) {

            Utility.getDACMessages(getApplicationContext());

        }

        jobFinished(jobParameters, true);
    }


    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        jobCancelled = true;

        Sms_and_Call_sender(getApplicationContext());
        return true;
    }


}