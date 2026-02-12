package com.example.chat_application;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainPage extends AppCompatActivity {

    TextView vext_app, chats, calls, plusnewchat,username,userphonenumber,msgbtntext,callbtntext;
    Button viewprofile, signout,exitbtn,voicecall,videocall;
    ImageButton menu_icon, newchat,msgbtn,callbtn_view;
    ImageView profile_view;
    ImageButton chatbtn, callbtn;
    RelativeLayout menu_content;
    ConstraintLayout root;
    LinearLayout message_btn_layout,call_btn_layout,call_options_layout;
    RecyclerView chats_layout, calls_layout;
    EditText search;
    FirebaseFirestore db;
    View overlay;

    List<CallItem> callList = new ArrayList<>();

    List<ChatItem> chatList = new ArrayList<>();

    ChatAdapter chatAdapter;
    CardView contactinfo_layout;
    private com.google.firebase.firestore.ListenerRegistration chatsListener;
    private String callId;
    private String otherUserId;



    private static final int PERMISSION_REQUEST_CODE = 100;
    public static boolean isCallIcon=false;


    private void loadContactsForNameResolution() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            chats_layout.setVisibility(View.GONE);
            plusnewchat.setVisibility(View.VISIBLE);
            plusnewchat.setText("Press + for new chat with your contact");
            loadChatsFromFirestore();
            return;
        }


        List<ContactModel> contacts = new ArrayList<>();
        android.database.Cursor cursor = getContentResolver().query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null
        );

        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                int nameIdx = cursor.getColumnIndex(
                        android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int phoneIdx = cursor.getColumnIndex(
                        android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);

                if (nameIdx >= 0 && phoneIdx >= 0) {
                    String name = cursor.getString(nameIdx);
                    String phone = cursor.getString(phoneIdx);

                    if (phone != null && name != null) {
                        phone = phone.replaceAll("[\\s\\-()\\+]", "");
                        if (phone.length() >= 10) {
                            contacts.add(new ContactModel(name, phone));
                        }
                    }
                }
            }
            cursor.close();
        }

        ContactAdapter.contactListFull = contacts;

        FirebaseFirestore.getInstance().collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String phone = doc.getString("phone");
                        if (phone != null) {
                            phone = phone.replaceAll("[\\s\\-()\\+]", "");

                            for (ContactModel contact : contacts) {
                                String contactPhone = contact.getPhoneNumber();

                                if (contactPhone.equals(phone)) {
                                    ContactAdapter.uidToLocalName.put(doc.getId(), contact.getName());
                                    break;
                                }

                                if (contactPhone.length() >= 10 && phone.length() >= 10) {
                                    String contactLast10 = contactPhone.substring(contactPhone.length() - 10);
                                    String phoneLast10 = phone.substring(phone.length() - 10);
                                    if (contactLast10.equals(phoneLast10)) {
                                        ContactAdapter.uidToLocalName.put(doc.getId(), contact.getName());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    loadChatsFromFirestore();
                })
                .addOnFailureListener(e -> {
                    loadChatsFromFirestore();
                });
    }
    private void checkAndRequestPermissions() {
        List<String> permissionNeeded = new ArrayList<>();


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionNeeded.add(Manifest.permission.READ_CONTACTS);
        }


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionNeeded.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionNeeded.add(Manifest.permission.READ_MEDIA_AUDIO);
            }


            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
                permissionNeeded.add(Manifest.permission.CAMERA);
            }
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
                permissionNeeded.add(Manifest.permission.RECORD_AUDIO);
            }
        } else {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        if (!permissionNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(new String[]{
                        Manifest.permission.POST_NOTIFICATIONS
                }, 100);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null && !nm.canUseFullScreenIntent()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


                    Toast.makeText(this,
                            "Please enable Full Screen Intent to receive incoming calls",
                            Toast.LENGTH_LONG).show();
                    startActivity(intent);
                }
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            StringBuilder deniedPermissions = new StringBuilder();

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    deniedPermissions.append(permissions[i]).append("\n");


                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                        Log.d("Permissions", "Permission permanently denied: " + permissions[i]);
                    }
                }
            }

            if (allGranted) {
                Log.d("Permissions", "All permissions granted");
            } else {
                Log.d("Permissions", "Some permissions denied: " + deniedPermissions.toString());

                showPermissionDeniedDialog(deniedPermissions.toString());
            }
        }
    }



    private void showPermissionDeniedDialog(String deniedPermissions) {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("The following permissions are required for the app to work properly:\n\n"
                        + deniedPermissions
                        + "\nPlease grant these permissions in Settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {

                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page);


        vext_app = findViewById(R.id.vextapp);
        chatbtn = findViewById(R.id.chats_btn);
        callbtn = findViewById(R.id.phone_btn);
        chats = findViewById(R.id.chats_btn_txt);
        calls = findViewById(R.id.phone_btn_txt);
        menu_icon = findViewById(R.id.menu_icon_btn);
        menu_content = findViewById(R.id.menu_content_layout);
        root = findViewById(R.id.root_layout);
        viewprofile = findViewById(R.id.view_profile_btn);
        signout = findViewById(R.id.signout_btn);
        chats_layout = findViewById(R.id.recyclerview_for_chats);
        calls_layout = findViewById(R.id.recyclerview_for_calls);
        search = findViewById(R.id.search_bar);
        newchat = findViewById(R.id.new_chat_btn);
        plusnewchat = findViewById(R.id.plusnewchat);
        contactinfo_layout = findViewById(R.id.contactinfo_layout);
        username = findViewById(R.id.username);
        userphonenumber = findViewById(R.id.userphonenumber);
        message_btn_layout = findViewById(R.id.messagebtnlayout);
        call_btn_layout = findViewById(R.id.callsbtnlayout);
        profile_view = findViewById(R.id.profile_view);
        exitbtn = findViewById(R.id.exitbtn);
        db = FirebaseFirestore.getInstance();
        voicecall = findViewById(R.id.voice_call);
        videocall = findViewById(R.id.video_call);
        call_options_layout = findViewById(R.id.call_options_layout);
        msgbtn = findViewById(R.id.messagebtn);
        msgbtntext = findViewById(R.id.messagebtn_text);
        callbtn_view = findViewById(R.id.callbtn);
        callbtntext = findViewById(R.id.callbtn_text);
        Intent intent = getIntent();
        String callId = intent.getStringExtra("callId");
        String otherUserId = intent.getStringExtra("otherUserId");


        checkAndRequestPermissions();
        loadContactsForNameResolution();
        String currentUserId = FirebaseAuth.getInstance().getUid();

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                String token = task.getResult();
                FirebaseFirestore.getInstance().collection("users")
                        .document(currentUserId)
                        .update("fcmToken", token);
            }
        });


        Paint paint = vext_app.getPaint();
        float width = paint.measureText(vext_app.getText().toString());
        Shader shader = new LinearGradient(
                0, 0, width, vext_app.getTextSize(),
                new int[]{Color.parseColor("#00C8F3"), Color.parseColor("#00CF55")},
                null, Shader.TileMode.CLAMP
        );
        vext_app.getPaint().setShader(shader);
        vext_app.invalidate();


        menu_content.bringToFront();
        overlay = findViewById(R.id.overlay_view);
        menu_icon.setOnClickListener(v -> {
            Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
            menu_content.startAnimation(slideIn);
            menu_content.setVisibility(View.VISIBLE);
            overlay.setVisibility(View.VISIBLE);
        });
        overlay.setOnClickListener(v -> {
            if (menu_content.getVisibility() == View.VISIBLE) {
                Animation slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out);
                menu_content.startAnimation(slideOut);
                slideOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        menu_content.setVisibility(View.INVISIBLE);
                        overlay.setVisibility(View.GONE);
                    }
                    @Override public void onAnimationRepeat(Animation animation) {}
                });
            }
        });


        viewprofile.setOnClickListener(v -> startActivity(new Intent(MainPage.this, ProfilePage.class)));
        signout.setOnClickListener(v -> signOutUser());


        chatbtn.setBackgroundResource(R.drawable.chats_selected);
        chats.setTypeface(null, Typeface.BOLD);
        chatbtn.setSelected(true);
        callbtn.setSelected(false);
        search.setHint("Search Chats...");


        setupSearchBar();


        chats_layout.setHasFixedSize(true);
        chats_layout.setLayoutManager(new LinearLayoutManager(this));

        calls_layout.setHasFixedSize(true);
        calls_layout.setLayoutManager(new LinearLayoutManager(this));


        chatAdapter = new ChatAdapter(chatList);
        chats_layout.setAdapter(chatAdapter);

        calls_layout.setAdapter(new CallAdapter(this, callList));


        chatbtn.setOnClickListener(v -> selectChatsTab(chatList));
        chats.setOnClickListener(v -> chatbtn.performClick());

        callbtn.setOnClickListener(v -> selectCallsTab(callList));
        calls.setOnClickListener(v -> callbtn.performClick());

        newchat.setOnClickListener(v -> startActivity(new Intent(MainPage.this, ContactView.class)));


        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    String phone = doc.getString("phone");
                    if (phone != null) {
                        CallListener listener = new CallListener();
                        listener.startListening(phone, getApplicationContext());
                    }
                });


        listenForCallLogs();


        listenForUserNamePhoneUpdates();


    }

    private void listenForIncomingCalls() {
        db.collection("calls")
                .whereEqualTo("receiverPhone",ChatAdapter.receiverphonenumber )
                .whereEqualTo("status", "ringing")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null || snapshots.isEmpty()) return;
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Map<String, Object> callData = dc.getDocument().getData();

                            Intent i = new Intent(this, IncomingCall.class);
                            i.putExtra("callId", dc.getDocument().getId());
                            i.putExtra("callerName", (String) callData.get("callerName"));
                            i.putExtra("callerProfile", (String) callData.get("callerProfile"));
                            i.putExtra("isVideoCall", (Boolean) callData.get("isVideoCall"));
                            startActivity(i);
                        }
                    }
                });
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }
    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("uid")) {
            String notificationUid = intent.getStringExtra("uid");
            String notificationChatId = intent.getStringExtra("chatId");
            String senderName = intent.getStringExtra("senderName");


            db.collection("users")
                    .document(notificationUid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String contactName = documentSnapshot.getString("contactname");
                            String receiverPhone = documentSnapshot.getString("phone");
                            String profilePicBase64 = documentSnapshot.getString("profilePicBase64");


                            if (contactName == null || contactName.isEmpty()) {
                                contactName = senderName != null ? senderName : receiverPhone;
                            }


                            if (contactName == null || contactName.isEmpty()) {
                                contactName = receiverPhone != null ? receiverPhone : "Unknown User";
                            }


                            Intent messageIntent = new Intent(MainPage.this, Message_layout.class);
                            messageIntent.putExtra("contactName", contactName);
                            messageIntent.putExtra("contactPhone", receiverPhone);
                            messageIntent.putExtra("receiverId", notificationUid);
                            messageIntent.putExtra("profilePicBase64", profilePicBase64);
                            messageIntent.putExtra("chatId", notificationChatId);

                            startActivity(messageIntent);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle failure silently or show a toast
                    });
        }
    }
    private void loadChatsFromFirestore() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        Map<String, String> userProfileCache = new LinkedHashMap<>();
        chatList.clear();
        chatAdapter.notifyDataSetChanged();

        chats_layout.setVisibility(View.GONE);
        plusnewchat.setVisibility(View.VISIBLE);
        fetchChats(userProfileCache, currentUserId);

        FirebaseDatabase.getInstance().getReference("users")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String uid = child.getKey();
                        String pic = child.child("profilePicBase64").getValue(String.class);
                        userProfileCache.put(uid, pic);
                    }

                    if (chatAdapter != null) {
                        chatAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                });
    }
    private void fetchChats(Map<String, String> userProfileCache, String currentUserId) {

        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((querySnapshot, error) -> {

                    if (error != null || querySnapshot == null) {
                        Log.e("FetchChats", "Error or null snapshot: " + (error != null ? error.getMessage() : "null"));
                        runOnUiThread(() -> {
                            if (chatList.isEmpty()) {
                                chats_layout.setVisibility(View.GONE);
                                plusnewchat.setVisibility(View.VISIBLE);
                                plusnewchat.setText("Press + for new chat with your contact");
                            }
                        });
                        return;
                    }

                    Log.d("FetchChats", "Documents found: " + querySnapshot.size());

                    Map<String, ChatItem> tempMap = new LinkedHashMap<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {

                        Log.d("FetchChats", "Processing doc: " + doc.getId());

                        List<String> participants = (List<String>) doc.get("participants");

                        if (participants == null) {
                            Log.e("FetchChats", "No participants found");
                            continue;
                        }

                        String receiverId = null;
                        if (participants.size() == 1 && participants.get(0).equals(currentUserId)) {
                            receiverId = currentUserId;
                        } else if (participants.size() == 2) {

                            for (String id : participants) {
                                if (!id.equals(currentUserId)) {
                                    receiverId = id;
                                    break;
                                }
                            }
                        }
                        if (receiverId == null) {
                            Log.e("FetchChats", "No receiverId found");
                            continue;
                        }

                        Log.d("FetchChats", "ReceiverID: " + receiverId);
                        Log.d("FetchChats", "CurrentUserID: " + currentUserId);

                        String lastMessage = "No messages yet";
                        long timestamp = 0;


                        Long globalTimestamp = doc.getLong("timestamp");
                        if (globalTimestamp != null) {
                            timestamp = globalTimestamp;
                        }


                        Map<String, Object> lastMessagesMap = (Map<String, Object>) doc.get("lastMessages");

                        if (lastMessagesMap != null && lastMessagesMap instanceof Map) {
                            Log.d("FetchChats", "Found nested lastMessages map: " + lastMessagesMap.keySet());


                            Object currentUserDataObj = lastMessagesMap.get(currentUserId);
                            Object receiverDataObj = lastMessagesMap.get(receiverId);

                            Map<String, Object> currentUserData = null;
                            Map<String, Object> receiverData = null;

                            if (currentUserDataObj instanceof Map) {
                                currentUserData = (Map<String, Object>) currentUserDataObj;
                            }
                            if (receiverDataObj instanceof Map) {
                                receiverData = (Map<String, Object>) receiverDataObj;
                            }

                            Log.d("FetchChats", "Current user data: " + currentUserData);
                            Log.d("FetchChats", "Receiver data: " + receiverData);


                            Map<String, Object> messageData = currentUserData != null ? currentUserData : receiverData;

                            if (messageData != null) {
                                Object textObj = messageData.get("text");
                                Object timeObj = messageData.get("timestamp");

                                Log.d("FetchChats", "Text object: " + textObj);
                                Log.d("FetchChats", "Time object: " + timeObj);

                                if (textObj != null && !textObj.toString().trim().isEmpty()) {
                                    lastMessage = textObj.toString();
                                }

                                if (timeObj instanceof Long) {
                                    timestamp = (Long) timeObj;
                                } else if (timeObj instanceof Double) {
                                    timestamp = ((Double) timeObj).longValue();
                                } else if (timeObj instanceof Integer) {
                                    timestamp = ((Integer) timeObj).longValue();
                                }
                            }
                        } else {

                            Log.w("FetchChats", "lastMessages is not a nested map, trying flat structure");

                            Map<String, Object> data = doc.getData();

                            if (data != null) {
                                String currentUserTextKey = "lastMessages." + currentUserId + ".text";
                                String currentUserTimeKey = "lastMessages." + currentUserId + ".timestamp";
                                String receiverTextKey = "lastMessages." + receiverId + ".text";
                                String receiverTimeKey = "lastMessages." + receiverId + ".timestamp";

                                Log.d("FetchChats", "Looking for flat keys:");
                                Log.d("FetchChats", "  - " + currentUserTextKey);
                                Log.d("FetchChats", "Available keys: " + data.keySet());

                                Object currentUserText = data.get(currentUserTextKey);
                                Object currentUserTime = data.get(currentUserTimeKey);
                                Object receiverText = data.get(receiverTextKey);
                                Object receiverTime = data.get(receiverTimeKey);


                                if (currentUserText != null && !currentUserText.toString().trim().isEmpty()) {
                                    lastMessage = currentUserText.toString();
                                    if (currentUserTime instanceof Long) {
                                        timestamp = (Long) currentUserTime;
                                    } else if (currentUserTime instanceof Double) {
                                        timestamp = ((Double) currentUserTime).longValue();
                                    } else if (currentUserTime instanceof Integer) {
                                        timestamp = ((Integer) currentUserTime).longValue();
                                    }
                                }

                                else if (receiverText != null && !receiverText.toString().trim().isEmpty()) {
                                    lastMessage = receiverText.toString();
                                    if (receiverTime instanceof Long) {
                                        timestamp = (Long) receiverTime;
                                    } else if (receiverTime instanceof Double) {
                                        timestamp = ((Double) receiverTime).longValue();
                                    } else if (receiverTime instanceof Integer) {
                                        timestamp = ((Integer) receiverTime).longValue();
                                    }
                                }
                            }
                        }

                        Log.d("FetchChats", "Final lastMessage: " + lastMessage);
                        Log.d("FetchChats", "Final timestamp: " + timestamp);

                        String displayName = ContactAdapter.uidToLocalName.get(receiverId);

                        if (displayName == null || displayName.isEmpty()) {
                            displayName = receiverId;
                        }
                        if (receiverId.equals(currentUserId)) {
                            displayName = displayName + " (You)";
                        }
                        String profilePic = userProfileCache.get(receiverId);

                        ChatItem item = new ChatItem(displayName, lastMessage, profilePic, receiverId);
                        item.setTimestamp(timestamp);

                        tempMap.put(receiverId, item);
                    }

                    chatList.clear();
                    chatList.addAll(tempMap.values());


                    chatList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                    chatAdapter.updateFullList(chatList);
                    chatAdapter.notifyDataSetChanged();


                    runOnUiThread(() -> {
                        if (chatList.isEmpty()) {
                            chats_layout.setVisibility(View.GONE);
                            plusnewchat.setVisibility(View.VISIBLE);
                            plusnewchat.setText("Press + for new chat with your contact");
                        } else {
                            chats_layout.setVisibility(View.VISIBLE);
                            plusnewchat.setVisibility(View.GONE);
                        }
                    });
                });
    }



    private void loadChatItem(DocumentSnapshot doc, String receiverId, String displayName,
                              String lastMessage, Map<String, ChatItem> tempMap,
                              Map<String, String> userProfileCache) {

        String profilePicBase64 = null;
        Map<String, Object> users = (Map<String, Object>) doc.get("users");
        if (users != null && users.containsKey(receiverId)) {
            Map<String, Object> userData = (Map<String, Object>) users.get(receiverId);
            if (userData != null) {
                profilePicBase64 = (String) userData.get("profilePicBase64");
            }
        }


        if (profilePicBase64 == null || profilePicBase64.isEmpty()) {
            profilePicBase64 = userProfileCache.get(receiverId);
        }

        long timestamp = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;

        ChatItem item = new ChatItem(displayName, lastMessage, profilePicBase64, receiverId);
        item.setTimestamp(timestamp);

        tempMap.put(receiverId, item);

        chatList.clear();
        chatList.addAll(tempMap.values());

        chatList.sort((a, b) ->
                Long.compare(b.getTimestamp(), a.getTimestamp()));

        chatAdapter.updateFullList(chatList);
        chatAdapter.notifyDataSetChanged();

        chats_layout.setVisibility(chatList.isEmpty()
                ? View.GONE : View.VISIBLE);

        plusnewchat.setVisibility(chatList.isEmpty()
                ? View.VISIBLE : View.GONE);

        calls_layout.setVisibility(View.GONE);

    }

    public void showContactOverlay(String name, String userId, String profileBase64) {

        overlay.setVisibility(View.VISIBLE);
        contactinfo_layout.setVisibility(View.VISIBLE);

        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom);
        contactinfo_layout.startAnimation(slideIn);

        username.setText(name);
        userphonenumber.setText("Loading...");


        SharedPreferences prefs = getSharedPreferences("UserCache", MODE_PRIVATE);
        String cachedPic = prefs.getString(userId + "_profile_pic", null);

        if (cachedPic != null) {
            byte[] decoded = Base64.decode(cachedPic, Base64.DEFAULT);
            Glide.with(this).asBitmap().load(decoded).circleCrop().into(profile_view);
        } else {
            profile_view.setImageResource(R.drawable.profile_icon);
        }
        profile_view.setOnClickListener(v -> {
            openContactFullScreen(userId);
        });



        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    String phone = doc.getString("phone");
                    userphonenumber.setText(phone != null ? phone : "Unknown");


                    message_btn_layout.setOnClickListener(v -> {
                        Intent i = new Intent(MainPage.this, Message_layout.class);
                        i.putExtra("receiverId", userId);
                        i.putExtra("contactName", name);
                        i.putExtra("contactPhone", phone);
                        startActivity(i);
                    });
                    msgbtntext.setOnClickListener(v->message_btn_layout.performClick());
                    msgbtn.setOnClickListener(v->message_btn_layout.performClick());


                    call_btn_layout.setOnClickListener(v -> {
                        call_options_layout.setVisibility(
                                call_options_layout.getVisibility() == View.VISIBLE
                                        ? View.GONE
                                        : View.VISIBLE
                        );
                    });
                    callbtn_view.setOnClickListener(v->call_btn_layout.performClick());
                    callbtntext.setOnClickListener(v->call_btn_layout.performClick());
                    voicecall.setOnClickListener(v -> {

                        CallLauncher.startCall(
                                MainPage.this,
                                userId,
                                name,
                                phone,
                                false
                        );
                    });


                    videocall.setOnClickListener(v -> {

                        CallLauncher.startCall(
                                MainPage.this,
                                userId,
                                name,
                                phone,
                                true
                        );
                    });

                });


        exitbtn.setOnClickListener(v -> {
            Animation slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_bottom);
            contactinfo_layout.startAnimation(slideOut);
            slideOut.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    overlay.setVisibility(View.GONE);
                    contactinfo_layout.setVisibility(View.GONE);
                    call_options_layout.setVisibility(View.GONE);
                }
                @Override public void onAnimationRepeat(Animation animation) {}
            });
        });
    }
    private void openContactFullScreen(String userId) {

        if (userId == null || userId.isEmpty()) {
            openDefaultFullScreen();
            return;
        }


        SharedPreferences prefs = getSharedPreferences("UserCache", MODE_PRIVATE);
        String cachedPic = prefs.getString(userId + "_profile_pic", null);

        if (cachedPic != null && !cachedPic.isEmpty()) {
            try {
                byte[] decoded = Base64.decode(cachedPic, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);

                File file = saveBitmapToCache(bitmap);
                if (file != null) {
                    openFullScreenProfile(file);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {

                    String base64 = doc.getString("profilePicBase64");

                    if (base64 != null && !base64.isEmpty()) {
                        try {
                            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);

                            File file = saveBitmapToCache(bitmap);
                            if (file != null) {
                                openFullScreenProfile(file);


                                prefs.edit().putString(userId + "_profile_pic", base64).apply();
                                return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    openDefaultFullScreen();
                })
                .addOnFailureListener(e -> openDefaultFullScreen());
    }
    private File saveBitmapToCache(Bitmap bitmap) {
        File cacheDir = getCacheDir();
        File file = new File(cacheDir, "profile_temp.jpg");

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private void openFullScreenProfile(File file) {
        Uri uri = Uri.fromFile(file);
        Intent intent = new Intent(this, Full_screen_profile.class);
        intent.putExtra("imagePath", uri.toString());
        startActivity(intent);
    }
    private void openDefaultFullScreen() {
        Intent intent = new Intent(this, Full_screen_profile.class);
        intent.putExtra(
                "imagePath",
                Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.profile_icon).toString()
        );
        startActivity(intent);
    }





    private void selectChatsTab(List<ChatItem> chatList) {
        chatbtn.setBackgroundResource(R.drawable.chats_selected);
        chats.setTypeface(null, Typeface.BOLD);
        chatbtn.setSelected(true);
        callbtn.setBackgroundResource(R.drawable.phone);
        calls.setTypeface(null, Typeface.NORMAL);
        callbtn.setSelected(false);

        chats_layout.setVisibility(chatList.isEmpty() ? View.GONE : View.VISIBLE);
        calls_layout.setVisibility(View.GONE);

        newchat.setImageResource(R.drawable.new_chat_xml);

        if (chatList.isEmpty()) {
            plusnewchat.setText("Press + for new chat with your contact");
            plusnewchat.setVisibility(View.VISIBLE);
        } else {
            plusnewchat.setVisibility(View.GONE);
        }
        MainPage.isCallIcon = false;

        search.setHint("Search Chats...");
    }


    private void selectCallsTab(List<CallItem> callList) {
        callbtn.setBackgroundResource(R.drawable.phone_selected);
        calls.setTypeface(null, Typeface.BOLD);
        callbtn.setSelected(true);
        chatbtn.setBackgroundResource(R.drawable.chats);
        chats.setTypeface(null, Typeface.NORMAL);
        chatbtn.setSelected(false);

        chats_layout.setVisibility(View.GONE);
        calls_layout.setVisibility(callList.isEmpty() ? View.GONE : View.VISIBLE);

        newchat.setImageResource(R.drawable.new_call_xml);

        if (callList.isEmpty()) {
            plusnewchat.setText("Press + for new call with your contact");
            plusnewchat.setVisibility(View.VISIBLE);
        } else {
            plusnewchat.setVisibility(View.GONE);
        }
        MainPage.isCallIcon = true;

        search.setHint("Search Calls...");
    }

    private void setupSearchBar() {
        search.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                search.setCompoundDrawablesWithIntrinsicBounds(R.drawable.back_btn_xml, 0, 0, 0);
                search.setCursorVisible(true);
            } else {
                search.setCompoundDrawablesWithIntrinsicBounds(R.drawable.search_icon_xml, 0, 0, 0);
                search.setCursorVisible(false);
            }
        });

        search.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP &&
                    event.getX() <= (search.getCompoundDrawables()[0].getBounds().width() + search.getPaddingStart())) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
                search.clearFocus();
                search.setCursorVisible(false);
                search.setCompoundDrawablesWithIntrinsicBounds(R.drawable.search_icon_xml, 0, 0, 0);
                return true;
            }
            return false;
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                chatAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        root.setOnClickListener(v -> {
            if (search.isCursorVisible()) {
                search.clearFocus();
                search.setCursorVisible(false);
                search.setCompoundDrawablesWithIntrinsicBounds(R.drawable.search_icon_xml, 0, 0, 0);
            }
        });
    }

    private void listenForCallLogs() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        FirebaseFirestore.getInstance().collection("call_logs")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    for (DocumentChange dc : snap.getDocumentChanges()) {
                        DocumentSnapshot d = dc.getDocument();
                        String callId = d.getString("callId");
                        if (callId == null) continue;
                        String direction = d.getString("direction") != null ? d.getString("direction") : "";

                        CallItem item = new CallItem(
                                callId,
                                d.getString("otherName") != null ? d.getString("otherName") : "Unknown",
                                (direction) + ": " + d.getString("status"),
                                null,
                                d.getString("otherUserId"),
                                d.getString("profilePicBase64"),
                                d.getBoolean("isVideoCall") != null && d.getBoolean("isVideoCall"),
                                d.getLong("timestamp") != null ? d.getLong("timestamp") : 0L,
                                getCallIcon(d.getString("status"), d.getString("direction")),
                                direction
                        );

                        switch (dc.getType()) {
                            case ADDED:
                                callList.add(dc.getNewIndex(), item);
                                break;
                            case MODIFIED:
                                if (dc.getOldIndex() < callList.size()) {
                                    callList.set(dc.getOldIndex(), item);
                                }
                                break;
                            case REMOVED:
                                if (dc.getOldIndex() < callList.size()) {
                                    callList.remove(dc.getOldIndex());
                                }
                                break;
                        }
                    }
                    RecyclerView.Adapter adapter = calls_layout.getAdapter();
                    if (adapter != null) adapter.notifyDataSetChanged();
                });
    }

    private int getCallIcon(String status, String direction) {
        if (status == null) return R.drawable.call_missed;
        switch (status) {
            case "accepted":
                return R.drawable.call_received;
            case "ringing":
                return R.drawable.call_outgoing;
            case "rejected":
            case "ended":
            default:
                return R.drawable.call_missed;
        }
    }

    private void listenForUserNamePhoneUpdates() {
        FirebaseFirestore.getInstance().collection("users")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    boolean changed = false;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String uid = d.getId();
                        String phone = d.getString("phone");
                        String name = d.getString("name");
                        String local = ContactAdapter.uidToLocalName.get(uid);
                        if (local != null && !local.isEmpty()) continue;
                        for (ChatItem ci : chatList) {
                            if (uid.equals(ci.getReceiverId())) {
                                String display = name != null && !name.isEmpty() ? name : (phone != null ? phone : ci.getName());
                                if (!display.equals(ci.getName())) {
                                    ci.setName(display);
                                    changed = true;
                                }
                            }
                        }
                    }
                    if (changed && chatAdapter != null) {
                        chatAdapter.updateFullList(chatList);
                        chatAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void signOutUser() {
        FirebaseAuth.getInstance().signOut();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            db.collection("users").document(uid).delete().addOnSuccessListener(aVoid -> {
                user.delete().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(MainPage.this, Login_Page.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    }
                });
            });
        }
        startActivity(new Intent(MainPage.this, Login_Page.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }


}
