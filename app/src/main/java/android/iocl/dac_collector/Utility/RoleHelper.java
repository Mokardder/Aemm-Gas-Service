package android.iocl.dac_collector.Utility;


import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;


public class RoleHelper {
    public static final int REQ_ROLE_SMS = 101;


    public static boolean isDefault(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager rm = ctx.getSystemService(RoleManager.class);
            return rm != null && rm.isRoleHeld(RoleManager.ROLE_SMS);
        } else {
            String def = Telephony.Sms.getDefaultSmsPackage(ctx);
            return ctx.getPackageName().equals(def);
        }
    }


    public static void requestRole(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager rm = activity.getSystemService(RoleManager.class);
            if (rm != null && !rm.isRoleHeld(RoleManager.ROLE_SMS)) {
                Intent i = rm.createRequestRoleIntent(RoleManager.ROLE_SMS);
                activity.startActivityForResult(i, REQ_ROLE_SMS);
            }
        } else {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.getPackageName());
            activity.startActivity(intent);
        }
    }
    public static void enableSmsLauncherIcon(Context context, boolean enable) {
        ComponentName alias = new ComponentName(context, "android.iocl.dac_collector.SmsLauncherAlias");
        PackageManager pm = context.getPackageManager();

        int state = enable ?
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        pm.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP);

        // Save preference
//        SharedPrefs.setAppIconStatus(context, enable);

        // Try to refresh launcher
        refreshLauncher(context);
    }

    private static void refreshLauncher(Context context) {
        try {
            Intent intent = new Intent("android.intent.action.PACKAGE_CHANGED");
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}