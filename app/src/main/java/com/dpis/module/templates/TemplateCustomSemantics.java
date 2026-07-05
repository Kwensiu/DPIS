package com.dpis.module.templates;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetType;

public final class TemplateCustomSemantics {
    private TemplateCustomSemantics() {
    }

    public static TemplateConfigValue fromEditorDraft(
            String viewportInput,
            String viewportTargetType,
            String viewportApplyMode,
            String viewportScaleInput,
            String viewportAbsoluteInput,
            String fontScaleInput,
            String fontApplyMode,
            String typefaceId,
            String fontHookDomainsRaw) {
        String normalizedViewportType = TemplateConfigValue.normalizeViewportTargetType(viewportTargetType);
        Integer viewportScaleMilliPercent = TemplateConfigValue.VIEWPORT_TARGET_RELATIVE_SCALE.equals(
                normalizedViewportType)
                ? TemplateConfigValue.parseViewportScaleMilliPercentOrNull(viewportInput)
                : null;
        Integer viewportWidthDp = TemplateConfigValue.VIEWPORT_TARGET_ABSOLUTE_DP.equals(
                normalizedViewportType)
                ? TemplateConfigValue.parsePositiveIntOrNull(viewportInput)
                : null;
        Integer viewportScaleMilliPercentDraft = parseViewportScaleMilliPercentDraft(viewportScaleInput);
        Integer viewportWidthDpDraft = parseViewportWidthDraft(viewportAbsoluteInput);
        Integer fontScalePercent = TemplateConfigValue.parseFontScalePercentOrNull(fontScaleInput);
        return customValue(new TemplateConfigValue(
                normalizedViewportType,
                viewportScaleMilliPercent,
                viewportWidthDp,
                viewportScaleMilliPercentDraft,
                viewportWidthDpDraft,
                TemplateConfigValue.normalizeViewportApplyMode(viewportApplyMode),
                fontScalePercent,
                fontApplyMode,
                typefaceId,
                fontHookDomainsRaw));
    }

    public static TemplateConfigValue customValue(TemplateConfigValue value) {
        TemplateConfigValue normalized = value != null ? value : TemplateConfigValue.EMPTY;
        TemplateConfigValue custom = new TemplateConfigValue(
                viewportTargetTypeForCustomValue(
                        normalized.viewportTargetType,
                        normalized.hasViewportTargetValue(),
                        normalized.viewportScaleMilliPercentDraft,
                        normalized.viewportWidthDpDraft),
                normalized.viewportScaleMilliPercent,
                normalized.viewportWidthDp,
                normalized.viewportScaleMilliPercentDraft,
                normalized.viewportWidthDpDraft,
                normalized.viewportApplyMode,
                normalized.fontScalePercent,
                fontApplyModeForCustomValue(
                        normalized.fontApplyMode,
                        normalized.fontScalePercent),
                normalized.typefaceId,
                normalized.fontHookDomainsRaw);
        return hasCustomValue(custom) ? custom : TemplateConfigValue.EMPTY;
    }

    public static boolean hasCustomValue(TemplateConfigValue value) {
        TemplateConfigValue normalized = value != null ? value : TemplateConfigValue.EMPTY;
        return normalized.hasViewportTargetValue()
                || !TemplateConfigValue.VIEWPORT_TARGET_OFF.equals(normalized.viewportTargetType)
                || normalized.viewportScaleMilliPercentDraft != null
                || normalized.viewportWidthDpDraft != null
                || isCustomViewportApplyMode(normalized.viewportApplyMode)
                || normalized.fontScalePercent != null
                || TemplateConfigValue.isFontApplyModeEnabled(normalized.fontApplyMode)
                || normalized.typefaceId != null
                || normalized.fontHookDomainsRaw != null;
    }

    public static boolean isCustomViewportApplyMode(String viewportApplyMode) {
        String normalized = TemplateConfigValue.normalizeViewportApplyMode(viewportApplyMode);
        return TemplateConfigValue.VIEWPORT_MODE_SYSTEM.equals(normalized)
                || TemplateConfigValue.VIEWPORT_MODE_COMPAT.equals(normalized);
    }

    public static String draftInputForTargetType(
            String viewportInput,
            String viewportTargetType,
            String expectedType) {
        return expectedType.equals(TemplateConfigValue.normalizeViewportTargetType(viewportTargetType))
                ? viewportInput
                : "";
    }

    private static String viewportTargetTypeForCustomValue(
            String viewportTargetType,
            boolean hasViewportTargetValue,
            Integer viewportScaleMilliPercentDraft,
            Integer viewportWidthDpDraft) {
        if (hasViewportTargetValue) {
            return TemplateConfigValue.normalizeViewportTargetType(viewportTargetType);
        }
        String normalized = TemplateConfigValue.normalizeViewportTargetType(viewportTargetType);
        if (TemplateConfigValue.VIEWPORT_TARGET_ABSOLUTE_DP.equals(normalized)
                || viewportScaleMilliPercentDraft != null
                || viewportWidthDpDraft != null) {
            return normalized;
        }
        return TemplateConfigValue.VIEWPORT_TARGET_OFF;
    }

    private static String fontApplyModeForCustomValue(
            String fontApplyMode,
            Integer fontScalePercent) {
        String normalized = TemplateConfigValue.normalizeFontApplyMode(fontApplyMode);
        if (fontScalePercent != null) {
            return TemplateConfigValue.isFontApplyModeEnabled(normalized)
                    ? normalized
                    : TemplateConfigValue.FONT_MODE_SYSTEM_EMULATION;
        }
        return TemplateConfigValue.FONT_MODE_FIELD_REWRITE.equals(normalized)
                ? TemplateConfigValue.FONT_MODE_FIELD_REWRITE
                : TemplateConfigValue.FONT_MODE_OFF;
    }

    private static Integer parseViewportScaleMilliPercentDraft(String rawInput) {
        String raw = rawInput != null ? rawInput.trim() : "";
        if (raw.isEmpty()) {
            return null;
        }
        return TemplateConfigValue.parseViewportScaleMilliPercentOrNull(raw);
    }

    private static Integer parseViewportWidthDraft(String rawInput) {
        return TemplateConfigValue.parsePositiveIntOrNull(rawInput);
    }

    public static boolean isViewportInputValid(String raw, String viewportTargetType) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        return TemplateConfigValue.isViewportInputValid(raw, viewportTargetType);
    }

    public static boolean isFontScaleInputValid(String raw) {
        return TemplateConfigValue.isFontScaleInputValid(raw);
    }
}
