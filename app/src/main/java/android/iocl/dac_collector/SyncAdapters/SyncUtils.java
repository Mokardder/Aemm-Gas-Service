package android.iocl.dac_collector.SyncAdapters;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;

public class SyncUtils {
    public static final String EXTRA_CHAIN_DEPTH = "chain_depth";
    public static final int MAX_CHAIN_DEPTH = 8;

    public static void initialize(android.content.Context context) {
        AccountContract.createSyncBothAccount(context);
    }

    public static void triggerImmediateSync(Account account) {
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(account, AccountContract.AUTHORITY, settingsBundle);
    }

    public static void triggerChainedSync(Account account, int nextDepth) {
        Bundle settingsBundle = new Bundle();
        settingsBundle.putInt(EXTRA_CHAIN_DEPTH, nextDepth);
        ContentResolver.requestSync(account, AccountContract.AUTHORITY, settingsBundle);
    }
}