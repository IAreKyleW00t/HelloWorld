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
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.csuoh.hello.adapters.GroupJoinAdapter;
import io.csuoh.hello.models.DatabaseGroup;

public class JoinActivity extends BaseActivity {
    public static final String TAG = JoinActivity.class.getSimpleName();

    // Activity elements
    @BindView(R.id.list_groups) RecyclerView mRecyclerView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;

    // Groups
    private List<DatabaseGroup> mGroups = new ArrayList<>();

    // RecyclerView
    private GroupJoinAdapter mAdapter;

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

        // Display progress dialog
        showProgressDialog(R.string.dialog_progress_loading_groups);

        // Configure our Adapter for the RecyclerView
        mAdapter = new GroupJoinAdapter(mGroups);

        // Configure our RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        // Attempt to read groups from the database
        mDatabase.getReference()
                .child("groups")
                .addListenerForSingleValueEvent(new ValueEventListener() {
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
                        Log.e(TAG, "SingleValueEvent", databaseError.toException());
                        FirebaseCrash.report(databaseError.toException());
                        showSnackbar(R.string.error_msg_groups_load);

                        // Remove the progress dialog
                        hideProgressDialog();
                    }
                });
    }
}
