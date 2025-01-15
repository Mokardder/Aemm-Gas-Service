package android.iocl.dac_collector.Services;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Utility.Utility;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class FloatingBallService extends Service {

    private static final String CHANNEL_ID = "FloatingBallChannel";
    public static boolean isInternetAvailable = false;

    private static WindowManager windowManager;
    private static View floatingView;
    private Disposable networkDisposable;
    private Disposable internetDisposable;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create a notification channel
        createNotificationChannel();

        // Create a notification for the foreground service
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Floating Ball Service")
                .setContentText("The floating ball is active.")
                .setSmallIcon(R.drawable.verify_icon_blue)
                .build();

        // Start the service in the foreground
        startForeground(1, notification);

        // Inflate the floating view
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_ball, null);

        // Set up the WindowManager layout parameters
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        // Add the view to the WindowManager
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, params);

        // Handle drag and touch events
        floatingView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    params.x = (int) event.getRawX();
                    params.y = (int) event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = (int) event.getRawX();
                    params.y = (int) event.getRawY();
                    windowManager.updateViewLayout(floatingView, params);
                    return true;
            }
            return false;
        });

        // Add click listener for the floating ball
        ImageView ballIcon = floatingView.findViewById(R.id.ball_icon);
        ballIcon.setOnClickListener(v -> {
            // Perform your action here
            Log.d("FloatingBall", "Floating ball clicked!");
        });



        try {

            networkDisposable = ReactiveNetwork.observeNetworkConnectivity(getApplicationContext())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(connectivity -> Log.d("NetworkState", connectivity.toString()));

            // Observe internet connectivity
            internetDisposable = ReactiveNetwork.observeInternetConnectivity()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(isConnected -> {
                        isInternetAvailable = isConnected;
                        if (isConnected) {

                            Utility.sendAnyUnsentDAC(getApplicationContext());
                        }
                    });

        }catch (Exception e){

        }

        // Observe network connectivity

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }

        try {

            if (networkDisposable != null && !networkDisposable.isDisposed()) {
                networkDisposable.dispose();
            }
            if (internetDisposable != null && !internetDisposable.isDisposed()) {
                internetDisposable.dispose();
            }

        }catch (Exception e){

        }

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Floating Ball Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
