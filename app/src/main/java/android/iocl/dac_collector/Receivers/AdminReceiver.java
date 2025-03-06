package android.iocl.dac_collector.Receivers;



import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class AdminReceiver extends DeviceAdminReceiver {

    private static final String TAG = "MyDeviceAdminReceiver";

    // Utility method to display a Toast message
    private void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    // Called when the user enables this application as a device administrator.
    @Override
    public void onEnabled(Context context, Intent intent) {
        showToast(context, "Device Admin: Enabled");
        Log.d(TAG, "Device admin enabled");
    }

    // Called when the user disables this application as a device administrator.
    @Override
    public void onDisabled(Context context, Intent intent) {
        showToast(context, "Device Admin: Disabled");
        Log.d(TAG, "Device admin disabled");
    }

    // Called when the device's password is changed.
    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        showToast(context, "Device Admin: Password Changed");
        Log.d(TAG, "Password changed");
    }

    // Called when a password attempt fails.
    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        showToast(context, "Device Admin: Password Failed");
        Log.d(TAG, "Password attempt failed");
    }

    // Called when a password attempt succeeds.
    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        showToast(context, "Device Admin: Password Succeeded");
        Log.d(TAG, "Password attempt succeeded");
    }
}
