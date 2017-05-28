/**
 * Project:  nagini
 * Filename: FileLogger.java
 *
 * Created by GuiSen Lin on 2017/5/26.
 * Copyright (c) 2017. Bearyinnovative. All rights reserved.
 */

package com.gershon.filetreelogger;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class FileLogger {

    private static FileLogger instance;
    private Context context;
    private ExecutorService executorService = null;
    private int availableProcessors = 1;
    private final int FILE_MAX_LENGTH = 10 * 1024 * 1024;
    private final String[] priorityLevel = new String[]{"V", "D", "I", "W", "E", "A"};

    private FileLogger(Context context) {
        this.context = context;
        availableProcessors = Runtime.getRuntime().availableProcessors();
    }

    synchronized static FileLogger getInstance(Context context) {
        if (null == context) {
            return null;
        }

        if (null == instance) {
            instance = new FileLogger(context);
        }
        return instance;
    }

    void log(Throwable throwable) {
        executeTask(new LoggerTask(throwable));
    }

    void log(int priority, String tag, String message) {
        executeTask(new LoggerTask(priority, tag, message));
    }

    private void executeTask(final Runnable runnable) {
        if (null == executorService || executorService.isShutdown()) {
            availableProcessors = Math.max(1, availableProcessors);
            executorService = Executors.newFixedThreadPool(availableProcessors,
                    new ThreadFactory() {
                        @Override
                        public Thread newThread(@NonNull Runnable r) {
                            Thread thread = new Thread(r);
                            thread.setPriority(Thread.NORM_PRIORITY);
                            return thread;
                        }
                    });
        }
        executorService.submit(runnable);
    }

    private void moveLogFile() {
        File memoryDir = Utils.getMemoryLogDir(context);
        try {
            if (memoryDir.exists() && memoryDir.isDirectory()) {
                for (File logFile : memoryDir.listFiles()) {
                    FileChannel inputChannel = null;
                    FileChannel outputChannel = null;
                    File destFile = new File(Utils.getSDCardLogDir(context), logFile.getName());
                    inputChannel = new FileInputStream(logFile).getChannel();
                    outputChannel = new FileOutputStream(destFile).getChannel();
                    outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
                    inputChannel.close();
                    outputChannel.close();
                    Utils.delete(logFile);
                }
            }
        } catch (IOException e) {
        }

    }

    void logToFile(int priority, String tag, String message, Throwable throwable) {
        if (Build.VERSION.SDK_INT >= 23) {
            boolean permission = PackageManager.PERMISSION_GRANTED
                    == ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (Utils.isSDCardAvailible() && permission) {
                moveLogFile();
                writeToSDCard(priority, tag, message, throwable);
            } else {
                writeToMemory(priority, tag, message, throwable);
            }
        } else {
            if (Utils.isSDCardAvailible()) {
                moveLogFile();
                writeToSDCard(priority, tag, message, throwable);
            } else {
                writeToMemory(priority, tag, message, throwable);
            }
        }
    }

    private void writeToMemory(int priority, String tag, String message, Throwable throwable) {
        try {
            String systemInfo = Utils.LINE_SEPARATOR;
            File file = new File(Utils.getLogFileName(context, false));
            if (!file.exists() || file.isDirectory()) {
                File dir = Utils.getMemoryLogDir(context);
                for (File logFile : dir.listFiles()) {
                    Utils.delete(logFile);
                }
                file.createNewFile();
                systemInfo = Utils.buildSystemInfo(context);
            }
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file, true)), true);
            if (null != throwable) {
                pw.println(formatLog(throwable, systemInfo));
                throwable.printStackTrace(pw);
            } else {
                pw.println(formatLog(priority, tag, message, systemInfo));
            }
            pw.flush();
            pw.close();
        } catch (Exception e) {
        }
    }

    private void writeToSDCard(int priority, String tag, String message, Throwable throwable) {
        try {
            String systemInfo = Utils.LINE_SEPARATOR;
            File file = new File(Utils.getLogFileName(context, true));
            //文件大于MFILE_MAX_LENGTH，删除重写
            if (!file.exists() || file.isDirectory() || file.length() > FILE_MAX_LENGTH) {
                Utils.delete(file);
                scanLogFile(); //创建新Log文件前删除七天以前文件
                file.createNewFile();
                systemInfo = Utils.buildSystemInfo(context);
            }
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file, true)), true);
            if (null != throwable) {
                pw.println(formatLog(throwable, systemInfo));
                throwable.printStackTrace(pw);
            } else {
                pw.println(formatLog(priority, tag, message, systemInfo));
            }
            pw.flush();
            pw.close();
        } catch (Exception e) {
        }
    }

    private void scanLogFile() {
        try {
            File directory = Utils.getSDCardLogDir(context);
            if (null != directory && directory.isDirectory()) {
                List<File> subFiles = Arrays.asList(directory.listFiles());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
                Collections.sort(subFiles, new Comparator<File>() {
                    @Override
                    public int compare(File file1, File file2) {
                        return file1.getName().compareTo(file2.getName());
                    }
                });
                for (File logFile : subFiles) {
                    Date logFileDate = dateFormat.parse(logFile.getName().substring(4, 14));
                    Calendar logFileCal = Calendar.getInstance();
                    logFileCal.setTime(logFileDate);
                    Calendar nowCal = Calendar.getInstance();
                    nowCal.setTime(new Date());
                    int logFileDay = logFileCal.get(Calendar.DAY_OF_YEAR);
                    int nowDay = nowCal.get(Calendar.DAY_OF_YEAR);
                    if (nowDay - logFileDay >= FileLoggerTree.getLogRetentionDays()) {
                        Utils.delete(logFile);
                    } else {
                        break;
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    //simple-format: 2017-05-27 11:08:02 D/MainActivity: activity onCreate()
    private String formatLog(int priority, String tag, String message, String systemInfo) {
        String priorityLevel = getPriorityLevel(priority);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        systemInfo = TextUtils.equals(systemInfo, Utils.LINE_SEPARATOR) ? "" : systemInfo;
        return systemInfo + format.format(System.currentTimeMillis()) + " " + priorityLevel
                + Utils.DIAGONAL_BAR + tag + ": " + message;
    }

    private String formatLog(Throwable throwable, String systemInfo) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        return systemInfo + "Date&Time: " + format.format(System.currentTimeMillis()) + Utils.LINE_SEPARATOR
                + "Message: " + Utils.LINE_SEPARATOR + throwable.getMessage() + Utils.LINE_SEPARATOR + "Throwable: ";
    }

    private String getPriorityLevel(int priority) {
        switch (priority) {
            case Log.VERBOSE:
                return priorityLevel[0];
            case Log.DEBUG:
                return priorityLevel[1];
            case Log.INFO:
                return priorityLevel[2];
            case Log.WARN:
                return priorityLevel[3];
            case Log.ERROR:
                return priorityLevel[4];
            case Log.ASSERT:
                return priorityLevel[5];
            default:
                return priorityLevel[1];
        }
    }

    private class LoggerTask implements Runnable {

        private int priority;
        private String tag;
        private String message;
        private Throwable throwable;

        LoggerTask(Throwable t) {
            throwable = t;
        }

        LoggerTask(int priority, String tag, String message) {
            this.priority = priority;
            this.tag = tag;
            this.message = message;
        }

        @Override
        public void run() {
            logToFile(priority, tag, message, throwable);
        }
    }
}