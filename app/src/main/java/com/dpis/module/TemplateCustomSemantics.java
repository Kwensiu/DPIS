package com.dpis.module;

final class TemplateCustomSemantics {
    private TemplateCustomSemantics() {
    }

    static TemplateConfigValue fromEditorDraft(
            String viewportInput,
            String viewportTargetType,
            String viewportApplyMode,
            String viewportScaleInput,
            String viewportAbsoluteInput,
            String fontScaleInput,
            String fontApplyMode,
            String typefaceId,
            String fontHookDomainsRaw) {
        ViewportTargetSpec viewportTargetSpec = AppConfigInputValidation.parseViewportTargetSpec(
                viewportInput, viewportTargetType);
        Integer viewportScaleMilliPercentDraft = parseViewportScaleMilliPercentDraft(viewportScaleInput);
        Integer viewportWidthDpDraft = parseViewportWidthDraft(viewportAbsoluteInput);
        Integer fontScalePercent = AppConfigInputValidation.parseFontScalePercentOrNull(
                fontScaleInput);
        return customValue(new TemplateConfigValue(
                viewportTargetSpec,
                viewportTargetType,
                viewportScaleMilliPercentDraft,
                viewportWidthDpDraft,
                ViewportApplyMode.normalize(viewportApplyMode),
                fontScalePercent,
                fontApplyMode,
                typefaceId,
                fontHookDomainsRaw));
    }

    static TemplateConfigValue customValue(TemplateConfigValue value) {
        TemplateConfigValue normalized = value != null ? value : TemplateConfigValue.EMPTY;
        TemplateConfigValue custom = new TemplateConfigValue(
                normalized.viewportTargetSpec,
                viewportTargetTypeForCustomValue(
                        normalized.viewportTargetType,
                        normalized.viewportTargetSpec,
                        normalized.viewportScaleMilliPercentDraft,
                        normalized.viewportWidthDpDraft),
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

    static boolean hasCustomValue(TemplateConfigValue value) {
        TemplateConfigValue normalized = value != null ? value : TemplateConfigValue.EMPTY;
        return normalized.viewportTargetSpec.isEnabled()
                || !ViewportTargetType.OFF.equals(normalized.viewportTargetType)
                || normalized.viewportScaleMilliPercentDraft != null
                || normalized.viewportWidthDpDraft != null
                || isCustomViewportApplyMode(normalized.viewportApplyMode)
                || normalized.fontScalePercent != null
                || FontApplyMode.isEnabled(normalized.fontApplyMode)
                || normalized.typefaceId != null
                || normalized.fontHookDomainsRaw != null;
    }

    static boolean isCustomViewportApplyMode(String viewportApplyMode) {
        String normalized = ViewportApplyMode.normalize(viewportApplyMode);
        return ViewportApplyMode.SYSTEM.equals(normalized)
                || ViewportApplyMode.COMPAT.equals(normalized);
    }

    static String draftInputForTargetType(
            String viewportInput,
            String viewportTargetType,
            String expectedType) {
        return expectedType.equals(ViewportTargetType.normalize(viewportTargetType))
                ? viewportInput
                : "";
    }

    private static String viewportTargetTypeForCustomValue(
            String viewportTargetType,
            ViewportTargetSpec viewportTargetSpec,
            Integer viewportScaleMilliPercentDraft,
            Integer viewportWidthDpDraft) {
        if (viewportTargetSpec != null && viewportTargetSpec.isEnabled()) {
            return viewportTargetSpec.type();
        }
        String normalized = ViewportTargetType.normalize(viewportTargetType);
        if (ViewportTargetType.ABSOLUTE_DP.equals(normalized)
                || viewportScaleMilliPercentDraft != null
                || viewportWidthDpDraft != null) {
            return normalized;
        }
        return ViewportTargetType.OFF;
    }

    private static String fontApplyModeForCustomValue(
            String fontApplyMode,
            Integer fontScalePercent) {
        String normalized = FontApplyMode.normalize(fontApplyMode);
        if (fontScalePercent != null) {
            return FontApplyMode.isEnabled(normalized)
                    ? normalized
                    : FontApplyMode.SYSTEM_EMULATION;
        }
        return FontApplyMode.FIELD_REWRITE.equals(normalized)
                ? FontApplyMode.FIELD_REWRITE
                : FontApplyMode.OFF;
    }

    private static Integer parseViewportScaleMilliPercentDraft(String rawInput) {
        String raw = rawInput != null ? rawInput.trim() : "";
        if (raw.isEmpty()) {
            return null;
        }
        return AppConfigInputValidation.parseViewportScaleMilliPercentOrNull(raw);
    }

    private static Integer parseViewportWidthDraft(String rawInput) {
        return AppConfigInputValidation.parsePositiveIntOrNull(rawInput);
    }
}
