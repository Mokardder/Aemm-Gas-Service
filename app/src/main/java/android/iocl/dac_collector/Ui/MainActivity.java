package android.iocl.dac_collector.Ui;

import static android.iocl.dac_collector.Utility.Constant.DefaultRegex;

import android.annotation.SuppressLint;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.Interface.CapturingInterceptor;
import android.iocl.dac_collector.Interface.ResponseListener;
import android.iocl.dac_collector.ModelData.AppUpdate;
import android.iocl.dac_collector.ModelData.ColumnValue;
import android.iocl.dac_collector.ModelData.ConsumerData;
import android.iocl.dac_collector.ModelData.DAC_Collector_Base;
import android.iocl.dac_collector.ModelData.PermissionItem;
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
import android.iocl.dac_collector.Services.JobSchedulerUtil;

import android.iocl.dac_collector.Utility.Constant;

import android.iocl.dac_collector.Utility.PermissionUtility;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.Utility;
import android.iocl.dac_collector.adapter.UpdateDescList;
import android.net.Uri;

import android.os.Bundle;
import android.os.Handler;

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



import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements ResponseListener {

    private static final String TAG = "MainActivity_Mokardder";


    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    FirebaseAnalytics mFirebaseAnalytics;

    Boolean isSkippingFCM = false;

    Button fetchUserDetails;
    LinearLayout loader;
    TextView subsidyBadge, loader_text, call_Akram, call_Emdadul, changeUser, tv_appVersion, call_Mokardder, refreshProfile, userName, remainBook, lastBook, mobNo, consID, Book_Btn, Check_Update, aboutApp, btn_skip;
    ImageView annualTick;
    MaterialCardView subsidyActivity;
    // FCM key - keep in sync with SharedPrefs
    String FCM_KEY = "";


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
            new AlertDialog.Builder(this).setTitle("App Crashed Previously").setMessage(crashDetails).setPositiveButton("OK", (dialog, which) -> {
                SharedPrefs.ClearCrashDetails(this);
                dialog.dismiss();
            }).setCancelable(true).show();
        }


        setContentView(R.layout.activity_main);


        //*** FIRST SET CONTENTVIEW ***//

//
//        new NewsDialog.Builder(this)
//                .setTitle("গুরুত্বপূর্ণ মেসেজ")
//                .setContent("আমরা বর কোম্পানি")
//                .setNewsType(NewsDialog.NewsType.WARNING)
//                .setPrimaryButtonText("I Understand")
//                .setBanglaFont(true)
//                .show();



        checkMissingPermissions();
        setViewsUI();
        initFirebaseThings();      // <-- improved token init
        sharedPrefsCheck();

        settingUpJobs();

        setClickListener();

        checkIfAppUpdated();


    }


    private void checkMissingPermissions() {


//        executorService.execute(() -> {
        boolean isOnce = getIntent().getBooleanExtra("SKIP_ONCE", false);
        boolean isPermanent = SharedPrefs.getPermanentlySkipping(this);


        if (!isOnce && !isPermanent) {

            if (PermissionUtility.isAnyPermissionMissing(this, false)) {
                List<String> missing = PermissionUtility.getMissingPermissions(this, false);
                List<PermissionItem> newList = PermissionUtility.buildPermissionItemList(missing);

                Log.d(TAG, "checkMissingPermissions: missing " + missing + " items " + newList);

                boolean hasMandatoryMissing = false;
                StringBuilder optionalMissing = new StringBuilder();

                for (PermissionItem item : newList) {
                    Log.d(TAG, "checkMissingPermissions: " + item.getTitle() +
                            " (optional=" + item.isOptional() + ")");
                    if (!item.isOptional()) {
                        hasMandatoryMissing = true;
                    } else {
                        if (optionalMissing.length() > 0) optionalMissing.append(", ");
                        optionalMissing.append(item.getTitle());
                    }
                }

                if (hasMandatoryMissing) {
                    // Mandatory missing → go to PermissionActivity
                    Intent start = new Intent(MainActivity.this, PermissionActivity.class);
                    startActivity(start);
                } else if (optionalMissing.length() > 0) {


                    TextView permissionTV = findViewById(R.id.permissionPage);

                    if (permissionTV.getVisibility() == View.GONE) {
                        permissionTV.setVisibility(View.VISIBLE);
                    }
                    permissionTV.setOnClickListener(v -> startActivity(new Intent(this, PermissionActivity.class).putExtra("showMandatory", false)));
                    // Only optional missing → just show toast

                }

                return;
            }



        }

        if (isPermanent) {

            if (PermissionUtility.isAnyPermissionMissing(this, true)) {
                TextView permissionTV = findViewById(R.id.permissionPage);

                if (permissionTV.getVisibility() == View.GONE) {
                    permissionTV.setVisibility(View.VISIBLE);
                }

                Log.d(TAG, "checkMissingPermissions: going from here 1" );
                permissionTV.setOnClickListener(v -> startActivity(new Intent(this, PermissionActivity.class).putExtra("showMandatory", false)));
            }

        }
    }


    @SuppressLint("SetTextI18n")
    private void sharedPrefsCheck() {
        Context ctx = MainActivity.this;
        String username = SharedPrefs.getUsername(ctx);
        String consumerId = SharedPrefs.getConsumerId(ctx);

        boolean missingInfo = "not_found".equals(username) || username.isEmpty() || "not_found".equals(consumerId) || consumerId.isEmpty();


        if (SharedPrefs.isFirstTime(ctx)) {

            subscribeTopics();
            SharedPrefs.setRestrictionEnabled(ctx);


            executorService.execute(() -> Utility.updateMessagePattern(DefaultRegex, ctx));

        }
        if (missingInfo){
            showUserDetailsDialog();
        }


        if (!SharedPrefs.getSubsidyDetails(ctx).isEmpty()) {
            String last = SharedPrefs.lastSubsidyDate(ctx); // "dd-MM-yyyy"
            try {
                long days = (Calendar.getInstance().getTimeInMillis() - new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).parse(last).getTime()) / 86_400_000L;
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
                subsidyBadge.setText("Your Request is in Pending");
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

                }

                // Ensure auto init and then fetch a fresh token in background
                FirebaseMessaging.getInstance().setAutoInitEnabled(true);


            } catch (Exception ignored) {

            }
        });

    }

    private void setViewsUI() {
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
        changeUser = findViewById(R.id.changeUser);

        Check_Update = findViewById(R.id.Check_Update);
        Book_Btn = findViewById(R.id.Book_Btn);
        aboutApp = findViewById(R.id.aboutApp);
        tv_appVersion = findViewById(R.id.tv_appVersion);
        subsidyActivity = findViewById(R.id.cardSubsidyHeader);

    }



    // TODO: Imlement to check if app is recently updated
    private void settingUpJobs() {


//        imageObserverSchedule();

        if (!Utility.isJobSchedulerActive(MainActivity.this, 1)) {
            JobSchedulerUtil.Sms_and_Call_sender(getApplicationContext());
            JobSchedulerUtil.fetch_profile_info(getApplicationContext());


        }

    }

    private void setClickListener() {


        // As We've hidden the icom it should not require
//        checkServerMessage();






        aboutApp.setOnClickListener(v -> showAboutDialog());

        Book_Btn.setOnClickListener(v -> makeCall("+918454955555"));

        Check_Update.setOnClickListener(v -> checkAppUpdate());

        refreshProfile.setOnClickListener(v -> {
            refreshUser(SharedPrefs.getUserID(this));
        });


        call_Akram.setOnClickListener(v -> makeCall("+919123386785"));
        call_Emdadul.setOnClickListener(v -> makeCall("+919231902703"));
        call_Mokardder.setOnClickListener(v -> makeCall("+919932896502"));

        subsidyActivity.setOnClickListener(view -> {
            String subsidyValue = SharedPrefs.getSubsidyDetails(this);
            if (!subsidyValue.isEmpty()) {
                if (subsidyValue.equals("W10=")) {
                    Toast.makeText(this, "Received Invalid Subsidy Data", Toast.LENGTH_SHORT).show();
                    SharedPrefs.clearSubsidyDetails(this);
                    SharedPrefs.setIsSubsidyRequestPending(this, false);

                    showSubsidyDialog();

                    return;
                }
                startActivity(new Intent(this, BankStatementActivity.class));
            } else {

                showSubsidyDialog();

            }
        });


        tv_appVersion.setText(BuildConfig.VERSION_NAME + (BuildConfig.DEBUG ? " DEBUG MODE 🐞" : ""));
        changeUser.setOnClickListener(view -> showUserDetailsDialog());
    }

//    private void checkServerMessage() {
//        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
//
//        FirebaseRemoteConfigSettings configSettings =
//                new FirebaseRemoteConfigSettings.Builder()
//                        .setMinimumFetchIntervalInSeconds(0) // always fetch fresh
//                        .build();
//        remoteConfig.setConfigSettingsAsync(configSettings);
//
//        remoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
//            if (task.isSuccessful()) {
//                Log.d("RemoteConfig", "Fetch and activate succeeded.");
//
//                Map<String, FirebaseRemoteConfigValue> allValues = remoteConfig.getAll();
//                for (Map.Entry<String, FirebaseRemoteConfigValue> entry : allValues.entrySet()) {
//                    Log.d("RemoteConfig", entry.getKey() + " = " + entry.getValue().asString());
//                }
//
//                boolean show = remoteConfig.getBoolean("news_enabled");
//                if (show) {
//                    String title = remoteConfig.getString("news_title");
//                    String body = remoteConfig.getString("news_body");
//                    String type = remoteConfig.getString("news_type");
//                    String topTitle = remoteConfig.getString("top_title");
//                    String published_time = remoteConfig.getString("published_time");
//
//                    new NewsDialog.Builder(this)
//                            .setTitle(title)
//                            .setContent(body)
//                            .setTopTitle(topTitle)
//                            .setPublishTime(published_time)
//                            .setNewsType(NewsDialog.NewsType.valueOf(type.toUpperCase()))
//                            .setPrimaryButtonText("I Understand")
//                            .setBanglaFont(true)
//                            .show();
//                }
//            } else {
//                Log.e("RemoteConfig", "Fetch failed", task.getException());
//            }
//        });
//    }



    private void refreshFCMToken(TokenCallback cb) {
        try {
            // ensure FirebaseApp initialized
            if (FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
                FirebaseApp.initializeApp(getApplicationContext());
            }

            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
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


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
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
            loader_controller("Requesting...", true, loader, loader_text);

            alertDialog.dismiss();

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


    private void checkIfAppUpdated () {
        if (!SharedPrefs.getConsumerId(getApplicationContext()).isEmpty()) {
            if (Utility.isAppUpdatedRecently(this)) {
                update_dac_collect(SharedPrefs.getConsumerId(this), SharedPrefs.getUsername(this), null, loader, loader_text);
            }
        }
    }

    private void loadProfileTV() {

        ConsumerData data = (ConsumerData) Utility.decodeApiResponse(!Utility.getProfile(getApplicationContext()).equals("N") ? Utility.getProfile(getApplicationContext()) : null, ConsumerData.class);


        if (data == null) {
            return;
        }
        boolean isAnnual = data.getCon_type().equals("UJJAWALA") && !data.getBooking_date().isEmpty();
        boolean isUjjawala = data.getCon_type().equals("UJJAWALA");
        boolean isGeneralAutomatedOn = !data.getCon_type().equals("UJJAWALA") && !data.getSubscription().isEmpty();

        userName.setText(data.getName());


        remainBook.setText(isAnnual ? data.getSubscription() : "Recharge End");
        lastBook.setText(isAnnual ? data.getBooking_date() : "No Subscription");
        mobNo.setText(data.getMobile_no() + (!data.getAlternate_number().isEmpty() ? " / " + data.getAlternate_number() : ""));
        consID.setText(data.getConsumer_id());

        TextView tv_lastBook = findViewById(R.id.tv_lastBook);
        if (!isAnnual) {

            tv_lastBook.setText("Recharge End On");
            remainBook.setText("Recharge End");
            lastBook.setText(data.getSub_end_month());
            annualTick.setVisibility(View.GONE);

        }

        if (!isUjjawala) {

            remainBook.setText("Unlimited");
            tv_lastBook.setText("Automatic Book");
            lastBook.setText("Turned ON");
            lastBook.setTextColor(Color.parseColor("#006400"));
            annualTick.setVisibility(View.VISIBLE);
            if (!isGeneralAutomatedOn) {
                annualTick.setVisibility(View.GONE);
                remainBook.setText("❌");
                tv_lastBook.setText("Automatic Book");
                lastBook.setText("Turned Off");
                lastBook.setTextColor(Color.parseColor("#FF0000"));

            }
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
            descriptions.add(new appUpdateDesc(part.trim(), index + " ˟"));
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
            refreshFCMToken(token -> runOnUiThread(() -> {
                if (token == null || token.isEmpty()) {
                    loader_controller("Updating Data...", false, loader, loader_text);
                    Toast.makeText(MainActivity.this, "FCM Key not received yet", Toast.LENGTH_SHORT).show();
                    return;
                }
                // proceed with real token
                performUpdate(cons_id, name, dialog, loader, loader_text, token);
            }));
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

        List<ColumnValue> userInfo = Arrays.asList(new ColumnValue("CONSUMER_ID", cons_id), new ColumnValue("USER_NAME", name), new ColumnValue("FCM_KEY", token), new ColumnValue("APP_VERSION", BuildConfig.VERSION_NAME), new ColumnValue("LAST_ACTIVE", Utility.getCurrentTime()));

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
                    SharedPrefs.setFirstTime(MainActivity.this, false);
                    if (cons_id.trim().isEmpty()) {
                        Toast.makeText(MainActivity.this, "Not Valid conumer ID", Toast.LENGTH_LONG).show();

                    }
                    subscribeToConsID(cons_id);
                } else {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
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
        SubsidyRequest req = new SubsidyRequest("addSubsidyRequest", SharedPrefs.getConsumerId(ctx), SharedPrefs.getUsername(ctx), FCM_KEY_param);

        Call<DAC_Collector_Base> auth = requestService.requestSubsidyDetails(req);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {
                loader_controller("Requesting...", false, loader, loader_text);
                DAC_Collector_Base body = response.body();

                if (body == null) {
                    Toast.makeText(MainActivity.this, "Empty response from server", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean isSuccess = Boolean.TRUE.equals(body.getSuccess());


                String message = body.getMessage() != null ? body.getMessage() : "Operation failed";

                if (isSuccess) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    SharedPrefs.setIsSubsidyRequestPending(MainActivity.this, true);
                    subsidyBadge.setBackgroundResource(R.drawable.bg_badge_pen);
                    subsidyBadge.setText("Your Request is Pending");
                    dialog.dismiss();
                } else {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {
                loader_controller("Requesting...", true, loader, loader_text);
                Toast.makeText(MainActivity.this, Constant.API_FAILURE, Toast.LENGTH_SHORT).show();

            }
        });

    }


    private void showUserDetailsDialog() {

        if (isFinishing() || isDestroyed()) return;


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

        if (!isFinishing() && !isDestroyed()) {
            alertDialog.show();
        }



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

    @SuppressLint("SetTextI18n")
    private void showAppDialog(String url, String desc, String versionCode_Name) {
        // Holder for the downloaded file
        AtomicReference<File> apkFileRef = new AtomicReference<>();

        RelativeLayout root = findViewById(R.id.update_layout_dialog);
        View view = LayoutInflater.from(this).inflate(R.layout.app_update_layout, root);
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
            btnText.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            progressTxt.setVisibility(View.VISIBLE);

            DownloadService.with(this).downloadFromUrl(url).onProgress(percent -> {
                // update progress
                progressBar.setProgress(percent);
                progressTxt.setText(percent + "%");
            }).onDownloadCompleted(file -> {


                Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file);

                Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                // store file
                apkFileRef.set(file);

                progressBar.setVisibility(View.GONE);
                progressTxt.setTextColor(Color.parseColor("#198754"));
                progressTxt.setText("INSTALL");

                // make it clickable
                progressTxt.setOnClickListener(installClick -> {
                    File apk = apkFileRef.get();
                    if (apk != null && apk.exists()) {

                        Intent intent2 = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent2);
                    }
                });
            }).onError(ex -> runOnUiThread(() -> {
                Log.e(TAG, "Download error", ex);
                Toast.makeText(this, "Download failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            })).start();
        });
    }

    private void userFind(String userSearchTerm, LinearLayout loader, TextView loader_text, View view, AlertDialog dialog) {
        loader_controller("Getting User Data ...", true, loader, loader_text);
        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        SearchQuery query = new SearchQuery(userSearchTerm, "");
        search_consumer receiver = new search_consumer("getUserDetails", query);
        Call<DAC_Collector_Base> auth = requestService.search_customer(receiver);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {
                loader_controller("Fetching Customer ...", false, loader, loader_text);

                DAC_Collector_Base body = response.body();

                if (body == null) {
                    Toast.makeText(MainActivity.this, "Try again !", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean isSuccess = Boolean.TRUE.equals(body.getSuccess());
                if (!isSuccess) {
                    String message = body.getMessage() != null ? body.getMessage() : "Operation failed";
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    return;
                }
                String encResponse = response.body().getData();

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
                btn_skip.setOnClickListener(v -> isSkippingFCM = true);

                saveData.setOnClickListener(v -> {

                    if (userName.equals("not_found")) {
                        loader_controller("Getting User Data ...", false, loader, loader_text);
                        Toast.makeText(MainActivity.this, "user name is empty, Refetch!!", Toast.LENGTH_SHORT).show();
                        return;

                    }
                    new Handler().postDelayed(() -> { // Wait for token retrieval (but update_dac_collect also ensures token)
                        Utility.updateProfile(encResponse, getApplicationContext());

                        SharedPrefs.setConsumerId(MainActivity.this, Cons_ID);
                        SharedPrefs.setUsername(MainActivity.this, userName);
                        update_dac_collect(Cons_ID, userName, dialog, loader, loader_text);
                        loadProfileTV();
                    }, 10); // Adjust delay as needed
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


    // Moved outside the onCreate method

    private void refreshUser(String userSearchTerm) {
        loader_controller("Refreshing Profile...", true, loader, loader_text);
        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        SearchQuery query = new SearchQuery(userSearchTerm, "");
        search_consumer receiver = new search_consumer("getUserDetails", query);
        Call<DAC_Collector_Base> auth = requestService.search_customer(receiver);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {
                DAC_Collector_Base body = response.body();

                if (body == null) {
                    return;
                }

                boolean isSuccess = Boolean.TRUE.equals(body.getSuccess());



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
        if (!SharedPrefs.isFirstTime(MainActivity.this)) return;
        String[] topics = {"heart_beat", "sendCustomMessage", "RechargeRelated"}; // Example topics
        for (String topic : topics) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener(task -> {
                String msg = topic + " Subscribed";
                if (!task.isSuccessful()) {
                    msg = topic + " Subscribe failed";
                }
                Log.d(TAG, msg);
            });
        }
    }


    // Applied from Version Code 497
    private void subscribeToConsID(String consumer_no) {
        if (!SharedPrefs.isFirstTime(MainActivity.this)) return;
        FirebaseMessaging.getInstance().subscribeToTopic("user_" + consumer_no).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(this, "Failed to subscribe private topic", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void checkAppUpdate() {

//        loader_controller("Checking Update....", true, loader, loader_text);
        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        check_update appUpdate_payload = new check_update("checkAppUpdate");
        Call<DAC_Collector_Base> auth = requestService.check_update(appUpdate_payload);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {
                loader_controller("", false, loader, loader_text);

                if (response.body() == null) return;
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
                    } else {
                        Toast.makeText(MainActivity.this, "Already Latest Version.", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileTV();
        checkMissingPermissions();
    }

    private String isServerError(String input) {
        Pattern pattern = Pattern.compile(
                "<div[^>]*text-align:center[^>]*>([^<]+)</div>",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String errorMessage = matcher.group(1).trim();
            return errorMessage;
        }

        return null;
    }


    private void showHtmlDialog(String title, String htmlContent) {
        if (!isFinishing() && !isDestroyed()) {
            runOnUiThread(() -> {
               new AlertDialog.Builder(this).setTitle(title).setMessage(htmlContent).setPositiveButton("OK", null).show();
            });
        }
    }


    @Override
    public void onResponse(String url, String body) {


        runOnUiThread(() -> {
            String errorMsg = isServerError(body);
            if (errorMsg != null) {
                showHtmlDialog("Error", errorMsg);
            }
        });


    }

    private interface TokenCallback {
        void onToken(String token);
    }


}
