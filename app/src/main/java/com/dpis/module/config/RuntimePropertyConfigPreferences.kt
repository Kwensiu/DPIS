package com.dpis.module.config

import android.content.SharedPreferences
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainPropertyBridge
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.runtime.RuntimeDebugPropertyBridge
import com.dpis.module.runtime.font.HyperOsFlutterFontBridge
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportPropertyBridge
import com.dpis.module.viewport.ViewportTargetSpec

class RuntimePropertyConfigPreferences @JvmOverloads constructor(
    private val packageName: String,
    route: AutoViewportRuntimeRoute = AutoViewportRuntimeRoute.NONE,
) : SharedPreferences {
    private val autoViewportRuntimeRoute = route
    @Volatile private var cachedSnapshot: Map<String, Any>? = null
    @Volatile private var cachedAtMillis = 0L

    internal constructor(packageName: String, resolveAutoViewportAsAppProcessRoute: Boolean) :
        this(
            packageName,
            if (resolveAutoViewportAsAppProcessRoute) {
                AutoViewportRuntimeRoute.ANY_ENABLED_TARGET
            } else {
                AutoViewportRuntimeRoute.NONE
            },
        )

    override fun getAll(): Map<String, *> {
        val cached = cachedSnapshot
        val now = System.currentTimeMillis()
        if (cached != null && now - cachedAtMillis < SNAPSHOT_TTL_MILLIS) return cached

        val values = LinkedHashMap<String, Any>()
        val targetSpec = ViewportPropertyBridge.readTargetSpec(packageName)
        var widthDp: Int? = targetSpec.absoluteWidthDp()
        var viewportMode = ViewportPropertyBridge.readCompatMode(packageName)
        if (widthDp == null || widthDp <= 0 || !ViewportApplyMode.isEnabled(viewportMode)) {
            widthDp = ViewportPropertyBridge.readTargetWidthDp(packageName)
            if (targetSpec.isRelativeScale() && ViewportApplyMode.isEnabled(viewportMode)) {
                widthDp = null
            } else {
                viewportMode = ViewportApplyMode.SYSTEM
            }
        }
        viewportMode = resolveRuntimeViewportMode(viewportMode, targetSpec, autoViewportRuntimeRoute)

        var fontScalePercent = HyperOsFlutterFontBridge.readCompatFontScalePercent(packageName)
        var forceFontScalePercent: Int? = null
        if (fontScalePercent == null || fontScalePercent <= 0) {
            forceFontScalePercent = HyperOsFlutterFontBridge.readForceFontScalePercent(packageName)
            fontScalePercent = forceFontScalePercent
        }
        val fontMode = resolveRuntimeFontMode(
            fontScalePercent,
            HyperOsFlutterFontBridge.readCompatFontMode(packageName),
            forceFontScalePercent,
        )
        val typefaceId = HyperOsFlutterFontBridge.readTypefaceId(packageName)
        if (targetSpec.isEnabled() && ViewportApplyMode.isEnabled(viewportMode)) {
            values[viewportTargetTypeKey()] = targetSpec.type()
            if (targetSpec.isRelativeScale()) values[viewportScaleMilliPercentKey()] = targetSpec.scaleMilliPercent()
            if (widthDp != null && widthDp > 0) values[viewportWidthKey()] = widthDp
            values[viewportModeKey()] = viewportMode
        }
        if (fontScalePercent != null && fontScalePercent > 0) {
            values[fontScaleKey()] = fontScalePercent
            values[fontModeKey()] = FontApplyMode.normalize(fontMode)
        }
        if (!typefaceId.isNullOrBlank()) values[typefaceIdKey()] = typefaceId

        val hasPackageRuntimeConfig = values.isNotEmpty()
        values[ConfigPreferenceKeys.GLOBAL_LOG_ENABLED] =
            RuntimeDebugPropertyBridge.readGlobalLogEnabled()
        values[ConfigPreferenceKeys.FONT_DEBUG_OVERLAY_ENABLED] =
            RuntimeDebugPropertyBridge.readFontDebugOverlayEnabled()
        if (hasPackageRuntimeConfig) {
            val override = FontHookDomainPropertyBridge.readOverride(packageName)
            if (override.customPathEnabled) {
                values[hookDomainsKey()] = FontHookDomainRegistry.orderedCustomizableSubset(
                    override.enabledKnownDomains,
                ).joinToString(",")
            }
            values[ConfigPreferenceKeys.TARGET_PACKAGES] = linkedSetOf(packageName)
        }
        val snapshot = values.toMap()
        cachedSnapshot = snapshot
        cachedAtMillis = now
        return snapshot
    }

    override fun getString(key: String?, defValue: String?): String? =
        getAll()[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? =
        (getAll()[key] as? Set<String>)?.toSet() ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        getAll()[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long =
        getAll()[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float =
        getAll()[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        getAll()[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = getAll().containsKey(key)
    override fun edit(): SharedPreferences.Editor =
        throw UnsupportedOperationException("RuntimePropertyConfigPreferences is read-only")
    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    enum class AutoViewportRuntimeRoute {
        NONE,
        ABSOLUTE_TARGETS_ONLY,
        ANY_ENABLED_TARGET;

        fun shouldUseAppProcessRoute(targetSpec: ViewportTargetSpec?): Boolean =
            when (this) {
                NONE -> false
                ABSOLUTE_TARGETS_ONLY -> targetSpec?.isAbsoluteDp() == true
                ANY_ENABLED_TARGET -> targetSpec?.isEnabled() == true
            }
    }

    companion object {
        private const val SNAPSHOT_TTL_MILLIS = 2_000L

        @JvmStatic
        fun resolveRuntimeFontModeForTest(
            runtimeFontScalePercent: Int?,
            rawMode: String?,
            forceFontScalePercent: Int?,
        ) = resolveRuntimeFontMode(runtimeFontScalePercent, rawMode, forceFontScalePercent)

        @JvmStatic
        fun resolveRuntimeViewportModeForTest(
            rawMode: String?,
            targetSpec: ViewportTargetSpec?,
            route: AutoViewportRuntimeRoute,
        ) = resolveRuntimeViewportMode(rawMode, targetSpec, route)

        @JvmStatic
        fun resolveRuntimeViewportModeForTest(
            rawMode: String?,
            targetSpec: ViewportTargetSpec?,
            resolveAutoViewportAsAppProcessRoute: Boolean,
        ) = resolveRuntimeViewportMode(
            rawMode,
            targetSpec,
            if (resolveAutoViewportAsAppProcessRoute) {
                AutoViewportRuntimeRoute.ANY_ENABLED_TARGET
            } else {
                AutoViewportRuntimeRoute.NONE
            },
        )

        private fun resolveRuntimeViewportMode(
            rawMode: String?,
            targetSpec: ViewportTargetSpec?,
            route: AutoViewportRuntimeRoute,
        ): String {
            val mode = ViewportApplyMode.normalize(rawMode)
            return if (mode == ViewportApplyMode.AUTO &&
                route.shouldUseAppProcessRoute(targetSpec)
            ) ViewportApplyMode.COMPAT else mode
        }

        private fun resolveRuntimeFontMode(
            fontScalePercent: Int?,
            rawMode: String?,
            forceFontScalePercent: Int?,
        ): String {
            val mode = FontApplyMode.normalize(rawMode)
            if (FontApplyMode.isEnabled(mode)) return mode
            if (fontScalePercent == null || fontScalePercent <= 0) return FontApplyMode.OFF
            return if (forceFontScalePercent != null && forceFontScalePercent > 0) {
                FontApplyMode.FIELD_REWRITE
            } else {
                FontApplyMode.SYSTEM_EMULATION
            }
        }
    }

    private fun viewportWidthKey() = "viewport.$packageName.width_dp"
    private fun viewportTargetTypeKey() = "viewport.$packageName.target_type"
    private fun viewportScaleMilliPercentKey() = "viewport.$packageName.scale_milli_percent"
    private fun viewportModeKey() = "viewport.$packageName.mode"
    private fun fontScaleKey() = "font.$packageName.scale_percent"
    private fun fontModeKey() = "font.$packageName.mode"
    private fun hookDomainsKey() = "font.$packageName.hook_domains"
    private fun typefaceIdKey() = "font.$packageName.typeface_id"
}
