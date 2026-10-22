package com.dpis.module.config

import android.content.SharedPreferences
import android.os.Process
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainPropertyBridge
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.runtime.delivery.RuntimeDebugPropertyBridge
import com.dpis.module.runtime.font.HyperOsFlutterFontBridge
import com.dpis.module.runtime.systemserver.selection.PackageUidResolver
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportPropertyBridge
import com.dpis.module.viewport.ViewportTargetSpec

class RuntimePropertyConfigPreferences @JvmOverloads constructor(
    private val packageName: String,
    route: AutoViewportRuntimeRoute = AutoViewportRuntimeRoute.NONE,
    private val userId: Int? = null,
    private val propertyReader: (String) -> String = ::readSystemProperty,
    private val clock: () -> Long = System::currentTimeMillis,
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
        val now = clock()
        if (cached != null && now - cachedAtMillis < SNAPSHOT_TTL_MILLIS) return cached

        val values = LinkedHashMap<String, Any>()
        val targetUserId = userId ?: runCatching {
            PackageUidResolver.userIdFromUid(Process.myUid())
        }.getOrNull()
        if (targetUserId == null || targetUserId < 0) return emptyMap<String, Any>()
        val targetSpec = ViewportPropertyBridge.readTargetSpec(packageName, targetUserId)
        var widthDp: Int? = targetSpec.absoluteWidthDp()
        var viewportMode = ViewportPropertyBridge.readCompatMode(packageName, targetUserId)
        if (widthDp == null || widthDp <= 0 || !ViewportApplyMode.isEnabled(viewportMode)) {
            widthDp = ViewportPropertyBridge.readTargetWidthDp(packageName, targetUserId)
            if (targetSpec.isRelativeScale() && ViewportApplyMode.isEnabled(viewportMode)) {
                widthDp = null
            } else {
                viewportMode = ViewportApplyMode.SYSTEM
            }
        }
        viewportMode = resolveRuntimeViewportMode(viewportMode, targetSpec, autoViewportRuntimeRoute)

        var fontScalePercent = HyperOsFlutterFontBridge.readCompatFontScalePercent(
            packageName,
            targetUserId,
            propertyReader
        )
        var forceFontScalePercent: Int? = null
        if (fontScalePercent == null || fontScalePercent <= 0) {
            forceFontScalePercent = HyperOsFlutterFontBridge.readForceFontScalePercent(
                packageName,
                targetUserId,
                propertyReader
            )
            fontScalePercent = forceFontScalePercent
        }
        val fontMode = resolveRuntimeFontMode(
            fontScalePercent,
            HyperOsFlutterFontBridge.readCompatFontMode(packageName, targetUserId, propertyReader),
            forceFontScalePercent,
        )
        val typefaceId =
            HyperOsFlutterFontBridge.readTypefaceId(packageName, targetUserId, propertyReader)
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
            RuntimeDebugPropertyBridge.readGlobalLogEnabled(propertyReader)
        values[ConfigPreferenceKeys.FONT_DEBUG_OVERLAY_ENABLED] =
            RuntimeDebugPropertyBridge.readFontDebugOverlayEnabled(propertyReader)
        if (hasPackageRuntimeConfig) {
            val override =
                FontHookDomainPropertyBridge.readOverride(packageName, targetUserId, propertyReader)
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

        private fun readSystemProperty(key: String): String = try {
            Class.forName("android.os.SystemProperties")
                .getDeclaredMethod("get", String::class.java, String::class.java)
                .invoke(null, key, "") as? String ?: ""
        } catch (_: Throwable) {
            ""
        }

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
            // Relative-scale AUTO must remain AUTO so EffectiveModeResolver can
            // select the system-owned route when system_server hooks are active.
            // Rewriting it to COMPAT here makes the app process own the same
            // configuration that system_server already transformed, causing
            // recursive scaling (360 -> 432 -> 518). Absolute-width AUTO keeps
            // the established app-process compatibility route.
            return if (mode == ViewportApplyMode.AUTO &&
                route.shouldUseAppProcessRoute(targetSpec) &&
                targetSpec?.isAbsoluteDp() == true
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
