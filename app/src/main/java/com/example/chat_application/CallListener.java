package com.example.chat_application;

import android.content.Context;
import android.content.Intent;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class CallListener {

    private ListenerRegistration reg;

    public void startListening(String receiverPhone, Context ctx) {

        reg = FirebaseFirestore.getInstance()
                .collection("calls")
                .whereEqualTo("receiverPhone", receiverPhone)
                .whereEqualTo("status", "ringing")
                .addSnapshotListener((snap, err) -> {

                    if (snap == null || snap.isEmpty()) return;

                    for (DocumentSnapshot d : snap.getDocuments()) {

                        Intent i = new Intent(ctx, IncomingCall.class);
                        i.putExtra("callId", d.getId());
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        ctx.startActivity(i);
                    }
                });
    }

    public void stop() {
        if (reg != null) reg.remove();
    }
}
