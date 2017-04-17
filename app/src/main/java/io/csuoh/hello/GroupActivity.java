package io.csuoh.hello;

import android.os.Bundle;

import butterknife.ButterKnife;

public class GroupActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);
        ButterKnife.bind(this);
    }
}
