package com.nicekun.tools.inserttool;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackupActivity extends BaseSmsActivity implements View.OnClickListener {

    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat sdfTime = new SimpleDateFormat("HHmmss", Locale.getDefault());
    private ProgressDialog mProgressDialog;
    private TextView mTips;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);
        initView();
    }

    private void initView() {
        mProgressDialog = new ProgressDialog(this);
        findViewById(R.id.backupnow).setOnClickListener(this);
        mTips = findViewById(R.id.tips);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.backupnow) {
            backup();
        }
    }

    private void backup() {
        ArrayList<String> lackPermissions = new ArrayList<>();
        if (lacksPermission(Manifest.permission.READ_SMS)) {
            lackPermissions.add(Manifest.permission.READ_SMS);
        }

        if (lacksPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            lackPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (lackPermissions.size() > 0) {
            ActivityCompat.requestPermissions(this, lackPermissions.toArray(new String[]{}), REQUEST_BACKUP_SMS);
            return;
        }

        mProgressDialog.show();
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                handleBackupSMS();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BACKUP_SMS) {
            StringBuilder tipInfo = new StringBuilder();
            if (lacksPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                tipInfo.append("write sdcard ");
                Toast.makeText(this, "request write sdcard permission failed.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (lacksPermission(Manifest.permission.READ_SMS)) {
                if (tipInfo.length() > 0) {
                    tipInfo.append("and ");
                }
                tipInfo.append("read sms ");

                return;
            }

            if (tipInfo.length() > 0) {
                tipInfo.insert(0, "Request ");
                if (tipInfo.indexOf("and") != -1) {
                    tipInfo.append(" permissions failed");
                } else {
                    tipInfo.append(" permission failed");
                }

                Toast.makeText(this, tipInfo, Toast.LENGTH_SHORT).show();
            }

            mProgressDialog.show();
            mExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    handleBackupSMS();
                }
            });
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    private void handleBackupSMS() {
        Date now = new Date();
        final File smsBackupfile = new File(Environment.getExternalStorageDirectory(), "NKSmsBackup/" + sdfDate.format(now) + "/smsbackup-" + sdfTime.format(now) + ".xml");
        if (!smsBackupfile.exists()) {
            smsBackupfile.getParentFile().mkdirs();
        }

        ContentResolver resolver = getContentResolver();
        Uri uri = Uri.parse("content://sms/");
        Cursor cursor = resolver.query(uri, new String[]{"address", "body"}, "type=?", new String[]{"1"}, null);
        if (cursor == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.hide();
                }
            });
            return;
        }

        BufferedWriter smsBackup;
        try {
            smsBackup = new BufferedWriter(new FileWriter(smsBackupfile));

            while (cursor.moveToNext()) {
                String address = cursor.getString(cursor.getColumnIndex("address"));
                String body = cursor.getString(cursor.getColumnIndex("body"));
                smsBackup.write(String.format("<string name=\"%s\"><![CDATA[%s]]></string>", address, body));
                smsBackup.write("\n");
            }

            smsBackup.flush();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(BackupActivity.this, "success", Toast.LENGTH_SHORT).show();
                    try {
                        mTips.setText(getString(R.string.save_path_tip, smsBackupfile.getCanonicalPath()));
                    } catch (IOException e) {
                    }
                }
            });

            scanFile(smsBackupfile.getCanonicalPath());
        } catch (final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(BackupActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } finally {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.hide();
                }
            });
            cursor.close();
        }

    }


    private static final int REQUEST_BACKUP_SMS = 321;

}
