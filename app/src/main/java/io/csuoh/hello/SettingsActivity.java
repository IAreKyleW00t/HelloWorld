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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsActivity extends BaseActivity {
    public static final String TAG = SettingsActivity.class.getSimpleName();

    // Request codes
    private static final int
            RC_OPEN_FILE    = 420;

    // Activity elements
    @BindView(R.id.image_user_picture) ImageView userPictureImage;
    @BindView(R.id.input_user_name) EditText userNameInput;
    @BindView(R.id.text_user_email) TextView userEmailText;
    @BindView(R.id.link_password_change) TextView passwordChangeLink;
    @BindView(R.id.text_user_id) TextView userIdText;
    @BindView(R.id.text_user_provider) TextView userProviderText;
    @BindView(R.id.button_save) Button saveButton;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseStorage mStorage;
    private FirebaseDatabase mDatabase;

    // Listeners
    private ReloadUserProfileResultListener mReloadUserProfileResultListener;
    private UserProfileUpdateResultListener mUserProfileUpdateResultListener;
    private UserProfilePhotoUpdateResultListener mUserProfilePhotoUpdateResultListener;
    private StorageUploadResultListener mStorageUploadResultListener;
    private DatabaseUpdateProfileResultListener mDatabaseUpdateProfileResultListener;
    private DatabaseUpdatePhotoResultListener mDatabaseUpdatePhotoResultListener;

    public static Intent createIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.title_settings);
        ButterKnife.bind(this);

        // Get the instances of all our Firebase objects
        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        // Initialize all of our Listeners
        mReloadUserProfileResultListener = new ReloadUserProfileResultListener();
        mUserProfileUpdateResultListener = new UserProfileUpdateResultListener();
        mUserProfilePhotoUpdateResultListener = new UserProfilePhotoUpdateResultListener();
        mStorageUploadResultListener = new StorageUploadResultListener();
        mDatabaseUpdateProfileResultListener = new DatabaseUpdateProfileResultListener();
        mDatabaseUpdatePhotoResultListener = new DatabaseUpdatePhotoResultListener();

        // Load the current users info to the UI
        loadUserInfo();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RC_OPEN_FILE: // Open file
                if (resultCode == RESULT_OK) { // File selected
                    firebaseUploadProfilePicture(data.getData());
                }
                break;
            default: // Default to super
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @OnClick(R.id.image_user_picture)
    public void onClickProfilePicture() {
        startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*"), RC_OPEN_FILE);
    }

    @OnClick(R.id.link_password_change)
    public void onClickChangePassword() {
        // Disable input in the background
        enableInput(false);

        // TODO Clean up change password Dialog
        // Create and display the change password dialog
        new MaterialDialog.Builder(this)
                .title(R.string.dialog_title_change_password)
                .customView(R.layout.dialog_change_password, true)
                .cancelable(false)
                .autoDismiss(false)
                .positiveText(R.string.action_confirm)
                .negativeText(R.string.action_cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        // Saved View and elements
                        View mView = dialog.getCustomView();
                        EditText _currentPassword = (EditText) mView.findViewById(R.id.input_password_current);
                        EditText _newPassword = (EditText) mView.findViewById(R.id.input_password_new);
                        EditText _confirmPassword = (EditText) mView.findViewById(R.id.input_password_confirm);

                        // Clear old input errors
                        _currentPassword.setError(null);
                        _newPassword.setError(null);
                        _confirmPassword.setError(null);

                        // Validate user input
                        final String currentPassword = _currentPassword.getText().toString();
                        if (currentPassword.isEmpty()) {
                            _currentPassword.setError(getString(R.string.error_input_password_current));
                            return;
                        }

                        final String newPassword = _newPassword.getText().toString();
                        if (newPassword.isEmpty() || newPassword.length() < 6) {
                            _newPassword.setError(getString(R.string.error_input_password_new));
                            return;
                        }

                        final String confirmPassword = _confirmPassword.getText().toString();
                        if (!confirmPassword.equals(newPassword)) {
                            _confirmPassword.setError(getString(R.string.error_input_password_confirm));
                            return;
                        }

                        // Dismiss the current dialog and display the progress dialog
                        dialog.dismiss();
                        showProgressDialog(R.string.dialog_progress_change_password);

                        // Automatically re-authenticate the current user before
                        // we change their password. This is required by Firebase
                        AuthCredential credential = EmailAuthProvider.getCredential(mAuth.getCurrentUser().getEmail(), currentPassword);
                        mAuth.getCurrentUser().reauthenticate(credential)
                                .addOnSuccessListener(SettingsActivity.this, new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Attempt to update the current users password after
                                        // they have re-authenticated successfully
                                        mAuth.getCurrentUser().updatePassword(newPassword)
                                                .addOnSuccessListener(SettingsActivity.this, new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        showSnackbar(R.string.msg_password_update);
                                                    }
                                                })
                                                .addOnFailureListener(SettingsActivity.this, new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.e(TAG, "updatePassword", e);
                                                        showSnackbar(R.string.error_msg_password_update);
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
                                        showSnackbar(R.string.error_msg_reauthenticate);

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

    @OnClick(R.id.button_save)
    public void onClickSave() {
        // Display progress dialog and disable input
        enableInput(false);
        showProgressDialog(R.string.dialog_progress_save);

        // Update the current users profile based on what is editable in the settings Activity
        UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                .setDisplayName(userNameInput.getText().toString())
                .build();

        mAuth.getCurrentUser().updateProfile(profile)
                .addOnCompleteListener(this, mUserProfileUpdateResultListener);
    }

    private void loadUserInfo() {
        // Display a progress dialog and disable input
        showProgressDialog(R.string.dialog_progress_loading);
        enableInput(false);

        // Manually reload the current profile from Firebase just in case any changes were made
        mAuth.getCurrentUser().reload()
                .addOnCompleteListener(this, mReloadUserProfileResultListener);
    }

    private void firebaseUploadProfilePicture(final Uri file) {
        // Display progress dialog and disable input
        enableInput(false);
        showProgressDialog(R.string.dialog_progress_upload);

        // Update the users profile picture by first uploading it to Firebase Storage
        mStorage.getReferenceFromUrl("gs://" + getString(R.string.firebase_bucket))
                .child("user").child(mAuth.getCurrentUser().getUid()).child("profilePicture")
                .putFile(file)
                // UploadTasks do not support OnCompleteListeners...?
                .addOnSuccessListener(this, mStorageUploadResultListener)
                .addOnFailureListener(this, mStorageUploadResultListener);
    }

    public void enableInput(boolean enabled) {
        userPictureImage.setClickable(enabled);
        userNameInput.setEnabled(enabled);
        passwordChangeLink.setEnabled(enabled);
        saveButton.setEnabled(enabled);
    }

    private class ReloadUserProfileResultListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            // Save current user reference
            FirebaseUser user = mAuth.getCurrentUser();

            // Check if reloading the users profile was successful
            // Even if this fails we can still display the current information, it just
            // has the posibility of not being in sync (which is very unlikely)
            if (!task.isSuccessful() ) {
                // Log the error internally before continuing
                Log.e(TAG, "ReloadUserProfileResultListener", task.getException());
            }

            // Display the current users profile picture
            Glide.with(SettingsActivity.this)
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.default_user_picture)
                    .centerCrop()
                    .dontAnimate()
                    .into(userPictureImage);

            // Display the current users remaining information
            userNameInput.setText(user.getDisplayName());
            userEmailText.setText(user.getEmail());
            userIdText.setText(user.getUid());
            switch (user.getProviders().get(0)) {
                case "google.com": // Google
                    userProviderText.setText(getString(R.string.provider_google));
                    break;
                case "facebook.com": // Facebook
                    userProviderText.setText(getString(R.string.provider_facebook));
                    break;
                case "twitter.com": // Twitter
                    userProviderText.setText(getString(R.string.provider_twitter));
                    break;
                case "github.com": // GitHub
                    userProviderText.setText(getString(R.string.provider_github));
                    break;
                default: // Default to Firebase
                    userProviderText.setText(getString(R.string.provider_firebase));
                    passwordChangeLink.setVisibility(View.VISIBLE);
                    break;
            }

            // Remove the progress dialog and enable input again
            hideProgressDialog();
            enableInput(true);
        }
    }

    private class UserProfileUpdateResultListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            // Check if the users profile was updated successfully
            if (task.isSuccessful()) {
                // Update the users profile in the database
                FirebaseUser user = mAuth.getCurrentUser();
                mDatabase.getReference("users")
                        .child(user.getUid()).child("name")
                        .setValue(user.getDisplayName())
                        .addOnCompleteListener(SettingsActivity.this, mDatabaseUpdateProfileResultListener);
            } else {
                // Log the error internally and notify the user
                Log.e(TAG, "UserProfileUpdateResultListener", task.getException());
                showSnackbar(R.string.error_msg_profile_update);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }

    private class UserProfilePhotoUpdateResultListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            // Check if the users profile was updated successfully
            if (task.isSuccessful()) {
                // Update the users photo URL in the database
                FirebaseUser user = mAuth.getCurrentUser();
                mDatabase.getReference("users")
                        .child(user.getUid()).child("photo")
                        .setValue(user.getPhotoUrl().toString())
                        .addOnCompleteListener(SettingsActivity.this, mDatabaseUpdatePhotoResultListener);
            } else {
                // Log the error internally and notify the user
                Log.e(TAG, "UserProfilePhotoUpdateResultListener", task.getException());
                showSnackbar(R.string.error_msg_profile_picture_update);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }

    private class StorageUploadResultListener implements OnSuccessListener<UploadTask.TaskSnapshot>, OnFailureListener {
        @Override
        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
            // Update the current users profile to use the newly uploaded picture
            UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(taskSnapshot.getDownloadUrl())
                    .build();

            mAuth.getCurrentUser().updateProfile(profile)
                    .addOnCompleteListener(SettingsActivity.this, mUserProfilePhotoUpdateResultListener);
        }

        @Override
        public void onFailure(@NonNull Exception exception) {
            // Log the error internally and notify the user
            Log.e(TAG, "StorageUploadResultListener", exception);
            showSnackbar(R.string.error_msg_profile_picture_update);

            // Remove the progress dialog and enable input again
            hideProgressDialog();
            enableInput(true);
        }
    }

    private class DatabaseUpdatePhotoResultListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            // Check if the photo value was successfully updated
            if (task.isSuccessful()) {
                // Reload the current users profile and tell them profile picture was updated successfully
                // This will automatically remove the progress dialog and enable input again
                loadUserInfo();
                showSnackbar(R.string.msg_profile_picture_update);
            } else {
                // Log the error internally and notify the user
                Log.e(TAG, "DatabaseUpdatePhotoResultListener", task.getException());
                showSnackbar(R.string.error_msg_profile_picture_update);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }

    private class DatabaseUpdateProfileResultListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            // Check if the user profile was successfully updated
            if (task.isSuccessful()) {
                // Tell the user their settings were saved successfully
                showSnackbar(R.string.msg_profile_update);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            } else {
                // Log the error internally and notify the user
                Log.e(TAG, "DatabaseUpdateProfileResultListener", task.getException());
                showSnackbar(R.string.error_msg_profile_update);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }
}
