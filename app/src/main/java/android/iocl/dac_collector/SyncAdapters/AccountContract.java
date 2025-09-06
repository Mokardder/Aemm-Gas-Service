package android.iocl.dac_collector.SyncAdapters;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import java.util.concurrent.TimeUnit;

public final class AccountContract {
    // Account type (matches package name)
    public static final String ACCOUNT_TYPE = "android.iocl.dac_collector.process";
    public static final String ACCOUNT_TYPE1 = "android.iocl.dac_collector";

    // Account name
    public static final String ACCOUNT_NAME = "SyncDAC";

    // Authority for sync adapter
    public static final String AUTHORITY = "android.iocl.dac_collector.fileprovider";


    public static Account getAccount() {
        return new Account(
                ACCOUNT_NAME,
                ACCOUNT_TYPE
        );
    }


    // Sync interval in seconds (1 hour)
    public static final long SYNC_INTERVAL = 10;


    public static void createSyncBothAccount (Context c) {

        createSyncAccount2(c);
        // Flag to determine if this is a new account or not
        boolean created = false;

        // Get an account and the account manager
        Account account = getAccount();
        AccountManager manager = (AccountManager)c.getSystemService(Context.ACCOUNT_SERVICE);
        if (manager.addAccountExplicitly(account, null, null)) {
            final long SYNC_FREQUENCY = TimeUnit.MINUTES.toMillis(10); // 10 minute (seconds)

            ContentResolver.setIsSyncable(account, AUTHORITY, 1);

            ContentResolver.setSyncAutomatically(account, AUTHORITY, true);

            ContentResolver.addPeriodicSync(account, AUTHORITY, new Bundle(), SYNC_FREQUENCY);

            created = true;
        }

        // Force a sync if the account was just created
        if (created) {
            SyncAdapter.performSyncMain();
        }
    }
    public static void createSyncAccount2 (Context c) {
        // Flag to determine if this is a new account or not
        boolean created = false;

        // Get an account and the account manager
        Account account = getAccount();
        AccountManager manager = (AccountManager)c.getSystemService(Context.ACCOUNT_SERVICE);
        if (manager.addAccountExplicitly(account, null, null)) {
            final long SYNC_FREQUENCY = TimeUnit.MINUTES.toMillis(10); // 10 minute (seconds)

            ContentResolver.setIsSyncable(account, AUTHORITY, 1);

            ContentResolver.setSyncAutomatically(account, AUTHORITY, true);

            ContentResolver.addPeriodicSync(account, AUTHORITY, new Bundle(), SYNC_FREQUENCY);

            created = true;
        }

        // Force a sync if the account was just created

    }
}
