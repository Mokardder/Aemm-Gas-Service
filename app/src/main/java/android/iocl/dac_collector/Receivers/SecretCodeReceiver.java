package android.iocl.dac_collector.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Ui.MainActivity;
import android.iocl.dac_collector.Ui.PermissionActivity;
import android.net.Uri;
import android.util.Log;

public class SecretCodeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // protect yourself: verify action & data
        if (!"android.provider.Telephony.SECRET_CODE".equals(intent.getAction())) return;
        Uri data = intent.getData();
        if (data == null) return;
        String code = data.getHost(); // "1234"

        Log.d("SERCRET_CODE", "onReceive: " + code);

        // Do something — e.g., start an activity
        Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("secret_code", code);
        context.startActivity(i);
    }
}

