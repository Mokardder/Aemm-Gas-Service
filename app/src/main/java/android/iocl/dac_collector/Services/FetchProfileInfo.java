package android.iocl.dac_collector.Services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.pm.PackageManager;
import android.iocl.dac_collector.ModelData.ConsumerData;
import android.iocl.dac_collector.ModelData.DAC_Collector_Base;
import android.iocl.dac_collector.ModelData.SearchQuery;
import android.iocl.dac_collector.ModelData.search_consumer;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.RetrofitClient.RequestService;
import android.iocl.dac_collector.RetrofitClient.RetrofitClient;
import android.iocl.dac_collector.Utility.NotificationHelper;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.Utility;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SpecifyJobSchedulerIdRange")
public class FetchProfileInfo extends JobService {

    private static final String CHANNEL_ID = "5662"; // ✅ keep your original
    private JobParameters mJobParameters;
    private final String TAG = "FetchProfileInfo";

    private static final String PREF_LAST_FETCH = "last_profile_fetch";
    private static final long CACHE_DURATION = 6 * 60 * 60 * 1000L; // 6 hours

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        mJobParameters = jobParameters;

        createNotificationChannel();
        startServiceWithNotification();



        String number = SharedPrefs.getConsumerId();

        if (!number.isEmpty()) {
            // Always run local follow-up work
            Utility.sendAnyUnsentDAC(getApplicationContext());

            Log.d(TAG, "onStartJob: Job Started");

            // API request is rate-limited by shared-preference check (6h)
            if (shouldFetchProfileNow(this)) {

                Log.d(TAG, "onStartJob: shouldFetchProfileNow -> true");
                doJob(number);
            } else {
                finishJob(jobParameters, false);
            }
        } else {
            finishJob(jobParameters, false);
        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }

    public static boolean shouldFetchProfileNow(android.content.Context context) {
        long lastFetch = context.getSharedPreferences("dac_prefs", android.content.Context.MODE_PRIVATE)
                .getLong(PREF_LAST_FETCH, 0);
        return (System.currentTimeMillis() - lastFetch) > CACHE_DURATION;
    }



    private void markFetched() {
        getSharedPreferences("dac_prefs", MODE_PRIVATE)
                .edit()
                .putLong(PREF_LAST_FETCH, System.currentTimeMillis())
                .apply();
    }

    private void doJob(String userSearchTerm) {
        RequestService requestService = RetrofitClient
                .retrofit_spreadsheet(getApplicationContext())
                .create(RequestService.class);

        SearchQuery query = new SearchQuery(userSearchTerm, "");
        search_consumer receiver = new search_consumer("getUserDetails", query);

        requestService.search_customer(receiver).enqueue(new Callback<DAC_Collector_Base>() {

            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {
                try {
                    if (response.body() == null) {
                        return;
                    }

                    if (!Boolean.TRUE.equals(response.body().getSuccess())) {
                        return;
                    }

                    if (response.isSuccessful()) {
                        String encResponse = response.body().getData();


                        Log.d(TAG, "onStartJob: fetched -> true");



                        ConsumerData data = (ConsumerData) Utility.decodeApiResponse(
                                encResponse,
                                ConsumerData.class
                        );

                        if (data != null) {
                            Utility.updateProfile(encResponse, getApplicationContext());
                        }

                        markFetched();
                        Log.d("FetchProfile", "onResponse: profile updated");
                    }
                } finally {
                    finishJob(mJobParameters, false);
                }
            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {
                finishJob(mJobParameters, true);
            }
        });
    }

    // ================= FIXED NOTIFICATION =================

    private void startServiceWithNotification() {
        try {

            // Android 13+ permission check
            if (Build.VERSION.SDK_INT >= 33) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            Notification notification;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                Notification notificationObj = new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("Gas App is active")
                        .setSmallIcon(R.drawable.verify_icon_blue)
                        .setOngoing(true)
                        .setCategory(Notification.CATEGORY_SERVICE)
                        .setVisibility(Notification.VISIBILITY_SECRET)
                        .setPriority(Notification.PRIORITY_LOW) // ✅ FIXED (was MIN)
                        .build();

                notification = notificationObj;

            } else {
                notification = new Notification.Builder(this)
                        .setContentTitle("Gas App")
                        .setContentText("Running quietly...")
                        .setSmallIcon(R.drawable.verify_icon_blue)
                        .setOngoing(true)
                        .setPriority(Notification.PRIORITY_LOW)
                        .build();
            }

            NotificationManager manager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (manager != null) {
                manager.notify(1001, notification);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void finishJob(JobParameters params, boolean needsReschedule) {
        cancelWorkingNotification();
        jobFinished(params, needsReschedule);
    }

    private void cancelWorkingNotification() {
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(1001);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel nc = new NotificationChannel(
                    CHANNEL_ID,
                    "Block this | Fetch Profile", // ✅ unchanged
                    NotificationManager.IMPORTANCE_LOW // ✅ FIXED (was MIN)
            );

            nc.setShowBadge(false);
            nc.setSound(null, null);
            nc.enableVibration(false);
            nc.enableLights(false);

            NotificationManager manager =
                    (NotificationManager) getSystemService(NotificationManager.class);

            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(nc);
            }
        }
    }
}