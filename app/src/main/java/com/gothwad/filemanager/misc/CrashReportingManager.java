package com.gothwad.filemanager.misc;

import android.content.Context;

import com.gothwad.filemanager.BuildConfig;

public class CrashReportingManager {

    public static void enable(Context context, boolean enable) {
    }

    public static void logException(Exception e) {
        logException(e, false);
    }

    public static void logException(Exception e, boolean log) {
        if(BuildConfig.DEBUG){
            e.printStackTrace();
        } else if(log) {
        }
    }

    public static void log(String s) {
    }

    public static void log(String tag, String s) {
    }
}
