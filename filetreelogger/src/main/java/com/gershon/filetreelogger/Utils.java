/**
 * Project:  FileLogger
 * Filename: Utils.java
 *
 * Created by GuiSen Lin on 2017/5/26.
 * Copyright (c) 2017. Bearyinnovative. All rights reserved.
 */
package com.gershon.filetreelogger;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Utils {

    private static String logFileName = null;
    private static final String LOG_DIRECTORY_NAME = "log";
    static final String DIAGONAL_BAR = "/";
    static final String LINE_SEPARATOR  = TextUtils.isEmpty(System.getProperty("line.separator")) ? "\n" : System.getProperty("line.separator");

    public static boolean isSDCardAvailible() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private static String getSystemVersion() {
        return Build.VERSION.RELEASE;
    }

    private static String getVersionName(Context context) {
        try {
            PackageInfo pinfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(),
                            PackageManager.GET_CONFIGURATIONS);
            return pinfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return "";
    }

    private static int getVersionCode(Context context) {
        try {
            PackageInfo pinfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(),
                            PackageManager.GET_CONFIGURATIONS);
            return pinfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return 1;
    }

    private static String getVendor() {
        return Build.MANUFACTURER;
    }

    private static String getModel() {
        return Build.MODEL;
    }

    public static String buildSystemInfo(Context context) {
        return "Vendor: " + getVendor() + LINE_SEPARATOR
                + "Model: " + getModel() + LINE_SEPARATOR
                + "system-version: " + getSystemVersion() + LINE_SEPARATOR
                + "version-name: " + getVersionName(context) + LINE_SEPARATOR
                + "version-code: " + getVersionCode(context) + LINE_SEPARATOR;

    }

    //simple-filename: log_2017-05-27.txt
    public static String getLogFileName(Context context, boolean toSDCard) {
        if (TextUtils.isEmpty(logFileName)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
            File logFile = toSDCard ? new File(getSDCardLogDir(context), "log_" + dateFormat.format(System.currentTimeMillis()) + ".txt")
                    : new File(getMemoryLogDir(context), "log_" + dateFormat.format(System.currentTimeMillis()) + ".txt");
            logFileName = logFile.getAbsolutePath();
        }
        return logFileName;
    }

    private static String getAppName(Context context) {
        String appName = "Nagini";
        try {
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
            String packageName = pinfo.packageName;
            if (!TextUtils.isEmpty(packageName)) {
                appName = packageName;
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        return appName;
    }

    public static File getSDCardLogDir(Context context) {
        if (!isSDCardAvailible()) {
            return null;
        }
        File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/" + getAppName(context));
        File sub = new File(dir, LOG_DIRECTORY_NAME);
        sub.mkdirs();
        return sub;
    }

    public static File getMemoryLogDir(Context context) {
        File dir = new File(context.getFilesDir().getPath());
        File sub = new File(dir, LOG_DIRECTORY_NAME);
        sub.mkdirs();
        return sub;
    }

    //递归删除 文件/目录
    public static boolean delete(File file) {
        if (file == null) {
            return false;
        }
        if (file.isFile()) {
            return file.delete();
        }
        File[] files = file.listFiles();
        if (files == null) {
            return false;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                delete(f);
            } else {
                return f.delete();
            }
        }
        return file.delete();
    }
}