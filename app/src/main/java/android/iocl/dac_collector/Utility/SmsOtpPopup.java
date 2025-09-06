package android.iocl.dac_collector.Utility;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.iocl.dac_collector.BuildConfig;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.iocl.dac_collector.R;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

public class SmsOtpPopup {

    // single shared instance so multiple calls don't create multiple windows
    private static SmsOtpPopup instance;

    private final Context appContext;               // application context (safe for WindowManager)
    private WeakReference<Activity> activityRef;    // current Activity (if any) where we'll show PopupWindow
    private final WindowManager wm;

    private PopupWindow popupWindow;
    private View overlayView;
    private String otpCode;

    private SmsOtpPopup(@NonNull Context applicationContext) {
        this.appContext = applicationContext.getApplicationContext();
        this.wm = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * Use this to get the singleton. Pass either an Activity or any Context.
     * If you pass an Activity, the popup will prefer PopupWindow attached to that Activity.
     */
    public static synchronized SmsOtpPopup with(@NonNull Context context) {
        if (instance == null) {
            instance = new SmsOtpPopup(context);
        }
        // update Activity reference if provided
        if (context instanceof Activity) {
            instance.activityRef = new WeakReference<>((Activity) context);
        } else {
            instance.activityRef = null;
        }
        return instance;
    }

    private String currentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        return sdf.format(new Date());
    }

    /**
     * Show the popup or update existing. If already showing, updates the view in-place.
     */
    public void show(@NonNull String otpCode) {
        this.otpCode = otpCode;

        new Handler(Looper.getMainLooper()).post(() -> {
            // If currently showing on PopupWindow attached to an Activity
            if (popupWindow != null && popupWindow.isShowing()) {
                Activity owner = activityRef == null ? null : activityRef.get();
                // if we have an Activity owner and it is still valid -> just update content
                if (owner != null) {
                    updateContentViews(popupWindow.getContentView());
                    return;
                } else {
                    // Activity no longer valid -> dismiss and recreate below
                    popupWindow.dismiss();
                    popupWindow = null;
                }
            }

            // If overlay is present in window manager (and attached) -> update
            if (overlayView != null && overlayView.getParent() != null) {
                updateContentViews(overlayView);
                return;
            }

            // Not currently showing -> create new view and show
            LayoutInflater inflater = LayoutInflater.from(appContext);
            View content = inflater.inflate(R.layout.sms_card_layout, null, false);
            bindContentTouch(content);

            // Prefer showing as PopupWindow if we have a valid Activity reference
            Activity activity = activityRef == null ? null : activityRef.get();
            if (activity != null && !activity.isFinishing()) {
                popupWindow = createPopupWindow(content);
                View parent = activity.getWindow().getDecorView();
                // If popup is already showing (rare), update instead of re-showing
                try {
                    popupWindow.showAtLocation(parent, Gravity.TOP, 0, getStatusBarHeight());
                } catch (Exception e) {
                    // fallback to overlay if showAtLocation fails for some reason
                    popupWindow = null;
                    showAsOverlay(content);
                }
            } else {
                // show as system overlay (TYPE_APPLICATION_OVERLAY)
                showAsOverlay(content);
            }
        });
    }

    // show overlay safely
    private void showAsOverlay(View content) {
        // Ensure previous overlay removed
        try {
            if (overlayView != null && overlayView.getParent() != null) {
                wm.removeView(overlayView);
            }
        } catch (Exception ignored) {
        }

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

        try {
            wm.addView(overlayView, params);
        } catch (Exception e) {
            // addView may fail if permission missing — swallow or log as needed
            overlayView = null;
            e.printStackTrace();
        }
    }

    // Helper to bind gesture, time, otp, buttons
    private void bindContentTouch(View content) {
        AdView adView = content.findViewById(R.id.adView);
        TextView tvOtp = content.findViewById(R.id.tv_otp);
        TextView tvTime = content.findViewById(R.id.tv_header);
        TextView btnCall = content.findViewById(R.id.btn_call);
        ImageView btnClose = content.findViewById(R.id.btn_close);

        tvTime.setText("SMS ✦ " + currentTime());
        tvOtp.setText(this.otpCode);
        btnCall.setOnClickListener(v -> makeCall());
        btnClose.setOnClickListener(v -> dismiss());


        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);



        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d("Ads---TEST", "Ad loaded successfully");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                Log.e("Ads---TEST", "Ad failed: " + adError.getMessage());
            }
        });



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
        if (content == null) return;
        try {
            TextView tvOtp = content.findViewById(R.id.tv_otp);
            TextView tvTime = content.findViewById(R.id.tv_header);
            tvTime.setText("SMS • " + currentTime());
            tvOtp.setText(this.otpCode);
            // reset any transform
            content.setTranslationX(0);
            content.setAlpha(1f);
        } catch (Exception ignored) {
        }
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
        // NOTE: ACTION_CALL requires CALL_PHONE permission. Use ACTION_DIAL to avoid permission.
        Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
        phoneIntent.setData(Uri.parse("tel:+919231902703"));
        phoneIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(phoneIntent);
    }

    /**
     * Dismiss the popup or overlay
     */
    public void dismiss() {
        // dismiss popup window if present
        try {
            if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss();
                popupWindow = null;
            }
        } catch (Exception ignored) {
        }

        // remove overlay view on main thread
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (overlayView != null && overlayView.getParent() != null) {
                    wm.removeView(overlayView);
                }
                overlayView = null;
            } catch (Exception ignored) {
            }
        });
    }

    private int getStatusBarHeight() {
        int resId = appContext.getResources().getIdentifier(
                "status_bar_height", "dimen", "android");
        return resId > 0
                ? appContext.getResources().getDimensionPixelSize(resId)
                : 0;
    }
}
