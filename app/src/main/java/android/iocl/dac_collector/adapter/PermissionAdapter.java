package android.iocl.dac_collector.adapter;

import android.app.Activity;
import android.content.Intent;
import android.iocl.dac_collector.ModelData.PermissionItem;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Ui.MainActivity;
import android.iocl.dac_collector.Utility.PermissionUtility;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.List;

public class PermissionAdapter
        extends RecyclerView.Adapter<PermissionAdapter.PermissionViewHolder> {

    private final Activity context;
    private List<PermissionItem> permissionList;

    public PermissionAdapter(Activity context, List<PermissionItem> permissionList) {
        this.context = context;
        this.permissionList = permissionList;
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
                SharedPrefs.setTileAdded(context);
                item.setGranted(true);
                // Immediately update UI and re-check completion
                notifyItemChanged(position);

                return true;
            });
        }

        // regular click actions
        holder.button.setOnClickListener(v -> {
            Log.d("PermsActivity", "onBindViewHolder: click at pos=" + position + " count=" + getItemCount());

            String title = item.getTitle();
            if (title.equals("General Permissions")) {
                PermissionUtility.requestEssentialPermissions(context);
            } else if (title.contains("Storage Access Permission")) {
                PermissionUtility.requestStoragePermission(context);
            } else if (title.contains("Add Tiles to Notification Bar")) {
                // Make tile-activation explicit and refresh state
                if (!SharedPrefs.isTileAdded(context)) {
                    Toast.makeText(context,
                            "Open Notification bar and add cylinder icon to first page.",
                            Toast.LENGTH_LONG).show();

                    // Persist the tile state and update adapter immediately
                    SharedPrefs.setTileAdded(context);
                    item.setGranted(true);
                    notifyItemChanged(position);

                } else {
                    // If SharedPrefs already says tile added, mark item granted and refresh
                    item.setGranted(true);
                    notifyItemChanged(position);

                }

            } else if (title.contains("Allow Installation of App")) {
                PermissionUtility.requestInstallPermission(context);
            } else if (title.toLowerCase().contains("accessibility")) {
                if (PermissionUtility.isAdmin(context)) {
                    PermissionUtility.requestAccessibility(context);
                } else {
                    Toast.makeText(context, "First Enable Admin", Toast.LENGTH_SHORT).show();
                }
            } else if (title.toLowerCase().contains("admin")) {
                PermissionUtility.requestDeviceAcmin(context);
            }

            refreshAndCheckCompletion();
            // Note: do NOT call refreshAndCheckCompletion() blindly for other cases because user flow may be async.
        });
    }

    @Override
    public int getItemCount() {
        return permissionList.size();
    }

    /**
     * Reloads the permission list, updates the adapter, and finishes the activity
     * if no more permissions are needed.
     */
    private void refreshAndCheckCompletion() {

        Log.d("PermsActivity", "Checking if permissions are granted ");
        // Always run UI updates on main thread
        context.runOnUiThread(() -> {
            // Re-fetch the latest permission state
            List<String> missing = PermissionUtility.getMissingPermissions(context);
            String[] missingArray = missing.toArray(new String[0]);


            List<PermissionItem> permissionItems = PermissionUtility.buildPermissionItemList(
                    Arrays.asList(missingArray)
            );
            this.permissionList = permissionItems;
            notifyDataSetChanged();

            // Check if all permissions are granted or tile added
            boolean allDone = true;
            for (PermissionItem pi : permissionList) {
                if (!pi.isGranted()) {
                    allDone = false;
                    break;
                }
            }

            // If permission list is empty, also consider tile flag (some devices don't include tile in missing list)
            if (permissionList.isEmpty()) {
                // If tile addition is required by your flow, check SharedPrefs
                if (!SharedPrefs.isTileAdded(context)) {
                    allDone = false;
                }
            }

            if (allDone) {
                Log.d("PermsActivity", "Everything good ");
                // Navigate to MainActivity
                Intent intent = new Intent(context, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);

                // Finish the current PermissionActivity
                context.finish();
            }

        });
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

    /**
     * Public update if external data change
     */
    public void updateData(List<PermissionItem> newList) {
        this.permissionList = newList;
        notifyDataSetChanged();
    }
}
