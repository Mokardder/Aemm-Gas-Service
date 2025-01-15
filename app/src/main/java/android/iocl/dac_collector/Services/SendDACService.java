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
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.MainActivity;
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


import org.json.JSONException;
import org.json.JSONObject;


public class SendDACService extends Service {

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

        Utility.getDACMessages(getApplicationContext());



        return START_NOT_STICKY;
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
                    .setContentTitle("Checking DAC")
                    .setContentText("searcing for dac ..")
                    .setSmallIcon(R.drawable.gas_cylinder_icon);
        }

        startForeground(101, notification.build());




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