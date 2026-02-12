package com.example.chat_application;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class IncomingCallService extends Service {

    private static final String TAG = "IncomingCallService";
    private static final String CHANNEL_ID = "call_notifications";
    private static final int NOTIFICATION_ID = 999;

    private Ringtone ringtone;
    private ListenerRegistration callStatusListener;
    private String currentCallId;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.e(TAG, "Intent is null, stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }

        String callId = intent.getStringExtra("callId");
        String callerName = intent.getStringExtra("callerName");
        String callerProfile = intent.getStringExtra("callerProfile");
        boolean isVideo = intent.getBooleanExtra("isVideoCall", true);

        if (callId == null) {
            Log.e(TAG, "No callId provided");
            stopSelf();
            return START_NOT_STICKY;
        }

        currentCallId = callId;
        Log.d(TAG, "Starting foreground service for call: " + callId);

        createNotificationChannel();


        Notification notification = buildCallNotification(callId, callerName, callerProfile, isVideo);
        startForeground(NOTIFICATION_ID, notification);


        launchIncomingCallActivity(callId, callerName, callerProfile, isVideo);


        playRingtone();


        listenForCallStatus(callId);


        new Handler().postDelayed(() -> {
            Log.d(TAG, "Call timeout - stopping service");
            stopSelfAndCleanup();
        }, 60000);

        return START_NOT_STICKY;
    }

    private Notification buildCallNotification(String callId, String callerName,
                                               String callerProfile, boolean isVideo) {
        int notificationId = callId.hashCode();


        Intent fullScreenIntent = new Intent(this, IncomingCall.class);
        fullScreenIntent.putExtra("callId", callId);
        fullScreenIntent.putExtra("callerName", callerName);
        fullScreenIntent.putExtra("callerProfile", callerProfile);
        fullScreenIntent.putExtra("isVideoCall", isVideo);
        fullScreenIntent.putExtra("notificationId", notificationId);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                notificationId,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );


        Intent acceptIntent = new Intent(this, CallActionReceiver.class);
        acceptIntent.setAction("ACCEPT_CALL");
        acceptIntent.putExtra("callId", callId);
        acceptIntent.putExtra("callerName", callerName);
        acceptIntent.putExtra("callerProfile", callerProfile);
        acceptIntent.putExtra("isVideoCall", isVideo);
        acceptIntent.putExtra("notificationId", notificationId);

        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId + 1,
                acceptIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );


        Intent declineIntent = new Intent(this, CallActionReceiver.class);
        declineIntent.setAction("DECLINE_CALL");
        declineIntent.putExtra("callId", callId);
        declineIntent.putExtra("notificationId", notificationId);

        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId + 2,
                declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );


        Person person = new Person.Builder()
                .setName(callerName != null ? callerName : "Incoming Call")
                .setImportant(true)
                .build();


        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_only)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setStyle(NotificationCompat.CallStyle.forIncomingCall(
                        person,
                        declinePendingIntent,
                        acceptPendingIntent
                ))
                .build();
    }

    private void launchIncomingCallActivity(String callId, String callerName,
                                            String callerProfile, boolean isVideo) {
        Intent activityIntent = new Intent(this, IncomingCall.class);
        activityIntent.putExtra("callId", callId);
        activityIntent.putExtra("callerName", callerName);
        activityIntent.putExtra("callerProfile", callerProfile);
        activityIntent.putExtra("isVideoCall", isVideo);
        activityIntent.putExtra("notificationId", callId.hashCode());
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(activityIntent);
        Log.d(TAG, "✅ Launched IncomingCall activity");
    }

    private void playRingtone() {
        try {
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            }

            ringtone.play();
            Log.d(TAG, "Ringtone started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to play ringtone", e);
        }
    }

    private void stopRingtone() {
        try {
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop();
                ringtone = null;
                Log.d(TAG, "Ringtone stopped");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop ringtone", e);
        }
    }

    private void listenForCallStatus(String callId) {
        callStatusListener = FirebaseFirestore.getInstance()
                .collection("calls")
                .document(callId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to call status", error);
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        Log.d(TAG, "Call document deleted - stopping service");
                        stopSelfAndCleanup();
                        return;
                    }

                    String status = snapshot.getString("status");
                    if (status != null) {
                        Log.d(TAG, "Call status changed: " + status);

                        if ("accepted".equals(status) || "rejected".equals(status) ||
                                "ended".equals(status)) {
                            Log.d(TAG, "Call " + status + " - stopping service");
                            stopSelfAndCleanup();
                        }
                    }
                });
    }

    private void stopSelfAndCleanup() {
        stopRingtone();

        if (callStatusListener != null) {
            callStatusListener.remove();
            callStatusListener = null;
        }

        stopForeground(true);
        stopSelf();
        Log.d(TAG, "Service stopped and cleaned up");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build();

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Incoming call notifications");
            channel.setSound(ringtoneUri, audioAttributes);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        stopRingtone();

        if (callStatusListener != null) {
            callStatusListener.remove();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}