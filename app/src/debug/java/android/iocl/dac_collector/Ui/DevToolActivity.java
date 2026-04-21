package android.iocl.dac_collector.Ui;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.MainApplication.MyApp;
import android.iocl.dac_collector.Services.FetchProfileInfo;
import android.iocl.dac_collector.Services.FixOppoAutoKill;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.iocl.dac_collector.R;

public class DevToolActivity extends AppCompatActivity {

    private Button btnTest1, btnTest2, btnTest3;
   private LinearLayout collapesable;
   private TextView subsidyHeader;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev_tool);

        initViews();
        initClicks();
    }

    private void initViews() {
        btnTest1 = findViewById(R.id.btn_test1);
        btnTest2 = findViewById(R.id.btn_test2);
        btnTest3 = findViewById(R.id.btn_test3);
//        collapesable = findViewById(R.id.collaplesable);
        subsidyHeader = findViewById(R.id.tv_subsidy_header);
    }

    private void initClicks() {
        View.OnClickListener clickHandler = v -> {
            int id = v.getId();

            if (id == R.id.btn_test1) {
                handleTest1();
            } else if (id == R.id.btn_test2) {
                handleTest2();
            } else if (id == R.id.btn_test3) {
                handleTest3();
            }
        };

        btnTest1.setOnClickListener(clickHandler);
        btnTest2.setOnClickListener(clickHandler);
        btnTest3.setOnClickListener(clickHandler);






        collapesable.setOnClickListener(v -> {
            if (collapesable.getVisibility() == View.GONE) {
                collapesable.setVisibility(View.VISIBLE);
                collapesable.setAlpha(0f);
                collapesable.animate().alpha(1f).setDuration(200);
                subsidyHeader.setText("Subsidies ▲");
            } else {
                collapesable.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                    collapesable.setVisibility(View.GONE);
                });
                subsidyHeader.setText("Subsidies ▼");
            }
        });


    }

    // ================= ACTION METHODS =================

    private void handleTest1() {
        startFetchJob();

    }

    private void handleTest2() {
        Toast.makeText(this, "Test Feature 2 clicked", Toast.LENGTH_SHORT).show();
    }

    private void handleTest3() {
        Toast.makeText(this, "Test Feature 3 clicked", Toast.LENGTH_SHORT).show();
    }


    private void startFetchJob() {
        Intent intent = new Intent(this, FixOppoAutoKill.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent); // ✅ MUST for Android 8+
        } else {
            startService(intent);
        }
    }
}