package com.example.chat_application;

import android.app.Activity;
import android.app.AlertDialog;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.yalantis.ucrop.UCrop;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
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

    private String profileCacheKey;

    public static String currentOpenChatUserId = null,lastSentChatId=null;



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

        currentUserId = FirebaseAuth.getInstance().getUid();
        receiverId = getIntent().getStringExtra("receiverId");
        loadProfilePicture(receiverId);

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

            Glide.with(this).load(R.drawable.profile_icon).circleCrop().into(profilepic);
            Glide.with(this).load(R.drawable.profile_icon).circleCrop().into(profileview_info);
        }


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


        sendbtn.setOnClickListener(v -> {

            lastSentChatId = chatId;

            String messageText = msginput.getText().toString().trim();
            if (messageText.isEmpty()) return;

            long now = System.currentTimeMillis();

            MessageModel message = new MessageModel(
                    currentUserId,
                    receiverId,
                    messageText,
                    now
            );
            message.setSeen(false);

            db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(message)
                    .addOnSuccessListener(docRef -> {

                        msginput.setText("");
                        scrollToBottomSmooth();

                        Map<String, Object> updates = new HashMap<>();


                        if (currentUserId.equals(receiverId)) {
                            updates.put("participants", Arrays.asList(currentUserId));
                        } else {
                            updates.put("participants", Arrays.asList(currentUserId, receiverId));
                        }

                        Map<String, Object> lastMessages = new HashMap<>();

                        Map<String, Object> currentUserMessage = new HashMap<>();
                        currentUserMessage.put("text", messageText);
                        currentUserMessage.put("timestamp", now);

                        lastMessages.put(currentUserId, currentUserMessage);


                        if (!currentUserId.equals(receiverId)) {
                            Map<String, Object> receiverMessage = new HashMap<>();
                            receiverMessage.put("text", messageText);
                            receiverMessage.put("timestamp", now);
                            lastMessages.put(receiverId, receiverMessage);
                        }

                        updates.put("lastMessages", lastMessages);

                        Map<String, Object> displayNames = new HashMap<>();
                        Map<String, String> currentUserView = new HashMap<>();
                        currentUserView.put(receiverId, contactname);
                        displayNames.put(currentUserId, currentUserView);

                        updates.put("displayNames", displayNames);
                        updates.put("timestamp", now);

                        db.collection("chats")
                                .document(chatId)
                                .set(updates, SetOptions.merge());

                        sendNotificationToDevice(receiverId, messageText, chatId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(Message_layout.this,
                                "Failed to send message",
                                Toast.LENGTH_SHORT).show();
                    });
        });




        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || querySnapshot == null) return;

                    messageList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        MessageModel msg = doc.toObject(MessageModel.class);
                        msg.setMessageid(doc.getId());
                        List<String> deletedFor =
                                (List<String>) doc.get("deletedFor");
                        if (deletedFor != null &&
                                deletedFor.contains(currentUserId)) {
                            continue;
                        }
                        messageList.add(msg);
                    }

                    messageAdapter.notifyDataSetChanged();
                    scrollToBottomSmooth();
                });



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
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (msginput.getRight() - msginput.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    openCamera();
                    return true;
                }
            }
            return false;
        });

        attachbtn.setOnClickListener(v -> {
            Intent intent1 = new Intent(Intent.ACTION_GET_CONTENT);
            intent1.setType("*/*");
            intent1.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent1, "Select File"), 200);
        });

        deletebtn.setOnClickListener(v -> {

            if (!MessageAdapter.selectedPositions.isEmpty()) {

                new AlertDialog.Builder(Message_layout.this)
                        .setTitle("Delete message?")
                        .setMessage("This action cannot be undone.")
                        .setNegativeButton("Cancel",
                                (dialog, which) -> dialog.dismiss())

                        .setPositiveButton("Delete", (dialog, which) -> {

                            List<Integer> toDelete =
                                    new ArrayList<>(MessageAdapter.selectedPositions);

                            Collections.sort(toDelete,
                                    Collections.reverseOrder());

                            for (int pos : toDelete) {

                                if (pos >= 0 &&
                                        pos < messageList.size()) {

                                    MessageModel msg =
                                            messageList.get(pos);

                                    if (msg.getMessageid() == null)
                                        continue;

                                    if (msg.getSenderId().equals(currentUserId)) {


                                        db.collection("chats")
                                                .document(chatId)
                                                .collection("messages")
                                                .document(msg.getMessageid())
                                                .delete()
                                                .addOnSuccessListener(aVoid -> {

                                                    updateLastMessageForUser(currentUserId);
                                                    updateLastMessageForUser(receiverId);
                                                });

                                    } else {


                                        db.collection("chats")
                                                .document(chatId)
                                                .collection("messages")
                                                .document(msg.getMessageid())
                                                .update("deletedFor",
                                                        FieldValue.arrayUnion(currentUserId))
                                                .addOnSuccessListener(aVoid -> {

                                                    updateLastMessageForUser(currentUserId);
                                                });
                                    }

                                }
                            }

                            MessageAdapter.isLongPressed = false;
                            MessageAdapter.selectedPositions.clear();
                            updateHeaderUI();

                            Toast.makeText(Message_layout.this,
                                    "Message deleted",
                                    Toast.LENGTH_SHORT).show();
                        })
                        .show();
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

    }

    @Override
    protected void onStop() {
        super.onStop();

    }


    @Override
    public void onBackPressed() {

        super.onBackPressed();
        navigateToMainPage();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        currentOpenChatUserId = null;
        ChatAdapter.isChatOpen=false;

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


        else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri croppedUri = UCrop.getOutput(data);
            if (croppedUri != null) {
                sendImageMessage(croppedUri, "");
            }
        }


        else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable cropError = UCrop.getError(data);
            if (cropError != null) cropError.printStackTrace();
        }


        else if (requestCode == 200 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();
            String fileName = getFileName(fileUri);
            sendFileMessage(fileUri, fileName);
        }
    }


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
    private void sendFileMessage(Uri fileUri, String fileName) {

        Toast.makeText(this, "Uploading file...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {


                File tempFile = new File(getCacheDir(), fileName);
                InputStream inputStream = getContentResolver().openInputStream(fileUri);
                FileOutputStream outputStream = new FileOutputStream(tempFile);

                byte[] buffer = new byte[8192];
                int len;

                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }

                inputStream.close();
                outputStream.close();


                OkHttpClient client = new OkHttpClient.Builder()
                        .retryOnConnectionFailure(true)
                        .build();

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                                "file",
                                tempFile.getName(),
                                RequestBody.create(
                                        tempFile,
                                        MediaType.parse("application/octet-stream")
                                )
                        )
                        .build();

                Request request = new Request.Builder()
                        .url(getString(R.string.RENDER_LINK))
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    throw new Exception("Server error: " + response.code());
                }

                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);

                String fileUrl = json.getString("fileUrl");


                MessageModel message = new MessageModel(
                        currentUserId,
                        receiverId,
                        "",
                        System.currentTimeMillis()
                );

                message.setType("file");
                message.setFileName(fileName);
                message.setFileUri(fileUrl);
                message.setSeen(false);


                runOnUiThread(() -> {

                    db.collection("chats")
                            .document(chatId)
                            .collection("messages")
                            .add(message)
                            .addOnSuccessListener(docRef -> {
                                long now = System.currentTimeMillis();
                                String preview = "📎 " + fileName;


                                Map<String, Object> updates = new HashMap<>();

                                Map<String, Object> lastMessages = new HashMap<>();

                                Map<String, Object> currentUserMessage = new HashMap<>();
                                currentUserMessage.put("text", preview);
                                currentUserMessage.put("timestamp", now);

                                Map<String, Object> receiverMessage = new HashMap<>();
                                receiverMessage.put("text", preview);
                                receiverMessage.put("timestamp", now);

                                lastMessages.put(currentUserId, currentUserMessage);
                                lastMessages.put(receiverId, receiverMessage);

                                updates.put("lastMessages", lastMessages);
                                updates.put("lastMessageType", "file");
                                updates.put("timestamp", now);


                                Map<String, Object> displayNames = new HashMap<>();
                                Map<String, String> currentUserView = new HashMap<>();
                                currentUserView.put(receiverId, contactname);
                                displayNames.put(currentUserId, currentUserView);
                                updates.put("displayNames", displayNames);

                                db.collection("chats")
                                        .document(chatId)
                                        .set(updates, SetOptions.merge());

                                Toast.makeText(
                                        Message_layout.this,
                                        "File sent successfully",
                                        Toast.LENGTH_SHORT
                                ).show();

                                sendNotificationToDevice(
                                        receiverId,
                                        "📎 " + fileName,
                                        chatId
                                );
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(
                                        Message_layout.this,
                                        "Failed to send message",
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                });

            } catch (Exception e) {

                e.printStackTrace();

                runOnUiThread(() -> Toast.makeText(
                        Message_layout.this,
                        "Upload failed: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
            }
        }).start();
    }
    private void sendImageMessage(Uri imageUri, @Nullable String caption) {
        try {

            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
            String imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);


            MessageModel message = new MessageModel(
                    currentUserId,
                    receiverId,
                    "",
                    System.currentTimeMillis()
            );
            message.setType("image");
            message.setImageBase64(imageBase64);
            message.setSeen(false);


            db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(message)
                    .addOnSuccessListener(docRef -> {
                        Log.d("Message_layout", "📷 Image message sent");

                        long now = System.currentTimeMillis();
                        String preview = "📷 Photo";


                        Map<String, Object> updates = new HashMap<>();


                        updates.put("participants", Arrays.asList(currentUserId, receiverId));
                        updates.put("lastMessageType", "image");
                        updates.put("timestamp", now);


                        Map<String, Object> lastMessages = new HashMap<>();

                        Map<String, Object> currentUserMessage = new HashMap<>();
                        currentUserMessage.put("text", preview);
                        currentUserMessage.put("timestamp", now);

                        Map<String, Object> receiverMessage = new HashMap<>();
                        receiverMessage.put("text", preview);
                        receiverMessage.put("timestamp", now);

                        lastMessages.put(currentUserId, currentUserMessage);
                        lastMessages.put(receiverId, receiverMessage);

                        updates.put("lastMessages", lastMessages);


                        Map<String, Object> displayNames = new HashMap<>();
                        Map<String, String> currentUserView = new HashMap<>();
                        currentUserView.put(receiverId, contactname);
                        displayNames.put(currentUserId, currentUserView);
                        updates.put("displayNames", displayNames);

                        db.collection("chats")
                                .document(chatId)
                                .set(updates, SetOptions.merge());

                        Toast.makeText(
                                Message_layout.this,
                                "Image sent successfully",
                                Toast.LENGTH_SHORT
                        ).show();


                        sendNotificationToDevice(receiverId, "📷 Photo", chatId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(
                                Message_layout.this,
                                "Failed to send image",
                                Toast.LENGTH_SHORT
                        ).show();
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


        Animation slideIn = AnimationUtils.loadAnimation(Message_layout.this, R.anim.slide_in_bottom);
        contactinfolayout.startAnimation(slideIn);



        SharedPreferences prefs = getSharedPreferences("UserCache", MODE_PRIVATE);
        String cachedPic = prefs.getString(receiverId + "_profile_pic", null);

        if (cachedPic != null) {
            byte[] decoded = Base64.decode(cachedPic, Base64.DEFAULT);
            Glide.with(this).asBitmap().load(decoded).circleCrop().into(profileview_info);
        } else {
            profileview_info.setImageResource(R.drawable.profile_icon);
        }




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


        db.collection("users").document(receiverId).get().addOnSuccessListener(receiverDoc -> {
            if (!receiverDoc.exists() || !receiverDoc.contains("fcmToken")) {
                Log.d("FCM_SEND", "Receiver doesn't have FCM token");
                return;
            }
            String deviceToken = receiverDoc.getString("fcmToken");



            db.collection("chats").document(chatId).get()
                    .addOnSuccessListener(chatDoc -> {
                        String senderName = "Unknown User";

                        if (chatDoc.exists() && chatDoc.contains("displayNames")) {
                            Map<String, Object> displayNamesMap = (Map<String, Object>) chatDoc.get("displayNames");


                            if (displayNamesMap != null && displayNamesMap.containsKey(receiverId)) {
                                Map<String, Object> receiverView = (Map<String, Object>) displayNamesMap.get(receiverId);
                                if (receiverView != null && receiverView.containsKey(currentUserId)) {
                                    senderName = (String) receiverView.get(currentUserId);
                                }
                            }
                        }


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
                String projectId = "chat-application-654b8";


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
                        +             "\"channel_id\":\"chat_notifications\","
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
    private void loadProfilePicture(String receiverId) {

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(receiverId)
                .child("profilePicBase64");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    profilepic.setImageResource(R.drawable.profile_icon);
                    profileview_info.setImageResource(R.drawable.profile_icon);
                    return;
                }

                String base64 = snapshot.getValue(String.class);

                if (base64 == null || base64.isEmpty()) {
                    profilepic.setImageResource(R.drawable.profile_icon);
                    profileview_info.setImageResource(R.drawable.profile_icon);
                    return;
                }

                try {
                    byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);

                    Glide.with(Message_layout.this)
                            .load(bitmap)
                            .circleCrop()
                            .into(profilepic);

                    Glide.with(Message_layout.this)
                            .load(bitmap)
                            .circleCrop()
                            .into(profileview_info);

                } catch (Exception e) {
                    profilepic.setImageResource(R.drawable.profile_icon);
                    profileview_info.setImageResource(R.drawable.profile_icon);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                profilepic.setImageResource(R.drawable.profile_icon);
                profileview_info.setImageResource(R.drawable.profile_icon);
            }
        });
    }
    private void updateLastMessageForUser(String userId) {

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {

                    String lastText = "No messages yet";
                    long lastTime = System.currentTimeMillis();

                    for (DocumentSnapshot doc : snapshot) {

                        List<String> deletedFor = (List<String>) doc.get("deletedFor");

                        if (deletedFor != null && deletedFor.contains(userId)) {
                            continue;
                        }

                        MessageModel msg = doc.toObject(MessageModel.class);

                        if (msg == null) continue;

                        if ("image".equals(msg.getType())) {
                            lastText = "📷 Photo";
                        } else if ("file".equals(msg.getType())) {
                            lastText = "📎 " + msg.getFileName();
                        } else {
                            lastText = msg.getMessage();
                        }

                        lastTime = msg.getTimestamp();
                        break;
                    }


                    String finalLastText = lastText;
                    long finalLastTime = lastTime;
                    db.collection("chats")
                            .document(chatId)
                            .get()
                            .addOnSuccessListener(chatDoc -> {
                                Map<String, Object> updates = new HashMap<>();


                                Map<String, Object> lastMessages =
                                        (Map<String, Object>) chatDoc.get("lastMessages");

                                if (lastMessages == null) {
                                    lastMessages = new HashMap<>();
                                }


                                Map<String, Object> userMessageData = new HashMap<>();
                                userMessageData.put("text", finalLastText);
                                userMessageData.put("timestamp", finalLastTime);

                                lastMessages.put(userId, userMessageData);

                                updates.put("lastMessages", lastMessages);
                                updates.put("timestamp", finalLastTime);

                                db.collection("chats")
                                        .document(chatId)
                                        .update(updates);
                            });
                });
    }






}