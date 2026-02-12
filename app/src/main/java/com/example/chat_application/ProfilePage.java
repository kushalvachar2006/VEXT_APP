package com.example.chat_application;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;

public class ProfilePage extends AppCompatActivity {

    private ImageButton profileViewBtn, backbtn;
    private Button editProfilePicBtn, saveBtn, exitBtn;
    private ImageView name_icon, abt_icon;
    private TextView setName, setAbout, setEmail, editTitle, name, about,setphone;
    private EditText editInput;
    private View overlay;
    private CardView editLayout;

    private FirebaseUser currentUser;
    private DatabaseReference dbRef;
    private FirebaseFirestore firestoreDb;

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;
    private SharedPreferences prefs;

    private static final HashMap<String, UserProfile> userProfileMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_page);

        profileViewBtn = findViewById(R.id.profile_view_btn);
        editProfilePicBtn = findViewById(R.id.profile_edit_btn);
        setName = findViewById(R.id.setname);
        setAbout = findViewById(R.id.setabout);
        setEmail = findViewById(R.id.setemail);
        setphone = findViewById(R.id.setphone);
        overlay = findViewById(R.id.overlay_view);
        editLayout = findViewById(R.id.edit_layout);
        editTitle = findViewById(R.id.edit_title);
        editInput = findViewById(R.id.edit_input);
        saveBtn = findViewById(R.id.save_btn);
        exitBtn = findViewById(R.id.exitbtn);
        name_icon = findViewById(R.id.name_icon);
        abt_icon = findViewById(R.id.about_icon);
        name = findViewById(R.id.name);
        about = findViewById(R.id.about);
        backbtn = findViewById(R.id.back_btn_profile_page);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        dbRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        firestoreDb = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("user_profile", MODE_PRIVATE);


        loadProfileOffline();



        View.OnClickListener openEditor = field -> {
            boolean isName = field.getId() == R.id.setname || field.getId() == R.id.name_icon;
            editTitle.setText(isName ? "Edit Name" : "Edit About");
            editInput.setText(isName ? setName.getText() : setAbout.getText());

            overlay.setVisibility(View.VISIBLE);
            editLayout.setVisibility(View.VISIBLE);

            saveBtn.setOnClickListener(v -> {
                String newText = editInput.getText().toString().trim();
                if (!newText.isEmpty()) {
                    if (isName) {
                        setName.setText(newText);
                        dbRef.child("name").setValue(newText);
                        firestoreDb.collection("users").document(currentUser.getUid()).update("name", newText);
                        prefs.edit().putString("name", newText).apply();
                    } else {
                        setAbout.setText(newText);
                        dbRef.child("about").setValue(newText);
                        firestoreDb.collection("users").document(currentUser.getUid()).update("about", newText);
                        prefs.edit().putString("about", newText).apply();
                    }
                }
                overlay.setVisibility(View.GONE);
                editLayout.setVisibility(View.GONE);
            });
        };
        firestoreDb.collection("users").document(currentUser.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        setphone.setText(document.getString("phone"));
                    }
                    else{
                        setphone.setText("null");
                    }
                }

            }
        });
        setName.setOnClickListener(openEditor);
        setAbout.setOnClickListener(openEditor);
        name_icon.setOnClickListener(openEditor);
        abt_icon.setOnClickListener(openEditor);

        exitBtn.setOnClickListener(v -> {
            overlay.setVisibility(View.GONE);
            editLayout.setVisibility(View.GONE);
        });

        backbtn.setOnClickListener(v -> finish());


        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) startCrop(uri);
        });

        cropLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Uri croppedUri = UCrop.getOutput(result.getData());
                if (croppedUri != null) processAndUploadImage(croppedUri);
            }
        });

        editProfilePicBtn.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        profileViewBtn.setOnClickListener(v -> {
            File localFile = new File(getFilesDir(), "profile_pic.jpg");
            String pathToPass = null;

            if (localFile.exists()) pathToPass = localFile.getAbsolutePath();
            else if (currentUser.getPhotoUrl() != null) pathToPass = currentUser.getPhotoUrl().toString();

            if (pathToPass != null) {
                Intent intent = new Intent(ProfilePage.this, Full_screen_profile.class);
                intent.putExtra("imagePath", pathToPass);
                startActivity(intent);
            }
        });


        if (isOnline()) {
            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    UserProfile profile = snapshot.getValue(UserProfile.class);
                    if (profile == null) profile = new UserProfile();

                    if (profile.name == null || profile.name.isEmpty())
                        profile.name = currentUser.getDisplayName();
                    if (profile.email == null || profile.email.isEmpty())
                        profile.email = currentUser.getEmail();
                    if (profile.about == null)
                        profile.about = "Hey there! I am using Vext App";


                    setName.setText(profile.name);
                    setAbout.setText(profile.about);
                    setEmail.setText(profile.email);

                    saveProfileLocally(profile);
                    userProfileMap.put(currentUser.getUid(), profile);

                    loadProfileImage(profile);
                }

                @Override
                public void onCancelled(DatabaseError error) {}
            });
        }
    }


    private void saveProfileLocally(UserProfile profile) {
        prefs.edit()
                .putString("name", profile.name)
                .putString("about", profile.about)
                .putString("email", profile.email)
                .apply();
    }

    private void loadProfileOffline() {
        setName.setText(prefs.getString("name", currentUser.getDisplayName()));
        setAbout.setText(prefs.getString("about", "Hey there! I am using Vext App"));
        setEmail.setText(prefs.getString("email", currentUser.getEmail()));

        File localFile = new File(getFilesDir(), "profile_pic.jpg");
        if (localFile.exists()) {
            Glide.with(this)
                    .load(localFile)
                    .circleCrop()
                    .signature(new ObjectKey(localFile.lastModified()))
                    .into(profileViewBtn);
        } else {
            profileViewBtn.setImageResource(R.drawable.profile_icon);
        }
    }


    private void loadProfileImage(UserProfile profile) {
        File localFile = new File(getFilesDir(), "profile_pic.jpg");

        if (profile.profilePicBase64 != null) {
            try {
                byte[] decoded = Base64.decode(profile.profilePicBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);

                try (FileOutputStream fos = new FileOutputStream(localFile)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                }

                Glide.with(this)
                        .load(localFile)
                        .circleCrop()
                        .signature(new ObjectKey(localFile.lastModified()))
                        .into(profileViewBtn);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (localFile.exists()) {
            Glide.with(this)
                    .load(localFile)
                    .circleCrop()
                    .signature(new ObjectKey(localFile.lastModified()))
                    .into(profileViewBtn);
        } else if (currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.profile_icon)
                    .into(profileViewBtn);
        } else {
            profileViewBtn.setImageResource(R.drawable.profile_icon);
        }
    }

    private void startCrop(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped.jpg"));
        UCrop.Options options = new UCrop.Options();
        options.setFreeStyleCropEnabled(true);
        options.setToolbarTitle("Crop Profile Picture");
        options.setCompressionQuality(90);

        cropLauncher.launch(UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withOptions(options)
                .getIntent(this));
    }

    private void processAndUploadImage(Uri uri) {
        try {
            Bitmap bitmap;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(is);
            }

            String encodedImage;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            }


            dbRef.child("profilePicBase64").setValue(encodedImage);


            firestoreDb.collection("users")
                    .document(currentUser.getUid())
                    .update("profilePicBase64", encodedImage);


            UserProfile profile = userProfileMap.get(currentUser.getUid());
            if (profile == null) {
                profile = new UserProfile();
                userProfileMap.put(currentUser.getUid(), profile);
            }
            profile.profilePicBase64 = encodedImage;


            File localFile = new File(getFilesDir(), "profile_pic.jpg");
            try (FileOutputStream fos = new FileOutputStream(localFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            }
            Glide.with(this)
                    .load(localFile)
                    .circleCrop()
                    .signature(new ObjectKey(System.currentTimeMillis()))
                    .into(profileViewBtn);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        }
        return false;
    }

    public static class UserProfile {
        public String uid, name, about, email, profilePicBase64;

        public UserProfile() {}

        public UserProfile(String uid, String name, String about, String email, String profilePicBase64) {
            this.uid = uid;
            this.name = name;
            this.about = about;
            this.email = email;
            this.profilePicBase64 = profilePicBase64;
        }
    }
}
