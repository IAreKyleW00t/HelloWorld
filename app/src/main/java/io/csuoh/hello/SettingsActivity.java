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
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;

public class SettingsActivity extends BaseActivity {
    public static final String TAG = SettingsActivity.class.getSimpleName();

    // Request codes
    private static final int
            RC_OPEN_FILE    = 420;

    // Activity elements
    @BindView(R.id.user_picture) ImageView _picture;
    @BindView(R.id.user_display_name) EditText _name;
    @BindView(R.id.user_email) TextView _email;
    @Nullable @BindView(R.id.link_change_password) TextView _changePassword;
    @BindView(R.id.user_id) TextView _uid;
    @BindView(R.id.user_provider) TextView _provider;
    @BindView(R.id.btn_save) Button _save;

    // Firebase
    FirebaseStorage mStorage;
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

        // Save the current Firebase Storage reference
        mStorage = FirebaseStorage.getInstance();

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
                                .centerCrop()
                                .placeholder(R.drawable.default_user_picture)
                                .dontAnimate()
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RC_OPEN_FILE: // Open file
                if (resultCode == RESULT_OK) { // File selected
                    firebaseUploadUserPhoto(data.getData());
                }
                break;
            default: // Default to super
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @OnClick(R.id.user_picture)
    public void onChangeProfilePicture() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*");
        startActivityForResult(intent, RC_OPEN_FILE);
    }

    @Optional
    @OnClick(R.id.link_change_password)
    public void onChangePassword() {
        // Disable input in the background
        enableInput(false);

        // Create and display the change password dialog
        new MaterialDialog.Builder(this)
                .title(R.string.title_change_password)
                .customView(R.layout.dialog_change_password, true)
                .cancelable(false)
                .autoDismiss(false)
                .positiveText(R.string.btn_confirm)
                .negativeText(R.string.btn_cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        // Saved View and elements
                        View mView = dialog.getCustomView();
                        EditText _currentPassword = (EditText) mView.findViewById(R.id.input_current_password);
                        EditText _newPassword = (EditText) mView.findViewById(R.id.input_new_password);
                        EditText _confirmPassword = (EditText) mView.findViewById(R.id.input_confirm_password);

                        // Clear old input errors
                        _currentPassword.setError(null);
                        _newPassword.setError(null);
                        _confirmPassword.setError(null);

                        // Validate user input
                        final String currentPassword = _currentPassword.getText().toString();
                        if (currentPassword.isEmpty()) {
                            _currentPassword.setError(getString(R.string.err_empty_input));
                            return;
                        }

                        final String newPassword = _newPassword.getText().toString();
                        if (newPassword.isEmpty() || newPassword.length() < 6) {
                            _newPassword.setError(getString(R.string.err_bad_password));
                            return;
                        }

                        final String confirmPassword = _confirmPassword.getText().toString();
                        if (!confirmPassword.equals(newPassword)) {
                            _confirmPassword.setError(getString(R.string.err_mismatch_passwords));
                            return;
                        }

                        // Dismiss the current dialog and display the progress dialog
                        dialog.dismiss();
                        showProgressDialog(R.string.dialog_change_password);

                        // Automatically re-authenticate the current user before
                        // we change their password. This is required by Firebase
                        AuthCredential credential = EmailAuthProvider.getCredential(mUser.getEmail(), currentPassword);
                        mUser.reauthenticate(credential)
                                .addOnSuccessListener(SettingsActivity.this, new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Attempt to update the current users password after
                                        // they have re-authenticated successfully
                                        mUser.updatePassword(newPassword)
                                                .addOnSuccessListener(SettingsActivity.this, new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        showSnackbar(R.string.msg_password_updated);
                                                    }
                                                })
                                                .addOnFailureListener(SettingsActivity.this, new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.e(TAG, "updatePassword", e);
                                                        showSnackbar(R.string.err_failed_password_change);
                                                    }
                                                })
                                                .addOnCompleteListener(SettingsActivity.this, new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        // Automatically dismiss the progress dialog and enable input again
                                                        enableInput(true);
                                                        hideProgressDialog();
                                                    }
                                                });
                                    }
                                })
                                .addOnFailureListener(SettingsActivity.this, new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Log error internally and display it to the user
                                        Log.e(TAG, "updatePassword", e);
                                        showSnackbar(R.string.err_failed_reauth);

                                        // Automatically dismiss the progress dialog and enable input again
                                        enableInput(true);
                                        hideProgressDialog();
                                    }
                                });
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        // Dismiss the dialog and enable input again
                        dialog.dismiss();
                        enableInput(true);
                    }
                })
                .show();
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
                        showSnackbar(R.string.msg_settings_save);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Log error internally and display it to the user
                        Log.e(TAG, "reload", e);
                        showSnackbar(R.string.err_failed_settings_save);
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

    private void firebaseUploadUserPhoto(final Uri file) {
        // Display progress dialog and disable input
        enableInput(false);
        showProgressDialog(R.string.dialog_uploading);

        // Create a reference to the file that will be stored
        // For simplicity, we make the filename the same as the users ID
        StorageReference storageReference = mStorage
                .getReferenceFromUrl("gs://" + getString(R.string.app_firebase_bucket))
                .child("user/" + mUser.getUid() + "/profilePicture");

        // Create a new task to upload the file
        UploadTask task = storageReference.putFile(file);
        task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Update the users photo URL to the one that was uploaded
                UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                        .setPhotoUri(taskSnapshot.getDownloadUrl())
                        .build();
                mUser.updateProfile(profile);

                // Reload users profile picture from a local file so the change is instant
                Glide.with(SettingsActivity.this)
                        .load(file)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.NONE) // Don't cache local file
                        .placeholder(R.drawable.default_user_picture)
                        .into(_picture);

                // Automatically dismiss the progress dialog, enable input again, and tell
                // the user their action was successful
                enableInput(true);
                hideProgressDialog();
                showSnackbar(R.string.msg_picture_updated);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Log error internally and display it to the user
                Log.e(TAG, "putFile", e);
                showSnackbar(R.string.err_failed_file_upload);

                // Automatically dismiss the progress dialog, enable input again, and
                // reload the users info
                enableInput(true);
                hideProgressDialog();
            }
        });
    }

    public void enableInput(boolean enabled) {
        _picture.setEnabled(enabled);
        _name.setEnabled(enabled);
        _changePassword.setEnabled(enabled);
        _save.setEnabled(enabled);
    }
}
