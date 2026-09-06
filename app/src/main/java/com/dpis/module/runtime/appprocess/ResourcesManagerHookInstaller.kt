package com.dpis.module.runtime.appprocess

import android.annotation.SuppressLint
import android.content.res.Configuration
import com.dpis.module.DpisConfigStore
import com.dpis.module.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.DebugPackageOverride
import com.dpis.module.runtime.RuntimeHotPathEvidenceSampler
import com.dpis.module.runtime.font.FontScaleOverride
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import com.dpis.module.viewport.TargetViewportWidthResolver
import com.dpis.module.viewport.ViewportConfigurationScope.isWindowScoped
import com.dpis.module.viewport.ViewportModePolicy
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportResolvedTarget
import com.dpis.module.viewport.ViewportRuntimeMarkerProbe
import com.dpis.module.viewport.ViewportRuntimeRecord
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.VirtualDisplayPlan
import com.dpis.module.viewport.VirtualDisplayState
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.Volatile

object ResourcesManagerHookInstaller {
    private const val HOOK_ID_APPLY_CONFIGURATION =
        "resources_manager_apply_configuration_to_resources"
    private const val HOOK_ID_UPDATE_RESOURCES_FOR_ACTIVITY =
        "resources_manager_update_resources_for_activity"
    private const val HOOK_ID_RESOURCE_CREATION_PREFIX =
        "resources_manager_resource_creation_config"
    private const val HOOK_ID_RESOURCES_KEY_PREFIX = "resources_manager_create_resources_impl_key"
    private const val PROP_DISABLE_VIEWPORT_RESOURCES_MANAGER_KEY_PACKAGE =
        "debug.dpis.viewport.disable_resources_manager_key_package"

    @Volatile
    private var hookInstalled = false
    private val LAST_MESSAGES: MutableMap<String?, String?> = ConcurrentHashMap<String?, String?>()
    private val HOTPATH_SAMPLER = RuntimeHotPathEvidenceSampler()

    @JvmStatic
    fun resetForHotReload() {
        hookInstalled = false
    }

    @Throws(ReflectiveOperationException::class)
    @JvmStatic
    fun install(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore,
        apiCapabilities: ModernApiCapabilities
    ) {
        install(xposed, packageName, store, HookRuntimePolicy.fromStore(store), apiCapabilities)
    }

    @Throws(ReflectiveOperationException::class)
    @JvmStatic
    fun install(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore?,
        policy: HookRuntimePolicy?,
        apiCapabilities: ModernApiCapabilities
    ) {
        if (hookInstalled) {
            return
        }
        synchronized(ResourcesManagerHookInstaller::class.java) {
            if (hookInstalled) {
                return
            }
            val bootClassLoader = ClassLoader.getSystemClassLoader()
            val resourcesManagerClass = Class.forName(
                "android.app.ResourcesManager", false, bootClassLoader
            )
            val compatibilityInfoClass = Class.forName(
                "android.content.res.CompatibilityInfo", false, bootClassLoader
            )
            val applyConfigurationMethod = resourcesManagerClass.getDeclaredMethod(
                "applyConfigurationToResources", Configuration::class.java, compatibilityInfoClass
            )
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(applyConfigurationMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_APPLY_CONFIGURATION
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val config = chain!!.getArg(0) as Configuration?
                    applyResourceOverrides(
                        config, store, packageName, "ResourcesManager",
                        policy
                    )
                    chain.proceed()
                })

            val updateResourcesForActivityMethod = resolveUpdateResourcesForActivityMethod(
                resourcesManagerClass, bootClassLoader
            )
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(updateResourcesForActivityMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_UPDATE_RESOURCES_FOR_ACTIVITY
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val overrideConfig = chain!!.getArg(1) as Configuration?
                    applyResourceOverrides(
                        overrideConfig, store, packageName,
                        "ResourcesManagerActivity", policy
                    )
                    chain.proceed()
                })

            val createHookCount = installResourceCreationHooks(
                xposed, resourcesManagerClass, packageName, store, policy, apiCapabilities
            )
            val keyHookCount = installResourcesKeyHooks(
                xposed, resourcesManagerClass, packageName, store, policy, apiCapabilities
            )
            hookInstalled = true
            DpisLog.i(
                ("ResourcesManager hook ready (createHooks=" + createHookCount
                        + ", keyHooks=" + keyHookCount + ")")
            )
        }
    }

    @SuppressLint("BlockedPrivateApi")
    @Throws(ReflectiveOperationException::class)
    private fun resolveUpdateResourcesForActivityMethod(
        resourcesManagerClass: Class<*>,
        bootClassLoader: ClassLoader?
    ): Method {
        // Xposed module runtime depends on this hidden framework method to keep
        // activity-scoped resource overrides aligned with viewport spoofing.
        val iBinderClass = Class.forName("android.os.IBinder", false, bootClassLoader)
        return resourcesManagerClass.getDeclaredMethod(
            "updateResourcesForActivity",
            iBinderClass,
            Configuration::class.java,
            Int::class.javaPrimitiveType
        )
    }

    private fun installResourceCreationHooks(
        xposed: XposedInterface,
        resourcesManagerClass: Class<*>,
        packageName: String?,
        store: DpisConfigStore?,
        policy: HookRuntimePolicy?,
        apiCapabilities: ModernApiCapabilities
    ): Int {
        var hookedCount = 0
        val hookedMethods: MutableSet<Method?> = HashSet<Method?>()
        for (method in resourcesManagerClass.declaredMethods) {
            val configArgIndex = findConfigurationArgIndex(method)
            if (configArgIndex < 0) {
                continue
            }
            val methodName = method.name
            if (!isResourceCreationMethod(methodName)) {
                continue
            }
            if (!hookedMethods.add(method)) {
                continue
            }
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_RESOURCE_CREATION_PREFIX + "#" + method.toGenericString()
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val config = chain!!.getArg(configArgIndex) as Configuration?
                    applyResourceOverrides(
                        config, store, packageName,
                        "ResourcesManagerCreate(" + methodName + ")", policy
                    )
                    chain.proceed()
                })
            hookedCount++
        }
        return hookedCount
    }

    private fun installResourcesKeyHooks(
        xposed: XposedInterface,
        resourcesManagerClass: Class<*>,
        packageName: String?,
        store: DpisConfigStore?,
        policy: HookRuntimePolicy?,
        apiCapabilities: ModernApiCapabilities
    ): Int {
        var hookedCount = 0
        val hookedMethods: MutableSet<Method?> = HashSet<Method?>()
        for (method in resourcesManagerClass.declaredMethods) {
            val methodName = method.name
            if (("createResourcesImpl" != methodName) || !hasResourcesKeyFirstArg(method) || !hookedMethods.add(
                    method
                )
            ) {
                continue
            }
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_RESOURCES_KEY_PREFIX + "#" + method.toGenericString()
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val key = chain!!.getArg(0)
                    maybeApplyKeyOverride(
                        chain.thisObject, key, store, packageName, methodName,
                        policy
                    )
                    chain.proceed()
                })
            hookedCount++
        }
        return hookedCount
    }

    @JvmOverloads
    @JvmStatic
    fun maybeApplyKeyOverride(
        resourcesManager: Any?,
        key: Any?,
        store: DpisConfigStore?,
        packageName: String?,
        sourceTag: String,
        policy: HookRuntimePolicy? = HookRuntimePolicy.fromStore(store)
    ) {
        var store = store
        var packageName = packageName
        packageName = WebApkRuntimeOwnerBridge.resolveEffectivePackage(store, packageName)
        store = WebApkRuntimeOwnerBridge.resolveEffectiveStore(store, packageName)
        if (resourcesManager == null || key == null) {
            recordViewportSkip(
                packageName, "resources_manager_key_override",
                "missing_resources_manager_or_key",
                ("source=ResourcesManagerKey(" + sourceTag
                        + "), reason=missing_resources_manager_or_key")
            )
            return
        }
        if (!ViewportModePolicy.shouldApplyConfigurationOverride(policy, store, packageName)) {
            recordViewportSkip(
                packageName, "resources_manager_key_override",
                "configuration_override_disabled",
                ("source=ResourcesManagerKey(" + sourceTag
                        + "), reason=configuration_override_disabled")
            )
            return
        }
        if (DebugPackageOverride.matches(
                PROP_DISABLE_VIEWPORT_RESOURCES_MANAGER_KEY_PACKAGE, packageName
            )
        ) {
            logIfChanged(
                packageName + ":ResourcesManagerKey(" + sourceTag + "):debug-skip",
                ("ResourcesManagerKey(" + sourceTag
                        + ") skipped by debug property for " + packageName)
            )
            return
        }
        val override = readField(key, "mOverrideConfiguration")
        if (override !is Configuration) {
            recordViewportSkip(
                packageName, "resources_manager_key_override",
                "missing_override_configuration",
                ("source=ResourcesManagerKey(" + sourceTag
                        + "), reason=missing_override_configuration")
            )
            return
        }
        val baseConfig = readResourcesManagerConfiguration(resourcesManager)
        if (baseConfig == null) {
            recordViewportSkip(
                packageName, "resources_manager_key_override",
                "missing_base_configuration",
                ("source=ResourcesManagerKey(" + sourceTag
                        + "), reason=missing_base_configuration")
            )
            return
        }
        if (!shouldReplaceResourcesKeyOverride(override, baseConfig)) {
            recordViewportSkip(
                packageName, "resources_manager_key_override",
                "preserve_existing_override",
                ("source=ResourcesManagerKey(" + sourceTag
                        + "), reason=preserve_existing_override")
            )
            return
        }
        val targetConfig = Configuration()
        val sourceConfig = if (isEffectivelyEmpty(override)) baseConfig else override
        if (!isEffectivelyEmpty(override)
            && shouldPreserveWindowLikeResourcesKeyOverride(
                sourceConfig, store, packageName, sourceTag
            )
        ) {
            recordViewportSkip(
                packageName, "resources_manager_key_override",
                "preserve_window_like_override",
                ("source=ResourcesManagerKey(" + sourceTag
                        + "), reason=preserve_window_like_override")
            )
            return
        }
        copyViewportConfiguration(sourceConfig, targetConfig)
        targetConfig.fontScale = sourceConfig.fontScale
        applyResourceOverrides(
            targetConfig, store, packageName,
            "ResourcesManagerKey(" + sourceTag + ")", policy
        )
        if (!hasViewportOverride(targetConfig, sourceConfig)) {
            recordViewportSkip(
                packageName, "resources_manager_key_override",
                "no_viewport_delta_after_resolution",
                ("source=ResourcesManagerKey(" + sourceTag
                        + "), reason=no_viewport_delta_after_resolution")
            )
            return
        }
        copyViewportConfiguration(targetConfig, override)
    }

    private fun readResourcesManagerConfiguration(resourcesManager: Any): Configuration? {
        try {
            val method = resourcesManager.javaClass.getDeclaredMethod("getConfiguration")
            method.isAccessible = true
            val result = method.invoke(resourcesManager)
            if (result is Configuration) {
                return result
            }
        } catch (ignored: ReflectiveOperationException) {
        } catch (ignored: RuntimeException) {
        }
        val config = readField(resourcesManager, "mResConfiguration")
        return if (config is Configuration) config else null
    }

    private fun isEffectivelyEmpty(config: Configuration?): Boolean {
        return config != null && config.screenWidthDp <= 0 && config.screenHeightDp <= 0 && config.smallestScreenWidthDp <= 0 && config.densityDpi <= 0
    }

    private fun shouldReplaceResourcesKeyOverride(
        overrideConfig: Configuration?,
        baseConfig: Configuration?
    ): Boolean {
        if (overrideConfig == null || baseConfig == null) {
            return false
        }
        if (isEffectivelyEmpty(overrideConfig)) {
            return true
        }
        val hasViewportFields =
            overrideConfig.screenWidthDp > 0 || overrideConfig.screenHeightDp > 0 || overrideConfig.smallestScreenWidthDp > 0 || overrideConfig.densityDpi > 0
        if (!hasViewportFields) {
            return true
        }
        val sameBounds = (overrideConfig.screenWidthDp <= 0
                || overrideConfig.screenWidthDp == baseConfig.screenWidthDp)
                && (overrideConfig.screenHeightDp <= 0
                || overrideConfig.screenHeightDp == baseConfig.screenHeightDp)
                && (overrideConfig.smallestScreenWidthDp <= 0
                || overrideConfig.smallestScreenWidthDp == baseConfig.smallestScreenWidthDp)
        val sameDensity = overrideConfig.densityDpi <= 0
                || overrideConfig.densityDpi == baseConfig.densityDpi
        return sameBounds && sameDensity
    }

    private fun shouldPreserveWindowLikeResourcesKeyOverride(
        sourceConfig: Configuration?,
        store: DpisConfigStore?,
        packageName: String?,
        sourceTag: String
    ): Boolean {
        if (sourceConfig == null || sourceConfig.screenWidthDp <= 0 || sourceConfig.screenHeightDp <= 0 || sourceConfig.smallestScreenWidthDp <= 0 || sourceConfig.densityDpi <= 0) {
            return false
        }
        val source = ViewportSourceSnapshot.fromConfiguration(
            ViewportSourceSnapshot.ORIGIN_RESOURCES_MANAGER, sourceConfig, null
        )
        val resolution =
            TargetViewportWidthResolver.resolve(store, packageName, source)
        if (resolution == null || resolution.record == null) {
            return false
        }
        val displayResult =
            ViewportResolvedTarget.viewportResult(resolution, false)
        if (displayResult == null || displayResult.widthDp <= 0 || displayResult.heightDp <= 0 || displayResult.smallestWidthDp <= 0 || displayResult.densityDpi <= 0) {
            return false
        }
        val targetWidthAndDensity =
            sourceConfig.screenWidthDp == displayResult.widthDp && sourceConfig.smallestScreenWidthDp == displayResult.smallestWidthDp && sourceConfig.densityDpi == displayResult.densityDpi
        val shorterThanDisplayTarget = sourceConfig.screenHeightDp < displayResult.heightDp
        if (!targetWidthAndDensity || !shorterThanDisplayTarget) {
            return false
        }
        val message = ("DPIS_VIEWPORT ResourcesManagerKey(" + sourceTag
                + ") preserve window-like key: package=" + packageName
                + ", source=" + describeConfiguration(sourceConfig)
                + ", displayTarget=" + describeViewportResult(displayResult))
        logIfChanged(packageName + ":" + sourceTag + ":preserve-window-like-key", message)
        return true
    }

    private fun hasViewportOverride(target: Configuration?, source: Configuration?): Boolean {
        return target != null && source != null && (target.screenWidthDp != source.screenWidthDp || target.screenHeightDp != source.screenHeightDp || target.smallestScreenWidthDp != source.smallestScreenWidthDp || target.densityDpi != source.densityDpi)
    }

    private fun copyViewportConfiguration(source: Configuration, target: Configuration) {
        target.screenWidthDp = source.screenWidthDp
        target.screenHeightDp = source.screenHeightDp
        target.smallestScreenWidthDp = source.smallestScreenWidthDp
        target.densityDpi = source.densityDpi
    }

    private fun hasResourcesKeyFirstArg(method: Method): Boolean {
        val parameterTypes = method.parameterTypes
        return parameterTypes.size > 0
                && "android.content.res.ResourcesKey" == parameterTypes[0]!!.name
    }

    private fun findConfigurationArgIndex(method: Method): Int {
        val parameterTypes = method.parameterTypes
        for (i in parameterTypes.indices) {
            if (Configuration::class.java == parameterTypes[i]) {
                return i
            }
        }
        return -1
    }

    private fun isResourceCreationMethod(methodName: String?): Boolean {
        return methodName != null
                && (methodName.contains("createResources")
                || methodName.contains("getOrCreateResources")
                || methodName.contains("createBaseTokenResources"))
    }

    @JvmOverloads
    @JvmStatic
    fun applyResourceOverrides(
        config: Configuration?,
        store: DpisConfigStore?,
        packageName: String?,
        sourceTag: String,
        policy: HookRuntimePolicy? = HookRuntimePolicy.fromStore(store)
    ) {
        var store = store
        var packageName = packageName
        packageName = WebApkRuntimeOwnerBridge.resolveEffectivePackage(store, packageName)
        store = WebApkRuntimeOwnerBridge.resolveEffectiveStore(store, packageName)
        if (config == null) {
            recordViewportSkip(
                packageName, "resources_manager_config_override",
                "null_configuration",
                "source=" + sourceTag + ", reason=null_configuration"
            )
            return
        }
        val fontScale = FontScaleOverride.resolveForResources(
            store, packageName, config.fontScale
        )
        FontScaleOverride.applyToConfiguration(config, fontScale)
        val originalWidthDp = config.screenWidthDp
        val originalHeightDp = config.screenHeightDp
        val originalSmallestWidthDp = config.smallestScreenWidthDp
        val originalDensityDpi = config.densityDpi
        if (originalWidthDp <= 0 && originalHeightDp <= 0 && originalDensityDpi <= 0) {
            if (fontScale.changed) {
                val fontMessage = ("DPIS_FONT " + sourceTag + " override: package="
                        + packageName + ", fontScale "
                        + fontScale.original + " -> " + config.fontScale)
                logIfChanged(packageName + ":" + sourceTag + ":font-only", fontMessage)
            } else {
                recordViewportSkip(
                    packageName, "resources_manager_config_override",
                    "empty_viewport_configuration",
                    "source=" + sourceTag + ", reason=empty_viewport_configuration"
                )
            }
            return
        }
        val source = ViewportSourceSnapshot.fromConfiguration(
            ViewportSourceSnapshot.ORIGIN_RESOURCES_MANAGER, config, null
        )
        val resolution =
            TargetViewportWidthResolver.resolve(store, packageName, source)
        val targetViewportWidth = if (resolution.hasTarget())
            resolution.effectiveSmallestWidthDp
        else
            null
        if (targetViewportWidth != null && resolution.spec.isEnabled) {
            ViewportRuntimeMarkerProbe.observeAppProcessProbe(
                packageName, resolution.spec, sourceTag
            )
        }
        val windowScoped = isWindowScoped(config)
        val stableTarget =
            ViewportResolvedTarget.virtualDisplayResult(resolution, targetViewportWidth)
        val resolvedRecordResult =
            ViewportResolvedTarget.viewportResult(resolution, windowScoped, config)
        val result = if (resolvedRecordResult != null)
            resolvedRecordResult
        else
            ViewportOverride.derive(
                config,
                if (targetViewportWidth != null) targetViewportWidth else 0,
                windowScoped,
                stableTarget
            )
        if (result == null) {
            if (fontScale.changed) {
                val fontMessage = ("DPIS_FONT " + sourceTag + " override: package="
                        + packageName + ", fontScale "
                        + fontScale.original + " -> " + config.fontScale)
                logIfChanged(packageName + ":" + sourceTag + ":font-only", fontMessage)
            } else {
                recordViewportSkip(
                    packageName, "resources_manager_config_override",
                    "no_viewport_result",
                    ("source=" + sourceTag
                            + ", reason=no_viewport_result"
                            + ", targetViewportWidthDp="
                            + describeNullable(targetViewportWidth))
                )
            }
            return
        }
        val sharedResult = VirtualDisplayPlan.derivePublishableResult(
            originalWidthDp,
            originalHeightDp,
            originalSmallestWidthDp,
            originalDensityDpi,
            0,
            0,
            result.smallestWidthDp
        )
        if (!windowScoped && resolution.spec.isEnabled && source != null) {
            var canPublishRecord = true
            if (sharedResult != null) {
                canPublishRecord = VirtualDisplayState.setUnlessDerivedFromTargetConfig(
                    sharedResult, originalSmallestWidthDp, targetViewportWidth
                )
            }
            if (canPublishRecord) {
                VirtualDisplayState.publish(
                    packageName,
                    resolution.spec,
                    source,
                    result,
                    sharedResult,
                    ViewportRuntimeRecord.PROVENANCE_APP_PROCESS
                )
            }
        }
        val needsViewportUpdate =
            result.widthDp != originalWidthDp || result.heightDp != originalHeightDp || result.smallestWidthDp != originalSmallestWidthDp || (result.densityDpi > 0 && result.densityDpi != originalDensityDpi)
        val applyToConfiguration = ViewportModePolicy.shouldApplyConfigurationOverride(
            policy, store, packageName, resolution, needsViewportUpdate
        )
        if (!needsViewportUpdate
            && !fontScale.changed
        ) {
            val stableResult =
                VirtualDisplayState.getStableTargetResult(
                    originalSmallestWidthDp, targetViewportWidth
                )
            var stableTargetApplied = false
            if (result.densityDpi <= 0 && stableResult != null && stableResult.densityDpi > 0 && config.densityDpi != stableResult.densityDpi) {
                config.densityDpi = stableResult.densityDpi
                stableTargetApplied = true
                val message = ("DPIS_VIEWPORT " + sourceTag
                        + " stable target: package=" + packageName
                        + ", targetViewportWidthDp=" + describeNullable(targetViewportWidth)
                        + ", actual=" + describeConfiguration(config)
                        + ", widthDp " + originalWidthDp
                        + " -> " + config.screenWidthDp
                        + ", heightDp " + originalHeightDp
                        + " -> " + config.screenHeightDp
                        + ", smallestWidthDp " + originalSmallestWidthDp
                        + " -> " + config.smallestScreenWidthDp
                        + ", densityDpi " + originalDensityDpi
                        + " -> " + config.densityDpi
                        + ", fontScale " + fontScale.original
                        + " -> " + config.fontScale)
                logIfChanged(packageName + ":" + sourceTag + ":stable-target", message)
            }
            if (!stableTargetApplied) {
                recordViewportSkip(
                    packageName, "resources_manager_config_override",
                    "stable_configuration",
                    ("source=" + sourceTag
                            + ", reason=stable_configuration"
                            + ", targetViewportWidthDp="
                            + describeNullable(targetViewportWidth))
                )
            }
            return
        }
        if (applyToConfiguration && needsViewportUpdate) {
            ViewportOverride.apply(config, result)
        }
        val modeLabel = if (applyToConfiguration) "config" else "metrics"
        val detail = ("source=" + sourceTag
                + ", mode=" + modeLabel
                + ", scope=" + (if (windowScoped) "window" else "display")
                + ", targetViewportWidthDp=" + describeNullable(targetViewportWidth)
                + ", widthDp=" + originalWidthDp + "->" + result.widthDp
                + ", heightDp=" + originalHeightDp + "->" + result.heightDp
                + ", smallestWidthDp=" + originalSmallestWidthDp + "->"
                + result.smallestWidthDp
                + ", densityDpi=" + originalDensityDpi + "->" + result.densityDpi
                + ", fontScale=" + fontScale.original + "->" + config.fontScale)
        val message = ("DPIS_VIEWPORT " + sourceTag + " (" + modeLabel
                + ") override: package=" + packageName
                + ", targetViewportWidthDp=" + describeNullable(targetViewportWidth)
                + ", scope=" + (if (windowScoped) "window" else "display")
                + ", target=" + describeViewportResult(result)
                + ", actual=" + describeConfiguration(config)
                + ", widthDp "
                + originalWidthDp + " -> " + result.widthDp
                + ", heightDp " + originalHeightDp + " -> " + result.heightDp
                + ", smallestWidthDp " + originalSmallestWidthDp + " -> "
                + result.smallestWidthDp
                + ", densityDpi " + originalDensityDpi + " -> "
                + result.densityDpi
                + ", fontScale " + fontScale.original + " -> " + config.fontScale)
        if (logIfChanged(packageName + ":" + sourceTag, message)) {
            RuntimeHotPathEvents.applied(
                packageName,
                "viewport",
                "resources_manager_config_override",
                detail
            )
        }
    }

    private fun logIfChanged(key: String?, message: String): Boolean {
        val previous = LAST_MESSAGES.put(key, message)
        if (message != previous) {
            DpisLog.i(message)
            return true
        }
        return false
    }

    private fun recordViewportSkip(
        packageName: String?,
        routeName: String?,
        reason: String?,
        detail: String?
    ) {
        val sample =
            HOTPATH_SAMPLER.sample(
                "skip|" + packageName + "|" + routeName + "|" + reason,
                detail
            )
        if (sample.emit) {
            RuntimeHotPathEvents.skipped(
                packageName,
                "viewport",
                routeName,
                sample.detail
            )
        }
    }

    @JvmStatic
    fun resetHotPathSamplerForTest() {
        HOTPATH_SAMPLER.resetForTest()
    }

    private fun readField(target: Any?, fieldName: String): Any? {
        if (target == null) {
            return null
        }
        try {
            val field = target.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            return field.get(target)
        } catch (ignored: ReflectiveOperationException) {
            return null
        } catch (ignored: RuntimeException) {
            return null
        }
    }

    private fun describeConfiguration(config: Configuration?): String {
        if (config == null) {
            return "null"
        }
        return ("{widthDp=" + config.screenWidthDp
                + ",heightDp=" + config.screenHeightDp
                + ",smallestWidthDp=" + config.smallestScreenWidthDp
                + ",densityDpi=" + config.densityDpi
                + ",fontScale=" + config.fontScale + "}")
    }

    private fun describeViewportResult(result: ViewportOverride.Result?): String {
        if (result == null) {
            return "null"
        }
        return ("{widthDp=" + result.widthDp
                + ",heightDp=" + result.heightDp
                + ",smallestWidthDp=" + result.smallestWidthDp
                + ",densityDpi=" + result.densityDpi + "}")
    }

    private fun describeNullable(value: Int?): String {
        return if (value == null) "none" else value.toString()
    }
}
