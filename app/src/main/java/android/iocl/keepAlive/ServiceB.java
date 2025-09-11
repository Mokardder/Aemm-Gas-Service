package android.iocl.keepAlive;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ServiceB extends Service {
    private static final String TAG = "ServiceB";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ServiceB created");

        // Ensure ServiceA is running
        startService(new Intent(this, ServiceA.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ServiceB running, keeping ServiceA alive");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ServiceB destroyed, restarting ServiceA");
        startService(new Intent(this, ServiceA.class));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
