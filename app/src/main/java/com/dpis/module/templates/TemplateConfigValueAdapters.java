package com.dpis.module.templates;

import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

import com.dpis.module.*;


public final class TemplateConfigValueAdapters {
    private TemplateConfigValueAdapters() {
    }

    public static TemplateConfigValue fromViewportTargetSpec(
            ViewportTargetSpec viewportTargetSpec,
            String viewportApplyMode,
            Integer fontScalePercent,
            String fontApplyMode,
            String typefaceId,
            String fontHookDomainsRaw) {
        return fromViewportTargetSpec(
                viewportTargetSpec,
                null,
                null,
                null,
                viewportApplyMode,
                fontScalePercent,
                fontApplyMode,
                typefaceId,
                fontHookDomainsRaw);
    }

    public static TemplateConfigValue fromViewportTargetSpec(
            ViewportTargetSpec viewportTargetSpec,
            String viewportTargetType,
            String viewportApplyMode,
            Integer fontScalePercent,
            String fontApplyMode,
            String typefaceId,
            String fontHookDomainsRaw) {
        return fromViewportTargetSpec(
                viewportTargetSpec,
                viewportTargetType,
                null,
                null,
                viewportApplyMode,
                fontScalePercent,
                fontApplyMode,
                typefaceId,
                fontHookDomainsRaw);
    }

    public static TemplateConfigValue fromViewportTargetSpec(
            ViewportTargetSpec viewportTargetSpec,
            String viewportTargetType,
            Integer viewportScaleMilliPercentDraft,
            Integer viewportWidthDpDraft,
            String viewportApplyMode,
            Integer fontScalePercent,
            String fontApplyMode,
            String typefaceId,
            String fontHookDomainsRaw) {
        ViewportTargetSpec spec = viewportTargetSpec != null
                ? viewportTargetSpec
                : ViewportTargetSpec.off();
        String targetType = spec.isEnabled()
                ? spec.type()
                : TemplateConfigValue.normalizeViewportTargetType(viewportTargetType);
        Integer scaleMilliPercent = spec.isRelativeScale()
                ? spec.scaleMilliPercent()
                : null;
        Integer widthDp = spec.isAbsoluteDp()
                ? spec.absoluteWidthDp()
                : null;
        return new TemplateConfigValue(
                targetType,
                scaleMilliPercent,
                widthDp,
                viewportScaleMilliPercentDraft,
                viewportWidthDpDraft,
                viewportApplyMode,
                fontScalePercent,
                fontApplyMode,
                typefaceId,
                fontHookDomainsRaw);
    }

    public static ViewportTargetSpec toViewportTargetSpec(TemplateConfigValue value) {
        TemplateConfigValue normalized = value != null ? value : TemplateConfigValue.EMPTY;
        if (normalized.isRelativeScaleViewport()) {
            return ViewportTargetSpec.relativeScale(normalized.viewportScaleMilliPercent);
        }
        if (normalized.isAbsoluteDpViewport()) {
            return ViewportTargetSpec.absoluteDp(normalized.viewportWidthDp);
        }
        return ViewportTargetSpec.off();
    }

    public static PackageConfigValue toPackageConfigValue(TemplateConfigValue value) {
        TemplateConfigValue normalized = value != null ? value : TemplateConfigValue.EMPTY;
        return new PackageConfigValue(
                toViewportTargetSpec(normalized),
                normalized.viewportTargetType,
                normalized.viewportApplyMode,
                normalized.fontScalePercent,
                normalized.fontApplyMode,
                normalized.typefaceId,
                normalized.fontHookDomainsRaw,
                null,
                null);
    }
}
