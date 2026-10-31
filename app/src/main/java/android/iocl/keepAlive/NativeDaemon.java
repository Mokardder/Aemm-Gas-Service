package android.iocl.keepAlive;


import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.io.File;

public class NativeDaemon {
    static {
        System.loadLibrary("keepalive");
    }

    public static native void startWatch(int targetPid, String pkgName);

    // Watch the suicide file to restart the daemon if it dies
    public static void startSelfHealing(Context context) {
        File suicideFile = new File("/data/local/tmp/keepalive_watchdog_suicide");

        // Use ContentObserver to watch for changes
        ContentResolver resolver = context.getContentResolver();
        resolver.registerContentObserver(
                Uri.fromFile(suicideFile),
                true,
                new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange, Uri uri) {
                        // Daemon died, restart it
                        Log.i("KeepAliveNative", "Watchdog died. Restarting...");
                        startWatch(Process.myPid(), context.getPackageName());
                    }
                });
    }
}