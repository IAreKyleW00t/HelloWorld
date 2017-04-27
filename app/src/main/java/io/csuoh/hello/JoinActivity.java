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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.csuoh.hello.adapters.GroupJoinAdapter;
import io.csuoh.hello.listeners.OnRecyclerClickListener;
import io.csuoh.hello.models.DatabaseGroup;

public class JoinActivity extends BaseActivity {
    public static final String TAG = JoinActivity.class.getSimpleName();

    // Activity elements
    @BindView(R.id.list_groups) RecyclerView mRecyclerView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;

    // RecyclerView elements
    private List<DatabaseGroup> mGroups = new ArrayList<>();
    private GroupJoinAdapter mAdapter;

    // Listeners
    private GroupItemClickListener mGroupItemClickListener;
    private DatabaseReadGroupsListener mDatabaseReadGroupsListener;

    public static Intent createIntent(Context context) {
        return new Intent(context, JoinActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);
        setTitle(R.string.title_join);
        showHomeUpButton(true);
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
        mGroupItemClickListener = new GroupItemClickListener();
        mDatabaseReadGroupsListener = new DatabaseReadGroupsListener();

        // Configure our Adapter for the RecyclerView
        mAdapter = new GroupJoinAdapter(mGroups, mGroupItemClickListener);

        // Configure our RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        // Display progress dialog
        showProgressDialog(R.string.dialog_progress_loading_groups);

        // Attempt to read groups from the database
        mDatabase.getReference()
                .child("groups")
                .addListenerForSingleValueEvent(mDatabaseReadGroupsListener);
    }

    private class GroupItemClickListener implements OnRecyclerClickListener {
        @Override
        public void onClick(int position) {
            // Save selected group
            DatabaseGroup group = mGroups.get(position);

            // Add the group to the current user
            mDatabase.getReference()
                    .child("users").child(mAuth.getCurrentUser().getUid()).child("groups")
                    .child(String.valueOf(group.id)).setValue(true)
                    .addOnCompleteListener(new DatabaseUserAddGroupListener(group));
        }
    }

    private class DatabaseReadGroupsListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Read and parse each group from the database
            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                mGroups.add(snapshot.getValue(DatabaseGroup.class));
            }

            // Tell the Adapter new data was added
            mAdapter.notifyDataSetChanged();

            // Remove the progress dialog
            hideProgressDialog();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Log the error and notify the user
            Log.e(TAG, "DatabaseReadGroupsListener", databaseError.toException());
            FirebaseCrash.report(databaseError.toException());
            showSnackbar(R.string.error_msg_groups_load);

            // Remove the progress dialog
            hideProgressDialog();
        }
    }

    private class DatabaseUserAddGroupListener implements OnCompleteListener<Void> {
        private final DatabaseGroup mGroup;

        public DatabaseUserAddGroupListener(final DatabaseGroup group) {
            mGroup = group;
        }

        @Override
        public void onComplete(@NonNull Task<Void> task) {
            // Check if the group was added to the user successfully
            if (task.isSuccessful()) {
                // Add the user to the current group
                mDatabase.getReference()
                        .child("groups").child(String.valueOf(mGroup.id)).child("users")
                        .child(mAuth.getCurrentUser().getUid()).setValue(true)
                        .addOnCompleteListener(new DatabaseGroupAddUserListener(mGroup));
            } else {
                // Log the error and notify the user
                Log.e(TAG, "DatabaseUserGroupAddListener", task.getException());
                FirebaseCrash.report(task.getException());
                showSnackbar(R.string.error_msg_group_join);

                // Remove the progress dialog
                hideProgressDialog();
            }
        }
    }

    private class DatabaseGroupAddUserListener implements OnCompleteListener<Void> {
        private final DatabaseGroup mGroup;

        public DatabaseGroupAddUserListener(final DatabaseGroup group) {
            mGroup = group;
        }

        @Override
        public void onComplete(@NonNull Task<Void> task) {
            // Check if the user was added to the group successfully
            if (task.isSuccessful()) {
                // Make sure new user is added to the group
                mGroup.users.put(mAuth.getCurrentUser().getUid(), true);

                // Add the new Group into the Bundle of extra data
                Bundle extras = new Bundle();
                extras.putParcelable("group", Parcels.wrap(DatabaseGroup.class, mGroup));

                // Create a new Intent with Bundle data
                Intent intent = GroupActivity.createIntent(JoinActivity.this)
                        .putExtras(extras);

                // Start GroupActivity
                startActivity(intent);
                finish();
            } else {
                // Log the error and notify the user
                Log.e(TAG, "DatabaseGroupUserAddListener", task.getException());
                FirebaseCrash.report(task.getException());
                showSnackbar(R.string.error_msg_group_join);

                // Remove the progress dialog
                hideProgressDialog();
            }
        }
    }
}
