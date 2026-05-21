package com.dpis.module;

import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

final class TouchFeedbackBinder {
    private static final long DEFAULT_PRESS_ANIMATION_DURATION_MS = 110L;
    private static final float DEFAULT_PRESS_SCALE = 0.92f;

    private TouchFeedbackBinder() {
    }

    static void bindPressScaleAndHaptic(View view) {
        bindPressScaleAndHaptic(
                view,
                DEFAULT_PRESS_SCALE,
                DEFAULT_PRESS_ANIMATION_DURATION_MS);
    }

    static void bindPressHaptic(View view) {
        if (view == null) {
            return;
        }
        view.setHapticFeedbackEnabled(true);
        view.setOnTouchListener((pressedView, event) -> {
            if (event == null) {
                return false;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    && pressedView.isEnabled()
                    && isInsideView(pressedView, event)) {
                performPressHaptic(pressedView);
            }
            return false;
        });
    }

    static void bindPressScaleAndHaptic(View view, float pressedScale, long animationDurationMs) {
        if (view == null) {
            return;
        }
        view.setHapticFeedbackEnabled(true);
        view.setOnTouchListener((pressedView, event) -> {
            if (event == null) {
                return false;
            }
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                performPressHaptic(pressedView);
                animateScale(pressedView, pressedScale, animationDurationMs);
                return false;
            }
            if (action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL
                    || action == MotionEvent.ACTION_OUTSIDE) {
                animateScale(pressedView, 1f, animationDurationMs);
            }
            return false;
        });
    }

    private static void animateScale(View view, float targetScale, long animationDurationMs) {
        view.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(animationDurationMs)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private static void performPressHaptic(View view) {
        view.performHapticFeedback(resolvePressHapticConstant());
    }

    private static boolean isInsideView(View view, MotionEvent event) {
        return event.getX() >= 0
                && event.getX() < view.getWidth()
                && event.getY() >= 0
                && event.getY() < view.getHeight();
    }

    private static int resolvePressHapticConstant() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return HapticFeedbackConstants.CONFIRM;
        }
        return HapticFeedbackConstants.VIRTUAL_KEY;
    }
}
