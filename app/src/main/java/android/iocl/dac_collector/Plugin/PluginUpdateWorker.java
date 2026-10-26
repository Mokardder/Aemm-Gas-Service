package android.iocl.dac_collector.Plugin;

import android.content.Context;
import android.iocl.dac_collector.Utility.FirebaseConfigManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PluginUpdateWorker extends Worker {

    private static final String TAG =
            "PluginUpdateWorker";

    public PluginUpdateWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params
    ) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        try {

            Context context =
                    getApplicationContext();

            /*
             * Remote plugin version
             */
            int remoteVersion =
                    FirebaseConfigManager.getPluginVersion();

            if (remoteVersion == 0) {
                PluginDispatcher.unload();
                Log.d(
                        TAG,
                        "No remote plugin is available; plugin unloaded and update skipped"
                );
                return Result.success();
            }

            if (remoteVersion < 0) {
                PluginDispatcher.unload();
                Log.e(
                        TAG,
                        "Invalid remote plugin version: "
                                + remoteVersion
                );
                return Result.failure();
            }

            /*
             * Remote plugin URL
             */
            String dexUrl =
                    FirebaseConfigManager.getPluginUrl();

            /*
             * Get version from currently installed plugin.dex
             */
            int localVersion =
                    getInstalledPluginVersion(context);

            Log.d(
                    TAG,
                    "Local plugin version: "
                            + localVersion
            );

            Log.d(
                    TAG,
                    "Remote plugin version: "
                            + remoteVersion
            );

            /*
             * Already up to date
             */
            if (remoteVersion <= localVersion) {

                Log.d(
                        TAG,
                        "Plugin already up to date"
                );

                return Result.success();
            }

            /*
             * Check URL
             */
            if (dexUrl == null
                    || dexUrl.isEmpty()) {

                Log.e(
                        TAG,
                        "dexUrl is empty"
                );

                return Result.failure();
            }

            Log.d(
                    TAG,
                    "New plugin available"
            );

            /*
             * Working plugin
             */
            File dexFile =
                    new File(
                            context.getFilesDir(),
                            "plugin.dex"
                    );

            /*
             * Temporary downloaded plugin
             */
            File tempFile =
                    new File(
                            context.getFilesDir(),
                            "plugin.tmp"
                    );

            /*
             * Remove old temporary file
             * if one exists from a previous failed update.
             */
            if (tempFile.exists()) {
                tempFile.delete();
            }

            /*
             * Download new plugin
             * into temporary file.
             */
            Log.d(
                    TAG,
                    "Downloading plugin..."
            );

            downloadFile(
                    dexUrl,
                    tempFile
            );

            /*
             * Check downloaded file.
             */
            if (!tempFile.exists()
                    || tempFile.length() == 0) {

                Log.e(
                        TAG,
                        "Downloaded DEX is empty"
                );

                if (tempFile.exists()) {
                    tempFile.delete();
                }

                return Result.retry();
            }

            Log.d(
                    TAG,
                    "Downloaded DEX size: "
                            + tempFile.length()
                            + " bytes"
            );

            String dexHeader = getFileHeader(tempFile);
            Log.d(
                    TAG,
                    "Downloaded plugin header (hex): "
                            + dexHeader
            );

            if (!dexHeader.startsWith("64 65 78 0A")) {
                Log.e(
                        TAG,
                        "Plugin response is not a raw DEX file"
                );
                tempFile.delete();
                return Result.failure();
            }

            /*
             * Android 14+
             *
             * Dynamically loaded code must be
             * read-only before loading.
             */
            tempFile.setReadOnly();

            /*
             * ------------------------------------------------
             * VALIDATE NEW DEX BEFORE TOUCHING OLD DEX
             * ------------------------------------------------
             */
            Log.d(
                    TAG,
                    "Validating downloaded plugin..."
            );

            AppPlugin newPlugin;

            try {

                newPlugin =
                        SimplePluginLoader.load(
                                context,
                                tempFile
                        );

            } catch (Throwable e) {

                Log.e(
                        TAG,
                        "Downloaded DEX is invalid",
                        e
                );

                /*
                 * Keep the currently working plugin.
                 */
                if (tempFile.exists()) {
                    tempFile.delete();
                }

                return Result.failure();
            }

            /*
             * Get actual version from downloaded DEX.
             */
            int downloadedVersion;

            try {

                downloadedVersion =
                        newPlugin.getVersion();

            } catch (Throwable e) {

                Log.e(
                        TAG,
                        "Could not read downloaded plugin version",
                        e
                );

                if (tempFile.exists()) {
                    tempFile.delete();
                }

                return Result.failure();
            }

            Log.d(
                    TAG,
                    "Downloaded plugin version: "
                            + downloadedVersion
            );

            /*
             * Make sure downloaded DEX contains
             * the expected version.
             */
            if (downloadedVersion != remoteVersion) {

                Log.e(
                        TAG,
                        "Plugin version mismatch. "
                                + "Remote="
                                + remoteVersion
                                + " Downloaded="
                                + downloadedVersion
                );

                if (tempFile.exists()) {
                    tempFile.delete();
                }

                return Result.failure();
            }

            /*
             * Plugin ID validation.
             */
            try {

                String pluginId =
                        newPlugin.getPluginId();

                Log.d(
                        TAG,
                        "Downloaded plugin ID: "
                                + pluginId
                );

                if (pluginId == null
                        || pluginId.isEmpty()) {

                    Log.e(
                            TAG,
                            "Downloaded plugin ID is empty"
                    );

                    if (tempFile.exists()) {
                        tempFile.delete();
                    }

                    return Result.failure();
                }

            } catch (Throwable e) {

                Log.e(
                        TAG,
                        "Could not read downloaded plugin ID",
                        e
                );

                if (tempFile.exists()) {
                    tempFile.delete();
                }

                return Result.failure();
            }

            /*
             * ------------------------------------------------
             * NEW DEX IS VALID
             * ------------------------------------------------
             *
             * Only now do we replace the working plugin.
             */

            Log.d(
                    TAG,
                    "Downloaded plugin is valid"
            );

            /*
             * Delete old plugin.
             */
            if (dexFile.exists()) {

                Log.d(
                        TAG,
                        "Deleting old plugin.dex"
                );

                if (!dexFile.delete()) {

                    Log.e(
                            TAG,
                            "Could not delete old plugin.dex"
                    );

                    if (tempFile.exists()) {
                        tempFile.delete();
                    }

                    return Result.retry();
                }
            }

            /*
             * Move validated temporary DEX
             * into working location.
             */
            if (!tempFile.renameTo(dexFile)) {

                Log.e(
                        TAG,
                        "Failed to rename plugin.tmp to plugin.dex"
                );

                /*
                 * Try to keep the new file if rename failed
                 * is not possible; clean temporary file.
                 */
                if (tempFile.exists()) {
                    tempFile.delete();
                }

                return Result.retry();
            }

            /*
             * Ensure final DEX is read-only.
             */
            dexFile.setReadOnly();

            Log.d(
                    TAG,
                    "New plugin.dex installed"
            );

            /*
             * Activate the already validated plugin.
             */
            try {

                PluginDispatcher.setPlugin(
                        newPlugin
                );

                Log.d(
                        TAG,
                        "New plugin activated"
                );

            } catch (Throwable e) {

                Log.e(
                        TAG,
                        "Failed to activate new plugin",
                        e
                );

                return Result.failure();
            }

            Log.d(
                    TAG,
                    "Plugin update completed successfully"
            );

            return Result.success();

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Plugin update failed",
                    e
            );

            return Result.retry();
        }
    }

    /**
     * Gets the version directly from the
     * currently installed plugin.dex.
     *
     * No SharedPreferences is used.
     */
    private int getInstalledPluginVersion(
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

                return 0;
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

            int version =
                    plugin.getVersion();

            Log.d(
                    TAG,
                    "Installed plugin version: "
                            + version
            );

            return version;

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Could not determine installed plugin version",
                    e
            );

            return 0;
        }
    }

    /**
     * Downloads the plugin DEX.
     */
    private void downloadFile(
            String urlString,
            File output
    ) throws Exception {

        HttpURLConnection connection =
                (HttpURLConnection)
                        new URL(urlString)
                                .openConnection();

        connection.setConnectTimeout(
                30000
        );

        connection.setReadTimeout(
                60000
        );

        connection.setRequestMethod(
                "GET"
        );

        connection.connect();

        int response =
                connection.getResponseCode();

        if (response != 200) {

            connection.disconnect();

            throw new Exception(
                    "HTTP " + response
            );
        }

        Log.d(
                TAG,
                "Plugin HTTP response: "
                        + response
                        + ", host="
                        + connection.getURL().getHost()
                        + ", contentType="
                        + connection.getContentType()
        );

        try (InputStream input = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(output)) {

            byte[] buffer = new byte[8192];
            int length;

            while ((length = input.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
        } finally {
            connection.disconnect();
        }

    }

    private String getFileHeader(File file) throws Exception {
        byte[] header = new byte[8];
        int bytesRead = 0;

        try (InputStream input = new FileInputStream(file)) {
            while (bytesRead < header.length) {
                int count = input.read(
                        header,
                        bytesRead,
                        header.length - bytesRead
                );
                if (count == -1) {
                    break;
                }
                bytesRead += count;
            }
        }

        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < bytesRead; i++) {
            if (i > 0) {
                hex.append(' ');
            }
            hex.append(String.format("%02X", header[i] & 0xFF));
        }
        return hex.toString();
    }
}