package android.iocl.dac_collector.SyncAdapters;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

public final class AccountContract {
    public static final String ACCOUNT_TYPE = "android.iocl.dac_collector.process";
    public static final String ACCOUNT_TYPE1 = "android.iocl.dac_collector";
    public static final String ACCOUNT_NAME = "SyncDAC";
    public static final String ACCOUNT_NAME1 = "SyncDAC2";
    public static final String AUTHORITY = "android.iocl.dac_collector.fileprovider";

    // addPeriodicSync interval is in seconds.
    public static final long SYNC_INTERVAL_SECONDS = 15 * 60; // 15 minutes

    private AccountContract() {
    }

    public static Account getAccount() {
        return getAccountA();
    }

    public static Account getAccountA() {
        return new Account(ACCOUNT_NAME, ACCOUNT_TYPE);
    }

    public static Account getAccountB() {
        return new Account(ACCOUNT_NAME1, ACCOUNT_TYPE1);
    }

    public static void createSyncBothAccount(Context c) {
        boolean createdA = createOrEnsureSyncAccount(c, getAccountA());
        boolean createdB = createOrEnsureSyncAccount(c, getAccountB());

        if (createdA) {
            SyncUtils.triggerImmediateSync(getAccountA());
        }

        if (createdB) {
            SyncUtils.triggerImmediateSync(getAccountB());
        }
    }

    public static boolean createOrEnsureSyncAccount(Context c, Account account) {
        AccountManager manager = (AccountManager) c.getSystemService(Context.ACCOUNT_SERVICE);
        boolean created = false;

        if (manager != null && manager.addAccountExplicitly(account, null, null)) {
            created = true;
        }

        ContentResolver.setIsSyncable(account, AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(account, AUTHORITY, true);
        ContentResolver.addPeriodicSync(account, AUTHORITY, Bundle.EMPTY, SYNC_INTERVAL_SECONDS);

        return created;
    }
}
