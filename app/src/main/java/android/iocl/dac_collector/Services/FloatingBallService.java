package android.iocl.dac_collector.Services;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.iocl.dac_collector.R;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class FloatingBallService extends Service {

    private static WindowManager windowManager;
    private static View floatingView;

    @Override
    public void onCreate() {
        super.onCreate();

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
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });

        // Add click listener for the floating ball
        ImageView ballIcon = floatingView.findViewById(R.id.ball_icon);
        ballIcon.setOnClickListener(v -> {
            // Perform your action here
        });
    }


    public static void resizeOverlay(int newWidth, int newHeight) {
        if (floatingView != null) {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) floatingView.getLayoutParams();
            params.width = newWidth;  // Set the new width
            params.height = newHeight; // Set the new height

            // Apply the new layout parameters
            windowManager.updateViewLayout(floatingView, params);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
    }
}
