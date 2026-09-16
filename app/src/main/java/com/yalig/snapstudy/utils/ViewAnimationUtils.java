package com.yalig.snapstudy.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.yalig.snapstudy.R;

/**
 * מחלקת שירות (Utility Class) לניהול אנימציות ממשק, משוב מגע (Haptic/Tactile Feedback) ומעברי מסכים.
 * מחלקה זו מרכזת פונקציות סטטיות המעניקות לאפליקציה תחושה מודרנית, חיה וזורמת:
 * 1. אפקט לחיצה קפיצי על כפתורים וכרטיסים.
 * 2. אנימציות כניסה מדורגות (Staggered Entrance) לרכיבים בעת עליית המסך.
 * 3. מיקרו-אנימציות לבחירת טאבים בסרגל הניווט התחתון.
 * 4. מעברי מסכים מותאמים אישית התואמים לכל גרסאות ה-Android (כולל תמיכה ב-Android 14+).
 */
public class ViewAnimationUtils {

    /**
     * מחילה אפקט לחיצה קפיצי ואינטראקטיבי על רכיב תצוגה (View).
     * בעת לחיצה (ACTION_DOWN) הרכיב מתכווץ קלות ל-95% מגודלו.
     * בעת שחרור האצבע (ACTION_UP) הרכיב חוזר לגודלו המלא עם קפיצה קלה (Overshoot).
     * הערה: הפונקציה מחזירה false מתוך ה-OnTouchListener כדי לא לבלוע את האירוע
     * ולאפשר ל-OnClickListener הרגיל לפעול כסדרו.
     * @param view הרכיב עליו יוחל האפקט.
     */
    public static void applyTouchScaleEffect(View view) {
        if (view == null) return;

        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // כיווץ מהיר ועדין בעת הנחת האצבע
                    v.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(100)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // חזרה קפיצית ונעימה לגודל המקורי
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .setInterpolator(new OvershootInterpolator(2.0f))
                            .start();
                    break;
            }
            // החזרת false מאפשרת לאירועי Click ו-Ripple להמשיך לפעול כרגיל
            return false;
        });
    }

    /**
     * מפעילה אנימציית כניסה מדורגת לרכיבי המסך:
     * הרכיב מתחיל בשקיפות מלאה (Alpha = 0) ומוסט מעט למטה (TranslationY = 48),
     * ומחליק למקומו הטבעי כלפי מעלה עם הופעה חלקה (Fade-in).
     * @param view הרכיב להנפשה.
     * @param delayMs השהייה במילי-שניות לפני תחילת האנימציה (ליצירת רצף מדורג בין רכיבים שונים).
     */
    public static void animateStaggeredEntrance(View view, long delayMs) {
        if (view == null) return;

        // הגדרת מצב התחלתי לפני תחילת האנימציה
        view.setAlpha(0f);
        view.setTranslationY(48f);

        // הפעלת האנימציה עם השהייה מתאימה
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(delayMs)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    /**
     * מיקרו-אנימציה קופצנית לבחירת טאב פעיל בסרגל הניווט התחתון.
     * @param iconView האייקון של הטאב שנבחר.
     * @param dotView נקודת החיווי הזעירה (אם קיימת).
     */
    public static void animateTabSelection(View iconView, View dotView) {
        if (iconView != null) {
            iconView.setScaleX(0.8f);
            iconView.setScaleY(0.8f);
            iconView.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .setDuration(120)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        iconView.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(120)
                                .setInterpolator(new OvershootInterpolator(1.8f))
                                .start();
                    })
                    .start();
        }

        if (dotView != null) {
            dotView.setVisibility(View.VISIBLE);
            dotView.setScaleX(0f);
            dotView.setScaleY(0f);
            dotView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220)
                    .setInterpolator(new OvershootInterpolator(2.5f))
                    .start();
        }
    }

    /**
     * מחילה אנימציית מעבר פתיחה חלקה למסך הנפתח.
     * בודקת את גרסת מערכת ההפעלה Android:
     * ב-Android 14 ומעלה (API 34+) משתמשת ב-overrideActivityTransition המודרני,
     * ובגרסאות קודמות משתמשת ב-overridePendingTransition המסורתי.
     * @param activity המסך ממנו מבוצע המעבר.
     */
    public static void applyActivityOpenTransition(Activity activity) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(
                    Activity.OVERRIDE_TRANSITION_OPEN,
                    R.anim.activity_open_enter,
                    R.anim.activity_open_exit
            );
        } else {
            activity.overridePendingTransition(R.anim.activity_open_enter, R.anim.activity_open_exit);
        }
    }

    /**
     * מחילה אנימציית מעבר סגירה חלקה בעת יציאה או סגירת מסך.
     * תואמת גם ל-Android 14 ומעלה וגם לגרסאות ותיקות.
     * @param activity המסך שנסגר.
     */
    public static void applyActivityCloseTransition(Activity activity) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(
                    Activity.OVERRIDE_TRANSITION_CLOSE,
                    R.anim.activity_close_enter,
                    R.anim.activity_close_exit
            );
        } else {
            activity.overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit);
        }
    }

    /**
     * פותחת Activity חדש בליווי אנימציית מעבר חלקה.
     * @param activity המסך הנוכחי.
     * @param intent כוונת המעבר (Intent).
     */
    public static void startActivityWithTransition(Activity activity, Intent intent) {
        if (activity == null || intent == null) return;
        activity.startActivity(intent);
        applyActivityOpenTransition(activity);
    }

    /**
     * סוגרת את ה-Activity הנוכחי עם אנימציית חזרה חלקה.
     * @param activity המסך לסגירה.
     */
    public static void finishWithTransition(Activity activity) {
        if (activity == null) return;
        activity.finish();
        applyActivityCloseTransition(activity);
    }
}

