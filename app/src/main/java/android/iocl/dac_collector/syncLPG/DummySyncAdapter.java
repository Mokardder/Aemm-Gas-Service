package android.iocl.dac_collector.syncLPG;


import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

public class DummySyncAdapter extends AbstractThreadedSyncAdapter {

    public DummySyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras,
                              String authority, ContentProviderClient provider,
                              SyncResult syncResult) {
        Log.d("DummySync", "System triggered sync for account: " + account.name);
        // 🔹 Do your real sync work here (fetch/upload data)
    }
}
