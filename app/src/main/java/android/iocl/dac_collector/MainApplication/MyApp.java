package android.iocl.dac_collector.MainApplication;

import android.app.Application;
import android.content.Intent;
import android.iocl.dac_collector.AntiCrashLibCockroach.Cockroach;
import android.iocl.dac_collector.AntiCrashLibCockroach.ExceptionHandler;
import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class MyApp extends Application {
    FirebaseCrashlytics crashlytics;
    FirebaseApp firebaseApp;
    @Override
    public void onCreate() {
        super.onCreate();

       firebaseApp =   FirebaseApp.initializeApp(this);

        SharedPrefs prefs = new SharedPrefs(getApplicationContext());
        String cons_id = prefs.getString("cons_id", "defaultForCrash");

         crashlytics  = FirebaseCrashlytics.getInstance();


         crashlytics.setUserId(cons_id);
        // Optionally enable debug logging
        crashlytics.setCrashlyticsCollectionEnabled(true);
        // Initialize Firebase

        install();
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
