package com.dpis.module

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import com.dpis.module.config.ConfigSnapshotLoader
import com.dpis.module.config.PerAppDisplayConfig
import com.dpis.module.config.PerAppDisplayConfigSource
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.runtime.probe.RuntimeClock
import com.dpis.module.runtime.systemserver.selection.ConfigRefreshTtl
import com.dpis.module.viewport.PerAppDisplayEnvironment
import com.dpis.module.viewport.PerAppDisplayOverrideCalculator
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge
import com.dpis.module.viewport.ViewportSourceSnapshot
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

internal object LegacySystemServerHookInstaller {
    private const val MAX_PACKAGE_RECURSION_DEPTH = 5
    private val PACKAGE_STRING_METHOD_NAMES = arrayOf(
        "getOwningPackage", "getPackageName", "getPackage", "getOpPackageName",
    )
    private val PACKAGE_OBJECT_METHOD_NAMES = arrayOf(
        "getIntent", "getComponent", "getActivityInfo", "getApplicationInfo", "getRequest",
        "getTargetActivity", "getOrigActivity", "getRealActivity",
    )
    private val PACKAGE_STRING_FIELD_NAMES = arrayOf(
        "packageName", "mPackageName", "package", "launchedFromPackage",
    )
    private val PACKAGE_OBJECT_FIELD_NAMES = arrayOf(
        "intent", "mIntent", "component", "mComponent", "activityInfo", "applicationInfo",
        "request", "mRequest", "targetActivity", "origActivity", "realActivity",
    )
    private val LAUNCH_ACTIVITY_ITEM_INSTALLED = AtomicBoolean(false)
    private val RUST_PROCESS_INSTALLED = AtomicBoolean(false)
    private val LAST_MESSAGES = ConcurrentHashMap<String, String>()

    @JvmStatic
    fun install() {
        val source = PerAppDisplayConfigSource.withLegacyRuntimePropertyFallback(
            PerAppDisplayConfigSource.RefreshingSnapshotProvider(
                { ConfigSnapshotLoader.fromStore(LegacyConfigStoreFactory.createSystemServer()) },
                ConfigRefreshTtl.SYSTEM_SERVER_TTL_MILLIS,
            ),
        )
        val systemServerHooksEnabled = source.isSystemServerHooksEnabled()
        var hookedCount = 0
        var constructorHookCount = 0
        var attempted = false
        logDebug("legacy system_server install enter: hooksEnabled=$systemServerHooksEnabled")

        if (RUST_PROCESS_INSTALLED.compareAndSet(false, true)) {
            attempted = true
            try {
                if (LegacyRustProcessHookInstaller.install(source)) {
                    hookedCount++
                } else {
                    RUST_PROCESS_INSTALLED.set(false)
                }
            } catch (throwable: Throwable) {
                RUST_PROCESS_INSTALLED.set(false)
                logError(
                    "legacy system_server rust-process hook failed: " +
                            "${throwable.javaClass.name}: ${throwable.message}",
                )
            }
        }

        if (!LAUNCH_ACTIVITY_ITEM_INSTALLED.compareAndSet(false, true)) {
            logInstallSummaryIfAttempted(attempted, hookedCount, constructorHookCount)
            return
        }
        attempted = true
        try {
            val launchActivityItemClass = Class.forName(
                "android.app.servertransaction.LaunchActivityItem",
            )
            for (constructor in launchActivityItemClass.declaredConstructors) {
                XposedBridge.hookMethod(constructor, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        applyLaunchActivityItemArgs(source, param.args)
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        applyLaunchActivityItemObject(source, param.thisObject)
                    }
                })
                hookedCount++
                constructorHookCount++
            }
        } catch (throwable: Throwable) {
            LAUNCH_ACTIVITY_ITEM_INSTALLED.set(false)
            logError(
                "legacy system_server launch-activity-item hook failed: " +
                        "${throwable.javaClass.name}: ${throwable.message}",
            )
        }
        logInstallSummaryIfAttempted(attempted, hookedCount, constructorHookCount)
    }

    @JvmStatic
    fun applyLaunchActivityItemArgs(source: PerAppDisplayConfigSource?, args: Array<Any?>?) {
        if (source == null || args == null) return
        if (!source.isSystemServerHooksEnabled()) {
            logDebug("legacy system_server launch-activity-item skipped: reason=hooks-disabled")
            return
        }
        val packageName = findActivityInfoPackage(args)
        if (packageName == null) {
            logDebug("legacy system_server launch-activity-item skipped: reason=package-unresolved")
            return
        }
        val config = source.get(packageName) ?: run {
            logDebug("legacy system_server launch-activity-item skipped: package=$packageName, reason=config-missing")
            return
        }
        val baseConfiguration = findFirstConfiguration(args)
        if (baseConfiguration == null) {
            logDebug("legacy system_server launch-activity-item skipped: package=$packageName, reason=configuration-missing")
            return
        }
        val environment = if (config.hasViewportOverride()) {
            resolveTargetEnvironment(packageName, baseConfiguration, config)
        } else {
            null
        }
        var changed = false
        for (arg in args) {
            if (arg is Configuration) {
                changed = applyConfiguration(arg, environment) || changed
                changed = applyFontScale(arg, config) || changed
            }
        }
        if (changed) {
            logDebug(
                "legacy system_server launch-activity-item apply: package=$packageName, " +
                        "widthDp=${baseConfiguration.screenWidthDp}, densityDpi=${baseConfiguration.densityDpi}, " +
                        "fontScale=${baseConfiguration.fontScale}",
            )
        }
    }

    @JvmStatic
    fun applyLaunchActivityItemObject(
        source: PerAppDisplayConfigSource?,
        launchActivityItem: Any?,
    ) {
        if (source == null || launchActivityItem == null) return
        val activityInfo = readLaunchActivityInfo(launchActivityItem)
        val packageName = activityInfo?.packageName ?: return
        if (!isLikelyPackageName(packageName)) return
        val config = source.get(packageName) ?: return
        val baseConfiguration = readLaunchActivityConfiguration(launchActivityItem) ?: return
        val environment = if (config.hasViewportOverride()) {
            resolveTargetEnvironment(packageName, baseConfiguration, config)
        } else {
            null
        }
        var changed = false
        changed = applyConfigurationField(launchActivityItem, "mCurConfig", environment) || changed
        changed =
            applyConfigurationField(launchActivityItem, "mOverrideConfig", environment) || changed
        changed = applyFontScaleField(launchActivityItem, "mCurConfig", config) || changed
        changed = applyFontScaleField(launchActivityItem, "mOverrideConfig", config) || changed
        if (changed) {
            logIfChanged(
                "$packageName:legacy-system-launch-object-apply",
                "legacy system_server launch-activity-item object apply: package=$packageName, " +
                        "widthDp=${baseConfiguration.screenWidthDp}, densityDpi=${baseConfiguration.densityDpi}, " +
                        "fontScale=${baseConfiguration.fontScale}",
            )
        }
    }

    private fun findActivityInfoPackage(args: Array<Any?>): String? {
        for (arg in args) {
            if (arg is ActivityInfo && !arg.packageName.isNullOrBlank()) return arg.packageName
        }
        for (arg in args) {
            findPackageNameRecursive(arg, 0)?.let { return it }
        }
        return null
    }

    private fun findPackageNameRecursive(target: Any?, depth: Int): String? {
        if (target == null || depth > MAX_PACKAGE_RECURSION_DEPTH) return null
        if (target is ActivityInfo && !target.packageName.isNullOrBlank()) return target.packageName
        if (target is String && isLikelyPackageName(target)) return target
        for (methodName in PACKAGE_STRING_METHOD_NAMES) {
            invokeStringMethod(target, methodName)?.let { return it }
        }
        for (methodName in PACKAGE_OBJECT_METHOD_NAMES) {
            findPackageNameRecursive(
                invokeObjectMethod(target, methodName),
                depth + 1
            )?.let { return it }
        }
        for (fieldName in PACKAGE_STRING_FIELD_NAMES) {
            val value = readField(target, fieldName)
            if (value is String && isLikelyPackageName(value)) return value
        }
        for (fieldName in PACKAGE_OBJECT_FIELD_NAMES) {
            findPackageNameRecursive(readField(target, fieldName), depth + 1)?.let { return it }
        }
        for (field in target.javaClass.declaredFields) {
            if (field.type.isPrimitive || field.type.isEnum) continue
            try {
                field.isAccessible = true
                val nested = field.get(target)
                if (nested == null || nested === target || field.name == "this\$0") continue
                findPackageNameRecursive(nested, depth + 1)?.let { return it }
            } catch (_: ReflectiveOperationException) {
                // Continue probing inaccessible fields.
            }
        }
        return extractPackageFromText(target.toString())
    }

    private fun invokeStringMethod(target: Any, methodName: String): String? = try {
        val method = target.javaClass.getMethod(methodName)
        if (method.parameterCount != 0 || method.returnType != String::class.java) return null
        (method.invoke(target) as? String)?.takeIf(::isLikelyPackageName)
    } catch (_: ReflectiveOperationException) {
        null
    }

    private fun invokeObjectMethod(target: Any, methodName: String): Any? = try {
        val method = target.javaClass.getMethod(methodName)
        if (method.parameterCount != 0) null else method.invoke(target)
    } catch (_: ReflectiveOperationException) {
        null
    }

    private fun readField(target: Any, fieldName: String): Any? {
        var current: Class<*>? = target.javaClass
        while (current != null && current != Any::class.java) {
            try {
                val field = current.getDeclaredField(fieldName)
                field.isAccessible = true
                return field.get(target)
            } catch (_: ReflectiveOperationException) {
                current = current.superclass
            }
        }
        return null
    }

    private fun isLikelyPackageName(value: String?): Boolean =
        !value.isNullOrBlank() && value.indexOf('.') > 0 &&
                !value.contains(' ') && !value.contains('/') &&
                Character.isJavaIdentifierStart(value[0])

    private fun extractPackageFromText(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return value.split(Regex("[^A-Za-z0-9_.$]+"))
            .firstOrNull(::isLikelyPackageName)
    }

    private fun findFirstConfiguration(args: Array<Any?>): Configuration? =
        args.firstOrNull { it is Configuration } as? Configuration

    private fun readLaunchActivityInfo(item: Any): ActivityInfo? =
        readField(item, "mInfo") as? ActivityInfo

    private fun readLaunchActivityConfiguration(item: Any): Configuration? =
        (readField(item, "mOverrideConfig") as? Configuration)
            ?: (readField(item, "mCurConfig") as? Configuration)

    private fun applyConfiguration(
        configuration: Configuration?,
        environment: PerAppDisplayEnvironment?,
    ): Boolean {
        if (configuration == null || environment == null) return false
        val changed = configuration.screenWidthDp != environment.widthDp ||
                configuration.screenHeightDp != environment.heightDp ||
                configuration.smallestScreenWidthDp != environment.smallestWidthDp ||
                configuration.densityDpi != environment.densityDpi
        if (changed) {
            ViewportOverride.apply(
                configuration,
                ViewportOverride.Result(
                    environment.widthDp,
                    environment.heightDp,
                    environment.smallestWidthDp,
                    environment.densityDpi,
                ),
            )
        }
        return changed
    }

    private fun applyConfigurationField(
        target: Any,
        fieldName: String,
        environment: PerAppDisplayEnvironment?,
    ): Boolean = applyConfiguration(readField(target, fieldName) as? Configuration, environment)

    private fun resolveTargetEnvironment(
        packageName: String,
        configuration: Configuration,
        config: PerAppDisplayConfig,
    ): PerAppDisplayEnvironment? {
        if (!config.hasViewportOverride()) return null
        if (ViewportApplyMode.COMPAT == ViewportApplyMode.normalize(config.targetViewportMode)) return null
        val widthPx = resolveWidthPx(configuration)
        val heightPx = resolveHeightPx(configuration)
        resolveAlreadyAppliedEnvironment(
            packageName,
            configuration,
            widthPx,
            heightPx,
            config
        )?.let { return it }
        val environment = PerAppDisplayOverrideCalculator.calculate(
            configuration, widthPx, heightPx, config.targetViewportSpec,
        )
        publishViewportRuntimeMarker(packageName, configuration, environment, config)
        return environment
    }

    private fun resolveAlreadyAppliedEnvironment(
        packageName: String,
        configuration: Configuration,
        widthPx: Int,
        heightPx: Int,
        config: PerAppDisplayConfig,
    ): PerAppDisplayEnvironment? {
        if (!config.targetViewportSpec.isEnabled) return null
        val marker = ViewportRuntimeMarkerBridge.read(
            packageName,
            config.targetViewportSpec.fingerprint(),
            RuntimeClock.crossProcessMarkerMillis(),
        )
        if (!matchesCurrentConfiguration(configuration, marker)) {
            maybeLogMarkerState(packageName, configuration, config, marker)
            return null
        }
        logIfChanged(
            "$packageName:legacy-system-marker-reuse",
            "legacy system_server launch-activity-item marker reuse: package=$packageName, " +
                    "widthDp=${configuration.screenWidthDp}, heightDp=${configuration.screenHeightDp}, " +
                    "smallestWidthDp=${configuration.smallestScreenWidthDp}, densityDpi=${configuration.densityDpi}, " +
                    "markerAgeMs=${marker.ageMillis}",
        )
        return PerAppDisplayEnvironment(
            configuration.screenWidthDp, configuration.screenHeightDp,
            configuration.smallestScreenWidthDp, configuration.densityDpi, widthPx, heightPx,
        )
    }

    private fun publishViewportRuntimeMarker(
        packageName: String,
        source: Configuration,
        environment: PerAppDisplayEnvironment?,
        config: PerAppDisplayConfig,
    ): Boolean {
        if (!config.targetViewportSpec.isEnabled || environment == null) return false
        val published = ViewportRuntimeMarkerBridge.publishSystemServerRecord(
            packageName,
            config.targetViewportSpec,
            ConfigurationMarker(source),
            EnvironmentMarker(environment),
            ViewportSourceSnapshot.SCOPE_DISPLAY,
            RuntimeClock.crossProcessMarkerMillis(),
        )
        logIfChanged(
            "$packageName:legacy-system-marker-publish",
            "legacy system_server launch-activity-item marker publish: package=$packageName, " +
                    "published=$published, targetFp=${config.targetViewportSpec.fingerprint()}, " +
                    "sourceWidthDp=${source.screenWidthDp}, sourceHeightDp=${source.screenHeightDp}, " +
                    "sourceSmallestWidthDp=${source.smallestScreenWidthDp}, sourceDensityDpi=${source.densityDpi}, " +
                    "targetWidthDp=${environment.widthDp}, targetHeightDp=${environment.heightDp}, " +
                    "targetSmallestWidthDp=${environment.smallestWidthDp}, targetDensityDpi=${environment.densityDpi}",
        )
        return published
    }

    private fun matchesCurrentConfiguration(
        configuration: Configuration?,
        marker: ViewportRuntimeMarkerBridge.ParseResult?,
    ): Boolean {
        if (configuration == null || marker == null || !marker.hit || marker.record == null) return false
        val signature = ViewportRuntimeMarkerBridge.configurationSignature(
            configuration.screenWidthDp,
            configuration.screenHeightDp,
            configuration.smallestScreenWidthDp,
            configuration.densityDpi,
            ViewportSourceSnapshot.SCOPE_DISPLAY,
        )
        return signature == marker.record.resultSignature
    }

    private fun applyFontScale(
        configuration: Configuration?,
        config: PerAppDisplayConfig?
    ): Boolean {
        if (configuration == null || config == null ||
            FontApplyMode.SYSTEM_EMULATION != config.targetFontMode ||
            config.targetFontScalePercent == null || config.targetFontScalePercent <= 0
        ) return false
        val fontScale = config.targetFontScalePercent / 100.0f
        if (kotlin.math.abs(configuration.fontScale - fontScale) < 0.0001f) return false
        configuration.fontScale = fontScale
        return true
    }

    private fun applyFontScaleField(
        target: Any,
        fieldName: String,
        config: PerAppDisplayConfig
    ): Boolean =
        applyFontScale(readField(target, fieldName) as? Configuration, config)

    private fun resolveWidthPx(configuration: Configuration): Int =
        if (configuration.screenWidthDp <= 0 || configuration.densityDpi <= 0) 0
        else (configuration.screenWidthDp * (configuration.densityDpi / 160.0f)).roundToInt()

    private fun resolveHeightPx(configuration: Configuration): Int =
        if (configuration.screenHeightDp <= 0 || configuration.densityDpi <= 0) 0
        else (configuration.screenHeightDp * (configuration.densityDpi / 160.0f)).roundToInt()

    private fun logDebug(message: String) {
        if (DpisLog.isLoggingEnabled()) DpisLog.i(message)
    }

    private fun logIfChanged(key: String, message: String): Boolean {
        val previous = LAST_MESSAGES.put(key, message)
        if (message == previous) return false
        logDebug(message)
        return true
    }

    private fun maybeLogMarkerState(
        packageName: String,
        configuration: Configuration,
        config: PerAppDisplayConfig,
        marker: ViewportRuntimeMarkerBridge.ParseResult?,
    ) {
        if (!config.targetViewportSpec.isEnabled || marker == null) return
        logIfChanged(
            "$packageName:legacy-system-marker-state",
            "legacy system_server launch-activity-item marker state: package=$packageName, " +
                    "hit=${marker.hit}, reason=${marker.reason}, targetFp=${config.targetViewportSpec.fingerprint()}, " +
                    "widthDp=${configuration.screenWidthDp}, heightDp=${configuration.screenHeightDp}, " +
                    "smallestWidthDp=${configuration.smallestScreenWidthDp}, densityDpi=${configuration.densityDpi}",
        )
    }

    private fun logInstallSummaryIfAttempted(
        attempted: Boolean,
        hookedCount: Int,
        constructorHookCount: Int
    ) {
        if (!attempted) return
        logDebug(
            "legacy system_server hook ready: hooks=$hookedCount, " +
                    "launchActivityItemConstructors=$constructorHookCount, " +
                    "rustProcessInstalled=${RUST_PROCESS_INSTALLED.get()}",
        )
    }

    private fun logError(message: String) {
        try {
            XposedBridge.log("DPIS $message")
        } catch (_: Throwable) {
        }
        try {
            Log.e(DpisLog.TAG, message)
        } catch (_: Throwable) {
        }
    }

    @JvmStatic
    fun matchesCurrentConfigurationForTest(
        configuration: Configuration?,
        marker: ViewportRuntimeMarkerBridge.ParseResult?,
    ): Boolean = matchesCurrentConfiguration(configuration, marker)

    private data class ConfigurationMarker(val configuration: Configuration) :
        ViewportRuntimeMarkerBridge.ConfigurationLike {
        override fun widthDp(): Int = configuration.screenWidthDp
        override fun heightDp(): Int = configuration.screenHeightDp
        override fun smallestWidthDp(): Int = configuration.smallestScreenWidthDp
        override fun densityDpi(): Int = configuration.densityDpi
    }

    private data class EnvironmentMarker(val environment: PerAppDisplayEnvironment) :
        ViewportRuntimeMarkerBridge.ConfigurationLike {
        override fun widthDp(): Int = environment.widthDp
        override fun heightDp(): Int = environment.heightDp
        override fun smallestWidthDp(): Int = environment.smallestWidthDp
        override fun densityDpi(): Int = environment.densityDpi
    }
}
