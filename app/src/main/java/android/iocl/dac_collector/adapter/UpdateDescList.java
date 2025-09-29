package android.iocl.dac_collector.adapter;

import android.content.Context;
import android.iocl.dac_collector.ModelData.appUpdateDesc;
import android.iocl.dac_collector.R;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

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



        int colorGreen  = ContextCompat.getColor(holder.itemView.getContext(), R.color.green);
        int colorRed  = ContextCompat.getColor(holder.itemView.getContext(), R.color.red);
        String desc = item.getDesc();

        String index = item.getNumbering() + ". ";
        if (holder.desc != null) {
            holder.desc.setText(index + item.getDesc());
            if (desc.contains("+")){
                holder.desc.setTextColor(colorGreen);
            }else if (desc.contains("-")){
                holder.desc.setTextColor(colorRed);
            }
        } else {
            Log.e("Mokardder-->", "TextViews are null!");
        }

    }

    @Override
    public int getItemCount() {
        return itemList.size(); // Return the actual size of the list
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView desc;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            desc = itemView.findViewById(R.id.txtNumbering);





        }
    }
}
