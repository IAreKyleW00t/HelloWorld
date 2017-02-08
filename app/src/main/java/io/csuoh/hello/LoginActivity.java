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

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends BaseActivity implements GoogleApiClient.OnConnectionFailedListener {
    public static final String TAG = LoginActivity.class.getSimpleName();

    // Request codes
    private static final int
            RC_GOOGLE_SIGNIN    = 42,
            RC_REGISTER         = 69;

    // Activity elements
    @BindView(R.id.input_email) EditText _email;
    @BindView(R.id.input_password) EditText _password;
    @BindView(R.id.btn_login) Button _login;
    @BindView(R.id.btn_login_google) SignInButton _google;
    @BindView(R.id.link_register) TextView _register;

    // Google
    private GoogleApiClient mGoogleApiClient;

    public static Intent createIntent(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, LoginActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        // Check if user is already logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.isEmailVerified()) { // Logged in w/ verified emailInput
            startActivity(MainActivity.createIntent(this));
            finish();
            return;
        }

        // Configure Google Sign in to request user ID, emailInput, etc.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.token_google_client_id))
                .requestEmail()
                .build();

        // Build GoogleApiClient with settings from GoogleSignInOptions
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RC_REGISTER: // DatabaseUser register
                if (resultCode == RESULT_OK) { // Successful registration
                    _email.setText(data.getStringExtra("firebase-email"));
                    _password.setText(null);
                    showSnackbar(R.string.msg_verify_email);
                }
                break;
            case RC_GOOGLE_SIGNIN: // Google sign in
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if (result.isSuccess()) { // Successfully linked Google account
                    GoogleSignInAccount account = result.getSignInAccount();
                    firebaseAuthWithGoogle(account);
                }
                break;
            default: // Default to super
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "onConnectionFailed:" + connectionResult);
        showToast(R.string.err_google_play);
    }

    @OnClick(R.id.btn_login)
    public void onClickEmailSignIn() {
        if (!validateInput()) return;
        firebaseAuthWithEmailAndPassword(_email.getText().toString(), _password.getText().toString());
    }

    @OnClick(R.id.btn_login_google)
    public void onClickGoogleSignIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_GOOGLE_SIGNIN);
    }

    @OnClick(R.id.link_register)
    public void onClickRegister() {
        FirebaseAuth.getInstance().signOut(); // Safety precaution
        startActivityForResult(RegisterActivity.createIntent(LoginActivity.this), RC_REGISTER);
    }

    private void firebaseAuthWithEmailAndPassword(String email, String password) {
        // Display progress dialog and disable input
        enableInput(false);
        showProgressDialog(R.string.dialog_login);

        // Attempt to sign in with emailInput and password
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        FirebaseUser user = authResult.getUser();

                        // Mover user to MainActivity if verified,
                        // otherwise send another confirmation emailInput
                        if (user.isEmailVerified()) {
                            startActivity(MainActivity.createIntent(LoginActivity.this));
                            finish();
                        } else {
                            user.sendEmailVerification();
                            showSnackbar(R.string.msg_verify_email);
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Log error internally and display it to the user
                        Log.e(TAG, "signInWithEmailAndPassword", e);
                        showSnackbar(R.string.err_show, e.getMessage());
                    }
                })
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Automatically dismiss the progress dialog and enable input again
                        enableInput(true);
                        hideProgressDialog();
                    }
                });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        // Display progress dialog and disable input
        enableInput(false);
        showProgressDialog(R.string.dialog_login);

        // Attempt to sign in with Google account
        FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(account.getIdToken(), null))
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        FirebaseUser user = authResult.getUser();

                        // Mover user to MainActivity if verified,
                        // otherwise send another confirmation emailInput
                        // This shouldn't be needed for Google accounts, but just to be safe...
                        if (user.isEmailVerified()) {
                            startActivity(MainActivity.createIntent(LoginActivity.this));
                            finish();
                        } else {
                            user.sendEmailVerification();
                            showSnackbar(R.string.msg_verify_email);
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Log error internally and display it to the user
                        Log.e(TAG, "signInWithCredential", e);
                        showSnackbar(R.string.err_show, e.getMessage());
                    }
                })
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Automatically dismiss the progress dialog and enable input again
                        enableInput(true);
                        hideProgressDialog();
                    }
                });
    }

    public boolean validateInput() {
        // Clear old input errors
        _email.setError(null);
        _password.setError(null);

        String email = _email.getText().toString();
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _email.setError(getString(R.string.err_bad_email));
            return false;
        } else _email.setError(null);

        String password = _password.getText().toString();
        if (password.isEmpty() || password.length() < 6) {
            _password.setError(getString(R.string.err_bad_password));
            return false;
        } else _email.setError(null);

        return true;
    }

    public void enableInput(boolean enabled) {
        _email.setEnabled(enabled);
        _password.setEnabled(enabled);
        _login.setEnabled(enabled);
        _google.setEnabled(enabled);
        _register.setEnabled(enabled);
    }
}
