package android.iocl.dac_collector.Utility;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.ModelData.PermissionItem;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Services.AcessibilitySettings;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PermissionUtility {

    private static final int REQUEST_CODE_STORAGE = 101;
    private static final int REQUEST_CODE_INSTALL = 102;

    /**
     * Toggle to control whether the app enforces an Always-on VPN check.
     * <p>
     * Default: true (existing behavior — the app will check and may add "always_on_vpn" to missing permissions)
     * If you set this to false, isAlwaysOnVpnEnabled(...) will short-circuit to `true` so callers won't mark VPN as missing.
     * <p>
     * Usage example:
     * PermissionUtility.setEnforceAlwaysOnVpn(false); // disable enforcement (likely in Application.onCreate)
     */
    private static volatile boolean ENFORCE_ALWAYS_ON_VPN = true;


    /**
     * Request storage permissions (Read & Write)
     */
    public static void requestStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (Scoped Storage override)
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    activity.startActivityForResult(intent, REQUEST_CODE_STORAGE);
                } catch (Exception e) {
                    // Fallback if above intent fails
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivityForResult(intent, REQUEST_CODE_STORAGE);
                }
            }
        } else {
            // Android 10 and below
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        REQUEST_CODE_STORAGE
                );
            }
        }
    }

    public static void openNotificationSettings(Context context) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ opens app-specific notification settings
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            // Older versions open app details settings
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + context.getPackageName()));
        }

        // Make sure to start activity from a valid context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void openAppInfo(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // In case context is not an Activity
        context.startActivity(intent);
    }


    public static boolean isMyImeEnabled(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return false;
        List<InputMethodInfo> enabled = imm.getEnabledInputMethodList();
        for (InputMethodInfo info : enabled) {
            if (info.getPackageName().equals(context.getPackageName())) {
                // optionally also check the service class name: info.getId() contains component name
                return true;
            }
        }
        return false;
    }

    public static void openAccountSyncPage(Context context) {
        Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void openVPNSetting(Context context) {
        Intent intent = new Intent("android.net.vpn.SETTINGS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            // Fallback: open main settings if the VPN screen isn't found
            context.startActivity(new Intent(Settings.ACTION_VPN_SETTINGS));
        }

        SharedPrefs.setVPNAlways(context, true);

    }


    /**
     * Request install unknown apps permission
     */
    public static void requestInstallPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.setData(Uri.parse(String.format("package:%s", activity.getPackageName())));
                activity.startActivityForResult(intent, REQUEST_CODE_INSTALL);
            }
        }
    }

    /**
     * Request battery optimization ignore permission
     */

    /**
     * Request Device Admin
     */
    public static void requestDeviceAcmin(Context activity) {
        enableDeviceAdmin(activity);
    }

    /**
     * Request Accessibility Permission
     */
    public static void requestAccessibility(Context activity) {
        activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));

    }

    /**
     * Request all essential runtime permissions
     */
    public static void requestEssentialPermissions(Activity activity) {
        try {
            XXPermissions.with(activity)
                    .permission(Permission.READ_PHONE_STATE)
                    .permission(Permission.READ_SMS)
                    .permission(Permission.BIND_VPN_SERVICE)
                    .permission(Permission.RECEIVE_SMS)
                    .permission(Permission.READ_CONTACTS)
                    .permission(Permission.CALL_PHONE)
                    .permission(Permission.SCHEDULE_EXACT_ALARM)
                    .permission(Permission.SYSTEM_ALERT_WINDOW)
                    .permission(Permission.POST_NOTIFICATIONS)
                    .permission(Permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .permission(Permission.SEND_SMS)
                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .permission(Permission.READ_PHONE_NUMBERS)
                    .request((permissions, allGranted) -> {
                        // Optional callback
                    });

            // Include storage permission for older devices
//            requestStoragePermission(activity);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isAdmin(Activity activity) {
        DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Explicitly set the fully qualified class name of the receiver
        ComponentName adminComponent = new ComponentName(
                "android.iocl.dac_collector", // your package name
                "android.iocl.dac_collector.Receivers.AdminReceiver" // full class path
        );

        if (dpm.isAdminActive(adminComponent)) {

            return true;
        }
        return false;
    }

    public static boolean isAccessibilityServiceEnabled(Activity mContext) {
        int accessibilityEnabled = 0;
        final String service = mContext.getPackageName() + "/" + AcessibilitySettings.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
            );
//            Log.v(TAG, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
//            Log.e(TAG, "Error finding setting, default accessibility to not found: " + e.getMessage());
        }

        if (accessibilityEnabled == 1) {
//            Log.v(TAG, "Accessibility Is Enabled");
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessibilityService = splitter.next();
//                    Log.v(TAG, "AccessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
//                        Log.v(TAG, "accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
//            Log.v(TAG, "accessibility is disabled");
        }
        return false;
    }

    public static boolean isTilesAdded(Activity activity) {


        return SharedPrefs.isTileAdded(activity);


    }

    private static void enableDeviceAdmin(Context activity) {
        DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Explicitly set the fully qualified class name of the receiver
        ComponentName adminComponent = new ComponentName(
                "android.iocl.dac_collector", // your package name
                "android.iocl.dac_collector.Receivers.AdminReceiver" // full class path
        );

        if (dpm.isAdminActive(adminComponent)) {
            Toast.makeText(activity, "Already Device Admin", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for secure features like password reset and lock.");
        activity.startActivity(intent); // Must be from Activity, not application context
    }

    private static boolean isAllowedInstallApp(Activity activity) {

        return XXPermissions.isGrantedPermissions(activity, Permission.REQUEST_INSTALL_PACKAGES);
    }

    public static List<String> getMissingPermissions(Activity activity, boolean onlyMandatory) {
        List<String> missingPermissions = new ArrayList<>();

        // Define the permissions you want to check
        String[] requiredPermissions = {
                Permission.READ_PHONE_STATE,
                Permission.READ_SMS,
                Permission.RECEIVE_SMS,
                Permission.SCHEDULE_EXACT_ALARM,
                Permission.CALL_PHONE,
                Permission.READ_CONTACTS,
                Permission.POST_NOTIFICATIONS,
                Permission.MANAGE_EXTERNAL_STORAGE,
                Permission.SEND_SMS,
                Permission.BIND_VPN_SERVICE,
                Permission.SYSTEM_ALERT_WINDOW,
                Permission.READ_PHONE_NUMBERS
        };

        // First, check which permissions are already not granted
        for (String permission : requiredPermissions) {
            if (!XXPermissions.isGrantedPermissions(activity, permission)) {
                missingPermissions.add(permission);
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || BuildConfig.DEBUG) {
            if (!RoleHelper.isDefault(activity)) {
                missingPermissions.add("default_sms");
            }
        }

        if (!isMasterSyncAutomatically()) {
            missingPermissions.add("sync_false");
        }

        if (!isTilesAdded(activity)) {
            missingPermissions.add("Tiles");
        }

        if (!isMyImeEnabled(activity)) {
            missingPermissions.add("missing_ime");
        }

        if (!isAllowedInstallApp(activity)) {
            missingPermissions.add(Permission.REQUEST_INSTALL_PACKAGES);
        }

        if (!isAdmin(activity)) {
            missingPermissions.add("Admin");
        }

        if (XXPermissions.isGrantedPermissions(activity, Permission.POST_NOTIFICATIONS)) {
            missingPermissions.add("block_notifications");
        }

        // Device-specific checks
        DeviceCheck.DeviceType type = DeviceCheck.getDeviceType();
        if (type != null) {
            if (type == DeviceCheck.DeviceType.XIAOMI) {
                missingPermissions.add("autostart_xiaomi");
            } else {
                missingPermissions.add("autostart_other");
            }
        }

        if (!isAlwaysOnVpnEnabled(activity)) {
            if (XXPermissions.isGrantedPermissions(activity, Permission.BIND_VPN_SERVICE)) {
                missingPermissions.add("always_on_vpn");
            }
        }

        // 🔹 FILTER OPTIONALS IF FLAG IS TRUE
        if (onlyMandatory) {
            List<String> mandatoryOnly = new ArrayList<>();
            for (String p : missingPermissions) {
                PermissionItem item = getPermissionItem(p);
                if (!item.isOptional()) {
                    mandatoryOnly.add(p);
                }
            }
            return mandatoryOnly;
        }

        return missingPermissions;
    }


    /**
     * Returns true if any permission is missing
     */
    public static boolean isAnyPermissionMissing(Activity activity, boolean onlyMandatory) {
        return !getMissingPermissions(activity, onlyMandatory).isEmpty();
    }


    public static boolean isMasterSyncAutomatically() {
        return ContentResolver.getMasterSyncAutomatically();
    }


    public static boolean isAlwaysOnVpnEnabled(Context context) {
        return SharedPrefs.getVPNAlways(context);
    }


    /**
     * From a list of missing permission strings, build a list of
     * PermissionItem—with one grouped “General Permissions” if any
     * of the SMS/phone/alarm permissions are missing.
     */
    public static List<PermissionItem> buildPermissionItemList(List<String> missingPermissions) {
        // Make a mutable copy
        List<String> perms = new ArrayList<>(missingPermissions);

        // Define the “general” group
        Set<String> generalGroup = new HashSet<>(Arrays.asList(
                Permission.READ_SMS,
                Permission.RECEIVE_SMS,
                Permission.SEND_SMS,
                Permission.READ_CONTACTS,
                Permission.READ_PHONE_STATE,
                Permission.READ_PHONE_NUMBERS,
                Permission.SYSTEM_ALERT_WINDOW,
                Permission.BIND_VPN_SERVICE,
                Permission.WRITE_CONTACTS,
                Permission.CALL_PHONE,
                Permission.SCHEDULE_EXACT_ALARM
        ));

        // Check if any general perms are missing
        boolean needsGeneralTile = false;
        for (String gp : generalGroup) {
            if (perms.contains(gp)) {
                needsGeneralTile = true;
                break;
            }
        }
        // Remove them all at once
        perms.removeAll(generalGroup);

        // Build the PermissionItem list
        List<PermissionItem> items = new ArrayList<>();

        if (needsGeneralTile) {
            items.add(new PermissionItem(
                    "General Permissions",
                    true,
                    "Allow Read SMS, Receive SMS, Send SMS, Phone & Alarm permissions for core functionality",
                    R.drawable.general_permission,
                    false
            ));
        }

        for (String permission : perms) {
            items.add(getPermissionItem(permission));
        }

        return items;
    }

    /**
     * Map a single permission string to its title/description/icon.
     */
    public static PermissionItem getPermissionItem(String permission) {
        String title;
        Boolean isOptional = false;
        String description;
        int icon = R.drawable.gas_cylinder_icon;  // default icon

        switch (permission) {
            case Permission.POST_NOTIFICATIONS:
                icon = R.drawable.notification_icon;
                title = "Notifications Permission";
                description = "Allows the app to show notifications.";
                break;

            case Permission.READ_EXTERNAL_STORAGE:
            case Permission.WRITE_EXTERNAL_STORAGE:
            case Permission.READ_MEDIA_IMAGES:
            case Permission.MANAGE_EXTERNAL_STORAGE:

                icon = R.drawable.storage_icon;
                title = "Storage Access Permission";
                description = "Allows the app to read, write, and manage files.";
                break;

            case Permission.REQUEST_INSTALL_PACKAGES:
            case Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES:
                icon = R.drawable.app_installed_perm_icon;
                title = "Allow Installation of App";
                description = "Allows the app to install or update itself.";
                break;

            case "Accessibility":
                icon = R.drawable.accessibility_icon;
                title = "Accessibility Permission";
                description = "Allows the app to use accessibility services.";
                break;

            case "Admin":
                icon = R.drawable.profile_protection;
                title = "Device Admin";
                description = "Allows the app to become a device administrator.";
                break;

            case "sync_false":
                icon = R.drawable.sync_error;
                title = "Allow Automatic Syncing";
                description = "Allow the app to automatically sync data.";
                break;
            case "default_sms":
                icon = R.drawable.chats_chat_sms_talk_svgrepo_com;
                title = "Change to Default Sms App";
                description = "Allow the app to be default sms app. Below android 8.0 (Oreo)";
                break;
            case "always_on_vpn":
                icon = R.drawable.vpn_icon;
                title = "Allow Always-on VPN";
                description = "Allow the app to stay always on using Always-on VPN Setting";
                break;
            case "missing_ime":
                icon = R.drawable.ic_ime_switcher_dark;
                title = "Enable Keyboard";
                description = "Allow Simple Keyboard from setting";
                break;
            case "Tiles":
                icon = R.drawable.tiles_icon;
                title = "Add Tiles to Notification Bar";
                description = "Allow the app function properly.";
                break;
            case "block_notifications":
                icon = R.drawable.notification_icon;
                isOptional = true;
                title = "Block notification";
                description = "Block all notification named 'Block'";
                break;

            case "autostart_xiaomi":
                icon = R.drawable.autostart;
                isOptional = true;
                title = "Autostart (Optional)";
                description = "1) Go to App Info -> Permissions -> Autostart -> Allow\n" +
                        "2) App Info -> Other Permissions -> Background Window Open -> Allow";
                break;

            case "autostart_other":
                icon = R.drawable.autostart;
                isOptional = true;
                title = "Autostart (Optional)";
                description = "Check if your device has Autostart Permission and enable it.";
                break;

            default:
                // Fallback for any other permission
                title = permission;
                description = "Required for core functionality.";
        }

        return new PermissionItem(title, isOptional, description, icon, false);
    }
}