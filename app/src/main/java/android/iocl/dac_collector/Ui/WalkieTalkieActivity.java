package android.iocl.dac_collector.Ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.IBinder;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.iocl.dac_collector.Services.WalkieTalkieService;
import android.iocl.dac_collector.Services.WebRtcWalkieTalkieClient;

public class WalkieTalkieActivity extends AppCompatActivity
        implements WebRtcWalkieTalkieClient.Listener {

    private static final int REQUEST_RECORD_AUDIO = 7001;
    public static final String EXTRA_DASHBOARD_CALL_REQUEST =
            "android.iocl.dac_collector.walkie.DASHBOARD_CALL_REQUEST";

    private TextView statusText;
    private TextView roomCodeText;
    private Button createButton;
    private Button talkButton;
    private Button endButton;
    private WalkieTalkieService callService;
    private boolean serviceBound;
    private boolean dashboardCallRequested;
    private Runnable pendingAudioAction;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            WalkieTalkieService.LocalBinder localBinder =
                    (WalkieTalkieService.LocalBinder) binder;
            callService = localBinder.getService();
            callService.setListener(WalkieTalkieActivity.this);
            boolean callActive = callService.isCallActive();
            setCallControlsEnabled(!callActive);
            endButton.setVisibility(callActive ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            callService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createContentView();
        updateProfileAvailability();
        showDashboardCallRequestIfPresent(getIntent());
    }

    private void createContentView() {
        int padding = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("Walkie-talkie");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, matchWrap());

        TextView instructions = new TextView(this);
        instructions.setText("Tap below to enable a dashboard-controlled call. When FCM_sender accepts, audio transmission starts automatically. Use the dashboard to mute, resume, or end the call. The ongoing phone notification stays visible while the microphone is active.");
        instructions.setTextSize(16);
        instructions.setPadding(0, dp(12), 0, dp(16));
        root.addView(instructions, matchWrap());

        createButton = new Button(this);
        createButton.setText("Enable dashboard call");
        root.addView(createButton, matchWrap());
        createButton.setOnClickListener(view -> withAudioPermission(this::createRoom));

        roomCodeText = new TextView(this);
        roomCodeText.setTextSize(24);
        roomCodeText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        roomCodeText.setGravity(Gravity.CENTER);
        roomCodeText.setPadding(0, dp(12), 0, dp(4));
        root.addView(roomCodeText, matchWrap());

        statusText = new TextView(this);
        statusText.setTextSize(16);
        statusText.setPadding(0, dp(18), 0, dp(12));
        root.addView(statusText, matchWrap());

        talkButton = new Button(this);
        talkButton.setText("HOLD TO TALK");
        talkButton.setEnabled(false);
        root.addView(talkButton, matchWrap());
        talkButton.setOnTouchListener(this::onTalkTouch);

        endButton = new Button(this);
        endButton.setText("End room");
        endButton.setVisibility(View.GONE);
        root.addView(endButton, matchWrap());
        endButton.setOnClickListener(view -> endCall("Room ended"));

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        setContentView(scrollView);
    }

    private void setCallControlsEnabled(boolean enabled) {
        createButton.setEnabled(enabled && hasSavedProfile());
    }

    private boolean hasSavedProfile() {
        return isSavedProfileValue(
                android.iocl.dac_collector.Utility.SharedPrefs.getConsumerId()
        ) && isSavedProfileValue(
                android.iocl.dac_collector.Utility.SharedPrefs.getUsername()
        );
    }

    private boolean isSavedProfileValue(String value) {
        return value != null
                && !value.trim().isEmpty()
                && !value.contains("not_found");
    }

    private void updateProfileAvailability() {
        if (hasSavedProfile()) {
            setCallControlsEnabled(true);
            statusText.setText("Ready to call FCM_sender.");
        } else {
            setCallControlsEnabled(false);
            statusText.setText("Save a valid consumer profile before enabling a dashboard call.");
        }
    }

    private void showDashboardCallRequestIfPresent(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_DASHBOARD_CALL_REQUEST, false)) {
            dashboardCallRequested = true;
            statusText.setText(hasSavedProfile()
                    ? "The dashboard is requesting a call. Tap Enable dashboard call to connect."
                    : "The dashboard requested a call, but a valid consumer profile must be saved first.");
        }
    }

    private void withAudioPermission(Runnable action) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            action.run();
            return;
        }
        pendingAudioAction = action;
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_RECORD_AUDIO) {
            return;
        }
        Runnable action = pendingAudioAction;
        pendingAudioAction = null;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (action != null) {
                action.run();
            }
        } else {
            statusText.setText("Microphone permission is required to use push-to-talk.");
        }
    }

    private void createRoom() {
        String consumerId = android.iocl.dac_collector.Utility.SharedPrefs.getConsumerId();
        if (consumerId == null || consumerId.trim().isEmpty()
                || consumerId.contains("not_found")) {
            statusText.setText("Set up the consumer profile before calling.");
            return;
        }
        dashboardCallRequested = false;
        beginCallUi();
        Intent serviceIntent = new Intent(this, WalkieTalkieService.class)
                .setAction(WalkieTalkieService.ACTION_START_CALL)
                .putExtra(WalkieTalkieService.EXTRA_CONSUMER_ID, consumerId.trim());
        try {
            ContextCompat.startForegroundService(this, serviceIntent);
        } catch (SecurityException error) {
            statusText.setText("Could not start the background call. Check microphone permission.");
            talkButton.setEnabled(false);
            endButton.setVisibility(View.GONE);
            createButton.setEnabled(true);
        }
    }

    private void beginCallUi() {
        setCallControlsEnabled(false);
        talkButton.setEnabled(false);
        endButton.setVisibility(View.VISIBLE);
        statusText.setText("Setting up WebRTC audio...");
    }

    private boolean onTalkTouch(View view, MotionEvent event) {
        if (callService == null) {
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            callService.setTransmitting(true);
            talkButton.setText("TRANSMITTING - RELEASE TO MUTE");
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
            callService.setTransmitting(false);
            talkButton.setText("HOLD TO TALK");
            return true;
        }
        return true;
    }

    private void endCall(String message) {
        if (callService != null) {
            callService.endCall(message);
        }
        showCallEnded(message);
    }

    private void showCallEnded(String message) {
        roomCodeText.setText("");
        talkButton.setEnabled(false);
        talkButton.setText("HOLD TO TALK");
        endButton.setVisibility(View.GONE);
        createButton.setEnabled(true);
        statusText.setText(message);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent serviceIntent = new Intent(this, WalkieTalkieService.class);
        serviceBound = bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!dashboardCallRequested && (callService == null || !callService.isCallActive())) {
            updateProfileAvailability();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showDashboardCallRequestIfPresent(intent);
    }

    @Override
    protected void onStop() {
        if (callService != null) {
            callService.setTransmitting(false);
            talkButton.setText("HOLD TO TALK");
        }
        if (serviceBound) {
            if (callService != null) {
                callService.setListener(null);
            }
            unbindService(serviceConnection);
            serviceBound = false;
        }
        callService = null;
        super.onStop();
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onRoomCreated(String roomCode) {
        runOnUiThread(() -> {
            roomCodeText.setText("Call ID: " + roomCode);
            statusText.setText("Ringing FCM_sender. Waiting for receiver...");
        });
    }

    @Override
    public void onStatus(String status) {
        runOnUiThread(() -> statusText.setText(status));
    }

    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            statusText.setText("Connected. FCM_sender controls audio transmission.");
            talkButton.setEnabled(true);
        });
    }

    @Override
    public void onTransmitChanged(boolean transmitting) {
        runOnUiThread(() -> {
            talkButton.setText(transmitting
                    ? "TRANSMITTING - RELEASE TO MUTE"
                    : "HOLD TO TALK");
            if (transmitting) {
                statusText.setText("Transmitting audio to FCM_sender.");
            }
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            statusText.setText(message);
            showCallEnded(message);
        });
    }

    @Override
    public void onRoomEnded(String message) {
        runOnUiThread(() -> showCallEnded(message));
    }
}
