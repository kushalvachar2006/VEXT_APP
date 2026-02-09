package com.example.chat_application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class CallAdapter extends RecyclerView.Adapter<CallAdapter.CallViewHolder> {

    private List<CallItem> callList;
    private Context context;

    public CallAdapter(Context context, List<CallItem> callList) {
        this.context = context;
        this.callList = callList;
    }

    @NonNull
    @Override
    public CallViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.calls_container_page, parent, false);
        return new CallViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CallViewHolder holder, int position) {
        CallItem item = callList.get(position);

        if ("Unknown".equalsIgnoreCase(item.getCallName()) && item.getOtherUserId() != null) {
            FirebaseFirestore.getInstance().collection("users").document(item.getOtherUserId()).get()
                    .addOnSuccessListener(userDoc -> {
                        String name = userDoc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            holder.callName.setText(name);
                            item.setCallName(name); // Update item for consistency
                        }
                    });
        } else {
            holder.callName.setText(item.getCallName());
        }

        holder.callStatus.setText(
                getDisplayStatus(item.getCallStatus())
        );

        boolean loaded = false;
        if (item.getProfilePicBase64() != null && !item.getProfilePicBase64().isEmpty()) {
            try {
                byte[] decoded = Base64.decode(item.getProfilePicBase64(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                Glide.with(holder.itemView.getContext())
                        .load(bitmap)
                        .placeholder(R.drawable.profile_icon)
                        .error(R.drawable.profile_icon)
                        .circleCrop()
                        .into(holder.profileicon);
                loaded = true;
            } catch (Exception ignored) {}
        }

        String otherUserId = item.getOtherUserId();
        if (!loaded && otherUserId != null && !otherUserId.isEmpty()) {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(otherUserId)
                    .child("profilePicBase64");
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String base64 = snapshot.getValue(String.class);
                    if (base64 != null && !base64.isEmpty()) {
                        item.setProfilePicBase64(base64); // Cache for future use
                        try {
                            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                            Glide.with(holder.itemView.getContext())
                                    .load(bitmap)
                                    .placeholder(R.drawable.profile_icon)
                                    .error(R.drawable.profile_icon)
                                    .circleCrop()
                                    .into(holder.profileicon);
                        } catch (Exception e) {
                            holder.profileicon.setImageResource(R.drawable.profile_icon);
                        }
                    } else {
                        holder.profileicon.setImageResource(R.drawable.profile_icon);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    holder.profileicon.setImageResource(R.drawable.profile_icon);
                }
            });
        }

        holder.newcall.setOnClickListener(v -> {
            Animation slideIn = AnimationUtils.loadAnimation(context, R.anim.slide_in);
            holder.callinfolayout.startAnimation(slideIn);
            holder.callinfolayout.setVisibility(View.VISIBLE);
        });

        holder.itemView.setOnClickListener(v -> {
            if (holder.callinfolayout.getVisibility() == View.VISIBLE) {
                Animation slideOut = AnimationUtils.loadAnimation(context, R.anim.slide_out);
                holder.callinfolayout.startAnimation(slideOut);
                slideOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        holder.callinfolayout.setVisibility(View.GONE);
                    }
                    @Override public void onAnimationRepeat(Animation animation) {}
                });
            }
        });

        if (otherUserId != null && !otherUserId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(otherUserId).get()
                    .addOnSuccessListener(userDoc -> {
                        String phone = userDoc.getString("phone");
                        View.OnClickListener voiceClickListener = v -> CallLauncher.startCall(context, otherUserId, item.getCallName(), phone, false);
                        holder.voice_layout.setOnClickListener(voiceClickListener);
                        holder.voicecall.setOnClickListener(voiceClickListener);
                        holder.voicecalltxt.setOnClickListener(voiceClickListener);

                        View.OnClickListener videoClickListener = v -> CallLauncher.startCall(context, otherUserId, item.getCallName(), phone, true);
                        holder.video_layout.setOnClickListener(videoClickListener);
                        holder.videocall.setOnClickListener(videoClickListener);
                        holder.videocalltxt.setOnClickListener(videoClickListener);
                    });
        }
    }

    @Override
    public int getItemCount() {
        return callList != null ? callList.size() : 0;
    }
    private String getDisplayStatus(String status) {

        if (status == null) return "Unknown";

        switch (status.toLowerCase()) {

            case "completed":
                return "Completed";

            case "missed":
                return "Missed";

            case "declined":
                return "Declined";

            case "cancelled":
                return "Cancelled";

            case "failed":
                return "Failed";

            default:
                return status;
        }
    }


    public static class CallViewHolder extends RecyclerView.ViewHolder {
        ImageView profileicon;
        TextView callName, callStatus, voicecalltxt, videocalltxt;
        ImageButton newcall, voicecall, videocall;
        LinearLayout voice_layout, video_layout;
        CardView callinfolayout;

        public CallViewHolder(@NonNull View itemView) {
            super(itemView);
            profileicon = itemView.findViewById(R.id.call_icon);
            callName = itemView.findViewById(R.id.call_name);
            callStatus = itemView.findViewById(R.id.call_status);
            newcall = itemView.findViewById(R.id.call_icon_btn);
            voicecall = itemView.findViewById(R.id.voicecall_icon);
            videocall = itemView.findViewById(R.id.videocall_icon);
            voice_layout = itemView.findViewById(R.id.voicecall_layout);
            video_layout = itemView.findViewById(R.id.videocall_layout);
            callinfolayout = itemView.findViewById(R.id.callinfo_layout);
            voicecalltxt = itemView.findViewById(R.id.text_voicecall);
            videocalltxt = itemView.findViewById(R.id.text_videocall);
        }
    }
}
