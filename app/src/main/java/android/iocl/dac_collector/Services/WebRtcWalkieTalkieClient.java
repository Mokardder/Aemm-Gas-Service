package android.iocl.dac_collector.Services;

import android.content.Context;
import android.media.AudioManager;
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
                    addRemoteCandidatesListener(hostUid);
                    listenForHostEnd();
                    SessionDescription offer = new SessionDescription(
                            SessionDescription.Type.OFFER,
                            offerSdp
                    );
                    peerConnection.setRemoteDescription(setDescriptionObserver(
                            () -> {
                                remoteDescriptionSet = true;
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
        closed = true;
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
        List<PeerConnection.IceServer> iceServers = Collections.singletonList(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        );
        PeerConnection.RTCConfiguration configuration =
                new PeerConnection.RTCConfiguration(iceServers);
        peerConnection = factory.createPeerConnection(configuration, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState state) {
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
                if (closed) {
                    return;
                }
                if (state == PeerConnection.IceConnectionState.CONNECTED
                        || state == PeerConnection.IceConnectionState.COMPLETED) {
                    listener.onConnected();
                } else if (state == PeerConnection.IceConnectionState.FAILED) {
                    reportError("WebRTC connection failed. Check the network or configure a TURN server.");
                } else if (state == PeerConnection.IceConnectionState.DISCONNECTED) {
                    listener.onStatus("Network interrupted. Trying to reconnect...");
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean receiving) {
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState state) {
            }

            @Override
            public void onIceCandidate(IceCandidate candidate) {
                publishLocalCandidate(candidate);
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] candidates) {
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
        audioSource = factory.createAudioSource(new MediaConstraints());
        audioTrack = factory.createAudioTrack("walkie-audio", audioSource);
        audioTrack.setEnabled(false);
        peerConnection.addTrack(audioTrack, Collections.singletonList("walkie-room"));
        return true;
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
                if (remoteDescriptionSet) {
                    if (!peerConnection.addIceCandidate(candidate)) {
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
                .addOnFailureListener(this::reportFirebaseError);
    }

    private void addQueuedRemoteCandidates() {
        if (peerConnection == null || closed) {
            return;
        }
        for (IceCandidate candidate : queuedRemoteCandidates) {
            if (!peerConnection.addIceCandidate(candidate)) {
                reportError("Could not apply a remote ICE candidate.");
            }
        }
        queuedRemoteCandidates.clear();
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
        Log.e(TAG, "Firebase walkie-talkie operation failed", error);
        reportError("Firebase signaling failed: " + error.getLocalizedMessage());
    }

    private void reportError(String message) {
        if (!closed) {
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
