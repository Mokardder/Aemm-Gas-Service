package android.iocl.dac_collector.Plugin;

import android.content.Context;
import android.os.Bundle;

/**
 * Base interface that every dynamic plugin must implement.
 *
 * The interface itself is inside the main APK.
 *
 * plugin.dex only needs to implement this interface.
 */
public interface AppPlugin {

    /**
     * Plugin ID.
     */
    String getPluginId();



    int getVersion();

    /**
     * Called when plugin is loaded.
     */
    void onLoad(Context context);

    /**
     * Called before plugin is unloaded/replaced.
     */
    void onUnload();

    /**
     * Called by PluginDispatcher.
     *
     * target:
     *     Identifies what the plugin should handle.
     *
     * context:
     *     Android Context.
     *
     * data:
     *     Data passed from the host application.
     *
     * return:
     *     Any object.
     *
     *     null = plugin did not handle it.
     */
    Object dispatch(
            String target,
            Context context,
            Bundle data
    );
}