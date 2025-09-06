package android.iocl.dac_collector.adapter;

import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.BuildConfig;
import android.iocl.dac_collector.ModelData.Conversation;
import android.iocl.dac_collector.R;
import android.iocl.sms_handler_8_0_below.SMSThreadActivity;
import android.net.Uri;
import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdSize;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_CONVERSATION = 0;
    private static final int VIEW_TYPE_AD = 1;
    private static final int AD_INTERVAL = 5; // Show ad every 5 items

    public interface ConversationListener {
        void onDeleteConversation(Conversation conv);
    }

    private final Context ctx;
    private final List<Conversation> original = new ArrayList<>();
    private final List<Conversation> filtered = new ArrayList<>();
    private final ConversationListener listener;

    public ConversationAdapter(Context ctx, List<Conversation> data, ConversationListener listener) {
        this.ctx = ctx;
        this.listener = listener;
        setData(data);
    }

    public void setData(List<Conversation> data) {
        original.clear();
        if (data != null) original.addAll(data);
        filtered.clear();
        filtered.addAll(original);
        notifyDataSetChanged();
    }

    public void filter(String q) {
        filtered.clear();
        if (q == null || q.trim().isEmpty()) {
            filtered.addAll(original);
        } else {
            String low = q.toLowerCase();
            for (Conversation c : original) {
                String contact = c.getContactName() != null ? c.getContactName().toLowerCase() : "";
                String addr = c.getAddress() != null ? c.getAddress().toLowerCase() : "";
                String last = c.getLastMessage() != null ? c.getLastMessage().toLowerCase() : "";
                if (contact.contains(low) || addr.contains(low) || last.contains(low)) filtered.add(c);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if ((position + 1) % AD_INTERVAL == 0) return VIEW_TYPE_AD;
        return VIEW_TYPE_CONVERSATION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_AD) {
            View v = LayoutInflater.from(ctx).inflate(R.layout.item_ad_for_list, parent, false);
            return new AdVH(v);
        } else {
            View v = LayoutInflater.from(ctx).inflate(R.layout.item_conversation, parent, false);
            return new VH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_AD) {
            AdVH adHolder = (AdVH) holder;
            AdRequest adRequest = new AdRequest.Builder().build();
            adHolder.adView.loadAd(adRequest);
        } else {
            int dataPos = position - (position / AD_INTERVAL);
            if (dataPos >= filtered.size()) return;

            Conversation c = filtered.get(dataPos);
            VH vh = (VH) holder;

            vh.tvName.setText(c.getContactName() != null ? c.getContactName() : c.getAddress());
            String lastMsg = c.getLastMessage() != null ? c.getLastMessage() : "";
            if (c.getLastMessageType() == Telephony.Sms.MESSAGE_TYPE_SENT) {
                lastMsg = "You: " + lastMsg;
            }
            vh.tvSnippet.setText(lastMsg);

            if (c.getPhotoUri() != null) {
                vh.ivAvatar.setImageURI(Uri.parse(c.getPhotoUri()));
            } else {
                vh.ivAvatar.setImageResource(R.drawable.default_user);
            }

            vh.itemView.setOnClickListener(v -> {
                Intent in = new Intent(ctx, SMSThreadActivity.class);
                in.putExtra(SMSThreadActivity.EXTRA_ADDRESS, c.getAddress());
                in.putExtra(SMSThreadActivity.EXTRA_NAME, c.getContactName());
                ctx.startActivity(in);
            });

            vh.itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onDeleteConversation(c);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        int extraAds = filtered.size() / AD_INTERVAL;
        return filtered.size() + extraAds;
    }

    // Conversation ViewHolder
    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvSnippet;
        ImageView ivAvatar;

        public VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.txtContactName);
            tvSnippet = itemView.findViewById(R.id.txtLastMessage);
            ivAvatar = itemView.findViewById(R.id.imgAvatar);
        }
    }

    // Ad ViewHolder
    static class AdVH extends RecyclerView.ViewHolder {
        AdView adView;

        public AdVH(@NonNull View itemView) {
            super(itemView);
            adView = itemView.findViewById(R.id.adView);
        }
    }
}
