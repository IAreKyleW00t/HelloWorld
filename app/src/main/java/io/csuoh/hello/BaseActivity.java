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

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

public class BaseActivity extends AppCompatActivity {
    public static final int NEW_MESSAGE_NOTIFICATION_ID = 470;
    private MaterialDialog mProgressDialog;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Automatically clear any app-related notifications
        notificationManager.cancel(NEW_MESSAGE_NOTIFICATION_ID);
    }

    @MainThread
    public final boolean isAppInForeground(@NonNull Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> services = activityManager.getRunningTasks(Integer.MAX_VALUE);
        if (services.get(0).topActivity.getPackageName().equalsIgnoreCase(context.getPackageName())) {
            return true;
        }

        return false;
    }

    @MainThread
    public final void showProgressDialog(@StringRes int title) {
        if (mProgressDialog == null) {
            mProgressDialog = new MaterialDialog.Builder(this)
                    .title(title)
                    .content(R.string.please_wait)
                    .progress(true, 0)
                    .cancelable(false)
                    .build();
        } else if (!mProgressDialog.isShowing()) {
            mProgressDialog.setTitle(title);
        }
        mProgressDialog.show();
    }

    @MainThread
    public final void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @MainThread
    public final void updateProgressDialog(@StringRes int title) {
        if (mProgressDialog != null) {
            mProgressDialog.setTitle(title);
        }
    }

    @MainThread
    public final void showHomeUpButton(boolean enabled) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(enabled);
        }
    }

    @MainThread
    public final void showToast(@StringRes int strResource) {
        Toast.makeText(this, strResource, Toast.LENGTH_LONG).show();
    }

    @MainThread
    public final void showToast(@StringRes int strResource, Object... args) {
        Toast.makeText(this, getString(strResource, args), Toast.LENGTH_LONG).show();
    }

    @MainThread
    public final void showSnackbar(@StringRes int strResource) {
        View view = getCurrentFocus();
        if (view != null) {
            Snackbar.make(getCurrentFocus(), strResource, Snackbar.LENGTH_LONG).show();
        }
    }

    @MainThread
    public final void showSnackbar(@StringRes int strResource, Object... args) {
        View view = getCurrentFocus();
        if (view != null) {
            Snackbar.make(getCurrentFocus(), getString(strResource, args), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
