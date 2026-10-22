package android.iocl.dac_collector.Services;

import android.app.PendingIntent;
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
        SharedPrefs.setTileAdded();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        if (getQsTile() == null) return;

    }

    @Override
    public void onClick() {
        try {
            super.onClick();

            boolean firstTime = SharedPrefs.isFirstTime();
            Class<?> targetActivity = firstTime
                    ? PermissionActivity.class
                    : FloatingTileActivity.class;

            Intent intent = new Intent(this, targetActivity)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (!firstTime) {
                intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                );
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    PendingIntent pendingIntent = PendingIntent.getActivity(
                            this,
                            1001,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE
                    );
                    startActivityAndCollapse(pendingIntent);
                } else {
                    startActivityAndCollapse(intent);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not collapse Quick Settings while opening tile screen", e);
                startActivity(intent);
            }

        } catch (RuntimeException e) {
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
