/**
 * Project:  FileLogger
 * Filename: FileLoggerTree.java
 *
 * Created by GuiSen Lin on 2017/5/26.
 * Copyright (c) 2017. Bearyinnovative. All rights reserved.
 */
package com.gershon.filetreelogger;

import android.content.Context;
import android.util.Log;

import timber.log.Timber;

public class FileLoggerTree extends Timber.Tree {

    private Context context;
    private int logPriority;
    private static int logRetentionDays = 7;

    public FileLoggerTree(Context appContext, int priority, int retentionDays) {
        context = appContext;
        logPriority = priority;
        logRetentionDays = retentionDays;
    }

    public static int getLogRetentionDays() {
        return logRetentionDays;
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable throwable) {
        if(null == context) {
            return;
        }

        if (priority < logPriority) {
            return;
        }

        LoggerManager.log(context, priority, tag, message);

        if (throwable != null) {
            if (priority == Log.ERROR) {
                LoggerManager.logError(context, throwable);
            } else if (priority == Log.WARN) {
                LoggerManager.logWarning(context, throwable);
            }
        }
    }
}