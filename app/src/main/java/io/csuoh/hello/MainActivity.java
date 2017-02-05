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
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import butterknife.ButterKnife;

public class MainActivity extends BaseActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    // Menu items
    private static final int
            MENU_SETTINGS   = Menu.FIRST,
            MENU_SIGNOUT    = Menu.FIRST + 1;

    public static Intent createIntent(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, MainActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.title_main);
        ButterKnife.bind(this);

        // Check if user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !user.isEmailVerified()) {
            startActivity(LoginActivity.createIntent(this));
            showToast(R.string.msg_reauthenticate);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_SETTINGS, Menu.NONE, R.string.menu_settings);
        menu.add(0, MENU_SIGNOUT, Menu.NONE, R.string.menu_logout);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SETTINGS: // Open settings
                startActivity(SettingsActivity.createIntent(MainActivity.this));
                return true;
            case MENU_SIGNOUT: // Logout
                FirebaseAuth.getInstance().signOut();
                startActivity(LoginActivity.createIntent(this));
                showToast(R.string.msg_logout);
                finish();
                return true;
            default: // Default to super
                return super.onOptionsItemSelected(item);
        }
    }
}
