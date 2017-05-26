/**
 * Project:  FileLogger
 * Filename: LoggerManager.java
 *
 * Created by GuiSen Lin on 2017/5/26.
 * Copyright (c) 2017. Bearyinnovative. All rights reserved.
 */

package com.gershon.filetreelogger;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
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

public class LoggerManager {

    private static LoggerManager instance;
    private Context context;
    private ExecutorService executorService = null;
    private int availableProcessors = 1;
    private final int FILE_MAX_LENGTH = 10 * 1024 * 1024;

    private LoggerManager(Context context) {
        this.context = context;
        availableProcessors = Runtime.getRuntime().availableProcessors();
    }

    public synchronized static LoggerManager getInstance(Context context) {
        if (null == context) {
            return null;
        }

        if (null == instance) {
            instance = new LoggerManager(context);
        }
        return instance;
    }

    public void log(Throwable throwable) {
        executeTask(new LoggerTask(throwable));
    }

    public void log(int priority, String tag, String message) {
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

    private void logToFile(int priority, String tag, String message, Throwable throwable) {
        if (Utils.isSDCardAvailible()) {
            try {
                String systemInfo = Utils.LINE_SEPARATOR;
                File file = new File(Utils.getLogFileName(context));

                if (!file.exists() || file.isDirectory() || file.length() > FILE_MAX_LENGTH) {
                    Utils.delete(file);
                    scanLogFile();
                    file.createNewFile();
                    systemInfo = Utils.buildSystemInfo(context);
                }
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file, true)), true);
                if(null != throwable) {
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
    }

    private void scanLogFile() {
        try {
            File directory = Utils.getLogDir(context);
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
                    if (nowDay - logFileDay >= 7) {
                        Utils.delete(logFile);
                    } else {
                        break;
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private String formatLog(int priority, String tag, String message, String systemInfo) {
        String priorityLevel = getPriorityLevel(priority);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        systemInfo = TextUtils.equals(systemInfo, Utils.LINE_SEPARATOR) ? "" : systemInfo;
        return systemInfo + format.format(System.currentTimeMillis()) + " " + priorityLevel
                + Utils.DIAGONAL_BAR + tag + ": " + message;
    }

    private String getPriorityLevel(int priority) {
        switch (priority) {
            case Log.VERBOSE: return "V";
            case Log.DEBUG: return "D";
            case Log.INFO: return "I";
            case Log.WARN: return"W";
            case Log.ERROR: return "E";
            case Log.ASSERT: return "A";
            default: return "D";
        }
    }

    private String formatLog(Throwable throwable, String systemInfo) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        return systemInfo + "Date&Time: " + format.format(System.currentTimeMillis()) + Utils.LINE_SEPARATOR
                + "Message: " + Utils.LINE_SEPARATOR + throwable.getMessage() + Utils.LINE_SEPARATOR + "Throwable: ";
    }

    private class LoggerTask implements Runnable {

        private int priority;
        private String tag;
        private String message;
        private Throwable throwable;

        public LoggerTask(Throwable t) {
            throwable = t;
        }

        public LoggerTask(int priority, String tag, String message) {
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
