package android.iocl.dac_collector.Ui;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.iocl.dac_collector.R;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

public class FloatingWebActivity extends AppCompatActivity {

    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_floating_web);

        webView = findViewById(R.id.webView);

        String url = getIntent().getStringExtra("url");

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient()); // 🔥 REQUIRED

        if (url != null && !url.isEmpty()) {
            webView.loadUrl(url);
        }

        ImageView closeBtn = findViewById(R.id.btnClose);

        closeBtn.setOnClickListener(view -> {
            finish();
        });
    }
}