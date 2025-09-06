package android.iocl.dac_collector.Services;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.iocl.dac_collector.Utility.LauncherIconHelper;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;



@RequiresApi(api = Build.VERSION_CODES.N)
public class AemmTileService extends TileService {
    @Override
    public void onTileAdded() {

        super.onTileAdded();



        SharedPrefs.setTileAdded(getApplicationContext());
        // Tile added to Quick Settings
        updateTileState(false);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
      


        // Update tile state when Quick Settings is opened
        boolean isActive = isForegroundServiceRunning();
        if (!isActive) {
            startForegroundService();
        }
        updateTileState(isActive);
    }

    @Override
    public void onClick() {
        super.onClick();


        // Toggle the tile state on click
        boolean isActive = isForegroundServiceRunning();
        if (!isActive) {
            startForegroundService();
        }
        updateTileState(!isActive);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        boolean isActive = isForegroundServiceRunning();
        if (!isActive) {
            startForegroundService();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        boolean isActive = isForegroundServiceRunning();
        if (!isActive) {
            startForegroundService();
        }
    }



    private void updateTileState(boolean isActive) {
        Tile tile = getQsTile();


        if (tile == null) return;

        tile.setState(isActive ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    private void startForegroundService() {


        try {
            Intent serviceIntent = new Intent(this, FixOppoAutoKill.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

        }catch (Exception e){

        }

    }




    private boolean isForegroundServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (FixOppoAutoKill.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
