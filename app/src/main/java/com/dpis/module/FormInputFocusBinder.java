package com.dpis.module;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

final class FormInputFocusBinder {
    private FormInputFocusBinder() {
    }

    static void bindDismissOnOutsideTouch(
            View touchRoot,
            View fallbackFocusView,
            View... inputs
    ) {
        if (touchRoot == null) {
            return;
        }
        touchRoot.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN
                    && hasFocusedInput(inputs)) {
                int rawX = (int) event.getRawX();
                int rawY = (int) event.getRawY();
                if (!isInsideAny(rawX, rawY, inputs)) {
                    clearFocusAndHideIme(fallbackFocusView, inputs);
                }
            }
            return false;
        });
    }

    static void clearFocusAndHideIme(
            View fallbackFocusView,
            View... inputs
    ) {
        if (inputs != null) {
            for (View input : inputs) {
                if (input != null) {
                    input.clearFocus();
                }
            }
        }
        if (fallbackFocusView != null) {
            fallbackFocusView.setFocusable(true);
            fallbackFocusView.setFocusableInTouchMode(true);
            fallbackFocusView.requestFocus();
            InputMethodManager imm = (InputMethodManager)
                    fallbackFocusView.getContext().getSystemService(
                            Context.INPUT_METHOD_SERVICE
                    );
            if (imm != null) {
                imm.hideSoftInputFromWindow(
                        fallbackFocusView.getWindowToken(),
                        0
                );
            }
        }
    }

    static boolean hasFocusedInput(View... inputs) {
        if (inputs == null) {
            return false;
        }
        for (View input : inputs) {
            if (input != null && input.hasFocus()) {
                return true;
            }
        }
        return false;
    }

    static boolean isInsideAny(
            int rawX,
            int rawY,
            View... inputs
    ) {
        if (inputs == null) {
            return false;
        }
        for (View input : inputs) {
            if (input != null && isInsideView(rawX, rawY, input)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInsideView(int rawX, int rawY, View view) {
        if (view.getVisibility() != View.VISIBLE) {
            return false;
        }
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return rawX >= location[0]
                && rawX < location[0] + view.getWidth()
                && rawY >= location[1]
                && rawY < location[1] + view.getHeight();
    }
}
