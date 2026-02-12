package com.example.chat_application;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SigninWithEmail extends AppCompatActivity {
    EditText email,password;
    TextView forgotpassword;
    ImageButton passwordtoggle;
    boolean isPasswordVisible = false;
    Button signin;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin_with_email);

        email = findViewById(R.id.enteremail);
        password = findViewById(R.id.enterpassword);
        signin = findViewById(R.id.signin_btn);
        forgotpassword = findViewById(R.id.forgot_password);
        passwordtoggle=findViewById(R.id.password_toggle);

        auth = FirebaseAuth.getInstance();


        passwordtoggle.setOnClickListener(v -> {
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


        forgotpassword.setOnClickListener(v -> {
            String emailStr = email.getText().toString().trim();
            if (emailStr.isEmpty()) {
                email.setError("Please enter your email to reset password");
                return;
            }

            auth.sendPasswordResetEmail(emailStr)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(SigninWithEmail.this,
                                    "Password reset email sent! Please check your inbox.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(SigninWithEmail.this,
                                    "Failed to send reset email: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });


        signin.setOnClickListener(v -> {
            String emailStr = email.getText().toString().trim();
            String passwordStr = password.getText().toString().trim();

            if (emailStr.isEmpty()) {
                email.setError("Enter your email");
                return;
            }

            if (passwordStr.isEmpty()) {
                password.setError("Enter your password");
                return;
            }

            auth.signInWithEmailAndPassword(emailStr, passwordStr)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                if (user.isEmailVerified()) {
                                    Toast.makeText(SigninWithEmail.this,
                                            "Login Successful!",
                                            Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(SigninWithEmail.this, PhoneNumberInput.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(SigninWithEmail.this,
                                            "Please verify your email before signing in.",
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        } else {
                            Toast.makeText(SigninWithEmail.this,
                                    "Login failed: User does not exist\nGo to signup to register",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}