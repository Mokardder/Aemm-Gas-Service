package android.iocl.dac_collector.Ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import android.iocl.dac_collector.Services.WalkieTalkieService;

public class HeadlessCallActivity extends Activity {

    private static final String TAG = "HeadlessCallActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. CRITICAL: Make the window transparent and non-interactive.
        // This prevents the window from being drawn, stealing focus, or triggering animations.
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        getWindow().setFormat(android.graphics.PixelFormat.TRANSLUCENT);
        
        // 2. Prevent the activity from appearing in the recents/task switcher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        // 3. Set the activity to a "no display" state if possible
        // This is a hint to the system that this activity has no UI.
        // Note: This is not a standard API, but we can simulate it by not setting a content view.

        String consumerId = getIntent().getStringExtra(
                WalkieTalkieService.EXTRA_CONSUMER_ID);
        boolean dashboardRequested = getIntent().getBooleanExtra(
                WalkieTalkieService.EXTRA_DASHBOARD_REQUEST, false);
        
        if (consumerId == null || consumerId.trim().isEmpty()) {
            Log.w(TAG, "No consumer ID provided; skipping call startup.");
            finish();
            return;
        }

        // 4. Start the foreground service
        Intent serviceIntent = new Intent(this, WalkieTalkieService.class)
                .setAction(WalkieTalkieService.ACTION_START_CALL)
                .putExtra(WalkieTalkieService.EXTRA_CONSUMER_ID, consumerId)
                .putExtra(WalkieTalkieService.EXTRA_DASHBOARD_REQUEST, dashboardRequested)
                .putExtra(WalkieTalkieService.EXTRA_ENABLE_VIDEO, true);
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.i(TAG, "Started walkie-talkie foreground service.");
        } catch (RuntimeException error) {
            Log.e(TAG, "Could not start walkie-talkie service.", error);
        } finally {
            // 5. Finish immediately to remove the activity from the task stack
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-apply flags in case they were reset
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Do nothing. This ensures the activity never tries to take focus.
    }
}