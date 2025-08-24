package android.iocl.dac_collector.Services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.iocl.dac_collector.ModelData.DAC_Collector_Base;
import android.iocl.dac_collector.ModelData.SearchQuery;
import android.iocl.dac_collector.ModelData.search_consumer;

import android.iocl.dac_collector.R;
import android.iocl.dac_collector.RetrofitClient.RequestService;
import android.iocl.dac_collector.RetrofitClient.RetrofitClient;
import android.iocl.dac_collector.SyncAdapters.SyncUtils;
import android.iocl.dac_collector.Ui.MainActivity;

import android.iocl.dac_collector.Utility.Utility;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;



// ... keep all original imports ...

@SuppressLint("SpecifyJobSchedulerIdRange")
public class FetchProfileInfo extends JobService {

    private static final String CHANNEL_ID = "5662";
    private JobParameters mJobParameters;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        mJobParameters = jobParameters;
        createNotificationChannel();
        startServiceWithNotification();

        SyncUtils.triggerImmediateSync();


        String number = Utility.getConsID(getApplicationContext());
        if (!number.isEmpty()) {
            doJob(number);
        } else {
            jobFinished(jobParameters, true);
        }
        return true; // Job is running on main thread
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true; // Reschedule if interrupted
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
                if (response.isSuccessful() && response.body() != null) {
                    String encResponse = response.body().getData();
                    Utility.updateProfile(encResponse, getApplicationContext());
                }
                jobFinished(mJobParameters, false); // Job complete
            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {
                jobFinished(mJobParameters, true); // Reschedule job
            }
        });
    }

    private void startServiceWithNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Profile")
                    .setContentText("Fetching Profile...")
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.verify_icon_blue)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("Profile")
                    .setContentText("Fetching Profile...")
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.verify_icon_blue)
                    .build();
        }

        startForeground(1001, notification);
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel nc = new NotificationChannel(
                    CHANNEL_ID,
                    "Also block  this too !",
                    NotificationManager.IMPORTANCE_LOW // Reduced importance
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(nc);
            }
        }
    }
}