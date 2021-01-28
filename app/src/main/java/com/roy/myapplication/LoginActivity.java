package com.roy.myapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

//TODO: GENERATE AND ADD SHA-1 OR SHA -256 KEYS FOR GOOGLE SIGN IN -> must be generated

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 1290;
    private EditText userEmail, password;
    FirebaseAuth mAuth;
    private SignInButton Google_signInButton;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userEmail = findViewById(R.id.editTextEmailAddress);
        password = findViewById(R.id.editTextPassword);

        findViewById(R.id.textViewSignIn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }
        });

        findViewById(R.id.loginBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UserLogin();
            }
        });

        Google_signInButton = findViewById(R.id.googleSignIn);

        mAuth = FirebaseAuth.getInstance();

        //config google sign in
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();


        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Google_signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleSignIn();
            }
        });
    }

    private void googleSignIn() {
        Intent googleSignIn = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(googleSignIn, RC_SIGN_IN); // HANDLE result code for launching the intent on ActivityResult
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Google Sign in intiated
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> completedTask = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                //Google signIn successful -> move on with firebase auth
                GoogleSignInAccount acc = completedTask.getResult(ApiException.class);
                Toast.makeText(getApplicationContext(), "Sign in Successful", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "GoogleSignIn id "+acc.getDisplayName());
                FirebaseGoogleAuth(acc);
            } catch (ApiException e) {
                Toast.makeText(getApplicationContext(), "Sign in Failed", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Google Sign in failed due to :"+e.getMessage() );
                e.printStackTrace();
            }
        }
    }

    private void FirebaseGoogleAuth(GoogleSignInAccount account) {
        AuthCredential authCredential = GoogleAuthProvider.getCredential(account.getIdToken(),null);
        mAuth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {  //checking the authCredentials for user data
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    Toast.makeText(getApplicationContext(), "Successfully Logged in", Toast.LENGTH_SHORT).show();
                    //update UI
                    startActivity(new Intent(LoginActivity.this, ProfileActivity.class));
                }
                else
                    Toast.makeText(getApplicationContext(), "Log in Failed", Toast.LENGTH_SHORT).show();


            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth.getCurrentUser() != null) {
            finish();
            startActivity(new Intent(LoginActivity.this, ProfileActivity.class));
        }
    }

    private void UserLogin() {

        String userMail = userEmail.getText().toString().trim();
        String passwrd = password.getText().toString().trim();

        if (userMail.isEmpty()) {
            userEmail.setError("Email is required");
            userEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(userMail).matches()) {
            userEmail.setError("Email invalid");
            userEmail.requestFocus();
            return;
        }

        if (passwrd.isEmpty()) {
            password.setError("Password is required");
            password.requestFocus();
            return;
        }
        if (passwrd.length() < 6) {
            password.setError("Minimum password is 6");
            password.requestFocus();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Signing In..");
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(userMail, passwrd).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (task.isSuccessful()) {
                    finish();
                    Toast.makeText(getApplicationContext(), "Logged In", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //TODO: check
                    startActivity(intent);
                    progressDialog.dismiss();
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}