package com.gothwad.filemanager.misc;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.gothwad.filemanager.BuildConfig;
import com.gothwad.filemanager.model.RootInfo;

public class AnalyticsManager {
    private static Context sAppContext = null;

    private final static String TAG = LogUtils.makeLogTag(AnalyticsManager.class);

    public static String FILE_TYPE = "file_type";
    public static String FILE_COUNT = "file_count";
    public static String FILE_MOVE = "file_move";

    private static boolean canSend() {
        return sAppContext != null
                && !BuildConfig.DEBUG ;
    }

    public static synchronized void intialize(Context context) {

    }

    public static void setProperty(String propertyName, String propertyValue){

    }

    public static void logEvent(String eventName){

    }

    public static void logEvent(String eventName, Bundle params){

    }

    public static void logEvent(String eventName, RootInfo rootInfo, Bundle params){

    }

    public static void setCurrentScreen(Activity activity, String screenName){

    }
}
