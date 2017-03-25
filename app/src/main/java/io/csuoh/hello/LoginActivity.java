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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.csuoh.hello.models.DatabaseUser;

public class LoginActivity extends BaseActivity {
    public static final String TAG = LoginActivity.class.getSimpleName();

    // Request codes
    private static final int
            RC_GOOGLE_SIGNIN    = 42,
            RC_REGISTER         = 69;

    // Activity elements
    @BindView(R.id.input_email) EditText mEmail;
    @BindView(R.id.input_password) EditText mPassword;
    @BindView(R.id.button_login_email) Button mEmailLoginButton;
    @BindView(R.id.button_login_google) SignInButton mGoogleLoginButton;
    @BindView(R.id.link_register) TextView mRegisterLink;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;

    // Google
    private GoogleApiClient mGoogleApiClient;

    // Listeners
    private UserLoginResultListener mUserLoginResultListener;
    private DatabaseReadUserResultListener mDatabaseReadUserResultListener;
    private DatabaseUpdateResultListener mDatabaseUpdateResultListener;
    private EmailVerificationResultListener mEmailVerificationResultListener;

    public static Intent createIntent(Context context) {
        return new Intent(context, LoginActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        // Get the instances of all our Firebase objects
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        // Check if the current user is logged in and verified
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
            startActivity(MainActivity.createIntent(this));
            finish();
            return;
        }

        // Configure the GoogleSignInOptions to request the users basic profile information and their email address
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_client_id))
                .requestEmail()
                .build();

        // Build a GoogleApiClient with the settings from our GoogleSignInOptions
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleConnectionFailedListener())
                .addApi(Auth.GOOGLE_SIGN_IN_API, options)
                .build();

        // Initialize all of our Listeners
        mUserLoginResultListener = new UserLoginResultListener();
        mDatabaseReadUserResultListener = new DatabaseReadUserResultListener();
        mDatabaseUpdateResultListener = new DatabaseUpdateResultListener();
        mEmailVerificationResultListener = new EmailVerificationResultListener();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RC_REGISTER: // Registration result
                // Check if the Activity was successful
                if (resultCode == RESULT_OK) {
                    // Auto-fill the users email address and clear the previous password
                    mEmail.setText(data.getStringExtra("firebase-email"));
                    mPassword.setText(null);

                    // Notify the user that their account was created successfully and that a confirmation email was sent to them
                    showSnackbar(R.string.msg_register_success, data.getStringExtra("firebase-email"));
                }
                break;

            case RC_GOOGLE_SIGNIN: // Google sign-in result
                // Save the result from the Google Sign-In process and check if it was successful
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if (result.isSuccess()) {
                    // Log the user in using their Google account
                    firebaseAuthWithGoogle(result.getSignInAccount());
                }
                break;

            default: // Default
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @OnClick(R.id.button_login_email)
    public void onClickEmailLogin() {
        // Validate user input before continuing
        if (!validateInput()) {
            return;
        }

        // Log the user in with their Firebase email and password
        firebaseAuthWithEmailAndPassword(mEmail.getText().toString(), mPassword.getText().toString());
    }

    @OnClick(R.id.button_login_google)
    public void onClickGoogleLogin() {
        // Start Google's authentication Activity
        startActivityForResult(Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient), RC_GOOGLE_SIGNIN);
    }

    @OnClick(R.id.link_register)
    public void onClickRegister() {
        // Start the Registration Activity and log the user out (safety)
        startActivityForResult(RegisterActivity.createIntent(LoginActivity.this), RC_REGISTER);
        mAuth.signOut();
    }

    private void firebaseAuthWithEmailAndPassword(String email, String password) {
        // Display a progress dialog and disable input
        showProgressDialog(R.string.dialog_progress_login);
        enableInput(false);

        // Start the login process with an email account
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, mUserLoginResultListener);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        // Display progress dialog and disable input
        showProgressDialog(R.string.dialog_progress_login);
        enableInput(false);

        // Start the login process with a Google account
        FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(account.getIdToken(), null))
                .addOnCompleteListener(this, mUserLoginResultListener);
    }

    public boolean validateInput() {
        // Clear old input errors
        mEmail.setError(null);
        mPassword.setError(null);

        // Check email
        String email = mEmail.getText().toString();
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEmail.setError(getString(R.string.error_input_email));
            return false;
        } else mEmail.setError(null);

        // Check password
        String password = mPassword.getText().toString();
        if (password.isEmpty() || password.length() < 6) {
            mPassword.setError(getString(R.string.error_input_password));
            return false;
        } else mEmail.setError(null);

        return true;
    }

    public void enableInput(boolean enabled) {
        mEmail.setEnabled(enabled);
        mPassword.setEnabled(enabled);
        mEmailLoginButton.setEnabled(enabled);
        mGoogleLoginButton.setEnabled(enabled);
        mRegisterLink.setEnabled(enabled);
    }

    private class GoogleConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            // Log the error and notify the user
            Log.e(TAG, connectionResult.getErrorMessage());
            FirebaseCrash.report(new GoogleAuthException(connectionResult.getErrorMessage()));
            showSnackbar(R.string.error_msg_google_play);

            // Clear the users password as an extra level of security
            mPassword.setText(null);
        }
    }

    private class UserLoginResultListener implements OnCompleteListener<AuthResult> {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
            // Check if the user was logged in successfully
            if (task.isSuccessful() && task.getResult().getUser() != null) {
                // Check if the current user exists in the database
                mDatabase.getReference("users")
                        .child(mAuth.getCurrentUser().getUid())
                        .addListenerForSingleValueEvent(mDatabaseReadUserResultListener);
            } else {
                // Log the error and notify the user
                Log.e(TAG, "UserLoginResultListener", task.getException());
                FirebaseCrash.report(task.getException());
                showSnackbar(R.string.error_msg_login);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }

    private class DatabaseReadUserResultListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Check if the key exists in the database
            if (!dataSnapshot.exists()) {
                // Add the user to the database
                FirebaseUser user = mAuth.getCurrentUser();
                mDatabase.getReference("users")
                        .child(user.getUid())
                        .setValue(new DatabaseUser(user.getDisplayName(), user.getPhotoUrl().toString(), new HashMap<String, Boolean>()))
                        .addOnCompleteListener(LoginActivity.this, mDatabaseUpdateResultListener);
            } else {
                // Check if their email is verified
                if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
                    // Move the user to the MainActivity
                    startActivity(MainActivity.createIntent(LoginActivity.this));
                    finish();

                    // Remove the progress dialog since it is no longer visible
                    hideProgressDialog();
                } else {
                    // Send another verification email
                    mAuth.getCurrentUser().sendEmailVerification()
                            .addOnCompleteListener(mEmailVerificationResultListener);
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Log the error and notify the user
            Log.e(TAG, "DatabaseReadUserResultListener", databaseError.toException());
            FirebaseCrash.report(databaseError.toException());
            showSnackbar(R.string.error_msg_login);

            // Remove the progress dialog and enable input again
            hideProgressDialog();
            enableInput(true);
        }
    }

    private class DatabaseUpdateResultListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            // Check if the user was successfully written to the database
            if (task.isSuccessful()) {
                // Send a verification email (if needed)
                if (!mAuth.getCurrentUser().isEmailVerified()) {
                    mAuth.getCurrentUser().sendEmailVerification()
                            .addOnCompleteListener(LoginActivity.this, mEmailVerificationResultListener);
                } else {
                    // Move the user to the MainActivity
                    startActivity(MainActivity.createIntent(LoginActivity.this));
                    finish();

                    // Remove the progress dialog since it is no longer visible
                    hideProgressDialog();
                }
            } else {
                // Log the error and notify the user
                Log.e(TAG, "DatabaseUpdateResultListener", task.getException());
                FirebaseCrash.report(task.getException());
                showSnackbar(R.string.error_msg_login);

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
                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);

                // Notify the user that a verification email was sent to them
                showSnackbar(R.string.msg_login_verify_email, mAuth.getCurrentUser().getEmail());
            } else {
                // Log the error and notify the user
                Log.e(TAG, "EmailVerificationResultListener", task.getException());
                FirebaseCrash.report(task.getException());
                showSnackbar(R.string.error_msg_login);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }
}
