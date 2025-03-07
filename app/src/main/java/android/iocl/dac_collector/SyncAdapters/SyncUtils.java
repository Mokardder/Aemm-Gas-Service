package android.iocl.dac_collector.SyncAdapters;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

public class SyncUtils {
    public static void initialize(Context context) {
        AccountManager accountManager = AccountManager.get(context);

        // Setup first account
        Account account1 = AccountContract.getAccount();
        if (accountManager.addAccountExplicitly(account1, null, null)) {
            ContentResolver.setIsSyncable(account1, AccountContract.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account1, AccountContract.AUTHORITY, true);
            setSyncInterval(account1, 2 * 60); // 120 seconds for account1
            triggerImmediateSync(account1);
        }

        // Setup second account
        Account account2 = AccountContract.getSecondAccount();
        if (accountManager.addAccountExplicitly(account2, null, null)) {
            ContentResolver.setIsSyncable(account2, AccountContract.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account2, AccountContract.AUTHORITY, true);
            setSyncInterval(account2, 2 * 60); // 120 seconds for account2
            triggerImmediateSync(account2);
        }
    }

    public static void triggerImmediateSync(Account account) {
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(
                account,
                AccountContract.AUTHORITY,
                settingsBundle
        );
    }

    // Overload method to trigger sync for both accounts if needed
    public static void triggerImmediateSync() {
        triggerImmediateSync(AccountContract.getAccount());
        triggerImmediateSync(AccountContract.getSecondAccount());
    }

    public static void setSyncInterval(Account account, int seconds) {
        ContentResolver.addPeriodicSync(
                account,
                AccountContract.AUTHORITY,
                Bundle.EMPTY,
                seconds
        );
    }
}