package com.yalig.snapstudy.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;

/**
 * שמירת הסטטיסטיקות של הלמידה בטלפון בעזרת SharedPreferences:
 * רצף ימי למידה, כמה כרטיסיות תורגלו היום, יעד יומי ודקות ריכוז.
 */
public class StatsManager {

    private static final String PREFS_NAME = "snapstudy_stats";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // נקרא בכל פעם שהמשתמש מדרג כרטיסייה בתרגול
    public static void recordCardStudied(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String today = LocalDate.now().toString();
        String yesterday = LocalDate.now().minusDays(1).toString();
        String lastDate = prefs.getString("last_study_date", "");

        int cardsToday = prefs.getInt("cards_today", 0);
        int streak = prefs.getInt("streak_days", 0);

        // כרטיסייה ראשונה של יום חדש
        if (!today.equals(lastDate)) {
            cardsToday = 0;
            if (yesterday.equals(lastDate)) {
                streak++;      // למדו גם אתמול - הרצף ממשיך
            } else {
                streak = 1;    // דילגו על יום - הרצף מתחיל מחדש
            }
        }
        cardsToday++;

        prefs.edit()
                .putString("last_study_date", today)
                .putInt("cards_today", cardsToday)
                .putInt("streak_days", streak)
                .apply();
    }

    public static int getCardsToday(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String today = LocalDate.now().toString();
        // אם היום עוד לא למדו - המונה מתחיל מ-0
        if (!today.equals(prefs.getString("last_study_date", ""))) {
            return 0;
        }
        return prefs.getInt("cards_today", 0);
    }

    public static int getStreakDays(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String lastDate = prefs.getString("last_study_date", "");
        String today = LocalDate.now().toString();
        String yesterday = LocalDate.now().minusDays(1).toString();
        // הרצף נשמר רק אם למדו היום או אתמול
        if (lastDate.equals(today) || lastDate.equals(yesterday)) {
            return prefs.getInt("streak_days", 0);
        }
        return 0;
    }

    public static int getDailyGoal(Context context) {
        return getPrefs(context).getInt("daily_goal", 50);
    }

    public static void setDailyGoal(Context context, int goal) {
        getPrefs(context).edit().putInt("daily_goal", goal).apply();
    }

    // הזמן נשמר בשניות כדי שגם תרגול קצר ייספר, ומוצג בדקות
    public static int getFocusMinutes(Context context) {
        return getPrefs(context).getInt("focus_seconds", 0) / 60;
    }

    // מוסיף את זמן התרגול (בשניות) לסך זמן הריכוז
    public static void addFocusSeconds(Context context, int seconds) {
        int total = getPrefs(context).getInt("focus_seconds", 0) + seconds;
        getPrefs(context).edit().putInt("focus_seconds", total).apply();
    }
}
