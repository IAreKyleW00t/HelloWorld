/*
 * Copyright (C) 2017  Kyle Colantonio <kyle10468@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.csuoh.hello;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.csuoh.hello.models.DatabaseUser;

public class RegisterActivity extends BaseActivity {
    public static final String TAG = RegisterActivity.class.getSimpleName();

    // Activity elements
    @BindView(R.id.input_name) EditText nameInput;
    @BindView(R.id.input_email) EditText emailInput;
    @BindView(R.id.input_password) EditText passwordInput;
    @BindView(R.id.input_password_confirm) EditText passwordConfirmButton;
    @BindView(R.id.button_register) Button registerButton;
    @BindView(R.id.link_login) TextView loginLink;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseStorage mStorage;
    private FirebaseDatabase mDatabase;

    // Listeners
    private UserCreationResultListener mUserCreationResultListener;
    private StorageUriResultListener mStorageUriResultListener;
    private UserProfileUpdateResultListener mUserProfileUpdateResultListener;
    private DatabaseUpdateResultListener mDatabaseUpdateResultListener;
    private EmailVerificationResultListener mEmailVerificationResultListener;

    public static Intent createIntent(Context context) {
        return new Intent(context, RegisterActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        setTitle(R.string.title_register);
        ButterKnife.bind(this);

        // Get the instances of all our Firebase objects
        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        // Initialize all of our Listeners
        mUserCreationResultListener = new UserCreationResultListener();
        mStorageUriResultListener = new StorageUriResultListener();
        mUserProfileUpdateResultListener = new UserProfileUpdateResultListener();
        mDatabaseUpdateResultListener = new DatabaseUpdateResultListener();
        mEmailVerificationResultListener = new EmailVerificationResultListener();
    }

    @OnClick(R.id.button_register)
    public void onClickRegister() {
        // Validate user input before continuing
        if (!validateInput()) {
            return;
        }

        // Create a new Firebase user with the given email and password
        firebaseCreateUserWithEmailAndPassword(emailInput.getText().toString(), passwordInput.getText().toString());
    }

    @OnClick(R.id.link_login)
    public void onClickLogin() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void firebaseCreateUserWithEmailAndPassword(String email, String password) {
        // Display a progress dialog and disable input
        showProgressDialog(R.string.dialog_progress_register);
        enableInput(false);

        // Start the account creation process
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, mUserCreationResultListener);
    }

    public boolean validateInput() {
        // Clear old error messages
        nameInput.setError(null);
        emailInput.setError(null);
        passwordInput.setError(null);
        passwordConfirmButton.setError(null);

        String name = this.nameInput.getText().toString();
        if (name.isEmpty()) {
            this.nameInput.setError(getString(R.string.error_input_name));
            return false;
        }

        String email = this.emailInput.getText().toString();
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            this.emailInput.setError(getString(R.string.error_input_email));
            return false;
        }

        String password = passwordInput.getText().toString();
        if (password.isEmpty() || password.length() < 6) {
            passwordInput.setError(getString(R.string.error_input_password));
            return false;
        }

        String password2 = passwordConfirmButton.getText().toString();
        if (!password2.equals(password)) {
            passwordConfirmButton.setError(getString(R.string.error_input_password_confirm));
            return false;
        }

        return true;
    }

    public void enableInput(boolean enabled) {
        nameInput.setEnabled(enabled);
        emailInput.setEnabled(enabled);
        passwordInput.setEnabled(enabled);
        passwordConfirmButton.setEnabled(enabled);
        registerButton.setEnabled(enabled);
        loginLink.setEnabled(enabled);
    }

    private class UserCreationResultListener implements OnCompleteListener<AuthResult> {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
            // Check if account creation was successful
            if (task.isSuccessful()) {
                // Get the URI of the default profile picture
                mStorage.getReferenceFromUrl("gs://" + getString(R.string.firebase_bucket))
                        .child("images")
                        .child("default_profilePicture")
                        .getDownloadUrl()
                        .addOnCompleteListener(RegisterActivity.this, mStorageUriResultListener);
            } else {
                // Log the error internally and notify the user
                Log.e(TAG, "UserCreationResultListener", task.getException());
                showSnackbar(R.string.error_msg_register);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }

    private class StorageUriResultListener implements OnCompleteListener<Uri> {
        @Override
        public void onComplete(@NonNull Task<Uri> task) {
            // Check if getting the download URI was successfull
            if (task.isSuccessful()) {
                // Update the users profile to include the display name they gave and a default profile picture
                UserProfileChangeRequest profileUpdateRequest = new UserProfileChangeRequest.Builder()
                        .setDisplayName(nameInput.getText().toString())
                        .setPhotoUri(task.getResult())
                        .build();

                mAuth.getCurrentUser().updateProfile(profileUpdateRequest)
                        .addOnCompleteListener(RegisterActivity.this, mUserProfileUpdateResultListener);
            } else {
                // Log the error internally and notify the user
                Log.e(TAG, "StorageUriResultListener", task.getException());
                showSnackbar(R.string.error_msg_register);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }

    private class UserProfileUpdateResultListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            // Check if the users profile was updated successfully
            if (task.isSuccessful()) {
                // Add the user to the database
                FirebaseUser user = mAuth.getCurrentUser();
                mDatabase.getReference("users")
                        .child(user.getUid())
                        .setValue(new DatabaseUser(user.getDisplayName(), user.getPhotoUrl().toString(), new HashMap<String, Boolean>()))
                        .addOnCompleteListener(RegisterActivity.this, mDatabaseUpdateResultListener);
            } else {
                // Log the error internally and notify the user
                Log.e(TAG, "UserProfileUpdateResultListener", task.getException());
                showSnackbar(R.string.error_msg_register);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }

    private class DatabaseUpdateResultListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            // Check if the user was successfuly written to the database
            if (task.isSuccessful()) {
                // Send a verification email
                mAuth.getCurrentUser().sendEmailVerification()
                        .addOnCompleteListener(RegisterActivity.this, mEmailVerificationResultListener);
            } else {
                // Log the error internally and notify the user
                Log.e(TAG, "DatabaseUpdateResultListener", task.getException());
                showSnackbar(R.string.error_msg_register);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }

    private class EmailVerificationResultListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            // Check if the verification email was successfully sent to the user
            if (task.isSuccessful()) {
                // Set the result of this activity to include the users ID and email,
                // then return back to the previous Activity (LoginActivity)
                setResult(RESULT_OK, new Intent()
                        .putExtra("firebase-uid", mAuth.getCurrentUser().getUid())
                        .putExtra("firebase-email", mAuth.getCurrentUser().getEmail()));
                finish();

                // Remove the progress dialog since it is no longer visible
                hideProgressDialog();
            } else {
                // Log the error internally and notify the user
                Log.e(TAG, "EmailVerificationResultListener", task.getException());
                showSnackbar(R.string.error_msg_register);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }
}
