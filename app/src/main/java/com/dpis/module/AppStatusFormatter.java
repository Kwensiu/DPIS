package com.dpis.module;

import android.content.res.Resources;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AppStatusFormatter {
    private AppStatusFormatter() {
    }

    static final class Labels {
        final String injected;
        final String notInjected;
        final String enabled;
        final String disabled;
        final String notEnabled;
        final String emulation;
        final String replace;
        final String font;
        final Locale locale;

        Labels(String injected,
                String notInjected,
                String enabled,
                String disabled,
                String notEnabled,
                String emulation,
                String replace,
                String font,
                Locale locale) {
            this.injected = injected;
            this.notInjected = notInjected;
            this.enabled = enabled;
            this.disabled = disabled;
            this.notEnabled = notEnabled;
            this.emulation = emulation;
            this.replace = replace;
            this.font = font;
            this.locale = locale;
        }
    }

    static Labels labelsFrom(Resources resources) {
        android.os.LocaleList locales = resources.getConfiguration().getLocales();
        Locale locale = locales.isEmpty() ? Locale.getDefault() : locales.get(0);
        return new Labels(
                resources.getString(R.string.app_status_injected),
                resources.getString(R.string.app_status_not_injected),
                resources.getString(R.string.app_status_enabled),
                resources.getString(R.string.app_status_disabled),
                resources.getString(R.string.app_status_not_enabled),
                resources.getString(R.string.app_status_mode_emulation),
                resources.getString(R.string.app_status_mode_replace),
                resources.getString(R.string.app_status_font_prefix),
                locale);
    }

    static String format(Resources resources,
            boolean inScope,
            Integer viewportWidthDp,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            boolean dpisEnabled) {
        return format(labelsFrom(resources), inScope, viewportWidthDp, viewportMode,
                fontScalePercent, fontMode, dpisEnabled);
    }

    static String format(Labels labels,
            boolean inScope,
            Integer viewportWidthDp,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            boolean dpisEnabled) {
        return formatInternal(labels, inScope, viewportWidthDp, viewportMode,
                fontScalePercent, fontMode, dpisEnabled, false);
    }

    static String formatCompact(Resources resources,
            boolean inScope,
            Integer viewportWidthDp,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            boolean dpisEnabled) {
        return formatCompact(labelsFrom(resources), inScope, viewportWidthDp, viewportMode,
                fontScalePercent, fontMode, dpisEnabled);
    }

    static String formatCompact(Labels labels,
            boolean inScope,
            Integer viewportWidthDp,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            boolean dpisEnabled) {
        return formatInternal(labels, inScope, viewportWidthDp, viewportMode,
                fontScalePercent, fontMode, dpisEnabled, true);
    }

    private static String formatInternal(Labels labels,
            boolean inScope,
            Integer viewportWidthDp,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            boolean dpisEnabled,
            boolean compact) {
        String scopeText = inScope ? labels.injected : labels.notInjected;
        if (!dpisEnabled) {
            return joinSegments(scopeText, labels.disabled);
        }
        String normalizedViewportMode = ViewportApplyMode.normalize(viewportMode);
        String widthText = viewportWidthDp != null
                ? formatViewport(labels, viewportWidthDp, normalizedViewportMode, compact)
                : labels.notEnabled;
        String normalizedFontMode = FontApplyMode.normalize(fontMode);
        if (!FontApplyMode.isEnabled(normalizedFontMode) || fontScalePercent == null) {
            return joinSegments(scopeText, widthText);
        }
        return joinSegments(scopeText, widthText,
                formatFont(labels, fontScalePercent, normalizedFontMode, compact));
    }

    private static String formatViewport(Labels labels,
            int viewportWidthDp,
            String viewportMode,
            boolean compact) {
        String value = String.format(labels.locale, "%ddp", viewportWidthDp);
        if (compact) {
            return value;
        }
        return value + "(" + modeText(labels, viewportMode) + ")";
    }

    private static String formatFont(Labels labels,
            int fontScalePercent,
            String fontMode,
            boolean compact) {
        String value = String.format(labels.locale, "%d%%", fontScalePercent);
        if (compact) {
            return value;
        }
        return labels.font + value + "(" + modeText(labels, fontMode) + ")";
    }

    private static String modeText(Labels labels, String mode) {
        return ViewportApplyMode.FIELD_REWRITE.equals(mode)
                || FontApplyMode.FIELD_REWRITE.equals(mode)
                        ? labels.replace
                        : labels.emulation;
    }

    private static String joinSegments(String... segments) {
        StringBuilder builder = new StringBuilder();
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(segment);
        }
        return builder.toString();
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
                    return new int[] { trimmedStart, trimmedEnd };
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
}
