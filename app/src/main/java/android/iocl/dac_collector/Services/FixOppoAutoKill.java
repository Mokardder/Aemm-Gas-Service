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
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.iocl.dac_collector.MainApplication.MyApp;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Receivers.MyReceiver;
import android.iocl.dac_collector.SyncAdapters.SyncUtils;
import android.iocl.dac_collector.Ui.MainActivity;
import android.iocl.dac_collector.Ui.PermissionActivity;
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
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.HashSet;
import java.util.Set;

public class FixOppoAutoKill extends Service {
    private static final String CHANNEL_ID = "fix_oppo_channel";
    private static final String CHANNEL_NAME = "Block this | FixOppoAutoKill";
    private static final String TAG = "FixOppoAutoKill";
    private static final String PREF_LAST_SMS_TIME = "last_sms_time";
    private static final String PREF_LAST_SMS_ID = "last_sms_id";
    private static final int NOTIFICATION_ID = 1;
    private final Object SMS_PROCESS_LOCK = new Object();
    private MyReceiver myReceiver;
    private boolean isReceiverRegistered = false;
    private SmsObserver smsObserver;
    private static boolean isServiceRunning = false;
    private boolean isForegroundStarted = false;

    @Override
    public void onCreate() {
        super.onCreate();

        if (isServiceRunning) {
            Log.d(TAG, "Service already initialized");
            return;
        }

        isServiceRunning = true;

        try {

            createNotificationChannel();

            if (!isForegroundStarted) {
                Notification notification = createNotification();
                startForeground(NOTIFICATION_ID, notification);
                isForegroundStarted = true;
            }

            new Thread(() -> {
                try {
                    JobSchedulerUtil.fetch_profile_info(getApplicationContext());
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing jobs", e);
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

                Log.d(TAG, "Receiver registered");

            } else {

                Log.d(TAG, "Receiver already registered");

            }


            if (smsObserver == null) {

                smsObserver = new SmsObserver(
                        new Handler(Looper.getMainLooper())
                );

                getContentResolver().registerContentObserver(
                        Uri.parse("content://sms"),
                        true,
                        smsObserver
                );

                Log.d(TAG, "SMS observer registered");

            } else {

                Log.d(TAG, "SMS observer already registered");

            }

        } catch (Exception e) {

            Log.e(TAG, "onStartCommand failed", e);

        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        isServiceRunning = false;
        isForegroundStarted = false;

        try {

            if (isReceiverRegistered && myReceiver != null) {

                unregisterReceiver(myReceiver);

                isReceiverRegistered = false;

                myReceiver = null;

                Log.d(TAG, "Receiver unregistered");
            }

        } catch (Exception e) {

            Log.e(TAG, "Receiver unregister failed", e);

        }

        try {

            if (smsObserver != null) {

                getContentResolver().unregisterContentObserver(smsObserver);

                smsObserver = null;

                Log.d(TAG, "SMS observer unregistered");
            }

        } catch (Exception e) {

            Log.e(TAG, "Observer unregister failed", e);

        }

        try {

            scheduleServiceRestart();

        } catch (Exception e) {

            Log.e(TAG, "Restart scheduling failed", e);

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

    private long getLastProcessedSmsId() {
        return getSharedPreferences("sms_guard", MODE_PRIVATE)
                .getLong(PREF_LAST_SMS_ID, -1);
    }

    private void saveLastProcessedSmsId(long smsId) {
        getSharedPreferences("sms_guard", MODE_PRIVATE)
                .edit()
                .putLong(PREF_LAST_SMS_ID, smsId)
                .apply();
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
            synchronized (SMS_PROCESS_LOCK) {

                Set<Long> smsIds;
                synchronized (mPendingSmsIds) {
                    smsIds = new HashSet<>(mPendingSmsIds);
                    mPendingSmsIds.clear();
                }

                if (!smsIds.isEmpty()) {
                    processByIds(smsIds);
                } else {
                    processLatestSmsFallback();
                }
            }
        }


        private void processLatestSmsFallback() {

            Cursor cursor = null;

            try {
                cursor = getContentResolver().query(
                        Uri.parse("content://sms/inbox"),
                        new String[]{"_id", "address", "body", "date"},
                        null,
                        null,
                        "date DESC LIMIT 1"
                );

                if (cursor == null || !cursor.moveToFirst()) return;

                long smsId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));

                // 🔒 HARD DEDUP — ID BASED
                if (smsId <= getLastProcessedSmsId()) {
                    Log.d(TAG, "Fallback skipped (duplicate SMS ID)");
                    return;
                }

                String sender = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                String message = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                long smsTime = cursor.getLong(cursor.getColumnIndexOrThrow("date"));

                long now = System.currentTimeMillis();
                final long TEN_SECONDS = 10_000L;

                if (now - smsTime > TEN_SECONDS) return;

                Log.d(TAG, "*** FALLBACK SMS ACCEPTED ***");

                showSmsToast(sender, message, smsTime);

                // 🔐 SAVE ID
                saveLastProcessedSmsId(smsId);

            } catch (Exception e) {
                Log.e(TAG, "Fallback SMS failed", e);
            } finally {
                if (cursor != null) cursor.close();
            }
        }


        private void processByIds(Set<Long> smsIds) {

            String selection = "_id IN (" + TextUtils.join(",", smsIds) + ")";
            Cursor cursor = null;

            try {
                cursor = getContentResolver().query(
                        Uri.parse("content://sms/inbox"),
                        new String[]{"_id", "address", "body", "date"},
                        selection,
                        null,
                        null
                );

                if (cursor == null) return;

                long now = System.currentTimeMillis();
                final long TEN_SECONDS = 10_000L;

                while (cursor.moveToNext()) {

                    long smsId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));

                    // 🔒 HARD DEDUP — ID BASED
                    if (smsId <= getLastProcessedSmsId()) {
                        Log.d(TAG, "Skipping duplicate SMS ID: " + smsId);
                        continue;
                    }

                    String sender = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    String message = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    long smsTime = cursor.getLong(cursor.getColumnIndexOrThrow("date"));

                    if (Math.abs(now - smsTime) > TEN_SECONDS) continue;

                    Log.d(TAG, "*** SMS ACCEPTED (ID MATCH) ***");

                    showSmsToast(sender, message, smsTime);

                    // 🔐 SAVE ID, NOT TIME
                    saveLastProcessedSmsId(smsId);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to process SMS by ID", e);
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

                            Log.d(TAG, "is Calling Twice ? :FixOppoAutoKill():showSmsToast");
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