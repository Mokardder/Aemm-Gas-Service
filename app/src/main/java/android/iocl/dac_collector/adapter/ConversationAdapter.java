package android.iocl.dac_collector.adapter;

import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.ModelData.Conversation;
import android.iocl.dac_collector.R;
import android.iocl.sms_handler_8_0_below.SMSThreadActivity;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

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
    private final List<Object> displayList = new ArrayList<>(); // holds Conversation or AD_MARKER
    private final ConversationListener listener;
    private static final Object AD_MARKER = new Object();

    public ConversationAdapter(Context ctx, List<Conversation> data, ConversationListener listener) {
        this.ctx = ctx;
        this.listener = listener;
        setData(data);
    }

    public void setData(List<Conversation> data) {
        original.clear();
        if (data != null) original.addAll(data);
        rebuildDisplayList(); // build displayList with ads
        notifyDataSetChanged();
    }

    public void filter(String q) {
        // filter original into a temp list then rebuild
        List<Conversation> filtered = new ArrayList<>();
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
        // replace original view data with filtered view (but keep original master untouched)
        // to keep behavior same as before, just set original to filtered for now:
        original.clear();
        original.addAll(filtered);
        rebuildDisplayList();
        notifyDataSetChanged();
    }

    private void rebuildDisplayList() {
        displayList.clear();
        if (original.isEmpty()) return;
        int count = 0;
        for (int i = 0; i < original.size(); i++) {
            // insert conversation
            displayList.add(original.get(i));
            count++;
            // after AD_INTERVAL real items, insert an ad marker (but don't append an ad after the very last item if you don't want it)
            if (count % AD_INTERVAL == 0 && i != original.size() - 1) {
                displayList.add(AD_MARKER);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Object o = displayList.get(position);
        return o == AD_MARKER ? VIEW_TYPE_AD : VIEW_TYPE_CONVERSATION;
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
            Conversation c = (Conversation) displayList.get(position);
            VH vh = (VH) holder;

            vh.tvName.setText(c.getContactName() != null ? c.getContactName() : c.getAddress());
            String lastMsg = c.getLastMessage() != null ? c.getLastMessage() : "";
            if (c.getLastMessageType() == Telephony.Sms.MESSAGE_TYPE_SENT) {
                lastMsg = "You: " + lastMsg;
            }
            vh.tvSnippet.setText(lastMsg);

            if (c.getPhotoUri() != null) {
                // setImageURI can be problematic for remote loading; keep it but guard
                try {
                    vh.ivAvatar.setImageURI(Uri.parse(c.getPhotoUri()));
                } catch (Exception ex) {
                    vh.ivAvatar.setImageResource(R.drawable.default_user);
                }
            } else {
                vh.ivAvatar.setImageResource(R.drawable.default_user);
            }

            vh.itemView.setOnClickListener(v -> {
                Intent in = new Intent(ctx, SMSThreadActivity.class);
                // Pass a normalized address so thread activity can match the same canonical key
                String addr = c.getAddress();
                String normalized = addr != null ? PhoneNumberUtils.normalizeNumber(addr) : addr;
                if (normalized == null || normalized.isEmpty()) normalized = addr;
                in.putExtra(SMSThreadActivity.EXTRA_ADDRESS, normalized);
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
        return displayList.size();
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
            // ensure adView has correct size if not configured in layout

        }
    }
}
