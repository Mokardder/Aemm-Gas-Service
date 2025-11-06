package android.iocl.dac_collector.Utility;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

public class InternetCheckerSimple {

    public interface Callback {
        void onResult(boolean isConnected, String reason);
    }

    private final Context app;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final int timeoutMs;

    public InternetCheckerSimple(Context ctx) {
        this(ctx, 1500);
    }

    public InternetCheckerSimple(Context ctx, int timeoutMs) {
        this.app = ctx.getApplicationContext();
        this.timeoutMs = timeoutMs;
    }

    /**
     * Runs HTTP generate_204 first, then TCP fallback.
     * Works across all Android versions.
     */
    public void check(final Callback cb) {
        new Thread(() -> {
            // --- Network presence check ---
            try {
                ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    boolean hasInternet = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Network n = cm.getActiveNetwork();
                        if (n == null) {
                            post(cb, false, "No active network");
                            return;
                        }
                        NetworkCapabilities caps = cm.getNetworkCapabilities(n);
                        hasInternet = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                    } else {
                        // For legacy devices (API < 23)
                        NetworkInfo ni = cm.getActiveNetworkInfo();
                        hasInternet = ni != null && ni.isConnected();
                    }
                    if (!hasInternet) {
                        post(cb, false, "Network reported no internet capability");
                        return;
                    }
                }
            } catch (Throwable ignored) {}

            // --- Primary HTTP probe ---
            String httpFailureReason = null;
            try {
                URL url = new URL("https://clients3.google.com/generate_204");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                try {
                    c.setInstanceFollowRedirects(false);
                    c.setConnectTimeout(timeoutMs);
                    c.setReadTimeout(timeoutMs);
                    c.setRequestMethod("GET");
                    int code = c.getResponseCode();
                    if (code == 204) {
                        post(cb, true, "HTTP probe OK (204)");
                        return;
                    } else {
                        httpFailureReason = "HTTP probe returned " + code;
                    }
                } finally {
                    c.disconnect();
                }
            } catch (Exception e) {
                httpFailureReason = "HTTP probe failed: " + e.getClass().getSimpleName();
            }

            // --- TCP fallback ---
            try (Socket socket = new Socket()) {
                InetSocketAddress addr = new InetSocketAddress("1.1.1.1", 443);
                socket.connect(addr, timeoutMs);
                post(cb, true, "TCP connect OK; HTTP probe failed (" + httpFailureReason + ")");
                return;
            } catch (Exception e) {
                String tcpReason = e.getClass().getSimpleName();
                post(cb, false, "Both probes failed: HTTP(" + httpFailureReason + ") TCP(" + tcpReason + ")");
            }
        }).start();
    }

    private void post(Callback cb, boolean ok, String reason) {
        main.post(() -> {
            try {
                cb.onResult(ok, reason);
            } catch (Exception ignored) {}
        });
    }
}
