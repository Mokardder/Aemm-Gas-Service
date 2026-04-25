package android.iocl.dac_collector.Ui;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.iocl.dac_collector.MainApplication.MyApp;
import android.iocl.dac_collector.ModelData.ConsumerData;
import android.iocl.dac_collector.ModelData.DAC_Collector_Base;
import android.iocl.dac_collector.ModelData.SubsidyRequest;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.RetrofitClient.RequestService;
import android.iocl.dac_collector.RetrofitClient.RetrofitClient;
import android.iocl.dac_collector.Utility.Constant;
import android.iocl.dac_collector.Utility.FirebaseConfigManager;
import android.iocl.dac_collector.Utility.ImageLoader;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.Utility;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.imageview.ShapeableImageView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FloatingTileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_floating_tile);


        ConsumerData data = (ConsumerData) Utility.decodeApiResponse(!Utility.getProfile().equals("N") ? Utility.getProfile() : null, ConsumerData.class);


        ConstraintLayout layoutOpenApp = findViewById(R.id.layoutOpenApp);


        layoutOpenApp.setOnClickListener(v -> {


            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            startActivity(intent);
            finish(); // 🔥 close tile activity
        });



        if (data == null) {
            return;
        }
        boolean isAnnual = data.getCon_type().equals("UJJAWALA") && !data.getBooking_date().isEmpty();
        boolean isUjjawala = data.getCon_type().equals("UJJAWALA");
        boolean isGeneralAutomatedOn = !data.getCon_type().equals("UJJAWALA") && !data.getSubscription().isEmpty();


        TextView tvHello = findViewById(R.id.tvHello);
        TextView tvUserName = findViewById(R.id.tvUserName);

        TextView tvNoticeTitle = findViewById(R.id.tvNoticeTitle);
        TextView tvNoticeDesc = findViewById(R.id.tvNoticeDesc);

        TextView tvActionText = findViewById(R.id.tvActionText);

        TextView tvRemainingValue = findViewById(R.id.tvRemainingValue);
        TextView tvRemainingUnit = findViewById(R.id.tvRemainingUnit);

        TextView tvLastLabel = findViewById(R.id.tvLastLabel);
        TextView tvLastBook = findViewById(R.id.tvLastBook);

        TextView tvVersion = findViewById(R.id.tvVersion);


        tvHello.setText("Hello, ");
        tvHello.setText("Hello, ");
        tvUserName.setText(data.getName());

// Notice
        tvNoticeTitle.setText("Don't Panic, Don't Extra Book");
        tvNoticeDesc.setText("প্যানিক হবেন না, আপনার গ্যাস বুক হবে");

// Action
        tvActionText.setText("গ্যাসের সাবসিডি চেক করুন");

// ===== VALIDITY LOGIC (SYNC WITH YOUR ABOVE CONDITIONS) =====

        if (isAnnual) {

            tvRemainingValue.setText(data.getSubscription()); // months
            tvRemainingUnit.setText("Months Left");

            tvLastLabel.setText("Last Recharge:");
            tvLastBook.setText(data.getBooking_date());

        } else if (!isUjjawala) {

            tvRemainingValue.setText(isGeneralAutomatedOn ? "∞" : "❌");
            tvRemainingUnit.setText("Unlimited");

            tvLastLabel.setText("Automatic Book");

            tvLastBook.setText(isGeneralAutomatedOn ? "Turned ON" : "Turned OFF");
            tvLastBook.setTextColor(Color.parseColor(
                    isGeneralAutomatedOn ? "#006400" : "#FF0000"
            ));

        } else {

            tvRemainingValue.setText("0");
            tvRemainingUnit.setText("Expired");

            tvLastLabel.setText("Recharge End On");
            tvLastBook.setText(data.getSub_end_month());
        }

        FirebaseConfigManager.fetch();


        if (FirebaseConfigManager.isNoticeEnabled()) {

            ShapeableImageView imgNotice = findViewById(R.id.imgNotice);


            findViewById(R.id.noticeLayout).setVisibility(VISIBLE);
            ImageLoader.load(
                    FirebaseConfigManager.getImageURL(),
                    imgNotice,
                    R.drawable.loading_bar
            );

            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) imgNotice.getLayoutParams();

            params.dimensionRatio = FirebaseConfigManager.getNoticeImageDimen(); // 👈 your ratio

            imgNotice.setLayoutParams(params);

            View borderView = findViewById(R.id.noticeLayout);

            ObjectAnimator animator = ObjectAnimator.ofFloat(borderView, "alpha", 1f, 0.5f, 1f);
            animator.setDuration(1000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.start();

            TextView titleTv = findViewById(R.id.tvNoticeTitle);
            TextView descTv = findViewById(R.id.tvNoticeDesc);


            descTv.setText(FirebaseConfigManager.getNoticeDesc());
            titleTv.setText(FirebaseConfigManager.getNoticeTitle());
        } else {
            findViewById(R.id.noticeLayout).setVisibility(GONE);
        }


        ConstraintLayout actionCard = findViewById(R.id.layoutSubsidyCheck);
        TextView subsidyStatustxt = findViewById(R.id.tvActionText);

        String subsidyValue = SharedPrefs.getSubsidyDetails();
        boolean isPending = SharedPrefs.isSubsidyRequestPending();
        boolean isSubsidyEnabled = FirebaseConfigManager.isSubsidyCheckEnabled();

// 👉 CASE 1: Already have subsidy result


        String last = SharedPrefs.getLastSubsidyDate(); // "dd-MM-yyyy"
        long days = Long.MAX_VALUE; // assume expired by default

        if (last != null && !last.trim().isEmpty()) {
            try {
                long lastTime = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
                        .parse(last)
                        .getTime();

                days = (Calendar.getInstance().getTimeInMillis() - lastTime) / 86_400_000L;

            } catch (ParseException e) {
                Log.e("DateError", "Invalid date: " + last, e);
                // keep days = MAX → will clear
            }
        } else {
            Log.w("DateError", "Empty subsidy date");
        }


        if (days >= 3) {
            SharedPrefs.clearSubsidyDetails();
        }


        if (!subsidyValue.isEmpty()) {


            if (subsidyValue.equals("W10=")) {


                Toast.makeText(this, "দয়া করে আরেকবার Request করুন", Toast.LENGTH_SHORT).show();
                SharedPrefs.clearSubsidyDetails();
                SharedPrefs.setIsSubsidyRequestPending(false);
            } else {
                subsidyStatustxt.setText("এখানে ক্লিক করুন সাবসিডি দেখার জন্য");

                subsidyStatustxt.setTextColor(
                        ContextCompat.getColor(MyApp.getContext(), R.color.red)
                );

                actionCard.setBackgroundResource(R.drawable.rounded_blue_background);

                actionCard.setEnabled(true);
                actionCard.setClickable(true);
                actionCard.setAlpha(1f);

                actionCard.setOnClickListener(v -> {
                    startActivity(new Intent(this, BankStatementActivity.class));
                });

                return; // ❗ STOP here (important)
            }
        }

// 👉 CASE 2: Request already pending
        if (isPending) {
            actionCard.setEnabled(false);
            actionCard.setClickable(false);
            actionCard.setAlpha(0.5f);
            subsidyStatustxt.setText("আপনার Request অলরেডি processed হয়েছে");
            return;
        }

// 👉 CASE 3: Feature disabled from Firebase
        if (!isSubsidyEnabled) {
            actionCard.setEnabled(false);
            actionCard.setClickable(false);
            actionCard.setAlpha(0.5f);
            subsidyStatustxt.setText(FirebaseConfigManager.getReasonForSubsidyBlock());
            return;
        }

// 👉 CASE 4: Normal flow (user can request)
        actionCard.setEnabled(true);
        actionCard.setClickable(true);
        actionCard.setAlpha(1f);
        subsidyStatustxt.setText("গ্যাসের সাবসিডি চেক করুন");

        actionCard.setOnClickListener(view -> {
            String FCM_Key = SharedPrefs.getFCMKey();
            requestSubsidyDetails(actionCard, subsidyStatustxt, FCM_Key);
        });


        try {
            String version = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;

            tvVersion.setText("V" + version + " | Prod: Padmalavpur");
        } catch (Exception e) {
            tvVersion.setText("V-");
        }


    }


    private void requestSubsidyDetails(ConstraintLayout actionCard, TextView subsidyStatustxt, String FCM_KEY_param) {

        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        Context ctx = getApplicationContext();
        SubsidyRequest req = new SubsidyRequest("addSubsidyRequest", SharedPrefs.getConsumerId(), SharedPrefs.getUsername(), FCM_KEY_param);

        Call<DAC_Collector_Base> auth = requestService.requestSubsidyDetails(req);
        auth.enqueue(new Callback<DAC_Collector_Base>() {
            @Override
            public void onResponse(Call<DAC_Collector_Base> call, Response<DAC_Collector_Base> response) {
//                loader_controller("Requesting...", false, loader, loader_text);
                DAC_Collector_Base body = response.body();

                if (body == null) {
                    Toast.makeText(ctx, "Empty response from server", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean isSuccess = Boolean.TRUE.equals(body.getSuccess());


                String message = body.getMessage() != null ? body.getMessage() : "Operation failed";

                if (isSuccess) {
                    Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
                    SharedPrefs.setIsSubsidyRequestPending(true);

                    actionCard.setEnabled(false);
                    actionCard.setClickable(false);
                    actionCard.setAlpha(0.5f);
                    subsidyStatustxt.setText("আপনার Request অলরেডি processed হয়েছে");

//                    dialog.dismiss();
                } else {
                    Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<DAC_Collector_Base> call, Throwable t) {
//                loader_controller("Requesting...", true, loader, loader_text);
                Toast.makeText(ctx, Constant.API_FAILURE, Toast.LENGTH_SHORT).show();

            }
        });

    }


}



