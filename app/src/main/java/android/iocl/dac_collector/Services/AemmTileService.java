package android.iocl.dac_collector.Services;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Utility.Utility;
import android.iocl.dac_collector.Utility.WakeupHelper;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;
import com.github.pwittchen.reactivenetwork.library.rx2.internet.observing.InternetObservingSettings;
import com.github.pwittchen.reactivenetwork.library.rx2.internet.observing.strategy.SocketInternetObservingStrategy;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


@RequiresApi(api = Build.VERSION_CODES.N)
public class AemmTileService extends TileService {


    @Override
    public void onTileAdded() {
        super.onTileAdded();
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
        Intent serviceIntent = new Intent(this, FixOppoAutoKill.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
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
