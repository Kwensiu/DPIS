package com.dpis.module.ui;

import android.os.Build;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.Display;
import android.view.DisplayShape;
import android.view.RoundedCorner;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;

import java.util.function.IntSupplier;

import androidx.core.graphics.Insets;
import androidx.annotation.DimenRes;
import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dpis.module.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public final class WindowInsetsBinder {
    private static final int DISPLAY_SHAPE_API_LEVEL = 35;

    private WindowInsetsBinder() {
    }

    public static void applySafeDrawingPadding(View target,
            boolean applyLeft,
            boolean applyTop,
            boolean applyRight,
            boolean applyBottom) {
        applySafeDrawingPadding(target, applyLeft, applyTop, applyRight, applyBottom,
                R.dimen.round_screen_safe_padding);
    }

    public static void applySafeDrawingPadding(View target,
            boolean applyLeft,
            boolean applyTop,
            boolean applyRight,
            boolean applyBottom,
            @DimenRes int compactWatchPaddingRes) {
        if (target == null) {
            return;
        }
        final int baseLeft = target.getPaddingLeft();
        final int baseTop = target.getPaddingTop();
        final int baseRight = target.getPaddingRight();
        final int baseBottom = target.getPaddingBottom();
        int initialRoundSafePadding = WatchUiMode.shouldUseCompactUi(target.getContext())
                ? target.getResources().getDimensionPixelSize(compactWatchPaddingRes)
                : 0;
        applyPadding(target, baseLeft, baseTop, baseRight, baseBottom,
                Insets.NONE, initialRoundSafePadding,
                applyLeft, applyTop, applyRight, applyBottom);
        ViewCompat.setOnApplyWindowInsetsListener(target, (view, windowInsets) -> {
            Insets safeDrawing = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            int roundSafePadding = hasCompactRoundDisplay(view, windowInsets)
                    ? view.getResources().getDimensionPixelSize(compactWatchPaddingRes)
                    : 0;
            applyPadding(view, baseLeft, baseTop, baseRight, baseBottom,
                    safeDrawing, roundSafePadding,
                    applyLeft, applyTop, applyRight, applyBottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(target);
    }

    /** Applies only platform safe-drawing insets, without compact-watch round-screen padding. */
    public static void applySystemBarPadding(View target,
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
            applyPadding(view, baseLeft, baseTop, baseRight, baseBottom,
                    safeDrawing, 0, applyLeft, applyTop, applyRight, applyBottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(target);
    }

    private static void applyPadding(View view,
            int baseLeft,
            int baseTop,
            int baseRight,
            int baseBottom,
            Insets safeDrawing,
            int roundSafePadding,
            boolean applyLeft,
            boolean applyTop,
            boolean applyRight,
            boolean applyBottom) {
        view.setPadding(
                baseLeft + (applyLeft ? safeDrawing.left + roundSafePadding : 0),
                baseTop + (applyTop ? safeDrawing.top + roundSafePadding : 0),
                baseRight + (applyRight ? safeDrawing.right + roundSafePadding : 0),
                baseBottom + (applyBottom ? safeDrawing.bottom + roundSafePadding : 0));
    }

    /**
     * Wear images do not always expose their circular DisplayShape through application insets.
     * A compact watch with missing shape metadata gets the conservative fallback rather than
     * allowing controls to be clipped by an unreported round edge.
     */
    private static boolean hasCompactRoundDisplay(View view, WindowInsetsCompat windowInsets) {
        if (!WatchUiMode.shouldUseCompactUi(view.getContext())) {
            return false;
        }
        if (WatchUiMode.shouldApplyRoundSafePadding(view.getContext())) {
            return true;
        }
        Boolean nonRectangularDisplay = isNonRectangularDisplay(view);
        if (nonRectangularDisplay != null) {
            return nonRectangularDisplay;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        WindowManager windowManager = view.getContext().getSystemService(WindowManager.class);
        if (windowManager != null && hasFullyRoundCorners(view,
                windowManager.getCurrentWindowMetrics().getWindowInsets())) {
            return true;
        }
        // Older platforms without shape metadata remain conservative to avoid circular clipping.
        return true;
    }

    /** Returns null when the platform cannot report a display shape. */
    private static Boolean isNonRectangularDisplay(View view) {
        if (Build.VERSION.SDK_INT < DISPLAY_SHAPE_API_LEVEL) {
            return null;
        }
        Display display = view.getDisplay();
        if (display == null) {
            return null;
        }
        DisplayShape shape = display.getShape();
        Path path = shape != null ? shape.getPath() : null;
        if (path == null || path.isEmpty()) {
            return null;
        }
        return !path.isRect(new RectF());
    }

    private static boolean hasFullyRoundCorners(View view, WindowInsets windowInsets) {
        int diameter = Math.min(view.getWidth(), view.getHeight());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || windowInsets == null
                || diameter <= 0) {
            return false;
        }
        int minimumRoundRadius = diameter / 2;
        return hasRoundedCorner(windowInsets, RoundedCorner.POSITION_TOP_LEFT, minimumRoundRadius)
                && hasRoundedCorner(windowInsets, RoundedCorner.POSITION_TOP_RIGHT, minimumRoundRadius)
                && hasRoundedCorner(windowInsets, RoundedCorner.POSITION_BOTTOM_LEFT, minimumRoundRadius)
                && hasRoundedCorner(windowInsets, RoundedCorner.POSITION_BOTTOM_RIGHT, minimumRoundRadius);
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static boolean hasRoundedCorner(WindowInsets windowInsets, int position,
            int minimumRoundRadius) {
        RoundedCorner corner = windowInsets.getRoundedCorner(position);
        return corner != null && corner.getRadius() >= minimumRoundRadius;
    }

    public static void applyNavigationBarMargins(FloatingActionButton fab) {
        applyNavigationBarMargins(fab, () -> 0);
    }

    /**
     * Preserves the legacy physical FAB clearance when an enclosing Compose
     * Scaffold has already reserved bottom navigation content.
     */
    public static void applyNavigationBarMargins(FloatingActionButton fab,
            IntSupplier contentBottomPaddingSupplier) {
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
            int contentBottomPadding = contentBottomPaddingSupplier != null
                    ? contentBottomPaddingSupplier.getAsInt()
                    : 0;
            params.bottomMargin = resolveNavigationBarBottomMargin(
                    baseBottom,
                    navigationBars.bottom,
                    contentBottomPadding
            );
            params.setMarginEnd(baseEnd + sideInset);
            fab.setLayoutParams(params);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(fab);
    }

    /** Reapplies the latest root insets after Compose changes its reserved content area. */
    public static void refreshNavigationBarMargins(FloatingActionButton fab) {
        if (fab == null) {
            return;
        }
        WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(fab);
        if (rootInsets != null) {
            ViewCompat.dispatchApplyWindowInsets(fab, rootInsets);
            return;
        }
        ViewCompat.requestApplyInsets(fab);
    }

    static int resolveNavigationBarBottomMargin(int baseBottom,
            int navigationBarBottomInset,
            int contentBottomPadding) {
        int safeBaseBottom = Math.max(0, baseBottom);
        int safeNavigationBarInset = Math.max(0, navigationBarBottomInset);
        int safeContentBottomPadding = Math.max(0, contentBottomPadding);
        int legacyPhysicalBottomClearance = safeBaseBottom + safeNavigationBarInset;
        return Math.max(0, legacyPhysicalBottomClearance - safeContentBottomPadding);
    }
}
