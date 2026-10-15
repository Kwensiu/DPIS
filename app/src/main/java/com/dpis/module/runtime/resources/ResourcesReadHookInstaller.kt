package com.dpis.module.runtime.resources


import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.diagnostics.RuntimeEvents
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.diagnostics.device.RuntimeTransport
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import com.dpis.module.runtime.hookapi.ModernApiCapabilitiesResolver
import com.dpis.module.runtime.probe.RuntimeHotPathEvidenceSampler
import com.dpis.module.viewport.RelativeViewportOwnership
import com.dpis.module.viewport.ResourcesMetricsReadReuse
import com.dpis.module.viewport.ViewportConfigurationScope
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.window.WindowBoundsState
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.Volatile

object ResourcesReadHookInstaller {
    @Volatile
    private var hookInstalled = false

    @Volatile
    private var viewportReadHandlingEnabled = true

    @Volatile
    private var configurationFontOverrideEnabled = true

    @Volatile
    private var metricsTargetFontOverrideEnabled = false
    private val INTERNAL_UPDATE = ThreadLocal<Boolean>()
    private val LAST_MESSAGES = ConcurrentHashMap<String, String>()
    private val HOTPATH_SAMPLER = RuntimeHotPathEvidenceSampler()
    private val reusedDisplayMetricsHits = AtomicInteger()

    internal fun retainPublishedDisplay(
        store: DpisConfigStore?,
        packageName: String?,
        config: Configuration,
        routeName: String,
        sourceTag: String,
        mode: String,
    ): Boolean {
        val densityBefore = config.densityDpi
        if (!RelativeViewportOwnership.retainPublishedDisplay(store, packageName, config) ||
            config.densityDpi == densityBefore
        ) {
            return false
        }
        RuntimeHotPathEvents.applied(
            packageName,
            "viewport",
            routeName,
            "source=" + sourceTag
                    + ", mode=" + mode + ", scope=display"
                    + ", widthDp=" + config.screenWidthDp + "->" + config.screenWidthDp
                    + ", heightDp=" + config.screenHeightDp + "->" + config.screenHeightDp
                    + ", smallestWidthDp=" + config.smallestScreenWidthDp
                    + "->" + config.smallestScreenWidthDp
                    + ", densityDpi=" + densityBefore + "->" + config.densityDpi,
        )
        return true
    }

    @JvmStatic
    fun resetForHotReload() {
        hookInstalled = false
        ResourcesMetricsReadReuse.clearForTest()
    }

    @Throws(ReflectiveOperationException::class)
    @JvmStatic
    fun install(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore,
        viewportHandlingEnabled: Boolean
    ) {
        install(
            xposed, packageName, store, ResourcesReadHookPolicy(
                viewportHandlingEnabled,
                true,
                false
            ), ModernApiCapabilitiesResolver.fromXposed(xposed)
        )
    }

    @Throws(ReflectiveOperationException::class)
    @JvmStatic
    fun install(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore,
        viewportHandlingEnabled: Boolean = true,
        fontConfigurationOverrideEnabled: Boolean = false
    ) {
        install(
            xposed, packageName, store, ResourcesReadHookPolicy(
                viewportHandlingEnabled,
                fontConfigurationOverrideEnabled,
                false
            ), ModernApiCapabilitiesResolver.fromXposed(xposed)
        )
    }

    @Throws(ReflectiveOperationException::class)
    @JvmStatic
    fun install(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore,
        policy: ResourcesReadHookPolicy?,
        apiCapabilities: ModernApiCapabilities
    ) {
        install(
            xposed, packageName, store, HookRuntimePolicy.fromStore(store), policy,
            apiCapabilities
        )
    }

    @JvmStatic
    @Throws(ReflectiveOperationException::class)
    fun install(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore?,
        runtimePolicy: HookRuntimePolicy?,
        policy: ResourcesReadHookPolicy?,
        apiCapabilities: ModernApiCapabilities
    ) {
        if (hookInstalled) {
            return
        }
        synchronized(ResourcesReadHookInstaller::class.java) {
            if (hookInstalled) {
                return
            }
            val resolvedPolicy = if (policy != null)
                policy
            else
                ResourcesReadHookPolicy.FULL
            viewportReadHandlingEnabled = resolvedPolicy.viewportHandlingEnabled
            configurationFontOverrideEnabled =
                resolvedPolicy.configurationFontOverrideEnabled
            metricsTargetFontOverrideEnabled =
                resolvedPolicy.metricsTargetFontOverrideEnabled
            val bootClassLoader = ClassLoader.getSystemClassLoader()
            val resourcesClass =
                Class.forName("android.content.res.Resources", false, bootClassLoader)

            val getConfigurationMethod = resourcesClass.getDeclaredMethod("getConfiguration")
            apiCapabilities.applyStableHookId<HookBuilder>(
                xposed.hook(getConfigurationMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                "resources_read_get_configuration"
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val result = chain!!.proceed()
                    if (result !is Configuration) {
                        return@Hooker result
                    }
                    // Skip override work when this getConfiguration call is a
                    // re-entrant read from our own getDisplayMetrics/getSystem
                    // hooks. Those paths run their own explicit overrides and
                    // want the raw system configuration as the event-gate
                    // observation baseline, so re-applying here is redundant
                    // (writes are idempotent) and avoids feeding DPIS output
                    // back in as input on a hot path.
                    if (true == INTERNAL_UPDATE.get()) {
                        return@Hooker result
                    }
                    val thisObject = chain.thisObject
                    applyConfigurationOverride(
                        if (thisObject is Resources) thisObject else null,
                        result, packageName, store,
                        "ResourcesRead(getConfiguration)",
                        null,
                        viewportReadHandlingEnabled,
                        configurationFontOverrideEnabled,
                        runtimePolicy
                    )
                    result
                })
            val getDisplayMetricsMethod = resourcesClass.getDeclaredMethod("getDisplayMetrics")
            apiCapabilities.applyStableHookId<HookBuilder>(
                xposed.hook(getDisplayMetricsMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                "resources_read_get_display_metrics"
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val result = chain!!.proceed()
                    if (result !is DisplayMetrics) {
                        return@Hooker result
                    }
                    if (true == INTERNAL_UPDATE.get()) {
                        return@Hooker result
                    }
                    val thisObject = chain.thisObject
                    if (thisObject !is Resources) {
                        return@Hooker result
                    }
                    INTERNAL_UPDATE.set(true)
                    val captureActive = RuntimeTransport.isCaptureActive
                    val startedAt = if (captureActive) System.nanoTime() else 0L
                    var reusedStableRead = false
                    try {
                        if (ResourcesMetricsReadReuse.matchesDisplayMetrics(
                                thisObject,
                                result.densityDpi,
                                result.density.toBits(),
                                result.scaledDensity.toBits(),
                                result.widthPixels,
                                result.heightPixels,
                                System.nanoTime(),
                            )
                        ) {
                            reusedStableRead = true
                            return@Hooker result
                        }
                        val config = thisObject.configuration
                        applyMetricsOverride(
                            thisObject,
                            result,
                            config,
                            packageName,
                            store,
                            viewportReadHandlingEnabled,
                            metricsTargetFontOverrideEnabled
                        )
                    } finally {
                        INTERNAL_UPDATE.set(false)
                        val durationNs = if (captureActive) System.nanoTime() - startedAt else 0L
                        if (captureActive) {
                            RuntimeHotPathEvents.recordBodySample(
                                packageName,
                                "resources_read_display_metrics_override",
                                durationNs,
                            )
                            if (reusedStableRead) {
                                recordReusedDisplayMetricsSkip(packageName)
                            }
                        }
                    }
                    result
                })
            val getSystemMethod = resourcesClass.getDeclaredMethod("getSystem")
            apiCapabilities.applyStableHookId<HookBuilder>(
                xposed.hook(getSystemMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                "resources_read_get_system"
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val result = chain!!.proceed()
                    if (result !is Resources) {
                        return@Hooker result
                    }
                    // Suppress the getConfiguration/getDisplayMetrics hooks for
                    // the internal reads below; this method applies both
                    // overrides explicitly, so without the guard each read would
                    // re-trigger the other hooks and run override work twice on
                    // a hot path.
                    val reentrant = true == INTERNAL_UPDATE.get()
                    if (!reentrant) {
                        INTERNAL_UPDATE.set(true)
                    }
                    try {
                        val config = result.configuration
                        applyConfigurationOverride(
                            result, config, packageName, store,
                            "ResourcesRead(getSystem)",
                            null,
                            // Resources.getSystem() is global system
                            // resources, not an app-scoped viewport. Applying
                            // the app ratio here feeds transformed density
                            // back into Display and compounds the scale.
                            false,
                            configurationFontOverrideEnabled,
                            runtimePolicy
                        )
                        val metrics = result.displayMetrics
                        applyMetricsOverride(
                            result,
                            metrics,
                            config,
                            packageName,
                            store,
                            false,
                            metricsTargetFontOverrideEnabled
                        )
                    } finally {
                        if (!reentrant) {
                            INTERNAL_UPDATE.set(false)
                        }
                    }
                    result
                })
            hookInstalled = true
            DpisLog.i("Resources read hook ready")
            RuntimeEvents.recordHotReload(
                packageName,
                "resources",
                "installed",
                "resources read hook ready"
            )
        }
    }

    @JvmStatic
    fun applyConfigurationOverride(
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore,
        sourceTag: String
    ) {
        applyConfigurationOverride(null, config, packageName, store, sourceTag)
    }

    @JvmStatic
    fun applyConfigurationOverride(
        resourceScope: Any?,
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore,
        sourceTag: String
    ) {
        applyConfigurationOverride(
            resourceScope,
            config,
            packageName,
            store,
            sourceTag,
            null,
            true,
            true,
            HookRuntimePolicy.fromStore(store)
        )
    }

    @JvmStatic
    fun applyConfigurationOverrideForTest(
        resourceScope: Any?,
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore,
        sourceTag: String,
        windowScoped: Boolean
    ) {
        applyConfigurationOverride(
            resourceScope,
            config,
            packageName,
            store,
            sourceTag,
            windowScoped,
            true,
            true,
            HookRuntimePolicy.fromStore(store)
        )
    }

    @JvmStatic
    @JvmOverloads
    fun applyConfigurationOverrideForTest(
        resourceScope: Any?,
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore?,
        sourceTag: String,
        windowScoped: Boolean,
        viewportHandlingEnabled: Boolean,
        fontConfigurationOverrideEnabled: Boolean = true,
        policy: HookRuntimePolicy? = HookRuntimePolicy.fromStore(store)
    ) {
        applyConfigurationOverride(
            resourceScope,
            config,
            packageName,
            store,
            sourceTag,
            windowScoped,
            viewportHandlingEnabled,
            fontConfigurationOverrideEnabled,
            policy
        )
    }

    private fun applyConfigurationOverride(
        resourceScope: Any?,
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore?,
        sourceTag: String,
        windowScopedOverride: Boolean?,
        viewportHandlingEnabled: Boolean = true,
        fontConfigurationOverrideEnabled: Boolean = true,
        policy: HookRuntimePolicy? = HookRuntimePolicy.fromStore(store)
    ) {
        ResourcesConfigurationRead.apply(
            resourceScope,
            config,
            packageName,
            store,
            sourceTag,
            windowScopedOverride,
            viewportHandlingEnabled,
            fontConfigurationOverrideEnabled,
            policy,
        )
    }

    @JvmStatic
    fun applyMetricsOverride(
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?
    ) {
        applyMetricsOverride(null, metrics, config, packageName)
    }

    @JvmStatic
    @JvmOverloads
    fun applyMetricsOverride(
        resourceScope: Any?,
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore? = null
    ) {
        applyMetricsOverride(
            resourceScope, metrics, config, packageName,
            isWindowScoped(packageName, config), store, true, false
        )
    }

    private fun applyMetricsOverride(
        resourceScope: Any?,
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore?,
        viewportHandlingEnabled: Boolean,
        metricsTargetFontOverrideEnabled: Boolean
    ) {
        applyMetricsOverride(
            resourceScope, metrics, config, packageName,
            isWindowScoped(packageName, config), store, viewportHandlingEnabled,
            metricsTargetFontOverrideEnabled
        )
    }

    @JvmStatic
    fun applyMetricsOverrideForTest(
        resourceScope: Any?,
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?,
        windowScoped: Boolean
    ) {
        applyMetricsOverride(resourceScope, metrics, config, packageName, windowScoped)
    }

    @JvmStatic
    fun applyMetricsOverrideForTest(
        resourceScope: Any?,
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?,
        windowScoped: Boolean,
        store: DpisConfigStore?
    ) {
        applyMetricsOverride(
            resourceScope,
            metrics,
            config,
            packageName,
            windowScoped,
            store,
            true,
            false
        )
    }

    @JvmStatic
    fun applyMetricsOverrideForTest(
        resourceScope: Any?,
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?,
        windowScoped: Boolean,
        store: DpisConfigStore?,
        viewportHandlingEnabled: Boolean
    ) {
        applyMetricsOverride(
            resourceScope,
            metrics,
            config,
            packageName,
            windowScoped,
            store,
            viewportHandlingEnabled,
            false
        )
    }

    @JvmStatic
    fun applyMetricsOverrideForTest(
        resourceScope: Any?,
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?,
        windowScoped: Boolean,
        store: DpisConfigStore?,
        viewportHandlingEnabled: Boolean,
        metricsTargetFontOverrideEnabled: Boolean
    ) {
        applyMetricsOverride(
            resourceScope,
            metrics,
            config,
            packageName,
            windowScoped,
            store,
            viewportHandlingEnabled,
            metricsTargetFontOverrideEnabled
        )
    }

    private fun applyMetricsOverride(
        resourceScope: Any?,
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?,
        windowScoped: Boolean,
        store: DpisConfigStore? = null,
        viewportHandlingEnabled: Boolean = true,
        metricsTargetFontOverrideEnabled: Boolean = false
    ) {
        ResourcesMetricsRead.apply(
            resourceScope,
            metrics,
            config,
            packageName,
            windowScoped,
            store,
            viewportHandlingEnabled,
            metricsTargetFontOverrideEnabled,
        )
    }

    private fun isWindowScoped(packageName: String?, config: Configuration?): Boolean {
        return ViewportConfigurationScope.isWindowScoped(config)
                || WindowBoundsState.hasActiveWindow(packageName)
                || WindowBoundsState.matchesWindowConfiguration(packageName, config)
    }

    internal fun recordMetricsSkip(packageName: String?, reason: String?, detail: String?) {
        val sample =
            HOTPATH_SAMPLER.sample("skip|metrics|" + packageName + "|" + reason, detail)
        if (sample.emit) {
            RuntimeHotPathEvents.skipped(
                packageName,
                "viewport",
                "resources_read_display_metrics_override",
                sample.detail
            )
        }
    }

    internal fun recordConfigurationSkip(
        packageName: String?,
        reason: String,
        detail: String,
    ) {
        val sample = HOTPATH_SAMPLER.sample(
            "skip|configuration|" + packageName + "|" + reason,
            detail,
        )
        if (sample.emit) {
            RuntimeHotPathEvents.skipped(
                packageName,
                "viewport",
                "resources_read_configuration_override",
                sample.detail,
            )
        }
    }

    @JvmStatic
    fun resetHotPathSamplerForTest() {
        HOTPATH_SAMPLER.resetForTest()
        reusedDisplayMetricsHits.set(0)
        ResourcesMetricsReadReuse.clearForTest()
    }

    @JvmStatic
    fun recordReusedDisplayMetricsSkip(packageName: String?) {
        if (!RuntimeTransport.isCaptureActive) {
            return
        }
        val hit = reusedDisplayMetricsHits.incrementAndGet()
        if (hit % RuntimeHotPathEvents.BODY_SAMPLE_STRIDE != 0) {
            return
        }
        RuntimeHotPathEvents.skipped(
            packageName,
            "viewport",
            "resources_read_display_metrics_override",
            "source=ResourcesRead(getDisplayMetrics), reason=reused_stable_metrics, hitCount=$hit",
        )
    }

    internal fun logIfChanged(key: String?, message: String): Boolean {
        if (key == null) {
            DpisLog.i(message)
            return true
        }
        val previous = LAST_MESSAGES.put(key, message)
        if (message != previous) {
            DpisLog.i(message)
            return true
        }
        return false
    }

    internal fun describeConfiguration(config: Configuration?): String {
        if (config == null) {
            return "null"
        }
        return ("{widthDp=" + config.screenWidthDp
                + ",heightDp=" + config.screenHeightDp
                + ",smallestWidthDp=" + config.smallestScreenWidthDp
                + ",densityDpi=" + config.densityDpi
                + ",fontScale=" + config.fontScale + "}")
    }

    internal fun describeViewportResult(result: ViewportOverride.Result?): String {
        if (result == null) {
            return "null"
        }
        return ("{widthDp=" + result.widthDp
                + ",heightDp=" + result.heightDp
                + ",smallestWidthDp=" + result.smallestWidthDp
                + ",densityDpi=" + result.densityDpi + "}")
    }

    internal fun describeNullable(value: Int?): String {
        return if (value == null) "none" else value.toString()
    }

}
