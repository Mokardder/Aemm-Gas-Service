package android.iocl.dac_collector.Services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
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
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.HashSet;
import java.util.Set;

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






// TODO: Still not fixed the error of deduplication of sms Receiving
//
//                if (smsObserver == null) {
//                    smsObserver = new SmsObserver(new Handler(Looper.getMainLooper()));
//                    getContentResolver().registerContentObserver(
//                            Uri.parse("content://sms"), true, smsObserver
//                    );
//                }


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

        private static final long DEBOUNCE_DELAY = 2000;

        private final Handler debounceHandler;
        private final Set<Long> mPendingSmsIds = new HashSet<>();

        private final Runnable debounceRunnable = this::processPendingSms;

        public SmsObserver(Handler handler) {
            super(handler);
            debounceHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);

            if (uri == null) return;

            String last = uri.getLastPathSegment();
            if (TextUtils.isEmpty(last) || !TextUtils.isDigitsOnly(last)) {
                return;
            }

            long smsId = ContentUris.parseId(uri);

            synchronized (mPendingSmsIds) {
                mPendingSmsIds.add(smsId);
            }

            debounceHandler.removeCallbacks(debounceRunnable);
            debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY);
        }

        private void processPendingSms() {

            Set<Long> smsIds;
            synchronized (mPendingSmsIds) {
                smsIds = new HashSet<>(mPendingSmsIds);
                mPendingSmsIds.clear();
            }

            if (smsIds.isEmpty()) return;

            String selection = "_id IN (" + TextUtils.join(",", smsIds) + ")";

            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(
                        Uri.parse("content://sms/inbox"),
                        new String[]{"address", "body", "date"},
                        selection,
                        null,
                        "date DESC"
                );

                if (cursor == null) return;

                while (cursor.moveToNext()) {
                    int addressIdx = cursor.getColumnIndex("address");
                    int bodyIdx = cursor.getColumnIndex("body");
                    int dateIdx = cursor.getColumnIndex("date");

                    if (addressIdx == -1 || bodyIdx == -1 || dateIdx == -1) {
                        Log.e(TAG, "SMS column missing, skipping row");
                        return;
                    }

                    String sender = cursor.getString(addressIdx);
                    String message = cursor.getString(bodyIdx);
                    long date = cursor.getLong(dateIdx);

                    showSmsToast(sender, message, date);

                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to read SMS", e);
            } finally {
                if (cursor != null) cursor.close();
            }
        }

        private void showSmsToast(String sender, String message, long dateMillis) {

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        boolean isPosted = NotificationHelper.isNotificationActive(
                                getApplicationContext(), Utility.NOTIFICATION_ID);

                        if (!isPosted) {
                            SmsWorkUtil.enqueueSmsWorker(
                                    getApplicationContext(),
                                    sender,
                                    message,
                                    Utility.getStandardDatenTime()
                            );
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error posting SMS toast", e);
                }
            }, 2000);
        }
    }
}