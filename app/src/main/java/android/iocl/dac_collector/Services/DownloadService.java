package android.iocl.dac_collector.Services;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.AsyncTask;

import androidx.annotation.RequiresApi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

public class DownloadService {

    private final Context context;
    private String apkUrl;
    private String githubPAT;
    private Consumer<Integer> progressListener;
    private Consumer<File> successListener;
    private Consumer<Exception> errorListener;

    private DownloadService(Context context) {
        this.context = context.getApplicationContext();
    }

    public static DownloadService with(Context context) {
        return new DownloadService(context);
    }

    public DownloadService downloadFromUrl(String url) {
        this.apkUrl = url;
        return this;
    }

    public DownloadService withGitHubPAT(String pat) {
        this.githubPAT = pat;
        return this;
    }

    public DownloadService onProgress(Consumer<Integer> listener) {
        this.progressListener = listener;
        return this;
    }

    public DownloadService onDownloadCompleted(Consumer<File> listener) {
        this.successListener = listener;
        return this;
    }

    public DownloadService onError(Consumer<Exception> listener) {
        this.errorListener = listener;
        return this;
    }

    public void start() {
        new DownloadTask().execute(apkUrl);
    }

    private class DownloadTask extends AsyncTask<String, Integer, File> {

        private Exception error;

        @Override
        protected File doInBackground(String... params) {

            HttpURLConnection conn = null;
            InputStream in = null;
            FileOutputStream out = null;

            try {
                URL url = new URL(params[0]);

                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);

                // ✅ CRITICAL HEADERS (for private GitHub asset API)
                if (githubPAT != null && !githubPAT.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + githubPAT);
                }

                conn.setRequestProperty("Accept", "application/octet-stream");
                conn.setRequestProperty("User-Agent", "Android");

                conn.connect();

                int responseCode = conn.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Download failed. HTTP Code: " + responseCode);
                }

                int fileLength = conn.getContentLength();

                in = new BufferedInputStream(conn.getInputStream());

                File apkFile;

                try {
                    apkFile = new File(
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                            "update.apk"
                    );
                } catch (Exception e) {
                    apkFile = new File(
                            context.getExternalFilesDir(null),
                            "update.apk"
                    );
                }

                out = new FileOutputStream(apkFile);

                byte[] buffer = new byte[8192];
                long total = 0;
                int count;

                while ((count = in.read(buffer)) != -1) {

                    if (isCancelled()) return null;

                    total += count;

                    if (fileLength > 0) {
                        int progress = (int) (total * 100 / fileLength);
                        publishProgress(progress);
                    }

                    out.write(buffer, 0, count);
                }

                out.flush();
                return apkFile;

            } catch (Exception e) {
                error = e;
                return null;

            } finally {
                try { if (out != null) out.close(); } catch (Exception ignored) {}
                try { if (in != null) in.close(); } catch (Exception ignored) {}
                if (conn != null) conn.disconnect();
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected void onProgressUpdate(Integer... values) {
            if (progressListener != null) {
                progressListener.accept(values[0]);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected void onPostExecute(File result) {
            if (error != null) {
                if (errorListener != null) errorListener.accept(error);
            } else {
                if (successListener != null) successListener.accept(result);
            }
        }
    }
}