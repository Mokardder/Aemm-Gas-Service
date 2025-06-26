package android.iocl.dac_collector.Ui;

import static android.iocl.dac_collector.Services.AcessibilitySettings.CUSTOM_ACTION;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;

public class DialogActivity extends AppCompatActivity {

    TextView messageTV, go_home;
    Button revealPassword;
    EditText ed_enterPassword;

    ConstraintLayout fullDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.anti_uninstalle_layout);
        messageTV = findViewById(R.id.tv_relative_txt);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);


        go_home = findViewById(R.id.go_home);
        revealPassword = findViewById(R.id.revealPassword);
        ed_enterPassword = findViewById(R.id.ed_enterPassword);

        fullDialog = findViewById(R.id.alertDialog_startup);


        // Create an OnBackPressedCallback that is always enabled
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {


            }
        };

        revealPassword.setOnClickListener(v -> {
            ed_enterPassword.setVisibility(View.VISIBLE);
        });

        ed_enterPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                Log.d("Specialll", "onTextChanged: " + s.toString());

                if (s.toString().equals("Mokardder")){

                    SharedPrefs.setRestrictionDisabled(DialogActivity.this);
                    finish();
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        go_home.setOnClickListener(v -> {
            goToHomePage();
        });

        // Add the callback to the OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, callback);







         fullDialog.setOnClickListener(v -> {
             return;
         });

        String message = getIntent().getStringExtra("key_string");




        // Create the broadcast intent with the custom action and extra data
        Intent broadcastIntent = new Intent(CUSTOM_ACTION);
    // Send the broadcast
        sendBroadcast(broadcastIntent);


    }

    public void goToHomePage() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }



}
