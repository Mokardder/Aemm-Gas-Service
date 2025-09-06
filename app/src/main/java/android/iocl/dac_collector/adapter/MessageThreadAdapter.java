package android.iocl.dac_collector.adapter;

import android.content.Context;
import android.iocl.dac_collector.ModelData.Message;
import android.iocl.dac_collector.R;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter that shows messages grouped by date headers.
 */
public class MessageThreadAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // === Listener for long press delete ===
    public interface MessageListener {
        void onLongPressDelete(Message message);
    }

    // === Types ===
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_MESSAGE = 1;

    // === Data model for adapter ===
    public static abstract class ThreadItem { }
    public static class DateHeader extends ThreadItem {
        public final String date;
        public DateHeader(String date) { this.date = date; }
    }
    public static class ChatMessage extends ThreadItem {
        public final Message message;
        public ChatMessage(Message m) { this.message = m; }
    }

    private final Context ctx;
    private final List<ThreadItem> items = new ArrayList<>();
    private final MessageListener listener;
    private ViewGroup mParent;

    public MessageThreadAdapter(Context ctx, List<Message> messages, MessageListener listener) {
        this.ctx = ctx;
        this.listener = listener;
        setMessages(messages);
    }

    @Override
    public int getItemViewType(int position) {
        ThreadItem item = items.get(position);
        if (item instanceof DateHeader) return TYPE_HEADER;
        else return TYPE_MESSAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.mParent = parent;
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(ctx).inflate(R.layout.item_date_header, parent, false);
            return new HeaderVH(v);
        } else {
            View v = LayoutInflater.from(ctx).inflate(R.layout.item_message, parent, false);
            return new MessageVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ThreadItem item = items.get(position);

        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).tvDate.setText(((DateHeader) item).date);

        } else if (holder instanceof MessageVH) {
            MessageVH vh = (MessageVH) holder;
            Message m = ((ChatMessage) item).message;

            int max = (int) (mParent.getResources().getDisplayMetrics().widthPixels * 0.7f);
            vh.tvBodyLeft.setMaxWidth(max);
            vh.tvBodyRight.setMaxWidth(max);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);


            Log.d("MySms", "onBindViewHolder: Right -> " + m.getBody());

            if (m.isOutgoing()) {
                vh.rightBubble.setVisibility(View.VISIBLE);
                vh.leftBubble.setVisibility(View.GONE);
                vh.tvBodyRight.setText(m.getBody());
                vh.tvTimeRight.setText(sdf.format(m.getDate()));
                vh.tvTimeRight.setVisibility(View.GONE);

                vh.rightBubble.setOnClickListener(v -> {

                    Toast.makeText(ctx, "" + sdf.format(m.getDate()), Toast.LENGTH_SHORT).show();
                    vh.tvTimeRight.setVisibility(
                            vh.tvTimeRight.getVisibility() == View.GONE ? View.VISIBLE : View.GONE
                    );
                });
            } else {
                vh.leftBubble.setVisibility(View.VISIBLE);
                vh.rightBubble.setVisibility(View.GONE);
                vh.tvBodyLeft.setText(m.getBody());
                vh.tvTimeLeft.setText(sdf.format(m.getDate()));
                vh.tvTimeLeft.setVisibility(View.GONE);

                vh.leftBubble.setOnClickListener(v -> {
                    vh.tvTimeLeft.setVisibility(
                            vh.tvTimeLeft.getVisibility() == View.GONE ? View.VISIBLE : View.GONE
                    );
                });

            }

            vh.itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onLongPressDelete(m);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ====== Public methods ======

    /** Load messages and group them by date */
    public void setMessages(List<Message> messages) {
        items.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        String lastDate = "";
        for (Message m : messages) {
            String msgDate = sdf.format(m.getDate());
            if (!msgDate.equals(lastDate)) {
                items.add(new DateHeader(msgDate));
                lastDate = msgDate;
            }
            items.add(new ChatMessage(m));
        }
        notifyDataSetChanged();
    }

    /** Add a single message (handles header if needed) */
    public void addMessage(Message message) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        String msgDate = sdf.format(message.getDate());

        if (items.isEmpty() || !(items.get(items.size() - 1) instanceof DateHeader) ||
                !((DateHeader) items.get(items.size() - 1)).date.equals(msgDate)) {
            items.add(new DateHeader(msgDate));
            notifyItemInserted(items.size() - 1);
        }

        items.add(new ChatMessage(message));
        notifyItemInserted(items.size() - 1);
    }

    // ====== ViewHolders ======
    static class MessageVH extends RecyclerView.ViewHolder {
        MaterialCardView leftBubble, rightBubble;
        TextView tvBodyLeft, tvTimeLeft, tvBodyRight, tvTimeRight;

        MessageVH(@NonNull View itemView) {
            super(itemView);
            leftBubble = itemView.findViewById(R.id.leftBubble);
            rightBubble = itemView.findViewById(R.id.rightBubble);
            tvBodyLeft = itemView.findViewById(R.id.tvBodyLeft);
            tvTimeLeft = itemView.findViewById(R.id.tvTimeLeft);
            tvBodyRight = itemView.findViewById(R.id.tvBodyRight);
            tvTimeRight = itemView.findViewById(R.id.tvTimeRight);
        }
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvDate;
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDateHeader);
        }
    }
}
