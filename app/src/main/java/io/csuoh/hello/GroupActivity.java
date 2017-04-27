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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.csuoh.hello.adapters.MessageAdapter;
import io.csuoh.hello.models.DatabaseGroup;
import io.csuoh.hello.models.DatabaseMessage;

public class GroupActivity extends BaseActivity {
    public static final String TAG = GroupActivity.class.getSimpleName();

    // Activity elements
    @BindView(R.id.list_messages) RecyclerView mRecyclerView;
    @BindView(R.id.input_message) EditText mMessage;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;

    // Group
    private DatabaseGroup mGroup;

    // Messages
    private DatabaseMessage mLastMessage;
    private List<DatabaseMessage> mMessages = new ArrayList<>();
    private MessageAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;

    // Listeners
    private DatabaseReadMessageListener mDatabaseReadMessageListener;
    private DatabaseUserGroupRemovedListener mDatabaseUserGroupRemovedListener;
    private DatabaseGroupUserRemovedListener mDatabaseGroupUserRemovedListener;
    private DatabaseWriteMessageListener mDatabaseWriteMessageListener;
    private DatabaseUpdateGroupMessageListener mDatabaseUpdateGroupMessageListener;

    public static Intent createIntent(Context context) {
        return new Intent(context, GroupActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);
        ButterKnife.bind(this);

        // Get the instances of all our Firebase objects
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        // Check if the current user is logged in and verified
        if (mAuth.getCurrentUser() == null || !mAuth.getCurrentUser().isEmailVerified()) {
            startActivity(LoginActivity.createIntent(this));
            finish();

            // Notify the user that they were logged out
            showToast(R.string.msg_reauthenticate);
        }

        // Initialize all of our Listeners
        mDatabaseReadMessageListener = new DatabaseReadMessageListener();
        mDatabaseUserGroupRemovedListener = new DatabaseUserGroupRemovedListener();
        mDatabaseGroupUserRemovedListener = new DatabaseGroupUserRemovedListener();
        mDatabaseWriteMessageListener = new DatabaseWriteMessageListener();
        mDatabaseUpdateGroupMessageListener = new DatabaseUpdateGroupMessageListener();

        // Check for required information from the Intent
        Bundle extras = getIntent().getExtras();
        if (extras == null || !extras.containsKey("group")) {
            showToast(R.string.error_msg_group_join);
            finish();
        }

        // Save information
        mGroup = Parcels.unwrap(extras.getParcelable("group"));

        // Update UI based on information provided
        setTitle(mGroup.name);

        // Configure our Adapter for the RecyclerView
        mAdapter = new MessageAdapter(mMessages, this, null);

        // Configure our RecyclerView
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setStackFromEnd(true);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        // Listen for any new messages
        mDatabase.getReference()
                .child("messages").child(String.valueOf(mGroup.id))
                .addChildEventListener(mDatabaseReadMessageListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_leave: // Leave
                // Remove group from the current user
                mDatabase.getReference()
                        .child("users").child(mAuth.getCurrentUser().getUid()).child("groups")
                        .child(String.valueOf(mGroup.id)).removeValue()
                        .addOnCompleteListener(mDatabaseUserGroupRemovedListener);
                return true;

            default: // Default
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.btn_send)
    public void onSendMessage() {
        // Validate user input before continuing
        if (!validateInput()) {
            return;
        }

        // Save then clear message
        String message = mMessage.getText().toString();
        mMessage.setText(null);

        // Generate a new unique ID for the message
        String key = mDatabase.getReference()
                .child("messages").child(String.valueOf(mGroup.id))
                .push().getKey();

        // Create a new DatabaseMessage with the given information
        mLastMessage = new DatabaseMessage(
                key,
                mAuth.getCurrentUser().getDisplayName(),
                mAuth.getCurrentUser().getPhotoUrl().toString(),
                message,
                mGroup.id,
                System.currentTimeMillis());

        // Update the group information
        mGroup.last_message = mAuth.getCurrentUser().getDisplayName() + ": " + message;
        mGroup.timestamp = mLastMessage.timestamp;

        // Write Message to the database
        mDatabase.getReference()
                .child("messages").child(String.valueOf(mGroup.id)).child(key)
                .setValue(mLastMessage)
                .addOnCompleteListener(mDatabaseWriteMessageListener);
    }

    private boolean validateInput() {
        mMessage.setError(null);

        String message = mMessage.getText().toString();
        if (message.isEmpty()) {
            mMessage.setError(getString(R.string.error_input_message_empty));
            return false;
        } else if (message.length() > 512) {
            mMessage.setError(getString(R.string.error_input_message_length));
            return false;
        } else mMessage.setError(null);

        return true;
    }

    private class DatabaseReadMessageListener implements ChildEventListener {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            mMessages.add(dataSnapshot.getValue(DatabaseMessage.class));
            mAdapter.notifyDataSetChanged();
            mLayoutManager.scrollToPosition(mMessages.size() - 1);
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            // Messages are immutable.
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            // Messages are immutable and should not be removed.
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            // Do nothing.
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Log the error and notify the user
            Log.e(TAG, "DatabaseReadMessageListener", databaseError.toException());
            FirebaseCrash.report(databaseError.toException());
            showSnackbar(R.string.error_msg_messages);
        }
    }

    private class DatabaseUserGroupRemovedListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            if (task.isSuccessful()) {
                // Remove current user from group
                mDatabase.getReference()
                        .child("groups").child(String.valueOf(mGroup.id)).child("users")
                        .child(mAuth.getCurrentUser().getUid()).removeValue()
                        .addOnCompleteListener(mDatabaseGroupUserRemovedListener);
            } else {
                // Log the error and notify the user
                Log.e(TAG, "DatabaseUserGroupRemovedListener", task.getException());
                FirebaseCrash.report(task.getException());
                showSnackbar(R.string.error_msg_group_leave);
            }
        }
    }

    private class DatabaseGroupUserRemovedListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            if (task.isSuccessful()) {
                // Notify the user that they have left the group successfully
                // and return to the MainActivity
                showToast(R.string.msg_group_leave);
                finish();
            } else {
                // Log the error and notify the user
                Log.e(TAG, "DatabaseGroupUserRemovedListener", task.getException());
                FirebaseCrash.report(task.getException());
                showSnackbar(R.string.error_msg_group_leave);
            }
        }
    }

    private class DatabaseWriteMessageListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            if (task.isSuccessful()) {
                // Update the last message for the group
                mDatabase.getReference()
                        .child("groups").child(String.valueOf(mGroup.id))
                        .setValue(mGroup)
                        .addOnCompleteListener(mDatabaseUpdateGroupMessageListener);
            } else {
                // Log the error and notify the user
                Log.e(TAG, "DatabaseWriteMessageListener", task.getException());
                FirebaseCrash.report(task.getException());
                showSnackbar(R.string.error_msg_send);
            }
        }
    }

    private class DatabaseUpdateGroupMessageListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            if (!task.isSuccessful()) {
                // Log the error and notify the user
                Log.e(TAG, "DatabaseUpdateGroupMessageListener", task.getException());
                FirebaseCrash.report(task.getException());
                showSnackbar(R.string.error_msg_send);
            }
        }
    }
}
