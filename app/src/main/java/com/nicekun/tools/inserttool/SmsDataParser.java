package com.nicekun.tools.inserttool;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsDataParser {
    private static int SMS_COUNT = 0;

    public static final int TYPE_CSV = 1;
    public static final int TYPE_XML = 2;

    private static Pattern sPattern = Pattern.compile("<string\\s+name=\\\"(.*)\\\"><!\\[CDATA\\[(.*)\\]\\]></string>");

    private static ExecutorService sExecutorService = Executors.newFixedThreadPool(3);

    private static LinkedList<JSONObject> smsInfo = new LinkedList<>();

    public static void parse(InputStream inputStream, int type, MainActivity.onParseFinishListener listener) throws IOException {
        smsInfo.clear();
        SMS_COUNT = 0;

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            Matcher matcher = sPattern.matcher(line);
            if (matcher.matches()) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("smsNumber", matcher.group(1));
                    jsonObject.put("smsBody", matcher.group(2));
                    smsInfo.add(jsonObject);
                    notifyAdd();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if (line.indexOf("\t") > 0) {
                String[] split = line.split("\t", 2);
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("smsNumber", split[0]);
                    jsonObject.put("smsBody", split[1]);
                    smsInfo.add(jsonObject);
                    notifyAdd();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        notifyFinish(listener);
    }

    private static void notifyFinish(MainActivity.onParseFinishListener listener) {
        insertSms();
        listener.onFinished(SMS_COUNT);
    }

    private static void notifyAdd() {
        if (smsInfo.size() > 100) {
            insertSms();
        }
    }

    private static void insertSms() {
        if (sContentResolver == null) {
            return;
        }

        final LinkedList<JSONObject> datas;
        synchronized (smsInfo) {
            datas = new LinkedList<>(smsInfo);
            smsInfo.clear();
        }

        sExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                Uri url = Uri.parse("content://sms/");
                ContentValues[] valueList = new ContentValues[datas.size()];
                for (int i = 0; i < datas.size(); i++) {
                    JSONObject jsonObject = datas.get(i);
                    ContentValues values = new ContentValues();
                    try {
                        values.put("address", jsonObject.getString("smsNumber"));
                        values.put("body", jsonObject.getString("smsBody"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    values.put("type", 1);
                    values.put("date", System.currentTimeMillis());
                    valueList[i] = values;
                }

                SMS_COUNT += valueList.length;
                sContentResolver.bulkInsert(url, valueList);
            }
        });

    }

    private static ContentResolver sContentResolver;

    public static void setContentResolver(ContentResolver contentResolver) {
        sContentResolver = contentResolver;
    }
}
