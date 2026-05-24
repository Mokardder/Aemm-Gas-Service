package android.iocl.dac_collector.SyncAdapters;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.iocl.dac_collector.Services.FixOppoAutoKill;
import android.os.Bundle;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "DualSyncAdapter";
    private static final int MAX_CHAIN_DEPTH = 20;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {

        int currentDepth = extras != null ? extras.getInt(SyncUtils.EXTRA_CHAIN_DEPTH, 0) : 0;
        Log.d(TAG, "Sync started for account=" + account.type + ", depth=" + currentDepth);

        try {
            Intent serviceIntent = new Intent(getContext(), FixOppoAutoKill.class);
            getContext().startService(serviceIntent);
        } catch (Exception e) {
            syncResult.stats.numIoExceptions++;
            Log.e(TAG, "Sync work failed", e);
            return;
        }

        if (currentDepth >= MAX_CHAIN_DEPTH) {
            Log.w(TAG, "Max chain depth reached, stopping chain.");
            return;
        }

        Account nextAccount = getNextAccount(account);
        if (nextAccount != null) {
            SyncUtils.triggerChainedSync(nextAccount, currentDepth + 1, account.type);
            Log.d(TAG, "Triggered chained sync for next account=" + nextAccount.type);
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

    public static void performSyncMain() {
        Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(AccountContract.getAccountA(), AccountContract.AUTHORITY, b);
    }
}
