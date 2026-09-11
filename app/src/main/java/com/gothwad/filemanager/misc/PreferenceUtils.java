package com.gothwad.filemanager.misc;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import com.gothwad.filemanager.DocumentsApplication;

public class PreferenceUtils {

    private static Context getContext() {
        return DocumentsApplication.getInstance();
    }

    private static SharedPreferences getPrefs(Context context) {
        if (context == null) {
            context = getContext();
        }
        if (context != null) {
            try {
                return PreferenceManager.getDefaultSharedPreferences(context);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static Boolean getBooleanPrefs(Context ctx, String key) {
        SharedPreferences p = getPrefs(ctx);
        return p != null && p.getBoolean(key, false);
    }

    public static Boolean getBooleanPrefs(Context ctx, String key, boolean defaultValue) {
        SharedPreferences p = getPrefs(ctx);
        return p != null ? p.getBoolean(key, defaultValue) : defaultValue;
    }

    public static Boolean getBooleanPrefs(String key) {
        return getBooleanPrefs(null, key, false);
    }

    public static Boolean getBooleanPrefs(String key, boolean defaultValue) {
        return getBooleanPrefs(null, key, defaultValue);
    }

    public static String getStringPrefs(String key) {
        return getStringPrefs(null, key, "");
    }

    public static String getStringPrefs(String key, String defaultValue) {
        return getStringPrefs(null, key, defaultValue);
    }

    public static String getStringPrefs(Context ctx, String key) {
        return getStringPrefs(ctx, key, "");
    }

    public static String getStringPrefs(Context ctx, String key, String defaultValue) {
        SharedPreferences p = getPrefs(ctx);
        return p != null ? p.getString(key, defaultValue) : defaultValue;
    }

    public static int getIntegerPrefs(Context ctx, String key) {
        return getIntegerPrefs(ctx, key, 0);
    }

    public static int getIntegerPrefs(String key) {
        return getIntegerPrefs(null, key, 0);
    }

    public static int getIntegerPrefs(String key, int defaultValue) {
        return getIntegerPrefs(null, key, defaultValue);
    }

    public static int getIntegerPrefs(Context ctx, String key, int defaultValue) {
        SharedPreferences p = getPrefs(ctx);
        return p != null ? p.getInt(key, defaultValue) : defaultValue;
    }

    public static long getLongPrefs(Context ctx, String key) {
        return getLongPrefs(ctx, key, 0L);
    }

    public static long getLongPrefs(Context ctx, String key, int defaultValue) {
        return getLongPrefs(ctx, key, (long) defaultValue);
    }

    public static long getLongPrefs(Context ctx, String key, long defaultValue) {
        SharedPreferences p = getPrefs(ctx);
        return p != null ? p.getLong(key, defaultValue) : defaultValue;
    }

    public static Long getLongPrefs(String key) {
        return getLongPrefs(null, key, 0L);
    }

    public static long getLongPrefs(String key, int defaultValue) {
        return getLongPrefs(null, key, (long) defaultValue);
    }

    public static Set<String> getStrinSetPrefs(String key) {
        SharedPreferences p = getPrefs(null);
        return p != null ? p.getStringSet(key, new HashSet<String>()) : new HashSet<String>();
    }

    public static void set(final String key, final Object value) {
        SharedPreferences p = getPrefs(null);
        if (p != null) {
            set(p, key, value);
        }
    }

    public static void set(final Context context, final String key, final Object value) {
        SharedPreferences p = getPrefs(context);
        if (p != null) {
            set(p, key, value);
        }
    }

    public static void set(final SharedPreferences preferences, final String key, final Object value) {
        if (preferences == null) return;
        SharedPreferences.Editor sharedPreferenceEditor = preferences.edit();
        if (value instanceof String) {
            sharedPreferenceEditor.putString(key, (String) value);
        } else if (value instanceof Long) {
            sharedPreferenceEditor.putLong(key, (Long) value);
        } else if (value instanceof Integer) {
            sharedPreferenceEditor.putInt(key, (Integer) value);
        } else if (value instanceof Boolean) {
            sharedPreferenceEditor.putBoolean(key, (Boolean) value);
        } else  if (value instanceof Set){
            sharedPreferenceEditor.putStringSet(key, (Set<String>) value);
        }
        sharedPreferenceEditor.apply();
    }

    public static void clear(final Context context) {
        SharedPreferences p = getPrefs(context);
        if (p != null) {
            p.edit().clear().commit();
        }
    }
}