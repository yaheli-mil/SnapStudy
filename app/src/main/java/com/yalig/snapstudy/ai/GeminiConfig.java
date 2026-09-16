package com.yalig.snapstudy.ai;

import android.content.Context;

/**
 * מחלקת קונפיגורציה לניהול מפתח ה-API של שירות הבינה המלאכותית (Google Gemini).
 * מחלקה זו מספקת למערכת את מפתח הגישה המובנה הנדרש לשליחת בקשות REST
 * אל מודל הראייה הרב-מודאלי Gemini 2.5 Flash.
 */
public class GeminiConfig {

    // מפתח גישה מערכתי מאושר ל-API של Google AI Studio
    private static final String SYSTEM_API_KEY = "AQ.Ab8RN6I1OeiLkVS6UC9m4tDYa4MmbUU1Wj7S_sZjYJ-wRm5lbg";

    /**
     * מחזירה את מפתח ה-API הפעיל לצורך שליחת קריאות הרשת.
     * @param context הקשר המערכת.
     * @return מחרוזת מפתח ה-API המערכתי.
     */
    public static String getApiKey(Context context) {
        return SYSTEM_API_KEY;
    }

    /**
     * בודקת האם מפתח ה-API מוגדר ותקין במערכת.
     * @param context הקשר המערכת.
     * @return אמת (true) היות והמפתח מוגדר מראש במערכת.
     */
    public static boolean hasApiKey(Context context) {
        return true;
    }
}

