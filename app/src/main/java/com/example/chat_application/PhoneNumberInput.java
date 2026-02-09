package com.example.chat_application;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;

public class PhoneNumberInput extends AppCompatActivity {

    private EditText phoneInput;
    private Button submitBtn;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_number_input);

        phoneInput = findViewById(R.id.enterphone);
        submitBtn = findViewById(R.id.nxtbtn);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        submitBtn.setOnClickListener(v -> {
            String phoneNumber = phoneInput.getText().toString().trim();

            if (TextUtils.isEmpty(phoneNumber)) {
                phoneInput.setError("Enter phone number");
                return;
            }
            if (!phoneNumber.matches("\\d{10}")) {
                phoneInput.setError("Enter a valid 10-digit phone number");
                return;
            }

            savePhoneNumberToFirestore(phoneNumber);
        });
    }

    private void savePhoneNumberToFirestore(String phoneNumber) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("phone", phoneNumber);

        db.collection("users")
                .document(user.getUid())
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Phone number saved", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(PhoneNumberInput.this, MainPage.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save phone number", Toast.LENGTH_SHORT).show());
    }

}
