package android.iocl.dac_collector.Ui;

import android.content.Intent;
import android.graphics.Color;
import android.iocl.dac_collector.ModelData.BaseUpdateResponse;
import android.iocl.dac_collector.ModelData.ConsumerData;
import android.iocl.dac_collector.ModelData.UpdateResponse;
import android.iocl.dac_collector.ModelData.search_consumer;
import android.iocl.dac_collector.ModelData.search_consumer_response;
import android.iocl.dac_collector.ModelData.update_consumer;
import android.iocl.dac_collector.RetrofitClient.RequestService;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.os.Bundle;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.RetrofitClient.RetrofitClient;
import android.iocl.dac_collector.Utility.Constant;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchCustomerAndAnnual extends AppCompatActivity {
    TextView user_point_searchTV,
            user_common_name_TV,
            userCons,
            convertedAnnualTV,
            afterCompleteAnnualMonth,
            subscriptionTV;

    SharedPrefs rewardStore;
    RadioButton search_customer, go_back;
    Button redeem_point;
    TextInputEditText search_term;

    RelativeLayout loader, button_group, userDetailsRL, annualCompleteRL;
    TextView loader_text;

    String AnnualPoints;
    private FirebaseAnalytics mFirebaseAnalytics;
    int[] convertedAnnualCalCulate;
    ConsumerData data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        rewardStore = new SharedPrefs(this);
        setContentView(R.layout.activity_search_customer_and_annual);
        loader = findViewById(R.id.ProgressView);
        button_group = findViewById(R.id.button_group);
        loader_text = findViewById(R.id.loadingTxt);
        redeem_point = findViewById(R.id.redeem_point);
        search_term = findViewById(R.id.search_term);
        userDetailsRL = findViewById(R.id.userDetailsRL);
        user_common_name_TV = findViewById(R.id.user_common_name_TV);
        userCons = findViewById(R.id.userCons);
        subscriptionTV = findViewById(R.id.subscription);
        convertedAnnualTV = findViewById(R.id.convertedAnnual);
        search_customer = findViewById(R.id.search_customer);
        afterCompleteAnnualMonth = findViewById(R.id.afterCompleteAnnualMonth);
        annualCompleteRL = findViewById(R.id.annualCompleteRL);
        go_back = findViewById(R.id.go_back);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        AnnualPoints = String.valueOf(rewardStore.getInt(Constant.Pref_AnnualPoint, 0));

        convertedAnnualCalCulate = calculateCoins(Integer.parseInt(AnnualPoints));


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        user_point_searchTV = findViewById(R.id.user_point_searchTV);

        user_point_searchTV.setText(AnnualPoints);


        go_back.setOnClickListener(v -> {
            Intent redeemActivity = new Intent(SearchCustomerAndAnnual.this, MainActivity.class);
            startActivity(redeemActivity);
            finish();
        });

        search_customer.setOnClickListener(v -> {
            String user_search_term = search_term.getText().toString();
            if (!user_search_term.isEmpty()) {
                userFind(user_search_term);
            } else {
                Toast.makeText(this, "Enter Something ...", Toast.LENGTH_SHORT).show();
            }
        });


        redeem_point.setOnClickListener(v -> {
            int[] calcPoint = calculateCoins(rewardStore.getInt(Constant.Pref_AnnualPoint, 0));
            if (calcPoint[0] < 1) {
                Toast.makeText(this, "You Don't have enough Annual Points to Redeem", Toast.LENGTH_SHORT).show();
                return;
            }

            update_user_annual(data.getConsumer_id(), "SUBSCRIPTION", String.valueOf(convertedAnnualCalCulate[0]));

        });
    }

    private void userFind(String userSearchTerm) {
        loader_controller("Fetching Customer ...", true);
        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        search_consumer receiver = new search_consumer("deedup", userSearchTerm);
        Call<search_consumer_response> auth = requestService.search_customer(receiver);

        auth.enqueue(new Callback<search_consumer_response>() {
            @Override
            public void onResponse(Call<search_consumer_response> call, Response<search_consumer_response> response) {

                loader_controller("Fetching Customer ...", false);

                String STATUS_CODE = response.body().getStatusCode();
                List<ConsumerData> userData = response.body().getData();


                if (STATUS_CODE.equals("OK")) {

                    if (userData.size() == 1) {

                        data = userData.get(0);
                        userCons.setText("Cons: " + data.getConsumer_id());
                        subscriptionTV.setText("Booking: " + data.getSubscription());
                        user_common_name_TV.setText("Hi, " + data.getName());



                        Bundle bundle = new Bundle();
                        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "UserProfileSearched");
                        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "User -> " + data.getConsumer_id());
                        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Annual -> " + data.getSubscription());
                        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "AnnualPoint -> " + String.valueOf(rewardStore.getInt(Constant.Pref_AnnualPoint, 0)));
                        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
                        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);


                        if (convertedAnnualCalCulate[0] < 1) {
                            convertedAnnualTV.setTextColor(Color.parseColor("#dc3545"));
                            convertedAnnualTV.setText("~" + String.valueOf(convertedAnnualCalCulate[0]) + " (Require 200)");
                            redeem_point.setEnabled(false);
                        } else {
                            convertedAnnualTV.setTextColor(Color.parseColor("#28a745"));
                            convertedAnnualTV.setText("~" + String.valueOf(convertedAnnualCalCulate[0]));
                            redeem_point.setEnabled(true);
                        }


                        userDetailsRL.setVisibility(View.VISIBLE);


                    } else {

                        Toast.makeText(SearchCustomerAndAnnual.this, "Customer Not Found", Toast.LENGTH_SHORT).show();

                    }


//                Intent user_details_activity = new Intent(getApplicationContext(), UserDetailsActivity.class)
//                        .putExtra("user_name", userData.getName())
//                        .putExtra("user_id", userData.getConsumer_id())
//                        .putExtra("user_mobile", userData.getMobile_no())
//                        .putExtra("user_nick_name", userData.getNick_name())
//                        .putExtra("user_annual", userData.getSubscription())
//                        .putExtra("user_location", userData.getLocation());
//                startActivity(user_details_activity);
                } else {
                    Toast.makeText(getApplicationContext(), userData.get(0).getMsg(), Toast.LENGTH_SHORT).show();

                }


            }

            @Override
            public void onFailure(Call<search_consumer_response> call, Throwable t) {

            }
        });
    }

    private void update_user_annual(String userSearchTerm, String db_column, String value) {
        loader_controller("Updating Booking...", true);


        RequestService requestService = RetrofitClient.retrofit_spreadsheet(getApplicationContext()).create(RequestService.class);
        update_consumer user_update_payload = new update_consumer("updateUserBase", userSearchTerm, db_column, value);
        Call<BaseUpdateResponse> auth = requestService.update_user(user_update_payload);

        auth.enqueue(new Callback<BaseUpdateResponse>() {
            @Override
            public void onResponse(Call<BaseUpdateResponse> call, Response<BaseUpdateResponse> response) {

                loader_controller("", false);


                String STATUS_CODE = response.body().getStatusCode();
                UpdateResponse userData = response.body().getData();

                if (STATUS_CODE.equals("OK")) {

                    annualCompleteRL.setVisibility(View.VISIBLE);

                    int totalMonth = Integer.parseInt(data.getSubscription()) + convertedAnnualCalCulate[0];
                    afterCompleteAnnualMonth.setText("Total Bookings: " + totalMonth);
                    subscriptionTV.setText("Booking: " + totalMonth);
                    deductCoin(convertedAnnualCalCulate[1]);
                    search_customer.setEnabled(false);


                    int[] calcPoint = calculateCoins(rewardStore.getInt(Constant.Pref_AnnualPoint, 0));

                    Toast.makeText(SearchCustomerAndAnnual.this, "Total ~ " + calcPoint[0] + " Remaining ~ " + convertedAnnualCalCulate[1], Toast.LENGTH_SHORT).show();


                    Log.d("Mokardder===>", "Converted: " + calcPoint[0] + " Remaining -> " + calcPoint[1] + " Current Point -> " + rewardStore.getInt(Constant.Pref_AnnualPoint, 0));
                    user_point_searchTV.setText(String.valueOf(calcPoint[1]));

                    convertedAnnualTV.setText(String.valueOf(calcPoint[0]));


                    Toast.makeText(getApplicationContext(), "" + userData.getMsg(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), userData.getMsg(), Toast.LENGTH_LONG).show();
                }


            }

            @Override
            public void onFailure(Call<BaseUpdateResponse> call, Throwable t) {

            }
        });
    }


    private void deductCoin(int Amount) {
        int totalPoint = Amount;
        rewardStore.setInt(Constant.Pref_AnnualPoint, totalPoint);
    }




    private int[] calculateCoins(int points) {
        int coins = points / 200;  // 1 coin for every 200 points
        int remainingPoints = points % 200;  // points left after conversion

        return new int[]{coins, remainingPoints}; // return both coins and remaining points
    }

    public void loader_controller(String loader_text_inp, Boolean ShouldBeShown) {

        if (ShouldBeShown) {
            loader.setVisibility(View.VISIBLE);
            if (!loader_text_inp.isEmpty()) {
                loader_text.setText(loader_text_inp);
            }
        } else {
            loader.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {

        Toast.makeText(this, "Action Disabled !", Toast.LENGTH_SHORT).show();


    }



    }