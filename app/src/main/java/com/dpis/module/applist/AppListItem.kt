package com.dpis.module.applist

import android.graphics.drawable.Drawable
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.templates.TemplateConfigValue
import com.dpis.module.templates.TemplateConfigValueAdapters
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType

class AppListItem private constructor(
    label: String?,
    packageName: String?,
    @JvmField val inScope: Boolean,
    @JvmField val scopeKnown: Boolean,
    viewportWidthDp: Int?,
    viewportScaleMilliPercent: Int?,
    viewportMode: String?,
    viewportTargetType: String?,
    viewportTargetSpec: ViewportTargetSpec?,
    @JvmField val fontScalePercent: Int?,
    fontMode: String?,
    @JvmField val typefaceId: String?,
    @JvmField val appSpecificConfigActive: Boolean,
    @JvmField val wechatDpi: Int?,
    @JvmField val dpisEnabled: Boolean,
    @JvmField val configured: Boolean,
    @JvmField val installed: Boolean,
    @JvmField val systemApp: Boolean,
    @JvmField val hyperOsNativeProxyCandidate: Boolean,
    @JvmField val previewFromGlobalPrefill: Boolean,
    fontHookDomainsRaw: String?,
    previewFontHookDomainsRaw: String?,
    @JvmField val icon: Drawable?,
) {
    @JvmField val label: String = label ?: ""
    @JvmField val packageName: String = packageName ?: ""

    @JvmField
    val viewportTargetSpec: ViewportTargetSpec = viewportTargetSpec
        ?: viewportWidthDp?.let(ViewportTargetSpec::absoluteDp)
        ?: ViewportTargetSpec.off()

    @JvmField
    val viewportTargetType: String = if (this.viewportTargetSpec.isEnabled()) {
        this.viewportTargetSpec.type()
    } else {
        ViewportTargetType.normalize(viewportTargetType)
    }

    @JvmField
    val viewportWidthDp: Int? = if (this.viewportTargetSpec.isAbsoluteDp()) {
        this.viewportTargetSpec.absoluteWidthDp()
    } else {
        viewportWidthDp
    }

    @JvmField
    val viewportScaleMilliPercent: Int? = if (this.viewportTargetSpec.isRelativeScale()) {
        this.viewportTargetSpec.scaleMilliPercent()
    } else {
        viewportScaleMilliPercent
    }

    @JvmField
    val viewportMode: String = ViewportApplyMode.normalize(viewportMode)

    @JvmField
    val fontMode: String = FontApplyMode.normalize(fontMode)

    @JvmField
    val fontHookDomainsRaw: String? = fontHookDomainsRaw?.trim()

    @JvmField
    val previewFontHookDomainsRaw: String? = previewFontHookDomainsRaw?.trim()

    /** PackageManager timestamps used only by the app-list ordering contract. */
    @JvmField var firstInstallTime: Long = 0L
    @JvmField var lastUpdateTime: Long = 0L

    constructor(
        label: String?, packageName: String?, inScope: Boolean, scopeKnown: Boolean,
        viewportWidthDp: Int?, viewportMode: String?, fontScalePercent: Int?, fontMode: String?,
        typefaceId: String?, dpisEnabled: Boolean, systemApp: Boolean,
        hyperOsNativeProxyCandidate: Boolean, icon: Drawable?,
    ) : this(
        label, packageName, inScope, scopeKnown, viewportWidthDp, null, viewportMode, null,
        viewportWidthDp?.let(ViewportTargetSpec::absoluteDp) ?: ViewportTargetSpec.off(),
        fontScalePercent, fontMode, typefaceId, false, null, dpisEnabled, false, true,
        systemApp, hyperOsNativeProxyCandidate, false, null, null, icon,
    )

    constructor(
        label: String?, packageName: String?, inScope: Boolean, scopeKnown: Boolean,
        viewportWidthDp: Int?, viewportMode: String?, fontScalePercent: Int?, fontMode: String?,
        dpisEnabled: Boolean, systemApp: Boolean, hyperOsNativeProxyCandidate: Boolean,
        icon: Drawable?,
    ) : this(
        label, packageName, inScope, scopeKnown, viewportWidthDp, null, viewportMode, null,
        viewportWidthDp?.let(ViewportTargetSpec::absoluteDp) ?: ViewportTargetSpec.off(),
        fontScalePercent, fontMode, null, false, null, dpisEnabled, false, true,
        systemApp, hyperOsNativeProxyCandidate, false, null, null, icon,
    )

    constructor(
        label: String?, packageName: String?, inScope: Boolean, scopeKnown: Boolean,
        viewportWidthDp: Int?, viewportScaleMilliPercent: Int?, viewportMode: String?,
        viewportTargetType: String?, viewportTargetSpec: ViewportTargetSpec?,
        fontScalePercent: Int?, fontMode: String?, typefaceId: String?,
        appSpecificConfigActive: Boolean, dpisEnabled: Boolean, systemApp: Boolean,
        hyperOsNativeProxyCandidate: Boolean, icon: Drawable?,
    ) : this(
        label, packageName, inScope, scopeKnown, viewportWidthDp, viewportScaleMilliPercent,
        viewportMode, viewportTargetType, viewportTargetSpec, fontScalePercent, fontMode,
        typefaceId, appSpecificConfigActive, null, dpisEnabled, false, true, systemApp,
        hyperOsNativeProxyCandidate, false, null, null, icon,
    )

    constructor(
        label: String?, packageName: String?, inScope: Boolean, scopeKnown: Boolean,
        viewportWidthDp: Int?, viewportMode: String?, viewportTargetSpec: ViewportTargetSpec?,
        fontScalePercent: Int?, fontMode: String?, typefaceId: String?,
        appSpecificConfigActive: Boolean, dpisEnabled: Boolean, systemApp: Boolean,
        hyperOsNativeProxyCandidate: Boolean, icon: Drawable?,
    ) : this(
        label, packageName, inScope, scopeKnown, viewportWidthDp, null, viewportMode, null,
        viewportTargetSpec, fontScalePercent, fontMode, typefaceId, appSpecificConfigActive,
        null, dpisEnabled, false, true, systemApp, hyperOsNativeProxyCandidate, false, null,
        null, icon,
    )

    constructor(
        label: String?, packageName: String?, inScope: Boolean, scopeKnown: Boolean,
        viewportWidthDp: Int?, viewportScaleMilliPercent: Int?, viewportMode: String?,
        viewportTargetType: String?, viewportTargetSpec: ViewportTargetSpec?,
        fontScalePercent: Int?, fontMode: String?, typefaceId: String?,
        appSpecificConfigActive: Boolean, dpisEnabled: Boolean, configured: Boolean,
        installed: Boolean, systemApp: Boolean, hyperOsNativeProxyCandidate: Boolean,
        icon: Drawable?,
    ) : this(
        label, packageName, inScope, scopeKnown, viewportWidthDp, viewportScaleMilliPercent,
        viewportMode, viewportTargetType, viewportTargetSpec, fontScalePercent, fontMode,
        typefaceId, appSpecificConfigActive, null, dpisEnabled, configured, installed,
        systemApp, hyperOsNativeProxyCandidate, false, null, null, icon,
    )

    constructor(
        label: String?, packageName: String?, inScope: Boolean, scopeKnown: Boolean,
        viewportWidthDp: Int?, viewportScaleMilliPercent: Int?, viewportMode: String?,
        viewportTargetType: String?, viewportTargetSpec: ViewportTargetSpec?,
        fontScalePercent: Int?, fontMode: String?, typefaceId: String?,
        appSpecificConfigActive: Boolean, wechatDpi: Int?, dpisEnabled: Boolean,
        configured: Boolean, installed: Boolean, systemApp: Boolean,
        hyperOsNativeProxyCandidate: Boolean, icon: Drawable?,
    ) : this(
        label, packageName, inScope, scopeKnown, viewportWidthDp, viewportScaleMilliPercent,
        viewportMode, viewportTargetType, viewportTargetSpec, fontScalePercent, fontMode,
        typefaceId, appSpecificConfigActive, wechatDpi, dpisEnabled, configured, installed,
        systemApp, hyperOsNativeProxyCandidate, false, null, null, icon,
    )

    constructor(
        label: String?, packageName: String?, inScope: Boolean, scopeKnown: Boolean,
        viewportWidthDp: Int?, viewportScaleMilliPercent: Int?, viewportMode: String?,
        viewportTargetType: String?, viewportTargetSpec: ViewportTargetSpec?,
        fontScalePercent: Int?, fontMode: String?, typefaceId: String?,
        appSpecificConfigActive: Boolean, wechatDpi: Int?, dpisEnabled: Boolean,
        configured: Boolean, installed: Boolean, systemApp: Boolean,
        hyperOsNativeProxyCandidate: Boolean, fontHookDomainsRaw: String?, icon: Drawable?,
    ) : this(
        label, packageName, inScope, scopeKnown, viewportWidthDp, viewportScaleMilliPercent,
        viewportMode, viewportTargetType, viewportTargetSpec, fontScalePercent, fontMode,
        typefaceId, appSpecificConfigActive, wechatDpi, dpisEnabled, configured, installed,
        systemApp, hyperOsNativeProxyCandidate, false, fontHookDomainsRaw, null, icon,
    )

    fun effectiveFontHookDomainsRaw(): String? =
        if (previewFromGlobalPrefill) previewFontHookDomainsRaw else fontHookDomainsRaw

    fun hasAppSpecificConfig(): Boolean = appSpecificConfigActive

    /** True when this row has DPIS-owned package configuration that reset can remove. */
    fun hasDpisPackageConfig(): Boolean =
        appSpecificConfigActive || viewportTargetSpec.isEnabled()
            || fontScalePercent != null || typefaceId != null
            || !fontHookDomainsRaw.isNullOrBlank() || wechatDpi != null || !dpisEnabled

    /** Refreshes the persisted enable switch without rebuilding the catalog snapshot. */
    fun withDpisEnabled(enabled: Boolean): AppListItem =
        if (dpisEnabled == enabled) this else copyWith(dpisEnabled = enabled)

    /** Returns this snapshot with only its asynchronously loaded icon replaced. */
    fun withIcon(updatedIcon: Drawable?): AppListItem =
        if (icon === updatedIcon) this else copyWith(icon = updatedIcon)

    fun withGlobalPrefillPreview(prefill: TemplateConfigValue?): AppListItem {
        val normalized = prefill ?: TemplateConfigValue.EMPTY
        val targetSpec = TemplateConfigValueAdapters.toViewportTargetSpec(normalized)
        return copyWith(
            viewportWidthDp = if (targetSpec.isAbsoluteDp()) targetSpec.absoluteWidthDp()
            else normalized.viewportWidthDpDraft,
            viewportScaleMilliPercent = if (targetSpec.isRelativeScale()) targetSpec.scaleMilliPercent()
            else normalized.viewportScaleMilliPercentDraft,
            viewportMode = normalized.viewportApplyMode,
            viewportTargetType = normalized.viewportTargetType,
            viewportTargetSpec = targetSpec,
            fontScalePercent = normalized.fontScalePercent,
            fontMode = normalized.fontApplyMode,
            typefaceId = normalized.typefaceId,
            // A global prefill is an editor baseline, never persisted per-app configuration.
            previewFromGlobalPrefill = true,
            previewFontHookDomainsRaw = normalized.fontHookDomainsRaw,
        )
    }

    fun withWechatDpi(updatedWechatDpi: Int?): AppListItem =
        if (wechatDpi == updatedWechatDpi) this else copyWith(wechatDpi = updatedWechatDpi)

    private fun copyWith(
        label: String? = this.label, packageName: String? = this.packageName,
        inScope: Boolean = this.inScope, scopeKnown: Boolean = this.scopeKnown,
        viewportWidthDp: Int? = this.viewportWidthDp,
        viewportScaleMilliPercent: Int? = this.viewportScaleMilliPercent,
        viewportMode: String? = this.viewportMode,
        viewportTargetType: String? = this.viewportTargetType,
        viewportTargetSpec: ViewportTargetSpec? = this.viewportTargetSpec,
        fontScalePercent: Int? = this.fontScalePercent, fontMode: String? = this.fontMode,
        typefaceId: String? = this.typefaceId,
        appSpecificConfigActive: Boolean = this.appSpecificConfigActive,
        wechatDpi: Int? = this.wechatDpi, dpisEnabled: Boolean = this.dpisEnabled,
        configured: Boolean = this.configured, installed: Boolean = this.installed,
        systemApp: Boolean = this.systemApp,
        hyperOsNativeProxyCandidate: Boolean = this.hyperOsNativeProxyCandidate,
        previewFromGlobalPrefill: Boolean = this.previewFromGlobalPrefill,
        fontHookDomainsRaw: String? = this.fontHookDomainsRaw,
        previewFontHookDomainsRaw: String? = this.previewFontHookDomainsRaw,
        icon: Drawable? = this.icon,
    ): AppListItem {
        val updated = AppListItem(
            label, packageName, inScope, scopeKnown, viewportWidthDp, viewportScaleMilliPercent,
            viewportMode, viewportTargetType, viewportTargetSpec, fontScalePercent, fontMode,
            typefaceId, appSpecificConfigActive, wechatDpi, dpisEnabled, configured, installed,
            systemApp, hyperOsNativeProxyCandidate, previewFromGlobalPrefill, fontHookDomainsRaw,
            previewFontHookDomainsRaw, icon,
        )
        updated.firstInstallTime = firstInstallTime
        updated.lastUpdateTime = lastUpdateTime
        return updated
    }
}
