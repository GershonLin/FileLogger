/**
 * Project:  nagini
 * Filename: LoggerManager.java
 *
 * Created by GuiSen Lin on 2017/5/26.
 * Copyright (c) 2017. Bearyinnovative. All rights reserved.
 */
package com.gershon.filetreelogger;

import android.content.Context;

public final class LoggerManager {

    static void log(Context context, int priority, String tag, String message) {
        FileLogger.getInstance(context).log(priority, tag, message);
    }

    static void logWarning(Context context, Throwable t) {
        FileLogger.getInstance(context).log(t);
    }

    static void logError(Context context, Throwable t) {
        FileLogger.getInstance(context).log(t);
    }

    private LoggerManager() {
        throw new AssertionError("No instances.");
    }
}