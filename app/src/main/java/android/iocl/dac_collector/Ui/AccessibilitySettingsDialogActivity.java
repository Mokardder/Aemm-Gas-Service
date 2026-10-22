package android.iocl.dac_collector.Ui;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Utility.SharedPrefs;

/**
 * A translucent confirmation shown only when the app's Accessibility entry is open.
 * Password verification is delegated to Android's credential UI; this activity never receives,
 * saves, or logs the user's password or PIN.
 */
public class AccessibilitySettingsDialogActivity extends AppCompatActivity {
    private static final String TAG = "AccessibilitySettingsDialog";
    public static final String ACTION_CONFIRMATION_FINISHED =
            "android.iocl.dac_collector.action.ACCESSIBILITY_CONFIRMATION_FINISHED";
    public static final String EXTRA_RETURNED_TO_HOME = "returned_to_home";
    public static final String EXTRA_RETRY_CONFIRMATION = "retry_confirmation";
    public static final String EXTRA_REQUIRE_CREDENTIAL = "require_device_credential";
    private static final int REQUEST_CONFIRM_DEVICE_CREDENTIAL = 1001;
    private boolean awaitingCredentialResult;
    private boolean completionReported;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setOtherAppTasksExcludedFromRecents(true);
        setContentView(R.layout.activity_accessibility_settings_dialog);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Keep the credential confirmation open until an explicit action completes it.
            }
        });

        findViewById(R.id.cancelButton).setOnClickListener(view -> cancelAndReturnHome());
        findViewById(R.id.verifyButton).setOnClickListener(view -> verifyWithDeviceCredential());
    }

    private void verifyWithDeviceCredential() {
        if (awaitingCredentialResult) {
            return;
        }

        KeyguardManager keyguardManager =
                (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (keyguardManager == null || !keyguardManager.isKeyguardSecure()) {
            Toast.makeText(
                    this,
                    R.string.accessibility_screen_lock_required,
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Intent credentialIntent = keyguardManager.createConfirmDeviceCredentialIntent(
                getString(R.string.accessibility_verify_title),
                getString(R.string.accessibility_verify_message)
        );
        if (credentialIntent == null) {
            Toast.makeText(this, R.string.accessibility_screen_lock_required, Toast.LENGTH_LONG)
                    .show();
            return;
        }

        awaitingCredentialResult = true;
        startActivityForResult(credentialIntent, REQUEST_CONFIRM_DEVICE_CREDENTIAL);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONFIRM_DEVICE_CREDENTIAL) {
            awaitingCredentialResult = false;
            if (resultCode == RESULT_OK || !requiresCredential()) {
                finishConfirmation();
            } else {
                cancelAndReturnHome();
            }
        }
    }

    private void finishConfirmation() {
        reportCompletion(false);
        finishAndRemoveTask();
    }

    private void cancelAndReturnHome() {
        reportCompletion(true);
        // The service first navigates away from Accessibility Settings, then sends the user Home.
        finishAndRemoveTask();
    }

    private void reportCompletion(boolean returnedToHome) {
        if (!completionReported) {
            completionReported = true;
            sendCompletionBroadcast(returnedToHome, false);
        }
    }

    private void sendCompletionBroadcast(boolean returnedToHome, boolean retryConfirmation) {
        Intent completionIntent = new Intent(ACTION_CONFIRMATION_FINISHED)
                .setPackage(getPackageName())
                .putExtra(EXTRA_RETURNED_TO_HOME, returnedToHome)
                .putExtra(EXTRA_RETRY_CONFIRMATION, retryConfirmation);
        sendBroadcast(completionIntent);
    }

    @Override
    protected void onDestroy() {
        if (!completionReported) {
            completionReported = true;
            sendCompletionBroadcast(false, true);
        }
        setOtherAppTasksExcludedFromRecents(false);
        super.onDestroy();
    }

    private boolean requiresCredential() {
        return getIntent().getBooleanExtra(EXTRA_REQUIRE_CREDENTIAL, false)
                || !SharedPrefs.isForceAccessibilityEnabled();
    }

    private void setOtherAppTasksExcludedFromRecents(boolean excluded) {
        ActivityManager activityManager =
                (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (activityManager == null) {
            Log.e(TAG, "ActivityManager unavailable; cannot update Recents visibility");
            return;
        }

        for (ActivityManager.AppTask task : activityManager.getAppTasks()) {
            try {
                ComponentName baseActivity = task.getTaskInfo().baseActivity;
                if (baseActivity == null
                        || !AccessibilitySettingsDialogActivity.class
                        .getName()
                        .equals(baseActivity.getClassName())) {
                    task.setExcludeFromRecents(excluded);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not update app task Recents visibility", e);
            }
        }
    }
}
