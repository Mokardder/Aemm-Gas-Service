package android.iocl.dac_collector.Ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.iocl.dac_collector.ModelData.PermissionItem;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Utility.PermissionUtility;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.adapter.PermissionAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PermissionActivity extends AppCompatActivity {
    private final Handler backHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    RecyclerView recycler;
    String TAG = PermissionActivity.class.toString();
    LinearLayout loader;
    TextView loader_text, skipPermissions;
    boolean doubleBack = false;
    SwipeRefreshLayout swipeRefresh;
    private String FCM_KEY = null;
    private PermissionAdapter adapter;
    private boolean isInitialLoad = true;
    boolean showMandatory = true;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // crucial
        showMandatory = intent.getBooleanExtra("showMandatory", true);
        Log.d(TAG, "Updated intent, showMandatory: " + showMandatory);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_permission);

        showMandatory = getIntent().getBooleanExtra("showMandatory", true);

        Log.d(TAG, "showMandatory: " + showMandatory);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (doubleBack || !showMandatory) {
                    finish();
                    return;
                }
                doubleBack = true;
                Toast.makeText(PermissionActivity.this, "Back again to exit!", Toast.LENGTH_SHORT).show();
                backHandler.postDelayed(() -> doubleBack = false, 2000);
            }
        });

        initViews();
        setupSwipeRefresh();
        populateMenuBar();
        setupSkipPermissions();

        // Show loader immediately and load permissions asynchronously
        showLoader("Checking permissions...");
        loadPermissionsAsync(true);

    }

    private void initViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh);
        loader = findViewById(R.id.loaderLayout);
        loader_text = findViewById(R.id.loadingText_UI);
        skipPermissions = findViewById(R.id.skipPermissions);
        recycler = findViewById(R.id.recycler_permissions);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        // Initially hide recycler until permissions are loaded
        recycler.setVisibility(View.GONE);
    }

    private void setupSwipeRefresh() {
//        showMandatory = true;
        swipeRefresh.setOnRefreshListener(() -> {
            // Show refreshing indicator in swipe refresh
            loadPermissionsAsync(false);
        });
    }

    private void setupSkipPermissions() {
        skipPermissions.setOnClickListener(view -> {
            showPermissionChoiceDialog(this);
        });
    }

    /**
     * Show loader with custom text
     */
    private void showLoader(String message) {
        runOnUiThread(() -> {
            loader.setVisibility(View.VISIBLE);
            loader_text.setText(message);
            recycler.setVisibility(View.GONE);
        });
    }

    /**
     * Hide loader and show content
     */
    private void hideLoader() {
        runOnUiThread(() -> {
            loader.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
            swipeRefresh.setRefreshing(false);
        });
    }

    /**
     * Show loader with error state
     */
    private void showErrorLoader(String message) {
        runOnUiThread(() -> {
            loader.setVisibility(View.VISIBLE);
            loader_text.setText(message);
            recycler.setVisibility(View.GONE);
        });
    }

    /**
     * Load permissions in background thread to avoid blocking UI
     */
    private void loadPermissionsAsync(boolean showLoader) {


        // Don't show loader for swipe refresh - use swipe refresh indicator instead
        if (!swipeRefresh.isRefreshing() && isInitialLoad) {
            if (showLoader){
                showLoader("Checking permissions...");
            }

        }

        backgroundExecutor.execute(() -> {
            try {
                // Simulate some delay to show loader (remove in production if not needed)
                if (isInitialLoad) {
                    Thread.sleep(500); // Just to show loader, remove if permissions check is fast
                }


                Log.d(TAG, "loadPermissionsAsync: isOnlyMandatory " + showMandatory);
                Log.d(TAG, "loadPermissionsAsync: isAnyMissning perm " + PermissionUtility.isAnyPermissionMissing(PermissionActivity.this, showMandatory));

                if (!PermissionUtility.isAnyPermissionMissing(PermissionActivity.this, showMandatory)){

                    startActivity(new Intent(this, MainActivity.class));

                }

                // Get missing permissions in background
                List<String> missingPermissions = PermissionUtility.getMissingPermissions(
                        PermissionActivity.this, showMandatory
                );

                // Build permission items list
                List<PermissionItem> permissionItems = PermissionUtility.buildPermissionItemList(
                        missingPermissions
                );



                // Update UI on main thread
                mainHandler.post(() -> {
                    updateAdapter(permissionItems, showMandatory);
                    hideLoader();
                    isInitialLoad = false;

                    // Optional: Show missing permissions in toast
                    if (missingPermissions != null && !missingPermissions.isEmpty()) {
//                        showMissingPermissionsToast(missingPermissions);
                    }
                });

            } catch (Exception e) {
                // Handle any errors on main thread
                mainHandler.post(() -> {
                    showErrorLoader("Error loading permissions");
                    Toast.makeText(PermissionActivity.this,
                            "Error loading permissions", Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false);
                });
            }
        });


    }

    /**
     * Update adapter with new data on UI thread
     */
    private void updateAdapter(List<PermissionItem> permissionItems, boolean showMandatory) {
        if (adapter == null) {
            adapter = new PermissionAdapter(this, permissionItems, showMandatory);
            recycler.setAdapter(adapter);
            // Show recycler now that we have data
            recycler.setVisibility(View.VISIBLE);
        } else {
            adapter.updateData(permissionItems);
        }

        if (SharedPrefs.getFCMKey(PermissionActivity.this).isEmpty()){
            refreshFCMToken();
        }

    }

    /**
     * Show missing permissions in toast (optional)
     */


    private void showPermissionChoiceDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Skipping Permissions")
                .setMessage("If you're stuck the 'Skip Permanently' or 'Skip Once'")
                .setPositiveButton("Skip Once", (dialog, which) -> {
                    Intent i = new Intent(this, MainActivity.class);
                    i.putExtra("SKIP_ONCE", true);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                })
                .setNegativeButton("Skip Permanently", (dialog, which) -> {
                    SharedPrefs.setPermanentlySkipping(context, true);
                    Intent i = new Intent(this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i);
                })
                .show();
    }

    public void loader_controller(String loader_text_inp, Boolean ShouldBeShown, LinearLayout loader, TextView loader_text) {
        runOnUiThread(() -> {
            if (ShouldBeShown) {
                loader.setVisibility(View.VISIBLE);
                if (!loader_text_inp.isEmpty()) {
                    loader_text.setText(loader_text_inp);
                }
            } else {
                loader.setVisibility(View.GONE);
            }
        });
    }

    private void refreshFCMToken() {
        // Don't show loader here if we're already showing permission loader
        if (!isInitialLoad) {
            loader_controller("Registering Device...", true, loader, loader_text);
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FCM_KEY = task.getResult();
                        if (!isInitialLoad) {
                            loader_controller("Registering Device...", false, loader, loader_text);
                        }
                        showFCMKeyDailog();
                        SharedPrefs.setFCMKey(getApplicationContext(), FCM_KEY);
                    } else {
                        if (!isInitialLoad) {
                            loader_controller("", false, loader, loader_text);
                        }
                        Toast.makeText(this, "Failed to get FCM token", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showFCMKeyDailog() {
        if (isFinishing() || isDestroyed()) return;

        ConstraintLayout relativeLayoutAlert = findViewById(R.id.alertDialog_startup);
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
        if (!showMandatory) return;
        // Refresh permissions asynchronously when returning to activity
        // Don't show full loader for resume - just use subtle refresh
        if (adapter != null) {
//            swipeRefresh.setRefreshing(true);
            loadPermissionsAsync(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up executor to avoid memory leaks
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}