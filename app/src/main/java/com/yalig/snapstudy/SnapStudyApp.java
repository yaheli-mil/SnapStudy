package com.yalig.snapstudy;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * מחלקת ה-Application של האפליקציה. רצה פעם אחת כשהאפליקציה עולה.
 * בנוסף יש בה פעולות לשמירת ההגדרות של המשתמש ב-SharedPreferences.
 */
public class SnapStudyApp extends Application {

    private static final String PREFS_NAME = "snapstudy_settings";

    @Override
    public void onCreate() {
        super.onCreate();
        // הפעלת ערכת הנושא (בהיר/כהה/אוטומטי) שהמשתמש בחר בפעם הקודמת
        AppCompatDelegate.setDefaultNightMode(getThemeMode(this));
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // --- ערכת נושא ---
    public static int getThemeMode(Context context) {
        return getPrefs(context).getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setThemeMode(Context context, int mode) {
        getPrefs(context).edit().putInt("theme_mode", mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    // --- תזכורת יומית ---
    public static boolean isReminderEnabled(Context context) {
        return getPrefs(context).getBoolean("reminder_enabled", true);
    }

    public static void setReminderEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean("reminder_enabled", enabled).apply();
    }

    public static String getReminderTime(Context context) {
        return getPrefs(context).getString("reminder_time", "20:00");
    }

    public static void setReminderTime(Context context, String time) {
        getPrefs(context).edit().putString("reminder_time", time).apply();
    }

    // --- צלילים ---
    public static boolean isSoundEnabled(Context context) {
        return getPrefs(context).getBoolean("sound_enabled", true);
    }

    public static void setSoundEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean("sound_enabled", enabled).apply();
    }
}
