package com.example.chat_application;

import android.content.Context;
import android.util.Log;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class WebRTCHelper {

    private static final String TAG = "WebRTCHelper";

    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private MediaStream localStream;

    private VideoCapturer videoCapturer;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;

    private EglBase eglBase;
    private final Context context;
    private final boolean isVideoCall;

    public SurfaceViewRenderer localView, remoteView;

    public WebRTCHelper(Context context,
                        SurfaceViewRenderer localView,
                        SurfaceViewRenderer remoteView,
                        boolean isVideoCall) {

        this.context = context;
        this.localView = localView;
        this.remoteView = remoteView;
        this.isVideoCall = isVideoCall;

        initializePeerConnectionFactory();
        createLocalStream();
    }

    // ✔ Initialize WebRTC factory and EGL context
    private void initializePeerConnectionFactory() {

        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .setEnableInternalTracer(true)
                        .createInitializationOptions();

        PeerConnectionFactory.initialize(initializationOptions);

        eglBase = EglBase.create();

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        DefaultVideoEncoderFactory encoderFactory =
                new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true);

        DefaultVideoDecoderFactory decoderFactory =
                new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());

        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
    }

    // ✔ Create local media stream (audio + video)
    private void createLocalStream() {

        localStream = factory.createLocalMediaStream("local_stream");

        AudioSource audioSource = factory.createAudioSource(new MediaConstraints());
        localAudioTrack = factory.createAudioTrack("AUDIO1", audioSource);

        localStream.addTrack(localAudioTrack);

        if (isVideoCall) {
            videoCapturer = createCameraCapturer();
            if (videoCapturer != null) {

                VideoSource videoSource =
                        factory.createVideoSource(videoCapturer.isScreencast());

                videoCapturer.initialize(
                        SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext()),
                        context,
                        videoSource.getCapturerObserver()
                );

                localVideoTrack = factory.createVideoTrack("VIDEO1", videoSource);
                localStream.addTrack(localVideoTrack);

            } else {
                Log.e(TAG, "⚠️ No video capturer found");
            }
        }
    }

    // ✔ Try front camera first
    private VideoCapturer createCameraCapturer() {

        Camera1Enumerator enumerator = new Camera1Enumerator(false);

        for (String device : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(device)) {
                VideoCapturer cap = enumerator.createCapturer(device, null);
                if (cap != null) return cap;
            }
        }

        for (String device : enumerator.getDeviceNames()) {
            VideoCapturer cap = enumerator.createCapturer(device, null);
            if (cap != null) return cap;
        }

        return null;
    }

    // ✔ Start the local preview
    public void startLocalVideo() {

        if (!isVideoCall) return;

        try {
            localView.init(eglBase.getEglBaseContext(), null);
            localView.setMirror(true);

            if (localVideoTrack != null) {
                localVideoTrack.addSink(localView);
                videoCapturer.startCapture(640, 480, 30);
                Log.d(TAG, "Local video started");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error starting local video", e);
        }
    }

    // ✔ Create peer connection and add stream
    public void setupPeerConnection(PeerConnection.Observer observer) {

        List<PeerConnection.IceServer> servers = new ArrayList<>();
        servers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        peerConnection = factory.createPeerConnection(servers, observer);

        if (peerConnection == null) {
            Log.e(TAG, " Failed to create PeerConnection");
            return;
        }

        for (AudioTrack track : localStream.audioTracks) {
            peerConnection.addTrack(track);
        }
        for (VideoTrack track : localStream.videoTracks) {
            peerConnection.addTrack(track);
        }


        Log.d(TAG, "✔ PeerConnection created and stream added");
    }

    // ✔ Attach remote stream to renderer
    public void attachRemoteStream(MediaStream remoteStream) {

        if (!isVideoCall || remoteStream.videoTracks.isEmpty()) {
            return;
        }

        try {
            VideoTrack remoteTrack = remoteStream.videoTracks.get(0);

            remoteView.init(eglBase.getEglBaseContext(), null);
            remoteTrack.addSink(remoteView);

            Log.d(TAG, "🎬 Remote video attached");

        } catch (Exception e) {
            Log.e(TAG, "⚠️ Remote video attach error", e);
        }
    }

    // ✔ Toggle mute
    public void setMute(boolean mute) {

        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(!mute);
        }
    }

    // ✔ Return local audio track reference
    public AudioTrack getAudioTrack() {

        return localAudioTrack;
    }

    // ✔ Stop camera, dispose tracks and EGL safely
    private boolean stopped = false;

    public synchronized void stop() {
        if (stopped) {
            Log.w(TAG, "stop() already called, ignoring");
            return;
        }
        stopped = true;

        Log.d(TAG, "🧹 Stopping WebRTC safely...");

        try {
            // Stop camera safely
            if (videoCapturer != null) {
                try {
                    videoCapturer.stopCapture();
                } catch (Exception ignored) {}
                try {
                    videoCapturer.dispose();
                } catch (Exception ignored) {}
                videoCapturer = null;
            }

            // Remove sinks before releasing views
            if (localVideoTrack != null && localView != null) {
                localVideoTrack.removeSink(localView);
            }

            if (remoteView != null) {
                try {
                    remoteView.release();
                } catch (Exception ignored) {}
                remoteView = null;
            }

            if (localView != null) {
                try {
                    localView.release();
                } catch (Exception ignored) {}
                localView = null;
            }

            // Close peer connection safely
            if (peerConnection != null) {
                try {
                    peerConnection.close();
                    peerConnection.dispose();
                } catch (Exception ignored) {}
                peerConnection = null;
            }

            // Audio track cleanup
            if (localAudioTrack != null) {
                try {
                    localAudioTrack.dispose();
                } catch (Exception ignored) {}
                localAudioTrack = null;
            }

            // Video track cleanup
            if (localVideoTrack != null) {
                try {
                    localVideoTrack.dispose();
                } catch (Exception ignored) {}
                localVideoTrack = null;
            }

            // Release EGL last
            if (eglBase != null) {
                try {
                    eglBase.release();
                } catch (Exception ignored) {}
                eglBase = null;
            }

            //  Do NOT dispose factory here — reuse is safer
            // if (factory != null) factory.dispose();

            Log.d(TAG, "WebRTC cleaned safely");

        } catch (Exception e) {
            Log.e(TAG, " WebRTC stop crash prevented", e);
        }
    }


    public PeerConnection getPeerConnection() {
        return peerConnection;
    }

    // ✔ Allow switching front/back camera
    public void switchCamera() {

        if (videoCapturer instanceof org.webrtc.CameraVideoCapturer) {

            org.webrtc.CameraVideoCapturer cam =
                    (org.webrtc.CameraVideoCapturer) videoCapturer;

            cam.switchCamera(null);
        }
    }

    // ⭐ Needed for Call_layout remote video init
    public EglBase.Context getEglContext() {
        return eglBase.getEglBaseContext();
    }
}
