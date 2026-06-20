package com.dpis.module;

import java.util.Objects;

final class PackageConfigValue {
    static final PackageConfigValue EMPTY = new PackageConfigValue(
            ViewportTargetSpec.off(),
            ViewportTargetType.OFF,
            ViewportApplyMode.OFF,
            null,
            FontApplyMode.OFF,
            null,
            null,
            null,
            null);

    final ViewportTargetSpec viewportTargetSpec;
    final String viewportTargetType;
    final String viewportApplyMode;
    final Integer fontScalePercent;
    final String fontApplyMode;
    final String typefaceId;
    final String fontHookDomainsRaw;
    final Boolean dpisEnabled;
    final Integer wechatDpi;

    PackageConfigValue(
            ViewportTargetSpec viewportTargetSpec,
            String viewportTargetType,
            String viewportApplyMode,
            Integer fontScalePercent,
            String fontApplyMode,
            String typefaceId,
            String fontHookDomainsRaw,
            Boolean dpisEnabled,
            Integer wechatDpi) {
        this.viewportTargetSpec = viewportTargetSpec != null
                ? viewportTargetSpec
                : ViewportTargetSpec.off();
        this.viewportTargetType = this.viewportTargetSpec.isEnabled()
                ? this.viewportTargetSpec.type()
                : ViewportTargetType.normalize(viewportTargetType);
        this.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode);
        this.fontScalePercent = normalizeFontScalePercent(fontScalePercent);
        this.fontApplyMode = FontApplyMode.normalize(fontApplyMode);
        this.typefaceId = normalizeNullableString(typefaceId);
        this.fontHookDomainsRaw = normalizeNullableString(fontHookDomainsRaw);
        this.dpisEnabled = normalizeDpisEnabled(dpisEnabled);
        this.wechatDpi = WechatDpiConfig.normalize(wechatDpi);
    }

    boolean hasAnyValue() {
        return viewportTargetSpec.isEnabled()
                || !ViewportTargetType.OFF.equals(viewportTargetType)
                || isStoredViewportApplyMode(viewportApplyMode)
                || fontScalePercent != null
                || isStoredFontApplyMode(fontApplyMode)
                || typefaceId != null
                || fontHookDomainsRaw != null
                || dpisEnabled != null
                || wechatDpi != null;
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

    private static Boolean normalizeDpisEnabled(Boolean enabled) {
        if (enabled == null || enabled) {
            return null;
        }
        return Boolean.FALSE;
    }

    private static boolean isStoredViewportApplyMode(String mode) {
        return ViewportApplyMode.SYSTEM.equals(mode)
                || ViewportApplyMode.COMPAT.equals(mode);
    }

    private static boolean isStoredFontApplyMode(String mode) {
        return FontApplyMode.FIELD_REWRITE.equals(mode);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PackageConfigValue other)) {
            return false;
        }
        return viewportTargetSpec.equals(other.viewportTargetSpec)
                && viewportTargetType.equals(other.viewportTargetType)
                && viewportApplyMode.equals(other.viewportApplyMode)
                && Objects.equals(fontScalePercent, other.fontScalePercent)
                && fontApplyMode.equals(other.fontApplyMode)
                && Objects.equals(typefaceId, other.typefaceId)
                && Objects.equals(fontHookDomainsRaw, other.fontHookDomainsRaw)
                && Objects.equals(dpisEnabled, other.dpisEnabled)
                && Objects.equals(wechatDpi, other.wechatDpi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                viewportTargetSpec,
                viewportTargetType,
                viewportApplyMode,
                fontScalePercent,
                fontApplyMode,
                typefaceId,
                fontHookDomainsRaw,
                dpisEnabled,
                wechatDpi);
    }
}
