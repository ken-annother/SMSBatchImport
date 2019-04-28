package com.nicekun.tools.inserttool;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.io.File;

public abstract class BaseSmsActivity extends AppCompatActivity {

    // 判断是否缺少权限
    protected boolean lacksPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED;
    }


    protected void settingDefaultSmsApp() {
        String defaultSmsApp = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this);//获取手机当前设置的默认短信应用的包名
        }
        String packageName = getPackageName();
        if (!defaultSmsApp.equals(packageName)) {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName);
            startActivity(intent);
        } else {
//            mTvDefaultAppTips.setText(getString(R.string.app_had_set_been_default));
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, "");
            startActivity(intent);
        }
    }

    protected boolean isNotDefaultSmsApp() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
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


    protected void scanFile(String filePath) {
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(new File(filePath)));
        sendBroadcast(scanIntent);
    }
}
