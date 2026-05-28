package com.dpis.module;

import java.util.Objects;

final class TemplateConfigValue {
    static final TemplateConfigValue EMPTY = new TemplateConfigValue(
            ViewportTargetSpec.off(),
            ViewportApplyMode.OFF,
            null,
            FontApplyMode.OFF,
            null,
            null);

    final ViewportTargetSpec viewportTargetSpec;
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
        this.viewportTargetSpec = viewportTargetSpec != null
                ? viewportTargetSpec
                : ViewportTargetSpec.off();
        this.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode);
        this.fontScalePercent = normalizeFontScalePercent(fontScalePercent);
        this.fontApplyMode = FontApplyMode.normalize(fontApplyMode);
        this.typefaceId = normalizeNullableString(typefaceId);
        this.fontHookDomainsRaw = normalizeNullableString(fontHookDomainsRaw);
    }

    boolean hasAnyValue() {
        return viewportTargetSpec.isEnabled()
                || ViewportApplyMode.isEnabled(viewportApplyMode)
                || fontScalePercent != null
                || FontApplyMode.isEnabled(fontApplyMode)
                || typefaceId != null
                || fontHookDomainsRaw != null;
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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TemplateConfigValue other)) {
            return false;
        }
        return viewportTargetSpec.equals(other.viewportTargetSpec)
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
                viewportApplyMode,
                fontScalePercent,
                fontApplyMode,
                typefaceId,
                fontHookDomainsRaw);
    }
}
