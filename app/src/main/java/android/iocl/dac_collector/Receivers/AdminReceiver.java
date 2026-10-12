package android.iocl.dac_collector.Receivers;



import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

import android.util.Log;
import android.widget.Toast;

public class AdminReceiver extends DeviceAdminReceiver {
    private static final String TAG = "AdminReceiver";

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Log.d(TAG, "Device Admin Enabled");
        Toast.makeText(context, "Device Admin Activated", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Log.e(TAG, "Device Admin Disabled - Re-enabling immediately!");

//        // Immediately try to re-enable
//        Intent reEnableIntent = new Intent(context, EnableAdminActivity.class);
//        reEnableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        reEnableIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        context.startActivity(reEnableIntent);
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        super.onPasswordFailed(context, intent);
        Log.w(TAG, "Password failed attempt");
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        super.onPasswordSucceeded(context, intent);
        Log.d(TAG, "Password succeeded");
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        // Prevent disabling by showing warning
        return "WARNING: Disabling admin will lock your device!";
    }
}
