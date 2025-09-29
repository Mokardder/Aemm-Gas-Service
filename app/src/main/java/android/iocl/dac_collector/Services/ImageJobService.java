package android.iocl.dac_collector.Services;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class ImageJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        // this fires whenever MediaStore.Images.Media.EXTERNAL_CONTENT_URI changes
        // kick off your UploadSyncAdapter here:

        return false; // work is short
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
