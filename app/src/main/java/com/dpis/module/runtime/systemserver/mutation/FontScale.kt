package com.dpis.module.runtime.systemserver.mutation

import android.content.res.Configuration
import com.dpis.module.config.PerAppDisplayConfig
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.runtime.probe.DebugPackageOverride
import com.dpis.module.runtime.systemserver.selection.ConfigSelection
import kotlin.math.abs

/**
 * Configuration.fontScale written from system_server. Debug properties can skip
 * one package, or yield that package to the app-process font route.
 */
object FontScale {
    const val DISABLE_PACKAGE_PROPERTY = "debug.dpis.font.disable_system_server_package"
    const val FALLBACK_PACKAGE_PROPERTY = "debug.dpis.font.system_server_fallback_package"

    @JvmStatic
    fun apply(configuration: Configuration?, config: PerAppDisplayConfig?): Boolean {
        if (configuration == null ||
            config == null ||
            !ConfigSelection.hasSystemServerFontOverride(config)
        ) {
            return false
        }
        if (isDisabled(config.packageName)) {
            DpisLog.i(
                "DPIS_FONT SystemServer config fontScale skipped: package=" +
                        config.packageName + ", reason=debug-disable-system-server-font",
            )
            return false
        }
        if (shouldYield(config)) {
            DpisLog.i(
                "DPIS_FONT SystemServer config fontScale skipped: package=" +
                        config.packageName + ", reason=debug-system-server-font-fallback-yield",
            )
            return false
        }
        val percent = config.targetFontScalePercent ?: return false
        val fontScale = percent / 100.0f
        if (abs(configuration.fontScale - fontScale) < 0.0001f) {
            return false
        }
        configuration.fontScale = fontScale
        return true
    }

    @JvmStatic
    fun report(packageName: String?, beforeFontScale: Float, afterFontScale: Float) {
        if (packageName.isNullOrEmpty()) {
            return
        }
        DpisLog.i(
            "DPIS_FONT SystemServer config fontScale: package=" + packageName +
                    ", fontScale=" + beforeFontScale + "->" + afterFontScale,
        )
    }

    @JvmStatic
    fun isDisabled(packageName: String?): Boolean =
        DebugPackageOverride.matches(DISABLE_PACKAGE_PROPERTY, packageName)

    @JvmStatic
    fun isDisabledForTest(packageName: String?, propertyValue: String?): Boolean =
        DebugPackageOverride.matchesForTest(DISABLE_PACKAGE_PROPERTY, packageName, propertyValue)

    @JvmStatic
    fun shouldYield(config: PerAppDisplayConfig?): Boolean =
        isFallbackEnabled(config?.packageName) && hasAppProcessRoute(config)

    @JvmStatic
    fun shouldYieldForTest(config: PerAppDisplayConfig?, propertyValue: String?): Boolean =
        DebugPackageOverride.matchesForTest(
            FALLBACK_PACKAGE_PROPERTY, config?.packageName, propertyValue,
        ) && hasAppProcessRoute(config)

    private fun isFallbackEnabled(packageName: String?): Boolean =
        DebugPackageOverride.matches(FALLBACK_PACKAGE_PROPERTY, packageName)

    private fun hasAppProcessRoute(config: PerAppDisplayConfig?): Boolean {
        val percent = config?.targetFontScalePercent
        return config != null &&
                FontApplyMode.SYSTEM_EMULATION == config.targetFontMode &&
                percent != null &&
                percent > 0
    }
}
