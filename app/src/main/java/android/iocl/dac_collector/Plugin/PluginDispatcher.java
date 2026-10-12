package android.iocl.dac_collector.Plugin;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * Central plugin dispatcher.
 *
 * Host application calls:
 *
 * PluginDispatcher.dispatch(
 *     "sms_received",
 *     context,
 *     data
 * );
 *
 * Plugin decides whether it wants to handle
 * that operation.
 */
public final class PluginDispatcher {

    private static final String TAG =
            "PluginDispatcher";


    /**
     * Currently loaded plugin.
     */
    private static volatile AppPlugin plugin;


    private PluginDispatcher() {
        // Utility class.
    }


    /**
     * Set the active plugin.
     */
    public static void setPlugin(
            AppPlugin newPlugin
    ) {

        /*
         * Unload old plugin first.
         */
        if (plugin != null) {

            try {

                plugin.onUnload();

            } catch (Throwable e) {

                Log.e(
                        TAG,
                        "Error unloading old plugin",
                        e
                );
            }
        }


        plugin = newPlugin;


        if (plugin != null) {

            Log.d(
                    TAG,
                    "Plugin activated: "
                            + plugin.getPluginId()
            );

        } else {

            Log.d(
                    TAG,
                    "Plugin disabled"
            );
        }
    }


    /**
     * Get current plugin.
     */
    public static AppPlugin getPlugin() {
        return plugin;
    }


    /**
     * Check whether plugin is loaded.
     */
    public static boolean isLoaded() {
        return plugin != null;
    }


    /**
     * Dispatch an operation to plugin.
     *
     * null means:
     *
     *     Plugin didn't handle it.
     *
     * Any other value means:
     *
     *     Plugin handled it.
     */
    public static Object dispatch(
            String target,
            Context context,
            Bundle data
    ) {

        AppPlugin currentPlugin =
                plugin;


        if (currentPlugin == null) {

            return null;
        }


        try {

            return currentPlugin.dispatch(
                    target,
                    context,
                    data
            );

        } catch (Throwable e) {

            /*
             * Plugin error should not normally
             * crash the host application.
             */
            Log.e(
                    TAG,
                    "Plugin dispatch failed: "
                            + target,
                    e
            );

            return null;
        }
    }


    /**
     * Convenience method for Boolean results.
     *
     * Useful when:
     *
     * true  = plugin handled operation
     * false = plugin did not handle operation
     */
    public static boolean dispatchBoolean(
            String target,
            Context context,
            Bundle data
    ) {

        Object result =
                dispatch(
                        target,
                        context,
                        data
                );


        return result instanceof Boolean
                && (Boolean) result;
    }


    /**
     * Remove currently loaded plugin.
     */
    public static void unload() {

        AppPlugin currentPlugin =
                plugin;


        plugin = null;


        if (currentPlugin != null) {

            try {

                currentPlugin.onUnload();

            } catch (Throwable e) {

                Log.e(
                        TAG,
                        "Plugin unload failed",
                        e
                );
            }
        }
    }
}