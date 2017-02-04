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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RegisterActivity extends BaseActivity {
    public static final String TAG = RegisterActivity.class.getSimpleName();

    // Activity elements
    @BindView(R.id.input_name) EditText _name;
    @BindView(R.id.input_email) EditText _email;
    @BindView(R.id.input_password) EditText _password;
    @BindView(R.id.input_confirm_password) EditText _confirmPassword;
    @BindView(R.id.btn_register) Button _register;
    @BindView(R.id.link_login) TextView _login;

    public static Intent createIntent(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, RegisterActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.btn_register)
    public void onClickRegister() {
        // Validate form data
        if (!validateForm()) return;

        // Display progress dialog and disable input
        enableInput(false);
        showProgressDialog(R.string.dialog_register);

        // Attempt to create the new account
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(_email.getText().toString(), _password.getText().toString())
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        FirebaseUser user = authResult.getUser();

                        // Set the users display name as the name they provided
                        // and send a confirmation email to the them automatically
                        UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                                .setDisplayName(_name.getText().toString())
                                .build();
                        user.updateProfile(profile);
                        user.sendEmailVerification();

                        // Set result to successful, add user information, and return to previous Activity
                        Intent data = new Intent()
                                .putExtra("firebase_uid", user.getUid())
                                .putExtra("firebase_email", user.getEmail());
                        setResult(RESULT_OK, data);
                        finish();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Log error internally and display it to the user
                        Log.e(TAG, "createUserWithEmailAndPassword", e);
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

    @OnClick(R.id.link_login)
    public void onClickLogin() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private boolean validateForm() {
        // Clear old input errors
        _name.setError(null);
        _email.setError(null);
        _password.setError(null);
        _confirmPassword.setError(null);

        String name = _name.getText().toString();
        if (name.isEmpty()) {
            _name.setError(getString(R.string.err_bad_name));
            return false;
        }

        String email = _email.getText().toString();
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _email.setError(getString(R.string.err_bad_email));
            return false;
        }

        String password = _password.getText().toString();
        if (password.isEmpty() || password.length() < 6) {
            _password.setError(getString(R.string.err_bad_password));
            return false;
        }

        String password2 = _confirmPassword.getText().toString();
        if (!password2.equals(password)) {
            _confirmPassword.setError(getString(R.string.err_mismatch_passwords));
            return false;
        }

        return true;
    }

    private void enableInput(boolean enabled) {
        _name.setEnabled(enabled);
        _email.setEnabled(enabled);
        _password.setEnabled(enabled);
        _confirmPassword.setEnabled(enabled);
        _register.setEnabled(enabled);
        _login.setEnabled(enabled);
    }
}
