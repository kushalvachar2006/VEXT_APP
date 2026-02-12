package com.example.chat_application;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Call_layout extends AppCompatActivity {

    private static final String TAG = "Call_layout";
    private static final int PERMISSION_REQUEST = 100;

    private WebRTCHelper rtc;
    private FirebaseFirestore db;

    private String callId = "test_call";
    private boolean isCaller = true;
    private boolean isVideoCall = true;

    private SurfaceViewRenderer localView, remoteView;
    private ToneGenerator toneGenerator;

    private DocumentReference callDocRef;

    private LinearLayout audioCallContainer;
    private ImageView receiverImage;
    private TextView receiverName, receiverPhone, callStatus;
    private ImageButton btnMuteAudio, btnSpeaker, btnEndAudio;

    private LinearLayout receiverInfoOverlay, videoControls;
    private ImageView receiverImageOverlay;
    private TextView receiverNameOverlay, receiverPhoneOverlay;
    private ImageButton btnMuteVideo, btnEndVideo, btnSwitchCamera;

    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private AudioManager audioManager;
    private ListenerRegistration offerListener;
    private ListenerRegistration answerListener;
    private ListenerRegistration iceListener;
    private ListenerRegistration statusListener;
    private boolean callWasConnected = false;
    private boolean remoteAnswerApplied = false;
    private boolean remoteOfferApplied = false;
    private boolean callEnded = false;
    private final List<IceCandidate> pendingIceCandidates = new ArrayList<>();


    private String receiverNameStr, receiverPhoneStr, receiverProfileBase64, receiverId;

    private Handler callTimeoutHandler = new Handler();
    private Runnable callTimeoutRunnable = () -> {
        if (isCaller && !callEnded) {
            db.collection("calls").document(callId).get().addOnSuccessListener(snap -> {
                if (snap.exists()) {
                    String status = snap.getString("status");

                    if ("ringing".equals(status)) {
                        Log.d(TAG, "Call timeout after 60 seconds - no answer");

                        //  Update to show as missed call
                        db.collection("calls").document(callId)
                                .update("status", "ended")
                                .addOnSuccessListener(aVoid -> {
                                    //  Log as missed for caller
                                    logCall("outgoing", "missed");

                                    runOnUiThread(() -> {
                                        stopDialTone();
                                        endCall();
                                    });
                                });
                    } else {
                        Log.d(TAG, "Call timeout skipped - call status is: " + status);
                    }
                }
            }).addOnFailureListener(ex -> {
                Log.e(TAG, "Failed to check call status for timeout", ex);
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_layout);

        Log.d(TAG, "onCreate called - callId will be: " +
                (getIntent() != null ? getIntent().getStringExtra("callId") : "null") +
                ", taskId: " + getTaskId() +
                ", isCaller: " + getIntent().getBooleanExtra("isCaller", true));

        db = FirebaseFirestore.getInstance();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        Intent intent = getIntent();
        if (intent != null) {
            callId = intent.getStringExtra("callId") != null ? intent.getStringExtra("callId") : "test_call";
            isCaller = intent.getBooleanExtra("isCaller", true);
            isVideoCall = intent.getBooleanExtra("isVideoCall", true);
            receiverNameStr = intent.getStringExtra("receiverName");
            receiverPhoneStr = intent.getStringExtra("receiverPhone");
            receiverId = intent.getStringExtra("receiverId");

            fetchReceiverProfile();

            callDocRef = db.collection("calls").document(callId);
            initializeCall();

        } else {
            Log.e(TAG, "Intent is null, cannot proceed with call");
            finish();
        }
    }
    private void fetchReceiverProfile() {
        if (receiverId == null) return;

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(receiverId)
                .child("profilePicBase64")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                        receiverProfileBase64 = snapshot.getValue(String.class);
                        if (receiverProfileBase64 == null) {
                            receiverProfileBase64 = "";
                        }


                        runOnUiThread(() -> {
                            if (isVideoCall) {
                                setReceiverInfo(receiverNameOverlay, receiverPhoneOverlay, receiverImageOverlay);
                            } else {
                                setReceiverInfo(receiverName, receiverPhone, receiverImage);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
                        Log.e(TAG, "Failed to fetch receiver profile", error.toException());
                        receiverProfileBase64 = "";
                    }
                });
    }

    private void initializeCall() {

        bindViews();
        setupUI();
        setupCallAudio();

        // Check permissions first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                }, PERMISSION_REQUEST);
                return;
            }
        }


        setupWebRTC();
    }


    private void setupWebRTC() {
        rtc = new WebRTCHelper(this, localView, remoteView, isVideoCall);
        if (isVideoCall) rtc.startLocalVideo();

        rtc.setupPeerConnection(new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate ice) {
                Log.d(TAG, "Local ICE candidate generated: " + ice.sdpMid);
                sendIceCandidate(ice);
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}

            @Override
            public void onAddStream(MediaStream stream) {
                Log.d(TAG, "Remote stream added");
                if (stream != null) {
                    runOnUiThread(() -> rtc.attachRemoteStream(stream));
                }
            }

            @Override
            public void onSignalingChange(PeerConnection.SignalingState s) {}

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
                Log.d(TAG, "ICE connection state: " + newState);


                if (newState == PeerConnection.IceConnectionState.CONNECTED) {

                    callWasConnected = true;

                    runOnUiThread(() -> {
                        stopDialTone();
                        if (callStatus != null) callStatus.setText("In Call");
                    });

                    if (isCaller) {
                        db.collection("calls").document(callId)
                                .update("status", "in-call");
                    }
                }


                if (newState == PeerConnection.IceConnectionState.FAILED ||
                        newState == PeerConnection.IceConnectionState.CLOSED) {
                    if (!callWasConnected) {
                        logCall(isCaller ? "outgoing" : "incoming", "failed");
                    }
                    Log.d(TAG, "ICE failed/closed → ending call");
                    runOnUiThread(()->endCall());
                }
                if (newState == PeerConnection.IceConnectionState.DISCONNECTED) {
                    Log.w(TAG, "ICE temporarily disconnected — waiting for recovery");

                    new Handler().postDelayed(() -> {
                        if (rtc != null) {
                            PeerConnection pc = rtc.getPeerConnection();
                            if (pc != null &&
                                    pc.iceConnectionState() == PeerConnection.IceConnectionState.DISCONNECTED) {

                                Log.e(TAG, "ICE still disconnected after timeout → ending call");
                                endCall();
                            }
                        }
                    }, 10_000);
                }

            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {}

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState s) {
                Log.d(TAG, "ICE gathering state: " + s);
            }

            @Override
            public void onRemoveStream(MediaStream s) {}

            @Override
            public void onDataChannel(DataChannel dc) {}

            @Override
            public void onRenegotiationNeeded() {}

            @Override
            public void onAddTrack(RtpReceiver r, MediaStream[] s) {}
        });

        listenForOffer();
        listenForAnswer();
        listenForIceCandidates();
        listenCallStatus();

        if (isCaller) {
            startDialTone();
            createOffer();
            callTimeoutHandler.postDelayed(callTimeoutRunnable, 60000);
        }

        setupButtonListeners();
    }

    private void bindViews() {
        localView = findViewById(R.id.local_view);
        remoteView = findViewById(R.id.remote_view);
        audioCallContainer = findViewById(R.id.audio_call_container);
        receiverImage = findViewById(R.id.receiver_image);
        receiverName = findViewById(R.id.receiver_name);
        receiverPhone = findViewById(R.id.receiver_phone);
        callStatus = findViewById(R.id.call_status);
        btnMuteAudio = findViewById(R.id.btn_mute);
        btnSpeaker = findViewById(R.id.btn_speaker);
        btnEndAudio = findViewById(R.id.btn_end_call);
        receiverInfoOverlay = findViewById(R.id.receiver_info_overlay);
        receiverImageOverlay = findViewById(R.id.receiver_image_overlay);
        receiverNameOverlay = findViewById(R.id.receiver_name_overlay);
        receiverPhoneOverlay = findViewById(R.id.receiver_phone_overlay);
        videoControls = findViewById(R.id.video_controls);
        btnMuteVideo = findViewById(R.id.btn_mute_video);
        btnEndVideo = findViewById(R.id.btn_end_video_call);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
    }

    private void setupUI() {
        if (isVideoCall) {
            audioCallContainer.setVisibility(View.GONE);
            localView.setVisibility(View.VISIBLE);
            remoteView.setVisibility(View.VISIBLE);
            receiverInfoOverlay.setVisibility(View.VISIBLE);
            videoControls.setVisibility(View.VISIBLE);
            setReceiverInfo(receiverNameOverlay, receiverPhoneOverlay, receiverImageOverlay);
        } else {
            audioCallContainer.setVisibility(View.VISIBLE);
            localView.setVisibility(View.GONE);
            remoteView.setVisibility(View.GONE);
            receiverInfoOverlay.setVisibility(View.GONE);
            videoControls.setVisibility(View.GONE);
            setReceiverInfo(receiverName, receiverPhone, receiverImage);
        }
    }

    private void setReceiverInfo(TextView nameView, TextView phoneView, ImageView imageView) {
        if (receiverNameStr != null) nameView.setText(receiverNameStr);
        if (receiverPhoneStr != null && !receiverPhoneStr.isEmpty()) {
            phoneView.setText(receiverPhoneStr);
            phoneView.setVisibility(View.VISIBLE);
        } else {
            phoneView.setVisibility(View.GONE);
        }

        if (receiverProfileBase64 != null && !receiverProfileBase64.isEmpty()) {
            try {
                byte[] decoded = Base64.decode(receiverProfileBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                Glide.with(this)
                        .load(bitmap)
                        .placeholder(R.drawable.profile_icon)
                        .error(R.drawable.profile_icon)
                        .circleCrop()
                        .into(imageView);
                Log.d(TAG, "Profile pic updated successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode receiver profile", e);
            }
        }
    }

    private void setupButtonListeners() {
        btnMuteAudio.setOnClickListener(v -> toggleMute());
        btnSpeaker.setOnClickListener(v -> toggleSpeaker());
        btnEndAudio.setOnClickListener(v -> endCall());
        btnMuteVideo.setOnClickListener(v -> toggleMute());
        btnEndVideo.setOnClickListener(v -> endCall());
        btnSwitchCamera.setOnClickListener(v -> {
            if (rtc != null) rtc.switchCamera();
        });
    }

    private void toggleMute() {
        isMuted = !isMuted;
        if (rtc != null) rtc.setMute(isMuted);

        if (isVideoCall)
            btnMuteVideo.setImageResource(isMuted ? R.drawable.ic_mic_off : R.drawable.micbtn);
        else
            btnMuteAudio.setImageResource(isMuted ? R.drawable.ic_mic_off : R.drawable.micbtn);
    }

    private void toggleSpeaker() {

        isSpeakerOn = !isSpeakerOn;

        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        if (isSpeakerOn) {
            audioManager.setSpeakerphoneOn(true);
            audioManager.setRouting(
                    AudioManager.MODE_IN_COMMUNICATION,
                    AudioManager.ROUTE_SPEAKER,
                    AudioManager.ROUTE_ALL
            );
        } else {
            audioManager.setSpeakerphoneOn(false);
            audioManager.setRouting(
                    AudioManager.MODE_IN_COMMUNICATION,
                    AudioManager.ROUTE_EARPIECE,
                    AudioManager.ROUTE_ALL
            );
        }

        btnSpeaker.setImageResource(
                isSpeakerOn
                        ? R.drawable.ic_volume_up
                        : R.drawable.ic_volume_up_active
        );
    }


    private synchronized void endCall() {
        if (callEnded) return;
        callEnded = true;

        // Remove Firestore listeners
        if (offerListener != null) {
            offerListener.remove();
            offerListener = null;
        }

        if (answerListener != null) {
            answerListener.remove();
            answerListener = null;
        }

        if (iceListener != null) {
            iceListener.remove();
            iceListener = null;
        }

        if (statusListener != null) {
            statusListener.remove();
            statusListener = null;
        }

        Log.d(TAG, "Call ended");


        callTimeoutHandler.removeCallbacks(callTimeoutRunnable);
        stopDialTone();

        if (rtc != null) {
            rtc.stop();
            rtc = null;
        }

        try {
            db.collection("calls").document(callId).delete();
        } catch (Exception ignored) {}

        Intent intent = new Intent(this, MainPage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }


    private void createOffer() {
        if (rtc == null) {
            Log.d(TAG, "RTC not initialized at createOffer - skipping offer");
            return;
        }
        PeerConnection pc = rtc.getPeerConnection();
        if (pc == null) return;

        pc.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription desc) {
                pc.setLocalDescription(new SimpleSdpObserver() {}, desc);

                Map<String, Object> data = new HashMap<>();
                data.put("offer", desc.description);
                data.put("callerName", ContactAdapter.currentname);
                data.put("callerProfile", ContactAdapter.currentProfileBase64);
                String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                data.put("callerId", currentUserId);
                data.put("receiverId", receiverId); //  ADDED receiverId
                data.put("receiverName", receiverNameStr);
                data.put("receiverProfile", receiverProfileBase64);
                data.put("isVideoCall", isVideoCall);
                data.put("timestamp", System.currentTimeMillis());
                data.put("status", "ringing");
                data.put("receiverPhone", receiverPhoneStr);

                db.collection("calls").document(callId)
                        .set(data)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Offer sent & receiver notified");
                            runOnUiThread(() -> {
                                if (callStatus != null) {
                                    callStatus.setText("Ringing...");
                                }
                            });
                            logCall("outgoing", "ringing");
                        });
            }
        }, new MediaConstraints());
    }

    private void listenForOffer() {
        offerListener = db.collection("calls")
                .document(callId)
                .addSnapshotListener((snap, e) -> {

                    if (e != null || snap == null || !snap.exists()) return;
                    if (!snap.contains("offer") || isCaller) return;
                    if (remoteOfferApplied) return; //  Prevent duplicate processing

                    if (rtc == null) {
                        Log.w(TAG, "RTC not ready - skipping offer");
                        return;
                    }

                    String offer = snap.getString("offer");
                    PeerConnection pc = rtc.getPeerConnection();
                    if (pc == null) return;

                    //  Set remote description and mark as applied
                    pc.setRemoteDescription(
                            new SimpleSdpObserver() {
                                @Override
                                public void onSetSuccess() {
                                    remoteOfferApplied = true;
                                    Log.d(TAG, "Remote OFFER SDP applied successfully");

                                    //  Process buffered ICE candidates
                                    processPendingIceCandidates();

                                    // Now create answer
                                    createAnswer();
                                }

                                @Override
                                public void onSetFailure(String s) {
                                    Log.e(TAG, "Failed to apply remote offer SDP: " + s);
                                }
                            },
                            new SessionDescription(SessionDescription.Type.OFFER, offer)
                    );
                });

    }

    private void createAnswer() {
        if (rtc == null) {
            Log.d(TAG, "RTC not initialized at createAnswer - skipping offer");
            return;
        }
        PeerConnection pc = rtc.getPeerConnection();
        if (pc == null) return;

        pc.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription desc) {
                pc.setLocalDescription(new SimpleSdpObserver() {}, desc);
                Map<String, Object> update = new HashMap<>();
                update.put("answer", desc.description);
                update.put("answerTimestamp", System.currentTimeMillis());
                update.put("status", "accepted");
                db.collection("calls").document(callId)
                        .set(update, SetOptions.merge());
                logCall("incoming", "accepted");
            }
        }, new MediaConstraints());
    }

    private void listenForAnswer() {

            answerListener = db.collection("calls")
                    .document(callId)
                    .addSnapshotListener((snap, e) -> {

                        if (e != null || snap == null || !snap.exists()) return;
                        if (!isCaller || !snap.contains("answer")) return;
                        if (remoteAnswerApplied) return; //  Already processed

                        if (rtc == null) {
                            Log.w(TAG, "RTC not ready - skipping answer");
                            return;
                        }

                        String answer = snap.getString("answer");
                        PeerConnection pc = rtc.getPeerConnection();
                        if (pc == null) return;

                        pc.setRemoteDescription(
                                new SimpleSdpObserver() {
                                    @Override
                                    public void onSetSuccess() {
                                        remoteAnswerApplied = true;
                                        Log.d(TAG, "Remote ANSWER SDP applied successfully");

                                        //  CANCEL TIMEOUT - Call was answered
                                        callTimeoutHandler.removeCallbacks(callTimeoutRunnable);
                                        runOnUiThread(() -> {
                                            stopDialTone();
                                            // Update status to show connection in progress
                                            if (callStatus != null) {
                                                callStatus.setText("Connecting...");
                                            }
                                        });

                                        //  Process buffered ICE candidates
                                        processPendingIceCandidates();
                                        logCall("outgoing", "accepted");
                                    }

                                    @Override
                                    public void onSetFailure(String s) {
                                        Log.e(TAG, "Failed to apply remote answer SDP: " + s);
                                    }
                                },
                                new SessionDescription(SessionDescription.Type.ANSWER, answer)
                        );
                    });

    }

    private void sendIceCandidate(IceCandidate ice) {
        if (ice == null) return;
        String role = isCaller ? "caller" : "callee";
        Map<String, Object> m = new HashMap<>();
        m.put("sdpMid", ice.sdpMid);
        m.put("sdpMLineIndex", ice.sdpMLineIndex);
        m.put("sdp", ice.sdp);

        db.collection("calls").document(callId)
                .collection("candidates").document(role)
                .collection("list")
                .add(m);
    }

    private void listenForIceCandidates() {
        String remote = isCaller ? "callee" : "caller";

        iceListener = db.collection("calls")
                .document(callId)
                .collection("candidates")
                .document(remote)
                .collection("list")
                .addSnapshotListener((snapshots, e) -> {

                    if (snapshots == null || rtc == null) return;

                    PeerConnection pc = rtc.getPeerConnection();
                    if (pc == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Map<String, Object> d = dc.getDocument().getData();
                            String sdpMid = (String) d.get("sdpMid");
                            int sdpMLineIndex = ((Long) d.get("sdpMLineIndex")).intValue();
                            String sdp = (String) d.get("sdp");

                            IceCandidate ice = new IceCandidate(sdpMid, sdpMLineIndex, sdp);

                            //  Check if remote description is set
                            boolean remoteDescSet = isCaller ? remoteAnswerApplied : remoteOfferApplied;

                            if (remoteDescSet) {
                                //  Safe to add ICE candidate
                                boolean added = pc.addIceCandidate(ice);
                                Log.d(TAG, "ICE candidate added: " + added + " (sdpMid: " + sdpMid + ")");
                            } else {
                                //  Buffer for later
                                synchronized (pendingIceCandidates) {
                                    pendingIceCandidates.add(ice);
                                    Log.d(TAG, "ICE candidate buffered (remote SDP not ready yet)");
                                }
                            }
                        }
                    }
                });

    }
    private void processPendingIceCandidates() {
        if (rtc == null) return;
        PeerConnection pc = rtc.getPeerConnection();
        if (pc == null) return;

        synchronized (pendingIceCandidates) {
            Log.d(TAG, "Processing " + pendingIceCandidates.size() + " buffered ICE candidates");

            for (IceCandidate ice : pendingIceCandidates) {
                boolean added = pc.addIceCandidate(ice);
                Log.d(TAG, "Buffered ICE candidate added: " + added);
            }

            pendingIceCandidates.clear();
        }
    }

    private void listenCallStatus() {
            statusListener = db.collection("calls")
                    .document(callId)
                    .addSnapshotListener((snap, e) -> {

                        if (e != null) {
                            Log.e(TAG, "Firestore error in status listener", e);
                            return;
                        }

                        if (snap == null) return;

                        if (rtc == null) return;

                        if (!snap.exists()) {
                            if (callWasConnected && !callEnded) {
                                Log.d(TAG, "Call document deleted after connection → ending call");
                                runOnUiThread(this::endCall);
                            } else {
                                Log.d(TAG, "Call document missing (startup / normal)");
                            }
                            return;
                        }

                        String status = snap.getString("status");
                        if (status == null) return;

                        Log.d(TAG, "Call status: " + status);

                        //  UPDATE UI WITH CURRENT STATUS
                        runOnUiThread(() -> {
                            if (callStatus != null) {
                                switch (status) {
                                    case "ringing":
                                        callStatus.setText(isCaller ? "Ringing..." : "Incoming Call");
                                        break;
                                    case "accepted":
                                        callStatus.setText("Connecting...");
                                        break;
                                    case "in-call":
                                        callStatus.setText("In Call");
                                        break;
                                    case "ended":
                                    case "rejected":
                                        callStatus.setText("Call Ended");
                                        break;
                                    default:
                                        callStatus.setText(status);
                                }
                            }
                        });


                        //  Handle status changes
                        if ("accepted".equals(status)) {
                            stopDialTone();
                            callTimeoutHandler.removeCallbacks(callTimeoutRunnable); //  Cancel timeout when accepted
                            //logCall(isCaller ? "outgoing" : "incoming", "accepted");
                        }

                        if ("rejected".equals(status)) {

                            stopDialTone();
                            playBusyTone();
                            callTimeoutHandler.removeCallbacks(callTimeoutRunnable);

                            logCall(isCaller ? "outgoing" : "incoming", "declined");

                            new Handler().postDelayed(this::endCall, 2000);
                        }


                        if ("ended".equals(status)) {
                            Log.d(TAG, "Remote ended the call");
                            if (callWasConnected) {
                                logCall(isCaller ? "outgoing" : "incoming", "completed");
                            } else {
                                // Call ended before connection
                                logCall(isCaller ? "outgoing" : "incoming", "cancelled");
                            }
                            endCall();
                        }
                    });
    }

    private void setupCallAudio() {
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setMicrophoneMute(false);

        audioManager.setSpeakerphoneOn(isVideoCall);
        isSpeakerOn = isVideoCall;

        audioManager.requestAudioFocus(
                focusChange -> {},
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            boolean granted = true;
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                //  FIXED: Now initialize WebRTC after permissions granted
                setupWebRTC();
            } else {
                Log.w(TAG, "Permissions denied");
                endCall();
            }
        }
    }

    private void startDialTone() {
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_NETWORK_USA_RINGBACK, -1);
        } catch (Exception e) {
            Log.e(TAG, "Failed to play dial tone", e);
        }
    }

    private void stopDialTone() {
        try {
            if (toneGenerator != null) {
                toneGenerator.stopTone();
                toneGenerator.release();
                toneGenerator = null;
            }
        } catch (Exception ignored) {}
    }

    private void playBusyTone() {
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
            toneGenerator.startTone(ToneGenerator.TONE_SUP_BUSY, 2000);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDialTone();
        callTimeoutHandler.removeCallbacks(callTimeoutRunnable);
    }

    private void logCall(String direction, String status) {
        try {
            String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
            if (currentUserId == null) return;
            Map<String, Object> log = new HashMap<>();
            log.put("userId", currentUserId);
            log.put("callId", callId);
            log.put("otherName", receiverNameStr);
            log.put("otherUserId", getOtherUserId());
            log.put("isVideoCall", isVideoCall);
            log.put("direction", direction);
            log.put("status", status);
            log.put("timestamp", System.currentTimeMillis());
            if (receiverProfileBase64 != null && !receiverProfileBase64.isEmpty()) {
                log.put("profilePicBase64", receiverProfileBase64);
            }
            String logDocId = currentUserId + "_" + callId;
            db.collection("call_logs").document(logDocId).set(log, SetOptions.merge());
        } catch (Exception ignored) {}
    }

    private String getOtherUserId() {
        if (getIntent() != null) {
            return getIntent().getStringExtra("receiverId");
        }
        return null;
    }

    private static class SimpleSdpObserver implements SdpObserver {
        @Override public void onCreateSuccess(SessionDescription sdp) {}
        @Override public void onSetSuccess() {}
        @Override public void onCreateFailure(String s) { Log.e(TAG, "SDP create fail: " + s); }
        @Override public void onSetFailure(String s) { Log.e(TAG, "SDP set fail: " + s); }
    }
}
