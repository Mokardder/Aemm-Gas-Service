package android.iocl.keepAlive;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

public class ServiceA extends Service {
    private static final String TAG = "KeepAliveServiceA";
    private static final int NOTIFICATION_ID = 1101;

    public static void start(Context context) {
        KeepAliveNotifications.startService(context, ServiceA.class);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(
                NOTIFICATION_ID,
                KeepAliveNotifications.build(
                        this,
                        "KeepAlive active",
                        "Background keep-alive service is running"
                )
        );
        new NativeDaemon().startWatch(Process.myPid(), getPackageName());
        KeepAliveNotifications.startService(this, ServiceB.class);
        Log.i(TAG, "KeepAlive service started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        KeepAliveNotifications.startService(this, ServiceB.class);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
