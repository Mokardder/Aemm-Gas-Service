package android.iocl.dac_collector.SyncAdapters;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.iocl.dac_collector.Services.FixOppoAutoKill;
import android.iocl.dac_collector.Services.JobSchedulerUtil;
import android.os.Bundle;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "DualSyncAdapter";

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {

        int depth = extras != null ? extras.getInt(SyncUtils.EXTRA_CHAIN_DEPTH, 0) : 0;
        Log.d(TAG, "Sync started for account=" + account.type + ", depth=" + depth);

        try {
            JobSchedulerUtil.ensureProfileFetchScheduled(getContext());

            Intent serviceIntent = new Intent(getContext(), FixOppoAutoKill.class);
            getContext().startService(serviceIntent);
        } catch (Exception e) {
            syncResult.stats.numIoExceptions++;
            Log.e(TAG, "Sync work failed", e);
            return;
        }

        if (depth >= SyncUtils.MAX_CHAIN_DEPTH) {
            Log.d(TAG, "Sync chain reached maximum depth");
            return;
        }

        Account nextAccount = getNextAccount(account);
        if (nextAccount != null) {
            SyncUtils.triggerChainedSync(nextAccount, depth + 1);
        }
    }

    private Account getNextAccount(Account current) {
        if (AccountContract.ACCOUNT_TYPE.equals(current.type)) {
            return AccountContract.getAccountB();
        }
        if (AccountContract.ACCOUNT_TYPE1.equals(current.type)) {
            return AccountContract.getAccountA();
        }
        return null;
    }
}