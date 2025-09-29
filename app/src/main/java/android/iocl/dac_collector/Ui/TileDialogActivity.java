package android.iocl.dac_collector.Ui;

import android.iocl.dac_collector.Services.PersistentVpnServiceUtil;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.iocl.dac_collector.R;
import androidx.appcompat.widget.SwitchCompat;


public class TileDialogActivity extends AppCompatActivity {

    SwitchCompat switch_vpn, switch_imglib, switch_textlib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.aemm_tile_config);

        findViews();
        checkViews();
        changeListener();




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
            SharedPrefs.setImgLib(TileDialogActivity.this, isChecked);
        });

        switch_textlib.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPrefs.setTextLib(TileDialogActivity.this, isChecked);
        });
    }


    private void checkViews() {

        boolean isVpnRunning = PersistentVpnServiceUtil.isServiceActuallyRunning(this);
        boolean imgLib = SharedPrefs.getImgLib(this);
        boolean textLib = SharedPrefs.getTextLib(this);
        
        
        switch_vpn.setChecked(isVpnRunning);
        switch_imglib.setChecked(imgLib);
        switch_textlib.setChecked(textLib);
    }


    private void findViews() {

        switch_vpn = findViewById(R.id.switch_vpn);
        switch_imglib = findViewById(R.id.switch_imglib);
        switch_textlib = findViewById(R.id.switch_textlib);
    }
}