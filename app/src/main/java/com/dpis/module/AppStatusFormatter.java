package com.dpis.module;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.util.ArrayList;
import java.util.List;

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

    static boolean shouldWarnFontEmulation(Integer fontScalePercent,
                                           String fontMode,
                                           boolean systemHooksEnabled,
                                           boolean dpisEnabled) {
        if (!dpisEnabled || fontScalePercent == null) {
            return false;
        }
        String requested = FontApplyMode.normalize(fontMode);
        String effective = EffectiveModeResolver.resolveFontMode(
                requested, systemHooksEnabled);
        return FontApplyMode.SYSTEM_EMULATION.equals(requested)
                && !FontApplyMode.SYSTEM_EMULATION.equals(effective);
    }

    static CharSequence applyConfigSegmentsWarnStyle(String statusText,
                                                     int warnColor,
                                                     boolean warnViewport,
                                                     boolean warnFont) {
        if (statusText == null || statusText.isEmpty()) {
            return statusText;
        }
        int[][] warnRanges = resolveWarnSegmentRanges(statusText, warnViewport, warnFont);
        if (warnRanges.length == 0) {
            return statusText;
        }
        SpannableString styled = new SpannableString(statusText);
        for (int[] range : warnRanges) {
            styled.setSpan(new ForegroundColorSpan(warnColor), range[0], range[1],
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return styled;
    }

    static int[][] resolveWarnSegmentRanges(String statusText,
                                            boolean warnViewport,
                                            boolean warnFont) {
        if (statusText == null || statusText.isEmpty() || (!warnViewport && !warnFont)) {
            return new int[0][];
        }
        List<int[]> ranges = new ArrayList<>(2);
        if (warnViewport) {
            int[] viewportRange = resolveSegmentRange(statusText, 1);
            if (viewportRange != null) {
                ranges.add(viewportRange);
            }
        }
        if (warnFont) {
            int[] fontRange = resolveSegmentRange(statusText, 2);
            if (fontRange != null) {
                ranges.add(fontRange);
            }
        }
        return ranges.toArray(new int[0][]);
    }

    private static int[] resolveSegmentRange(String statusText, int targetSegmentIndex) {
        int segmentStart = 0;
        int segmentIndex = 0;
        while (segmentStart <= statusText.length()) {
            int separatorIndex = statusText.indexOf('|', segmentStart);
            int segmentEnd = separatorIndex >= 0 ? separatorIndex : statusText.length();
            if (segmentIndex == targetSegmentIndex) {
                int trimmedStart = segmentStart;
                int trimmedEnd = segmentEnd;
                while (trimmedStart < trimmedEnd
                        && Character.isWhitespace(statusText.charAt(trimmedStart))) {
                    trimmedStart++;
                }
                while (trimmedEnd > trimmedStart
                        && Character.isWhitespace(statusText.charAt(trimmedEnd - 1))) {
                    trimmedEnd--;
                }
                if (trimmedStart < trimmedEnd) {
                    return new int[]{trimmedStart, trimmedEnd};
                }
                return null;
            }
            if (separatorIndex < 0) {
                return null;
            }
            segmentStart = separatorIndex + 1;
            segmentIndex++;
        }
        return null;
    }

    static String toCompactDisplay(String statusText) {
        if (statusText == null || statusText.isEmpty()) {
            return statusText;
        }
        String[] segments = statusText.split("\\|");
        if (segments.length == 0) {
            return statusText;
        }
        StringBuilder builder = new StringBuilder();
        int appendedCount = 0;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i] != null ? segments[i].trim() : "";
            if (segment.isEmpty()) {
                continue;
            }
            String compact = compactSegment(i, segment);
            if (compact.isEmpty()) {
                continue;
            }
            if (appendedCount > 0) {
                builder.append(" | ");
            }
            builder.append(compact);
            appendedCount++;
        }
        return appendedCount > 0 ? builder.toString() : statusText;
    }

    private static String compactSegment(int index, String segment) {
        String compact = segment
                .replace("(伪装)", "")
                .replace("(替换)", "")
                .replace("·伪装", "")
                .replace("·替换", "")
                .trim();
        if (index >= 2 && compact.startsWith("字体")) {
            compact = compact.substring(2).trim();
        } else if (index >= 2 && compact.startsWith("字")) {
            compact = compact.substring(1).trim();
        }
        return compact;
    }
}
