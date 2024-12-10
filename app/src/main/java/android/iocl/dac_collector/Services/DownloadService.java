package android.iocl.dac_collector.Services;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Interface.OnCompleteInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
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
    private static ProgressDialog mProgressDialog = null;
    private final OnCompleteInterface mCallBack;
    private Context mContext;
    private File apkFile;

    public DownloadService(Context context, String apk, OnCompleteInterface callBack) {
        mContext = context;
        mCallBack = callBack;

        if (mProgressDialog != null) {
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
            return e.getLocalizedMessage();
        }
        return null;
    }

    protected void onPreExecute() {
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage("Updating...");
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setMax(100);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.show();
    }

    protected void onProgressUpdate(Integer... progress) {
        mProgressDialog.setProgress(progress[0]);
    }

    protected void onPostExecute(String result) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        if (result == null && apkFile != null && apkFile.exists()) {
            installApk(apkFile);
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

    private void installApk( File apk) {



        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(Objects.requireNonNull(mContext),
                    mContext.getPackageName() + ".provider", apk);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(apk);
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        mContext.startActivity(intent);
    }

    public void dismiss() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }
}
