package com.yalig.snapstudy.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.yalig.snapstudy.R;

public class ViewAnimationUtils {

    /**
     * Applies a tactile spring press effect (scale down on press, bounce back on release).
     */
    public static void applyTouchScaleEffect(View view) {
        if (view == null) return;

        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(100)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .setInterpolator(new OvershootInterpolator(2.0f))
                            .start();
                    break;
            }
            // Return false so existing OnClickListener or Ripple effects continue to work normally
            return false;
        });
    }

    /**
     * Staggered entrance animation for cards/views sliding up smoothly from bottom.
     */
    public static void animateStaggeredEntrance(View view, long delayMs) {
        if (view == null) return;

        view.setAlpha(0f);
        view.setTranslationY(48f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(delayMs)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    /**
     * Bouncy tab selection micro-animation for bottom navigation.
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
     * Applies open transition to current activity.
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
     * Applies close transition to current activity.
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
     * Starts an Activity with modern smooth slide/fade transition.
     */
    public static void startActivityWithTransition(Activity activity, Intent intent) {
        if (activity == null || intent == null) return;
        activity.startActivity(intent);
        applyActivityOpenTransition(activity);
    }

    /**
     * Finishes an Activity with modern smooth reverse slide/fade transition.
     */
    public static void finishWithTransition(Activity activity) {
        if (activity == null) return;
        activity.finish();
        applyActivityCloseTransition(activity);
    }
}
