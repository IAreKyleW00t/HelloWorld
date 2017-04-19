package io.csuoh.hello;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.FirebaseDatabase;

import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.csuoh.hello.models.DatabaseGroup;

public class GroupActivity extends BaseActivity {
    public static final String TAG = GroupActivity.class.getSimpleName();

    // Activity elements
    @BindView(R.id.list_messages) RecyclerView mMessages;
    @BindView(R.id.input_message) EditText mMessage;
    @BindView(R.id.btn_send) FloatingActionButton mSend;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;

    // Group
    private DatabaseGroup mGroup;

    // Listeners
    private DatabaseUserGroupRemovedListener mDatabaseUserGroupRemovedListener;

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

        // Initialize all of our Listeners
        mDatabaseUserGroupRemovedListener = new DatabaseUserGroupRemovedListener();

        // Check if the current user is logged in and verified
        if (mAuth.getCurrentUser() == null || !mAuth.getCurrentUser().isEmailVerified()) {
            startActivity(LoginActivity.createIntent(this));
            finish();

            // Notify the user that they were logged out
            showToast(R.string.msg_reauthenticate);
        }

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

    private class DatabaseUserGroupRemovedListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
            if (task.isSuccessful()) {
                // Remove current user from group
                mDatabase.getReference()
                        .child("groups").child(String.valueOf(mGroup.id)).child("users")
                        .child(mAuth.getCurrentUser().getUid()).removeValue()
                        .addOnCompleteListener(new DatabaseGroupUserRemovedListener());
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
}
