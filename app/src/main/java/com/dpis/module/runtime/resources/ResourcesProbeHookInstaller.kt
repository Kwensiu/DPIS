package com.dpis.module.runtime.resources


import android.content.res.Configuration
import android.util.DisplayMetrics
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.ProcessScopedInstallGate
import com.dpis.module.runtime.probe.CallerTrace
import com.dpis.module.viewport.DensityOverride
import com.dpis.module.viewport.TargetViewportWidthResolver
import com.dpis.module.viewport.ViewportModePolicy
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.VirtualDisplayOverride
import io.github.libxposed.api.XposedInterface
import java.util.concurrent.atomic.AtomicInteger

object ResourcesProbeHookInstaller {
    private const val MAX_LOGS_PER_METHOD = 8
    private val displayMetricsLogCount = AtomicInteger()
    private val configurationLogCount = AtomicInteger()
    @Volatile
    private var targetPackageName: String? = null
    @Volatile
    private var configStore: DpisConfigStore? = null
    @Volatile
    private var runtimePolicy: HookRuntimePolicy? = null
    @Volatile
    private var installedPid = -1

    @JvmStatic
    fun resetForHotReload() {
        installedPid = -1
    }

    @JvmStatic
    @Throws(ReflectiveOperationException::class)
    fun install(xposed: XposedInterface, packageName: String?, store: DpisConfigStore?) {
        install(xposed, packageName, store, HookRuntimePolicy.fromStore(store))
    }

    @JvmStatic
    @Throws(ReflectiveOperationException::class)
    fun install(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore?,
        policy: HookRuntimePolicy?
    ) {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) return
        synchronized(this) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) return
            targetPackageName = packageName
            configStore = store
            runtimePolicy = policy
            val resourcesClass = Class.forName(
                "android.content.res.Resources",
                false,
                ClassLoader.getSystemClassLoader()
            )
            val getDisplayMetricsMethod = resourcesClass.getDeclaredMethod("getDisplayMetrics")
            xposed.hook(getDisplayMetricsMethod)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    var result = chain.proceed()
                    if (result is DisplayMetrics) {
                        val overridden = maybeOverrideDisplayMetrics(result)
                        if (overridden !== result) result = overridden
                        if (displayMetricsLogCount.incrementAndGet() <= MAX_LOGS_PER_METHOD) {
                            DpisLog.i(appendCaller(buildDisplayMetricsLog(result)))
                        }
                    }
                    result
                }
            val getConfigurationMethod = resourcesClass.getDeclaredMethod("getConfiguration")
            xposed.hook(getConfigurationMethod)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    var result = chain.proceed()
                    if (result is Configuration) {
                        val overridden = maybeOverrideConfiguration(result)
                        if (overridden !== result) result = overridden
                        if (configurationLogCount.incrementAndGet() <= MAX_LOGS_PER_METHOD) {
                            DpisLog.i(appendCaller(buildConfigurationLog(result)))
                        }
                    }
                    result
                }
            installedPid = ProcessScopedInstallGate.currentPid()
            DpisLog.i("Resources probe hook ready")
        }
    }

    @JvmStatic
    fun createOverriddenDisplayMetrics(
        metrics: DisplayMetrics?,
        targetWidthDp: Int,
        fontScale: Float
    ): DisplayMetrics? {
        if (metrics == null || targetWidthDp <= 0 || metrics.densityDpi <= 0) return metrics
        val sourceWidthDp =
            maxOf(1, kotlin.math.round(metrics.widthPixels * 160f / metrics.densityDpi).toInt())
        val sourceHeightDp =
            maxOf(1, kotlin.math.round(metrics.heightPixels * 160f / metrics.densityDpi).toInt())
        val result = VirtualDisplayOverride.derive(
            sourceWidthDp, sourceHeightDp, minOf(sourceWidthDp, sourceHeightDp),
            metrics.densityDpi, metrics.widthPixels, metrics.heightPixels, targetWidthDp
        ) ?: return metrics
        return DisplayMetrics().apply {
            widthPixels = metrics.widthPixels
            heightPixels = metrics.heightPixels
            densityDpi = result.densityDpi
            density = DensityOverride.densityFromDpi(result.densityDpi)
            scaledDensity = DensityOverride.scaledDensityFrom(result.densityDpi, fontScale)
            xdpi = metrics.xdpi
            ydpi = metrics.ydpi
        }
    }

    @JvmStatic
    fun createOverriddenConfiguration(
        configuration: Configuration?,
        targetWidthDp: Int
    ): Configuration? {
        if (configuration == null || targetWidthDp <= 0) return configuration
        val result = ViewportOverride.derive(configuration, targetWidthDp) ?: return configuration
        return Configuration(configuration).also { ViewportOverride.apply(it, result) }
    }

    private fun maybeOverrideDisplayMetrics(metrics: DisplayMetrics): DisplayMetrics {
        val targetWidthDp =
            TargetViewportWidthResolver.resolve(configStore, targetPackageName) ?: return metrics
        val fontScale = if (metrics.density > 0) metrics.scaledDensity / metrics.density else 1.0f
        return createOverriddenDisplayMetrics(metrics, targetWidthDp, fontScale) ?: metrics
    }

    private fun maybeOverrideConfiguration(configuration: Configuration): Configuration {
        val targetWidthDp = TargetViewportWidthResolver.resolve(configStore, targetPackageName)
            ?: return configuration
        if (!ViewportModePolicy.shouldApplyConfigurationOverride(
                runtimePolicy,
                configStore,
                targetPackageName
            )
        ) return configuration
        return createOverriddenConfiguration(configuration, targetWidthDp) ?: configuration
    }

    @JvmStatic
    fun buildDisplayMetricsLog(metrics: DisplayMetrics): String =
        "Resources probe(getDisplayMetrics): widthPx=${metrics.widthPixels}, heightPx=${metrics.heightPixels}, " +
                "densityDpi=${metrics.densityDpi}, density=${metrics.density}, scaledDensity=${metrics.scaledDensity}"

    @JvmStatic
    fun buildConfigurationLog(configuration: Configuration): String =
        "Resources probe(getConfiguration): widthDp=${configuration.screenWidthDp}, " +
                "heightDp=${configuration.screenHeightDp}, smallestWidthDp=${configuration.smallestScreenWidthDp}, " +
                "densityDpi=${configuration.densityDpi}"

    private fun appendCaller(message: String): String {
        val caller = CallerTrace.capture(targetPackageName) ?: return message
        return "$message, caller=$caller"
    }
}
