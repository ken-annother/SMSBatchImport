package com.nicekun.tools.inserttool;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class SingleInsertActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText mSmsFrom;
    private EditText mSmsbody;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_insert);
        initView();
    }

    private void initView() {
        mSmsFrom = findViewById(R.id.sms_from);
        mSmsbody = findViewById(R.id.sms_body);
        findViewById(R.id.save).setOnClickListener(this);
        findViewById(R.id.back).setOnClickListener(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.save:
                saveSms(mSmsFrom.getText().toString(), mSmsbody.getText().toString());
                break;
        }
    }


    private void saveSms(String from, String body) {
        if (from == null || TextUtils.isEmpty(from)) {
            Toast.makeText(this, "please enter sms number", Toast.LENGTH_SHORT).show();
        }

        Uri url = Uri.parse("content://sms/");
        ContentValues values = new ContentValues();
        values.put("address", from);
        values.put("body", body);
        values.put("type", 1);
        values.put("date", System.currentTimeMillis());
        getContentResolver().insert(url, values);

        Toast.makeText(this, "success", Toast.LENGTH_SHORT).show();
    }
}
