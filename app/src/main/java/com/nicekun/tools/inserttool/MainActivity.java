package com.nicekun.tools.inserttool;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.leon.lfilepickerlibrary.LFilePicker;
import com.leon.lfilepickerlibrary.utils.Constant;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.nicekun.tools.inserttool.SmsDataParser.TYPE_CSV;
import static com.nicekun.tools.inserttool.SmsDataParser.TYPE_XML;

public class MainActivity extends BaseSmsActivity implements View.OnClickListener {

    private TextView mTvDefaultAppTips;
    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ContentResolver contentResolver = getApplication().getContentResolver();
        SmsDataParser.setContentResolver(contentResolver);
        initView();
    }

    private void initView() {
        findViewById(R.id.setting_default_sms_app).setOnClickListener(this);
        findViewById(R.id.import_inner_data).setOnClickListener(this);
        findViewById(R.id.import_from_outer).setOnClickListener(this);
        findViewById(R.id.insert_single_sms).setOnClickListener(this);
        findViewById(R.id.backup_system_sms).setOnClickListener(this);
        mTvDefaultAppTips = findViewById(R.id.default_tips);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isNotDefaultSmsApp()) {
            mTvDefaultAppTips.setText(getString(R.string.app_already_default));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.setting_default_sms_app:
                settingDefaultSmsApp();
                break;
            case R.id.import_inner_data:
                if (isNotDefaultSmsApp()) {
                    return;
                }
//                checkSmsWritePermission();
                mExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        importDataFromAssets();
                    }
                });


                break;
            case R.id.import_from_outer:
                if (isNotDefaultSmsApp()) {
                    return;
                }
                if (checkSDCardWritePermission()) {
                    mExecutorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            importDataFromSDCard();
                        }
                    });

                }
                break;

            case R.id.insert_single_sms:
                startActivity(new Intent(this, SingleActivity.class));
                break;

            case R.id.backup_system_sms:
                startActivity(new Intent(this,BackupActivity.class));
                break;
        }
    }

    private static final int OPEN_SMS_DATA_REQUEST_CODE = 1020;

    private static final int PERMISSION_WRITE_SDCARD_REQUEST_CODE = 810;

    @SuppressLint("ResourceType")
    private void importDataFromSDCard() {
        new LFilePicker()
                .withActivity(MainActivity.this)
                .withBackgroundColor(getResources().getString(R.color.colorPrimary))
                .withRequestCode(OPEN_SMS_DATA_REQUEST_CODE)
                .withIconStyle(Constant.ICON_STYLE_GREEN)
                .withTitle(getResources().getString(R.string.sms_file_select_title))
                .withFileFilter(new String[]{".txt", ".xml", ".csv"})
                .withMutilyMode(false)
                .start();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_SMS_DATA_REQUEST_CODE) {
            if (data != null) {
                List<String> files = data.getStringArrayListExtra(com.leon.lfilepickerlibrary.utils.Constant.RESULT_INFO);
                if (files == null || files.size() == 0) {
                    return;
                }
                String file = files.get(0);

                if (!TextUtils.isEmpty(file)) {
                    int fileType;
                    if (file.endsWith(".xml")) {
                        fileType = TYPE_XML;
                    } else if (file.endsWith(".txt") || file.endsWith(".csv")) {
                        fileType = TYPE_CSV;
                    } else {
                        fileType = TYPE_CSV;
                    }

                    try {
                        insertSMSData(new FileInputStream(file), fileType);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, "take back the  file path error,please try again!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    private void importDataFromAssets() {
        try {
            InputStream innerData = getAssets().open("innersms.txt");
            insertSMSData(innerData, TYPE_CSV);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void insertSMSData(InputStream innerData, int type) {
        try {
            SmsDataParser.parse(innerData, type, new onParseFinishListener() {

                @Override
                public void onFinished(final int smsNumer) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "insert sms Data finshed !", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    interface onParseFinishListener {
        void onFinished(int smsNumer);
    }

    private boolean checkSDCardWritePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {  // 6.0以下不用检查
            return true;
        }

        if (lacksPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_WRITE_SDCARD_REQUEST_CODE);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_WRITE_SDCARD_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                importDataFromSDCard();
            } else {
                Toast.makeText(this, "request read sdcard permission failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }




}
