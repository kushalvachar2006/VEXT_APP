package com.example.chat_application;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Message_layout extends AppCompatActivity {
    TextView username,username_info,userphone_info,userstatus,voicecall_text,videocall_text;
    ImageView profileview_info;
    Button exitbtn;
    ImageButton profilepic,sendbtn,back,deletebtn,callbtn,attachbtn,voicecall_icon,videocall_icon;
    EditText msginput;

    CardView contactinfolayout,headercard,callinfolayout;
    LinearLayout userinfolayout,voicecall,videocall;
    View view;
    RecyclerView recyclerView;
    MessageAdapter messageAdapter;
    List<MessageModel> messageList = new ArrayList<>();
    String currentUserId, receiverId, chatId;
    FirebaseFirestore db;
    DatabaseReference presence,currentuserref,connectedref;
    public static String contactname,phonenumber,currentname,name;
    //public static String profilepicbase64;
    private String profileCacheKey;

    public static String currentOpenChatUserId = null,lastSentChatId=null;

    //public static boolean isinMessageLayout=false;

    private static final int CAMERA_REQUEST = 101;
    private Uri imageUri;
    public static class AppState {
        public static boolean isInMessageLayout = false;
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_layout);

        username = findViewById(R.id.userName);
        profilepic = findViewById(R.id.profileImage);
        back = findViewById(R.id.backbtn);
        sendbtn = findViewById(R.id.sendBtn);
        msginput = findViewById(R.id.messageInput);
        recyclerView = findViewById(R.id.messagesRecyclerView);
        userinfolayout = findViewById(R.id.userInfoLayout);
        view = findViewById(R.id.shadowOverlay);
        contactinfolayout = findViewById(R.id.contactinfo_layout);
        exitbtn = findViewById(R.id.exitbtn);
        profileview_info = findViewById(R.id.profile_view);
        username_info = findViewById(R.id.username);
        userphone_info = findViewById(R.id.userphonenumber);
        userstatus = findViewById(R.id.userStatus);
        headercard = findViewById(R.id.headerCard);
        deletebtn = findViewById(R.id.deleteBtn);
        callbtn = findViewById(R.id.callbtnplus);
        callinfolayout = findViewById(R.id.callinfo_layout);
        attachbtn = findViewById(R.id.attachBtn);
        voicecall = findViewById(R.id.voicecall);
        videocall = findViewById(R.id.videocall);
        voicecall_icon = findViewById(R.id.voicecall_icon);
        videocall_icon = findViewById(R.id.videocall_icon);
        voicecall_text = findViewById(R.id.text_voicecall);
        videocall_text = findViewById(R.id.text_videocall);



        ChatAdapter.isChatOpen=true;
        db = FirebaseFirestore.getInstance();
        Intent intent = getIntent();
        contactname = intent.getStringExtra("contactName");
        phonenumber = intent.getStringExtra("contactPhone");
//        profilepicbase64 = intent.getStringExtra("profilePicBase64");
        currentUserId = FirebaseAuth.getInstance().getUid();
        receiverId = getIntent().getStringExtra("receiverId");
        receiverId = getIntent().getStringExtra("receiverId");

        if (receiverId == null || receiverId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid chat user", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        profileCacheKey = receiverId + "_profile_pic";

        currentOpenChatUserId = receiverId;

        chatId = receiverId.compareTo(currentUserId) < 0 ?
                receiverId + "_" + currentUserId :
                currentUserId + "_" + receiverId;

        username.setText(contactname);
        username_info.setText(contactname);
        userphone_info.setText(phonenumber);
        updateHeaderUI();
        view.bringToFront();


        SharedPreferences prefs = getSharedPreferences("UserCache", MODE_PRIVATE);
        String cachedBase64 = prefs.getString(profileCacheKey, null);

        if (cachedBase64 != null && !cachedBase64.isEmpty()) {
            try {
                byte[] decoded = Base64.decode(cachedBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);

                Glide.with(this).load(bitmap).circleCrop().into(profilepic);
                Glide.with(this).load(bitmap).circleCrop().into(profileview_info);

            } catch (Exception e) {
                Glide.with(this).load(R.drawable.profile_icon).circleCrop().into(profilepic);
                Glide.with(this).load(R.drawable.profile_icon).circleCrop().into(profileview_info);
            }
        } else {
            // No cache yet → show placeholder
            Glide.with(this).load(R.drawable.profile_icon).circleCrop().into(profilepic);
            Glide.with(this).load(R.drawable.profile_icon).circleCrop().into(profileview_info);
        }

// Step 2: Always fetch latest profile image from Firebase
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(receiverId)
                .child("profilePicBase64");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String freshBase64 = snapshot.getValue(String.class);
                if (freshBase64 == null || freshBase64.isEmpty()) return;

                // 🔄 Save to cache
                prefs.edit().putString(profileCacheKey, freshBase64).apply();

                try {
                    byte[] decoded = Base64.decode(freshBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);

                    Glide.with(Message_layout.this)
                            .load(bitmap)
                            .circleCrop()
                            .into(profilepic);

                    Glide.with(Message_layout.this)
                            .load(bitmap)
                            .circleCrop()
                            .into(profileview_info);

                } catch (Exception ignored) {}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(messageList, currentUserId);
        recyclerView.setAdapter(messageAdapter);

        // --- Send message ---
        sendbtn.setOnClickListener(v -> {
            //isinMessageLayout=true;
            lastSentChatId=chatId;
            String messageText = msginput.getText().toString().trim();
            if (messageText.isEmpty()) return;

            MessageModel message = new MessageModel(
                    currentUserId,
                    receiverId,
                    messageText,
                    System.currentTimeMillis()
            );
            message.setSeen(false);
            // Add message to messages collection
            db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(message)
                    .addOnSuccessListener(docRef -> {
                        msginput.setText("");
                        Log.d("Message_layout", "Message added to messages collection, id=" + docRef.getId());
                        scrollToBottomSmooth();
                        // Send notification (still async)
                        sendNotificationToDevice(receiverId, messageText, chatId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Message_layout", "Failed to add message to messages collection", e);
                        Toast.makeText(Message_layout.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    });


            // --- Update chat metadata with names stored per user ---
            HashMap<String, Object> chatMeta = new HashMap<>();
            chatMeta.put("lastMessage", messageText);
            chatMeta.put("timestamp", System.currentTimeMillis());
            chatMeta.put("participants", Arrays.asList(currentUserId, receiverId));

            db.collection("chats").document(chatId).get().addOnSuccessListener(doc -> {
                if (!doc.exists()) {
                    // New chat - store initial data
                    db.collection("users").document(currentUserId).get().addOnSuccessListener(currentUserDoc -> {
                        String currentProfile = currentUserDoc.getString("profilePicBase64");

                        db.collection("users").document(receiverId).get().addOnSuccessListener(receiverDoc -> {
                            String receiverProfile = receiverDoc.exists() ? receiverDoc.getString("profilePicBase64") : null;

                            // Create displayNames map - each user stores what they call the other
                            Map<String, Map<String, String>> displayNames = new HashMap<>();

                            // Current user's view: what they call the receiver
                            Map<String, String> currentUserView = new HashMap<>();
                            currentUserView.put(receiverId, contactname); // Current user calls receiver this name
                            displayNames.put(currentUserId, currentUserView);

                            // Receiver's view will be added when they open the chat
                            chatMeta.put("displayNames", displayNames);

                            // Store profile pics
                            HashMap<String, Object> usersMap = new HashMap<>();
                            HashMap<String, Object> currentUserData = new HashMap<>();
                            currentUserData.put("profilePicBase64", currentProfile);
                            usersMap.put(currentUserId, currentUserData);

                            HashMap<String, Object> receiverUserData = new HashMap<>();
                            receiverUserData.put("profilePicBase64", receiverProfile);
                            usersMap.put(receiverId, receiverUserData);

                            chatMeta.put("users", usersMap);

                            // Save chat metadata
                            db.collection("chats").document(chatId).set(chatMeta);
                        });
                    });
                } else {
                    // Existing chat - update displayNames for current user
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("lastMessage", messageText);
                    updates.put("timestamp", System.currentTimeMillis());
                    updates.put("displayNames." + currentUserId + "." + receiverId, contactname);

                    db.collection("chats")
                            .document(chatId)
                            .update(updates);
                }
            });
        });

        // --- Listen for new messages ---
        SharedPreferences prefs1 = getSharedPreferences("DeletedMessages", MODE_PRIVATE);
        Set<String> deletedIds = prefs1.getStringSet(receiverId, new HashSet<>());

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || querySnapshot == null) return;

                    messageList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        if (!deletedIds.contains(doc.getId())) {  // 👈 skip locally deleted ones
                            MessageModel msg = doc.toObject(MessageModel.class);
                            msg.setMessageid(doc.getId());
                            messageList.add(msg);
                        }
                    }

                    messageAdapter.notifyDataSetChanged();
                    scrollToBottomSmooth();
                });


        // Mark unseen messages as seen when this chat is opened
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("seen", false)
                .get()
                .addOnSuccessListener(docs -> {
                    for (DocumentSnapshot doc : docs) {
                        doc.getReference().update("seen", true);
                    }
                });

        userinfolayout.setOnClickListener(v -> {
            showContactOverlay(contactname, receiverId);
        });
        voicecall.setOnClickListener(v->{
            CallLauncher.startCall(Message_layout.this,receiverId,contactname,phonenumber,false);
        });
        voicecall_text.setOnClickListener(v->voicecall.performClick());
        voicecall_icon.setOnClickListener(v->voicecall.performClick());

        videocall.setOnClickListener(v->{
            CallLauncher.startCall(Message_layout.this,receiverId,contactname,phonenumber,true);
        });
        videocall_icon.setOnClickListener(v->videocall.performClick());
        videocall_text.setOnClickListener(v->videocall.performClick());

        back.setOnClickListener(v -> {
            navigateToMainPage();
        });

        DatabaseReference statusref = FirebaseDatabase.getInstance()
                .getReference("presence")
                .child(receiverId);

        checkuserstatus(currentUserId);
        statusref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String state = snapshot.child("state").getValue(String.class);
                    Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);

                    userstatus.setVisibility(View.VISIBLE);

                    if ("online".equals(state)) {
                        userstatus.setText("🟢 Online");
                    } else if (lastSeen != null) {
                        userstatus.setText("⚫ Last seen " + getTimeAgo(lastSeen));
                    } else {
                        userstatus.setText("⚫ Offline");
                    }
                } else {
                    userstatus.setVisibility(View.VISIBLE);
                    userstatus.setText("⚫ Offline");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Presence", "Error reading user presence", error.toException());
            }
        });

        callbtn.setOnClickListener(v->{
            Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
            callinfolayout.startAnimation(slideIn);
            callinfolayout.setVisibility(View.VISIBLE);
            view.setVisibility(View.VISIBLE);
        });
        view.setOnClickListener(v->{
            if(callinfolayout.getVisibility() == View.VISIBLE){
                Animation slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out);
                callinfolayout.startAnimation(slideOut);
                slideOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        callinfolayout.setVisibility(View.GONE);
                        view.setVisibility(View.GONE);
                    }
                    @Override public void onAnimationRepeat(Animation animation) {}
                });
            }
        });

        msginput.setOnTouchListener((v,event)->{
            final int DRAWABLE_RIGHT = 2; // index for drawableEnd
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (msginput.getRight() - msginput.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    openCamera(); // <-- this method handles camera capture
                    return true;
                }
            }
            return false;
        });

        attachbtn.setOnClickListener(v -> {
            Intent intent1 = new Intent(Intent.ACTION_GET_CONTENT);
            intent1.setType("*/*"); // any file type
            intent1.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent1, "Select File"), 200);
        });

        deletebtn.setOnClickListener(v -> {
            if (!MessageAdapter.selectedPositions.isEmpty()) {

                // Sort positions descending to avoid shifting indexes
                List<Integer> toDelete = new ArrayList<>(MessageAdapter.selectedPositions);
                Collections.sort(toDelete, Collections.reverseOrder());

                SharedPreferences prefs2 = getSharedPreferences("DeletedMessages", MODE_PRIVATE);
                Set<String> deletedIds2 = prefs2.getStringSet(receiverId, new HashSet<>());

                for (int pos : toDelete) {
                    if (pos >= 0 && pos < messageList.size()) {

                        MessageModel msg = messageList.get(pos);

                        // Store deleted message ID for local hiding
                        if (msg.getMessageid() != null) {
                            deletedIds2.add(msg.getMessageid());
                        }

                        // Remove from UI list
                        messageList.remove(pos);
                        messageAdapter.notifyItemRemoved(pos);
                    }
                }

                // Save hidden IDs so they don't appear on refresh
                prefs2.edit().putStringSet(receiverId, deletedIds2).apply();

                // Reset selection mode
                MessageAdapter.isLongPressed = false;
                MessageAdapter.selectedPositions.clear();
                messageAdapter.notifyDataSetChanged();
                updateHeaderUI();

                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
            }
        });



    }
    private void scrollToBottomSmooth() {
        if (messageList.size() > 0) {
            recyclerView.smoothScrollToPosition(messageList.size() - 1);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        //isinMessageLayout = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        //isinMessageLayout = false;
    }


    @Override
    public void onBackPressed() {
        //isinMessageLayout=false;
        super.onBackPressed();
        navigateToMainPage();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        currentOpenChatUserId = null;
        ChatAdapter.isChatOpen=false;
        //isinMessageLayout=false;
    }
    public void updateHeaderUI() {
        if (MessageAdapter.isLongPressed && !MessageAdapter.selectedPositions.isEmpty()) {
            deletebtn.setVisibility(View.VISIBLE);
            headercard.setBackgroundColor(ContextCompat.getColor(this, R.color.grey));
            back.setBackgroundColor(ContextCompat.getColor(this, R.color.grey));
            callbtn.setVisibility(View.GONE);
        } else {
            deletebtn.setVisibility(View.GONE);
            headercard.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
            back.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
            callbtn.setVisibility(View.VISIBLE);
        }
    }


    private void openCamera() {

        File imageFile = new File(getExternalFilesDir(null), "camera_image_" + System.currentTimeMillis() + ".jpg");
        imageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);

        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 📸 CAMERA CAPTURE
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            if (imageUri != null) {
                Uri destinationUri = Uri.fromFile(
                        new File(getCacheDir(), "cropped_image_" + System.currentTimeMillis() + ".jpg")
                );
                UCrop.of(imageUri, destinationUri)
                        .withAspectRatio(1, 1)
                        .withMaxResultSize(1080, 1080)
                        .start(this);
            }
        }

        // ✂️ CROPPED IMAGE RESULT
        else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri croppedUri = UCrop.getOutput(data);
            if (croppedUri != null) {
                sendImageMessage(croppedUri, "");
            }
        }

        // ❌ UCrop ERROR
        else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable cropError = UCrop.getError(data);
            if (cropError != null) cropError.printStackTrace();
        }

        // 📎 FILE PICKER RESULT
        else if (requestCode == 200 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();
            String fileName = getFileName(fileUri);
            sendFileMessage(fileUri, fileName);
        }
    }

    // Helper to extract file name from URI
    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }
    private String saveFileToLocal(Uri fileUri, String fileName) {
        try {
            File dir = new File(getExternalFilesDir(null), "ChatApp/SentFiles");
            if (!dir.exists()) dir.mkdirs();

            File localFile = new File(dir, fileName);

            try (
                    InputStream in = getContentResolver().openInputStream(fileUri);
                    FileOutputStream out = new FileOutputStream(localFile)
            ) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }

            return localFile.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendFileMessage(Uri fileUri, String fileName) {
        try {
            MessageModel message = new MessageModel(
                    currentUserId,
                    receiverId,
                    "",
                    System.currentTimeMillis()
            );
            message.setType("file");
            message.setFileName(fileName);
            String localPath = saveFileToLocal(fileUri, fileName);
            message.setFileUri(localPath);
            message.setSeen(false);

            // Save message to Firestore (no upload)
            db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(message)
                    .addOnSuccessListener(docRef -> {
                        Log.d("Message_layout", "📎 File message sent (local)");

                        // Update chat metadata
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("lastMessage", "📎 " + fileName);
                        updates.put("timestamp", System.currentTimeMillis());
                        updates.put("displayNames." + currentUserId + "." + receiverId, contactname);
                        updates.put("lastMessageType", "file");

                        db.collection("chats")
                                .document(chatId)
                                .set(updates, SetOptions.merge());

                        sendNotificationToDevice(receiverId, "📎 " + fileName, chatId);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to send file: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }



    private void sendImageMessage(Uri imageUri, @Nullable String caption) {
        try {
            // Convert image to Base64
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos); // compress for smaller size
            String imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            // Build message
            MessageModel message = new MessageModel(
                    currentUserId,
                    receiverId,
                    "",
                    System.currentTimeMillis()
            );
            message.setType("image");
            message.setImageBase64(imageBase64);
            message.setSeen(false);

            // Add image message to Firestore
            db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(message)
                    .addOnSuccessListener(docRef -> {
                        Log.d("Message_layout", "📷 Image message sent");

                        // Update chat metadata
                        HashMap<String, Object> chatMeta = new HashMap<>();
                        chatMeta.put("lastMessage", "📷 Photo");
                        chatMeta.put("timestamp", System.currentTimeMillis());
                        chatMeta.put("participants", Arrays.asList(currentUserId, receiverId));
                        chatMeta.put("lastMessageType", "image");

                        db.collection("chats").document(chatId).get().addOnSuccessListener(doc -> {
                            if (!doc.exists()) {
                                // New chat - store displayNames and profile pics
                                db.collection("users").document(currentUserId).get().addOnSuccessListener(currentUserDoc -> {
                                    String currentProfile = currentUserDoc.getString("profilePicBase64");

                                    db.collection("users").document(receiverId).get().addOnSuccessListener(receiverDoc -> {
                                        String receiverProfile = receiverDoc.exists() ? receiverDoc.getString("profilePicBase64") : null;

                                        Map<String, Map<String, String>> displayNames = new HashMap<>();
                                        Map<String, String> currentUserView = new HashMap<>();
                                        currentUserView.put(receiverId, contactname);
                                        displayNames.put(currentUserId, currentUserView);

                                        chatMeta.put("displayNames", displayNames);

                                        HashMap<String, Object> usersMap = new HashMap<>();
                                        HashMap<String, Object> currentUserData = new HashMap<>();
                                        currentUserData.put("profilePicBase64", currentProfile);
                                        usersMap.put(currentUserId, currentUserData);

                                        HashMap<String, Object> receiverUserData = new HashMap<>();
                                        receiverUserData.put("profilePicBase64", receiverProfile);
                                        usersMap.put(receiverId, receiverUserData);

                                        chatMeta.put("users", usersMap);

                                        db.collection("chats").document(chatId).set(chatMeta);
                                    });
                                });
                            } else {
                                // Update existing chat metadata
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("lastMessage","📷 Photo");
                                updates.put("timestamp", System.currentTimeMillis());
                                updates.put("displayNames." + currentUserId + "." + receiverId, contactname);
                                updates.put("lastMessageType", "image");

                                db.collection("chats")
                                        .document(chatId)
                                        .update(updates);
                            }
                        });

                        // Send notification (optional)
                        sendNotificationToDevice(receiverId, "📷 Photo", chatId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to send image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkuserstatus(String currentuserid) {
        presence = FirebaseDatabase.getInstance().getReference("presence");
        currentuserref = presence.child(currentuserid);
        connectedref = FirebaseDatabase.getInstance().getReference(".info/connected");

        connectedref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    Map<String, Object> status = new LinkedHashMap<>();
                    status.put("state", "online");
                    status.put("lastSeen", System.currentTimeMillis());
                    currentuserref.setValue(status);

                    currentuserref.onDisconnect().setValue(
                            new LinkedHashMap<String, Object>() {{
                                put("state", "offline");
                                put("lastSeen", System.currentTimeMillis());
                            }}
                    );
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Presence", "Listener was cancelled");
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("state", "offline");
        status.put("currentChat", null);
        AppState.isInMessageLayout = false;
        ChatAdapter.isChatOpen = false;
        currentOpenChatUserId = null;
        if (currentuserref != null) currentuserref.setValue(status);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("state", "online");
        AppState.isInMessageLayout=true;
        ChatAdapter.isChatOpen = true;
        currentOpenChatUserId = getIntent().getStringExtra("receiverId");
        status.put("currentChat", currentOpenChatUserId);
        if (currentuserref != null) currentuserref.setValue(status);
        markMessagesAsSeen();
    }
    private void markMessagesAsSeen() {
        FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("seen", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot) {
                        doc.getReference().update("seen", true);
                    }
                });
    }

    private String getTimeAgo(long time) {
        long now = System.currentTimeMillis();
        long diff = now - time;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) return "just now";
        else if (minutes < 60) return minutes + " min ago";
        else if (hours < 24) return hours + " hr ago";
        else return days + " day" + (days > 1 ? "s" : "") + " ago";
    }


    private void navigateToMainPage() {
        Intent intent = new Intent(Message_layout.this, MainPage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showContactOverlay(String name, String userId) {
        contactinfolayout.setVisibility(View.VISIBLE);
        view.setVisibility(View.VISIBLE);

        // Slide-in animation
        Animation slideIn = AnimationUtils.loadAnimation(Message_layout.this, R.anim.slide_in_bottom);
        contactinfolayout.startAnimation(slideIn);


        // Load profile picture
        SharedPreferences prefs = getSharedPreferences("UserCache", MODE_PRIVATE);
        String cachedPic = prefs.getString(receiverId + "_profile_pic", null);

        if (cachedPic != null) {
            byte[] decoded = Base64.decode(cachedPic, Base64.DEFAULT);
            Glide.with(this).asBitmap().load(decoded).circleCrop().into(profileview_info);
        } else {
            profileview_info.setImageResource(R.drawable.profile_icon);
        }



        // Exit button logic
        exitbtn.setOnClickListener(v -> {
            Animation slideOut = AnimationUtils.loadAnimation(Message_layout.this, R.anim.slide_out_bottom);
            contactinfolayout.startAnimation(slideOut);
            slideOut.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    contactinfolayout.setVisibility(View.GONE);
                    view.setVisibility(View.GONE);
                }
                @Override public void onAnimationRepeat(Animation animation) {}
            });
        });
    }
    private void sendNotificationToDevice(String receiverId, String messageText, String chatId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getUid();

        //  Step 1: Get receiver's FCM token
        db.collection("users").document(receiverId).get().addOnSuccessListener(receiverDoc -> {
            if (!receiverDoc.exists() || !receiverDoc.contains("fcmToken")) {
                Log.d("FCM_SEND", "Receiver doesn't have FCM token");
                return;
            }
            String deviceToken = receiverDoc.getString("fcmToken");

            //  Step 2: Get the sender name from the chat's displayNames
            // This is the name the RECEIVER has saved for the current user
            db.collection("chats").document(chatId).get()
                    .addOnSuccessListener(chatDoc -> {
                        String senderName = "Unknown User";

                        if (chatDoc.exists() && chatDoc.contains("displayNames")) {
                            Map<String, Object> displayNamesMap = (Map<String, Object>) chatDoc.get("displayNames");

                            // Get what the RECEIVER calls the current user
                            if (displayNamesMap != null && displayNamesMap.containsKey(receiverId)) {
                                Map<String, Object> receiverView = (Map<String, Object>) displayNamesMap.get(receiverId);
                                if (receiverView != null && receiverView.containsKey(currentUserId)) {
                                    senderName = (String) receiverView.get(currentUserId);
                                }
                            }
                        }

                        //  If receiver hasn't saved a name yet, fallback to sender's profile data
                        if (senderName == null || senderName.equals("Unknown User")) {
                            db.collection("users").document(currentUserId).get()
                                    .addOnSuccessListener(senderDoc -> {
                                        String fallbackName = "Unknown User";
                                        if (senderDoc.exists()) {
                                            if (senderDoc.contains("contactname") && senderDoc.getString("contactname") != null) {
                                                fallbackName = senderDoc.getString("contactname");
                                            } else if (senderDoc.contains("phone")) {
                                                fallbackName = senderDoc.getString("phone");
                                            }
                                        }
                                        sendFCMMessage(deviceToken, chatId, currentUserId, receiverId, fallbackName, messageText,null);
                                    });
                        } else {
                            sendFCMMessage(deviceToken, chatId, currentUserId, receiverId, senderName, messageText,null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FCM_SEND", "Failed to fetch chat displayNames", e);
                        // Fallback to user's own profile name
                        db.collection("users").document(currentUserId).get()
                                .addOnSuccessListener(senderDoc -> {
                                    String fallbackName = "Unknown User";
                                    if (senderDoc.exists()) {
                                        if (senderDoc.contains("contactname") && senderDoc.getString("contactname") != null) {
                                            fallbackName = senderDoc.getString("contactname");
                                        } else if (senderDoc.contains("phone")) {
                                            fallbackName = senderDoc.getString("phone");
                                        }
                                    }
                                    sendFCMMessage(deviceToken, chatId, currentUserId, receiverId, fallbackName, messageText,null);
                                });
                    });
        });
    }

    private void sendFCMMessage(String deviceToken, String chatId, String senderId,
                                String receiverId, String senderName, String messageText,String profilepicbase64) {
        new Thread(() -> {
            try {
                InputStream serviceAccount = getAssets().open("service-account.json");

                GoogleCredentials googleCredentials = GoogleCredentials
                        .fromStream(serviceAccount)
                        .createScoped(Collections.singleton("https://www.googleapis.com/auth/firebase.messaging"));
                googleCredentials.refreshIfExpired();

                String accessToken = googleCredentials.getAccessToken().getTokenValue();
                String projectId = "chat-application-654b8"; // your Firebase project ID

                //  Escape special characters in message text and sender name
                String escapedMessage = messageText.replace("\"", "\\\"").replace("\n", "\\n");
                String escapedSenderName = senderName.replace("\"", "\\\"");

                String json = "{"
                        + "\"message\":{"
                        +     "\"token\":\"" + deviceToken + "\","
                        +     "\"notification\":{"
                        +         "\"title\":\"" + escapedSenderName + "\","
                        +         "\"body\":\"" + escapedMessage + "\""
                        +     "},"
                        +     "\"data\":{"
                        +         "\"openFromNotification\":\"true\","
                        +         "\"notificationUid\":\"" + senderId + "\","
                        +         "\"notificationChatId\":\"" + chatId + "\","
                        +         "\"senderName\":\"" + escapedSenderName + "\","
                        +         "\"body\":\"" + escapedMessage + "\""
                        + "},"
                        +     "\"android\":{"
                        +         "\"priority\":\"high\","
                        +         "\"notification\":{"
                        +             "\"sound\":\"default\","
                        +             "\"channel_id\":\"chat_notifications\","  //  Match the CHANNEL_ID
                        +         "}"
                        +     "}"
                        + "}"
                        + "}";

                OkHttpClient client = new OkHttpClient();
                MediaType JSON = MediaType.get("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(JSON, json);

                Request request = new Request.Builder()
                        .url("https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send")
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json; UTF-8")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    Log.d("FCM_SEND", "Notification sent successfully: " + responseBody);
                } else {
                    Log.e("FCM_SEND", "Failed to send notification. Code: " + response.code() + ", Response: " + responseBody);
                }
            } catch (Exception e) {
                Log.e("FCM_SEND", "Error sending FCM message", e);
            }
        }).start();
    }



}