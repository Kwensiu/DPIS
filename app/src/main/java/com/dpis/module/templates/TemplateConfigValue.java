package com.dpis.module.templates;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetType;

import java.util.Objects;

public final class TemplateConfigValue {
    public static final String VIEWPORT_TARGET_OFF = "off";
    public static final String VIEWPORT_TARGET_RELATIVE_SCALE = "relative_scale";
    public static final String VIEWPORT_TARGET_ABSOLUTE_DP = "absolute_dp";
    public static final String VIEWPORT_MODE_OFF = "off";
    public static final String VIEWPORT_MODE_AUTO = "auto";
    public static final String VIEWPORT_MODE_SYSTEM = "system";
    public static final String VIEWPORT_MODE_COMPAT = "compat";
    public static final String FONT_MODE_OFF = "off";
    public static final String FONT_MODE_SYSTEM_EMULATION = "system_emulation";
    public static final String FONT_MODE_FIELD_REWRITE = "field_rewrite";
    public static final int MIN_VIEWPORT_SCALE_MILLI_PERCENT = 30_000;
    public static final int MAX_VIEWPORT_SCALE_MILLI_PERCENT = 300_000;
    public static final int MIN_FONT_SCALE_PERCENT = 50;
    public static final int MAX_FONT_SCALE_PERCENT = 300;

    public static final TemplateConfigValue EMPTY = new TemplateConfigValue(
            VIEWPORT_TARGET_OFF,
            null,
            null,
            null,
            null,
            VIEWPORT_MODE_OFF,
            null,
            FONT_MODE_OFF,
            null,
            null);

    public final String viewportTargetType;
    public final Integer viewportScaleMilliPercent;
    public final Integer viewportWidthDp;
    public final Integer viewportScaleMilliPercentDraft;
    public final Integer viewportWidthDpDraft;
    public final String viewportApplyMode;
    public final Integer fontScalePercent;
    public final String fontApplyMode;
    public final String typefaceId;
    public final String fontHookDomainsRaw;

    public TemplateConfigValue(
            String viewportTargetType,
            Integer viewportScaleMilliPercent,
            Integer viewportWidthDp,
            String viewportApplyMode,
            Integer fontScalePercent,
            String fontApplyMode,
            String typefaceId,
            String fontHookDomainsRaw) {
        this(viewportTargetType,
                viewportScaleMilliPercent,
                viewportWidthDp,
                null,
                null,
                viewportApplyMode,
                fontScalePercent,
                fontApplyMode,
                typefaceId,
                fontHookDomainsRaw);
    }

    public TemplateConfigValue(
            String viewportTargetType,
            Integer viewportScaleMilliPercent,
            Integer viewportWidthDp,
            Integer viewportScaleMilliPercentDraft,
            Integer viewportWidthDpDraft,
            String viewportApplyMode,
            Integer fontScalePercent,
            String fontApplyMode,
            String typefaceId,
            String fontHookDomainsRaw) {
        String normalizedViewportTargetType = normalizeViewportTargetType(viewportTargetType);
        Integer normalizedScale = normalizeViewportScaleMilliPercent(viewportScaleMilliPercent);
        Integer normalizedWidth = normalizeViewportWidthDp(viewportWidthDp);
        if (VIEWPORT_TARGET_RELATIVE_SCALE.equals(normalizedViewportTargetType)) {
            normalizedWidth = null;
        } else if (VIEWPORT_TARGET_ABSOLUTE_DP.equals(normalizedViewportTargetType)) {
            normalizedScale = null;
        } else {
            normalizedScale = null;
            normalizedWidth = null;
        }
        this.viewportTargetType = normalizedViewportTargetType;
        this.viewportScaleMilliPercent = normalizedScale;
        this.viewportWidthDp = normalizedWidth;
        this.viewportScaleMilliPercentDraft = normalizeViewportScaleMilliPercent(
                viewportScaleMilliPercentDraft);
        this.viewportWidthDpDraft = normalizeViewportWidthDp(viewportWidthDpDraft);
        this.viewportApplyMode = normalizeViewportApplyMode(viewportApplyMode);
        this.fontScalePercent = normalizeFontScalePercent(fontScalePercent);
        this.fontApplyMode = normalizeFontApplyMode(fontApplyMode);
        this.typefaceId = normalizeNullableString(typefaceId);
        this.fontHookDomainsRaw = normalizeNullableString(fontHookDomainsRaw);
    }

    public boolean hasAnyValue() {
        return hasViewportTargetValue()
                || !VIEWPORT_TARGET_OFF.equals(viewportTargetType)
                || viewportScaleMilliPercentDraft != null
                || viewportWidthDpDraft != null
                || isViewportApplyModeEnabled(viewportApplyMode)
                || fontScalePercent != null
                || isFontApplyModeEnabled(fontApplyMode)
                || typefaceId != null
                || fontHookDomainsRaw != null;
    }

    public boolean hasViewportTargetValue() {
        return isRelativeScaleViewport() || isAbsoluteDpViewport();
    }

    public boolean isRelativeScaleViewport() {
        return VIEWPORT_TARGET_RELATIVE_SCALE.equals(viewportTargetType)
                && viewportScaleMilliPercent != null;
    }

    public boolean isAbsoluteDpViewport() {
        return VIEWPORT_TARGET_ABSOLUTE_DP.equals(viewportTargetType)
                && viewportWidthDp != null;
    }

    public String initialViewportTargetType() {
        if (!VIEWPORT_TARGET_OFF.equals(viewportTargetType)) {
            return viewportTargetType;
        }
        return VIEWPORT_TARGET_RELATIVE_SCALE;
    }

    public String initialViewportInput() {
        if (isAbsoluteDpViewport()) {
            return String.valueOf(viewportWidthDp);
        }
        if (isRelativeScaleViewport()) {
            return formatScaleMilliPercentInput(viewportScaleMilliPercent);
        }
        if (VIEWPORT_TARGET_ABSOLUTE_DP.equals(initialViewportTargetType())
                && viewportWidthDpDraft != null) {
            return String.valueOf(viewportWidthDpDraft);
        }
        if (VIEWPORT_TARGET_RELATIVE_SCALE.equals(initialViewportTargetType())
                && viewportScaleMilliPercentDraft != null) {
            return formatScaleMilliPercentInput(viewportScaleMilliPercentDraft);
        }
        return "";
    }

    public String initialViewportScaleInput() {
        if (isRelativeScaleViewport()) {
            return formatScaleMilliPercentInput(viewportScaleMilliPercent);
        }
        return viewportScaleMilliPercentDraft != null
                ? formatScaleMilliPercentInput(viewportScaleMilliPercentDraft)
                : "";
    }

    public String initialViewportAbsoluteInput() {
        if (isAbsoluteDpViewport()) {
            return String.valueOf(viewportWidthDp);
        }
        return viewportWidthDpDraft != null
                ? String.valueOf(viewportWidthDpDraft)
                : "";
    }

    public static String normalizeViewportTargetType(String type) {
        if (VIEWPORT_TARGET_RELATIVE_SCALE.equals(type)) {
            return VIEWPORT_TARGET_RELATIVE_SCALE;
        }
        if (VIEWPORT_TARGET_ABSOLUTE_DP.equals(type)) {
            return VIEWPORT_TARGET_ABSOLUTE_DP;
        }
        return VIEWPORT_TARGET_OFF;
    }

    public static String normalizeViewportApplyMode(String mode) {
        if (VIEWPORT_MODE_AUTO.equals(mode)) {
            return VIEWPORT_MODE_AUTO;
        }
        if (VIEWPORT_MODE_SYSTEM.equals(mode)) {
            return VIEWPORT_MODE_SYSTEM;
        }
        if (VIEWPORT_MODE_COMPAT.equals(mode)) {
            return VIEWPORT_MODE_COMPAT;
        }
        return VIEWPORT_MODE_OFF;
    }

    public static boolean isViewportApplyModeEnabled(String mode) {
        return !VIEWPORT_MODE_OFF.equals(normalizeViewportApplyMode(mode));
    }

    public static String normalizeFontApplyMode(String mode) {
        if (FONT_MODE_SYSTEM_EMULATION.equals(mode)) {
            return FONT_MODE_SYSTEM_EMULATION;
        }
        if (FONT_MODE_FIELD_REWRITE.equals(mode)) {
            return FONT_MODE_FIELD_REWRITE;
        }
        return FONT_MODE_OFF;
    }

    public static boolean isFontApplyModeEnabled(String mode) {
        return !FONT_MODE_OFF.equals(normalizeFontApplyMode(mode));
    }

    public static String formatScaleMilliPercent(int scaleMilliPercent) {
        return formatScaleMilliPercentInput(scaleMilliPercent) + "%";
    }

    public static String formatScaleMilliPercentInput(int scaleMilliPercent) {
        int wholePercent = scaleMilliPercent / 1000;
        int fractional = scaleMilliPercent % 1000;
        if (fractional == 0) {
            return String.valueOf(wholePercent);
        }
        String fraction = String.format("%03d", fractional);
        int end = fraction.length();
        while (end > 0 && fraction.charAt(end - 1) == '0') {
            end--;
        }
        return wholePercent + "." + fraction.substring(0, end);
    }

    public static Integer parsePositiveIntOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static Integer parseFontScalePercentOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return (value >= MIN_FONT_SCALE_PERCENT && value <= MAX_FONT_SCALE_PERCENT)
                    ? value
                    : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static Integer parseViewportScaleMilliPercentOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()
                || trimmed.startsWith(".")
                || trimmed.startsWith("+")
                || trimmed.endsWith("%")) {
            return null;
        }
        int dotIndex = trimmed.indexOf('.');
        int integerPart;
        int fractionalPart = 0;
        int fractionalDigits = 0;
        if (dotIndex < 0) {
            try {
                integerPart = Integer.parseInt(trimmed);
            } catch (NumberFormatException ignored) {
                return null;
            }
        } else {
            String intPart = trimmed.substring(0, dotIndex);
            String fracPart = trimmed.substring(dotIndex + 1);
            if (intPart.isEmpty() || fracPart.length() > 3) {
                return null;
            }
            try {
                integerPart = Integer.parseInt(intPart);
            } catch (NumberFormatException ignored) {
                return null;
            }
            if (!fracPart.isEmpty()) {
                try {
                    fractionalPart = Integer.parseInt(fracPart);
                    fractionalDigits = fracPart.length();
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        if (trimmed.length() > 1 && trimmed.charAt(0) == '0' && trimmed.charAt(1) != '.') {
            return null;
        }
        int scaleMilliPercent = integerPart * 1000;
        for (int i = fractionalDigits; i > 0 && i < 3; i++) {
            fractionalPart *= 10;
        }
        scaleMilliPercent += fractionalPart;
        return normalizeViewportScaleMilliPercent(scaleMilliPercent);
    }

    public static boolean isViewportInputValid(String raw, String viewportTargetType) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        if (VIEWPORT_TARGET_RELATIVE_SCALE.equals(normalizeViewportTargetType(viewportTargetType))) {
            return parseViewportScaleMilliPercentOrNull(raw) != null;
        }
        return parsePositiveIntOrNull(raw) != null;
    }

    public static boolean isFontScaleInputValid(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        return parseFontScalePercentOrNull(raw) != null;
    }

    public static int toLegacyScalePermille(int scaleMilliPercent) {
        return Math.round(scaleMilliPercent / 100.0f);
    }

    public static int fromLegacyScalePermille(int scalePermille) {
        return scalePermille * 100;
    }

    private static String normalizeNullableString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Integer normalizeFontScalePercent(Integer percent) {
        if (percent == null
                || percent < MIN_FONT_SCALE_PERCENT
                || percent > MAX_FONT_SCALE_PERCENT) {
            return null;
        }
        return percent;
    }

    public static Integer normalizeViewportScaleMilliPercent(Integer scaleMilliPercent) {
        if (scaleMilliPercent == null
                || scaleMilliPercent < MIN_VIEWPORT_SCALE_MILLI_PERCENT
                || scaleMilliPercent > MAX_VIEWPORT_SCALE_MILLI_PERCENT) {
            return null;
        }
        return scaleMilliPercent;
    }

    public static Integer normalizeViewportWidthDp(Integer widthDp) {
        if (widthDp == null || widthDp <= 0) {
            return null;
        }
        return widthDp;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TemplateConfigValue other)) {
            return false;
        }
        return viewportTargetType.equals(other.viewportTargetType)
                && Objects.equals(viewportScaleMilliPercent, other.viewportScaleMilliPercent)
                && Objects.equals(viewportWidthDp, other.viewportWidthDp)
                && Objects.equals(viewportScaleMilliPercentDraft, other.viewportScaleMilliPercentDraft)
                && Objects.equals(viewportWidthDpDraft, other.viewportWidthDpDraft)
                && viewportApplyMode.equals(other.viewportApplyMode)
                && Objects.equals(fontScalePercent, other.fontScalePercent)
                && fontApplyMode.equals(other.fontApplyMode)
                && Objects.equals(typefaceId, other.typefaceId)
                && Objects.equals(fontHookDomainsRaw, other.fontHookDomainsRaw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                viewportTargetType,
                viewportScaleMilliPercent,
                viewportWidthDp,
                viewportScaleMilliPercentDraft,
                viewportWidthDpDraft,
                viewportApplyMode,
                fontScalePercent,
                fontApplyMode,
                typefaceId,
                fontHookDomainsRaw);
    }
}
