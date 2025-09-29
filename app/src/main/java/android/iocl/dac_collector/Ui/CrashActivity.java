package android.iocl.dac_collector.Ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.iocl.dac_collector.R;

public class CrashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView crashDetails = findViewById(R.id.crashDetails);
        Button btnRestart = findViewById(R.id.btnRestart);

        // Get crash details from intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("crash_details")) {
            String crashInfo = intent.getStringExtra("crash_details");
            crashDetails.setText(crashInfo);
        }

        // Restart app button
        btnRestart.setOnClickListener(v -> {
            Intent restartIntent = getPackageManager()
                    .getLaunchIntentForPackage(getPackageName());
            if (restartIntent != null) {
                restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(restartIntent);
            }
            finish();
        });
    }
}
