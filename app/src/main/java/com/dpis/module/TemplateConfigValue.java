package com.dpis.module;

import java.util.Objects;

final class TemplateConfigValue {
    static final TemplateConfigValue EMPTY = new TemplateConfigValue(
            ViewportTargetSpec.off(),
            ViewportTargetType.OFF,
            ViewportApplyMode.OFF,
            null,
            FontApplyMode.OFF,
            null,
            null);

    final ViewportTargetSpec viewportTargetSpec;
    final String viewportTargetType;
    final Integer viewportScalePermilleDraft;
    final Integer viewportWidthDpDraft;
    final String viewportApplyMode;
    final Integer fontScalePercent;
    final String fontApplyMode;
    final String typefaceId;
    final String fontHookDomainsRaw;

    TemplateConfigValue(
            ViewportTargetSpec viewportTargetSpec,
            String viewportApplyMode,
            Integer fontScalePercent,
            String fontApplyMode,
            String typefaceId,
            String fontHookDomainsRaw) {
        this(viewportTargetSpec,
                null,
                null,
                null,
                viewportApplyMode,
                fontScalePercent,
                fontApplyMode,
                typefaceId,
                fontHookDomainsRaw);
    }

    TemplateConfigValue(
            ViewportTargetSpec viewportTargetSpec,
            String viewportTargetType,
            String viewportApplyMode,
            Integer fontScalePercent,
            String fontApplyMode,
            String typefaceId,
            String fontHookDomainsRaw) {
        this(viewportTargetSpec,
                viewportTargetType,
                null,
                null,
                viewportApplyMode,
                fontScalePercent,
                fontApplyMode,
                typefaceId,
                fontHookDomainsRaw);
    }

    TemplateConfigValue(
            ViewportTargetSpec viewportTargetSpec,
            String viewportTargetType,
            Integer viewportScalePermilleDraft,
            Integer viewportWidthDpDraft,
            String viewportApplyMode,
            Integer fontScalePercent,
            String fontApplyMode,
            String typefaceId,
            String fontHookDomainsRaw) {
        this.viewportTargetSpec = viewportTargetSpec != null
                ? viewportTargetSpec
                : ViewportTargetSpec.off();
        this.viewportTargetType = this.viewportTargetSpec.isEnabled()
                ? this.viewportTargetSpec.type()
                : ViewportTargetType.normalize(viewportTargetType);
        this.viewportScalePermilleDraft = normalizeViewportScalePermille(
                viewportScalePermilleDraft);
        this.viewportWidthDpDraft = normalizeViewportWidthDp(viewportWidthDpDraft);
        this.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode);
        this.fontScalePercent = normalizeFontScalePercent(fontScalePercent);
        this.fontApplyMode = FontApplyMode.normalize(fontApplyMode);
        this.typefaceId = normalizeNullableString(typefaceId);
        this.fontHookDomainsRaw = normalizeNullableString(fontHookDomainsRaw);
    }

    boolean hasAnyValue() {
        return viewportTargetSpec.isEnabled()
                || !ViewportTargetType.OFF.equals(viewportTargetType)
                || viewportScalePermilleDraft != null
                || viewportWidthDpDraft != null
                || ViewportApplyMode.isEnabled(viewportApplyMode)
                || fontScalePercent != null
                || FontApplyMode.isEnabled(fontApplyMode)
                || typefaceId != null
                || fontHookDomainsRaw != null;
    }

    String initialViewportTargetType() {
        if (viewportTargetSpec.isEnabled()) {
            return viewportTargetSpec.type();
        }
        if (!ViewportTargetType.OFF.equals(viewportTargetType)) {
            return viewportTargetType;
        }
        return ViewportTargetType.RELATIVE_SCALE;
    }

    String initialViewportInput() {
        if (viewportTargetSpec.isEnabled()) {
            return AppConfigInputValidation.formatViewportInput(viewportTargetSpec);
        }
        if (ViewportTargetType.ABSOLUTE_DP.equals(initialViewportTargetType())
                && viewportWidthDpDraft != null) {
            return String.valueOf(viewportWidthDpDraft);
        }
        if (ViewportTargetType.RELATIVE_SCALE.equals(initialViewportTargetType())
                && viewportScalePermilleDraft != null) {
            return String.valueOf(viewportScalePermilleDraft / 10);
        }
        return "";
    }

    String initialViewportScaleInput() {
        if (viewportTargetSpec.isRelativeScale()) {
            return AppConfigInputValidation.formatViewportInput(viewportTargetSpec);
        }
        return viewportScalePermilleDraft != null
                ? String.valueOf(viewportScalePermilleDraft / 10)
                : "";
    }

    String initialViewportAbsoluteInput() {
        if (viewportTargetSpec.isAbsoluteDp()) {
            return AppConfigInputValidation.formatViewportInput(viewportTargetSpec);
        }
        return viewportWidthDpDraft != null
                ? String.valueOf(viewportWidthDpDraft)
                : "";
    }

    private static String normalizeNullableString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Integer normalizeFontScalePercent(Integer percent) {
        if (percent == null || percent < 50 || percent > 300) {
            return null;
        }
        return percent;
    }

    private static Integer normalizeViewportScalePermille(Integer scalePermille) {
        if (scalePermille == null
                || scalePermille < ViewportTargetSpec.MIN_SCALE_PERMILLE
                || scalePermille > ViewportTargetSpec.MAX_SCALE_PERMILLE) {
            return null;
        }
        return scalePermille;
    }

    private static Integer normalizeViewportWidthDp(Integer widthDp) {
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
        return viewportTargetSpec.equals(other.viewportTargetSpec)
                && viewportTargetType.equals(other.viewportTargetType)
                && Objects.equals(viewportScalePermilleDraft, other.viewportScalePermilleDraft)
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
                viewportTargetSpec,
                viewportTargetType,
                viewportScalePermilleDraft,
                viewportWidthDpDraft,
                viewportApplyMode,
                fontScalePercent,
                fontApplyMode,
                typefaceId,
                fontHookDomainsRaw);
    }
}
