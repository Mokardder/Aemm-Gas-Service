package android.iocl.dac_collector.Utility;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.iocl.dac_collector.R;

public class SmsOtpPopup {

    private final Context context;
    private final WindowManager wm;
    private PopupWindow popupWindow;
    private View overlayView;
    private String otpCode;

    private SmsOtpPopup(@NonNull Context context) {
        this.context = context;
        this.wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * Entry point: invoke with SmsOtpPopup.with(context).show("1234")
     */
    public static SmsOtpPopup with(@NonNull Context context) {
        return new SmsOtpPopup(context);
    }

    private String currentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        return sdf.format(new Date());
    }

    /**
     * Show the popup or update existing, with swipe-to-dismiss and fade effect
     */
    public void show(@NonNull String otpCode) {
        this.otpCode = otpCode;

        new Handler(Looper.getMainLooper()).post(() -> {
            // If Activity popup already shown, update its views
            if (popupWindow != null && popupWindow.isShowing()) {
                View content = popupWindow.getContentView();
                updateContentViews(content);
                return;
            }
            // If overlay already shown, update its views
            if (overlayView != null) {
                updateContentViews(overlayView);
                return;
            }

            // Create new content view
            LayoutInflater inflater = LayoutInflater.from(context);
            View content = inflater.inflate(R.layout.sms_card_layout, null, false);
            bindContentTouch(content);

            // Show as PopupWindow for Activity contexts
            if (context instanceof Activity) {
                popupWindow = createPopupWindow(content);
                View parent = ((Activity) context).getWindow().getDecorView();
                popupWindow.showAtLocation(parent, Gravity.TOP, 0, getStatusBarHeight());

            } else {
                // system overlay
                overlayView = content;
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );
                params.gravity = Gravity.TOP;
                params.y = getStatusBarHeight();
                wm.addView(overlayView, params);
            }
        });
    }

    // Helper to bind gesture, time, otp, buttons
    private void bindContentTouch(View content) {
        // bind views
        TextView tvOtp     = content.findViewById(R.id.tv_otp);
        TextView tvTime    = content.findViewById(R.id.tv_header);
        TextView btnCall   = content.findViewById(R.id.btn_call);
        ImageView btnClose = content.findViewById(R.id.btn_close);

        tvTime.setText("SMS • " + currentTime());
        tvOtp.setText(this.otpCode);
        btnCall.setOnClickListener(v -> makeCall());
        btnClose.setOnClickListener(v -> dismiss());

        // swipe-to-dismiss thresholds
        final int SWIPE_THRESHOLD = 100;
        content.setOnTouchListener(new View.OnTouchListener() {
            float downX;
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = ev.getX();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = ev.getX() - downX;
                        v.setTranslationX(dx);
                        v.setAlpha(1f - Math.min(Math.abs(dx) / v.getWidth(), 1f));
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        float totalDx = ev.getX() - downX;
                        if (Math.abs(totalDx) > SWIPE_THRESHOLD) {
                            v.animate()
                                    .translationX(totalDx > 0 ? v.getWidth() : -v.getWidth())
                                    .alpha(0f)
                                    .setDuration(200)
                                    .withEndAction(() -> SmsOtpPopup.this.dismiss());
                        } else {
                            v.animate()
                                    .translationX(0)
                                    .alpha(1f)
                                    .setDuration(200)
                                    .start();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    // Helper to update existing content view (in-place)
    private void updateContentViews(View content) {
        TextView tvOtp  = content.findViewById(R.id.tv_otp);
        TextView tvTime = content.findViewById(R.id.tv_header);
        tvTime.setText("SMS • " + currentTime());
        tvOtp.setText(this.otpCode);
        // reset any transform
        content.setTranslationX(0);
        content.setAlpha(1f);
    }

    private PopupWindow createPopupWindow(View content) {
        PopupWindow pw = new PopupWindow(
                content,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        pw.setOutsideTouchable(false);
        pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pw.setAnimationStyle(android.R.style.Animation_Dialog);
        return pw;
    }

    private void makeCall() {
        Intent phoneIntent = new Intent(Intent.ACTION_CALL);
        phoneIntent.setData(Uri.parse("tel:+919231902703"));
        phoneIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(phoneIntent);
    }

    /**
     * Dismiss the popup or overlay
     */
    public void dismiss() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
            popupWindow = null;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            if (overlayView != null) {
                wm.removeView(overlayView);
                overlayView = null;
            }
        });
    }

    private int getStatusBarHeight() {
        int resId = context.getResources().getIdentifier(
                "status_bar_height", "dimen", "android");
        return resId > 0
                ? context.getResources().getDimensionPixelSize(resId)
                : 0;
    }
}
