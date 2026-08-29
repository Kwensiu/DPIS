package com.dpis.module.config

import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType

class PackageConfigValue(
    viewportTargetSpec: ViewportTargetSpec?,
    viewportTargetType: String?,
    viewportApplyMode: String?,
    fontScalePercent: Int?,
    fontApplyMode: String?,
    typefaceId: String?,
    fontHookDomainsRaw: String?,
    dpisEnabled: Boolean?,
    wechatDpi: Int?,
) {
    @JvmField val viewportTargetSpec: ViewportTargetSpec =
        viewportTargetSpec ?: ViewportTargetSpec.off()
    @JvmField val fontScalePercent: Int? = fontScalePercent?.takeIf { it in 50..300 }
    @JvmField val typefaceId: String? = typefaceId?.trim()?.takeIf(String::isNotEmpty)
    @JvmField val fontHookDomainsRaw: String? = fontHookDomainsRaw?.trim()
    @JvmField val dpisEnabled: Boolean? = dpisEnabled?.takeIf { !it }
    @JvmField val wechatDpi: Int? = WechatDpiConfig.normalize(wechatDpi)
    @JvmField val viewportTargetType: String =
        if (this.viewportTargetSpec.isEnabled()) this.viewportTargetSpec.type()
        else ViewportTargetType.normalize(viewportTargetType)
    @JvmField val viewportApplyMode: String = ViewportApplyMode.normalize(viewportApplyMode)
    @JvmField val fontApplyMode: String = FontApplyMode.normalize(fontApplyMode)

    fun viewportTargetSpec(): ViewportTargetSpec = viewportTargetSpec
    fun viewportTargetType(): String = viewportTargetType
    fun viewportApplyMode(): String = viewportApplyMode
    fun fontScalePercent(): Int? = fontScalePercent
    fun fontApplyMode(): String = fontApplyMode
    fun typefaceId(): String? = typefaceId
    fun fontHookDomainsRaw(): String? = fontHookDomainsRaw
    fun dpisEnabled(): Boolean? = dpisEnabled
    fun wechatDpi(): Int? = wechatDpi

    fun hasAnyValue(): Boolean =
        viewportTargetSpec().isEnabled() ||
            viewportTargetType != ViewportTargetType.OFF ||
            viewportApplyMode == ViewportApplyMode.SYSTEM ||
            viewportApplyMode == ViewportApplyMode.COMPAT ||
            fontScalePercent != null ||
            fontApplyMode == FontApplyMode.FIELD_REWRITE ||
            typefaceId() != null ||
            fontHookDomainsRaw() != null ||
            dpisEnabled() != null ||
            wechatDpi() != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PackageConfigValue) return false
        return viewportTargetSpec() == other.viewportTargetSpec() &&
            viewportTargetType == other.viewportTargetType &&
            viewportApplyMode == other.viewportApplyMode &&
            fontScalePercent == other.fontScalePercent &&
            fontApplyMode == other.fontApplyMode &&
            typefaceId() == other.typefaceId() &&
            fontHookDomainsRaw() == other.fontHookDomainsRaw() &&
            dpisEnabled() == other.dpisEnabled() &&
            wechatDpi() == other.wechatDpi()
    }

    override fun hashCode(): Int = arrayOf(
        viewportTargetSpec(), viewportTargetType, viewportApplyMode, fontScalePercent,
        fontApplyMode, typefaceId(), fontHookDomainsRaw(), dpisEnabled(), wechatDpi(),
    ).contentHashCode()

    companion object {
        @JvmField val EMPTY = PackageConfigValue(
            ViewportTargetSpec.off(),
            ViewportTargetType.OFF,
            ViewportApplyMode.OFF,
            null,
            FontApplyMode.OFF,
            null,
            null,
            null,
            null,
        )
    }
}
