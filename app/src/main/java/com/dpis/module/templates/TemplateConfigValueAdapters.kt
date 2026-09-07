package com.dpis.module.templates

import com.dpis.module.config.PackageConfigValue
import com.dpis.module.viewport.ViewportTargetSpec

object TemplateConfigValueAdapters {
    @JvmStatic
    fun fromViewportTargetSpec(
        viewportTargetSpec: ViewportTargetSpec?,
        viewportApplyMode: String?,
        fontScalePercent: Int?,
        fontApplyMode: String?,
        typefaceId: String?,
        fontHookDomainsRaw: String?
    ): TemplateConfigValue {
        return fromViewportTargetSpec(
            viewportTargetSpec,
            null,
            null,
            null,
            viewportApplyMode,
            fontScalePercent,
            fontApplyMode,
            typefaceId,
            fontHookDomainsRaw
        )
    }

    @JvmStatic
    fun fromViewportTargetSpec(
        viewportTargetSpec: ViewportTargetSpec?,
        viewportTargetType: String?,
        viewportApplyMode: String?,
        fontScalePercent: Int?,
        fontApplyMode: String?,
        typefaceId: String?,
        fontHookDomainsRaw: String?
    ): TemplateConfigValue {
        return fromViewportTargetSpec(
            viewportTargetSpec,
            viewportTargetType,
            null,
            null,
            viewportApplyMode,
            fontScalePercent,
            fontApplyMode,
            typefaceId,
            fontHookDomainsRaw
        )
    }

    @JvmStatic
    fun fromViewportTargetSpec(
        viewportTargetSpec: ViewportTargetSpec?,
        viewportTargetType: String?,
        viewportScaleMilliPercentDraft: Int?,
        viewportWidthDpDraft: Int?,
        viewportApplyMode: String?,
        fontScalePercent: Int?,
        fontApplyMode: String?,
        typefaceId: String?,
        fontHookDomainsRaw: String?
    ): TemplateConfigValue {
        val spec = if (viewportTargetSpec != null)
            viewportTargetSpec
        else
            ViewportTargetSpec.off()
        val targetType = if (spec.isEnabled)
            spec.type()
        else
            TemplateConfigValue.normalizeViewportTargetType(viewportTargetType)
        val scaleMilliPercent = if (spec.isRelativeScale)
            spec.scaleMilliPercent()
        else
            null
        val widthDp = if (spec.isAbsoluteDp)
            spec.absoluteWidthDp()
        else
            null
        return TemplateConfigValue(
            targetType,
            scaleMilliPercent,
            widthDp,
            viewportScaleMilliPercentDraft,
            viewportWidthDpDraft,
            viewportApplyMode,
            fontScalePercent,
            fontApplyMode,
            typefaceId,
            fontHookDomainsRaw
        )
    }

    @JvmStatic
    fun toViewportTargetSpec(value: TemplateConfigValue?): ViewportTargetSpec {
        val normalized = if (value != null) value else TemplateConfigValue.EMPTY
        if (normalized == null) {
            return ViewportTargetSpec.off()
        }
        if (normalized.isRelativeScaleViewport) {
            return ViewportTargetSpec.relativeScale(normalized.viewportScaleMilliPercent)
        }
        if (normalized.isAbsoluteDpViewport) {
            return ViewportTargetSpec.absoluteDp(normalized.viewportWidthDp)
        }
        return ViewportTargetSpec.off()
    }

    @JvmStatic
    fun toPackageConfigValue(value: TemplateConfigValue?): PackageConfigValue {
        val normalized = if (value != null) value else TemplateConfigValue.EMPTY
        return PackageConfigValue(
            toViewportTargetSpec(normalized),
            normalized.viewportTargetType,
            normalized.viewportApplyMode,
            normalized.fontScalePercent,
            normalized.fontApplyMode,
            normalized.typefaceId,
            normalized.fontHookDomainsRaw,
            null,
            null
        )
    }
}
