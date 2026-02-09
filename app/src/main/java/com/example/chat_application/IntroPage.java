package com.example.chat_application;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class IntroPage extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ConstraintLayout rootLayout;
    private ImageView imgLogo, imgKVA;
    private TextView txtPoweredBy;
    FirebaseFirestore db=FirebaseFirestore.getInstance();
    private String notificationUid = null;
    boolean isFromNotification;
    private String notificationChatId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_page);

        mAuth = FirebaseAuth.getInstance();
        rootLayout = findViewById(R.id.intro_root);
        imgLogo = findViewById(R.id.img1);
        txtPoweredBy = findViewById(R.id.powered_by);
        imgKVA = findViewById(R.id.kva_logo);

        Intent intent = getIntent();
        if (intent != null) {
            String openFromNotificationStr = intent.getStringExtra("openFromNotification");
            isFromNotification = "true".equals(openFromNotificationStr);
            notificationUid = intent.getStringExtra("notificationUid");
            notificationChatId = intent.getStringExtra("notificationChatId");

            setIntent(intent);
            android.util.Log.d("IntroPage", "Intent extras → openFromNotification=" + isFromNotification
                    + ", uid=" + notificationUid + ", chatId=" + notificationChatId);
        }

        //  CRITICAL FIX: Handle notification immediately without animation
        if ( isFromNotification && notificationUid != null && notificationChatId != null) {
            android.util.Log.d("IntroPage", "Opened from notification → navigating to chat");
            // Skip animations and go directly to chat
            handleNotificationNavigation();
            return; //  IMPORTANT: Return here to prevent animation flow
        }

        //  Normal app launch - show splash animation
        android.util.Log.d("IntroPage", "Normal app launch → showing splash");
        Animation rootFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        rootLayout.startAnimation(rootFadeIn);
        sequentialFadeIn();
    }

    private void handleNotificationNavigation() {
        // Check if user is logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            android.util.Log.d("IntroPage", "User not logged in, redirecting to login");
            startActivity(new Intent(IntroPage.this, TermsAndCondition.class));
            finish();
            return;
        }

        android.util.Log.d("IntroPage", "Fetching user data for: " + notificationUid);

        // User is logged in, fetch data from Firestore
        db.collection("users")
                .document(notificationUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        android.util.Log.d("IntroPage", "User document found, opening chat");

                        // Read values from Firestore
                        String contactName = documentSnapshot.getString("contactname");
                        String receiverPhone = documentSnapshot.getString("phone");
                        String profilePicBase64 = documentSnapshot.getString("profilePicBase64");

                        // If contactname is null, use phone as fallback
                        if (contactName == null || contactName.isEmpty()) {
                            contactName = receiverPhone;
                        }

                        // If still null, use "Unknown"
                        if (contactName == null || contactName.isEmpty()) {
                            contactName = "Unknown User";
                        }

                        android.util.Log.d("IntroPage", "Opening Message_layout with: " + contactName);

                        // Open Message_layout activity
                        Intent chatIntent = new Intent(IntroPage.this, Message_layout.class);
                        chatIntent.putExtra("contactName", contactName);
                        chatIntent.putExtra("contactPhone", receiverPhone);
                        chatIntent.putExtra("receiverId", notificationUid);
                        chatIntent.putExtra("profilePicBase64", profilePicBase64);
                        chatIntent.putExtra("chatId", notificationChatId);
                        //  Use NEW_TASK to ensure proper navigation
                        chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        startActivity(chatIntent);
                        finish();
                    } else {
                        android.util.Log.d("IntroPage", "User document not found, going to MainPage");
                        // If user doesn't exist in Firestore, navigate to MainPage
                        startActivity(new Intent(IntroPage.this, MainPage.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("IntroPage", "Failed to fetch user data", e);
                    // If fetch fails, navigate to MainPage
                    startActivity(new Intent(IntroPage.this, MainPage.class));
                    finish();
                });
    }

    private void sequentialFadeIn() {
        Animation fadeItem = AnimationUtils.loadAnimation(this, R.anim.fade_in_item);

        // Logo first
        imgLogo.setVisibility(View.INVISIBLE);
        txtPoweredBy.setVisibility(View.INVISIBLE);
        imgKVA.setVisibility(View.INVISIBLE);

        new Handler().postDelayed(() -> {
            imgLogo.setVisibility(View.VISIBLE);
            imgLogo.startAnimation(fadeItem);
        }, 300);

        // “Powered by” text
        new Handler().postDelayed(() -> {
            txtPoweredBy.setVisibility(View.VISIBLE);
            txtPoweredBy.startAnimation(fadeItem);
        }, 900); // 600ms after logo

        // KVA logo
        new Handler().postDelayed(() -> {
            imgKVA.setVisibility(View.VISIBLE);
            imgKVA.startAnimation(fadeItem);
        }, 1500); // 600ms after text

        // After all animations, check Firebase user and navigate
        new Handler().postDelayed(() -> checkFirebaseUser(), 1500); // 1.5s splash
    }

    private void checkFirebaseUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (isInternetAvailable()) {
                // Refresh from server
                currentUser.reload().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        startFadeOutTransition(true); // valid user
                    } else {
                        mAuth.signOut();
                        startFadeOutTransition(false); // account deleted/invalid
                    }
                });
            } else {
                // Offline → trust cached session
                startFadeOutTransition(true);
            }
        } else {
            startFadeOutTransition(false);
        }
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }


    private void startFadeOutTransition(boolean goToMain) {
        Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        rootLayout.startAnimation(fadeOut);

        rootLayout.postDelayed(() -> {
            Intent intent = goToMain ?
                    new Intent(IntroPage.this, MainPage.class) :
                    new Intent(IntroPage.this, TermsAndCondition.class);
            startActivity(intent);
            finish();
        }, 500); // match fade-out duration
    }
}
