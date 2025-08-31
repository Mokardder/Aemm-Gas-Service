package android.iocl.dac_collector.Services;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.List;

public final class PersistentVpnServiceUtil {
    private static final String TAG = "PersistentVpnUtil";
    private static final String PREFS = "persistent_vpn_prefs";

    private PersistentVpnServiceUtil() {}

    public static void startService(Context ctx) {
        // mark that service should restart if it dies
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean("vpn_service_should_restart", true).apply();

        Intent svc = new Intent(ctx, PersistentVpnService.class);
        svc.setAction(PersistentVpnService.ACTION_START);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                ctx.startForegroundService(svc);
            } catch (Exception ex) {
                // fallback
                ctx.startService(svc);
            }
        } else {
            ctx.startService(svc);
        }

        prefs.edit().putBoolean("vpn_service_running", true).apply();
        Log.d(TAG, "Requested VPN service start");
    }

    public static void stopService(Context ctx) {
        // don't restart after stop
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean("vpn_service_should_restart", false).apply();

        Intent svc = new Intent(ctx, PersistentVpnService.class);
        svc.setAction(PersistentVpnService.ACTION_DISCONNECT);
        ctx.startService(svc);

        prefs.edit().putBoolean("vpn_service_running", false).apply();
        Log.d(TAG, "Requested VPN service stop");
    }

    public static void restartService(Context ctx) {
        stopService(ctx);
        // small delay to ensure stop propagates; then start
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        startService(ctx);
    }

    public static boolean isMarkedRunning(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean("vpn_service_running", false);
    }

    // Best-effort check. NOTE: getRunningServices is deprecated from O; prefer prefs marker above in modern apps.
    public static boolean isServiceActuallyRunning(Context ctx, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return false;
        List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
        if (services == null) return false;
        for (ActivityManager.RunningServiceInfo service : services) {
            ComponentName name = service.service;
            if (name != null && name.getClassName().equals(serviceClass.getName())) {
                return true;
            }
        }
        return false;
    }
}
