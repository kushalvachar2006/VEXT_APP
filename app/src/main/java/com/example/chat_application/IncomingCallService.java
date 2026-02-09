package com.example.chat_application;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class IncomingCallService extends Service {

    private static final String CHANNEL_ID = "call_notifications";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String callId = intent.getStringExtra("callId");
        String callerName = intent.getStringExtra("callerName");
        boolean isVideo = intent.getBooleanExtra("isVideoCall", true);

        createNotificationChannel();

        Intent fullScreenIntent = new Intent(this, IncomingCall.class);
        fullScreenIntent.putExtras(intent);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent fullScreenPendingIntent =
                PendingIntent.getActivity(
                        this,
                        callId.hashCode(),
                        fullScreenIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_only)
                .setContentTitle(callerName)
                .setContentText(isVideo ? "Incoming video call" : "Incoming voice call")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true)
                .setSound(ringtoneUri)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .build();

        startForeground(999, notification);

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build();

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setSound(ringtoneUri, audioAttributes);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
