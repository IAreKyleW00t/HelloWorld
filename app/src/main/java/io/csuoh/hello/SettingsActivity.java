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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsActivity extends BaseActivity {
    public static final String TAG = SettingsActivity.class.getSimpleName();
    public static final String PREFS_NAME = "HelloWorld";
    public static final int
            RC_OPEN_FILE    = 420;

    // Activity elements
    @BindView(R.id.image_user_picture) ImageView mProfilePicture;
    @BindView(R.id.input_user_name) EditText mName;
    @BindView(R.id.check_notifications) Switch mEnableNotifications;
    @BindView(R.id.text_user_email) TextView mEmail;
    @BindView(R.id.link_password_change) TextView mChangePasswordLink;
    @BindView(R.id.text_user_id) TextView mUserId;
    @BindView(R.id.text_user_provider) TextView mProvider;
    @BindView(R.id.button_save) Button mSaveButton;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseStorage mStorage;
    private FirebaseDatabase mDatabase;

    // Listeners
    private ReloadUserProfileResultListener mReloadUserProfileResultListener;
    private UserChangePasswordResultListener mUserChangePasswordResultListener;
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
        showHomeUpButton(true);
        ButterKnife.bind(this);

        // Get the instances of all our Firebase objects
        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        // Initialize all of our Listeners
        mReloadUserProfileResultListener = new ReloadUserProfileResultListener();
        mUserChangePasswordResultListener = new UserChangePasswordResultListener();
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
                        // Use ButterKnife to bind dialog elements
                        EditText passwordCurrentInput = ButterKnife.findById(dialog, R.id.input_password_current);
                        EditText passwordNewInput = ButterKnife.findById(dialog, R.id.input_password_new);
                        EditText passwordConfirmInput = ButterKnife.findById(dialog, R.id.input_password_confirm);

                        // Clear old error messages
                        passwordCurrentInput.setError(null);
                        passwordNewInput.setError(null);
                        passwordConfirmInput.setError(null);

                        // Validate user input before continuing
                        String currentPassword = passwordCurrentInput.getText().toString();
                        if (currentPassword.isEmpty()) {
                            passwordCurrentInput.setError(getString(R.string.error_input_password_current));
                            return;
                        }

                        String newPassword = passwordNewInput.getText().toString();
                        if (newPassword.isEmpty() || newPassword.length() < 6) {
                            passwordNewInput.setError(getString(R.string.error_input_password_new));
                            return;
                        }

                        String confirmPassword = passwordConfirmInput.getText().toString();
                        if (!confirmPassword.equals(newPassword)) {
                            passwordConfirmInput.setError(getString(R.string.error_input_password_confirm));
                            return;
                        }

                        // Remove the current dialog and display the progress dialog
                        dialog.dismiss();
                        showProgressDialog(R.string.dialog_progress_change_password);

                        // Automatically re-authenticate the current user before we change their password.
                        // This is required by Firebase after a certain period of time
                        AuthCredential credential = EmailAuthProvider.getCredential(mAuth.getCurrentUser().getEmail(), currentPassword);
                        mAuth.getCurrentUser().reauthenticate(credential)
                                .addOnCompleteListener(SettingsActivity.this, new UserReauthenicateResultListener(newPassword));
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

        // Update local preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        settings.edit()
                .putBoolean("notifications", mEnableNotifications.isChecked())
                .apply();

        // Update the current users profile based on what is editable in the settings Activity
        UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                .setDisplayName(mName.getText().toString())
                .build();

        mAuth.getCurrentUser().updateProfile(profile)
                .addOnCompleteListener(this, mUserProfileUpdateResultListener);
    }

    private void loadUserInfo() {
        // Display a progress dialog and disable input
        showProgressDialog(R.string.dialog_progress_loading);
        enableInput(false);

        // Load local preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mEnableNotifications.setChecked(settings.getBoolean("notifications", true));

        // Manually reload the current profile from Firebase just in case any changes were made
        mAuth.getCurrentUser().reload()
                .addOnCompleteListener(this, mReloadUserProfileResultListener);
    }

    private void firebaseUploadProfilePicture(Uri file) {
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
        mProfilePicture.setClickable(enabled);
        mName.setEnabled(enabled);
        mChangePasswordLink.setEnabled(enabled);
        mSaveButton.setEnabled(enabled);
    }

    private class ReloadUserProfileResultListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            // Save current user reference
            FirebaseUser user = mAuth.getCurrentUser();

            // Check if reloading the users profile was successful
            // Even if this fails we can still display the current information, it just
            // has the possibility of not being in sync (which is very unlikely)
            if (!task.isSuccessful() ) {
                // Log the error before continuing
                Log.e(TAG, "ReloadUserProfileResultListener", task.getException());
                FirebaseCrash.report(task.getException());
            }

            // Display the current users profile picture
            Picasso.with(SettingsActivity.this)
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.default_user_picture)
                    .into(mProfilePicture);

            // Display the current users remaining information
            mName.setText(user.getDisplayName());
            mEmail.setText(user.getEmail());
            mUserId.setText(user.getUid());
            switch (user.getProviders().get(0)) {
                case "google.com": // Google
                    mProvider.setText(getString(R.string.provider_google));
                    break;
                case "facebook.com": // Facebook
                    mProvider.setText(getString(R.string.provider_facebook));
                    break;
                case "twitter.com": // Twitter
                    mProvider.setText(getString(R.string.provider_twitter));
                    break;
                case "github.com": // GitHub
                    mProvider.setText(getString(R.string.provider_github));
                    break;
                default: // Default to Firebase
                    mProvider.setText(getString(R.string.provider_firebase));
                    mChangePasswordLink.setVisibility(View.VISIBLE);
                    break;
            }

            // Remove the progress dialog and enable input again
            hideProgressDialog();
            enableInput(true);
        }
    }

    private class UserReauthenicateResultListener implements OnCompleteListener<Void> {
        private final String password;

        private UserReauthenicateResultListener(final String password) {
            this.password = password;
        }

        @Override
        public void onComplete(@NonNull Task<Void> task) {
            // Check if the user re-authenticated themselves successfully
            if (task.isSuccessful()) {
                mAuth.getCurrentUser().updatePassword(password)
                        .addOnCompleteListener(SettingsActivity.this, mUserChangePasswordResultListener);
            } else {
                // Log the error and notify the user
                Log.e(TAG, "UserReauthenicateResultListener", task.getException());
                FirebaseCrash.report(task.getException());
                showSnackbar(R.string.error_msg_reauthenticate);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }

    private class UserChangePasswordResultListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            // Check if the users password was updated successfully
            if (task.isSuccessful()) {
                // Tell the user their password was updated successfully
                showSnackbar(R.string.msg_password_update);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            } else {
                // Log the error and notify the user
                Log.e(TAG, "UserChangePasswordResultListener", task.getException());
                FirebaseCrash.report(task.getException());
                showSnackbar(R.string.error_msg_password_update);

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
                // Update the users profile in the database
                FirebaseUser user = mAuth.getCurrentUser();
                mDatabase.getReference("users")
                        .child(user.getUid()).child("name")
                        .setValue(user.getDisplayName())
                        .addOnCompleteListener(SettingsActivity.this, mDatabaseUpdateProfileResultListener);
            } else {
                // Log the error and notify the user
                Log.e(TAG, "UserProfileUpdateResultListener", task.getException());
                FirebaseCrash.report(task.getException());
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
                mDatabase.getReference()
                        .child("users").child(user.getUid()).child("photo")
                        .setValue(user.getPhotoUrl().toString())
                        .addOnCompleteListener(SettingsActivity.this, mDatabaseUpdatePhotoResultListener);
            } else {
                // Log the error and notify the user
                Log.e(TAG, "UserProfilePhotoUpdateResultListener", task.getException());
                FirebaseCrash.report(task.getException());
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
            // Log the error and notify the user
            Log.e(TAG, "StorageUploadResultListener", exception);
            FirebaseCrash.report(exception);
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
                // Log the error and notify the user
                Log.e(TAG, "DatabaseUpdatePhotoResultListener", task.getException());
                FirebaseCrash.report(task.getException());
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
                // Log the error and notify the user
                Log.e(TAG, "DatabaseUpdateProfileResultListener", task.getException());
                FirebaseCrash.report(task.getException());
                showSnackbar(R.string.error_msg_profile_update);

                // Remove the progress dialog and enable input again
                hideProgressDialog();
                enableInput(true);
            }
        }
    }
}
