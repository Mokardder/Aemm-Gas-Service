package android.iocl.keepAlive;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ServiceB extends Service {
    private static final String TAG = "ServiceB";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Forwarding legacy KeepAlive start request to ServiceA");
        ServiceA.start(this);
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Legacy KeepAlive relay stopped");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
