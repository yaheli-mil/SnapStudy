package com.yalig.snapstudy.ai;

import com.yalig.snapstudy.BuildConfig;

/**
 * ההגדרות של Gemini: מפתח ה-API ושם המודל.
 */
public class GeminiConfig {

    // מפתח הגישה ל-Gemini API (מ-Google AI Studio).
    // המפתח נשמר בקובץ local.properties, שלא עולה ל-GitHub, ומגיע לכאן דרך BuildConfig.
    public static final String API_KEY = BuildConfig.GEMINI_API_KEY;

    // המודל שמקבל תמונה ומחזיר טקסט
    public static final String MODEL = "gemini-2.5-flash";
}
