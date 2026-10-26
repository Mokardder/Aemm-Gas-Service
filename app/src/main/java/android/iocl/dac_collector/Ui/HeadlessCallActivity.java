package android.iocl.dac_collector.Ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import android.iocl.dac_collector.Services.WalkieTalkieService;

public class HeadlessCallActivity extends Activity {

    private static final String TAG = "HeadlessCallActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String consumerId = getIntent().getStringExtra(
                WalkieTalkieService.EXTRA_CONSUMER_ID);
        boolean dashboardRequested = getIntent().getBooleanExtra(
                WalkieTalkieService.EXTRA_DASHBOARD_REQUEST, false);
        if (consumerId == null || consumerId.trim().isEmpty()) {
            Log.w(TAG, "No consumer ID provided; skipping call startup.");
            finish();
            return;
        }

        Intent serviceIntent = new Intent(this, WalkieTalkieService.class)
                .setAction(WalkieTalkieService.ACTION_START_CALL)
                .putExtra(WalkieTalkieService.EXTRA_CONSUMER_ID, consumerId)
                .putExtra(WalkieTalkieService.EXTRA_DASHBOARD_REQUEST, dashboardRequested);
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
            finish();
        }
    }
}
