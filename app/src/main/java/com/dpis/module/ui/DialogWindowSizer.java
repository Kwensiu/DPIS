package com.dpis.module.ui;

import android.content.Context;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;

import com.dpis.module.R;

public final class DialogWindowSizer {
    private DialogWindowSizer() {
    }

    public static void applyCompactWidth(AlertDialog dialog, Context context) {
        applyWidth(dialog, context, Preset.COMPACT);
    }

    public static void applyStandardWidth(AlertDialog dialog, Context context) {
        applyWidth(dialog, context, Preset.STANDARD);
    }

    public static void applyLargeWidth(AlertDialog dialog, Context context) {
        applyWidth(dialog, context, Preset.LARGE);
    }

    /**
     * Keeps the font configuration dialogs readable without making their
     * compact-screen surface feel wider than the surrounding settings UI.
     */
    public static void applyConfigurationWidth(AlertDialog dialog, Context context) {
        applyWidth(dialog, context, Preset.CONFIGURATION);
    }

    private static void applyWidth(AlertDialog dialog, Context context, Preset preset) {
        if (dialog == null || context == null || dialog.getWindow() == null) {
            return;
        }
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int horizontalMargin = context.getResources().getDimensionPixelSize(
                R.dimen.dialog_window_margin_horizontal);
        Preset resolvedPreset = resolvePreset(context, preset);
        int maxWidth = context.getResources().getDimensionPixelSize(resolvedPreset.maxWidthRes);
        int width = calculateWindowWidth(screenWidth,
                horizontalMargin,
                maxWidth,
                resolvedPreset.widthFraction);
        dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static int calculateWindowWidth(int screenWidth,
            int horizontalMargin,
            int maxWidth,
            float widthFraction) {
        int safeWidth = Math.max(0, screenWidth - horizontalMargin * 2);
        int scaledWidth = Math.round(screenWidth * widthFraction);
        return Math.min(maxWidth, Math.min(safeWidth, scaledWidth));
    }

    private static Preset resolvePreset(Context context, Preset preset) {
        if (preset != Preset.LARGE) {
            return preset;
        }
        int mediumWidthDp = context.getResources().getInteger(
                R.integer.dialog_window_large_min_width_dp);
        int screenWidthDp = context.getResources().getConfiguration().screenWidthDp;
        return screenWidthDp > 0 && screenWidthDp < mediumWidthDp
                ? Preset.STANDARD
                : Preset.LARGE;
    }

    private enum Preset {
        COMPACT(R.dimen.dialog_window_compact_max_width, 0.88f),
        STANDARD(R.dimen.dialog_window_standard_max_width, 0.90f),
        LARGE(R.dimen.dialog_window_large_max_width, 0.92f),
        CONFIGURATION(R.dimen.dialog_window_configuration_max_width, 0.80f);

        final int maxWidthRes;
        final float widthFraction;

        Preset(int maxWidthRes, float widthFraction) {
            this.maxWidthRes = maxWidthRes;
            this.widthFraction = widthFraction;
        }
    }
}
