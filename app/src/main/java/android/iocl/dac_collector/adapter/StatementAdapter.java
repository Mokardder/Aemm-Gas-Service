package android.iocl.dac_collector.adapter;

import android.iocl.dac_collector.ModelData.BankStatementItem;
import android.iocl.dac_collector.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StatementAdapter extends RecyclerView.Adapter<StatementAdapter.ViewHolder> {

    private List<BankStatementItem> items;

    public StatementAdapter(List<BankStatementItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_statement_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BankStatementItem item = items.get(position);
        holder.bookDate.setText(item.getBookDate());
        holder.subsidyStatus.setText(item.getSubsidyStatus());
        holder.subsidyAmount.setText(item.getSubsidyAmount());
        holder.sentToBank.setText(item.getSentToBank());
        holder.bankAccountNo.setText(item.getBankAccountNo());

        // Alternate row colors
        int bgColor = position % 2 == 0 ?
                ContextCompat.getColor(holder.itemView.getContext(), R.color.row_even) :
                ContextCompat.getColor(holder.itemView.getContext(), R.color.row_odd);
        holder.itemView.setBackgroundColor(bgColor);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView bookDate, subsidyStatus, subsidyAmount, sentToBank, bankAccountNo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            bookDate = itemView.findViewById(R.id.tv_book_date);
            subsidyStatus = itemView.findViewById(R.id.tv_subsidy_status);
            subsidyAmount = itemView.findViewById(R.id.tv_subsidy_amount);
            sentToBank = itemView.findViewById(R.id.tv_sent_to_bank);
            bankAccountNo = itemView.findViewById(R.id.tv_bank_account);
        }
    }
}