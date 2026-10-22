package android.iocl.dac_collector.SyncAdapters;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public final class AccountContract {
    private static final String TAG = "AccountContract";
    public static final String ACCOUNT_TYPE = "android.iocl.dac_collector.process";
    public static final String ACCOUNT_TYPE1 = "android.iocl.dac_collector";
    public static final String ACCOUNT_NAME = "SyncDAC";
    public static final String ACCOUNT_NAME1 = "SyncDAC2";
    public static final String AUTHORITY = "android.iocl.dac_collector.provider";
    private static final String LEGACY_AUTHORITY = "android.iocl.dac_collector.fileprovider";

    // Android may clamp AccountManager periodic sync intervals below one hour.
    public static final long SYNC_INTERVAL_SECONDS = 15 * 60;

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
        if (manager == null) {
            Log.e(TAG, "AccountManager unavailable; cannot register sync account " + account.type);
            return false;
        }

        try {
            boolean created = manager.addAccountExplicitly(account, null, null);
            boolean accountExists = created;
            if (!accountExists) {
                for (Account existing : manager.getAccountsByType(account.type)) {
                    if (account.equals(existing)) {
                        accountExists = true;
                        break;
                    }
                }
            }

            if (!accountExists) {
                Log.e(TAG, "Could not create or find sync account " + account.type);
                return false;
            }

            boolean needsInitialSync =
                    created || ContentResolver.getIsSyncable(account, AUTHORITY) != 1;
            ContentResolver.removePeriodicSync(account, LEGACY_AUTHORITY, Bundle.EMPTY);
            ContentResolver.setSyncAutomatically(account, LEGACY_AUTHORITY, false);
            ContentResolver.setIsSyncable(account, LEGACY_AUTHORITY, 0);
            ContentResolver.setIsSyncable(account, AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, AUTHORITY, true);
            ContentResolver.addPeriodicSync(
                    account,
                    AUTHORITY,
                    Bundle.EMPTY,
                    SYNC_INTERVAL_SECONDS
            );
            return needsInitialSync;
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to configure sync account " + account.type, e);
            return false;
        }
    }
}