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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "chat_notifications";
    private static final String CALL_CHANNEL_ID = "call_notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            String type = remoteMessage.getData().get("type");

            //  Handle incoming call notification
            if (type != null && "incoming_call".equalsIgnoreCase(type)) {
                String callId = remoteMessage.getData().get("callId");
                String callerName = remoteMessage.getData().get("callerName");
                String callerProfile = remoteMessage.getData().get("callerProfile");
                String isVideoStr = remoteMessage.getData().get("isVideoCall");
                boolean isVideo = "true".equalsIgnoreCase(isVideoStr) || "1".equals(isVideoStr);

                Log.d(TAG, "Incoming call - callId: " + callId + ", caller: " + callerName + ", video: " + isVideo);
//                showIncomingCallNotification(callerName, isVideo, callId, callerProfile);
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

                return;
            }

            //  Handle regular message notification
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
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed FCM token: " + token);
        // TODO: Send token to your server or save to Firestore user document
    }

    // ============================================================================
    // MESSAGE NOTIFICATION (Chat messages)
    // ============================================================================

    private void showMessageNotification(String title, String body, String chatId, String senderId) {
        //  Don't show notification if user is already in the chat
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

    // ============================================================================
    // INCOMING CALL NOTIFICATION (Full-screen intent)
    // ============================================================================

    private void showIncomingCallNotification(String callerName, boolean isVideo, String callId, String callerProfile) {
        Log.d(TAG, "showIncomingCallNotification - callId: " + callId + ", caller: " + callerName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null && !nm.canUseFullScreenIntent()) {
                Log.w(TAG, "Full screen intent permission not granted");
                // Still show notification, but it won't be full-screen
            }
        }

        createCallNotificationChannel();

        //  CRITICAL FIX: Use a consistent but unique request code based on callId
        // This ensures each call gets a unique notification that can be triggered
        int notificationId = callId != null ? Math.abs(callId.hashCode()) : (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

        Log.d(TAG, "Using notification ID: " + notificationId);

        //  Full-screen intent to launch IncomingCall activity
        Intent fullScreenIntent = new Intent(this, IncomingCall.class);
        fullScreenIntent.putExtra("callId", callId);
        fullScreenIntent.putExtra("callerName", callerName);
        fullScreenIntent.putExtra("callerProfile", callerProfile);
        fullScreenIntent.putExtra("isVideoCall", isVideo);
        fullScreenIntent.putExtra("notificationId", notificationId);

        //  CRITICAL: These flags ensure the activity launches even when app is killed
        fullScreenIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        //  CRITICAL FIX: Use FLAG_IMMUTABLE for better compatibility on Android 12+
        // But use FLAG_MUTABLE for full-screen intents as they need to update extras
        int pendingIntentFlags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires explicit mutability declaration
            pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
        } else {
            pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
        }

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                notificationId,  //  Use the same ID for consistency
                fullScreenIntent,
                pendingIntentFlags
        );

        // Ringtone for incoming calls
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CALL_CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_only)
                .setContentTitle(callerName != null ? callerName : "Incoming Call")
                .setContentText(isVideo ? "Incoming video call" : "Incoming voice call")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)  //  Cannot be swiped away
                .setAutoCancel(false)
                .setSound(ringtoneUri)
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setFullScreenIntent(fullScreenPendingIntent, true)  //  CRITICAL for lockscreen
                .setContentIntent(fullScreenPendingIntent)
                .setTimeoutAfter(60000);  //  Auto-dismiss after 60 seconds

        //  Accept button - launches CallActionReceiver
        Intent acceptIntent = new Intent(this, CallActionReceiver.class);
        acceptIntent.setAction("ACCEPT_CALL");
        acceptIntent.putExtra("callId", callId);
        acceptIntent.putExtra("callerName", callerName);
        acceptIntent.putExtra("callerProfile", callerProfile);
        acceptIntent.putExtra("isVideoCall", isVideo);
        acceptIntent.putExtra("notificationId", notificationId);

        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId + 1,  //  Offset to avoid conflicts
                acceptIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        //  Decline button - launches CallActionReceiver
        Intent declineIntent = new Intent(this, CallActionReceiver.class);
        declineIntent.setAction("DECLINE_CALL");
        declineIntent.putExtra("callId", callId);
        declineIntent.putExtra("notificationId", notificationId);

        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId + 2,  //  Offset to avoid conflicts
                declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        //  Add action buttons to notification
        builder.addAction(R.drawable.accept_call_bg, "Accept", acceptPendingIntent);
        builder.addAction(R.drawable.reject_call_bg, "Decline", declinePendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            // CRITICAL: Cancel any existing notification with this ID first
            // This ensures a fresh notification is always created
            notificationManager.cancel(notificationId);

            //  Post the new notification
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Incoming call notification posted with ID: " + notificationId);
        } else {
            Log.e(TAG, "NotificationManager is null");
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
                    NotificationManager.IMPORTANCE_HIGH  //  HIGH is sufficient for full-screen intent
            );
            channel.setDescription("Incoming call notifications");
            channel.setSound(ringtoneUri, audioAttributes);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true);  //  Bypass Do Not Disturb for calls

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Call notification channel created");
            }
        }
    }
}