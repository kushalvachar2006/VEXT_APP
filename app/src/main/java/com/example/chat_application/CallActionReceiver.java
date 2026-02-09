package com.example.chat_application;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;

public class CallActionReceiver extends BroadcastReceiver {
    private static final String TAG = "CallActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String callId = intent.getStringExtra("callId");
        int notificationId = intent.getIntExtra("notificationId", -1);

        Log.d(TAG, "Received action: " + action + ", callId: " + callId);

        if ("ACCEPT_CALL".equals(action) && callId != null) {
            //  ACCEPT: Navigate to Call_layout
            String callerName = intent.getStringExtra("callerName");
            String callerProfile = intent.getStringExtra("callerProfile");
            boolean isVideoCall = intent.getBooleanExtra("isVideoCall", false);

            Log.d(TAG, "Accepting call from notification");

            //  Dismiss notification
            dismissNotification(context, notificationId, callId);

            //  Update Firestore first
            FirebaseFirestore.getInstance()
                    .collection("calls")
                    .document(callId)
                    .update("status", "accepted")
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Call accepted via notification");

                        //  Launch Call_layout activity
                        Intent callIntent = new Intent(context, Call_layout.class);
                        callIntent.putExtra("callId", callId);
                        callIntent.putExtra("isCaller", false);
                        callIntent.putExtra("isVideoCall", isVideoCall);
                        callIntent.putExtra("receiverName", callerName);
                        callIntent.putExtra("receiverProfile", callerProfile);
                        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        context.startActivity(callIntent);
                    });

            //  Close IncomingCall activity if it's open
            Intent closeIntent = new Intent("CLOSE_INCOMING_CALL");
            closeIntent.putExtra("callId", callId);
            context.sendBroadcast(closeIntent);

        } else if ("DECLINE_CALL".equals(action) && callId != null) {
            //  DECLINE: Update Firestore and dismiss
            Log.d(TAG, "Declining call from notification");

            //  Update Firestore to reject the call
            FirebaseFirestore.getInstance()
                    .collection("calls")
                    .document(callId)
                    .update("status", "rejected")
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Call rejected via notification"))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to reject call", e));

            //  Dismiss the notification
            dismissNotification(context, notificationId, callId);

            //  Close IncomingCall activity if it's open
            Intent closeIntent = new Intent("CLOSE_INCOMING_CALL");
            closeIntent.putExtra("callId", callId);
            closeIntent.putExtra("action", "declined");
            context.sendBroadcast(closeIntent);
        }
    }

    private void dismissNotification(Context context, int notificationId, String callId) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            if (notificationId != -1) {
                notificationManager.cancel(notificationId);
            } else if (callId != null) {
                notificationManager.cancel(callId.hashCode());
            }
            Log.d(TAG, "Notification dismissed");
        }
    }
}