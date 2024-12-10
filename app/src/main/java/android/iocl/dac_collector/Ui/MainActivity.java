package android.iocl.dac_collector.Ui;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.iocl.dac_collector.Interface.OnCompleteInterface;
import android.iocl.dac_collector.ModelData.RegexModel;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Services.DownloadService;
import android.iocl.dac_collector.Utility.Utility;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity_Mokardder";


    CardView show_notification;


    FirebaseAnalytics mFirebaseAnalytics;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        show_notification = findViewById(R.id.show_notification);






            requestPerms();
            reqIgnoreBattery();





        FirebaseApp.initializeApp(this);


        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        mFirebaseAnalytics.setAnalyticsCollectionEnabled(true);

        FirebaseMessaging.getInstance().setAutoInitEnabled(true);


        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d("Mokardder--->", "Token: -> " + token);

                    }

                });


//        Utility.getAutostartSettingIntent(this);
        show_notification.setOnClickListener(v -> showDialog());

    }



    private void showDialog() {
        RelativeLayout relativeLayoutAlert = findViewById(R.id.update_layout_dialog);
        View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.app_update_layout, relativeLayoutAlert);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setView(view);

        RelativeLayout updateBtn = view.findViewById(R.id.update_rl_button);
        TextView btnText = view.findViewById(R.id.update_btn_text);
        final AlertDialog alertDialog = builder.create();

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();

        updateBtn.setOnClickListener(v -> {

            DownloadService mDownloadService = new DownloadService(MainActivity.this, "app_update.apk",  new OnCompleteInterface() {
                @Override
                public void onComplete(int count, String who) {

                    Toast.makeText(MainActivity.this, " -> " + who, Toast.LENGTH_SHORT).show();

                }
            });

            btnText.setVisibility(View.GONE);
            String downloadUrl = "https://raw.githubusercontent.com/Mokardder/mokardder.github.io/main/app-release.apk"; // Replace with your actual APK URL
            mDownloadService.execute(downloadUrl);

        });


//        RelativeLayout  relativeLayoutAlert = findViewById(R.id.alertDialog_userdetails);
//        View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.userdetails_alert_layout, relativeLayoutAlert);
//        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//        builder.setView(view);
//        final AlertDialog alertDialog = builder.create();
//
//        if (alertDialog.getWindow() != null){
//            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
//        }
//        alertDialog.show();
    }


    private void checkReceiveSms(String msg) {

        List<RegexModel> details = Utility.checkDACRegex(msg, getApplicationContext());


        if (details != null && !details.isEmpty()) {

            boolean isDAC = details.get(0).getCaptures().size() > 1 ? true : false;
//                        boolean isDAC = details.get(0).getCaptures().size() > 1 ? details.get(0).getCaptures().get(1) : details.get(0).getCaptures().get(0)
            String DAC = isDAC ? details.get(0).getCaptures().get(1) : details.get(0).getCaptures().get(0);

            String message = isDAC ? details.get(0).getCaptures().get(0) : "Indian Oil OTP";

            if (Utility.isConnectedToInternet(getApplicationContext())) {
                if (details.get(0).getId().equals("GeneratedDAC")) {
                    Log.e(TAG, "CM - " + message + " DAC - " + DAC + " |Online | Path - GENERATED");
                    return;
                }

                Log.e(TAG, "CM - " + message + " DAC - " + DAC + " |Online");
            } else {
                Log.e(TAG, "CM - " + message + " DAC - " + DAC + " |Online");
            }
        } else {
            Log.e(TAG, "Details array is empty or invalid");
        }
    }

    // Moved outside the onCreate method
    private void reqIgnoreBattery() {
        XXPermissions.with(this)
                .permission(Permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .request((permissions, allGranted) -> {
                    Toast.makeText(this, "Battery Ignored", Toast.LENGTH_SHORT).show();
                });
    }


    // Moved outside the onCreate method
    private void requestPerms() {
        XXPermissions.with(this)
                .permission(Permission.READ_PHONE_STATE)
                .permission(Permission.READ_SMS)
                .permission(Permission.READ_EXTERNAL_STORAGE)
                .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .permission(Permission.RECEIVE_SMS)
                .permission(Permission.POST_NOTIFICATIONS)
                .permission(Permission.SEND_SMS)
                .permission(Permission.READ_PHONE_NUMBERS)
                .request((permissions, allGranted) -> {
                    if (!allGranted) {
                        Toast.makeText(MainActivity.this, "Some Permissions were granted", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(MainActivity.this, "Permission Already Granted", Toast.LENGTH_SHORT).show();
                });
    }



}
