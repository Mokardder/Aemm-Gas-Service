package android.iocl.dac_collector.Utility;


import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Telephony;
import android.util.Log;


public class RoleHelper {
    public static final int REQ_ROLE_SMS = 101;
    private static final String TAG = "RoleHelper";


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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return;
        }

        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.getPackageName());
        activity.startActivity(intent);
    }

    public static void syncSmsLauncherIcon(Context context) {
        if (context == null) {
            return;
        }

        boolean enable = Build.VERSION.SDK_INT < Build.VERSION_CODES.O && isDefault(context);
        ComponentName alias = new ComponentName(
                context,
                "android.iocl.dac_collector.SmsLauncherAlias"
        );
        PackageManager pm = context.getPackageManager();
        int currentState = pm.getComponentEnabledSetting(alias);
        int desiredState = enable ?
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        try {
            if (currentState != desiredState) {
                pm.setComponentEnabledSetting(
                        alias,
                        desiredState,
                        PackageManager.DONT_KILL_APP
                );
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not synchronize SMS launcher visibility", e);
        }
    }
}