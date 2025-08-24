package android.iocl.dac_collector.Ui;

import android.iocl.dac_collector.ModelData.BankStatementItem;
import android.iocl.dac_collector.ModelData.SubsidyRecord;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.iocl.dac_collector.Utility.Utility;
import android.iocl.dac_collector.adapter.StatementAdapter;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.iocl.dac_collector.R;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BankStatementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private boolean showBankStatement = true; // Your condition variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_statement);

        recyclerView = findViewById(R.id.recycler_view);

        // Check if we should show the statement
        if(showBankStatement) {
            setupRecyclerView();
        } else {
            // Hide or show alternative view
            recyclerView.setVisibility(View.GONE);
            // Show alternative view if needed
        }
    }

    private void setupRecyclerView() {

        String enc = SharedPrefs.getSubsidyDetails(BankStatementActivity.this);

        Object result = Utility.decodeApiResponse(enc, SubsidyRecord.class);

        List<BankStatementItem> items = new ArrayList<>();

        if (result instanceof List) {
            List<SubsidyRecord> orders = (List<SubsidyRecord>) result;

            for (SubsidyRecord order : orders) {
                // Map SubsidyRecord to BankStatementItem
                String bookDate = order.getOrderDate(); // or deliveryDate
                String status = order.getSubsidyStatus();
                String amount = "₹" + order.getSubsidyAmount();
                String Dos = order.getBankDOS();
                String account = order.getBankAccountNumber();

                items.add(new BankStatementItem(bookDate, status, amount, Dos, account));
            }

        } else if (result instanceof SubsidyRecord) {
            SubsidyRecord order = (SubsidyRecord) result;

            String bookDate = order.getOrderDate(); // or deliveryDate
            String status = order.getSubsidyStatus();
            String amount = "₹" + order.getSubsidyAmount();
            String Dos = order.getBankDOS();
            String account = order.getBankAccountNumber();

            items.add(new BankStatementItem(bookDate, status, amount, Dos, account));

        } else {
            Log.e("Decode", "Failed to decode response");
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new StatementAdapter(items));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

}