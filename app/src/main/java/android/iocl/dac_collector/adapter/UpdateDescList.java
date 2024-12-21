package android.iocl.dac_collector.adapter;

import android.content.Context;
import android.iocl.dac_collector.ModelData.appUpdateDesc;
import android.iocl.dac_collector.R;
import android.iocl.dac_collector.Utility.Utility;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class UpdateDescList extends RecyclerView.Adapter<UpdateDescList.ViewHolder>{


    private final List<appUpdateDesc> itemList;

    private final Context context;
    public UpdateDescList(List<appUpdateDesc> itemList, Context context) {
        this.itemList = itemList;
        this.context = context;
    }
    @NonNull
    @Override
    public UpdateDescList.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.update_desc_list, parent, false);
        return new UpdateDescList.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UpdateDescList.ViewHolder holder, int position) {
        appUpdateDesc item = itemList.get(position);

        if (holder.numbering != null && holder.desc != null) {
            holder.numbering.setText(item.getNumbering());
            holder.desc.setText(item.getDesc());
        } else {
            Log.e("Mokardder-->", "TextViews are null!");
        }

    }

    @Override
    public int getItemCount() {
        return itemList.size(); // Return the actual size of the list
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView numbering, desc;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            numbering = itemView.findViewById(R.id.txtNumbering);
            desc = itemView.findViewById(R.id.descString);




        }
    }
}
