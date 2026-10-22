package android.iocl.dac_collector.Services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.AccessibilitySettingsDialogActivity;

import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.WakeupHelper;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AcessibilitySettings extends AccessibilityService {
    public static final String CUSTOM_ACTION = "com.example.mybroadcastapp.CUSTOM_ACTION";
    private static final String CHANNEL_ID = "AccessibilitySettingsID";
    private static final String TAG = "AccessibilitySettings";
    private boolean ownAccessibilitySettingsVisitActive;
    private boolean confirmationShownForCurrentVisit;
    private boolean ownDeviceAdminSettingsVisitActive;
    private boolean deviceAdminConfirmationShownForCurrentVisit;
    private boolean confirmationForDeviceAdmin;
    private boolean confirmationFlowActive;
    private boolean movingUserHome;
    private boolean receiverRegistered = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (CUSTOM_ACTION.equals(intent.getAction())) {

                clickBackButton();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    simulateBackGesture();
                }

            } else if (AccessibilitySettingsDialogActivity
                    .ACTION_CONFIRMATION_FINISHED
                    .equals(intent.getAction())) {

                confirmationFlowActive = false;

                boolean returnedToHome = intent.getBooleanExtra(
                        AccessibilitySettingsDialogActivity.EXTRA_RETURNED_TO_HOME,
                        false
                );
                boolean retryConfirmation = intent.getBooleanExtra(
                        AccessibilitySettingsDialogActivity.EXTRA_RETRY_CONFIRMATION,
                        false
                );

                Log.d(TAG,
                        "Confirmation finished. returnedToHome="
                                + returnedToHome
                                + ", retryConfirmation="
                                + retryConfirmation);

                if (returnedToHome) {

                    movingUserHome = true;

                    ownAccessibilitySettingsVisitActive = false;
                    confirmationShownForCurrentVisit = false;
                    ownDeviceAdminSettingsVisitActive = false;
                    deviceAdminConfirmationShownForCurrentVisit = false;

                    exitAccessibilitySettingsThenHome();

                } else if (retryConfirmation) {
                    setConfirmationShownForCurrentVisit(false);
                    Log.d(TAG, "Confirmation dismissed without completion; allowing retry");

                } else {

                    /*
                     * The user is still in the same Settings visit.
                     *
                     * DO NOT reset the visit here.
                     * The dialog has already been shown for this visit.
                     */
                    setConfirmationShownForCurrentVisit(true);

                    Log.d(TAG,
                            "Confirmation finished - keeping current Settings visit");
                }
            }
        }
    };

    // 1) Define handler interface
    public interface AccessibilityHandler {
        void handleEvent(AccessibilityService service, AccessibilityEvent event);
    }

    // 2) Registry of brand-specific handlers
    private final Map<String, AccessibilityHandler> handlerMap = new HashMap<>();

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        // Wake up or keep alive service
        WakeupHelper.wakeupAppService(getApplicationContext());

        // Register brand handlers
        handlerMap.put("realme", this::handleRealmeVivo);
        handlerMap.put("vivo",  this::handleRealmeVivo);
        handlerMap.put("iqoo",  this::handleRealmeVivo);
        handlerMap.put("samsung", this::handleSamsung);

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.notificationTimeout = 0;
        setServiceInfo(info);

        registerReceiverSafely();
        Log.d(TAG, "BroadcastReceiver registered");
    }

    /**
     * Register the receiver once. Safe to call multiple times
     * (e.g. if onServiceConnected() is ever invoked again after a rebind).
     */
    private void registerReceiverSafely() {
        if (receiverRegistered) {
            return;
        }
        try {
            IntentFilter filter = new IntentFilter(CUSTOM_ACTION);
            filter.addAction(AccessibilitySettingsDialogActivity.ACTION_CONFIRMATION_FINISHED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // CUSTOM_ACTION comes from an external app, so the receiver
                // must be exported on Android 13+.
                registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(receiver, filter);
            }
            receiverRegistered = true;
        } catch (RuntimeException e) {
            receiverRegistered = false;
            Log.e(TAG, "Failed to register receiver", e);
        }
    }

    private void unregisterReceiverSafely() {
        if (!receiverRegistered) {
            return;
        }
        try {
            unregisterReceiver(receiver);
        } catch (RuntimeException e) {
            Log.w(TAG, "unregisterReceiver failed", e);
        } finally {
            receiverRegistered = false;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        final int eventType = event.getEventType();

        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }

        // The confirmation activity temporarily replaces the Settings window. Keep
        // the current visit active so returning from the dialog is not treated as
        // leaving the app's accessibility settings page.
        if (isConfirmationActivityOpen(event)) {
            return;
        }

        // Do not let device-specific navigation handling interfere with the
        // confirmation Activity or the system credential screen it launches.
        if (confirmationFlowActive) {
            return;
        }

        /*
         * Detect whether our own Accessibility Settings entry/page
         * is currently visible.
         */
        boolean ownDeviceAdminSettingsOpen = isOwnDeviceAdminSettingsOpen(event);
        boolean ownSettingsOpen = !ownDeviceAdminSettingsOpen
                && isOwnAccessibilitySettingsOpen(event);

        if (ownSettingsOpen || ownDeviceAdminSettingsOpen) {

            /*
             * New visit to our Accessibility Settings page.
             */
            if (ownSettingsOpen && !ownAccessibilitySettingsVisitActive) {

                Log.d(TAG, "=== NEW ACCESSIBILITY SETTINGS VISIT ===");

                ownDeviceAdminSettingsVisitActive = false;
                deviceAdminConfirmationShownForCurrentVisit = false;
                ownAccessibilitySettingsVisitActive = true;
                confirmationShownForCurrentVisit = false;
            }

            if (ownDeviceAdminSettingsOpen && !ownDeviceAdminSettingsVisitActive) {
                Log.d(TAG, "=== NEW DEVICE ADMIN SETTINGS VISIT ===");
                ownAccessibilitySettingsVisitActive = false;
                confirmationShownForCurrentVisit = false;
                ownDeviceAdminSettingsVisitActive = true;
                deviceAdminConfirmationShownForCurrentVisit = false;
            }

            /*
             * Also retry when an earlier confirmation Activity was dismissed
             * without completing its flow (for example, from the Recents screen).
             */
            boolean confirmationShown = ownDeviceAdminSettingsOpen
                    ? deviceAdminConfirmationShownForCurrentVisit
                    : confirmationShownForCurrentVisit;
            if (!confirmationShown && !confirmationFlowActive) {
                confirmationForDeviceAdmin = ownDeviceAdminSettingsOpen;
                setConfirmationShownForCurrentVisit(true);
                showAccessibilitySettingsDialog(ownDeviceAdminSettingsOpen);
            }

            return;
        }

        boolean wasInOwnSettingsVisit = ownAccessibilitySettingsVisitActive
                || ownDeviceAdminSettingsVisitActive;

        /*
         * We only use WINDOW_STATE_CHANGED to determine
         * that the user actually left the Settings page.
         */
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            if (ownAccessibilitySettingsVisitActive || ownDeviceAdminSettingsVisitActive) {

                Log.d(TAG, "=== LEFT PROTECTED SETTINGS PAGE ===");

                ownAccessibilitySettingsVisitActive = false;
                confirmationShownForCurrentVisit = false;
                ownDeviceAdminSettingsVisitActive = false;
                deviceAdminConfirmationShownForCurrentVisit = false;
            }
        }

        // The transition away from our page can still contain "accessibility"
        // in its event text. Do not let the brand handler immediately navigate
        // Back again while the user is leaving the page.
        if (wasInOwnSettingsVisit) {
            return;
        }

        /*
         * Your existing brand-specific handling.
         */
        if (!SharedPrefs.isRestrictionEnabled()
                || !SharedPrefs.isForceAccessibilityEnabled()) {
            return;
        }

        String brand = Build.BRAND.toLowerCase(Locale.US);

        AccessibilityHandler handler = handlerMap.get(brand);

        if (handler != null) {
            handler.handleEvent(this, event);
        } else {
            Log.d(TAG, "No handler for brand=" + brand);
        }
    }

    /**
     * Accessibility events identify Android Settings; the active window's content distinguishes
     * this app's service entry from the general Accessibility page.
     */
    private boolean isOwnAccessibilitySettingsOpen(AccessibilityEvent event) {

        if (event == null) {
            return false;
        }

        CharSequence packageName = event.getPackageName();

        if (packageName == null
                || !packageName.toString()
                .toLowerCase(Locale.US)
                .contains("settings")) {
            return false;
        }

        String appName = getApplicationInfo()
                .loadLabel(getPackageManager())
                .toString();

        // ---------------------------------------------------------
        // Get complete Accessibility hierarchy
        // ---------------------------------------------------------
        AccessibilityNodeInfo root = getRootInActiveWindow();

        if (root == null) {
            Log.d(TAG, "Settings root is null");
            return false;
        }

        try {

            boolean matched = containsAppInAccessibilityDescription(
                    root,
                    appName
            );

            if (matched) {

                Log.d(TAG,
                        "=== OUR APP FOUND IN ACCESSIBILITY DESCRIPTION ===");

                return true;
            }

        } finally {
            root.recycle();
        }

        return false;
    }

    private boolean isOwnDeviceAdminSettingsOpen(AccessibilityEvent event) {
        if (event == null || !isSettingsWindow(event)) {
            return false;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            Log.d(TAG, "Settings root is null while checking Device Admin page");
            return false;
        }

        try {
            String appName = getApplicationInfo()
                    .loadLabel(getPackageManager())
                    .toString();
            boolean appNameVisible = windowContainsText(
                    root,
                    appName.toLowerCase(Locale.US)
            );
            boolean adminControlsVisible = windowContainsText(root, "admin")
                    || windowContainsText(root, "administrator");
            return appNameVisible && adminControlsVisible;
        } finally {
            root.recycle();
        }
    }

    private boolean containsAppInAccessibilityDescription(
            AccessibilityNodeInfo node,
            String appName
    ) {
        if (node == null || TextUtils.isEmpty(appName)) {
            return false;
        }

        String target = appName.trim().toLowerCase(Locale.US);

        // ---------------------------------------------------------
        // Check THIS node's content description
        // ---------------------------------------------------------
        CharSequence description = node.getContentDescription();

        if (description != null) {
            String descriptionText = description.toString()
                    .trim()
                    .toLowerCase(Locale.US);

            if (descriptionText.contains(target)) {

                Log.d(TAG,
                        "MATCHED APP IN ACCESSIBILITY DESCRIPTION: "
                                + description);

                return true;
            }
        }

        // ---------------------------------------------------------
        // Also check normal text
        // ---------------------------------------------------------
        CharSequence text = node.getText();

        if (text != null) {
            String textValue = text.toString()
                    .trim()
                    .toLowerCase(Locale.US);

            if (textValue.contains(target)) {

                Log.d(TAG,
                        "MATCHED APP IN NODE TEXT: "
                                + text);

                return true;
            }
        }

        // ---------------------------------------------------------
        // Recursively check ALL children
        // ---------------------------------------------------------
        for (int i = 0; i < node.getChildCount(); i++) {

            AccessibilityNodeInfo child = node.getChild(i);

            if (child == null) {
                continue;
            }

            try {
                if (containsAppInAccessibilityDescription(child, appName)) {
                    return true;
                }
            } finally {
                child.recycle();
            }
        }

        return false;
    }



    private boolean isConfirmationActivityOpen(AccessibilityEvent event) {

        if (event == null) {
            return false;
        }

        CharSequence packageName = event.getPackageName();
        CharSequence className = event.getClassName();

        if (packageName == null || className == null) {
            return false;
        }

        return getPackageName().contentEquals(packageName)
                && AccessibilitySettingsDialogActivity.class
                .getName()
                .contentEquals(className);
    }

    private boolean isSettingsWindow(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        return packageName != null
                && packageName.toString().toLowerCase(Locale.US).contains("settings");
    }

    private void moveUserHome() {
        movingUserHome = true;
        confirmationFlowActive = false;
        ownAccessibilitySettingsVisitActive = false;
        confirmationShownForCurrentVisit = false;
        ownDeviceAdminSettingsVisitActive = false;
        deviceAdminConfirmationShownForCurrentVisit = false;
        if (!performGlobalAction(GLOBAL_ACTION_HOME)) {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
        }
    }

    private void setConfirmationShownForCurrentVisit(boolean shown) {
        if (confirmationForDeviceAdmin) {
            deviceAdminConfirmationShownForCurrentVisit = shown;
        } else {
            confirmationShownForCurrentVisit = shown;
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        ownAccessibilitySettingsVisitActive = false;
        confirmationShownForCurrentVisit = false;
        ownDeviceAdminSettingsVisitActive = false;
        deviceAdminConfirmationShownForCurrentVisit = false;
        confirmationFlowActive = false;
        Log.d(TAG, "App task removed; reset accessibility settings visit state");
    }

    private void exitAccessibilitySettingsThenHome() {
        performGlobalAction(GLOBAL_ACTION_BACK);
        moveUserHome();
    }

    private boolean windowContainsText(AccessibilityNodeInfo node, String textToFind) {
        if (node == null) {
            return false;
        }

        if (containsIgnoreCase(String.valueOf(node.getText()), textToFind)
                || containsIgnoreCase(String.valueOf(node.getContentDescription()), textToFind)) {
            return true;
        }

        for (int index = 0; index < node.getChildCount(); index++) {
            AccessibilityNodeInfo child = node.getChild(index);
            if (child == null) {
                continue;
            }

            try {
                if (windowContainsText(child, textToFind)) {
                    return true;
                }
            } finally {
                child.recycle();
            }
        }

        return false;
    }

    private boolean containsIgnoreCase(String value, String textToFind) {
        return !TextUtils.isEmpty(value)
                && value.toLowerCase(Locale.US).contains(textToFind);
    }

    private void showAccessibilitySettingsDialog(boolean forDeviceAdmin) {

        if (confirmationFlowActive) {
            Log.d(TAG, "Confirmation already active - ignoring");
            return;
        }

        confirmationFlowActive = true;
        confirmationForDeviceAdmin = forDeviceAdmin;

        try {

            Intent intent = new Intent(
                    this,
                    AccessibilitySettingsDialogActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            | Intent.FLAG_ACTIVITY_NO_ANIMATION
            ).putExtra(
                    AccessibilitySettingsDialogActivity.EXTRA_REQUIRE_CREDENTIAL,
                    forDeviceAdmin
            );

            startActivity(intent);

            Log.i(TAG, "Opened accessibility confirmation Activity");

        } catch (RuntimeException e) {

            confirmationFlowActive = false;
            setConfirmationShownForCurrentVisit(false);

            Log.e(TAG,
                    "Could not open accessibility confirmation Activity",
                    e);
        }
    }

    @Override
    public void onInterrupt() {
        // IMPORTANT: Do NOT unregister the receiver here.
        // onInterrupt() is a temporary interruption (focus change, another
        // accessibility client, lockscreen, etc.). The service is still
        // alive and must continue receiving ACTION_CONFIRMATION_FINISHED
        // from the dialog activity. Registering the receiver in
        // onServiceConnected() and unregistering in onDestroy() keeps the
        // lifecycle symmetric — this is what was blocking re-show of the
        // dialog on subsequent visits.
        Log.d(TAG, "onInterrupt - keeping receiver registered");
    }

    @Override
    public void onDestroy() {
        unregisterReceiverSafely();
        super.onDestroy();
    }

    // 3) Brand-specific logic
    private void handleRealmeVivo(AccessibilityService service, AccessibilityEvent event) {
        String cls = event.getClassName() != null
                ? event.getClassName().toString().toLowerCase(Locale.US)
                : "";
        String text = !event.getText().isEmpty()
                ? event.getText().toString().toLowerCase(Locale.US)
                : "";

        Log.d(TAG,  "Event - " + event + " | cls - " +  cls  + " | txt - " + text);

        if (cls.contains("notification")){return;}
        if (cls.contains("android.iocl.dac_collector")){ return;};

        boolean appNameMatch = text.contains(getString(R.string.app_name).toLowerCase(Locale.US));
        boolean accessibilityMatch = text.contains("accessibility");
        boolean forceStopMatch = text.contains("force stop");
        boolean adminMatch = cls.contains("admin");

        if ((appNameMatch || accessibilityMatch || forceStopMatch || adminMatch)
                && !text.contains("notification")
                && !text.contains("volume")) {

            clickBackButton();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) simulateBackGesture();
        }
    }

    private void handleSamsung(AccessibilityService service, AccessibilityEvent event) {
        Log.d(TAG, "Samsung-specific logic can go here");
    }

    // 4) Helpers
    public void clickBackButton() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void simulateBackGesture() {
        Path path = new Path();
        path.moveTo(1000, 2000);
        path.lineTo(100, 2000);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        GestureDescription description = builder
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 200))
                .build();
        dispatchGesture(description, null, null);
    }
}