package com.dpis.module;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

final class AppStatusFormatter {
    private AppStatusFormatter() {
    }

    static String format(boolean inScope,
                         Integer viewportWidthDp,
                         String viewportMode,
                         Integer fontScalePercent,
                         String fontMode,
                         boolean dpisEnabled) {
        String scopeText = inScope ? "已注入" : "未注入";
        if (!dpisEnabled) {
            return scopeText + " | 已禁用";
        }
        String normalizedViewportMode = ViewportApplyMode.normalize(viewportMode);
        String widthText = viewportWidthDp != null
                ? viewportWidthDp + "dp" + (ViewportApplyMode.FIELD_REWRITE.equals(normalizedViewportMode)
                ? "(替换)" : "(伪装)")
                : "未启用";
        String normalizedFontMode = FontApplyMode.normalize(fontMode);
        if (!FontApplyMode.isEnabled(normalizedFontMode) || fontScalePercent == null) {
            return scopeText + " | " + widthText;
        }
        String fontModeText = FontApplyMode.SYSTEM_EMULATION.equals(normalizedFontMode)
                ? "伪装"
                : "替换";
        return scopeText + " | " + widthText + " | 字体"
                + fontScalePercent + "%(" + fontModeText + ")";
    }

    static boolean shouldWarnViewportEmulation(Integer viewportWidthDp, String viewportMode,
                                               boolean systemHooksEnabled,
                                               boolean dpisEnabled) {
        if (!dpisEnabled) {
            return false;
        }
        if (viewportWidthDp == null) {
            return false;
        }
        String requested = ViewportApplyMode.normalize(viewportMode);
        String effective = EffectiveModeResolver.resolveViewportMode(
                requested, systemHooksEnabled);
        return ViewportApplyMode.SYSTEM_EMULATION.equals(requested)
                && !ViewportApplyMode.SYSTEM_EMULATION.equals(effective);
    }

    static CharSequence applyMiddleSegmentWarnStyle(String statusText, int warnColor) {
        if (statusText == null || statusText.isEmpty()) {
            return statusText;
        }
        int firstSeparator = statusText.indexOf('|');
        if (firstSeparator < 0) {
            return statusText;
        }
        int secondSeparator = statusText.indexOf('|', firstSeparator + 1);
        int segmentStart = firstSeparator + 1;
        int segmentEnd = secondSeparator >= 0 ? secondSeparator : statusText.length();
        while (segmentStart < segmentEnd && Character.isWhitespace(statusText.charAt(segmentStart))) {
            segmentStart++;
        }
        while (segmentEnd > segmentStart && Character.isWhitespace(statusText.charAt(segmentEnd - 1))) {
            segmentEnd--;
        }
        if (segmentStart >= segmentEnd) {
            return statusText;
        }
        SpannableString styled = new SpannableString(statusText);
        styled.setSpan(new ForegroundColorSpan(warnColor), segmentStart, segmentEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return styled;
    }
}
