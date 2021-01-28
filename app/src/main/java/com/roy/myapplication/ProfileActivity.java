package com.roy.myapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private static final int CHOOSE_IMAGE = 102;
    ImageView imageViewBtn;
    EditText dispName;
    Uri uriProfileImg;
    private TextView textViewVerifyUser;
    private String profileImgUrl, displayName;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private StorageReference profileImgRef;

    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth.getCurrentUser() == null) { // if user not logged in
            finish();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        dispName = findViewById(R.id.editTextDisplayName);
        imageViewBtn = findViewById(R.id.imageBtn);
        mAuth = FirebaseAuth.getInstance();
        textViewVerifyUser = findViewById(R.id.textViewVerifiedUser);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);

        imageViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImgChooser();
            }
        });

        findViewById(R.id.BtnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveUserInfo();
            }
        });

        loadUserInfo();
    }

    private void loadUserInfo() {
        mUser = mAuth.getCurrentUser();

        if (mUser != null) {
            if (mUser.getPhotoUrl() != null) {

                Log.d(TAG, "loadUserInfo: image present " + mUser.getPhotoUrl());
                Glide.with(ProfileActivity.this)
                        .load(mUser.getPhotoUrl().toString())
                        .into(imageViewBtn);
            }

            if (mUser.getDisplayName() != null) {
                dispName.setText(mUser.getDisplayName());
                dispName.setSelection(dispName.getText().length()); // moving cursor to the end

            }

            /**
             * Can only come to be verified only
             *  after verification of the email &
             *  signing in again
             */
            if (mUser.isEmailVerified()) // checks if email verified
                textViewVerifyUser.setText("Email Verified");
            else {
                textViewVerifyUser.setText("Email Not Verified (Click to Verify)");

                textViewVerifyUser.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mUser.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(getApplicationContext(), "Verification Email Sent", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }

        }

    }

    private void saveUserInfo() {
        String displayname = dispName.getText().toString().trim();

        if (displayname.isEmpty()) {
            dispName.requestFocus();
            dispName.setError("Name Required");
            return;
        }

        mUser = mAuth.getCurrentUser(); // getting current Firebase User

        if (mUser != null && profileImgUrl != null) { //TODO: make separated for updating image and name
            //creating a UserProfileChangeRequest for updating user profile
            UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder()
                    .setDisplayName(displayname)
                    .setPhotoUri(Uri.parse(profileImgUrl))
                    .build();

            mUser.updateProfile(profileChangeRequest)  //calling firebaseUser object to update profile
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful())
                                Toast.makeText(getApplicationContext(), "Profile Updated", Toast.LENGTH_SHORT).show();

                            else
                                Toast.makeText(getApplicationContext(), "Fail to Updated Profile", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showImgChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select File image"), CHOOSE_IMAGE); // file chooser
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CHOOSE_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uriProfileImg = data.getData(); // returns Uri

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uriProfileImg);
                imageViewBtn.setImageBitmap(bitmap);

                uploadImageToFirebase();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * To upload the image whenever new image is selected
     */
    private void uploadImageToFirebase() {
        profileImgRef =
                FirebaseStorage.getInstance().getReference("profilepics/" + mAuth.getUid() + ".jpg");  // creating storage folder

        ProgressDialog progressBar = new ProgressDialog(this);
        progressBar.setIndeterminate(true);
        progressBar.setMessage("Uploading Image...");
        progressBar.show();

        if (uriProfileImg != null) {
            profileImgRef.putFile(uriProfileImg) // uploading image uri
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            progressBar.dismiss();

                            profileImgRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    profileImgUrl = task.getResult().toString();
                                    Log.d(TAG, "onComplete: " + profileImgUrl);
                                }
                            });
//                            profileImgUrl = taskSnapshot.getMetadata().getReference().getDownloadUrl().toString(); //depricated
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.dismiss();

                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();

                        }
                    });
        }

    }

    /**
     * Override this method to inflate menu items
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu,menu);

        return true;
    }

    /**
     * Override this method to handle clicks on menu items
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

            case R.id.menulogout:
                FirebaseAuth.getInstance().signOut();
                finish();
                startActivity(new Intent(this,LoginActivity.class));
        }

        return true;
    }
}