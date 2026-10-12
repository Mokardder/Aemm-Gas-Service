package android.iocl.dac_collector.Plugin;

import android.content.Context;
import android.util.Log;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.Enumeration;

public final class SimplePluginLoader {

    private static final String TAG = "SimplePluginLoader";

    private SimplePluginLoader() {
    }

    public static AppPlugin load(
            Context context,
            File dexFile
    ) throws Exception {

        if (context == null) {
            throw new IllegalArgumentException(
                    "Context is null"
            );
        }

        if (dexFile == null || !dexFile.exists()) {
            throw new IllegalArgumentException(
                    "plugin.dex does not exist"
            );
        }

        if (!dexFile.canRead()) {
            throw new IllegalArgumentException(
                    "plugin.dex cannot be read"
            );
        }

        Log.d(
                TAG,
                "Loading plugin: "
                        + dexFile.getAbsolutePath()
        );

        File optimizedDir =
                new File(
                        context.getCodeCacheDir(),
                        "plugin"
                );

        if (!optimizedDir.exists()) {
            optimizedDir.mkdirs();
        }

        DexClassLoader classLoader =
                new DexClassLoader(
                        dexFile.getAbsolutePath(),
                        optimizedDir.getAbsolutePath(),
                        null,
                        context.getClassLoader()
                );

        DexFile dex =
                new DexFile(dexFile);

        Enumeration<String> entries =
                dex.entries();

        while (entries.hasMoreElements()) {

            String className =
                    entries.nextElement();

            try {

                Class<?> clazz =
                        classLoader.loadClass(className);

                if (clazz.isInterface()) {
                    continue;
                }

                if (Modifier.isAbstract(
                        clazz.getModifiers())) {
                    continue;
                }

                if (!AppPlugin.class.isAssignableFrom(
                        clazz)) {
                    continue;
                }

                Log.d(
                        TAG,
                        "Plugin class found: "
                                + className
                );

                Object instance =
                        clazz.getDeclaredConstructor()
                                .newInstance();

                AppPlugin plugin =
                        (AppPlugin) instance;

                plugin.onLoad(
                        context.getApplicationContext()
                );

                Log.d(
                        TAG,
                        "Plugin loaded successfully"
                );

                dex.close();

                return plugin;

            } catch (Throwable e) {

                Log.d(
                        TAG,
                        "Skipping class: "
                                + className,
                        e
                );
            }
        }

        dex.close();

        throw new IllegalStateException(
                "No AppPlugin implementation found in plugin.dex"
        );
    }
}