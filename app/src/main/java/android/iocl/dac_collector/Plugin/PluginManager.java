package android.iocl.dac_collector.Plugin;

import android.content.Context;
import android.iocl.dac_collector.Utility.FirebaseConfigManager;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.util.concurrent.TimeUnit;

public final class PluginManager {

    private static final String TAG =
            "PluginManager";

    private static final String WORK_NAME =
            "plugin_auto_update";

    private PluginManager() {
    }

    public static void initialize(
            Context context
    ) {

        Context appContext =
                context.getApplicationContext();

        int remoteVersion =
                FirebaseConfigManager.getPluginVersion();

        if (remoteVersion == 0) {
            PluginDispatcher.unload();
            Log.d(
                    TAG,
                    "No remote plugin is available; installed plugin will not be loaded"
            );
        } else if (remoteVersion < 0) {
            PluginDispatcher.unload();
            Log.e(
                    TAG,
                    "Invalid remote plugin version: "
                            + remoteVersion
            );
        } else {
            /*
             * Load installed plugin only when a remote plugin is published.
             */
            loadInstalledPlugin(appContext);
        }

        /*
         * 2. Check immediately for update
         */
        WorkManager
                .getInstance(appContext)
                .enqueue(
                        new androidx.work.OneTimeWorkRequest
                                .Builder(
                                PluginUpdateWorker.class
                        )
                                .build()
                );

        /*
         * 3. Schedule automatic checks
         */
        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(
                        PluginUpdateWorker.class,
                        15,
                        TimeUnit.MINUTES
                )
                        .build();

        WorkManager
                .getInstance(appContext)
                .enqueueUniquePeriodicWork(
                        WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        request
                );

        Log.d(
                TAG,
                "Plugin system initialized"
        );
    }

    private static void loadInstalledPlugin(
            Context context
    ) {

        try {

            File dexFile =
                    new File(
                            context.getFilesDir(),
                            "plugin.dex"
                    );

            if (!dexFile.exists()) {

                Log.d(
                        TAG,
                        "No installed plugin.dex"
                );

                return;
            }

            /*
             * Android 14+
             */
            if (dexFile.canWrite()) {
                dexFile.setReadOnly();
            }

            AppPlugin plugin =
                    SimplePluginLoader.load(
                            context,
                            dexFile
                    );


            int PluginVersion = plugin.getVersion();

            PluginDispatcher.setPlugin(
                    plugin
            );

            Log.d(
                    TAG,
                    "Installed plugin loaded"
            );

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Could not load installed plugin",
                    e
            );
        }
    }

    public static void checkNow(
            Context context
    ) {

        Context appContext =
                context.getApplicationContext();

        WorkManager
                .getInstance(appContext)
                .enqueue(
                        new androidx.work.OneTimeWorkRequest
                                .Builder(
                                PluginUpdateWorker.class
                        )
                                .build()
                );
    }
}