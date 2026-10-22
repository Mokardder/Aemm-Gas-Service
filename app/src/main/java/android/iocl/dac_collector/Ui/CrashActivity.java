package android.iocl.dac_collector.Ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Utility.CrashReportStore;

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

        displayCrashDetails(crashDetails, getIntent());

        btnRestart.setOnClickListener(v -> {
            CrashReportStore.clear(this);
            Intent restartIntent = new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(restartIntent);
            finishAndRemoveTask();
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        displayCrashDetails(findViewById(R.id.crashDetails), intent);
    }

    private void displayCrashDetails(TextView crashDetails, Intent intent) {
        String crashInfo = intent == null ? null : intent.getStringExtra("crash_details");
        if (TextUtils.isEmpty(crashInfo)) {
            crashInfo = CrashReportStore.get(this);
        }
        crashDetails.setText(TextUtils.isEmpty(crashInfo)
                ? "No crash details are available."
                : crashInfo);
    }
}
