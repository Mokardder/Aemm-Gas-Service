package android.iocl.dac_collector.syncLPG;



import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class DummySyncService extends Service {
    private static final Object sLock = new Object();
    private static DummySyncAdapter sSyncAdapter = null;

    @Override
    public void onCreate() {
        synchronized (sLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new DummySyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
