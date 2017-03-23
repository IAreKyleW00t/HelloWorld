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
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    // Activity elements
    @BindView(R.id.list_chats) RecyclerView mRecyclerView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;

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
        startActivity(JoinActivity.createIntent(this));
    }
}
