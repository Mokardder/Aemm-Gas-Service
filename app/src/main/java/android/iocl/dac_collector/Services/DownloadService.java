package android.iocl.dac_collector.Services;


import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Interface.OnCompleteInterface;
import android.iocl.dac_collector.Utility.Utility;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

/**
 * Class to handle APK file download and installation.
 */
public class DownloadService extends AsyncTask<String, Integer, String> {

    private final OnCompleteInterface mCallBack;
    private Context mContext;
    private File apkFile;
    ProgressBar progressBar;
    TextView progressTxt;

    public DownloadService(Context context, String apk,TextView progressBarTxt, ProgressBar progressBarDialog, OnCompleteInterface callBack) {
        mContext = context;
        this.progressBar = progressBarDialog;
        this.progressTxt = progressBarTxt;
        mCallBack = callBack;

        if (progressBarDialog != null) {
            onPreExecute();
        }
    }

    @Override
    protected String doInBackground(String... sUrl) {
        try {
            URLConnection connection = new URL(sUrl[0]).openConnection();
            connection.connect();
            int fileLength = connection.getContentLength();

            InputStream input = connection.getInputStream();
            byte[] buffer = new byte[4096];
            int n, total = 0;

            // Save the file in the Downloads directory
            apkFile = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "app_update.apk");
            FileOutputStream output = new FileOutputStream(apkFile);

            while ((n = input.read(buffer)) != -1) {
                publishProgress((int) ((total += n) * 100 / fileLength));
                output.write(buffer, 0, n);
            }
            output.close();

        } catch (Exception e) {

            boolean error = e.toString().contains("FileNotFoundException");
            Toast.makeText(mContext, "FileNotFoundException", Toast.LENGTH_SHORT).show();
            Log.d("DownloadServie", "doInBackground: " + e);
        }
        return null;
    }

    protected void onPreExecute() {

    }

    protected void onProgressUpdate(Integer... progress) {
        progressBar.setProgress(progress[0]);
        progressTxt.setText( progress[0] + " %");
    }

    protected void onPostExecute(String result) {
        if (progressBar != null) {
            progressBar.setMax(100);
        }

        if (result == null && apkFile != null && apkFile.exists()) {
            installAPK(apkFile);
        }

        if (mCallBack != null) {
            mCallBack.onComplete(0, result);
        }
    }

//    private void installApk(File apkFile) {
//        Uri apkUri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".provider", apkFile);
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        mContext.startActivity(intent);
//    }

    void installAPK(File apkUri){

        if(apkUri.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uriFromFile(mContext, apkUri), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                mContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();

            }
        }else{

        }
    }
    Uri uriFromFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, "android.iocl.dac_collector" + ".provider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    public void dismiss() {
        if (progressBar != null) {

        }
    }
}
