package android.iocl.dac_collector.Ui;


import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.Interface.OnCompleteInterface;
import android.iocl.dac_collector.ModelData.AppUpdate;
import android.iocl.dac_collector.ModelData.ColumnValue;
import android.iocl.dac_collector.ModelData.DAC_Collector_Base;
import android.iocl.dac_collector.ModelData.RegexModel;
import android.iocl.dac_collector.ModelData.appUpdateDesc;
import android.iocl.dac_collector.ModelData.check_update;
import android.iocl.dac_collector.ModelData.search_consumer;
import android.iocl.dac_collector.ModelData.search_consumer_response;
import android.iocl.dac_collector.ModelData.update_dac_collect;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Receivers.smsReceivers;
import android.iocl.dac_collector.RetrofitClient.RequestService;
import android.iocl.dac_collector.RetrofitClient.RetrofitClient;
import android.iocl.dac_collector.Services.DownloadService;
import android.iocl.dac_collector.Services.JobSchedulerUtil;
import android.iocl.dac_collector.Utility.AutoStartPermissionHelper;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.Utility;
import android.iocl.dac_collector.adapter.UpdateDescList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity_Mokardder";


    FirebaseAnalytics mFirebaseAnalytics;

    RelativeLayout loader;
    TextView loader_text;
    String FCM_KEY = "";
    SharedPrefs sharedPrefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        sharedPrefs = new SharedPrefs(MainActivity.this);

        loader = findViewById(R.id.ProgressView);
        loader_text = findViewById(R.id.loadingTxt);


        if (sharedPrefs.getBoolean("isFirstTime", true)) {

            showUserDetailsDialog();

        } else {
            checkAppUpdate();
        }




        installpermission();

        String regx = "{\r\n  \"patterns\": [\r\n    {\r\n      \"id\": \"DAC\",\r\n      \"regex\": \"Invoice Number # (\\\\d+-\\\\d+) is (\\\\d{4})\",\r\n      \"capture\": \"1, 2\"\r\n    },\r\n    {\r\n      \"id\": \"OTP\",\r\n      \"regex\": \"Your IOCL one time password is :(\\\\d{4})\",\r\n      \"capture\": \"1\"\r\n    },\r\n    {\r\n      \"id\": \"GeneratedDAC\",\r\n      \"regex\": \"Invoice generated for Rs. (\\\\d{3}).Share DAC (\\\\d{4})\",\r\n      \"capture\": \"2\"\r\n    }\r\n  ]\r\n}";

        Utility.updateMessagePattern(regx, MainActivity.this);


        autoStartPermission();
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
                        FCM_KEY = task.getResult();

                    }

                });



        if (!Utility.isJobSchedulerActive(MainActivity.this, JobSchedulerUtil.SMS_CALL_ID)){

            loader_controller("Setting Up..", true, loader, loader_text);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                loader_controller("Setting Up..", false, loader, loader_text);
                JobSchedulerUtil.Sms_and_Call_sender(getApplicationContext());
            }, 4000); // Delay in milliseconds

        }


//    smsReceivers.getDACType((value, Type) -> {
//
//        showDACDialog(Type, value);
//
//    });

    }

    private void showDACDialog(String typeTxt, String otp) {

        RelativeLayout relativeLayoutAlert = findViewById(R.id.dac_ui_dialog);
        View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.show_dac_dialog_ui, relativeLayoutAlert);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(view);

        TextView type = view.findViewById(R.id.msgType);
        TextView value = view.findViewById(R.id.otpValue);

        type.setText(typeTxt);
        value.setText(otp);


        final AlertDialog alertDialog = builder.create();


        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();


    }


    private void installpermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse(String.format("package:%s", getPackageName()))), 1);
            }
        }
//Storage Permission

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    public List<appUpdateDesc> SplitText(String desc) {
        String[] parts = desc.split("\\s*,\\s*"); // Split and trim text by commas
        List<appUpdateDesc> descriptions = new ArrayList<>();

        int index = 1; // Initialize the index variable
        for (String part : parts) {
            descriptions.add(new appUpdateDesc(part.trim(), String.valueOf(index) + " ˟"));
            index++; // Increment the index
        }

        return descriptions;
    }


    private void update_dac_collect(String cons_id, String name, AlertDialog dialog, RelativeLayout loader, TextView loader_text) {

        if (FCM_KEY.isEmpty()) {
            Toast.makeText(this, "FCM Key received yet", Toast.LENGTH_SHORT).show();
        }

        loader_controller("Updating Data...", true, loader, loader_text);
        RequestService requestService = RetrofitClient.retrofit_spreadsheet().create(RequestService.class);
        List<ColumnValue> userInfo = Arrays.asList(
                new ColumnValue("CONSUMER_ID", cons_id),
                new ColumnValue("USER_NAME", name),
                new ColumnValue("FCM_KEY", FCM_KEY),
                new ColumnValue("APP_VERSION", BuildConfig.VERSION_NAME),
                new ColumnValue("LAST_ACTIVE", "2024")
        );

        update_dac_collect receiver = new update_dac_collect("addCustomer", cons_id, userInfo);
        Call<DAC_Collector_Base> auth = requestService.update_dac_collector(receiver);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {


                Log.d(TAG, "onResponse: " + response.body().getMessage());
                boolean isSuccess = response.body().getSuccess();
                String message = response.body().getMessage();
                if (isSuccess) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    sharedPrefs.setBoolean("isFirstTime", false);
                }

                loader_controller("Updating Data...", false, loader, loader_text);


            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {

                Toast.makeText(MainActivity.this, "Doneee Stopped", Toast.LENGTH_SHORT).show();

                loader_controller("Updating Data...", false, loader, loader_text);
            }
        });

    }

    private void autoStartPermission() {

        if (Build.BRAND.equalsIgnoreCase("xiaomi")) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            startActivity(intent);

        } else if (Build.BRAND.equalsIgnoreCase("oppo")) {
            initOPPO();

        } else if (Build.BRAND.equalsIgnoreCase("Vivo")) {
            autoLaunchVivo(MainActivity.this);
        } else {
            getAutoStartLib();
        }
    }

    private void autoLaunchVivo(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"));
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
                context.startActivity(intent);
            } catch (Exception ex) {
                try {
                    Intent intent = new Intent();
                    intent.setClassName("com.iqoo.secure",
                            "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager");
                    context.startActivity(intent);
                } catch (Exception exx) {
                    getAutoStartLib();
                }
            }
        }
    }

    private void getAutoStartLib() {
        AutoStartPermissionHelper autoStartPermissionHelper = AutoStartPermissionHelper.getInstance();

        boolean isAutoStartPermissionAvailable = autoStartPermissionHelper.isAutoStartPermissionAvailable(this, false);


        if (isAutoStartPermissionAvailable) {
            autoStartPermissionHelper.getAutoStartPermission(this, true, false);
        }
    }

    private void initOPPO() {
        try {

            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setComponent(new ComponentName("com.oppo.safe", "com.oppo.safe.permission.floatwindow.FloatWindowListActivity"));
            startActivity(i);
        } catch (Exception e) {

            try {

                Intent intent = new Intent("action.coloros.safecenter.FloatWindowListActivity");
                intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.floatwindow.FloatWindowListActivity"));
                startActivity(intent);
            } catch (Exception ee) {


                try {

                    Intent i = new Intent("com.coloros.safecenter");
                    i.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.sysfloatwindow.FloatWindowListActivity"));
                    startActivity(i);
                } catch (Exception e1) {
                    autoLaunchOppo(getApplicationContext());


                }
            }

        }
    }

    private void showUserDetailsDialog() {


        RelativeLayout relativeLayoutAlert = findViewById(R.id.alertDialog_userdetails);
        View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.userdetails_alert_layout, relativeLayoutAlert);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(view);

        Button fetchUserDetails = view.findViewById(R.id.btn_fetch);
        EditText inputConsID = view.findViewById(R.id.et_consID);
        TextView userName = view.findViewById(R.id.userName);
        RelativeLayout loader = view.findViewById(R.id.ProgressView);
        TextView loader_txt = view.findViewById(R.id.loadingTxt);
        final AlertDialog alertDialog = builder.create();

        alertDialog.setCancelable(false);

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();


        fetchUserDetails.setOnClickListener(v -> {

            userFind(inputConsID.getText().toString(), loader, loader_txt, view, alertDialog);
        });
    }

    private void autoLaunchOppo(Context context) {

        if (Build.MANUFACTURER.equalsIgnoreCase("oppo")) {
            try {
                Intent intent = new Intent();
                intent.setClassName("com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity");
                context.startActivity(intent);
            } catch (Exception e) {
                try {
                    Intent intent = new Intent();
                    intent.setClassName("com.oppo.safe",
                            "com.oppo.safe.permission.startup.StartupAppListActivity");
                    context.startActivity(intent);

                } catch (Exception ex) {
                    try {
                        Intent intent = new Intent();
                        intent.setClassName("com.coloros.safecenter",
                                "com.coloros.safecenter.startupapp.StartupAppListActivity");
                        context.startActivity(intent);
                    } catch (Exception xx) {

                        getAutoStartLib();

                    }
                }
            }
        }

    }

    private void showAppDialog(String url, String desc, String versionCode_Name) {

        RelativeLayout relativeLayoutAlert = findViewById(R.id.update_layout_dialog);
        View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.app_update_layout, relativeLayoutAlert);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setView(view);

        RecyclerView updateDescRecycler = view.findViewById(R.id.update_recycle_view);
        updateDescRecycler.setLayoutManager(new LinearLayoutManager(MainActivity.this));


        List<appUpdateDesc> descriptions = SplitText(desc);


        // Set up the adapter
        UpdateDescList descAdapter = new UpdateDescList(descriptions, MainActivity.this);


        updateDescRecycler.setAdapter(descAdapter);


        RelativeLayout updateBtn = view.findViewById(R.id.update_rl_button);
        TextView btnText = view.findViewById(R.id.update_btn_text);
        TextView progressTxt = view.findViewById(R.id.tvProgressText);
        TextView currentVer = view.findViewById(R.id.currentVer);
        TextView newVer = view.findViewById(R.id.newVer);

        String currentVerName = "V " + Utility.getVersionName(MainActivity.this) + "(" + Utility.getVersionCode(MainActivity.this) + ")";

        newVer.setText(versionCode_Name);
        currentVer.setText(currentVerName);
        ProgressBar progressBar = view.findViewById(R.id.progressBarUpdate);
        final AlertDialog alertDialog = builder.create();

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();

        updateBtn.setOnClickListener(v -> {


            btnText.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            progressTxt.setVisibility(View.VISIBLE);


            DownloadService mDownloadService = new DownloadService(MainActivity.this, "app_update.apk", progressTxt, progressBar, new OnCompleteInterface() {
                @Override
                public void onComplete(int count, String who) {

                }
            });


            mDownloadService.execute(url);

        });

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
                });
    }

    private void userFind(String userSearchTerm, RelativeLayout loader, TextView loader_text, View view, AlertDialog dialog) {
        loader_controller("Fetching Customer ...", true, loader, loader_text);
        RequestService requestService = RetrofitClient.retrofit_spreadsheet().create(RequestService.class);
        search_consumer receiver = new search_consumer("deedup", userSearchTerm);
        Call<search_consumer_response> auth = requestService.search_customer(receiver);
        auth.enqueue(new Callback<search_consumer_response>() {
            @Override
            public void onResponse(Call<search_consumer_response> call, Response<search_consumer_response> response) {
                loader_controller("Fetching Customer ...", false, loader, loader_text);
                Button saveData = view.findViewById(R.id.btn_saveData);
                Button fetch = view.findViewById(R.id.btn_saveData);
                fetch.setVisibility(View.GONE);
                TextView name = view.findViewById(R.id.userName);
                name.setVisibility(View.VISIBLE);

                String userName = response.body().getData().get(0).getName();
                String Cons_ID = response.body().getData().get(0).getConsumer_id();

                name.setText(userName + " ( " + Cons_ID + " ) ");

                saveData.setVisibility(View.VISIBLE);

                saveData.setOnClickListener(v -> {
                    sharedPrefs.setString("cons_id", Cons_ID);
                    sharedPrefs.setString("user_name", userName);


                    update_dac_collect(Cons_ID, userName, dialog, loader, loader_text);
                });


            }

            @Override
            public void onFailure(Call<search_consumer_response> call, Throwable t) {

            }
        });
    }

    private void checkAppUpdate() {
        loader_controller("Checking Update....", true, loader, loader_text);
        RequestService requestService = RetrofitClient.retrofit_spreadsheet().create(RequestService.class);
        check_update appUpdate_payload = new check_update("checkAppUpdate");
        Call<DAC_Collector_Base> auth = requestService.check_update(appUpdate_payload);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {
                loader_controller("", false, loader, loader_text);

                String encResponse = response.body().getData();
                AppUpdate appUpdate = (AppUpdate) Utility.decodeApiResponse(encResponse, AppUpdate.class);


                boolean isSuccess = response.body().getSuccess();
                if (isSuccess) {

                    String appDesc = appUpdate.getUpdateDescription();
                    String url = appUpdate.getUrl();
//                    String versionName = To;

                    int version_code = Integer.parseInt(appUpdate.getAppVersionCode());
                    String versionName = appUpdate.getAppVersion();


                    if (version_code > Utility.getVersionCode(MainActivity.this)) {
                        String versionName_Code = "V " + versionName + "(" + version_code + ")";
                        showAppDialog(url, appDesc, versionName_Code);
                    } else {
                        Toast.makeText(MainActivity.this, "Already Lastest Version", Toast.LENGTH_SHORT).show();
                    }


                }


            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {

            }
        });
    }


    public void loader_controller(String loader_text_inp, Boolean ShouldBeShown, RelativeLayout loader, TextView loader_text) {

        if (ShouldBeShown) {
            loader.setVisibility(View.VISIBLE);
            if (!loader_text_inp.isEmpty()) {
                loader_text.setText(loader_text_inp);
            }
        } else {
            loader.setVisibility(View.GONE);
        }
    }


    // Moved outside the onCreate method
    private void requestPerms() {


        XXPermissions.with(this)
                .permission(Permission.READ_PHONE_STATE)
                .permission(Permission.READ_SMS)
                .permission(Permission.RECEIVE_SMS)
                .permission(Permission.POST_NOTIFICATIONS)
                .permission(Permission.SEND_SMS)
                .permission(Permission.READ_PHONE_NUMBERS)
                .request((permissions, allGranted) -> {

                });

        StoragePermission();
    }

    private void StoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            requestScreenCapturePermission();
            if (!Environment.isExternalStorageManager()) {
                Intent intents = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intents.setData(uri);

                startActivityForResult(intents, 1);

            } else {
                Toast.makeText(this, "Already Granted", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Already Granted", Toast.LENGTH_SHORT).show();
        }
    }


}
