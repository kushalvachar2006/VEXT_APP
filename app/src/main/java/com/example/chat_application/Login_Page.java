package com.example.chat_application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class Login_Page extends AppCompatActivity {

    private static final String TAG = "Login_Page";
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private FirebaseFirestore db;

    TextView signup;
    Button signinwithgooglebtn,signinwithemail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        credentialManager = CredentialManager.create(this);
        signinwithgooglebtn = findViewById(R.id.signin_with_google_btn);
        signinwithemail = findViewById(R.id.signin_with_email);
        signup = findViewById(R.id.sign_up_text);

        boolean fromSignup = getIntent().getBooleanExtra("fromSignup", false);


        signinwithemail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login_Page.this,SigninWithEmail.class);
                startActivity(intent);

            }
        });

        if (!fromSignup) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                currentUser.reload().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && currentUser.isEmailVerified()) {
                        navigateToMainPage();
                    } else {
                        mAuth.signOut();
                    }
                });
            }
        }


        signinwithgooglebtn.setOnClickListener(v -> startSignIn());


        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login_Page.this,SignUp_Page.class);
                startActivity(intent);
                finish();
            }
        });

    }
    private void startSignIn() {
        GetSignInWithGoogleOption googleOption =
                new GetSignInWithGoogleOption.Builder(getString(R.string.web_client_id)).build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        handleSignInResponse(response);
                    }
                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e(TAG, "Credential Error: ", e);
                        Toast.makeText(Login_Page.this, "Sign-in failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void handleSignInResponse(@NonNull GetCredentialResponse response) {
        Credential credential = response.getCredential();
        if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            try {
                GoogleIdTokenCredential googleCred =
                        GoogleIdTokenCredential.createFrom(credential.getData());

                String idToken = googleCred.getIdToken();
                String email = googleCred.getId();


                mAuth.fetchSignInMethodsForEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                boolean isNewUser = task.getResult().getSignInMethods().isEmpty();
                                if (isNewUser) {

                                    firebaseAuthWithGoogle(idToken,true);
                                } else {

                                    firebaseAuthWithGoogle(idToken,false);
                                    Toast.makeText(this,
                                            "This email is already registered. Please use another Google account.",
                                            Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Log.e(TAG, "Error checking account", task.getException());
                                Toast.makeText(this, "Error checking account", Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "GoogleIdToken parse failed", e);
                Toast.makeText(this, "Sign-in failed", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Unexpected credential type: " + credential.getType());
            Toast.makeText(this, "Unsupported credential type", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken, boolean isNewUser) {
        mAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if(isNewUser && user!=null){
                            saveUserToFirestore(user);
                            navigatetophonenumber();
                        }
                        else{
                            navigateToMainPage();
                        }
                    } else {
                        Toast.makeText(this, "Firebase auth failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigatetophonenumber() {
        Intent intent = new Intent(Login_Page.this, PhoneNumberInput.class);
        startActivity(intent);
        finish();
    }


    private void saveUserToFirestore(FirebaseUser user) {
        if (user == null) return;

        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("uid", user.getUid());
        userMap.put("email", user.getEmail());
        userMap.put("name", user.getDisplayName());
            db.collection("users")
                    .document(user.getUid())
                    .set(userMap)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User saved in Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save user", e));

    }
    private void navigateToMainPage() {
        Intent intent = new Intent(Login_Page.this, MainPage.class);
        startActivity(intent);
        finish();
    }
}
