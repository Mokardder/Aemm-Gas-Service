package android.iocl.dac_collector.Services;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.ChildEventListener;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WebRtcWalkieTalkieClient {

    private static final String TAG = "WalkieTalkie";
    private static final String ROOM_PATH = "walkie_talkie/rooms";
    private static final char[] ROOM_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final Object FACTORY_LOCK = new Object();
    private static final long ICE_FAILURE_GRACE_PERIOD_MS = 15_000L;
    private static PeerConnectionFactory sharedFactory;

    public interface Listener {
        void onRoomCreated(String roomCode);
        void onStatus(String status);
        void onConnected();
        void onTransmitChanged(boolean transmitting);
        void onError(String message);
        void onRoomEnded(String message);
    }

    private final Context context;
    private final String localUid;
    private final String localParticipantId;
    private final Listener listener;
    private final List<IceCandidate> queuedRemoteCandidates = new ArrayList<>();
    private final List<IceCandidate> queuedLocalCandidates = new ArrayList<>();
    private final AudioManager audioManager;
    private final int previousAudioMode;
    private final boolean previousSpeakerphoneState;
    private final Handler connectionHandler = new Handler(Looper.getMainLooper());
    private final Runnable iceFailureTimeout = this::handleIceFailureTimeout;

    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private AudioSource audioSource;
    private AudioTrack audioTrack;
    private DatabaseReference roomRef;
    private ValueEventListener answerListener;
    private ValueEventListener declinedListener;
    private ValueEventListener roomListener;
    private ValueEventListener transmitListener;
    private ChildEventListener remoteCandidatesListener;
    private String roomCode;
    private String hostUid;
    private String remoteUid;
    private boolean host;
    private boolean remoteDescriptionSet;
    private boolean answerPublished;
    private boolean declinedCallHandled;
    private boolean closed;
    private int localCandidateCount;
    private int remoteCandidateCount;

    private void handleIceFailureTimeout() {
        if (!closed && peerConnection != null
                && peerConnection.iceConnectionState() == PeerConnection.IceConnectionState.FAILED) {
            logPeerConnectionState("ICE failure grace period expired");
            reportError("WebRTC connection failed. Check the network or TURN server configuration.");
        }
    }

    public WebRtcWalkieTalkieClient(
            Context context,
            String localUid,
            String localParticipantId,
            Listener listener
    ) {
        this.context = context.getApplicationContext();
        this.localUid = localUid;
        this.localParticipantId = localParticipantId;
        this.listener = listener;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        previousAudioMode = audioManager.getMode();
        previousSpeakerphoneState = audioManager.isSpeakerphoneOn();
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);
        initializePeerConnectionFactory();
    }

    public void createRoom() {
        host = true;
        roomCode = generateRoomCode();
        Log.i(TAG, "Creating walkie-talkie room " + roomCode);
        roomRef = FirebaseDatabase.getInstance().getReference(ROOM_PATH).child(roomCode);

        roomRef.child("hostUid").setValue(localUid)
                .addOnSuccessListener(unused -> {
                    if (closed) {
                        removeRoomAfterCancelledCreation();
                        return;
                    }
                    roomRef.child("hostLabel").setValue(localParticipantId)
                            .addOnSuccessListener(labelUnused -> {
                                if (closed) {
                                    removeRoomAfterCancelledCreation();
                                    return;
                                }
                                roomRef.child("createdAt").setValue(ServerValue.TIMESTAMP)
                            .addOnSuccessListener(ignored -> {
                                if (closed) {
                                    removeRoomAfterCancelledCreation();
                                    return;
                                }
                                roomRef.onDisconnect().removeValue()
                                        .addOnFailureListener(error ->
                                                reportError("Could not secure room cleanup."));
                                listener.onRoomCreated(roomCode);
                                if (preparePeerConnection()) {
                                    listenForAnswer();
                                    listenForDecline();
                                    listenForTransmitCommand();
                                    createOffer();
                                }
                            })
                                    .addOnFailureListener(this::reportFirebaseError);
                            })
                            .addOnFailureListener(this::reportFirebaseError);
                })
                .addOnFailureListener(this::reportFirebaseError);
    }

    public void joinRoom(String requestedCode) {
        host = false;
        roomCode = requestedCode;
        Log.i(TAG, "Joining walkie-talkie room " + roomCode);
        roomRef = FirebaseDatabase.getInstance().getReference(ROOM_PATH).child(roomCode);
        roomRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (closed) {
                        return;
                    }
                    if (!snapshot.exists()) {
                        reportError("That room does not exist or has ended.");
                        return;
                    }
                    hostUid = snapshot.child("hostUid").getValue(String.class);
                    String offerSdp = snapshot.child("offer").child("sdp").getValue(String.class);
                    if (hostUid == null || offerSdp == null) {
                        reportError("The room is not ready yet. Try again in a moment.");
                        return;
                    }
                    if (localUid.equals(hostUid)) {
                        reportError("You cannot join a room you created.");
                        return;
                    }
                    if (!preparePeerConnection()) {
                        return;
                    }
                    Log.i(TAG, "Room offer loaded; preparing remote ICE candidates");
                    addRemoteCandidatesListener(hostUid);
                    listenForHostEnd();
                    SessionDescription offer = new SessionDescription(
                            SessionDescription.Type.OFFER,
                            offerSdp
                    );
                    peerConnection.setRemoteDescription(setDescriptionObserver(
                            () -> {
                                remoteDescriptionSet = true;
                                logPeerConnectionState("Remote offer applied");
                                addQueuedRemoteCandidates();
                                createAnswer();
                            },
                            error -> reportError("Could not read the WebRTC offer: " + error)
                    ), offer);
                })
                .addOnFailureListener(this::reportFirebaseError);
    }

    public void setTransmitting(boolean transmitting) {
        AudioTrack track = audioTrack;
        if (!closed && track != null) {
            track.setEnabled(transmitting);
        }
    }

    public void publishTransmitState(boolean transmitting) {
        if (closed || roomRef == null) {
            return;
        }
        roomRef.child("transmitting").setValue(transmitting)
                .addOnFailureListener(this::reportFirebaseError);
    }

    public void close() {
        if (closed) {
            return;
        }
        logPeerConnectionState("Closing walkie-talkie client");
        closed = true;
        connectionHandler.removeCallbacks(iceFailureTimeout);
        if (audioTrack != null) {
            audioTrack.setEnabled(false);
        }
        if (roomRef != null) {
            if (answerListener != null) {
                roomRef.child("answer").removeEventListener(answerListener);
            }
            if (declinedListener != null) {
                roomRef.child("declinedBy").removeEventListener(declinedListener);
            }
            if (transmitListener != null) {
                roomRef.child("transmitting").removeEventListener(transmitListener);
            }
            if (roomListener != null) {
                roomRef.removeEventListener(roomListener);
            }
            if (remoteCandidatesListener != null && remoteUid != null) {
                roomRef.child("candidates").child(remoteUid)
                        .removeEventListener(remoteCandidatesListener);
            }
            if (host) {
                roomRef.onDisconnect().cancel().addOnFailureListener(error ->
                        Log.e(TAG, "Failed to cancel room disconnect cleanup", error));
                removeRoomAfterCancelledCreation();
            } else {
                roomRef.child("answer").onDisconnect().cancel()
                        .addOnFailureListener(error ->
                                Log.e(TAG, "Failed to cancel answer disconnect cleanup", error));
                roomRef.child("answer").removeValue().addOnFailureListener(error ->
                        Log.e(TAG, "Failed to remove answer during call cleanup", error));
                roomRef.child("candidates").child(localUid).removeValue()
                        .addOnFailureListener(error ->
                                Log.e(TAG, "Failed to remove local ICE candidates", error));
            }
        }
        disposeMedia();
        audioManager.setSpeakerphoneOn(previousSpeakerphoneState);
        audioManager.setMode(previousAudioMode);
    }

    private void initializePeerConnectionFactory() {
        synchronized (FACTORY_LOCK) {
            if (sharedFactory == null) {
                PeerConnectionFactory.initialize(
                        PeerConnectionFactory.InitializationOptions.builder(context)
                                .createInitializationOptions()
                );
                sharedFactory = PeerConnectionFactory.builder().createPeerConnectionFactory();
            }
            factory = sharedFactory;
        }
    }

    private boolean preparePeerConnection() {
        if (closed) {
            return false;
        }
        List<PeerConnection.IceServer> iceServers = defaultIceServers();
        PeerConnection.RTCConfiguration configuration =
                new PeerConnection.RTCConfiguration(iceServers);
        peerConnection = factory.createPeerConnection(configuration, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState state) {
                logPeerConnectionState("Signaling state changed: " + state);
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
                if (closed) {
                    return;
                }
                logPeerConnectionState("ICE connection state changed: " + state);
                if (state == PeerConnection.IceConnectionState.CONNECTED
                        || state == PeerConnection.IceConnectionState.COMPLETED) {
                    connectionHandler.removeCallbacks(iceFailureTimeout);
                    listener.onConnected();
                } else if (state == PeerConnection.IceConnectionState.FAILED) {
                    logConnectionStats(peerConnection, state.toString());
                    listener.onStatus("ICE connection checks failed. Waiting for TURN candidates...");
                    connectionHandler.removeCallbacks(iceFailureTimeout);
                    connectionHandler.postDelayed(iceFailureTimeout, ICE_FAILURE_GRACE_PERIOD_MS);
                } else if (state == PeerConnection.IceConnectionState.DISCONNECTED) {
                    logConnectionStats(peerConnection, state.toString());
                    connectionHandler.removeCallbacks(iceFailureTimeout);
                    listener.onStatus("Network interrupted. Trying to reconnect...");
                } else if (state == PeerConnection.IceConnectionState.CHECKING) {
                    connectionHandler.removeCallbacks(iceFailureTimeout);
                }
            }

            @Override
            public void onConnectionChange(PeerConnection.PeerConnectionState state) {
                if (!closed) {
                    logPeerConnectionState("Peer connection state changed: " + state);
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean receiving) {
                if (!closed) {
                    Log.i(TAG, "ICE receiving changed to " + receiving + " for room " + roomCode);
                }
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState state) {
                if (!closed) {
                    logPeerConnectionState("ICE gathering state changed: " + state);
                }
            }

            @Override
            public void onIceCandidate(IceCandidate candidate) {
                localCandidateCount++;
                Log.d(TAG, "Gathered local ICE candidate #" + localCandidateCount
                        + " (mid=" + candidate.sdpMid
                        + ", mLine=" + candidate.sdpMLineIndex
                        + ") for room " + roomCode);
                publishLocalCandidate(candidate);
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] candidates) {
                Log.i(TAG, "WebRTC removed " + candidates.length
                        + " local ICE candidate(s) for room " + roomCode);
            }

            @Override
            public void onAddStream(org.webrtc.MediaStream stream) {
            }

            @Override
            public void onRemoveStream(org.webrtc.MediaStream stream) {
            }

            @Override
            public void onDataChannel(org.webrtc.DataChannel dataChannel) {
            }

            @Override
            public void onRenegotiationNeeded() {
            }

            @Override
            public void onAddTrack(org.webrtc.RtpReceiver receiver, org.webrtc.MediaStream[] streams) {
            }
        });
        if (peerConnection == null) {
            reportError("Could not create a WebRTC connection.");
            return false;
        }
        logPeerConnectionState("Peer connection created");
        audioSource = factory.createAudioSource(new MediaConstraints());
        audioTrack = factory.createAudioTrack("walkie-audio", audioSource);
        audioTrack.setEnabled(false);
        peerConnection.addTrack(audioTrack, Collections.singletonList("walkie-room"));
        return true;
    }

    private static List<PeerConnection.IceServer> defaultIceServers() {
        List<PeerConnection.IceServer> servers = new ArrayList<>();
        servers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                .createIceServer());
        servers.add(PeerConnection.IceServer.builder(
                        "turn:a.relay.metered.ca:3478?transport=udp")
                .setUsername("83eebabf8b4cce9d5dbcb649")
                .setPassword("2D7JvfkOQtBdYW3R")
                .createIceServer());
        servers.add(PeerConnection.IceServer.builder(
                        "turn:a.relay.metered.ca:3478?transport=tcp")
                .setUsername("83eebabf8b4cce9d5dbcb649")
                .setPassword("2D7JvfkOQtBdYW3R")
                .createIceServer());
        servers.add(PeerConnection.IceServer.builder(
                        "turn:a.relay.metered.ca:443?transport=tcp")
                .setUsername("83eebabf8b4cce9d5dbcb649")
                .setPassword("2D7JvfkOQtBdYW3R")
                .createIceServer());
        servers.add(PeerConnection.IceServer.builder(
                        "turns:a.relay.metered.ca:5349?transport=tcp")
                .setUsername("83eebabf8b4cce9d5dbcb649")
                .setPassword("2D7JvfkOQtBdYW3R")
                .createIceServer());
        return servers;
    }

    private void createOffer() {
        peerConnection.createOffer(createDescriptionObserver(
                description -> peerConnection.setLocalDescription(setDescriptionObserver(
                        () -> publishDescription("offer", description, true),
                        error -> reportError("Could not set the local offer: " + error)
                ), description),
                error -> reportError("Could not create a WebRTC offer: " + error)
        ), createAudioSdpConstraints());
    }

    private void createAnswer() {
        if (closed || peerConnection == null) {
            return;
        }
        peerConnection.createAnswer(createDescriptionObserver(
                description -> peerConnection.setLocalDescription(setDescriptionObserver(
                        () -> publishDescription("answer", description, false),
                        error -> reportError("Could not set the local answer: " + error)
                ), description),
                error -> reportError("Could not create a WebRTC answer: " + error)
        ), createAudioSdpConstraints());
    }

    private void publishDescription(String key, SessionDescription description, boolean isOffer) {
        if (closed || roomRef == null) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        values.put("type", description.type.canonicalForm());
        values.put("sdp", description.description);
        if (!isOffer) {
            values.put("uid", localUid);
            roomRef.child("answer").onDisconnect().removeValue()
                    .addOnFailureListener(error -> reportError("Could not register room leave cleanup."));
        }
        roomRef.child(key).setValue(values)
                .addOnSuccessListener(unused -> {
                    if (!closed) {
                        Log.i(TAG, "Published " + key + " SDP to Firebase for room " + roomCode);
                        if (!isOffer) {
                            answerPublished = true;
                            publishQueuedLocalCandidates();
                        }
                        listener.onStatus(isOffer
                                ? "Room ready. Waiting for the other person..."
                                : "Answer sent. Connecting audio...");
                    }
                })
                .addOnFailureListener(this::reportFirebaseError);
    }

    private void listenForAnswer() {
        answerListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (closed) {
                    return;
                }
                if (!snapshot.exists()) {
                    if (remoteDescriptionSet) {
                        Log.i(TAG, "Remote participant removed the answer for room " + roomCode);
                        listener.onRoomEnded("The other participant left the room.");
                    }
                    return;
                }
                if (remoteDescriptionSet) {
                    return;
                }
                remoteUid = snapshot.child("uid").getValue(String.class);
                String sdp = snapshot.child("sdp").getValue(String.class);
                if (remoteUid == null || sdp == null || localUid.equals(remoteUid)) {
                    reportError("Received an invalid room answer.");
                    return;
                }
                addRemoteCandidatesListener(remoteUid);
                SessionDescription answer = new SessionDescription(
                        SessionDescription.Type.ANSWER,
                        sdp
                );
                peerConnection.setRemoteDescription(setDescriptionObserver(
                        () -> {
                            remoteDescriptionSet = true;
                            logPeerConnectionState("Remote answer applied");
                            addQueuedRemoteCandidates();
                        },
                        error -> reportError("Could not read the WebRTC answer: " + error)
                ), answer);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                reportFirebaseError(error.toException());
            }
        };
        roomRef.child("answer").addValueEventListener(answerListener);
    }

    private void listenForDecline() {
        declinedListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (closed || declinedCallHandled || !snapshot.exists()) {
                    return;
                }
                String declinedBy = snapshot.getValue(String.class);
                if (declinedBy != null && !localUid.equals(declinedBy)) {
                    declinedCallHandled = true;
                    Log.i(TAG, "Room " + roomCode + " was declined by " + declinedBy);
                    listener.onRoomEnded("FCM_sender declined the call.");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                reportFirebaseError(error.toException());
            }
        };
        roomRef.child("declinedBy").addValueEventListener(declinedListener);
    }

    private void listenForTransmitCommand() {
        transmitListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (closed) {
                    return;
                }
                Boolean requested = snapshot.getValue(Boolean.class);
                listener.onTransmitChanged(Boolean.TRUE.equals(requested));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                reportFirebaseError(error.toException());
            }
        };
        roomRef.child("transmitting").addValueEventListener(transmitListener);
    }

    private void addRemoteCandidatesListener(String uid) {
        remoteUid = uid;
        remoteCandidatesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                if (closed || peerConnection == null) {
                    return;
                }
                String candidateText = snapshot.child("candidate").getValue(String.class);
                String sdpMid = snapshot.child("sdpMid").getValue(String.class);
                Object lineValue = snapshot.child("sdpMLineIndex").getValue();
                if (candidateText == null || !(lineValue instanceof Number)) {
                    reportError("Received an invalid ICE candidate.");
                    return;
                }
                Number lineIndex = (Number) lineValue;
                IceCandidate candidate = new IceCandidate(sdpMid, lineIndex.intValue(), candidateText);
                remoteCandidateCount++;
                Log.d(TAG, "Received remote ICE candidate #" + remoteCandidateCount
                        + " (mid=" + sdpMid
                        + ", mLine=" + lineIndex.intValue()
                        + ", remoteDescriptionSet=" + remoteDescriptionSet
                        + ") for room " + roomCode);
                if (remoteDescriptionSet) {
                    if (!peerConnection.addIceCandidate(candidate)) {
                        Log.w(TAG, "WebRTC rejected remote ICE candidate #" + remoteCandidateCount
                                + " for room " + roomCode);
                        reportError("Could not apply a remote ICE candidate.");
                    }
                } else {
                    queuedRemoteCandidates.add(candidate);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
            }

            @Override
            public void onCancelled(DatabaseError error) {
                reportFirebaseError(error.toException());
            }
        };
        roomRef.child("candidates").child(uid).addChildEventListener(remoteCandidatesListener);
    }

    private void listenForHostEnd() {
        roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!closed && !snapshot.exists()) {
                    Log.i(TAG, "Room owner removed room " + roomCode);
                    setTransmitting(false);
                    listener.onRoomEnded("The room owner ended the call.");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                reportFirebaseError(error.toException());
            }
        };
        roomRef.addValueEventListener(roomListener);
    }

    private void publishLocalCandidate(IceCandidate candidate) {
        if (closed || roomRef == null) {
            return;
        }
        if (!host && !answerPublished) {
            queuedLocalCandidates.add(candidate);
            return;
        }
        writeLocalCandidate(candidate);
    }

    private void publishQueuedLocalCandidates() {
        for (IceCandidate candidate : queuedLocalCandidates) {
            writeLocalCandidate(candidate);
        }
        queuedLocalCandidates.clear();
    }

    private void writeLocalCandidate(IceCandidate candidate) {
        if (closed || roomRef == null) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        values.put("candidate", candidate.sdp);
        values.put("sdpMid", candidate.sdpMid);
        values.put("sdpMLineIndex", candidate.sdpMLineIndex);
        roomRef.child("candidates").child(localUid).push().setValue(values)
                .addOnSuccessListener(unused -> Log.d(TAG,
                        "Published local ICE candidate (mid=" + candidate.sdpMid
                                + ", mLine=" + candidate.sdpMLineIndex
                                + ") for room " + roomCode))
                .addOnFailureListener(this::reportFirebaseError);
    }

    private void addQueuedRemoteCandidates() {
        if (peerConnection == null || closed) {
            return;
        }
        for (IceCandidate candidate : queuedRemoteCandidates) {
            if (!peerConnection.addIceCandidate(candidate)) {
                Log.w(TAG, "WebRTC rejected a queued remote ICE candidate for room " + roomCode);
                reportError("Could not apply a remote ICE candidate.");
            }
        }
        if (!queuedRemoteCandidates.isEmpty()) {
            Log.i(TAG, "Applied " + queuedRemoteCandidates.size()
                    + " queued remote ICE candidate(s) for room " + roomCode);
        }
        queuedRemoteCandidates.clear();
    }

    private void logPeerConnectionState(String event) {
        PeerConnection connection = peerConnection;
        if (connection == null) {
            Log.i(TAG, event + " (room=" + roomCode + ", role="
                    + (host ? "host" : "receiver") + ", peerConnection=unavailable)");
            return;
        }
        Log.i(TAG, event + " (room=" + roomCode
                + ", role=" + (host ? "host" : "receiver")
                + ", signaling=" + connection.signalingState()
                + ", ice=" + connection.iceConnectionState()
                + ", connection=" + connection.connectionState()
                + ", gathering=" + connection.iceGatheringState()
                + ", localCandidates=" + localCandidateCount
                + ", remoteCandidates=" + remoteCandidateCount
                + ", queuedRemoteCandidates=" + queuedRemoteCandidates.size()
                + ")");
    }

    private void logConnectionStats(PeerConnection connection, String reason) {
        if (connection == null) {
            return;
        }
        connection.getStats(report -> {
            if (peerConnection != connection) {
                return;
            }
            Map<String, RTCStats> statsMap = report.getStatsMap();
            boolean foundCandidatePair = false;
            for (RTCStats stats : statsMap.values()) {
                if (!"candidate-pair".equals(stats.getType())) {
                    continue;
                }
                Map<String, Object> members = stats.getMembers();
                String state = String.valueOf(members.get("state"));
                boolean selected = Boolean.TRUE.equals(members.get("selected"));
                boolean nominated = Boolean.TRUE.equals(members.get("nominated"));
                if (!selected && !nominated && !"succeeded".equals(state)
                        && !"failed".equals(state)) {
                    continue;
                }
                foundCandidatePair = true;
                Log.w(TAG, "ICE stats after " + reason + " (room=" + roomCode
                        + ", pairState=" + state
                        + ", selected=" + selected
                        + ", nominated=" + nominated
                        + ", local=" + describeCandidate(statsMap,
                        String.valueOf(members.get("localCandidateId")))
                        + ", remote=" + describeCandidate(statsMap,
                        String.valueOf(members.get("remoteCandidateId")))
                        + ", rtt=" + members.get("currentRoundTripTime")
                        + ", bytesSent=" + members.get("bytesSent")
                        + ", bytesReceived=" + members.get("bytesReceived")
                        + ")");
            }
            if (!foundCandidatePair) {
                Log.w(TAG, "ICE stats after " + reason
                        + " contain no selected/succeeded/failed candidate pair (room="
                        + roomCode + ", statsCount=" + statsMap.size() + ")");
            }
        });
    }

    private String describeCandidate(Map<String, RTCStats> statsMap, String candidateId) {
        RTCStats candidate = statsMap.get(candidateId);
        if (candidate == null) {
            return "unavailable";
        }
        Map<String, Object> members = candidate.getMembers();
        return "type=" + members.get("candidateType")
                + ",protocol=" + members.get("protocol");
    }

    private MediaConstraints createAudioSdpConstraints() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
        return constraints;
    }

    private SdpObserver setDescriptionObserver(Runnable onSuccess, SdpFailureHandler onFailure) {
        return new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription description) {
            }

            @Override
            public void onSetSuccess() {
                onSuccess.run();
            }

            @Override
            public void onCreateFailure(String error) {
                onFailure.onFailure(error);
            }

            @Override
            public void onSetFailure(String error) {
                onFailure.onFailure(error);
            }
        };
    }

    private SdpObserver createDescriptionObserver(
            SdpCreateSuccessHandler onSuccess,
            SdpFailureHandler onFailure
    ) {
        return new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription description) {
                onSuccess.onSuccess(description);
            }

            @Override
            public void onSetSuccess() {
            }

            @Override
            public void onCreateFailure(String error) {
                onFailure.onFailure(error);
            }

            @Override
            public void onSetFailure(String error) {
                onFailure.onFailure(error);
            }
        };
    }

    private interface SdpCreateSuccessHandler {
        void onSuccess(SessionDescription description);
    }

    private interface SdpFailureHandler {
        void onFailure(String error);
    }

    private void disposeMedia() {
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection.dispose();
            peerConnection = null;
        }
        if (audioTrack != null) {
            audioTrack.dispose();
            audioTrack = null;
        }
        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }
    }

    private void removeRoomAfterCancelledCreation() {
        if (roomRef != null) {
            roomRef.removeValue().addOnFailureListener(error ->
                    Log.e(TAG, "Failed to remove room during call cleanup", error));
        }
    }

    private void reportFirebaseError(Exception error) {
        Log.e(TAG, "Firebase signaling operation failed for room " + roomCode, error);
        reportError("Firebase signaling failed: " + error.getLocalizedMessage());
    }

    private void reportError(String message) {
        if (!closed) {
            Log.e(TAG, message);
            listener.onError(message);
        }
    }

    private String generateRoomCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            code.append(ROOM_ALPHABET[random.nextInt(ROOM_ALPHABET.length)]);
        }
        return code.toString();
    }
}
