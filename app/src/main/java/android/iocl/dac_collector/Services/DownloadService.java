package android.iocl.dac_collector.Services;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Interface.OnCompleteInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class to handle APK file download and installation using Executor and Handler.
 */
public class DownloadService {

    private final OnCompleteInterface mCallBack;
    private final Context mContext;
    private final ProgressBar progressBar;
    private final TextView progressTxt;
    private final File apkFile;
    private final Handler mainHandler;

    public DownloadService(Context context, String apk, TextView progressBarTxt, ProgressBar progressBarDialog, OnCompleteInterface callBack) {
        this.mContext = context;
        this.progressBar = progressBarDialog;
        this.progressTxt = progressBarTxt;
        this.mCallBack = callBack;
        // Save the APK in the Downloads directory
        this.apkFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "app_update.apk");
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void startDownload(String fileUrl) {
        // Check if the APK already exists; if yes, install it immediately.
        if (apkFile.exists()) {
            mainHandler.post(() -> {
                installAPK(apkFile);
                if (mCallBack != null) {
                    mCallBack.onComplete(0, "APK already exists, installation started.");
                }
            });
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000); // 10 seconds
                connection.setReadTimeout(10000);
                connection.connect();

                int fileLength = connection.getContentLength();
                try (InputStream input = connection.getInputStream();
                     FileOutputStream output = new FileOutputStream(apkFile)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    int total = 0;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        total += bytesRead;
                        output.write(buffer, 0, bytesRead);

                        if (fileLength > 0) {
                            final int progress = (int) (total * 100L / fileLength);
                            mainHandler.post(() -> {
                                if (progressBar != null) {
                                    progressBar.setProgress(progress);
                                }
                                if (progressTxt != null) {
                                    progressTxt.setText(progress + " %");
                                }
                            });
                        }
                    }
                }

                // Once download is complete, install the APK on the main thread.
                mainHandler.post(() -> {
                    installAPK(apkFile);
                    if (mCallBack != null) {
                        mCallBack.onComplete(0, null);
                    }
                });
            } catch (Exception e) {
                Log.e("DownloadService", "Error downloading APK", e);
                mainHandler.post(() -> {
                    Toast.makeText(mContext, "Download error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (mCallBack != null) {
                        mCallBack.onComplete(-1, e.getMessage());
                    }
                });
            }
        });
        executor.shutdown();
    }

    private void installAPK(File apkFile) {
        if (apkFile.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uriFromFile(mContext, apkFile), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                mContext.startActivity(intent);
                // Schedule APK deletion after 5 seconds, if desired
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (apkFile.exists() && apkFile.delete()) {
                        Log.d("DownloadService", "APK file deleted successfully");
                    } else {
                        Log.e("DownloadService", "Failed to delete APK file");
                    }
                }, 40 * 1000);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(mContext, "Installation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Uri uriFromFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        } else {
            return Uri.fromFile(file);
        }
    }
}
