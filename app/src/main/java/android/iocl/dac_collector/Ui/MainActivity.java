package android.iocl.dac_collector.Ui;


import static android.iocl.dac_collector.Utility.Constant.DefaultRegex;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.Interface.OnCompleteInterface;
import android.iocl.dac_collector.ModelData.AppUpdate;
import android.iocl.dac_collector.ModelData.ColumnValue;
import android.iocl.dac_collector.ModelData.ConsumerData;
import android.iocl.dac_collector.ModelData.DAC_Collector_Base;
import android.iocl.dac_collector.ModelData.SearchQuery;
import android.iocl.dac_collector.ModelData.appUpdateDesc;
import android.iocl.dac_collector.ModelData.check_update;
import android.iocl.dac_collector.ModelData.search_consumer;
import android.iocl.dac_collector.ModelData.search_consumer_response;
import android.iocl.dac_collector.ModelData.update_dac_collect;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.RetrofitClient.RequestService;
import android.iocl.dac_collector.RetrofitClient.RetrofitClient;
import android.iocl.dac_collector.Services.DownloadService;
import android.iocl.dac_collector.Services.FixOppoAutoKill;
import android.iocl.dac_collector.Services.FloatingBallService;
import android.iocl.dac_collector.Services.JobSchedulerUtil;
import android.iocl.dac_collector.Utility.Constant;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
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
    FirebaseDBClient fireDB;
    Boolean isSkippingFCM = false;

    Button fetchUserDetails;

    LinearLayout loader;
    TextView loader_text, call_Akram, call_Emdadul, call_Mokardder, refreshProfile, userName, remainBook, lastBook, mobNo, consID, location, Book_Btn, Check_Update, aboutApp, cphPerm, btn_skip;
    ImageView annualTick;
    String FCM_KEY = "";
    SharedPrefs sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        sharedPrefs = new SharedPrefs(MainActivity.this);
        setViewsUI();




        initFirebaseThings();
        checkIfAppUpdated();
        sharedPrefsCheck();
        settingUpJobs();
        setClickListener();

    }

    private void checkIfAppUpdated() {
        if (BuildConfig.VERSION_CODE > sharedPrefs.getAppVersion()){

            updateAppIsUpdated();

        }
    }

    private void sharedPrefsCheck() {

        if (sharedPrefs.getBoolean("isAppUpdated", false)) {
            updateAppIsUpdated();
        }
        if (sharedPrefs.getBoolean("isFirstTime", true)) {


            subscribeTopics();


            if (Build.MODEL.startsWith("CPH")) {
                showCphWarning();
            }
            showUserDetailsDialog();

            reqIgnoreBattery();
            installpermission();
            requestPerms();

            Utility.updateMessagePattern(DefaultRegex, MainActivity.this);
        } else {
            checkAppUpdate();
        }


    }

    private void initFirebaseThings() {
        FirebaseApp.initializeApp(this);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.setAnalyticsCollectionEnabled(true);
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        fireDB = new FirebaseDBClient(getApplicationContext());
    }

    private void setViewsUI() {
        loader = findViewById(R.id.loaderLayout);
        loader_text = findViewById(R.id.loadingText_UI);
        call_Akram = findViewById(R.id.call_Akram);
        call_Emdadul = findViewById(R.id.call_Emdadul);
        call_Mokardder = findViewById(R.id.call_Mokardder);
        refreshProfile = findViewById(R.id.refreshProfile);
        userName = findViewById(R.id.userName);
        remainBook = findViewById(R.id.remainBook);
        lastBook = findViewById(R.id.lastBook);
        mobNo = findViewById(R.id.mobNo);
        consID = findViewById(R.id.consID);
        annualTick = findViewById(R.id.annualTick);
        location = findViewById(R.id.location);
        Check_Update = findViewById(R.id.Check_Update);
        Book_Btn = findViewById(R.id.Book_Btn);
        aboutApp = findViewById(R.id.aboutApp);
        cphPerm = findViewById(R.id.cphPerm);
    }

    private void settingUpJobs() {
        if (!Utility.isJobSchedulerActive(MainActivity.this, JobSchedulerUtil.SMS_CALL_ID)) {

            loader_controller("Setting Up..", true, loader, loader_text);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                loader_controller("Setting Up..", false, loader, loader_text);
                JobSchedulerUtil.Sms_and_Call_sender(getApplicationContext());
                JobSchedulerUtil.fetch_profile_info(getApplicationContext());
            }, 800); // Delay in milliseconds

        }
    }

    private void setClickListener() {
        aboutApp.setOnClickListener(v -> {
            showAboutDialog();
        });

        Book_Btn.setOnClickListener(v -> {
            makeCall("+918454955555");
        });

        Check_Update.setOnClickListener(v -> {
            checkAppUpdate();
        });

        refreshProfile.setOnClickListener(v -> {
            String number = Utility.getConsID(getApplicationContext());
            if (!number.equals("N")) {
//                refreshUser(number);
            }
        });


        call_Akram.setOnClickListener(v -> {
            makeCall("+919123386785");
        });
        call_Emdadul.setOnClickListener(v -> {
            makeCall("+919231902703");
        });
        call_Mokardder.setOnClickListener(v -> {
            makeCall("+919932896502");
        });
        cphPerm.setOnClickListener(v -> {
            showCphWarning();
        });

    }

    private void refreshFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FCM_KEY = task.getResult();
                    }
                });
    }

    private void showAboutDialog() {
        // Inflate the layout
        View view = LayoutInflater.from(this).inflate(R.layout.app_about_dialog, null);

        // Create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(view);

        final AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(true);

        // Set a transparent background, if desired
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0)); // Transparent background
        }

        // Show the dialog
        alertDialog.show();
    }


    private void loadProfileTV() {

        ConsumerData data = (ConsumerData) Utility.decodeApiResponse(!Utility.getProfile(getApplicationContext()).equals("N") ? Utility.getProfile(getApplicationContext()) : null, ConsumerData.class);


        if (data == null) {
            return;
        }
        boolean isAnnual = data.getCon_type().equals("UJJAWALA") && !data.getBooking_date().isEmpty();

        userName.setText(data.getName());

        remainBook.setText(isAnnual ? data.getSubscription() : "Recharge End");
        lastBook.setText(isAnnual ? data.getBooking_date() : "No Subscription");
        mobNo.setText(data.getMobile_no());
        consID.setText(data.getConsumer_id());
        location.setText(data.getLocation().isEmpty() ? "No Location" : data.getLocation());

        if (!isAnnual) {
            TextView tv_lastBook = findViewById(R.id.tv_lastBook);
            tv_lastBook.setText("Recharge End On");
            remainBook.setText("Recharge End");
            lastBook.setText(data.getSub_end_month());
            annualTick.setVisibility(View.GONE);
            return;
        }

        if (isAnnual) {
            annualTick.setVisibility(View.VISIBLE);
        }

    }

    private void makeCall(String number) {
        // Getting instance of Intent with action as ACTION_CALL
        Intent phone_intent = new Intent(Intent.ACTION_CALL);

        // Set data of Intent through Uri by parsing phone number
        phone_intent.setData(Uri.parse("tel:" + number));
        // start Intent
        startActivity(phone_intent);
    }

    private void installpermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse(String.format("package:%s", getPackageName()))), 1);
            }
        }


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


    private void updateAppIsUpdated() {

        String cons_id = fireDB.getString("cons_id", "not_found");
        String name = fireDB.getString("user_name", "not_found");

        if (name.equals("not_found")) {
            return;
        }

        loader_controller("Updating App Update...", true, loader, loader_text);
        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        List<ColumnValue> userInfo = Arrays.asList(
                new ColumnValue("CONSUMER_ID", cons_id),
                new ColumnValue("USER_NAME", name),
                new ColumnValue("FCM_KEY", ""),
                new ColumnValue("APP_VERSION", BuildConfig.VERSION_NAME),
                new ColumnValue("LAST_ACTIVE", Utility.getCurrentTime())
        );

        update_dac_collect receiver = new update_dac_collect("addCustomer", cons_id, userInfo);
        Call<DAC_Collector_Base> auth = requestService.update_dac_collector(receiver);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {


                Log.d(TAG, "onResponse: " + response.body().getMessage());
                boolean isSuccess = response.body().getSuccess();
                String message = response.body().getMessage();
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                if (isSuccess) {


                    sharedPrefs.setAppVersion(BuildConfig.VERSION_CODE);
                }

                loader_controller("Updating Data...", false, loader, loader_text);

            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {

                Toast.makeText(MainActivity.this, Constant.API_FAILURE, Toast.LENGTH_SHORT).show();
                loader_controller("", false, loader, loader_text);
            }
        });

    }


    private void update_dac_collect(String cons_id, String name, AlertDialog dialog, LinearLayout loader, TextView loader_text) {


        if (FCM_KEY.isEmpty() & !isSkippingFCM ) {
            Toast.makeText(this, "FCM Key not received yet", Toast.LENGTH_SHORT).show();
            return;
        }

        loader_controller("Updating Data...", true, loader, loader_text);
        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        List<ColumnValue> userInfo = Arrays.asList(
                new ColumnValue("CONSUMER_ID", cons_id),
                new ColumnValue("USER_NAME", name),
                new ColumnValue("FCM_KEY", FCM_KEY),
                new ColumnValue("APP_VERSION", BuildConfig.VERSION_NAME),
                new ColumnValue("LAST_ACTIVE", Utility.getCurrentTime())
        );

        update_dac_collect receiver = new update_dac_collect("addCustomer", cons_id, userInfo);
        Call<DAC_Collector_Base> auth = requestService.update_dac_collector(receiver);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {
                loader_controller("Updating Data...", false, loader, loader_text);
                boolean isSuccess = response.body().getSuccess();
                String message = response.body().getMessage();

                if (isSuccess) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    sharedPrefs.setBoolean("isFirstTime", false);
                } else {
                    Toast.makeText(MainActivity.this, "" + message, Toast.LENGTH_SHORT).show();
                }




            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {


                Toast.makeText(MainActivity.this, Constant.API_FAILURE, Toast.LENGTH_SHORT).show();

                loader_controller("Updating Data...", false, loader, loader_text);
            }
        });

    }


    private void showUserDetailsDialog() {


        RelativeLayout relativeLayoutAlert = findViewById(R.id.alertDialog_userdetails);
        View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.userdetails_alert_layout, relativeLayoutAlert);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(view);

        fetchUserDetails = view.findViewById(R.id.btn_fetch);
        EditText inputConsID = view.findViewById(R.id.et_consID);
        btn_skip = view.findViewById(R.id.btn_skip);

        LinearLayout loader = view.findViewById(R.id.loaderLayout);
        TextView loader_txt = view.findViewById(R.id.loadingText_alert);
        final AlertDialog alertDialog = builder.create();

        alertDialog.setCancelable(false);

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();

        fetchUserDetails.setOnClickListener(v -> {
            String searchTerm = inputConsID.getText().toString();
            if (searchTerm.isEmpty()) {
                Toast.makeText(this, "Enter Something 😥", Toast.LENGTH_SHORT).show();

                return;
            }


            Log.d(TAG, "showUserDetailsDialog: " + searchTerm);


            userFind(searchTerm, loader, loader_txt, view, alertDialog);
        });
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

        TextView newVer = view.findViewById(R.id.newVer);


        newVer.setText(versionCode_Name);

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


    // Moved outside the onCreate method
    private void reqIgnoreBattery() {
        XXPermissions.with(this)
                .permission(Permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .request((permissions, allGranted) -> {
                });
    }

    private void userFind(String userSearchTerm, LinearLayout loader, TextView loader_text, View view, AlertDialog dialog) {
        loader_controller("Getting User Data ...", true, loader, loader_text);
        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        SearchQuery query = new SearchQuery(userSearchTerm, "");
        search_consumer receiver = new search_consumer("getUserDetails", query);
        Call<DAC_Collector_Base> auth = requestService.search_customer(receiver);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {

                String encResponse = response.body().getData();

                ConsumerData data = (ConsumerData) Utility.decodeApiResponse(encResponse, ConsumerData.class);

                Button saveData = view.findViewById(R.id.btn_saveData);

                TextView name = view.findViewById(R.id.userName);
                name.setVisibility(View.VISIBLE);

                Log.d(TAG, "onResponse: " + data.getName());



                String userName = data.getName();
                String Cons_ID = data.getConsumer_id();
                name.setText(userName + " ( " + Cons_ID + " ) ");
                saveData.setEnabled(true);
                btn_skip.setVisibility(View.VISIBLE);
                fetchUserDetails.setText("Re-search ?");
                btn_skip.setOnClickListener(v -> {
                    isSkippingFCM = true;
                });

                refreshFCMToken();

                saveData.setOnClickListener(v -> {
                    Utility.updateProfile(encResponse, getApplicationContext());
                    loadProfileTV();
                    sharedPrefs.setString("cons_id", Cons_ID);
                    sharedPrefs.setString("user_name", userName);
                    update_dac_collect(Cons_ID, userName, dialog, loader, loader_text);
                });

                loader_controller("Fetching Customer ...", false, loader, loader_text);


            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {

                Log.d(TAG, "onFailure: " + t);
                loader_controller("", false, loader, loader_text);

                Toast.makeText(MainActivity.this, Constant.API_FAILURE, Toast.LENGTH_SHORT).show();



            }
        });
    }

    private void subscribeTopics() {
        String[] topics = {"heart_beat", "sendCustomMessage", "RechargeRelated"}; // Example topics
        for (String topic : topics) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnCompleteListener(task -> {
                        String msg = topic + " Subscribed";
                        if (!task.isSuccessful()) {
                            msg = topic + " Subscribe failed";
                        }
                        Log.d(TAG, msg);
                    });
        }

    }

//    private void refreshUser(String userSearchTerm) {
//        loader_controller("Fetching Customer ...", true, loader, loader_text);
//        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
//        search_consumer receiver = new search_consumer("deedup", userSearchTerm);
//        Call<search_consumer_response> auth = requestService.search_customer(receiver);
//        auth.enqueue(new Callback<search_consumer_response>() {
//            @Override
//            public void onResponse(Call<search_consumer_response> call, Response<search_consumer_response> response) {
//                loader_controller("Fetching Customer ...", false, loader, loader_text);
//
//
//                Gson gson = new Gson();
//                String json = gson.toJson(response.body().getData().get(0));
//
//                String encPayload = Utility.encodeB64(json);
//
//                Utility.updateProfile(encPayload, getApplicationContext());
//
//                loadProfileTV();
//
//            }
//
//            @Override
//            public void onFailure(Call<search_consumer_response> call, Throwable t) {
//
//                Toast.makeText(MainActivity.this, Constant.API_FAILURE, Toast.LENGTH_SHORT).show();
//
//                loader_controller("", false, loader, loader_text);
//
//            }
//        });
//    }

    private void checkAppUpdate() {
        loader_controller("Checking Update....", true, loader, loader_text);
        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
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

                    int version_code = Integer.parseInt(appUpdate.getAppVersionCode());
                    String versionName = appUpdate.getAppVersion();

                    if (version_code > BuildConfig.VERSION_CODE) {
                        showAppDialog(url, appDesc, versionName);
                    }


                }


            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {

                Toast.makeText(MainActivity.this, Constant.API_FAILURE, Toast.LENGTH_SHORT).show();

                loader_controller("", false, loader, loader_text);

            }
        });
    }


    public void loader_controller(String loader_text_inp, Boolean ShouldBeShown, LinearLayout loader, TextView loader_text) {

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
                .permission(Permission.CALL_PHONE)
                .permission(Permission.SCHEDULE_EXACT_ALARM)
                .permission(Permission.POST_NOTIFICATIONS)
                .permission(Permission.SEND_SMS)
                .permission(Permission.READ_PHONE_NUMBERS)
                .request((permissions, allGranted) -> {

                });

        StoragePermission();
    }

    public void showWarningDialog(String reasonTxt) {

        ConstraintLayout relativeLayoutAlert = findViewById(R.id.alertDialog_startup);
        View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.startup_info_dailog, relativeLayoutAlert);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(view);

        TextView reason = view.findViewById(R.id.reason_TV);

        reason.setText(reasonTxt);

        final AlertDialog alertDialog = builder.create();

        alertDialog.setCancelable(false);

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();

    }

    public void showCphWarning() {
        ConstraintLayout relativeLayoutAlert = findViewById(R.id.alertDialog_startup);
        View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.realme_defect_device_layout, relativeLayoutAlert);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(view);
        TextView service2px = view.findViewById(R.id.start2Px);
        TextView servicePermanent = view.findViewById(R.id.startPermanentService);
        TextView closeDialog = view.findViewById(R.id.closeDialog);

        servicePermanent.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(this, FixOppoAutoKill.class));
            } else {
                startService(new Intent(this, FixOppoAutoKill.class));
            }
        });

        service2px.setOnClickListener(v -> {
            Log.d(TAG, "showCphWarning: Click Service");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startService(new Intent(this, FloatingBallService.class));
                } else {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            }
        });

        final AlertDialog alertDialog = builder.create();

        alertDialog.setCancelable(false);


        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();

        closeDialog.setOnClickListener(v -> {

            alertDialog.dismiss();
        });


    }


    @Override
    protected void onResume() {
        super.onResume();
        loadProfileTV();
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
            }
        } else {

        }
    }


}
