package android.iocl.dac_collector.Ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.iocl.dac_collector.ModelData.PermissionItem;
import android.iocl.dac_collector.Utility.PermissionUtility;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.adapter.PermissionAdapter;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.iocl.dac_collector.R;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PermissionActivity extends AppCompatActivity {
    RecyclerView recycler;
    private String FCM_KEY = null;
    LinearLayout loader;
    TextView loader_text;


    private PermissionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_permission);

        loader = findViewById(R.id.loaderLayout);
        loader_text = findViewById(R.id.loadingText_UI);


        populateMenuBar();


        String[] missingPermissions = getIntent().getStringArrayExtra("permissions");

        if (missingPermissions != null) {
            List<PermissionItem> permissionItems = PermissionUtility.buildPermissionItemList(
                    Arrays.asList(missingPermissions)
            );

            recycler = findViewById(R.id.recycler_permissions);
            recycler.setLayoutManager(new LinearLayoutManager(this));
            adapter = new PermissionAdapter(this, permissionItems);
            recycler.setAdapter(adapter);
        }


        refreshFCMToken();


    }

    public void loader_controller(String loader_text_inp, Boolean ShouldBeShown, LinearLayout loader, TextView loader_text) {

        if (ShouldBeShown) {
            loader.setVisibility(View.VISIBLE);
            if (!loader_text_inp.isEmpty()) {
                loader_text.setText(loader_text_inp);
            }
        } else {
            loader.setVisibility(View.GONE);
        }
    }

    private void refreshFCMToken() {
        loader_controller("Registering Device...", true, loader, loader_text);
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FCM_KEY = task.getResult();
                        loader_controller("Registering Device...", false, loader, loader_text);
                        showFCMKeyDailog();
                        SharedPrefs.setFCMKey(getApplicationContext(), FCM_KEY);
                    }
                });
    }

    private void showFCMKeyDailog() {

        if (isFinishing() || isDestroyed()) return; // avoid showing after activity is gone

        ConstraintLayout relativeLayoutAlert = findViewById(R.id.alertDialog_startup);

        // Use Activity context to inflate layout
        View view = LayoutInflater.from(PermissionActivity.this).inflate(R.layout.fcm_key_dialog, relativeLayoutAlert, false);

        AlertDialog.Builder builder = new AlertDialog.Builder(PermissionActivity.this);
        builder.setView(view);

        TextView tvKey = view.findViewById(R.id.fcmkeyTV);
        TextView fetchBtn = view.findViewById(R.id.fetchBtn);

        if (FCM_KEY != null) {
            tvKey.setText(FCM_KEY);
        } else {
            tvKey.setText("Not yet generated");
        }


        final AlertDialog alertDialog = builder.create();

        fetchBtn.setOnClickListener(v -> {


            refreshFCMToken();

            alertDialog.dismiss();
        });


        alertDialog.setCancelable(true);

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();
    }

    private void populateMenuBar() {
        ImageView menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(PermissionActivity.this, v);
            popup.getMenuInflater().inflate(R.menu.permission_activity_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_fcm_key) {

                    showFCMKeyDailog();
                    // Handle FCM Key click
//                    Toast.makeText(this, "FCM Key selected", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });
            popup.show();
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        adapter.refreshAndCheckCompletion();

    }
}