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
            ProbeResult result = probeInternet();
            post(cb, result.isConnected, result.reason);
        }).start();
    }

    public boolean checkNow() {
        return probeInternet().isConnected;
    }

    private ProbeResult probeInternet() {
        try {
            ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                boolean hasInternet;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Network network = cm.getActiveNetwork();
                    if (network == null) {
                        return new ProbeResult(false, "No active network");
                    }
                    NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                    hasInternet = capabilities != null
                            && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                } else {
                    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                    hasInternet = networkInfo != null && networkInfo.isConnected();
                }
                if (!hasInternet) {
                    return new ProbeResult(false, "Network reported no internet capability");
                }
            }
        } catch (RuntimeException ignored) {
        }

        String httpFailureReason;
        try {
            URL url = new URL("https://clients3.google.com/generate_204");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(timeoutMs);
                connection.setReadTimeout(timeoutMs);
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                if (responseCode == 204) {
                    return new ProbeResult(true, "HTTP probe OK (204)");
                }
                httpFailureReason = "HTTP probe returned " + responseCode;
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            httpFailureReason = "HTTP probe failed: " + e.getClass().getSimpleName();
        }

        try (Socket socket = new Socket()) {
            InetSocketAddress address = new InetSocketAddress("1.1.1.1", 443);
            socket.connect(address, timeoutMs);
            return new ProbeResult(
                    true,
                    "TCP connect OK; HTTP probe failed (" + httpFailureReason + ")"
            );
        } catch (Exception e) {
            String tcpReason = e.getClass().getSimpleName();
            return new ProbeResult(
                    false,
                    "Both probes failed: HTTP(" + httpFailureReason + ") TCP(" + tcpReason + ")"
            );
        }
    }

    private void post(Callback cb, boolean ok, String reason) {
        main.post(() -> {
            try {
                cb.onResult(ok, reason);
            } catch (Exception ignored) {}
        });
    }

    private static final class ProbeResult {
        private final boolean isConnected;
        private final String reason;

        private ProbeResult(boolean isConnected, String reason) {
            this.isConnected = isConnected;
            this.reason = reason;
        }
    }
}
