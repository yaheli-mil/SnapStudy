package com.yalig.snapstudy;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class SnapStudyApp extends Application {

    public static final String PREFS_NAME = "snapstudy_theme_prefs";
    public static final String KEY_NIGHT_MODE = "night_mode";

    @Override
    public void onCreate() {
        super.onCreate();
        applySavedTheme(this);
    }

    public static void applySavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int mode = prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static void saveThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static void applyThemeToActivity(androidx.appcompat.app.AppCompatActivity activity) {
        int mode = getSavedThemeMode(activity);
        activity.getDelegate().setLocalNightMode(mode);
    }

    public static int getSavedThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String KEY_REMINDER_TIME = "reminder_time";
    public static final String KEY_SOUND_ENABLED = "sound_enabled";

    public static boolean isNotificationsEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    public static String getReminderTime(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_REMINDER_TIME, "20:00");
    }

    public static void setReminderTime(Context context, String time) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_REMINDER_TIME, time).apply();
    }

    public static boolean isSoundEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SOUND_ENABLED, true);
    }

    public static void setSoundEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }
}
