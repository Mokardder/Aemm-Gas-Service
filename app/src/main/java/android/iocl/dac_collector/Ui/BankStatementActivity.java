package android.iocl.dac_collector.Ui;

import android.iocl.dac_collector.ModelData.BankStatementItem;
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
        // Sample data - replace with your actual data
        List<BankStatementItem> items = new ArrayList<>();
        items.add(new BankStatementItem("2023-07-15", "HOLD", "₹319.57", "2023-07-20", "**** 4567"));
        items.add(new BankStatementItem("2023-07-10", "Start", "₹319.57", "2023-07-15", "**** 8910"));
        items.add(new BankStatementItem("2023-07-05", "Start", "₹319.57", "2023-07-10", "**** 1122"));
        items.add(new BankStatementItem("2023-07-01", "Start", "₹319.57", "2023-07-05", "**** 3344"));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new StatementAdapter(items));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }
}