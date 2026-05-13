package com.dpis.displaytool.scene;

import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.TextView;

import com.dpis.displaytool.CompanionContract;

final class TextSceneSupport {
    static final float BASE_SP = 14f;
    static final String VIEW_PRIMARY = "text_primary";
    static final String EVENT_FIRST_LAYOUT = "first_layout";

    private TextSceneSupport() {
    }

    static boolean supportsNormal(String variant) {
        return CompanionContract.VARIANT_NORMAL.equals(variant);
    }

    static boolean supportsNormalFragile(String variant) {
        return CompanionContract.VARIANT_NORMAL.equals(variant)
                || CompanionContract.VARIANT_FRAGILE.equals(variant);
    }

    static boolean isFragile(String variant) {
        return CompanionContract.VARIANT_FRAGILE.equals(variant);
    }

    static void applySp(TextView textView, float baseSp) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSp);
    }

    static void applyDoubleScaledPx(TextView textView, float baseSp) {
        DisplayMetrics metrics = textView.getResources().getDisplayMetrics();
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseSp * metrics.scaledDensity * 1.5f);
    }

    static void applyProgrammaticPx(TextView textView, float baseSp) {
        DisplayMetrics metrics = textView.getResources().getDisplayMetrics();
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseSp * metrics.scaledDensity);
    }

    static void applyNoScalePx(TextView textView, float baseSp) {
        DisplayMetrics metrics = textView.getResources().getDisplayMetrics();
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseSp * metrics.density);
    }
}
