package com.dpis.module.ui;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public final class WindowInsetsBinder {
    private WindowInsetsBinder() {
    }

    public static void applySafeDrawingPadding(View target,
            boolean applyLeft,
            boolean applyTop,
            boolean applyRight,
            boolean applyBottom) {
        if (target == null) {
            return;
        }
        final int baseLeft = target.getPaddingLeft();
        final int baseTop = target.getPaddingTop();
        final int baseRight = target.getPaddingRight();
        final int baseBottom = target.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(target, (view, windowInsets) -> {
            Insets safeDrawing = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(
                    baseLeft + (applyLeft ? safeDrawing.left : 0),
                    baseTop + (applyTop ? safeDrawing.top : 0),
                    baseRight + (applyRight ? safeDrawing.right : 0),
                    baseBottom + (applyBottom ? safeDrawing.bottom : 0));
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(target);
    }

    public static void applyNavigationBarMargins(FloatingActionButton fab) {
        if (fab == null) {
            return;
        }
        ViewGroup.MarginLayoutParams baseParams
                = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
        final int baseBottom = baseParams.bottomMargin;
        final int baseEnd = baseParams.getMarginEnd();
        ViewCompat.setOnApplyWindowInsetsListener(fab, (view, windowInsets) -> {
            Insets navigationBars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.navigationBars());
            int sideInset = Math.max(navigationBars.left, navigationBars.right);
            ViewGroup.MarginLayoutParams params
                    = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
            params.bottomMargin = baseBottom + navigationBars.bottom;
            params.setMarginEnd(baseEnd + sideInset);
            fab.setLayoutParams(params);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(fab);
    }
}
