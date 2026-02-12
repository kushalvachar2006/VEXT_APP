package com.example.chat_application;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class IncomingCall extends AppCompatActivity {

    private static final String TAG = "IncomingCall";

    private ImageView callerImage;
    private TextView callerName, callTypeLabel;
    private Button btnAccept, btnReject;

    private FirebaseFirestore db;
    private DocumentReference callDocRef;
    private ListenerRegistration callStatusListener;

    private String callId;
    private boolean isVideoCall = true;
    private int notificationId = -1;

    private String callerUid = "";
    private String callerPhone = "";
    private String callerNameStr = "Unknown";
    private String callerProfileBase64 = "";

    private Ringtone ringtone;
    private BroadcastReceiver closeReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.incoming_call);

        db = FirebaseFirestore.getInstance();


        setupWindowFlags();


        processIntent(getIntent());

        bindViews();
        playRingtone();

        fetchCallerInfo();
        listenCallCancel();


        registerCloseReceiver();

        btnAccept.setOnClickListener(v -> acceptCall());
        btnReject.setOnClickListener(v -> rejectCall());
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent called");
        stopRingtone();


        if (callStatusListener != null) {
            callStatusListener.remove();
            callStatusListener = null;
        }


        setIntent(intent);
        processIntent(intent);


        playRingtone();


        fetchCallerInfo();
        listenCallCancel();
    }
    private void processIntent(Intent intent) {
        if (intent == null) {
            Log.e(TAG, "❌ No intent supplied — finishing");
            finish();
            return;
        }

        callId = intent.getStringExtra("callId");

        if (callId == null) {
            Log.e(TAG, "❌ No callId supplied — finishing");
            finish();
            return;
        }


        notificationId = intent.getIntExtra("notificationId", callId.hashCode());

        callDocRef = db.collection("calls").document(callId);


        if (intent.hasExtra("callerName")) {
            callerNameStr = intent.getStringExtra("callerName");
            if (callerName != null) callerName.setText(callerNameStr);
        }
        if (intent.hasExtra("isVideoCall")) {
            isVideoCall = intent.getBooleanExtra("isVideoCall", true);
            if (callTypeLabel != null) callTypeLabel.setText(isVideoCall ? "VIDEO CALL" : "VOICE CALL");
        }
        if (intent.hasExtra("callerProfile")) {
            callerProfileBase64 = intent.getStringExtra("callerProfile");
            updateProfile(callerProfileBase64);
        }

        Log.d(TAG, "Processing call - callId: " + callId + ", caller: " + callerNameStr);
    }


    private void setupWindowFlags() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
                getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().addFlags(
                        android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                );
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if (keyguardManager != null) {
                    keyguardManager.requestDismissKeyguard(this, null);
                }
            }
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null) {
                PowerManager.WakeLock wakeLock = pm.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK |
                                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                PowerManager.ON_AFTER_RELEASE,
                        "chatapp:IncomingCallWakeLock"
                );
                wakeLock.acquire(10 * 60 * 1000L);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to setup window flags", e);
        }
    }


    private void registerCloseReceiver() {
        closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getStringExtra("action");
                String receivedCallId = intent.getStringExtra("callId");

                if (callId != null && callId.equals(receivedCallId)) {
                    Log.d(TAG, "Received close broadcast: " + action);
                    stopRingtone();
                    dismissNotification();
                    finish();
                }
            }
        };

        IntentFilter filter = new IntentFilter("CLOSE_INCOMING_CALL");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(closeReceiver, filter);
        }
    }

    private void bindViews() {
        callerImage = findViewById(R.id.caller_image);
        callerName = findViewById(R.id.caller_name);
        callTypeLabel = findViewById(R.id.call_type_label);
        btnAccept = findViewById(R.id.btn_accept);
        btnReject = findViewById(R.id.btn_reject);
    }

    private void fetchCallerInfo() {
        callDocRef.get()
                .addOnSuccessListener(this::populateCallerUI)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load caller info", e));
    }

    private void populateCallerUI(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Log.w(TAG, "Call document doesn't exist");
            finish();
            return;
        }


        Boolean videoFlag = doc.getBoolean("isVideoCall");
        isVideoCall = videoFlag != null && videoFlag;
        callTypeLabel.setText(isVideoCall ? "VIDEO CALL" : "VOICE CALL");


        callerPhone = doc.getString("callerPhone");
        callerUid = doc.getString("callerId");


        String explicitCallerName = doc.getString("callerName");
        if (explicitCallerName != null && !explicitCallerName.isEmpty()) {
            callerNameStr = explicitCallerName;
        } else if (callerPhone != null) {
            String resolvedName = ContactAdapter.resolveName(callerPhone);
            if (resolvedName != null) callerNameStr = resolvedName;
            else callerNameStr = callerPhone;
        }
        callerName.setText(callerNameStr);


        if ((callerPhone == null || callerPhone.isEmpty()) && callerUid != null && !callerUid.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(callerUid)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        String phone = userDoc.getString("phone");
                        if (phone != null && !phone.isEmpty()) {
                            callerPhone = phone;
                            String resolved = ContactAdapter.resolveName(phone);
                            if (resolved != null && !resolved.isEmpty()) {
                                callerNameStr = resolved;
                                callerName.setText(callerNameStr);
                            }
                        }
                    });
        }


        if (callerUid != null && !callerUid.isEmpty()) {
            FirebaseDatabase.getInstance().getReference("users")
                    .child(callerUid)
                    .child("profilePicBase64")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String base64 = snapshot.getValue(String.class);
                        this.callerProfileBase64 = base64;
                        updateProfile(base64);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch photo from realtime DB", e);
                        callerImage.setImageResource(R.drawable.profile_icon);
                    });
        } else {
            callerImage.setImageResource(R.drawable.profile_icon);
        }
    }

    private void updateProfile(String base64) {
        if (base64 != null && !base64.isEmpty()) {
            try {
                byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                Glide.with(this)
                        .load(bitmap)
                        .circleCrop()
                        .into(callerImage);
            } catch (Exception ex) {
                Log.e(TAG, "Failed to decode profile image", ex);
                callerImage.setImageResource(R.drawable.profile_icon);
            }
        } else {
            callerImage.setImageResource(R.drawable.profile_icon);
        }
    }


    private void listenCallCancel() {
        callStatusListener = callDocRef.addSnapshotListener((snap, e) -> {
            if (e != null) {
                Log.e(TAG, "Error listening to call status", e);
                return;
            }

            if (snap == null || !snap.exists()) {
                Log.d(TAG, "Call document deleted — caller ended call");
                stopRingtone();
                dismissNotification();
                finish();
                return;
            }

            String status = snap.getString("status");
            if (status != null) {
                Log.d(TAG, "Call status: " + status);


                if ("ended".equals(status) || "rejected".equals(status)) {
                    Log.d(TAG, "Call was cancelled by caller");
                    stopRingtone();
                    dismissNotification();
                    finish();
                }
            }
        });
    }

    private void playRingtone() {
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            }

            ringtone.play();
            Log.d(TAG, "Ringtone started");

        } catch (Exception e) {
            Log.e(TAG, "Ringtone failed", e);
        }
    }

    private void stopRingtone() {
        try {
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop();
                Log.d(TAG, "Ringtone stopped");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop ringtone", e);
        }
    }


    private void dismissNotification() {
        try {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null && notificationId != -1) {
                notificationManager.cancel(notificationId);
                Log.d(TAG, "Notification dismissed: " + notificationId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to dismiss notification", e);
        }
    }


    private void acceptCall() {
        Log.d(TAG, "Call accepted");

        stopRingtone();
        dismissNotification();
        Intent serviceIntent = new Intent(this, IncomingCallService.class);
        stopService(serviceIntent);


        callDocRef.update("status", "accepted")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Call status updated to accepted");


                    Intent intent = new Intent(this, Call_layout.class);
                    intent.putExtra("isCaller", false);
                    intent.putExtra("isVideoCall", isVideoCall);
                    intent.putExtra("callId", callId);
                    intent.putExtra("receiverName", callerNameStr);
                    intent.putExtra("receiverId", callerUid);
                    intent.putExtra("receiverPhone", callerPhone);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to accept call", e);
                    finish();
                });
    }


    private void rejectCall() {
        Log.d(TAG, "Call rejected");

        stopRingtone();
        dismissNotification();
        Intent serviceIntent = new Intent(this, IncomingCallService.class);
        stopService(serviceIntent);


        callDocRef.update("status", "rejected")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Call status updated to rejected");
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to reject call", e);
                    finish();
                });
    }

    @Override
    protected void onDestroy() {
        stopRingtone();


        if (callStatusListener != null) {
            callStatusListener.remove();
            callStatusListener = null;
        }


        try {
            if (closeReceiver != null) {
                unregisterReceiver(closeReceiver);
                closeReceiver = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to unregister receiver", e);
        }

        super.onDestroy();
    }
}