package com.example.chat_application;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "chat_notifications";
    private static final String CALL_CHANNEL_ID = "call_notifications";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "✅ FCM Service created");
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "📩 Message received from: " + remoteMessage.getFrom());
        Log.d(TAG, "📦 Data payload: " + remoteMessage.getData());


        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "chatapp:FCMWakeLock"
        );
        wakeLock.acquire(10000);

        try {
            if (remoteMessage.getData().size() > 0) {
                String type = remoteMessage.getData().get("type");


                if (type != null && "incoming_call".equalsIgnoreCase(type)) {
                    String callId = remoteMessage.getData().get("callId");
                    String callerName = remoteMessage.getData().get("callerName");
                    String callerProfile = remoteMessage.getData().get("callerProfile");
                    String isVideoStr = remoteMessage.getData().get("isVideoCall");
                    boolean isVideo = "true".equalsIgnoreCase(isVideoStr) || "1".equals(isVideoStr);

                    Log.d(TAG, "📞 Incoming call - callId: " + callId + ", caller: " + callerName + ", video: " + isVideo);

                    if (callId != null && !callId.isEmpty()) {
                        handleIncomingCall(callId, callerName, callerProfile, isVideo);
                    } else {
                        Log.e(TAG, "❌ No callId in FCM message");
                    }
                    return;
                }


                String senderId = remoteMessage.getData().get("notificationUid");
                String senderName = remoteMessage.getData().get("senderName");
                String chatId = remoteMessage.getData().get("notificationChatId");
                String messageText = remoteMessage.getData().get("body");

                if (remoteMessage.getNotification() != null) {
                    if (senderName == null || senderName.isEmpty()) {
                        senderName = remoteMessage.getNotification().getTitle();
                    }
                    if (messageText == null || messageText.isEmpty()) {
                        messageText = remoteMessage.getNotification().getBody();
                    }
                }
                showMessageNotification(senderName, messageText, chatId, senderId);
            }

            if (remoteMessage.getNotification() != null) {
                Log.d(TAG, "Notification body: " + remoteMessage.getNotification().getBody());
            }
        } finally {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private void handleIncomingCall(String callId, String callerName, String callerProfile, boolean isVideo) {
        Log.d(TAG, "🚀 Handling incoming call");

        try {

            Intent serviceIntent = new Intent(this, IncomingCallService.class);
            serviceIntent.putExtra("callId", callId);
            serviceIntent.putExtra("callerName", callerName);
            serviceIntent.putExtra("callerProfile", callerProfile);
            serviceIntent.putExtra("isVideoCall", isVideo);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            Log.d(TAG, "✅ IncomingCallService started");

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start IncomingCallService", e);


            showIncomingCallNotificationDirect(callId, callerName, callerProfile, isVideo);
        }
    }


    private void showIncomingCallNotificationDirect(String callId, String callerName,
                                                    String callerProfile, boolean isVideo) {
        Log.d(TAG, "📱 Showing call notification directly (fallback)");

        createCallNotificationChannel();

        int notificationId = callId != null
                ? Math.abs(callId.hashCode())
                : (int) (System.currentTimeMillis() % Integer.MAX_VALUE);


        Intent fullScreenIntent = new Intent(this, IncomingCall.class);
        fullScreenIntent.putExtra("callId", callId);
        fullScreenIntent.putExtra("callerName", callerName);
        fullScreenIntent.putExtra("callerProfile", callerProfile);
        fullScreenIntent.putExtra("isVideoCall", isVideo);
        fullScreenIntent.putExtra("notificationId", notificationId);
        fullScreenIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

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


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CALL_CHANNEL_ID)
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
                ));

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(notificationId);
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "✅ Call notification posted with ID: " + notificationId);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed FCM token: " + token);

    }

    private void showMessageNotification(String title, String body, String chatId, String senderId) {
        if (Message_layout.AppState.isInMessageLayout) {
            Log.d(TAG, "Notification suppressed - user is in MessageLayout");
            return;
        }

        Log.d(TAG, "Showing message notification - title: " + title);
        createMessageNotificationChannel();

        Intent intent = new Intent(this, IntroPage.class);
        intent.putExtra("notificationUid", senderId);
        intent.putExtra("notificationChatId", chatId);
        intent.putExtra("openFromNotification", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int requestCode = (senderId != null) ? senderId.hashCode() : (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_only)
                .setContentTitle(title != null ? title : "New Message")
                .setContentText(body != null ? body : "")
                .setAutoCancel(true)
                .setSound(soundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(requestCode, builder.build());
            Log.d(TAG, "Message notification posted");
        }
    }

    private void createMessageNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Chat Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Shows new chat messages");
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void createCallNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build();

            NotificationChannel channel = new NotificationChannel(
                    CALL_CHANNEL_ID,
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
                Log.d(TAG, "Call notification channel created");
            }
        }
    }
}