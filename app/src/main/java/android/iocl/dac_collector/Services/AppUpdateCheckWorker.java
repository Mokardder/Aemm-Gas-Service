package android.iocl.dac_collector.Services;

import android.content.Context;
import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.ModelData.GitHubRelease;
import android.iocl.dac_collector.RetrofitClient.GitHubService;
import android.iocl.dac_collector.Utility.FirebaseConfigManager;
import android.iocl.dac_collector.Utility.NotificationHelper;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AppUpdateCheckWorker extends Worker {
    private static final String TAG = "AppUpdateCheckWorker";
    private static final String OWNER = "Mokardder";
    private static final String REPOSITORY = "Aemm-Gas-Service";
    private static final String PERIODIC_WORK_NAME = "background_app_update_check";
    private static final String IMMEDIATE_WORK_NAME = "immediate_app_update_check";

    public AppUpdateCheckWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
    }

    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest periodicRequest = new PeriodicWorkRequest.Builder(
                AppUpdateCheckWorker.class,
                24,
                TimeUnit.HOURS
        ).setConstraints(constraints).build();

        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
        workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
        );

        OneTimeWorkRequest immediateRequest = new OneTimeWorkRequest.Builder(
                AppUpdateCheckWorker.class
        ).setConstraints(constraints).build();
        workManager.enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                immediateRequest
        );
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String pat = FirebaseConfigManager.getGithubPAT();
            GitHubService service = new Retrofit.Builder()
                    .baseUrl("https://api.github.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(GitHubService.class);

            Response<GitHubRelease> response = service
                    .getLatestRelease(
                            OWNER,
                            REPOSITORY,
                            pat == null || pat.trim().isEmpty() ? null : "token " + pat
                    )
                    .execute();

            if (!response.isSuccessful() || response.body() == null) {
                Log.w(TAG, "GitHub release check failed with HTTP " + response.code());
                return response.code() >= 400 && response.code() < 500
                        ? Result.failure()
                        : Result.retry();
            }

            GitHubRelease release = response.body();
            int latestVersion = parseVersionCode(release.getTag_name());
            if (latestVersion <= BuildConfig.VERSION_CODE) {
                SharedPrefs.clearAppUpdateInfo();
                SharedPrefs.clearDownloadedUpdateApk();
                return Result.success();
            }

            String assetUrl = findApkAssetUrl(release);
            if (assetUrl == null) {
                Log.w(TAG, "Latest release has no APK asset");
                return Result.failure();
            }

            SharedPrefs.saveAppUpdateInfo(release, assetUrl, latestVersion);
            String cachedApkPath = SharedPrefs.getDownloadedUpdateApkPath(latestVersion);
            if (cachedApkPath != null) {
                File cachedApk = new File(cachedApkPath);
                if (cachedApk.isFile() && cachedApk.length() > 0) {
                    return Result.success();
                }
            }

            File apk = downloadApk(assetUrl, pat, latestVersion);
            SharedPrefs.saveDownloadedUpdateApk(latestVersion, apk.getAbsolutePath());
            NotificationHelper.showUpdateAvailable(
                    getApplicationContext(),
                    release.getTag_name()
            );
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Background app update check/download failed", e);
            return Result.retry();
        }
    }

    private static int parseVersionCode(String tag) {
        if (tag == null) {
            throw new IllegalArgumentException("GitHub release tag is missing");
        }
        String[] parts = tag.split("-");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Unexpected GitHub release tag: " + tag);
        }
        return Integer.parseInt(parts[parts.length - 1]);
    }

    private static String findApkAssetUrl(GitHubRelease release) {
        if (release.getAssets() == null) {
            return null;
        }
        for (GitHubRelease.Asset asset : release.getAssets()) {
            if (asset.getName() != null
                    && asset.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".apk")
                    && asset.getId() > 0) {
                return "https://api.github.com/repos/" + OWNER + "/" + REPOSITORY
                        + "/releases/assets/" + asset.getId();
            }
        }
        return null;
    }

    private File downloadApk(String apkUrl, String pat, int versionCode) throws Exception {
        File downloadDirectory = getApplicationContext().getExternalFilesDir(
                Environment.DIRECTORY_DOWNLOADS
        );
        if (downloadDirectory == null) {
            throw new IllegalStateException("App download directory is unavailable");
        }
        if (!downloadDirectory.exists() && !downloadDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create app update directory");
        }

        File apkFile = new File(downloadDirectory, "update-" + versionCode + ".apk");
        File tempFile = new File(downloadDirectory, "update-" + versionCode + ".apk.part");
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) new URL(apkUrl).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Accept", "application/octet-stream");
            connection.setRequestProperty("User-Agent", "Android");
            if (pat != null && !pat.trim().isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + pat);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("APK download failed with HTTP " + responseCode);
            }

            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream output = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (isStopped()) {
                        throw new InterruptedException("APK download was stopped");
                    }
                    output.write(buffer, 0, count);
                }
                output.getFD().sync();
            }

            if (tempFile.length() == 0) {
                throw new IllegalStateException("Downloaded APK is empty");
            }
            if (apkFile.exists() && !apkFile.delete()) {
                throw new IllegalStateException("Could not replace old APK download");
            }
            if (!tempFile.renameTo(apkFile)) {
                throw new IllegalStateException("Could not finalize APK download");
            }
            return apkFile;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
