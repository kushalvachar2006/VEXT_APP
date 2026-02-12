package com.example.chat_application;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> implements Filterable {
    public static boolean isChatOpen = false;

    private final List<ChatItem> chatListFull;
    public static List<ChatItem> chatList;
    public static ChatAdapter instance;

    public static String receiverphonenumber;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final DatabaseReference realtimeDb = FirebaseDatabase.getInstance().getReference("users");
    private ListenerRegistration chatListener;
    private HashMap<String, String> contactMap = new HashMap<>();
    private final HashMap<String, String> profileCache = new HashMap<>();


    public ChatAdapter(List<ChatItem> chatList) {
        this.chatListFull = new ArrayList<>(chatList);
        ChatAdapter.chatList = chatList;
        ChatAdapter.instance = this;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chats_container_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatItem item = chatList.get(position);
        holder.chatTitle.setText(item.getName());
        holder.chatPreview.setText(item.getLastMessage());
        String displayName = item.getName();
        String currentUserId = FirebaseAuth.getInstance().getUid();
        String receiverId = item.getReceiverId();
        if (receiverId != null && receiverId.equals(currentUserId)) {
            if (!displayName.endsWith(" (You)")) {
                displayName = displayName + " (You)";
            }
        }
        if (receiverId != null && !receiverId.isEmpty()) {
            String chatId = receiverId.compareTo(currentUserId) < 0 ?
                    receiverId + "_" + currentUserId :
                    currentUserId + "_" + receiverId;

            FirebaseFirestore.getInstance()
                    .collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .whereEqualTo("receiverId", currentUserId)
                    .whereEqualTo("seen", false)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null) return;

                        DatabaseReference presRef = FirebaseDatabase.getInstance()
                                .getReference("presence").child(receiverId);

                        presRef.get().addOnSuccessListener(presSnap -> {
                            boolean receiverInChatWithMe = false;
                            if (presSnap.exists()) {
                                String state = presSnap.child("state").getValue(String.class);
                                String currentChat = presSnap.child("currentChat").getValue(String.class);
                                if ("online".equals(state) && currentUserId.equals(currentChat)) {
                                    receiverInChatWithMe = true;
                                }
                            }

                            if ((ChatAdapter.isChatOpen && receiverId.equals(Message_layout.currentOpenChatUserId))
                                    || receiverInChatWithMe) {
                                holder.unseenmessages.setVisibility(View.GONE);
                                holder.chatTitle.setTypeface(null, Typeface.NORMAL);
                                return;
                            }

                            if (snapshot != null && !snapshot.isEmpty()) {
                                int count = snapshot.size();
                                holder.unseenmessages.setText(String.valueOf(count));
                                holder.unseenmessages.setVisibility(View.VISIBLE);
                                holder.chatTitle.setTypeface(null, Typeface.BOLD);
                            } else {
                                holder.unseenmessages.setVisibility(View.GONE);
                                holder.chatTitle.setTypeface(null, Typeface.NORMAL);
                            }
                        }).addOnFailureListener(ex -> {

                            if (ChatAdapter.isChatOpen && receiverId.equals(Message_layout.currentOpenChatUserId)) {
                                holder.unseenmessages.setVisibility(View.GONE);
                                holder.chatTitle.setTypeface(null, Typeface.NORMAL);
                                return;
                            }
                            if (snapshot != null && !snapshot.isEmpty()) {
                                int count = snapshot.size();
                                holder.unseenmessages.setText(String.valueOf(count));
                                holder.unseenmessages.setVisibility(View.VISIBLE);
                                holder.chatTitle.setTypeface(null, Typeface.BOLD);
                            } else {
                                holder.unseenmessages.setVisibility(View.GONE);
                                holder.chatTitle.setTypeface(null, Typeface.NORMAL);
                            }
                        });

                    });
        }

        String base64 = item.getIconResId();
        Context context = holder.itemView.getContext();
        String receiverIdForPic = item.getReceiverId();


        if (base64 != null && !base64.isEmpty()) {

            try {
                byte[] decoded = Base64.decode(base64, Base64.DEFAULT);

                Glide.with(context)
                        .asBitmap()
                        .load(decoded)
                        .placeholder(R.drawable.profile_icon)
                        .circleCrop()
                        .into(holder.chatIcon);

            } catch (Exception e) {
                holder.chatIcon.setImageResource(R.drawable.profile_icon);
            }

        }

        else {
            SharedPreferences prefs =
                    context.getSharedPreferences("UserCache", Context.MODE_PRIVATE);

            String cachedPic =
                    prefs.getString(receiverIdForPic + "_profile_pic", null);

            if (cachedPic != null && !cachedPic.isEmpty()) {
                try {
                    byte[] decoded = Base64.decode(cachedPic, Base64.DEFAULT);

                    Glide.with(context)
                            .asBitmap()
                            .load(decoded)
                            .placeholder(R.drawable.profile_icon)
                            .circleCrop()
                            .into(holder.chatIcon);

                } catch (Exception e) {
                    holder.chatIcon.setImageResource(R.drawable.profile_icon);
                }

            }

            else {

                holder.chatIcon.setImageResource(R.drawable.profile_icon);

                if (receiverIdForPic != null && !receiverIdForPic.isEmpty()) {

                    FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(receiverIdForPic)
                            .child("profilePicBase64")
                            .get()
                            .addOnSuccessListener(snapshot -> {

                                if (!snapshot.exists()) return;

                                String fetchedBase64 = snapshot.getValue(String.class);
                                if (fetchedBase64 == null || fetchedBase64.isEmpty()) return;

                                try {
                                    byte[] decoded = Base64.decode(fetchedBase64, Base64.DEFAULT);

                                    Glide.with(context)
                                            .asBitmap()
                                            .load(decoded)
                                            .placeholder(R.drawable.profile_icon)
                                            .circleCrop()
                                            .into(holder.chatIcon);

                                    ((SharedPreferences) prefs).edit()
                                            .putString(receiverIdForPic + "_profile_pic", fetchedBase64)
                                            .apply();

                                } catch (Exception ignored) {}
                            });
                }
            }
        }

        String chatId = receiverId.compareTo(currentUserId) < 0 ?
                receiverId + "_" + currentUserId :
                currentUserId + "_" + receiverId;

        holder.chatinfo.setOnClickListener(v -> {
            if (receiverId == null || receiverId.trim().isEmpty()) {
                Toast.makeText(v.getContext(), "Cannot open chat: invalid user", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseFirestore.getInstance().collection("users")
                    .document(receiverId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String receiverPhone = (doc.exists() && doc.getString("phone") != null)
                                ? doc.getString("phone") : "Unknown";

                        receiverphonenumber = receiverPhone;
                        if (item.getReceiverId() == null || item.getReceiverId().trim().isEmpty()) {
                            Toast.makeText(v.getContext(), "Chat unavailable", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        openMessageLayout(context, item, receiverPhone);

                        db.collection("chats").document(chatId)
                                .update("displayNames." + currentUserId + "." + receiverId, item.getName()).addOnCompleteListener(task -> {
                                })
                                .addOnFailureListener(e -> {
                                });
                        db.collection("users")
                                .document(receiverId)
                                .update("contactname", item.getName());

                    })
                    .addOnFailureListener(e -> {
                        openMessageLayout(context, item, "Unknown");
                    });
        });

        holder.chatIcon.setOnClickListener(v -> {
            if (context instanceof MainPage) {
                ((MainPage) context).showContactOverlay(item.getName(), item.getReceiverId(), item.getIconResId());
            }
        });
    }
    public void updateFullList(List<ChatItem> newList) {
        chatListFull.clear();
        chatListFull.addAll(newList);
    }

    @Override
    public Filter getFilter() {
        return chatFilter;
    }

    private final Filter chatFilter = new Filter() {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<ChatItem> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(chatListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (ChatItem item : chatListFull) {
                    if (item.getName() != null &&
                            item.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            chatList.clear();
            chatList.addAll((List<ChatItem>) results.values);
            notifyDataSetChanged();
        }
    };


    private void openMessageLayout(Context context, ChatItem item, String receiverPhone) {

        Intent intent = new Intent(context, Message_layout.class);
        intent.putExtra("contactName", item.getName());
        intent.putExtra("contactPhone", receiverPhone);
        intent.putExtra("receiverId", item.getReceiverId());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }


    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView chatTitle, chatPreview;
        ImageView chatIcon;
        LinearLayout chatinfo;
        Button unseenmessages;

        ViewHolder(View itemView) {
            super(itemView);
            chatTitle = itemView.findViewById(R.id.chat_title);
            chatPreview = itemView.findViewById(R.id.chat_preview);
            chatIcon = itemView.findViewById(R.id.chat_icon);
            chatinfo = itemView.findViewById(R.id.chat_info);
            unseenmessages = itemView.findViewById(R.id.unseenmessages);
        }
    }
}
