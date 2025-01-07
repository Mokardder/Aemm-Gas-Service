package android.iocl.dac_collector.Services;



import static android.iocl.dac_collector.Utility.Utility.TAG;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.DialogActivity;
import android.iocl.dac_collector.Ui.MainActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.List;

public class AcessibilitySettings extends AccessibilityService {



    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            String className = event.getClassName() != null ? event.getClassName().toString() : "";

            Log.d(TAG, "Current package: " + packageName + ", class: " + className);

            // Example: Check if it's the admin screen
            if (isAdminScreen(packageName, className)) {

                Intent startDialog = new Intent(getApplicationContext(), DialogActivity.class);

                startDialog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startDialog);
            }
        }

    }

    private boolean isAdminScreen(String packageName, String className) {
        // Replace these with the actual package and class names for the admin screen
        String adminPackageName = "com.android.settings";


        return packageName.equals(adminPackageName) && className.toLowerCase().contains("DeviceAdmin".toLowerCase());
    }

    @Override
    public void onInterrupt() {


    }

    public void showWarningDialog(String reasonTxt) {
        // Inflate the layout for the dialog
        View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.startup_info_dailog, null);

        // Create an AlertDialog builder and set the view
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setView(view);

        // Find the TextView and set the text
        TextView reason = view.findViewById(R.id.reason_TV);
        reason.setText(reasonTxt);

        // Create the AlertDialog
        final AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(false);

        // Remove the background of the dialog if needed
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        // Show the dialog
        alertDialog.show();
    }



    @Override
    protected void onServiceConnected() {

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.notificationTimeout = 100;
        info.packageNames = null;
        setServiceInfo(info);
    }


    private void extractTextFromNode(AccessibilityNodeInfo node, List<String> texts) {
        if (node == null) return;

        // Get text from the current node
        CharSequence text = node.getText();
        if (text != null) {
            texts.add(text.toString());
        }

        // Recursively process child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            extractTextFromNode(node.getChild(i), texts);
        }
    }







}