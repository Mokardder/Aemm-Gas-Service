package android.iocl.dac_collector.Services;

import android.app.ActivityManager;
import android.content.Intent;
import android.iocl.dac_collector.Ui.FloatingTileActivity;
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

    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        if (getQsTile() == null) return;


    }

    @Override
    public void onClick() {
        super.onClick();


        try {

            if (SharedPrefs.isTileAdded()){
                SharedPrefs.setTileAdded();
            }

            if (SharedPrefs.isFirstTime()){
                Intent intent = new Intent(this, PermissionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityAndCollapse(intent);
            }else {
                Intent intent = new Intent(getApplicationContext(), FloatingTileActivity.class);
                intent.setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                );;
                startActivityAndCollapse(intent);
            }



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



}
