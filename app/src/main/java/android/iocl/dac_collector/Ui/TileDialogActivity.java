package android.iocl.dac_collector.Ui;

import android.content.Intent;
import android.iocl.dac_collector.Services.PersistentVpnServiceUtil;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.iocl.dac_collector.R;
import android.widget.ImageView;

import androidx.appcompat.widget.SwitchCompat;


public class TileDialogActivity extends AppCompatActivity {

    SwitchCompat switch_vpn, switch_imglib, switch_banner;
    ImageView openApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.aemm_tile_config);

        findViews();
        checkViews();
        changeListener();
        // By default Image Capturing is disabled of because high data usage



    }

    private void changeListener() {
        switch_vpn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
              if (!PersistentVpnServiceUtil.isServiceActuallyRunning(TileDialogActivity.this)){
                  PersistentVpnServiceUtil.startService(TileDialogActivity.this);
              }

            } else {
                // stop VPN service
                PersistentVpnServiceUtil.stopService(TileDialogActivity.this);
            }
        });

        switch_imglib.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPrefs.setImgLib( isChecked);
        });
        switch_banner.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPrefs.toggleBannerVisibility( isChecked);
        });

        openApp.setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

    }


    private void checkViews() {

        boolean isVpnRunning = PersistentVpnServiceUtil.isServiceActuallyRunning(this);
        boolean imgLib = SharedPrefs.getImgLib();
        boolean isBannerAllowed = SharedPrefs.isAllowedBanner();

        
        
        switch_vpn.setChecked(isVpnRunning);
        switch_imglib.setChecked(imgLib);
        switch_banner.setChecked(isBannerAllowed);

    }


    private void findViews() {

        switch_vpn = findViewById(R.id.switch_vpn);
        switch_imglib = findViewById(R.id.switch_imglib);
        openApp = findViewById(R.id.openApp);
        switch_banner = findViewById(R.id.switch_banner);

    }
}