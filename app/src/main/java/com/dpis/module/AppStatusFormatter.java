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
        final String viewportScale;
        final String viewportWidth;
        final String font;
        final Locale locale;

        Labels(String injected,
                String notInjected,
                String enabled,
                String disabled,
                String notEnabled,
                String emulation,
                String replace,
                String viewportScale,
                String viewportWidth,
                String font,
                Locale locale) {
            this.injected = injected;
            this.notInjected = notInjected;
            this.enabled = enabled;
            this.disabled = disabled;
            this.notEnabled = notEnabled;
            this.emulation = emulation;
            this.replace = replace;
            this.viewportScale = viewportScale;
            this.viewportWidth = viewportWidth;
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
                resources.getString(R.string.app_status_mode_system),
                resources.getString(R.string.app_status_mode_compat),
                resources.getString(R.string.app_status_viewport_scale),
                resources.getString(R.string.app_status_viewport_width),
                resources.getString(R.string.app_status_font_prefix),
                locale);
    }

    static String format(Resources resources,
            boolean inScope,
            Integer viewportWidthDp,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled) {
        return format(labelsFrom(resources), inScope, viewportWidthDp, viewportMode,
                fontScalePercent, fontMode, typefaceId, dpisEnabled);
    }

    static String format(Labels labels,
            boolean inScope,
            Integer viewportWidthDp,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled) {
        return formatInternal(labels, inScope, viewportWidthDp, viewportMode,
                fontScalePercent, fontMode, typefaceId, dpisEnabled, true, false);
    }

    static String format(Labels labels,
            boolean inScope,
            boolean scopeKnown,
            Integer viewportWidthDp,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled) {
        return formatInternal(labels, inScope, viewportWidthDp, viewportMode,
                fontScalePercent, fontMode, typefaceId, dpisEnabled, scopeKnown, false);
    }

    static String formatCompact(Resources resources,
            boolean inScope,
            Integer viewportWidthDp,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled) {
        return formatCompact(labelsFrom(resources), inScope, viewportWidthDp, viewportMode,
                fontScalePercent, fontMode, typefaceId, dpisEnabled);
    }

    static String formatCompact(Labels labels,
            boolean inScope,
            Integer viewportWidthDp,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled) {
        return formatInternal(labels, inScope, viewportWidthDp, viewportMode,
                fontScalePercent, fontMode, typefaceId, dpisEnabled, true, true);
    }

    static String formatCompact(Resources resources,
            boolean inScope,
            boolean scopeKnown,
            ViewportTargetSpec viewportTargetSpec,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled) {
        return formatCompact(resources, inScope, scopeKnown, viewportTargetSpec, viewportMode,
                fontScalePercent, fontMode, typefaceId, dpisEnabled, false);
    }

    static String formatCompact(Resources resources,
            boolean inScope,
            boolean scopeKnown,
            ViewportTargetSpec viewportTargetSpec,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled,
            boolean appSpecificConfigActive) {
        return formatInternal(labelsFrom(resources), inScope, viewportTargetSpec,
                viewportMode, fontScalePercent, fontMode, typefaceId, dpisEnabled,
                scopeKnown, true, appSpecificConfigActive);
    }

    static String formatCompact(Labels labels,
            boolean inScope,
            boolean scopeKnown,
            ViewportTargetSpec viewportTargetSpec,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled) {
        return formatInternal(labels, inScope, viewportTargetSpec,
                viewportMode, fontScalePercent, fontMode, typefaceId, dpisEnabled,
                scopeKnown, true, false);
    }

    static String formatCompact(Resources resources,
            boolean inScope,
            boolean scopeKnown,
            Integer viewportWidthDp,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled) {
        return formatCompact(labelsFrom(resources), inScope, scopeKnown, viewportWidthDp,
                viewportMode, fontScalePercent, fontMode, typefaceId, dpisEnabled);
    }

    static String formatCompact(Labels labels,
            boolean inScope,
            boolean scopeKnown,
            Integer viewportWidthDp,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled) {
        return formatInternal(labels, inScope, viewportWidthDp, viewportMode,
                fontScalePercent, fontMode, typefaceId, dpisEnabled, scopeKnown, true);
    }

    private static String formatInternal(Labels labels,
            boolean inScope,
            ViewportTargetSpec viewportTargetSpec,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled,
            boolean scopeKnown,
            boolean compact) {
        return formatInternal(labels, inScope, viewportTargetSpec, viewportMode,
                fontScalePercent, fontMode, typefaceId, dpisEnabled, scopeKnown, compact, false);
    }

    private static String formatInternal(Labels labels,
            boolean inScope,
            ViewportTargetSpec viewportTargetSpec,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled,
            boolean scopeKnown,
            boolean compact,
            boolean appSpecificConfigActive) {
        Integer viewportWidthDp = viewportTargetSpec != null && viewportTargetSpec.isAbsoluteDp()
                ? viewportTargetSpec.absoluteWidthDp()
                : null;
        String scopeText = scopeKnown
                ? (inScope ? labels.injected : labels.notInjected)
                : null;
        if (!dpisEnabled) {
            return joinSegments(scopeText, labels.disabled);
        }
        String normalizedViewportMode = ViewportApplyMode.normalize(viewportMode);
        String widthText = viewportTargetSpec != null && viewportTargetSpec.isRelativeScale()
                ? formatViewportScale(labels, viewportTargetSpec.scalePermille(),
                        normalizedViewportMode, compact)
                : (viewportWidthDp != null
                        ? formatViewport(labels, viewportWidthDp, normalizedViewportMode, compact)
                        : (appSpecificConfigActive ? labels.enabled : labels.notEnabled));
        boolean hasCustomTypeface = typefaceId != null && !typefaceId.isBlank();
        String normalizedFontMode = FontApplyMode.normalize(fontMode);
        if (!FontApplyMode.isEnabled(normalizedFontMode) || fontScalePercent == null) {
            return joinSegments(scopeText, widthText,
                    formatFont(labels, null, normalizedFontMode, compact, hasCustomTypeface));
        }
        return joinSegments(scopeText, widthText,
                formatFont(labels, fontScalePercent, normalizedFontMode, compact,
                        hasCustomTypeface));
    }

    private static String formatInternal(Labels labels,
            boolean inScope,
            Integer viewportWidthDp,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled,
            boolean scopeKnown,
            boolean compact) {
        String scopeText = scopeKnown
                ? (inScope ? labels.injected : labels.notInjected)
                : null;
        if (!dpisEnabled) {
            return joinSegments(scopeText, labels.disabled);
        }
        String normalizedViewportMode = ViewportApplyMode.normalize(viewportMode);
        String widthText = viewportWidthDp != null
                ? formatViewport(labels, viewportWidthDp, normalizedViewportMode, compact)
                : labels.notEnabled;
        boolean hasCustomTypeface = typefaceId != null && !typefaceId.isBlank();
        String normalizedFontMode = FontApplyMode.normalize(fontMode);
        if (!FontApplyMode.isEnabled(normalizedFontMode) || fontScalePercent == null) {
            return joinSegments(scopeText, widthText,
                    formatFont(labels, null, normalizedFontMode, compact, hasCustomTypeface));
        }
        return joinSegments(scopeText, widthText,
                formatFont(labels, fontScalePercent, normalizedFontMode, compact,
                        hasCustomTypeface));
    }

    private static String formatViewport(Labels labels,
            int viewportWidthDp,
            String viewportMode,
            boolean compact) {
        String value = labels.viewportWidth
                + " "
                + String.format(labels.locale, "%ddp", viewportWidthDp);
        if (compact) {
            return value;
        }
        return value + "(" + modeText(labels, viewportMode) + ")";
    }

    private static String formatViewportScale(Labels labels,
            int scalePermille,
            String viewportMode,
            boolean compact) {
        String value = labels.viewportScale
                + " "
                + String.format(labels.locale, "%d%%", scalePermille / 10);
        if (compact) {
            return value;
        }
        return value + "(" + modeText(labels, viewportMode) + ")";
    }

    private static String formatFont(Labels labels,
            Integer fontScalePercent,
            String fontMode,
            boolean compact,
            boolean hasCustomTypeface) {
        if (fontScalePercent == null && !hasCustomTypeface) {
            return null;
        }
        String prefix = hasCustomTypeface ? labels.font + "[C]" : labels.font;
        if (fontScalePercent == null) {
            return prefix;
        }
        String value = prefix + " " + String.format(labels.locale, "%d%%", fontScalePercent);
        if (compact) {
            return value;
        }
        return value + "(" + modeText(labels, fontMode) + ")";
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

    static boolean shouldWarnViewportEmulation(ViewportTargetSpec viewportTargetSpec,
            String viewportMode,
            boolean systemHooksEnabled,
            boolean dpisEnabled) {
        return shouldWarnViewportEmulation(
                viewportTargetSpec != null && viewportTargetSpec.isEnabled()
                        ? viewportTargetSpec.activeValue()
                        : null,
                viewportMode,
                systemHooksEnabled,
                dpisEnabled);
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
        int segmentCount = countSegments(statusText);
        int viewportSegmentIndex = segmentCount >= 3 ? 1 : 0;
        int fontMinSegmentIndex = segmentCount >= 3 ? 2 : 1;
        if (warnViewport) {
            int[] viewportRange = resolveSegmentRange(statusText, viewportSegmentIndex);
            if (viewportRange != null) {
                ranges.add(viewportRange);
            }
        }
        if (warnFont) {
            int[] fontRange = resolveFontScaleSegmentRange(statusText, fontMinSegmentIndex);
            if (fontRange != null) {
                ranges.add(fontRange);
            }
        }
        return ranges.toArray(new int[0][]);
    }

    private static int countSegments(String statusText) {
        int count = 1;
        for (int i = 0; i < statusText.length(); i++) {
            if (statusText.charAt(i) == '|') {
                count++;
            }
        }
        return count;
    }

    private static int[] resolveFontScaleSegmentRange(String statusText, int minSegmentIndex) {
        return resolveFirstSegmentRangeContaining(statusText, "%", minSegmentIndex);
    }

    private static int[] resolveFirstSegmentRangeContaining(String statusText,
            String needle,
            int minSegmentIndex) {
        int segmentStart = 0;
        int segmentIndex = 0;
        while (segmentStart <= statusText.length()) {
            int separatorIndex = statusText.indexOf('|', segmentStart);
            int segmentEnd = separatorIndex >= 0 ? separatorIndex : statusText.length();
            if (segmentIndex >= minSegmentIndex
                    && statusText.substring(segmentStart, segmentEnd).contains(needle)) {
                return trimRange(statusText, segmentStart, segmentEnd);
            }
            if (separatorIndex < 0) {
                return null;
            }
            segmentStart = separatorIndex + 1;
            segmentIndex++;
        }
        return null;
    }

    private static int[] resolveSegmentRange(String statusText, int targetSegmentIndex) {
        int segmentStart = 0;
        int segmentIndex = 0;
        while (segmentStart <= statusText.length()) {
            int separatorIndex = statusText.indexOf('|', segmentStart);
            int segmentEnd = separatorIndex >= 0 ? separatorIndex : statusText.length();
            if (segmentIndex == targetSegmentIndex) {
                return trimRange(statusText, segmentStart, segmentEnd);
            }
            if (separatorIndex < 0) {
                return null;
            }
            segmentStart = separatorIndex + 1;
            segmentIndex++;
        }
        return null;
    }

    private static int[] trimRange(String statusText, int segmentStart, int segmentEnd) {
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
}
