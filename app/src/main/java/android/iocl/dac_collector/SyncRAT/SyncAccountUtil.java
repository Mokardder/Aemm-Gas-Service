package android.iocl.dac_collector.SyncRAT;



import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

public class SyncAccountUtil {
    public static Account getSyncAccount(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account account = new Account(Config.Sync.ACCOUNT, Config.Sync.ACCOUNT_TYPE);

        if (accountManager.addAccountExplicitly(account, null, null)) {
            ContentResolver.setIsSyncable(account, Config.Sync.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, Config.Sync.AUTHORITY, true);
        }
        return account;
    }


}