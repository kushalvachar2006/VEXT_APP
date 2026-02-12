package com.example.chat_application;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;

public class Full_screen_profile extends AppCompatActivity {

    private PhotoView fullScreenImage;
    private ImageButton closeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.full_screen_profile);

        fullScreenImage = findViewById(R.id.full_screen_image);
        closeBtn = findViewById(R.id.close_btn);

        String imagePath = getIntent().getStringExtra("imagePath");

        if (imagePath != null) {
            Uri uri = Uri.parse(imagePath);
            File file = new File(uri.getPath());

            if (file.exists()) {
                Glide.with(this)
                        .load(file)
                        .signature(new ObjectKey(file.lastModified()))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(fullScreenImage);
            } else if (imagePath.startsWith("android.resource://")) {
                Glide.with(this)
                        .load(uri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(fullScreenImage);
            } else {
                loadFirebaseProfile();
            }
        } else {
            loadFirebaseProfile();
        }

        closeBtn.setOnClickListener(v -> finish());
    }

    private void loadFirebaseProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(fullScreenImage);
        } else {
            fullScreenImage.setImageResource(R.drawable.profile_icon);
        }
    }
}
