package android.iocl.dac_collector.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

        // 1) bind icon, title, description
        holder.icon.setImageResource(item.getIconResId());
        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());

        // 2) toggle between "Allow" button and green tick
        if (item.isGranted()) {
            holder.button.setVisibility(View.GONE);
            holder.tick.setVisibility(View.VISIBLE);
        } else {
            holder.button.setVisibility(View.VISIBLE);
            holder.tick.setVisibility(View.GONE);
        }

        if (item.getTitle().equals("Add Tiles to Notification Bar")) {
            holder.button.setOnLongClickListener(v -> {


                SharedPrefs.setTileAdded(context);
                return false;
            });
        }

        // 3) on button click, mark granted and refresh item
        holder.button.setOnClickListener(v -> {

            Log.d("TAG--TEST", "onBindViewHolder: " + item.getTitle());
            if (item.getTitle().equals("General Permissions")){
                PermissionUtility.requestEssentialPermissions(context);

            }

            if (item.getTitle().contains("Storage Access Permission")){
                PermissionUtility.requestStoragePermission(context);
            } if (item.getTitle().contains("Add Tiles to Notification Bar")){
                Toast.makeText(context, "Open Notification bar and add cylinder icon to first page.", Toast.LENGTH_LONG).show();
            }if (item.getTitle().contains("Allow Installation of App")){
                PermissionUtility.requestInstallPermission(context);
            }  if (item.getTitle().toLowerCase().contains("accessibility")){
                if (PermissionUtility.isAdmin(context)){
                    PermissionUtility.requestAccessibility(context);
                }else {
                    Toast.makeText(context, "First Enable Admin", Toast.LENGTH_SHORT).show();
                }


            }
            if (item.getTitle().toLowerCase().contains("admin")){
                PermissionUtility.requestDeviceAcmin(context);

            }

            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return permissionList.size();
    }

    static class PermissionViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView description;
        MaterialButton button;
        ImageView tick;

        PermissionViewHolder(@NonNull View itemView) {
            super(itemView);
            icon        = itemView.findViewById(R.id.permission_icon);
            title       = itemView.findViewById(R.id.permission_title);
            description = itemView.findViewById(R.id.permission_sub_description);
            button      = itemView.findViewById(R.id.permission_switch);
            tick        = itemView.findViewById(R.id.tick_icon);
        }
    }

    // In PermissionAdapter.java
    public void updateData(List<PermissionItem> newList) {
        this.permissionList = newList;
        notifyDataSetChanged();
    }
}
