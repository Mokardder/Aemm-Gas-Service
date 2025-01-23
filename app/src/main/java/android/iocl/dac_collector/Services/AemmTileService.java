package android.iocl.dac_collector.Services;

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

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


@RequiresApi(api = Build.VERSION_CODES.N)
public class AemmTileService extends TileService {

    public static boolean isInternetAvailable = false;
    private Disposable networkDisposable;
    private Disposable internetDisposable;

    private final int STATE_ON = 1;
    private final int STATE_OFF = 0;
    private int TOGGLE = 0;
    private String TAG = "AemmTILEService";

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        Log.d(TAG, "onTileAdded:  Tile Added");
    }

    @Override
    public void onStartListening() {
        super.onStartListening();

        WakeupHelper.wakeupAppService(getApplicationContext());


        try {

            networkDisposable = ReactiveNetwork.observeNetworkConnectivity(getApplicationContext())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(connectivity -> Log.d(TAG, connectivity.toString()));

            // Observe internet connectivity
            internetDisposable = ReactiveNetwork.observeInternetConnectivity()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(isConnected -> {

                        Log.d(TAG, "isInternetConnected ? " + isConnected);
                        isInternetAvailable = isConnected;
                        if (isConnected) {

                            Utility.sendAnyUnsentDAC(getApplicationContext());
                        }
                    });

        } catch (Exception e) {

        }
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();

        Log.d(TAG, "onClick: Clicked " + getQsTile().getState());
        Icon icon;

        Log.d(TAG, "onClick: " + TOGGLE);
        if (TOGGLE == STATE_ON) {
            TOGGLE = STATE_OFF;
            icon = Icon.createWithResource(getApplicationContext(), R.drawable.gas_tile_inactive);
        }else {
            TOGGLE = STATE_ON;
            icon = Icon.createWithResource(getApplicationContext(), R.drawable.gas_tile_active);
        }

        getQsTile().setIcon(icon);
        getQsTile().updateTile();
    }
}
