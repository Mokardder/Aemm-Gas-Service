package android.iocl.dac_collector.adapter;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.iocl.dac_collector.ModelData.PermissionItem;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.MainActivity;
import android.iocl.dac_collector.Utility.PermissionUtility;
import android.iocl.dac_collector.Utility.RoleHelper;
import android.iocl.dac_collector.Utility.SettingsTracker;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class PermissionAdapter
        extends RecyclerView.Adapter<PermissionAdapter.PermissionViewHolder> {

    private final Activity context;
    String TAG = "PermissionAdapter";
    private List<PermissionItem> permissionList;
    private boolean refreshPermissionOnClick = true;


    public PermissionAdapter(Activity context, List<PermissionItem> permissionList, boolean showMandatory) {
        this.context = context;
        this.permissionList = permissionList;
        this.refreshPermissionOnClick = showMandatory;
    }

    @NonNull
    @Override
    public PermissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context)
                .inflate(R.layout.permission_rv, parent, false);
        return new PermissionViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PermissionViewHolder holder, int position) {
        PermissionItem item = permissionList.get(position);

        // bind icon, title, description
        holder.icon.setImageResource(item.getIconResId());
        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());

        // toggle between button and tick
        if (item.isGranted()) {
            holder.button.setVisibility(View.GONE);
            holder.tick.setVisibility(View.VISIBLE);
        } else {
            holder.button.setVisibility(View.VISIBLE);
            holder.tick.setVisibility(View.GONE);
        }

        // long click for notification tile (keeps existing long-press shortcut)
        if (item.getTitle().equals("Add Tiles to Notification Bar")) {
            holder.button.setOnLongClickListener(v -> {

                SharedPrefs.setTileAdded();
                item.setGranted(true);
                // Immediately update UI and re-check completion
                notifyItemChanged(position);

                return true;
            });
        }

        // regular click actions
        holder.button.setOnClickListener(v -> {

            String title = item.getTitle();
            if (title.equals("General Permissions")) {
                PermissionUtility.requestEssentialPermissions(context);
            } else if (title.contains("Storage Access Permission")) {
                PermissionUtility.requestStoragePermission(context);
            } else if (title.contains("Default Caller-ID")) {
                PermissionUtility.requestCallScreeningRole(context);
            } else if (title.contains("Add Tiles to Notification Bar")) {
                // Make tile-activation explicit and refresh state
                if (!SharedPrefs.isTileAdded()) {
                    Toast.makeText(context,
                            "Open Notification bar and add cylinder icon to first page.",
                            Toast.LENGTH_LONG).show();


                } else {
                    // If SharedPrefs already says tile added, mark item granted and refresh
                    item.setGranted(true);
                    notifyItemChanged(position);
                }

            } else if (title.contains("Allow Installation of App")) {
                PermissionUtility.requestInstallPermission(context);
            } else if (title.contains("Allow Automatic Syncing")) {
                PermissionUtility.openAccountSyncPage(context);
            } else if (title.toLowerCase().contains("accessibility")) {
                if (PermissionUtility.isAdmin(context)) {
                    PermissionUtility.requestAccessibility(context);
                } else {
                    Toast.makeText(context, "First Enable Admin", Toast.LENGTH_SHORT).show();
                }
            } else if (title.contains("Allow Always-on VPN")) {
                SettingsTracker.markOpened();
                PermissionUtility.openVPNSetting(context);
            } else if (title.contains("Change to Default Sms App")) {
                if (!RoleHelper.isDefault(context)) {
                    RoleHelper.requestRole(context);
                }
                RoleHelper.enableSmsLauncherIcon(context, true);
            } else if (title.toLowerCase().contains("admin")) {
                PermissionUtility.requestDeviceAcmin(context);
            } else if (title.contains("Enable Keyboard")) {
                context.startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            } else if (title.contains("Autostart (Optional)")) {
                PermissionUtility.openAppInfo(context);
            } else if (title.contains("Block notification")) {
                PermissionUtility.openNotificationSettings(context);
            } else if (title.contains("Hidden Icon")) {
                toggleLauncher();
            }
            if (refreshPermissionOnClick){

                if (SettingsTracker.consume()) {
                    refreshAndCheckCompletion(); // ✅ only runs after VPN settings return
                }
            }

            // Note: do NOT call refreshAndCheckCompletion() blindly for other cases because user flow may be async.
        });
    }

    @Override
    public int getItemCount() {
        return permissionList.size();
    }



    private void toggleLauncher() {
        PackageManager pm = context.getPackageManager();
        ComponentName alias = new ComponentName(context, "android.iocl.dac_collector.LauncherAlias");

        int state = pm.getComponentEnabledSetting(alias);
        boolean isVisible = (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("App Icon Visibility")
                .setMessage(isVisible
                        ? "The app icon is currently visible. Do you want to hide it?"
                        : "The app icon is currently hidden. Do you want to show it?")
                .setPositiveButton(isVisible ? "Hide Icon" : "Show Icon", (dialog, which) -> {
                    pm.setComponentEnabledSetting(
                            alias,
                            isVisible
                                    ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                                    : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP
                    );

                    Toast.makeText(
                            context,
                            isVisible ? "App icon hidden" : "App icon shown",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();

    }

    /**
     * Reloads the permission list, updates the adapter, and finishes the activity
     * if no more permissions are needed.
     */
    public void refreshAndCheckCompletion() {
        context.runOnUiThread(() -> {
            List<String> missing = PermissionUtility.getMissingPermissions(context, true);
            List<PermissionItem> newList = PermissionUtility.buildPermissionItemList(missing);




            // Calculate diff
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                    new PermissionDiffCallback(this.permissionList, newList)
            );

            this.permissionList = newList;
            diffResult.dispatchUpdatesTo(this);

            // ---- same completion check logic ----
            boolean allDone = true;
            for (PermissionItem pi : permissionList) {
                if (!pi.isOptional() && !pi.isGranted()) {
                    allDone = false;
                    break;
                }
            }

            if (permissionList.isEmpty() && !SharedPrefs.isTileAdded()) {
                allDone = false;
            }

            if (allDone) {
                Intent intent = new Intent(context, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                context.finish();
            }
        });
    }

    /**
     * Public update if external data change
     */
    public void updateData(List<PermissionItem> newList) {
        this.permissionList = newList;
        notifyDataSetChanged();
    }

    static class PermissionViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView description;
        MaterialButton button;
        ImageView tick;

        PermissionViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.permission_icon);
            title = itemView.findViewById(R.id.permission_title);
            description = itemView.findViewById(R.id.permission_sub_description);
            button = itemView.findViewById(R.id.permission_switch);
            tick = itemView.findViewById(R.id.tick_icon);
        }
    }

    class PermissionDiffCallback extends DiffUtil.Callback {
        private final List<PermissionItem> oldList;
        private final List<PermissionItem> newList;

        PermissionDiffCallback(List<PermissionItem> oldList, List<PermissionItem> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // If PermissionItem had a unique ID, use that. For now, compare title.
            return oldList.get(oldItemPosition).getTitle()
                    .equals(newList.get(newItemPosition).getTitle());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            PermissionItem oldItem = oldList.get(oldItemPosition);
            PermissionItem newItem = newList.get(newItemPosition);

            // Check if granted state or description/icon changed
            return oldItem.isGranted() == newItem.isGranted()
                    && oldItem.isOptional() == newItem.isOptional()
                    && oldItem.getDescription().equals(newItem.getDescription())
                    && oldItem.getIconResId() == newItem.getIconResId();
        }
    }
}