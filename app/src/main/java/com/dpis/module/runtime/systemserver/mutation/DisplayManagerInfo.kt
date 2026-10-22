package com.dpis.module.runtime.systemserver.mutation

import android.content.res.Configuration
import android.os.Binder
import com.dpis.module.config.PackageIdentity
import com.dpis.module.config.PerAppDisplayConfig
import com.dpis.module.config.PerAppDisplayConfigSource
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.runtime.systemserver.HookInstaller
import com.dpis.module.runtime.systemserver.diagnostics.HookLog
import com.dpis.module.runtime.systemserver.reflection.Reflection
import com.dpis.module.runtime.systemserver.selection.ConfigSelection
import com.dpis.module.runtime.systemserver.selection.PackageUidResolver
import com.dpis.module.runtime.systemserver.viewport.RelativeViewport
import com.dpis.module.viewport.PerAppDisplayEnvironment
import com.dpis.module.viewport.PerAppDisplayOverrideCalculator
import kotlin.math.roundToInt

/** DisplayManagerService info callback. The calling uid selects the configured package. */
object DisplayManagerInfo {
    @JvmStatic
    fun apply(source: PerAppDisplayConfigSource?, entryName: String?, displayInfo: Any?) {
        if (displayInfo == null || source == null) {
            logSkip(entryName, "empty-result", -1, null, displayInfo)
            return
        }
        val callingUid = Binder.getCallingUid()
        val userId = PackageUidResolver.userIdFromUid(callingUid)
        if (userId == null) {
            logSkip(entryName, "uid-not-configured", callingUid, null, displayInfo)
            return
        }
        val packageName = configuredPackage(source, callingUid, userId)
        if (packageName == null) {
            logSkip(entryName, "uid-not-configured", callingUid, null, displayInfo)
            return
        }
        val lookup = source.inspect(PackageIdentity(packageName, userId))
        if (DpisLog.isLoggingEnabled()) {
            HookLog.logSourceLookup(entryName, lookup)
        }
        val config = ConfigSelection.selectConfigForSystemServerEntry(
            entryName, lookup.config,
        )
        if (config == null) {
            logSkip(entryName, "no-viewport-config", callingUid, packageName, displayInfo)
            return
        }
        if (!ConfigSelection.hasSystemServerViewportOverride(config)) {
            logSkip(entryName, "no-viewport-mutation", callingUid, packageName, displayInfo)
            return
        }
        val environment = environment(displayInfo, config)
        if (environment == null) {
            logSkip(entryName, "env-null", callingUid, packageName, displayInfo)
            return
        }
        val beforeSummary = if (DpisLog.isLoggingEnabled()) {
            HookLog.describeDisplayInfo(displayInfo)
        } else {
            null
        }
        val applyEnvironment = RelativeViewport.resolveMarkerGatedEnvironment(
            entryName,
            packageName,
            configurationFrom(displayInfo),
            displayInfo,
            environment,
            config,
        )
        if (!EnvironmentWrite.applyDisplayInfo(displayInfo, applyEnvironment)) {
            if (DpisLog.isLoggingEnabled()) {
                HookLog.logMutationOutcome(
                    entryName,
                    packageName,
                    userId,
                    "unchanged",
                    "display_info",
                    beforeSummary,
                    HookLog.describeDisplayInfo(displayInfo),
                    HookLog.describeEnvironment(environment),
                )
            }
            return
        }
        if (DpisLog.isLoggingEnabled()) {
            val message = "system_server display-manager-info apply: package=" + packageName +
                    ", before=" + HookLog.safeToString(beforeSummary) +
                    ", after=" + HookLog.safeToString(
                HookLog.describeDisplayInfo(displayInfo),
            ) +
                    ", target=" + HookLog.describeEnvironment(environment)
            HookLog.logDiagnosticIfChanged(
                "display-manager-info|$packageName",
                message,
                HookLog.resolveLogMinIntervalMs(entryName),
                packageName,
                "mutation_applied",
            )
        }
    }

    private fun configuredPackage(
        source: PerAppDisplayConfigSource,
        callingUid: Int,
        userId: Int,
    ): String? {
        if (callingUid <= 0) return null
        var fallbackPackage: String? = null
        for (packageName in source.getConfiguredPackages()) {
            val configuredName = packageName ?: continue
            if (HookInstaller.configuredUid(configuredName, callingUid) != callingUid) {
                continue
            }
            if (ConfigSelection.selectConfigForSystemServerEntry(
                    "display-manager-info", source.get(PackageIdentity(configuredName, userId)),
                ) != null
            ) {
                return configuredName
            }
            fallbackPackage = configuredName
        }
        return fallbackPackage
    }

    private fun environment(
        displayInfo: Any,
        config: PerAppDisplayConfig,
    ): PerAppDisplayEnvironment? {
        val configuration = configurationFrom(displayInfo) ?: return null
        val widthPx = Reflection.readIntField(displayInfo, "logicalWidth")
            ?: return null
        val heightPx = Reflection.readIntField(displayInfo, "logicalHeight")
            ?: return null
        return PerAppDisplayOverrideCalculator.calculate(
            configuration,
            widthPx,
            heightPx,
            config.targetViewportSpec,
        )
    }

    private fun configurationFrom(displayInfo: Any?): Configuration? {
        val logicalWidth = Reflection.readIntField(displayInfo, "logicalWidth")
        val logicalHeight = Reflection.readIntField(displayInfo, "logicalHeight")
        val logicalDensityDpi = Reflection.readIntField(
            displayInfo, "logicalDensityDpi",
        )
        if (logicalWidth == null || logicalHeight == null || logicalDensityDpi == null ||
            logicalWidth <= 0 || logicalHeight <= 0 || logicalDensityDpi <= 0
        ) {
            return null
        }
        val configuration = Configuration()
        configuration.densityDpi = logicalDensityDpi
        configuration.screenWidthDp =
            (logicalWidth / (logicalDensityDpi / 160.0f)).roundToInt().coerceAtLeast(1)
        configuration.screenHeightDp =
            (logicalHeight / (logicalDensityDpi / 160.0f)).roundToInt().coerceAtLeast(1)
        configuration.smallestScreenWidthDp = minOf(
            configuration.screenWidthDp,
            configuration.screenHeightDp,
        )
        return configuration
    }

    private fun logSkip(
        entryName: String?,
        reason: String,
        callingUid: Int,
        packageName: String?,
        displayInfo: Any?,
    ) {
        if (!DpisLog.isLoggingEnabled()) {
            return
        }
        val message = "system_server display-manager-info skip: reason=" + reason +
                ", uid=" + callingUid +
                ", package=" + HookLog.safeToString(packageName) +
                ", displayInfo=" + HookLog.safeToString(
            HookLog.describeDisplayInfo(displayInfo),
        )
        HookLog.logDiagnosticIfChanged(
            "display-manager-info-skip|$reason|$callingUid",
            message,
            HookLog.resolveLogMinIntervalMs(entryName),
            packageName,
            "skipped",
        )
    }

}
