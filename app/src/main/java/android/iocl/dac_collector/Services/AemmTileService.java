package android.iocl.dac_collector.Services;

import android.app.ActivityManager;
import android.content.Intent;
import android.iocl.dac_collector.Ui.PermissionActivity;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.os.Build;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class AemmTileService extends TileService {

    private static final String TAG = "AemmTileService";

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        if (getQsTile() == null) return;

        try {
            SharedPrefs.setTileAdded(getApplicationContext());
            updateTileState(false);
        } catch (Exception e) {
            Log.e(TAG, "onTileAdded error", e);
        }
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        if (getQsTile() == null) return;

        try {
//            boolean isActive = isForegroundServiceRunning();
//            if (!isActive) {
//                startForegroundServiceSafe();
//            }
//            updateTileState(isActive);
        } catch (Exception e) {
            Log.e(TAG, "onStartListening error", e);
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        try {
            Intent intent = new Intent(this, PermissionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(intent); // Launches and collapses QS panel
        } catch (Exception e) {
            Log.e(TAG, "onClick error", e);
        }
    }

    @Override
    public void onStopListening() {
        super.onStopListening();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    private void updateTileState(boolean isActive) {
        try {
            // Disabled by user request
            /*
            Tile tile = getQsTile();
            if (tile == null) return;
            tile.setState(isActive ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
            */
        } catch (Exception e) {
            Log.e(TAG, "updateTileState error", e);
        }
    }

    private void startForegroundServiceSafe() {
        try {
            Intent serviceIntent = new Intent(this, FixOppoAutoKill.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "startForegroundServiceSafe error", e);
        }
    }

    private boolean isForegroundServiceRunning() {
        try {
            ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (manager != null) {
                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (FixOppoAutoKill.class.getName().equals(service.service.getClassName())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "isForegroundServiceRunning error", e);
        }
        return false;
    }
}
