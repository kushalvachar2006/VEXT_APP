package com.example.chat_application;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class SignUp_Page extends AppCompatActivity {
    EditText name,email,password;
    Button next,verifyandcontinue;
    ImageButton passwordtoggle;
    TextView instruction;
    boolean isPasswordVisible = false;

    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up_page);
        name=findViewById(R.id.entername);
        email = findViewById(R.id.enteremail);
        password = findViewById(R.id.enterpassword);
        next = findViewById(R.id.nxt_btn);
        verifyandcontinue = findViewById(R.id.verfy_continue_btn);
        passwordtoggle = findViewById(R.id.password_toggle);
        instruction = findViewById(R.id.instruction_verification_email);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!*]).{8,}$";

        String instruction_email = getString(R.string.instruction_verify_email);
        instruction.setText(Html.fromHtml(instruction_email,Html.FROM_HTML_MODE_COMPACT));

        passwordtoggle.setOnClickListener(v ->{
            if (isPasswordVisible) {

                password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                passwordtoggle.setBackgroundResource(R.drawable.eye_open);
                isPasswordVisible = false;
            } else {

                password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                passwordtoggle.setBackgroundResource(R.drawable.eye_close);
                isPasswordVisible = true;
            }

            password.setSelection(password.getText().length());
        });



        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nameStr = name.getText().toString().trim();
                String emailStr = email.getText().toString().trim();
                String passwordStr = password.getText().toString().trim();

                if (nameStr.isEmpty()) {
                    name.setError("Please enter your name");
                    return;
                }

                if (emailStr.isEmpty()) {
                    email.setError("Please enter Email");
                    return;
                } else if (!emailStr.matches(emailPattern)) {
                    email.setError("Enter a valid Email");
                    return;
                }

                if (passwordStr.isEmpty() || !passwordStr.matches(passwordPattern)) {
                    password.setError("Please enter a valid password");
                    return;
                }


                auth.createUserWithEmailAndPassword(emailStr, passwordStr)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = auth.getCurrentUser();
                                if (user != null) {

                                    user.sendEmailVerification()
                                            .addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful()) {
                                                    Toast.makeText(SignUp_Page.this, "Verification email sent", Toast.LENGTH_SHORT).show();
                                                    verifyandcontinue.setVisibility(View.VISIBLE);
                                                    instruction.setVisibility(View.VISIBLE);
                                                } else {
                                                    Toast.makeText(SignUp_Page.this, "Failed to send verification email", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            } else {
                                Toast.makeText(SignUp_Page.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });



        verifyandcontinue.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                user.reload().addOnCompleteListener(task -> {
                    if (user.isEmailVerified()) {
                        saveUserToFirestore(user, name.getText().toString().trim());

                        Toast.makeText(SignUp_Page.this,
                                "Email Verified: Account Registered Successfully!\nSign in with email to get started",
                                Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(SignUp_Page.this, Login_Page.class);
                        intent.putExtra("fromSignup", true);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SignUp_Page.this, "Please verify your email first!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    private void saveUserToFirestore(FirebaseUser user, String nameStr) {
        if (user == null) return;

        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("uid", user.getUid());
        userMap.put("email", user.getEmail());
        userMap.put("name", nameStr);

        db.collection("users")
                .document(user.getUid())
                .set(userMap)
                .addOnSuccessListener(aVoid -> Log.d("SignUp_Page", "User saved in Firestore"))
                .addOnFailureListener(e -> Log.e("SignUp_Page", "Failed to save user", e));
    }
}