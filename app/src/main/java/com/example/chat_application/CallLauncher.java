package com.example.chat_application;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CallLauncher {

    public static void startCall(Context context,
                                 String receiverId,
                                 String receiverName,
                                 String receiverPhone,
                                 boolean isVideoCall) {

        if (receiverId == null) return;

        //  REMOVED: No longer fetching profile pic here
        Intent intent = new Intent(context, Call_layout.class);
        intent.putExtra("receiverId", receiverId);
        intent.putExtra("receiverName", receiverName);
        intent.putExtra("receiverPhone", receiverPhone);
        //  REMOVED: No longer passing base64 profile through Intent
        intent.putExtra("isVideoCall", isVideoCall);
        intent.putExtra("isCaller", true);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Log.d("CallLauncher", "Launching call screen");
        context.startActivity(intent);
    }
}