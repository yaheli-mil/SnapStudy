package com.yalig.snapstudy.ai;

import android.content.Context;
public class GeminiConfig {

    // Built-in system key
    private static final String SYSTEM_API_KEY = "AQ.Ab8RN6I1OeiLkVS6UC9m4tDYa4MmbUU1Wj7S_sZjYJ-wRm5lbg";

    public static String getApiKey(Context context) {
        return SYSTEM_API_KEY;
    }

    public static boolean hasApiKey(Context context) {
        return true;
    }
}
