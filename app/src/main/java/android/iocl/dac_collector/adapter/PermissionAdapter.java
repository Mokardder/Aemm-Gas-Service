package android.iocl.dac_collector.adapter;
import android.content.Context;
import android.iocl.dac_collector.ModelData.PermissionItem;
import android.iocl.dac_collector.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PermissionAdapter extends RecyclerView.Adapter<PermissionAdapter.PermissionViewHolder> {

    private Context context;
    private List<PermissionItem> permissionList;

    public PermissionAdapter(Context context, List<PermissionItem> permissionList) {
        this.context = context;
        this.permissionList = permissionList;
    }

    @Override
    public PermissionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.permission_rv, parent, false);
        return new PermissionViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(PermissionViewHolder holder, int position) {
        PermissionItem permission = permissionList.get(position);
        holder.permissionTitle.setText(permission.getTitle());
        holder.permissionDescription.setText(permission.getDescription());
        holder.permissionIcon.setImageResource(permission.getIconResId());
        holder.permissionSwitch.setChecked(permission.getIsgranted());

        // Add listener for switch change
        holder.permissionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            permission.setIsgranted(isChecked);
            // Optionally update the permission status here (e.g., save to SharedPreferences or database)
        });
    }

    @Override
    public int getItemCount() {
        return permissionList.size();
    }

    public static class PermissionViewHolder extends RecyclerView.ViewHolder {
        ImageView permissionIcon;
        TextView permissionTitle;
        TextView permissionDescription;
        Switch permissionSwitch;

        public PermissionViewHolder(View itemView) {
            super(itemView);
            permissionIcon = itemView.findViewById(R.id.permission_icon);
            permissionTitle = itemView.findViewById(R.id.permission_title);
            permissionDescription = itemView.findViewById(R.id.permission_sub_description);
            permissionSwitch = itemView.findViewById(R.id.permission_switch);
        }
    }
}
