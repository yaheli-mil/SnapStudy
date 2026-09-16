package com.yalig.snapstudy;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * מחלקת היישום הראשית (Application Class) של SnapStudy.
 * מחלקה זו מופעלת ראשונה כאשר מערכת ההפעלה Android מעלה את תהליך האפליקציה לזיכרון.
 * היא משמשת כנקודת גישה גלובלית (Singleton) ל-Context של היישום,
 * ומנהלת את הגדרות המערכת הגלובליות: ערכת הנושא (Theme), נעילת כיוון המסך, והעדפות משתמש.
 */
public class SnapStudyApp extends Application {

    // מופע יחיד של מחלקת ה-Application לצורך גישה גלובלית (תבנית Singleton)
    private static SnapStudyApp instance;

    // שמות קובץ ההעדפות והמפתחות לשמירה מקומית ב-SharedPreferences
    public static final String PREFS_NAME = "snapstudy_theme_prefs";
    public static final String KEY_NIGHT_MODE = "night_mode";
    public static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String KEY_REMINDER_TIME = "reminder_time";
    public static final String KEY_SOUND_ENABLED = "sound_enabled";

    /**
     * פונקציה סטטית להחזרת המופע היחיד של האפליקציה מכל מקום בקוד.
     * @return מופע ה-Application הנוכחי.
     */
    public static SnapStudyApp getInstance() {
        return instance;
    }

    /**
     * פונקציית האתחול הראשית של האפליקציה במחזור החיים של מערכת Android.
     * מופעלת לפני עליית כל Activity או Service אחר במערכת.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // שמירת המופע הנוכחי במשתנה הסטטי
        instance = this;

        // החלת ערכת הנושא (בהיר/כהה/מערכת) שנשמרה בעבר על ידי המשתמש
        applySavedTheme(this);

        // רישום מאזין גלובלי למחזור החיים של כל ה-Activities באפליקציה
        // מטרתו: לוודא שכל מסך שנפתח ננעל אוטומטית למצב מאונך (Portrait)
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(android.app.Activity activity, android.os.Bundle savedInstanceState) {
                try {
                    // נעילת כיוון המסך לתצוגה אנכית
                    activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } catch (Exception ignored) {
                    // הגנה מפני קריסה במכשירים מסוימים עם חלון צף
                }
            }

            @Override public void onActivityStarted(android.app.Activity activity) {}
            @Override public void onActivityResumed(android.app.Activity activity) {}
            @Override public void onActivityPaused(android.app.Activity activity) {}
            @Override public void onActivityStopped(android.app.Activity activity) {}
            @Override public void onActivitySaveInstanceState(android.app.Activity activity, android.os.Bundle outState) {}
            @Override public void onActivityDestroyed(android.app.Activity activity) {}
        });
    }

    /**
     * קוראת את מצב הנושא השמור מה-SharedPreferences ומחילה אותו גלובלית על האפליקציה.
     * ברירת המחדל הינה מעקב אחר הגדרת המערכת של המכשיר.
     * @param context הקשר המערכת לקריאת ההעדפות.
     */
    public static void applySavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int mode = prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /**
     * שומרת את מצב התצוגה הנבחר (בהיר, כהה, או אוטומטי לפי המערכת) ומחילה אותו מידית.
     * @param context הקשר המערכת לכתיבת ההעדפות.
     * @param mode קוד מצב הנושא (מתוך מחלקת AppCompatDelegate).
     */
    public static void saveThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /**
     * מחילה את מצב הנושא השמור ספציפית על Activity מסוים, למניעת הבהובי צבע בעת מעבר מסכים.
     * @param activity המסך שעליו מעוניינים להחיל את העיצוב.
     */
    public static void applyThemeToActivity(androidx.appcompat.app.AppCompatActivity activity) {
        int mode = getSavedThemeMode(activity);
        activity.getDelegate().setLocalNightMode(mode);
    }

    /**
     * מחזירה את מצב הנושא ששמור כרגע בהגדרות המכשיר.
     * @param context הקשר המערכת.
     * @return קוד המצב (MODE_NIGHT_NO, MODE_NIGHT_YES או MODE_NIGHT_FOLLOW_SYSTEM).
     */
    public static int getSavedThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    /**
     * בדיקה האם התראות המערכת ותזכורות הלימוד מופעלות.
     * @param context הקשר המערכת לקריאה.
     * @return אמת (true) אם התראות פעילות, אחרת שקר (false). ברירת מחדל: אמת.
     */
    public static boolean isNotificationsEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    /**
     * שמירת מצב הפעלת/כיבוי ההתראות בהגדרות המשתמש.
     * @param context הקשר המערכת.
     * @param enabled האם להפעיל התראות.
     */
    public static void setNotificationsEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    /**
     * קריאת שעת התזכורת היומית שנבחרה לתרגול כרטיסיות.
     * @param context הקשר המערכת.
     * @return מחרוזת בפורמט HH:mm (למשל "20:00").
     */
    public static String getReminderTime(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_REMINDER_TIME, "20:00");
    }

    /**
     * שמירת שעת התזכורת היומית החדשה שנבחרה ע"י המשתמש.
     * @param context הקשר המערכת.
     * @param time שעה בפורמט HH:mm.
     */
    public static void setReminderTime(Context context, String time) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_REMINDER_TIME, time).apply();
    }

    /**
     * בדיקה האם אפקטים קוליים מופעלים באפליקציה (בעת דירוג כרטיסיות, לחיצות וכד').
     * @param context הקשר המערכת.
     * @return אמת (true) אם צלילים מופעלים, ברירת מחדל: אמת.
     */
    public static boolean isSoundEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SOUND_ENABLED, true);
    }

    /**
     * עדכון הגדרת האפקטים הקוליים.
     * @param context הקשר המערכת.
     * @param enabled האם להפעיל צלילים.
     */
    public static void setSoundEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }
}
