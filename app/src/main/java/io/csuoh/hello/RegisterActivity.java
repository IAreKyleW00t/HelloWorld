/**
 * Group 18
 * Kyle Colantonio, 2595744
 * 4/28/2017
 *
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
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.csuoh.hello.models.DatabaseUser;

public class RegisterActivity extends BaseActivity {
    public static final String TAG = RegisterActivity.class.getSimpleName();

    // Activity elements
    @BindView(R.id.input_name) EditText mName;
    @BindView(R.id.input_email) EditText mEmail;
    @BindView(R.id.input_password) EditText mPassword;
    @BindView(R.id.input_password_confirm) EditText mPasswordConfirm;
    @BindView(R.id.button_register) Button mRegisterButton;
    @BindView(R.id.link_login) TextView mLoginLink;

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
        firebaseCreateUserWithEmailAndPassword(mEmail.getText().toString(), mPassword.getText().toString());
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
        mName.setError(null);
        mEmail.setError(null);
        mPassword.setError(null);
        mPasswordConfirm.setError(null);

        // Check name
        String name = this.mName.getText().toString();
        if (name.isEmpty()) {
            this.mName.setError(getString(R.string.error_input_name));
            return false;
        }

        // Check email
        String email = this.mEmail.getText().toString();
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            this.mEmail.setError(getString(R.string.error_input_email));
            return false;
        }

        // Check password
        String password = mPassword.getText().toString();
        if (password.isEmpty() || password.length() < 6) {
            mPassword.setError(getString(R.string.error_input_password));
            return false;
        }

        // Check confirmation password
        String password2 = mPasswordConfirm.getText().toString();
        if (!password2.equals(password)) {
            mPasswordConfirm.setError(getString(R.string.error_input_password_confirm));
            return false;
        }

        return true;
    }

    public void enableInput(boolean enabled) {
        mName.setEnabled(enabled);
        mEmail.setEnabled(enabled);
        mPassword.setEnabled(enabled);
        mPasswordConfirm.setEnabled(enabled);
        mRegisterButton.setEnabled(enabled);
        mLoginLink.setEnabled(enabled);
    }

    private class UserCreationResultListener implements OnCompleteListener<AuthResult> {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
            // Check if account creation was successful
            if (task.isSuccessful()) {
                // Get the URI of the default profile picture
                mStorage.getReferenceFromUrl("gs://" + getString(R.string.firebase_bucket))
                        .child("images").child("default_profilePicture")
                        .getDownloadUrl()
                        .addOnCompleteListener(RegisterActivity.this, mStorageUriResultListener);
            } else {
                // Log the error and notify the user
                Log.e(TAG, "UserCreationResultListener", task.getException());
                FirebaseCrash.report(task.getException());
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
            // Check if getting the download URI was successful
            if (task.isSuccessful()) {
                // Update the users profile to include the display name they gave and a default profile picture
                UserProfileChangeRequest profileUpdateRequest = new UserProfileChangeRequest.Builder()
                        .setDisplayName(mName.getText().toString())
                        .setPhotoUri(task.getResult())
                        .build();

                mAuth.getCurrentUser().updateProfile(profileUpdateRequest)
                        .addOnCompleteListener(RegisterActivity.this, mUserProfileUpdateResultListener);
            } else {
                // Log the error and notify the user
                Log.e(TAG, "StorageUriResultListener", task.getException());
                FirebaseCrash.report(task.getException());
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
                mDatabase.getReference()
                        .child("users").child(user.getUid())
                        .setValue(new DatabaseUser(user.getDisplayName(), user.getPhotoUrl().toString(), new HashMap<String, Boolean>()))
                        .addOnCompleteListener(RegisterActivity.this, mDatabaseUpdateResultListener);
            } else {
                // Log the error and notify the user
                Log.e(TAG, "UserProfileUpdateResultListener", task.getException());
                FirebaseCrash.report(task.getException());
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
            // Check if the user was successfully written to the database
            if (task.isSuccessful()) {
                // Send a verification email
                mAuth.getCurrentUser().sendEmailVerification()
                        .addOnCompleteListener(RegisterActivity.this, mEmailVerificationResultListener);
            } else {
                // Log the error and notify the user
                Log.e(TAG, "DatabaseUpdateResultListener", task.getException());
                FirebaseCrash.report(task.getException());
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
                // Log the error and notify the user
                Log.e(TAG, "EmailVerificationResultListener", task.getException());
                FirebaseCrash.report(task.getException());
                showSnackbar(R.string.error_msg_register);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }
}
