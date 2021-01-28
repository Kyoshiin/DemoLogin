package com.roy.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class SignUpActivity extends AppCompatActivity {

    private EditText userEmail,password;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();

        userEmail = findViewById(R.id.editTextEmailAddress);
        password = findViewById(R.id.editTextPassword);

        findViewById(R.id.signinBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                registerUser();
            }
        });
    }

    private void registerUser() {
        String userMail = userEmail.getText().toString().trim();
        String passwrd = password.getText().toString().trim();

        if (userMail.isEmpty()){
            userEmail.setError("Email is required");
            userEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(userMail).matches()){
            userEmail.setError("Email invalid");
            userEmail.requestFocus();
            return;
        }

        if (passwrd.isEmpty()){
            password.setError("Password is required");
            password.requestFocus();
            return;
        }
        if (passwrd.length()<6){
            password.setError("Minimum password is 6");
            password.requestFocus();
            return;
        }


        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Signing In..");
        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(userMail,passwrd).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "User Reg done", Toast.LENGTH_SHORT).show();
                    finish();
                    Intent intent =  new Intent(getApplicationContext(), ProfileActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    progressDialog.dismiss();
                }
                else if (task.getException() instanceof FirebaseAuthUserCollisionException){
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "User Already Registered", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}