package android.iocl.dac_collector.Utility;



import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

public final class LauncherIconHelper {

    private LauncherIconHelper() { /* no-inst */ }


    public static void hideLauncherIcon(Context ctx) {
        ComponentName alias = new ComponentName(ctx.getPackageName(), ctx.getPackageName() + ".Ui.LauncherAlias");
        ctx.getPackageManager().setComponentEnabledSetting(
                alias,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
        // optional nudge for some launchers — may help but not guaranteed
        nudgeLauncher(ctx);
    }

    public static void restoreLauncherIcon(Context ctx) {
        ComponentName alias = new ComponentName(ctx.getPackageName(), ctx.getPackageName() + ".Ui.LauncherAlias");
        ctx.getPackageManager().setComponentEnabledSetting(
                alias,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );
        nudgeLauncher(ctx);
    }

    /**
     * Try to nudge the launcher to refresh its icon cache.
     * This is a best-effort, not guaranteed. Some OEM launchers ignore it.
     */
    private static void nudgeLauncher(Context ctx) {
        try {
            Intent intent = new Intent(Intent.ACTION_PACKAGE_CHANGED);
            intent.setData(Uri.parse("package:" + ctx.getPackageName()));
            ctx.sendBroadcast(intent);
        } catch (Exception ignored) {
            // ignore — it's just a nudge
        }
    }
}
