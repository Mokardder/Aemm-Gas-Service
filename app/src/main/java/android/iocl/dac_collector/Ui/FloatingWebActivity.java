package android.iocl.dac_collector.Ui;

import android.os.Bundle;
import android.util.Log;

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

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient()); // 🔥 REQUIRED

        loadUrl(getIntent());

        ImageView closeBtn = findViewById(R.id.btnClose);

        closeBtn.setOnClickListener(view -> {
            finish();
        });
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadUrl(intent);
    }

    private void loadUrl(android.content.Intent intent) {
        String url = intent == null ? null : intent.getStringExtra("url");
        if (url == null || url.trim().isEmpty()) {
            Log.e("FloatingWebActivity", "No URL supplied in notification intent");
            return;
        }

        Log.d("FloatingWebActivity", "Loading URL: " + url);
        webView.loadUrl(url);
    }
}