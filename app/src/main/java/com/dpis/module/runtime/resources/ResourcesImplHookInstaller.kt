package com.dpis.module.runtime.resources

import android.content.res.Configuration
import android.util.DisplayMetrics
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import com.dpis.module.viewport.ResourcesMetricsReadReuse
import com.dpis.module.viewport.ViewportTargetResolution
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker
import kotlin.concurrent.Volatile

object ResourcesImplHookInstaller {
    @Volatile
    private var hookInstalled = false

    @JvmStatic
    fun resetForHotReload() {
        hookInstalled = false
        ResourcesMetricsReadReuse.clearForTest()
    }

    @JvmStatic
    @Throws(ReflectiveOperationException::class)
    fun install(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore?,
        policy: HookRuntimePolicy?,
        apiCapabilities: ModernApiCapabilities,
    ) {
        if (hookInstalled) {
            return
        }
        synchronized(ResourcesImplHookInstaller::class.java) {
            if (hookInstalled) {
                return
            }
            val bootClassLoader = ClassLoader.getSystemClassLoader()
            val resourcesImplClass = Class.forName(
                "android.content.res.ResourcesImpl", false, bootClassLoader,
            )
            val compatibilityInfoClass = Class.forName(
                "android.content.res.CompatibilityInfo", false, bootClassLoader,
            )
            val method = resourcesImplClass.getDeclaredMethod(
                "updateConfiguration", Configuration::class.java, DisplayMetrics::class.java,
                compatibilityInfoClass,
            )
            apiCapabilities.applyStableHookId<HookBuilder>(
                xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                "resources_impl_update_configuration",
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val config = chain!!.getArg(0) as Configuration?
                    val metrics = chain.getArg(1) as DisplayMetrics?
                    ResourcesMetricsReadReuse.bump()
                    applyDensityOverride(packageName, config, metrics, store, policy)
                    chain.proceed()
                })
            hookInstalled = true
            DpisLog.i("ResourcesImpl hook ready")
        }
    }

    @JvmStatic
    @JvmOverloads
    fun applyDensityOverride(
        packageName: String?,
        config: Configuration?,
        metrics: DisplayMetrics?,
        store: DpisConfigStore?,
        policy: HookRuntimePolicy? = null,
    ) {
        ResourcesImplOverrideEngine.apply(packageName, config, metrics, store, policy)
    }

    @JvmStatic
    fun applyDensityOverrideForTest(
        packageName: String?,
        config: Configuration?,
        metrics: DisplayMetrics?,
        store: DpisConfigStore?,
        windowScoped: Boolean,
    ) {
        ResourcesImplOverrideEngine.applyForTest(
            packageName, config, metrics, store, windowScoped,
        )
    }

    @JvmStatic
    fun shouldPublishResourcesImplResultForTest(
        packageName: String?,
        resolution: ViewportTargetResolution?,
        needsViewportUpdate: Boolean,
    ): Boolean = ResourcesImplOverrideEngine.shouldPublishResourcesImplResultForTest(
        packageName, resolution, needsViewportUpdate,
    )
}
