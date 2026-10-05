package com.dpis.module.runtime.appprocess

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.config.ModulePackagePlan
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.fonts.hookdomain.FontHookArbitration
import com.dpis.module.hooks.FontMode
import com.dpis.module.hooks.HookExecutionPlan
import com.dpis.module.hooks.HookExecutionPlanner
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.ConfigStoreFactory
import com.dpis.module.runtime.font.ActivityThreadFontHookInstaller
import com.dpis.module.runtime.font.ComposeFontRuntimeDiagnosticsInstaller
import com.dpis.module.runtime.font.DebugFontOverride
import com.dpis.module.runtime.font.FlutterSettingsFontHookInstaller
import com.dpis.module.runtime.font.ForceTextSizeHookInstaller
import com.dpis.module.runtime.font.HyperOsFlutterFontHookInstaller
import com.dpis.module.runtime.font.TypefaceOverrideHookInstaller
import com.dpis.module.runtime.font.WebViewFontHookInstaller
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import com.dpis.module.runtime.hookapi.ModernApiCapabilitiesResolver
import com.dpis.module.runtime.probe.DebugPackageOverride
import com.dpis.module.runtime.probe.RuntimeDiagnosticLogFingerprint
import com.dpis.module.viewport.AppProcessViewportStateSeeder
import io.github.libxposed.api.XposedInterface

object AppProcessHookInstaller {
    private const val PROP_FORCE_FLUTTER_SETTINGS_PACKAGE =
        "debug.dpis.font.force_flutter_settings_package"
    private const val PROP_FLUTTER_SETTINGS_ONLY_PACKAGE =
        "debug.dpis.font.flutter_settings_only_package"
    private const val PROP_DISABLE_TEXTVIEW_ABSOLUTE_REWRITE_PACKAGE =
        "debug.dpis.font.disable_textview_absolute_rewrite_package"
    private const val PROP_DISABLE_ACTIVITY_THREAD_PACKAGE =
        "debug.dpis.font.disable_activity_thread_package"
    private const val PROP_DISABLE_VIEWPORT_DISPLAY_SUPPLEMENT_PACKAGE =
        "debug.dpis.viewport.disable_display_supplement_package"
    private const val PROP_DISABLE_VIEWPORT_RESOURCES_IMPL_PACKAGE =
        "debug.dpis.viewport.disable_resources_impl_package"
    private const val PROP_DISABLE_VIEWPORT_RESOURCES_READ_PACKAGE =
        "debug.dpis.viewport.disable_resources_read_package"

    class FontHookPlan(
        @JvmField val emulationEnabled: Boolean,
        @JvmField val fieldRewriteEnabled: Boolean,
    )

    @JvmStatic
    @Throws(Throwable::class)
    fun install(
        xposed: XposedInterface,
        store: DpisConfigStore?,
        policy: HookRuntimePolicy?,
        packagePlan: ModulePackagePlan,
        apiCapabilities: ModernApiCapabilities,
    ) {
        val packageName = packagePlan.packageName
        val debugOverride = resolveDebugFontOverrideForPackage(packageName)
        val runtimePolicy = policy
        val plan = packagePlan.buildExecutionPlan(requireNotNull(runtimePolicy), debugOverride)
        DpisLog.i(
            "DPIS_FONT app hook plan: package=" + packageName +
                    ", " + RuntimeDiagnosticLogFingerprint.field() +
                    ", fontScaleActive=" + packagePlan.fontScaleActive +
                    ", fontMode=" + packagePlan.targetFontMode +
                    ", resolvedFontMode=" + plan.resolvedFontMode +
                    ", resolvedViewportMode=" + plan.resolvedViewportMode +
                    ", domain=" + plan.fontDomainPlan.reason +
                    ", flutterSettings=" + plan.flutterSettingsEnabled +
                    ", hyperOsNativeFlutter=" + plan.hyperOsNativeFlutterEnabled +
                    ", debugForceFlutterSettings=" + plan.debugForceFlutterSettings +
                    ", debugFlutterSettingsOnly=" + plan.debugFlutterSettingsOnly +
                    ", debugDisableTextViewAbsoluteRewrite=" + plan.debugDisableTextViewAbsoluteRewrite +
                    ", debugDisableActivityThreadFont=" + plan.debugDisableActivityThreadFont +
                    ", hookDomains=" + plan.hookDomains +
                    ", hookDomainSource=" + plan.hookDomainSource +
                    ", builtinDomains=" + plan.builtinDomains +
                    ", unknownCustomDomains=" + plan.unknownCustomDomains +
                    ", reason={" + plan.reason.formatForLog() + "}",
        )
        if (packagePlan.typefaceEnabled) {
            installTypefaceHooks(xposed, packageName, store, packagePlan.targetTypefaceId)
            HyperOsFlutterFontHookInstaller.installTypefaceProbe(xposed, packageName, store)
        }
        if (plan.viewportEnabled) {
            AppProcessViewportStateSeeder.seedDisplayBaseline(
                packageName,
                packagePlan.targetViewportSpec,
                packagePlan.targetViewportMode,
                policy == null || policy.systemServerHooksEnabled,
            )
        }
        WebApkRuntimeOwnerBridge.installLifecycleHooks(xposed, packageName, apiCapabilities)
        installFromPlan(xposed, packageName, store, policy, plan, apiCapabilities)
        if (plan.probeHooksRequested) {
            DpisLog.i(
                "hooks installed (full): viewportEnabled=" + plan.viewportEnabled +
                        ", viewportMode=" + packagePlan.targetViewportMode +
                        ", fontMode=" + packagePlan.targetFontMode + " for " + packageName,
            )
            return
        }
        DpisLog.i(
            "hooks installed (" + plan.probeInstallMode + "): viewportEnabled=" + plan.viewportEnabled +
                    ", viewportMode=" + packagePlan.targetViewportMode +
                    ", fontMode=" + packagePlan.targetFontMode +
                    ", fontDomainPlan=" + plan.fontDomainPlan.reason +
                    ", resourcesFont=" + plan.fontDomainPlan.resourcesFontEnabled +
                    ", textViewSpRewrite=" + plan.fontDomainPlan.textViewSpRewriteEnabled +
                    ", textViewAbsoluteRewrite=" + plan.fontDomainPlan.textViewAbsoluteRewriteEnabled +
                    ", textViewCurrentPxFallback=" + plan.fontDomainPlan.textViewCurrentPxFallbackEnabled +
                    ", paintFallback=" + plan.fontDomainPlan.paintFallbackEnabled +
                    ", flutterSettings=" + plan.fontDomainPlan.flutterSettingsEnabled +
                    ", hyperOsNativeFlutter=" + plan.fontDomainPlan.hyperOsNativeFlutterEnabled +
                    ", genericNativeFlutter=" + plan.fontDomainPlan.genericNativeFlutterEnabled +
                    ", hookDomains=" + plan.hookDomains +
                    ", hookDomainSource=" + plan.hookDomainSource +
                    ", builtinDomains=" + plan.builtinDomains +
                    ", unknownCustomDomains=" + plan.unknownCustomDomains +
                    ", debugForceFlutterSettings=" + plan.debugForceFlutterSettings +
                    ", debugFlutterSettingsOnly=" + plan.debugFlutterSettingsOnly +
                    ", debugDisableTextViewAbsoluteRewrite=" + plan.debugDisableTextViewAbsoluteRewrite +
                    ", debugDisableActivityThreadFont=" + plan.debugDisableActivityThreadFont +
                    " for " + packageName,
        )
    }

    @JvmStatic
    fun shouldInstallProbeHooks(policy: HookRuntimePolicy?): Boolean {
        return policy != null && policy.probeHooksEnabled
    }

    @JvmStatic
    fun resolveProbeInstallMode(policy: HookRuntimePolicy?): String {
        return HookExecutionPlanner.resolveProbeInstallMode(policy)
    }

    @JvmStatic
    fun resolveViewportHookEnabled(
        policy: HookRuntimePolicy?,
        viewportConfigured: Boolean,
        viewportMode: String?,
    ): Boolean {
        return HookExecutionPlanner.resolveViewportHookEnabled(
            policy,
            viewportConfigured,
            viewportMode,
        )
    }

    @JvmStatic
    fun resolveFontDomainPlan(fontHookPlan: FontHookPlan?): FontHookArbitration.FontDomainPlan {
        return resolveFontDomainPlan(fontHookPlan, false)
    }

    @JvmStatic
    fun resolveFontDomainPlan(
        fontHookPlan: FontHookPlan?,
        hyperOsNativeFlutterEnabled: Boolean,
    ): FontHookArbitration.FontDomainPlan {
        return resolveFontDomainPlan(fontHookPlan, false, hyperOsNativeFlutterEnabled)
    }

    @JvmStatic
    fun resolveFontDomainPlan(
        fontHookPlan: FontHookPlan?,
        flutterSettingsEnabled: Boolean,
        hyperOsNativeFlutterEnabled: Boolean,
    ): FontHookArbitration.FontDomainPlan {
        return FontHookArbitration.resolveDomainPlan(
            fontHookPlan != null &&
                    (fontHookPlan.emulationEnabled || fontHookPlan.fieldRewriteEnabled),
            fontHookPlan != null && fontHookPlan.fieldRewriteEnabled,
            flutterSettingsEnabled,
            hyperOsNativeFlutterEnabled,
        )
    }

    @JvmStatic
    fun resolveResourcesHooksEnabled(
        viewportEnabled: Boolean,
        fontHookPlan: FontHookPlan?,
        domainPlan: FontHookArbitration.FontDomainPlan?,
    ): Boolean {
        return viewportEnabled ||
                (fontHookPlan != null && fontHookPlan.emulationEnabled) ||
                (domainPlan != null && domainPlan.resourcesFontEnabled)
    }

    @JvmStatic
    fun isDebugPropertyPackageMatchForTest(
        propertyName: String?,
        packageName: String?,
        propertyValue: String?,
    ): Boolean {
        return DebugPackageOverride.matchesForTest(propertyName, packageName, propertyValue)
    }

    @JvmStatic
    fun resolveFontHookPlan(
        policy: HookRuntimePolicy?,
        fontScaleActive: Boolean,
        fontMode: String?,
    ): FontHookPlan {
        val resolved = HookExecutionPlanner.resolveFontMode(policy, fontScaleActive, fontMode)
        return FontHookPlan(
            resolved == FontMode.EMULATION,
            resolved == FontMode.FIELD_REWRITE,
        )
    }

    @JvmStatic
    fun resolveDebugFontOverrideForPackage(packageName: String?): DebugFontOverride {
        val debugFlutterSettingsOnly = isDebugPropertyPackageMatch(
            PROP_FLUTTER_SETTINGS_ONLY_PACKAGE,
            packageName,
        )
        val debugForceFlutterSettings = debugFlutterSettingsOnly ||
                isDebugPropertyPackageMatch(PROP_FORCE_FLUTTER_SETTINGS_PACKAGE, packageName)
        val disableTextViewAbsoluteRewrite = isDebugPropertyPackageMatch(
            PROP_DISABLE_TEXTVIEW_ABSOLUTE_REWRITE_PACKAGE,
            packageName,
        )
        val disableActivityThreadFont = isDebugPropertyPackageMatch(
            PROP_DISABLE_ACTIVITY_THREAD_PACKAGE,
            packageName,
        )
        return DebugFontOverride.of(
            debugForceFlutterSettings,
            debugFlutterSettingsOnly,
            disableTextViewAbsoluteRewrite,
            disableActivityThreadFont,
        )
    }

    private fun installFromPlan(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore?,
        policy: HookRuntimePolicy?,
        plan: HookExecutionPlan,
        apiCapabilities: ModernApiCapabilities,
    ) {
        if (ComposeFontRuntimeDiagnosticsInstaller.shouldInstall(plan)) {
            ComposeFontRuntimeDiagnosticsInstaller.install(
                xposed,
                packageName,
                store,
                plan.fontDomainPlan,
                plan.hookDomains,
                plan.hookDomainSource,
            )
        }
        if (plan.resourcesHooksEnabled) {
            if (plan.resourcesWriteHooksEnabled) {
                ResourcesManagerHookInstaller.install(
                    xposed,
                    packageName,
                    store,
                    policy,
                    apiCapabilities,
                )
            } else {
                DpisLog.i("Resources write hooks skipped: package=$packageName")
            }
            if (!shouldInstallResourcesImplHook(plan)) {
                DpisLog.i("ResourcesImpl hook skipped: package=$packageName")
            } else if (isDebugPropertyPackageMatch(
                    PROP_DISABLE_VIEWPORT_RESOURCES_IMPL_PACKAGE,
                    packageName,
                )
            ) {
                DpisLog.i("ResourcesImpl hook skipped by debug property for $packageName")
            } else {
                ResourcesImplHookInstaller.install(
                    xposed,
                    packageName,
                    store,
                    policy,
                    apiCapabilities,
                )
            }
            if (!plan.resourcesReadHooksEnabled) {
                DpisLog.i("ResourcesRead hook skipped: package=$packageName")
            } else if (isDebugPropertyPackageMatch(
                    PROP_DISABLE_VIEWPORT_RESOURCES_READ_PACKAGE,
                    packageName,
                )
            ) {
                DpisLog.i("ResourcesRead hook skipped by debug property for $packageName")
            } else {
                ResourcesReadHookInstaller.install(
                    xposed,
                    packageName,
                    store,
                    policy,
                    plan.resourcesReadPolicy,
                    apiCapabilities,
                )
            }
        }
        if (plan.activityThreadFontEnabled) {
            ActivityThreadFontHookInstaller.install(xposed, packageName, store, apiCapabilities)
        }
        if (plan.textViewHooksEnabled) {
            ForceTextSizeHookInstaller.install(
                xposed,
                requireNotNull(packageName),
                store,
                plan.fontDomainPlan,
                apiCapabilities,
            )
        }
        if (plan.flutterSettingsEnabled) {
            DpisLog.i("DPIS_FONT installing Flutter settings font hooks for $packageName")
            FlutterSettingsFontHookInstaller.install(
                xposed,
                packageName,
                store,
                plan.fontDomainPlan,
            )
        }
        if (plan.hyperOsNativeFlutterEnabled) {
            DpisLog.i("DPIS_FONT installing HyperOS native Flutter font hooks for $packageName")
            HyperOsFlutterFontHookInstaller.install(xposed, packageName, store)
        }
        if (plan.webViewTextZoomEnabled) {
            WebViewFontHookInstaller.install(xposed, packageName, store, apiCapabilities)
        }
        if (!isViewportDisplaySupplementDisabled(packageName)) {
            // Keep 102 replaceable anchors for the small viewport supplement hooks.
            // Recording window bounds is required in system mode: system_server
            // copies the display result onto a smaller window, and the app
            // process needs those pixels to restore the window height. Frame
            // replacement stays off, so the rect itself remains system-owned.
            // Display metrics still have to be installed: React Native copies
            // Display.getRealMetrics density for drawing and Resources
            // scaledDensity for measuring, and leaves the text clipped when
            // those two differ.
            if (shouldInstallAppProcessViewportHooks(plan)) {
                WindowMetricsHookInstaller.install(xposed, packageName)
                DisplayHookInstaller.install(xposed, packageName, store)
            }
        }
        if (plan.resourcesProbeEnabled) {
            ResourcesProbeHookInstaller.install(xposed, packageName, store, policy)
        }
        if (plan.viewportProbeEnabled) {
            WindowManagerProbeHookInstaller.install(xposed, packageName)
            WindowSessionProbeHookInstaller.install(xposed)
            ViewRootProbeHookInstaller.install(xposed, packageName)
        }
    }

    @JvmStatic
    fun shouldInstallAppProcessViewportSupplementHooksForTest(plan: HookExecutionPlan?): Boolean {
        return shouldInstallAppProcessViewportHooks(plan)
    }

    @JvmStatic
    fun shouldInstallDisplayMetricsHooksForTest(plan: HookExecutionPlan?): Boolean {
        return shouldInstallAppProcessViewportHooks(plan)
    }

    @JvmStatic
    fun shouldInstallResourcesImplHookForTest(plan: HookExecutionPlan?): Boolean {
        return shouldInstallResourcesImplHook(plan)
    }

    private fun shouldInstallAppProcessViewportHooks(plan: HookExecutionPlan?): Boolean {
        return plan != null && plan.viewportEnabled
    }

    private fun shouldInstallResourcesImplHook(plan: HookExecutionPlan?): Boolean {
        return plan != null && (plan.resourcesWriteHooksEnabled || plan.resourcesImplHookEnabled)
    }

    private fun isViewportDisplaySupplementDisabled(packageName: String?): Boolean {
        return isDebugPropertyPackageMatch(
            PROP_DISABLE_VIEWPORT_DISPLAY_SUPPLEMENT_PACKAGE,
            packageName,
        )
    }

    @JvmStatic
    fun installTypefaceHooks(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore?,
        targetTypefaceId: String?,
    ) {
        try {
            DpisLog.i(
                "DPIS_FONT_STYLE install requested: package=$packageName, targetTypefaceId=$targetTypefaceId",
            )
            TypefaceOverrideHookInstaller.install(
                xposed,
                requireNotNull(packageName),
                targetTypefaceId,
                store,
                ConfigStoreFactory.createFontLibraryForXposedHost(xposed),
                ModernApiCapabilitiesResolver.fromXposed(xposed),
            )
        } catch (throwable: Throwable) {
            DpisLog.e("failed to install typeface hooks: package=$packageName", throwable)
        }
    }

    private fun isDebugPropertyPackageMatch(propertyName: String, packageName: String?): Boolean {
        return DebugPackageOverride.matches(propertyName, packageName)
    }
}
