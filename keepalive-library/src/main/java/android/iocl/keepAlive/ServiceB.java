package android.iocl.keepAlive;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ServiceB extends Service {
    private static final String TAG = "KeepAliveServiceB";
    private static final int NOTIFICATION_ID = 1102;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(
                NOTIFICATION_ID,
                KeepAliveNotifications.build(
                        this,
                        "KeepAlive monitor active",
                        "Monitoring the KeepAlive service"
                )
        );
        KeepAliveNotifications.startService(this, ServiceA.class);
        Log.i(TAG, "KeepAlive monitor started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        KeepAliveNotifications.startService(this, ServiceA.class);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
