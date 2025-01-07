package android.iocl.dac_collector.Services;



import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.iocl.dac_collector.ModelData.search_consumer;
import android.iocl.dac_collector.ModelData.search_consumer_response;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.RetrofitClient.RequestService;
import android.iocl.dac_collector.RetrofitClient.RetrofitClient;
import android.iocl.dac_collector.Ui.MainActivity;
import android.iocl.dac_collector.Utility.Constant;
import android.iocl.dac_collector.Utility.Utility;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.Toast;


import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;


import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class FetchProfileInfo extends Service {
    private static String CHANNEL_ID = "MainServiceActions";

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate() {


        createNotificationChannel();
        startServiceWithNotification();



        super.onCreate();

    }




    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String number = Utility.getConsID(getApplicationContext());

        refreshUser(number);



        return START_STICKY;
    }

    private void refreshUser(String userSearchTerm) {

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


            }

            @Override
            public void onFailure(Call<search_consumer_response> call, Throwable t) {

            }
        });
    }









    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                    .setSmallIcon(R.drawable.gas_cylinder_icon);
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