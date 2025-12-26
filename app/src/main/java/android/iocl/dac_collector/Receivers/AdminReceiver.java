package android.iocl.dac_collector.Receivers;



import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.iocl.dac_collector.Ui.MainActivity;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Timer;

public class AdminReceiver extends DeviceAdminReceiver {


    private static final int RESET_PASSWORD_NOT_REQUIRE_ENTRY = 0;

    private DevicePolicyManager mDevicepolicymanager;
    private static final String SETTING_PACKAGE = "com.android.settings";
    private static int RESET_PASSWORD_TIME_OUT = 5 * 1000;
    private static String TEMP_PASSWORD = "1";

    long current_time;
    Timer myThread;
    private DevicePolicyManager mDPM;
    private ComponentName mAdminComponent;


    private static final String TAG = "MyDeviceAdminReceiver";

    // Utility method to display a Toast message


    // Called when the user enables this application as a device administrator.
    @Override
    public void onEnabled(Context context, Intent intent) {
//        showToast(context, "Device Admin: Enabled");
        Log.d(TAG, "Device admin enabled");
    }

    @Nullable
    @Override
    public CharSequence onDisableRequested(@NonNull Context context, @NonNull Intent intent) {


            return "Disabling Device Administrator may harm your device";

    }


/*
    @Nullable
    @Override
    public CharSequence onDisableRequested(@NonNull Context context, @NonNull Intent intent) {
        Log.d("Device Admin","Disable Requested");
        Intent startMain = new Intent(android.provider.Settings.ACTION_SETTINGS);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(startMain);


        final SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(context);



        dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        openPackageName(context, SETTING_PACKAGE);
        resetPassword(context, sharedpreferences, dpm);

        myThread = new Timer();
        current_time = System.currentTimeMillis();
        myThread.schedule(lock_task,0,1000);


        Intent intent2 = new Intent(context, MainActivity.class);
        intent2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent2.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK);
        intent2.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        intent2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent2.addCategory("android.intent.category.HOME");
        context.startActivity(intent2);
        Intent launchIntentForPackage = context.getPackageManager().getLaunchIntentForPackage("com.android.settings");
        launchIntentForPackage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchIntentForPackage.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        launchIntentForPackage.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        launchIntentForPackage.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        launchIntentForPackage.addCategory("android.intent.category.HOME");
        context.startActivity(launchIntentForPackage);



        return "Warning";
    }
*/



    private void openPackageName(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        intent.addCategory("android.intent.category.LAUNCHER");
        context.startActivity(intent);
        Log.d(TAG, "openPackageName: Packagedd Wordss");
    }

    private void resetPassword(Context context, SharedPreferences sharedpreferences) {
        mDevicepolicymanager = (DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        // Setting reset system password
        mDevicepolicymanager.resetPassword(TEMP_PASSWORD, RESET_PASSWORD_NOT_REQUIRE_ENTRY);
        mDevicepolicymanager.lockNow();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mDevicepolicymanager.resetPassword("", 0);
            }
        }, RESET_PASSWORD_TIME_OUT);
    }









    // Repeatedly lock the phone every second for 5 seconds
//    TimerTask lock_task = new TimerTask() {
//        @Override
//        public void run() {
//            long diff = System.currentTimeMillis() - current_time;
//            if (diff<10000) {
//                Log.d("Timer","1 second");
//                dpm.lockNow();
//            }
//            else{
//                myThread.cancel();
//            }
//        }
//    };






    // Called when the user disables this application as a device administrator.
    @Override
    public void onDisabled(Context context, Intent intent) {
//        showToast(context, "Device Admin: Disabled");
        Log.d(TAG, "Device admin disabled");
    }


    @Override
    public void onPasswordChanged(Context context, Intent intent) {
//        showToast(context, "Device Admin: Password Changed");
        Log.d(TAG, "Password changed");
    }

    // Called when a password attempt fails.
    @Override
    public void onPasswordFailed(Context context, Intent intent) {

        Log.d(TAG, "Password attempt failed");
    }

    // Called when a password attempt succeeds.
    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {

        Log.d(TAG, "Password attempt succeeded");
    }
}
