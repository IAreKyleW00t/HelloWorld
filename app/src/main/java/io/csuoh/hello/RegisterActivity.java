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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

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
    @BindView(R.id.input_confirm_password) EditText confirmPasswordInput;
    @BindView(R.id.button_register) Button registerButton;
    @BindView(R.id.text_login) TextView loginText;

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
        ButterKnife.bind(this);

        // Get the instances of all our Firebase objects
        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        // Initialize all of our Listeners
        // We do this here becuase we are only creating one instance of each Listener when
        // the Activity is created instead of each time the register button is clicked
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

        // Display a progress dialog and disable input
        showProgressDialog(R.string.dialog_register);
        enableInput(false);

        // Begin to go through the entire account creation process
        // For better readability, this is broken into multiple Classes at the bottom of this file
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(emailInput.getText().toString(), passwordInput.getText().toString())
                .addOnCompleteListener(this, mUserCreationResultListener);
    }

    @OnClick(R.id.text_login)
    public void onClickLogin() {
        setResult(RESULT_CANCELED);
        finish();
    }

    public boolean validateInput() {
        // Clear old error messages
        nameInput.setError(null);
        emailInput.setError(null);
        passwordInput.setError(null);
        confirmPasswordInput.setError(null);

        String name = this.nameInput.getText().toString();
        if (name.isEmpty()) {
            this.nameInput.setError(getString(R.string.err_bad_name));
            return false;
        }

        String email = this.emailInput.getText().toString();
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            this.emailInput.setError(getString(R.string.err_bad_email));
            return false;
        }

        String password = passwordInput.getText().toString();
        if (password.isEmpty() || password.length() < 6) {
            passwordInput.setError(getString(R.string.err_bad_password));
            return false;
        }

        String password2 = confirmPasswordInput.getText().toString();
        if (!password2.equals(password)) {
            confirmPasswordInput.setError(getString(R.string.err_mismatch_passwords));
            return false;
        }

        return true;
    }

    public void enableInput(boolean enabled) {
        nameInput.setEnabled(enabled);
        emailInput.setEnabled(enabled);
        passwordInput.setEnabled(enabled);
        confirmPasswordInput.setEnabled(enabled);
        registerButton.setEnabled(enabled);
        loginText.setEnabled(enabled);
    }

    private class UserCreationResultListener implements OnCompleteListener<AuthResult> {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
            // Check if account creation was successful
            if (task.isSuccessful() && task.getResult().getUser() != null) {
                // Cool, now we can get the URI of the default profile picture
                mStorage.getReference("images/default_profilePicture").getDownloadUrl()
                        .addOnCompleteListener(RegisterActivity.this, mStorageUriResultListener);
            } else {
                // Log the error internally and notify the user
                Log.e(TAG, "UserCreationResultListener", task.getException());
                showSnackbar(R.string.error_register_failed);

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
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                // Awesome, we're about halfway done. Now we can update the users profile
                UserProfileChangeRequest profileUpdateRequest = new UserProfileChangeRequest.Builder()
                        .setDisplayName(nameInput.getText().toString())
                        .setPhotoUri(task.getResult())
                        .build();

                mAuth.getCurrentUser().updateProfile(profileUpdateRequest)
                        .addOnCompleteListener(RegisterActivity.this, mUserProfileUpdateResultListener);
            } else {
                // Log the error internally and notify the user
                Log.e(TAG, "StorageUriResultListener", task.getException());
                showSnackbar(R.string.error_register_failed);

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
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                // Sweet, we just need to add the user to the database
                mDatabase.getReference("users")
                        .child(mAuth.getCurrentUser().getUid())
                        .setValue(new DatabaseUser(mAuth.getCurrentUser()))
                        .addOnCompleteListener(RegisterActivity.this, mDatabaseUpdateResultListener);
            } else {
                // Log the error internally and notify the user
                Log.e(TAG, "UserProfileUpdateResultListener", task.getException());
                showSnackbar(R.string.error_register_failed);

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
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                // Home stretch baby! Only thing left is to send a verification emailInput
                mAuth.getCurrentUser().sendEmailVerification()
                        .addOnCompleteListener(RegisterActivity.this, mEmailVerificationResultListener);
            } else {
                // Log the error internally and notify the user
                Log.e(TAG, "DatabaseUpdateResultListener", task.getException());
                showSnackbar(R.string.error_register_failed);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }

    private class EmailVerificationResultListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            // Check if the verification emailInput was successfully sent to the user
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                // Finally, the registration process is complete
                // Set the result of this activity to include the users ID and emailInput,
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
                showSnackbar(R.string.error_register_failed);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }
}
