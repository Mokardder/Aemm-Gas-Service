package android.iocl.dac_collector.adapter;

import android.app.Activity;
import android.iocl.dac_collector.ModelData.PermissionItem;
import android.iocl.dac_collector.R;
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

        if (getItemCount() == 0){
            context.finish();
        }

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

        // long click for notification tile
        if (item.getTitle().equals("Add Tiles to Notification Bar")) {
            holder.button.setOnLongClickListener(v -> {
                SharedPrefs.setTileAdded(context);
                refreshAndCheckCompletion();
                return true;
            });
        }

        holder.button.setOnClickListener(v -> {
            Log.d("PermsActivity", "onBindViewHolder: " + getItemCount());
            if (getItemCount() == 0){
                Toast.makeText(context, "All Permission Granted", Toast.LENGTH_SHORT).show();
            }
            String title = item.getTitle();
            if (title.equals("General Permissions")) {
                PermissionUtility.requestEssentialPermissions(context);
            } else if (title.contains("Storage Access Permission")) {
                PermissionUtility.requestStoragePermission(context);
            } else if (title.contains("Add Tiles to Notification Bar")) {
                if (!SharedPrefs.isTileAdded(context)) {
                    Toast.makeText(context,
                            "Open Notification bar and add cylinder icon to first page.",
                            Toast.LENGTH_LONG).show();
                    item.setGranted(true);
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

//            refreshAndCheckCompletion();
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
        if (allDone) {
            context.finish();
        }
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