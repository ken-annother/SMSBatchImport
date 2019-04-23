package com.nicekun.tools.inserttool;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.nicekun.tools.inserttool.SmsDataParser.TYPE_CSV;
import static com.nicekun.tools.inserttool.SmsDataParser.TYPE_XML;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

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
        mTvDefaultAppTips = findViewById(R.id.default_tips);
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
        }
    }

    private static final int OPEN_SMS_DATA_REQUEST_CODE = 1020;

    private static final int PERMISSION_WRITE_SDCARD_REQUEST_CODE = 810;

    private void importDataFromSDCard() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(intent, OPEN_SMS_DATA_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "oh my god, not fount file explore in this devices -_-!!", Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_SMS_DATA_REQUEST_CODE) {
            if (data != null) {
                Uri uri = data.getData();//得到uri，后面就是将uri转化成file的过程。
                if (uri != null) {
                    String file = uri.getPath();
                    if (!TextUtils.isEmpty(file)) {
                        int fileType;
                        if (file.endsWith(".xml")) {
                            fileType = TYPE_XML;
                        } else if (file.endsWith(".txt")|| file.endsWith(".csv")) {
                            fileType = TYPE_CSV;
                        } else {
                            fileType = TYPE_CSV;
                        }

                        try {
                            insertSMSData(new FileInputStream(file), fileType);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                Toast.makeText(this, "take back the  file path error,please try again!", Toast.LENGTH_SHORT).show();
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


    /**
     * 检查写短信的权限
     */
    private void checkSmsWritePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {  // 6.0以下不用检查
            return;
        }

        if (lacksPermission(Manifest.permission.READ_SMS)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, 321);
        }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_WRITE_SDCARD_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                importDataFromSDCard();
            } else {
                Toast.makeText(this, "request read sdcard permission failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 判断是否缺少权限
    private boolean lacksPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED;
    }


    private void settingDefaultSmsApp() {
        String defaultSmsApp = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this);//获取手机当前设置的默认短信应用的包名
        }
        String packageName = getPackageName();
        if (!defaultSmsApp.equals(packageName)) {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName);
            startActivity(intent);
        } else {
            mTvDefaultAppTips.setText(getString(R.string.app_had_set_been_default));
        }
    }

    private boolean isNotDefaultSmsApp() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            return false;
        }

        String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this);//获取手机当前设置的默认短信应用的包名

        String packageName = getPackageName();
        if (!defaultSmsApp.equals(packageName)) {
            Toast.makeText(this, "The app is not the default sms . please to set it first!", Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }


}
