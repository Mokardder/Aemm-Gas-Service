package android.iocl.dac_collector.Interface;

import java.io.File;

public interface DownloadCallback {
    /** Called periodically with the percent complete (0–100). */
    void onProgress(int percent);

    /** Called when download finishes successfully, with the saved APK file. */
    void onSuccess(File apkFile);

    /** Called if the download fails for any reason. */
    void onError(Exception e);
}
