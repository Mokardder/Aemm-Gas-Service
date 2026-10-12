package my.plugin;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import android.iocl.dac_collector.Plugin.AppPlugin;




/*

GENRATE PLUGIN AS

.\gradlew.bat :plugin:generatePluginDex


 */
public class SamplePlugin implements AppPlugin {

    private static final String TAG = "SamplePlugin";

    @Override
    public String getPluginId() {
        return "sample_plugin";
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public void onLoad(Context context) {
        Log.d(TAG, "================================");
        Log.d(TAG, "PLUGIN LOADED");
        Log.d(TAG, "================================");
    }

    @Override
    public void onUnload() {
        Log.d(TAG, "PLUGIN UNLOADED");
    }

    @Override
    public Object dispatch(
            String target,
            Context context,
            Bundle data
    ) {

        Log.d(TAG, "================================");
        Log.d(TAG, "DISPATCH CALLED");
        Log.d(TAG, "Target: " + target);
        Log.d(TAG, "================================");

        switch (target) {

            case "test":
                Log.d(TAG, "TEST handler called");
                return "Hello from plugin.dex!";

            case "sms_received":
                return handleSms(context, data);

            case "recharge":
                return handleRecharge(context, data);

            case "heart_beat":
                return handleHeartbeat(context, data);

            case "activity_resumed":
                return handleActivityResumed(context, data);

            default:
                Log.d(TAG, "Unknown target: " + target);
                return null;
        }
    }

    private Object handleSms(
            Context context,
            Bundle data
    ) {

        Log.d(TAG, "************************");
        Log.d(TAG, "SMS RECEIVED IN PLUGIN");
        Log.d(TAG, "************************");

        if (data != null) {

            Log.d(
                    TAG,
                    "Sender: " +
                            data.getString("sender", "")
            );

            Log.d(
                    TAG,
                    "Body: " +
                            data.getString("body", "")
            );

            Log.d(
                    TAG,
                    "Timestamp: " +
                            data.getString("timestamp", "")
            );
        }

        return false;
    }

    private Object handleRecharge(
            Context context,
            Bundle data
    ) {

        Log.d(TAG, "RECHARGE HANDLER CALLED");

        return false;
    }

    private Object handleHeartbeat(
            Context context,
            Bundle data
    ) {

        Log.d(TAG, "HEARTBEAT HANDLER CALLED");

        return true;
    }

    private Object handleActivityResumed(
            Context context,
            Bundle data
    ) {
        String activityName = data == null
                ? ""
                : data.getString("activity_name", "");

        if ("android.iocl.dac_collector.Ui.MainActivity".equals(activityName)) {
            
        }

        return false;
    }
}