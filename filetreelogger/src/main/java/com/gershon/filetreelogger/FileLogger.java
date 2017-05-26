/**
 * Project:  FileLogger
 * Filename: FileLogger.java
 *
 * Created by GuiSen Lin on 2017/5/26.
 * Copyright (c) 2017. Bearyinnovative. All rights reserved.
 */
package com.gershon.filetreelogger;

import android.content.Context;

public final class FileLogger {

    public static void log(Context context, int priority, String tag, String message) {
        LoggerManager.getInstance(context).log(priority, tag, message);
    }

    public static void logWarning(Context context, Throwable t) {
        LoggerManager.getInstance(context).log(t);
    }

    public static void logError(Context context, Throwable t) {
        LoggerManager.getInstance(context).log(t);
    }

    private FileLogger() {
        throw new AssertionError("No instances.");
    }
}
