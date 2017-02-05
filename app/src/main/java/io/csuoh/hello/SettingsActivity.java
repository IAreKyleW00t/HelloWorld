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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsActivity extends BaseActivity {
    public static final String TAG = SettingsActivity.class.getSimpleName();

    // Activity elements
    @BindView(R.id.user_picture) ImageView _picture;
    @BindView(R.id.user_display_name) EditText _name;
    @BindView(R.id.user_email) TextView _email;
    @BindView(R.id.link_change_password) TextView _changePassword;
    @BindView(R.id.user_id) TextView _uid;
    @BindView(R.id.user_provider) TextView _provider;
    @BindView(R.id.btn_save) Button _save;

    // Firebase
    FirebaseUser mUser;

    public static Intent createIntent(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, SettingsActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.title_settings);
        ButterKnife.bind(this);

        // Save current user and display info in UI
        // It is not possible to be "logged out" at this point
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        loadUserInfo();
    }

    private void loadUserInfo() {
        // Display progress dialog and disable input
        enableInput(false);
        showProgressDialog(R.string.dialog_load);

        // Attempt to manually reload the current users profile
        mUser.reload()
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Load users profile picture
                        Glide.with(SettingsActivity.this)
                                .load(mUser.getPhotoUrl())
                                .placeholder(R.drawable.default_user_picture)
                                .into(_picture);

                        // Display the users information
                        _name.setText(mUser.getDisplayName());
                        _email.setText(mUser.getEmail());
                        _uid.setText(mUser.getUid());
                        switch (mUser.getProviders().get(0)) {
                            case "google.com": // Google
                                _provider.setText(getString(R.string.provider_google));
                                break;
                            case "facebook.com": // Facebook
                                _provider.setText(getString(R.string.provider_facebook));
                                break;
                            case "twitter.com": // Twitter
                                _provider.setText(getString(R.string.provider_twitter));
                                break;
                            case "github.com": // GitHub
                                _provider.setText(getString(R.string.provider_github));
                                break;
                            default: // Default to Firebase
                                _provider.setText(getString(R.string.provider_firebase));
                                _changePassword.setVisibility(View.VISIBLE);
                                break;
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Log error internally and display it to the user
                        Log.e(TAG, "reload", e);
                        showSnackbar(R.string.err_show, e.getMessage());
                    }
                })
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Automatically dismiss the progress dialog and enable input again
                        enableInput(true);
                        hideProgressDialog();
                    }
                });
    }

    @OnClick(R.id.link_change_password)
    public void onChangePassword() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.title_change_password)
                .content(R.string.debug_wip)
                .positiveText(R.string.btn_confirm)
                .build();
        dialog.show();
    }

    @OnClick(R.id.btn_save)
    public void onSave() {
        // Display progress dialog and disable input
        enableInput(false);
        showProgressDialog(R.string.dialog_save);

        // Create the UserProfileChange request to include all changeable data
        UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                .setDisplayName(_name.getText().toString())
                .build();

        // Attempt to update the current users profiles
        mUser.updateProfile(profile)
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        showToast(R.string.msg_save);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Log error internally and display it to the user
                        Log.e(TAG, "reload", e);
                        showSnackbar(R.string.err_show, e.getMessage());
                    }
                })
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Automatically dismiss the progress dialog and enable input again
                        enableInput(true);
                        hideProgressDialog();
                    }
                });
    }

    private void enableInput(boolean enabled) {
        _name.setEnabled(enabled);
        _changePassword.setEnabled(enabled);
        _save.setEnabled(enabled);
    }
}
