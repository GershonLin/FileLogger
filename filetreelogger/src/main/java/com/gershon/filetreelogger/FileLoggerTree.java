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

    private static Context context;
    private static int logPoiority;
    private static int logRetentionDays;

    public static void init(Context appContext, int poiority, int retentionDays) {
        context = appContext;
        logPoiority = poiority;
        logRetentionDays = retentionDays;
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable throwable) {
        if(null == context) {
            return;
        }

        if (priority <= logPoiority) {
            return;
        }

        FileLogger.log(context, priority, tag, message);

        if (throwable != null) {
            if (priority == Log.ERROR) {
                FileLogger.logError(context, throwable);
            } else if (priority == Log.WARN) {
                FileLogger.logWarning(context, throwable);
            }
        }
    }
}
