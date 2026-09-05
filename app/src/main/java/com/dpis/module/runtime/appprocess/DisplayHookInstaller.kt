package com.dpis.module.runtime.appprocess

import android.graphics.Point
import android.util.DisplayMetrics
import com.dpis.module.DpisConfigStore
import com.dpis.module.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.runtime.ProcessScopedInstallGate
import com.dpis.module.runtime.RuntimeDiagnosticLogFingerprint
import com.dpis.module.runtime.RuntimeHotPathEvidenceSampler
import com.dpis.module.runtime.hookapi.ModernApiCapabilitiesResolver
import com.dpis.module.viewport.DensityOverride
import com.dpis.module.viewport.ViewportPropertyBridge
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.VirtualDisplayOverride
import com.dpis.module.viewport.VirtualDisplayState
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker
import java.lang.Float
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Throwable
import kotlin.Throws
import kotlin.concurrent.Volatile
import kotlin.math.max
import kotlin.math.min
import kotlin.synchronized

object DisplayHookInstaller {
    @Volatile
    private var installedPid = -1

    @Volatile
    private var targetPackageName: String? = null

    @Volatile
    private var targetStore: DpisConfigStore? = null

    @Volatile
    private var currentPackageNameMethod: Method? = null

    @Volatile
    private var currentPackageNameUnavailable = false
    private val RUNTIME_FALLBACK_OVERRIDES = ConcurrentHashMap<String, RuntimeFallbackOverride>()
    private val LAST_MESSAGES = ConcurrentHashMap<String, String>()
    private val HOTPATH_SAMPLER = RuntimeHotPathEvidenceSampler()
    private const val HOOK_ID_DISPLAY_GET_METRICS = "display_get_metrics"
    private const val HOOK_ID_DISPLAY_GET_REAL_METRICS = "display_get_real_metrics"
    private const val HOOK_ID_DISPLAY_GET_DISPLAY_INFO = "display_get_display_info"
    private const val HOOK_ID_DISPLAY_GET_SIZE = "display_get_size"
    private const val HOOK_ID_DISPLAY_GET_REAL_SIZE = "display_get_real_size"

    @JvmStatic
    fun resetForHotReload() {
        installedPid = -1
        RUNTIME_FALLBACK_OVERRIDES.clear()
    }

    @JvmStatic
    @Throws(ReflectiveOperationException::class)
    fun install(xposed: XposedInterface, packageName: String?, store: DpisConfigStore?) {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            return
        }
        synchronized(DisplayHookInstaller::class.java) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                return
            }
            targetPackageName = packageName
            targetStore = store
            val bootClassLoader = ClassLoader.getSystemClassLoader()
            val displayClass = Class.forName("android.view.Display", false, bootClassLoader)
            hookDisplayMetricsMethod(xposed, displayClass, "getMetrics")
            hookDisplayMetricsMethod(xposed, displayClass, "getRealMetrics")
            hookDisplayInfoMethod(xposed, displayClass, bootClassLoader)
            hookPointMethod(xposed, displayClass, "getSize")
            hookPointMethod(xposed, displayClass, "getRealSize")
            installedPid = ProcessScopedInstallGate.currentPid()
            DpisLog.i("Display hook ready, " + RuntimeDiagnosticLogFingerprint.field())
        }
    }

    @JvmStatic
    fun setTargetPackageNameForLegacy(packageName: String?) {
        targetPackageName = packageName
    }

    @JvmStatic
    fun setTargetStoreForLegacy(store: DpisConfigStore?) {
        targetStore = store
    }

    @Throws(ReflectiveOperationException::class)
    private fun hookDisplayMetricsMethod(
        xposed: XposedInterface, displayClass: Class<*>,
        methodName: String
    ) {
        val method = displayClass.getDeclaredMethod(methodName, DisplayMetrics::class.java)
        var hookBuilder =
            xposed.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
        if ("getMetrics" == methodName) {
            hookBuilder = ModernApiCapabilitiesResolver.fromXposed(xposed)
                .applyStableHookId<HookBuilder>(hookBuilder, HOOK_ID_DISPLAY_GET_METRICS)
        } else if ("getRealMetrics" == methodName) {
            hookBuilder = ModernApiCapabilitiesResolver.fromXposed(xposed)
                .applyStableHookId<HookBuilder>(hookBuilder, HOOK_ID_DISPLAY_GET_REAL_METRICS)
        }
        hookBuilder
            .intercept(Hooker { chain: XposedInterface.Chain? ->
                val result = chain!!.proceed()
                val metrics = chain.getArg(0) as DisplayMetrics?
                applyDisplayMetrics(metrics, methodName)
                result
            })
    }

    @Throws(ReflectiveOperationException::class)
    private fun hookPointMethod(
        xposed: XposedInterface, displayClass: Class<*>,
        methodName: String
    ) {
        val method = displayClass.getDeclaredMethod(methodName, Point::class.java)
        var hookBuilder =
            xposed.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
        if ("getSize" == methodName) {
            hookBuilder = ModernApiCapabilitiesResolver.fromXposed(xposed)
                .applyStableHookId<HookBuilder>(hookBuilder, HOOK_ID_DISPLAY_GET_SIZE)
        } else if ("getRealSize" == methodName) {
            hookBuilder = ModernApiCapabilitiesResolver.fromXposed(xposed)
                .applyStableHookId<HookBuilder>(hookBuilder, HOOK_ID_DISPLAY_GET_REAL_SIZE)
        }
        hookBuilder
            .intercept(Hooker { chain: XposedInterface.Chain? ->
                val result = chain!!.proceed()
                val point = chain.getArg(0) as Point?
                applyPoint(point, methodName)
                result
            })
    }

    private fun hookDisplayInfoMethod(
        xposed: XposedInterface,
        displayClass: Class<*>,
        bootClassLoader: ClassLoader?
    ) {
        try {
            val displayInfoClass = Class.forName("android.view.DisplayInfo", false, bootClassLoader)
            val method = displayClass.getDeclaredMethod("getDisplayInfo", displayInfoClass)
            // Stable id lets API 102 replace the same display info hook during hot reload.
            ModernApiCapabilitiesResolver.fromXposed(xposed).applyStableHookId<HookBuilder>(
                xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_DISPLAY_GET_DISPLAY_INFO
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val result = chain!!.proceed()
                    val displayInfo = chain.getArg(0)
                    applyDisplayInfo(displayInfo, "getDisplayInfo")
                    result
                })
        } catch (ignored: ReflectiveOperationException) {
            DpisLog.i("Display getDisplayInfo hook skipped")
        }
    }

    @JvmStatic
    fun applyDisplayMetrics(metrics: DisplayMetrics?, sourceTag: String) {
        if (metrics == null) {
            return
        }
        val routeName = "display_metrics_override"
        val currentPackageName = resolveCurrentPackageName()
        recordViewportProbeAtMostEvery(
            routeName,
            ("source=" + sourceTag
                    + ", " + RuntimeDiagnosticLogFingerprint.field()
                    + ", callback=Display." + sourceTag)
        )
        if (!shouldApplyOverrideForPackage(targetPackageName, currentPackageName)) {
            recordViewportSkipAtMostEvery(
                routeName,
                ("source=" + sourceTag
                        + ", reason=package_mismatch_or_unresolved"
                        + ", targetPackage=" + safeValue(targetPackageName)
                        + ", currentPackage=" + safeValue(currentPackageName))
            )
            return
        }
        val effectivePackageName = WebApkRuntimeOwnerBridge.resolveEffectivePackage(
            targetStore, targetPackageName
        )
        val effectiveStore = WebApkRuntimeOwnerBridge.resolveEffectiveStore(
            targetStore, effectivePackageName
        )
        var override = resolvePackageScopedOverride(
            effectivePackageName, effectiveStore
        )
        if (override == null) {
            override = derivePackageScopedOverride(effectivePackageName, effectiveStore, metrics)
        }
        if (override == null) {
            recordViewportSkipAtMostEvery(
                routeName,
                "source=" + sourceTag + ", reason=no_package_scoped_override"
            )
            return
        }
        val originalDensityDpi = metrics.densityDpi
        val originalDensity = metrics.density
        val originalScaledDensity = metrics.scaledDensity
        val originalWidthPixels = metrics.widthPixels
        val originalHeightPixels = metrics.heightPixels
        if (override.densityDpi > 0 && override.widthPx > 0 && override.heightPx > 0) {
            val targetSpec = effectiveStore.getTargetViewportSpec(effectivePackageName!!)
            if (targetSpec.isEnabled()) {
                RUNTIME_FALLBACK_OVERRIDES.put(
                    effectivePackageName,
                    RuntimeFallbackOverride(targetSpec, override)
                )
            }
        }
        var fontScale =
            if (metrics.density > 0f) (metrics.scaledDensity / metrics.density) else 1.0f
        if (fontScale <= 0f) {
            fontScale = 1.0f
        }
        metrics.densityDpi = override.densityDpi
        metrics.density = DensityOverride.densityFromDpi(override.densityDpi)
        metrics.scaledDensity = metrics.density * fontScale
        metrics.widthPixels = override.widthPx
        metrics.heightPixels = override.heightPx
        val changed = originalDensityDpi != metrics.densityDpi || Float.compare(
            originalDensity,
            metrics.density
        ) != 0 || Float.compare(
            originalScaledDensity,
            metrics.scaledDensity
        ) != 0 || originalWidthPixels != metrics.widthPixels || originalHeightPixels != metrics.heightPixels
        if (!changed) {
            recordViewportSkipAtMostEvery(
                routeName,
                ("source=" + sourceTag + ", reason=stable_target"
                        + ", widthPx=" + metrics.widthPixels
                        + ", heightPx=" + metrics.heightPixels
                        + ", densityDpi=" + metrics.densityDpi)
            )
            return
        }
        val message = ("Display override(" + sourceTag + "): package="
                + safeValue(effectivePackageName)
                + ", widthPx=" + metrics.widthPixels
                + ", heightPx=" + metrics.heightPixels
                + ", densityDpi=" + metrics.densityDpi)
        if (logIfChanged(effectivePackageName + ":metrics:" + sourceTag, message)) {
            RuntimeHotPathEvents.applied(
                effectivePackageName,
                "viewport",
                routeName,
                ("source=" + sourceTag
                        + ", widthPx=" + originalWidthPixels + "->" + metrics.widthPixels
                        + ", heightPx=" + originalHeightPixels + "->" + metrics.heightPixels
                        + ", densityDpi=" + originalDensityDpi + "->" + metrics.densityDpi
                        + ", density=" + originalDensity + "->" + metrics.density
                        + ", scaledDensity=" + originalScaledDensity + "->"
                        + metrics.scaledDensity)
            )
        }
    }

    @JvmStatic
    fun applyPoint(point: Point?, sourceTag: String) {
        if (point == null) {
            return
        }
        val routeName = "display_size_override"
        val currentPackageName = resolveCurrentPackageName()
        recordViewportProbeAtMostEvery(
            routeName,
            ("source=" + sourceTag
                    + ", " + RuntimeDiagnosticLogFingerprint.field()
                    + ", callback=Display." + sourceTag)
        )
        if (!shouldApplyOverrideForPackage(targetPackageName, currentPackageName)) {
            recordViewportSkipAtMostEvery(
                routeName,
                ("source=" + sourceTag
                        + ", reason=package_mismatch_or_unresolved"
                        + ", targetPackage=" + safeValue(targetPackageName)
                        + ", currentPackage=" + safeValue(currentPackageName))
            )
            return
        }
        val effectivePackageName = WebApkRuntimeOwnerBridge.resolveEffectivePackage(
            targetStore, targetPackageName
        )
        val effectiveStore = WebApkRuntimeOwnerBridge.resolveEffectiveStore(
            targetStore, effectivePackageName
        )
        var override = resolvePackageScopedOverride(
            effectivePackageName, effectiveStore
        )
        if (override == null) {
            override = resolveCachedFallback(effectivePackageName, effectiveStore)
        }
        if (override == null) {
            recordViewportSkipAtMostEvery(
                routeName,
                "source=" + sourceTag + ", reason=no_package_scoped_override"
            )
            return
        }
        val originalX = point.x
        val originalY = point.y
        point.x = override.widthPx
        point.y = override.heightPx
        if (originalX == point.x && originalY == point.y) {
            recordViewportSkipAtMostEvery(
                routeName,
                ("source=" + sourceTag + ", reason=stable_target"
                        + ", size=" + point.x + "x" + point.y)
            )
            return
        }
        val message = ("Display override(" + sourceTag + "): package="
                + safeValue(effectivePackageName) + ", size=" + point.x + "x" + point.y)
        if (logIfChanged(effectivePackageName + ":point:" + sourceTag, message)) {
            RuntimeHotPathEvents.applied(
                effectivePackageName,
                "viewport",
                routeName,
                ("source=" + sourceTag
                        + ", widthPx=" + originalX + "->" + point.x
                        + ", heightPx=" + originalY + "->" + point.y)
            )
        }
    }

    @JvmStatic
    fun applyDisplayInfo(displayInfo: Any?, sourceTag: String) {
        if (displayInfo == null) {
            return
        }
        val routeName = "display_info_override"
        val currentPackageName = resolveCurrentPackageName()
        recordViewportProbeAtMostEvery(
            routeName,
            ("source=" + sourceTag
                    + ", " + RuntimeDiagnosticLogFingerprint.field()
                    + ", callback=Display." + sourceTag)
        )
        if (!shouldApplyOverrideForPackage(targetPackageName, currentPackageName)) {
            recordViewportSkipAtMostEvery(
                routeName,
                ("source=" + sourceTag
                        + ", reason=package_mismatch_or_unresolved"
                        + ", targetPackage=" + safeValue(targetPackageName)
                        + ", currentPackage=" + safeValue(currentPackageName))
            )
            return
        }
        val effectivePackageName = WebApkRuntimeOwnerBridge.resolveEffectivePackage(
            targetStore, targetPackageName
        )
        val effectiveStore = WebApkRuntimeOwnerBridge.resolveEffectiveStore(
            targetStore, effectivePackageName
        )
        var override = resolvePackageScopedOverride(
            effectivePackageName, effectiveStore
        )
        if (override == null) {
            override = resolveCachedFallback(effectivePackageName, effectiveStore)
        }
        if (override == null) {
            recordViewportSkipAtMostEvery(
                routeName,
                "source=" + sourceTag + ", reason=no_package_scoped_override"
            )
            return
        }
        var changed = false
        changed = changed or writeIntField(displayInfo, "logicalDensityDpi", override.densityDpi)
        changed = changed or writeIntField(displayInfo, "logicalWidth", override.widthPx)
        changed = changed or writeIntField(displayInfo, "logicalHeight", override.heightPx)
        changed = changed or writeIntField(displayInfo, "appWidth", override.widthPx)
        changed = changed or writeIntField(displayInfo, "appHeight", override.heightPx)
        changed = changed or writeIntField(displayInfo, "smallestNominalAppWidth", override.widthPx)
        changed =
            changed or writeIntField(displayInfo, "smallestNominalAppHeight", override.heightPx)
        changed = changed or writeIntField(displayInfo, "largestNominalAppWidth", override.widthPx)
        changed =
            changed or writeIntField(displayInfo, "largestNominalAppHeight", override.heightPx)
        if (!changed) {
            recordViewportSkipAtMostEvery(
                routeName,
                ("source=" + sourceTag + ", reason=stable_target"
                        + ", logical=" + override.widthPx + "x" + override.heightPx
                        + ", densityDpi=" + override.densityDpi)
            )
            return
        }
        val message = ("Display override(" + sourceTag + "): package="
                + safeValue(effectivePackageName) + ", logical=" + override.widthPx + "x"
                + override.heightPx + ", densityDpi=" + override.densityDpi)
        if (logIfChanged(effectivePackageName + ":displayInfo:" + sourceTag, message)) {
            RuntimeHotPathEvents.applied(
                effectivePackageName,
                "viewport",
                routeName,
                ("source=" + sourceTag
                        + ", logicalWidth=" + override.widthPx
                        + ", logicalHeight=" + override.heightPx
                        + ", densityDpi=" + override.densityDpi)
            )
        }
    }

    @JvmStatic
    @JvmOverloads
    fun shouldApplyOverrideForPackage(
        packageName: String?,
        currentPackageName: String? = resolveCurrentPackageName()
    ): Boolean {
        if (packageName == null || packageName.isBlank()) {
            return false
        }
        if (currentPackageName == null || currentPackageName.isBlank()) {
            return false
        }
        return packageName == currentPackageName
    }

    @JvmStatic
    fun resolvePackageScopedOverrideForTest(
        packageName: String?,
        store: DpisConfigStore?
    ): VirtualDisplayOverride.Result? {
        return resolvePackageScopedOverride(packageName, store)
    }

    private fun derivePackageScopedOverride(
        packageName: String?, store: DpisConfigStore?, metrics: DisplayMetrics?
    ): VirtualDisplayOverride.Result? {
        if (packageName == null || store == null || metrics == null || metrics.widthPixels <= 0 || metrics.heightPixels <= 0 || metrics.densityDpi <= 0) {
            return null
        }
        var targetSpec = ViewportPropertyBridge.readTargetSpec(packageName)
        if (!targetSpec.isEnabled()) {
            targetSpec = store.getTargetViewportSpec(packageName)
        }
        if (targetSpec == null || !targetSpec.isEnabled()) {
            return null
        }
        val sourceSmallestDp = max(
            1, Math.round(
                min(
                    metrics.widthPixels, metrics.heightPixels
                ) * 160f / metrics.densityDpi
            )
        )
        val targetSmallestDp = if (targetSpec.isAbsoluteDp())
            targetSpec.absoluteWidthDp()
        else max(
            1, Math.round(
                sourceSmallestDp
                        * targetSpec.scaleMilliPercent() / 100000f
            )
        )
        val targetDensityDpi = max(
            1, Math.round(
                min(metrics.widthPixels, metrics.heightPixels) * 160f / targetSmallestDp
            )
        )
        return VirtualDisplayOverride.Result(
            max(1, Math.round(metrics.widthPixels * 160f / targetDensityDpi)),
            max(1, Math.round(metrics.heightPixels * 160f / targetDensityDpi)),
            targetSmallestDp, targetDensityDpi,
            metrics.widthPixels, metrics.heightPixels
        )
    }

    private fun resolvePackageScopedOverride(
        packageName: String?,
        store: DpisConfigStore?
    ): VirtualDisplayOverride.Result? {
        if (packageName == null || packageName.isBlank() || store == null) {
            return null
        }
        val targetSpec = store.getTargetViewportSpec(packageName)
        if (!targetSpec.isEnabled()) {
            return null
        }
        val record =
            VirtualDisplayState.findDisplayRecordForTarget(packageName, targetSpec)
        return if (record != null) record.virtualDisplayResult else null
    }

    private fun resolveCachedFallback(
        packageName: String?,
        store: DpisConfigStore?
    ): VirtualDisplayOverride.Result? {
        if (packageName == null || store == null) {
            if (packageName != null) {
                RUNTIME_FALLBACK_OVERRIDES.remove(packageName)
            }
            return null
        }
        val targetSpec = store.getTargetViewportSpec(packageName)
        val cached = RUNTIME_FALLBACK_OVERRIDES.get(packageName)
        if (!targetSpec.isEnabled() || cached == null || targetSpec != cached.targetSpec) {
            RUNTIME_FALLBACK_OVERRIDES.remove(packageName)
            return null
        }
        return cached.result
    }

    private fun writeIntField(target: Any, fieldName: String, value: Int): Boolean {
        try {
            val field = target.javaClass.getDeclaredField(fieldName)
            field.setAccessible(true)
            val current = field.getInt(target)
            if (current == value) {
                return false
            }
            field.setInt(target, value)
            return true
        } catch (ignored: Throwable) {
            return false
        }
    }

    private fun logIfChanged(key: String, message: String): Boolean {
        val previous = LAST_MESSAGES.put(key, message)
        if (message != previous) {
            DpisLog.i(message)
            return true
        }
        return false
    }

    @JvmStatic
    fun resetHotPathSamplerForTest() {
        HOTPATH_SAMPLER.resetForTest()
    }

    private fun recordViewportProbeAtMostEvery(routeName: String?, detail: String?) {
        val sample =
            HOTPATH_SAMPLER.sample("probe:" + routeName + "|" + detail, detail)
        if (sample.emit) {
            val sampledDetail = sample.detail
            DpisLog.i(
                ("DPIS_VIEWPORT Display callback: package=" + safeValue(targetPackageName)
                        + ", route=" + routeName
                        + ", " + sampledDetail)
            )
            RuntimeHotPathEvents.probe(
                targetPackageName,
                "viewport",
                routeName,
                sampledDetail
            )
        }
    }

    private fun recordViewportSkipAtMostEvery(routeName: String?, detail: String?) {
        val sample =
            HOTPATH_SAMPLER.sample("skip:" + routeName + "|" + detail, detail)
        if (sample.emit) {
            val sampledDetail = sample.detail
            DpisLog.i(
                ("DPIS_VIEWPORT Display skip: package=" + safeValue(targetPackageName)
                        + ", route=" + routeName
                        + ", " + sampledDetail)
            )
            RuntimeHotPathEvents.skipped(
                targetPackageName,
                "viewport",
                routeName,
                sampledDetail
            )
        }
    }

    private fun safeValue(value: String?): String {
        return if (value == null || value.isBlank()) "unknown" else value
    }

    private fun resolveCurrentPackageName(): String? {
        try {
            var method = currentPackageNameMethod
            if (method == null) {
                if (currentPackageNameUnavailable) {
                    return null
                }
                synchronized(DisplayHookInstaller::class.java) {
                    method = currentPackageNameMethod
                    if (method == null && !currentPackageNameUnavailable) {
                        method = Class.forName("android.app.ActivityThread")
                            .getDeclaredMethod("currentPackageName")
                        currentPackageNameMethod = method
                    }
                }
            }
            if (method == null) {
                return null
            }
            val value = method.invoke(null)
            return if (value is String) value else null
        } catch (ignored: Throwable) {
            currentPackageNameUnavailable = true
            return null
        }
    }

    private class RuntimeFallbackOverride(
        val targetSpec: ViewportTargetSpec?,
        val result: VirtualDisplayOverride.Result?
    )
}
