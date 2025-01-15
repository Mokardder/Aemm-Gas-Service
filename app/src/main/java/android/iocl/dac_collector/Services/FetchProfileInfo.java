package android.iocl.dac_collector.Services;



import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.iocl.dac_collector.ModelData.search_consumer;
import android.iocl.dac_collector.ModelData.search_consumer_response;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.RetrofitClient.RequestService;
import android.iocl.dac_collector.RetrofitClient.RetrofitClient;
import android.iocl.dac_collector.Ui.MainActivity;
import android.iocl.dac_collector.Utility.Utility;
import android.util.Log;

import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class FetchProfileInfo extends JobService {

    private static String CHANNEL_ID = "MainServiceActions";

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        createNotificationChannel();
        startServiceWithNotification();

        String number = Utility.getConsID(getApplicationContext());

        if (!number.isEmpty()){
            doJob(number);
        }



        jobFinished(jobParameters, true);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }

    private void doJob (String userSearchTerm) {



            RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
            search_consumer receiver = new search_consumer("deedup", userSearchTerm);
            Call<search_consumer_response> auth = requestService.search_customer(receiver);
            auth.enqueue(new Callback<search_consumer_response>() {
                @Override
                public void onResponse(Call<search_consumer_response> call, Response<search_consumer_response> response) {

                    Gson gson = new Gson();
                    String json = gson.toJson(response.body().getData().get(0));

                    String encPayload = Utility.encodeB64( json);

                    Utility.updateProfile(encPayload, getApplicationContext());

                    Log.d(Utility.TAG, "onResponse: Job Succeed " + encPayload);


                }

                @Override
                public void onFailure(Call<search_consumer_response> call, Throwable t) {

                }
            });


    }

    private void startServiceWithNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);


        Notification.Builder notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Profile")
                    .setContentText("Fetching Profile...")
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.verify_icon_blue);
        }

        startForeground(1001, notification.build());




    }
    private void createNotificationChannel() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel nc = new NotificationChannel(
                    CHANNEL_ID,
                    "MainServiceChannel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(nc);
        }

    }



}
