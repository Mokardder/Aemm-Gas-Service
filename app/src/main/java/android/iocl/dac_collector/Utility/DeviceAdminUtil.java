package android.iocl.dac_collector.Utility;


import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import org.checkerframework.checker.units.qual.C;

public class DeviceAdminUtil {

    /**
     * Checks if the device admin is enabled for the provided DeviceAdminReceiver.
     *
     * @param context             The application or activity context.
     * @param adminReceiverClass  The class of your DeviceAdminReceiver.
     * @return                    True if device admin is active; false otherwise.
     */
    public static boolean isDeviceAdminEnabled(Context context,
                                               Class<? extends DeviceAdminReceiver> adminReceiverClass) {
        DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(context, adminReceiverClass);
        return devicePolicyManager.isAdminActive(adminComponent);
    }

}
