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
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

public class BaseActivity extends AppCompatActivity {
    private MaterialDialog _progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @MainThread
    public void showProgressDialog(@StringRes int title) {
        if (_progress == null) {
            _progress = new MaterialDialog.Builder(this)
                    .title(title)
                    .content(R.string.text_wait)
                    .progress(true, 0)
                    .build();
        }
        _progress.show();
    }

    @MainThread
    public void hideProgressDialog() {
        if (_progress != null && _progress.isShowing()) {
            _progress.dismiss();
        }
    }

    @MainThread
    public void showToast(@StringRes int strResource) {
        Context context = getApplicationContext();
        if (context != null) {
            Toast.makeText(context, getString(strResource), Toast.LENGTH_LONG).show();
        }
    }

    @MainThread
    public void showToast(@StringRes int strResource, Object... args) {
        Context context = getApplicationContext();
        if (context != null) {
            Toast.makeText(context, getString(strResource, args), Toast.LENGTH_LONG).show();
        }
    }

    @MainThread
    public void showSnackbar(@StringRes int strResource) {
        View view = getCurrentFocus();
        if (view != null) {
            Snackbar.make(view, getString(strResource), Snackbar.LENGTH_LONG).show();
        }
    }

    @MainThread
    public void showSnackbar(@StringRes int strResource, Object... args) {
        View view = getCurrentFocus();
        if (view != null) {
            Snackbar.make(view, getString(strResource, args), Snackbar.LENGTH_LONG).show();
        }
    }
}
