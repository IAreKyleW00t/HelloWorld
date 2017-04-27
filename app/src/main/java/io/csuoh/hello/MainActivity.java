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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.csuoh.hello.adapters.GroupAdapter;
import io.csuoh.hello.listeners.OnRecyclerClickListener;
import io.csuoh.hello.models.DatabaseGroup;

public class MainActivity extends BaseActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String PREFS_NAME = "HelloWorld";

    // Activity elements
    @BindView(R.id.list_active_groups) RecyclerView mRecyclerView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;

    // RecyclerView elements
    private List<DatabaseGroup> mGroups = new ArrayList<DatabaseGroup>() {
        @Override
        public boolean contains(Object o) {
            if (o instanceof DatabaseGroup) {
                DatabaseGroup group = (DatabaseGroup) o;

                for (DatabaseGroup g : this) {
                    if (g.id == group.id) {
                        return true;
                    }
                }
            }
            return false;
        }
    };
    private GroupAdapter mAdapter;

    // Listeners
    private GroupItemClickListener mGroupItemClickListener;
    private DatabaseUserGroupsListener mDatabaseUserGroupsListener;
    private GroupItemUpdatedListener mGroupItemUpdatedListener;

    public static Intent createIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.title_main);
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
        mDatabaseUserGroupsListener = new DatabaseUserGroupsListener();
        mGroupItemUpdatedListener = new GroupItemUpdatedListener();

        // Configure our Adapter for the RecyclerView
        mAdapter = new GroupAdapter(mGroups, mGroupItemClickListener);

        // Configure our RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        // Listen for any changes to the groups the current user is in
        mDatabase.getReference()
                .child("users").child(mAuth.getCurrentUser().getUid()).child("groups")
                .addChildEventListener(mDatabaseUserGroupsListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings: // Settings
                startActivity(SettingsActivity.createIntent(MainActivity.this));
                return true;

            case R.id.menu_signout: // Logout
                // Sign the user out of their current Firebase session and move them back to the login Activity
                mAuth.signOut();
                startActivity(LoginActivity.createIntent(this));
                finish();

                // Notify the user that they were logged out successfully
                showToast(R.string.msg_signout);
                return true;

            default: // Default
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.fab_group_add)
    public void onJoinGroup() {
        // Start the Join Activity
        startActivity(JoinActivity.createIntent(this));
    }

    private class GroupItemClickListener implements OnRecyclerClickListener {
        @Override
        public void onClick(int position) {
            // Save selected Group
            DatabaseGroup group = mGroups.get(position);

            // Add the Group into the Bundle data
            Bundle extras = new Bundle();
            extras.putParcelable("group", Parcels.wrap(DatabaseGroup.class, group));

            // Create a new Intent with the extra Bundle data
            Intent intent = GroupActivity.createIntent(MainActivity.this)
                    .putExtras(extras);

            // Start the GroupActivity with the specified Group
            startActivity(intent);
        }
    }

    private class DatabaseUserGroupsListener implements ChildEventListener {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            mDatabase.getReference()
                    .child("groups").child(dataSnapshot.getKey())
                    .addValueEventListener(mGroupItemUpdatedListener);
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            int groupId = Integer.parseInt(dataSnapshot.getKey());

            int i = 0;
            for (DatabaseGroup group : mGroups) {
                if (group.id == groupId) {
                    // Remove Group from list and Adapter
                    mGroups.remove(i);
                    mAdapter.notifyItemRemoved(i);
                    break;
                }
                i++;
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            // Add or remove the Group based on the value (true/false)
            if (dataSnapshot.getValue(Boolean.class)) {
                onChildAdded(dataSnapshot, s);
            } else {
                onChildRemoved(dataSnapshot);
            }
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            // Nothing to do.
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Log the error and notify the user
            Log.e(TAG, "DatabaseUserGroupsListener", databaseError.toException());
            FirebaseCrash.report(databaseError.toException());
            showSnackbar(R.string.error_msg_groups_load);
        }
    }

    private class GroupItemUpdatedListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Save the Group and current user ID
            DatabaseGroup group = dataSnapshot.getValue(DatabaseGroup.class);
            String userId = mAuth.getCurrentUser().getUid();

            if (mGroups.contains(group) && group.users.containsKey(userId)) { // Update group
                int i = 0;
                for (DatabaseGroup g : mGroups) {
                    if (g.id == group.id) {
                        mGroups.set(i, group);
                        break;
                    }
                    i++;
                }

                // Send notification if a group is updated, aka: new message.
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                if (!isAppInForeground(MainActivity.this)) {
                    // Add the Group into the Bundle data
                    Bundle extras = new Bundle();
                    extras.putParcelable("group", Parcels.wrap(DatabaseGroup.class, group));

                    // Create a new Intent with the extra Bundle data
                    Intent intent = GroupActivity.createIntent(MainActivity.this)
                            .putExtras(extras);

                    // Create pending Intent
                    PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, (int) System.currentTimeMillis(), intent, 0);

                    Uri notificationSound = null;
                    if (settings.getBoolean("notifications", true)) {
                        notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    }

                    // Create notification
                    Notification notification = new Notification.Builder(MainActivity.this)
                            .setContentTitle("New Message")
                            .setContentText(group.last_message)
                            .setSmallIcon(R.drawable.ic_smile)
                            .setContentIntent(pendingIntent)
                            .setSound(notificationSound)
                            .setAutoCancel(true)
                            .build();

                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.notify(NEW_MESSAGE_NOTIFICATION_ID, notification);
                }

            } else if (!mGroups.contains(group) && group.users.containsKey(userId)) { // Add group
                mGroups.add(group);
            }

            // Notify the Adapter that data was updated
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Log the error and notify the user
            Log.e(TAG, "GroupItemUpdatedListener", databaseError.toException());
            FirebaseCrash.report(databaseError.toException());
            showSnackbar(R.string.error_msg_groups_load);
        }
    }
}
