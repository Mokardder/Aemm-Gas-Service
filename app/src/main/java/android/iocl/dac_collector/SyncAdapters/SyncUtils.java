package android.iocl.dac_collector.SyncAdapters;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;

public class SyncUtils {
    public static final String EXTRA_CHAIN_DEPTH = "chain_depth";
    public static final String EXTRA_CHAIN_SOURCE = "chain_source";

    public static void initialize(android.content.Context context) {
        AccountContract.createSyncBothAccount(context);
    }

    public static void triggerImmediateSync(Account account) {
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(account, AccountContract.AUTHORITY, settingsBundle);
    }

    public static void triggerChainedSync(Account account, int nextDepth, String sourceAccountType) {
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putInt(EXTRA_CHAIN_DEPTH, nextDepth);
        settingsBundle.putString(EXTRA_CHAIN_SOURCE, sourceAccountType);
        ContentResolver.requestSync(account, AccountContract.AUTHORITY, settingsBundle);
    }


}