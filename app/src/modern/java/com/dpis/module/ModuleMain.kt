package com.dpis.module

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import com.dpis.module.config.ConfigSnapshot
import com.dpis.module.config.ConfigSnapshotLoader
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.config.ModulePackagePlan
import com.dpis.module.config.RuntimePropertyConfigPreferences
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.diagnostics.RuntimeBridgeEvents
import com.dpis.module.diagnostics.RuntimeEvents
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.fonts.PublishedFontFileResolver
import com.dpis.module.hooks.HookExecutionPlan
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.hooks.SystemScopeCoordinator
import com.dpis.module.runtime.ConfigStoreFactory
import com.dpis.module.runtime.application.AppProcessHookInstaller
import com.dpis.module.runtime.application.AppProcessHotReloadResetter
import com.dpis.module.runtime.chromium.ChromiumViewportProbeHookInstaller
import com.dpis.module.runtime.font.DebugFontOverride
import com.dpis.module.runtime.font.FlutterSettingsFontHookInstaller
import com.dpis.module.runtime.font.HyperOsFlutterFontHookInstaller
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import com.dpis.module.runtime.hookapi.ModernApiCapabilitiesResolver
import com.dpis.module.runtime.lifecycle.ModuleRuntimeStateReporter
import com.dpis.module.runtime.lifecycle.XposedSelfActivation
import com.dpis.module.runtime.probe.DebugPackageOverride
import com.dpis.module.runtime.systemserver.HookDiagnostics
import com.dpis.module.runtime.systemserver.HookInstaller
import com.dpis.module.runtime.systemserver.HostProcess
import com.dpis.module.runtime.systemserver.MutationPolicy
import com.dpis.module.runtime.webapk.WebApkRuntimeOwnerBridge
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

class ModuleMain : XposedModule() {
    @Volatile
    private var configStore: DpisConfigStore? = null
    @Volatile
    private var modernApiCapabilities: ModernApiCapabilities? = null
    @Volatile
    private var moduleLoadedObserved = false
    @Volatile
    private var systemServerInstallAttempted = false
    @Volatile
    private var firstPackageReadyLogged = false
    @Volatile
    private var appProcessInstallAttempted = false
    @Volatile
    private var currentProcessName = "unknown"
    @Volatile
    private var lastPackageReadyPackageName: String? = null
    @Volatile
    private var lastPackageReadyClassLoader: ClassLoader? = null
    @Volatile
    private var lastPackageReadyApplicationInfo: ApplicationInfo? = null

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        moduleLoadedObserved = true
        currentProcessName = param.processName
        val store = ConfigStoreFactory.createForXposedHost(this)
        configStore = store
        DpisLog.setLoggingEnabled(store.isGlobalLogEnabled())
        val message = "module loaded: process=${param.processName}, marker=${HookDiagnostics.BUILD_MARKER}"
        if (HostProcess.isSystemServer(param.processName, "")) {
            ModuleRuntimeStateReporter.reportSystemServerLoaded()
        }
        HookDiagnostics.recordPending(message)
        DpisLog.i(message)
        log(android.util.Log.INFO, BRIDGE_LOG_PREFIX + message)
        try {
            ModernAppSpecificRouteInstaller.handleModuleLoaded(this, param.processName)
            if (HostProcess.isSystemServer(param.processName, "")) {
                val policy = resolveSystemServerRuntimePolicy(store, param.processName)
                maybeInstallSystemServerHooks(
                    store,
                    policy,
                    param.processName,
                    "android",
                    null,
                    "module-loaded",
                )
            }
            maybeInstallAppProcessFromModuleLoaded(store, param.processName)
        } catch (throwable: Throwable) {
            rawBridgeLog(
                "module-loaded app hook install failed: process=${param.processName}, " +
                    "error=${throwable.javaClass.name}: ${throwable.message}",
            )
            DpisLog.e("module-loaded app hook install failed: process=${param.processName}", throwable)
        }
    }

    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        val store = getOrCreateConfigStore()
        var processName = currentProcessName
        if (!HostProcess.isSystemServer(processName, "android")) {
            processName = "system"
        }
        val policy = resolveSystemServerRuntimePolicy(store, processName)
        bridgeRuntimeLog(
            "system_server starting hook install enter: process=$processName, " +
                "classLoader=${describeClassLoader(param)}",
        )
        maybeInstallSystemServerHooks(
            store,
            policy,
            processName,
            "android",
            param.classLoader,
            "system-server-starting",
        )
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        val processName = resolveCurrentProcessName()
        log(
            android.util.Log.INFO,
            BRIDGE_LOG_PREFIX + "onPackageLoaded enter: process=$processName, package=${param.packageName}",
        )
        val store = getOrCreateConfigStore()
        try {
            maybeInstallAppProcessFromPackageLoaded(store, processName, param.packageName)
        } catch (throwable: Throwable) {
            rawBridgeLog(
                "package-loaded app hook install failed: process=$processName, " +
                    "package=${param.packageName}, error=${throwable.javaClass.name}: " +
                    throwable.message,
            )
            DpisLog.e(
                "package-loaded app hook install failed: process=$processName, package=${param.packageName}",
                throwable,
            )
        }
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        log(
            android.util.Log.INFO,
            BRIDGE_LOG_PREFIX + "onPackageReady enter: process=$currentProcessName, package=${param.packageName}",
        )
        XposedSelfActivation.markIfSelfPackage(
            param.packageName,
            param.classLoader,
            "libxposed-package-ready",
        )
        rememberPackageReady(param.packageName, param.classLoader, param.applicationInfo)
        val store = getOrCreateConfigStore()
        val snapshot = ConfigSnapshotLoader.fromStore(store)
        val systemPolicy = resolveSystemServerRuntimePolicy(store, currentProcessName)
        val appProcessPolicy = resolveHookedRuntimePolicy(store)
        DpisLog.setLoggingEnabled(appProcessPolicy.globalLogEnabled)
        log(
            android.util.Log.INFO,
            BRIDGE_LOG_PREFIX + "package ready: process=$currentProcessName, package=${param.packageName}",
        )
        HookDiagnostics.flushPending()
        maybeInstallSystemServerHooks(
            store,
            systemPolicy,
            currentProcessName,
            param.packageName,
            null,
            "package-ready",
        )
        maybeLogFirstPackageReady(param.packageName)
        if (ModernAppSpecificRouteInstaller.handlePackageReady(this, param, currentProcessName)) {
            return
        }
        installAppProcessHooksIfConfigured(
            store,
            appProcessPolicy,
            snapshot,
            param.packageName,
            "package-ready",
        )
        installChromiumViewportProbe(param.packageName, param.classLoader)
        retryTypefaceHooksWithPackageReady(store, snapshot, param.packageName)
        retryFlutterHooksWithAppClassLoader(
            store,
            snapshot,
            param.classLoader,
            param.packageName,
        )
        retryTypefaceFlutterProbe(snapshot, param.classLoader, param.packageName)
    }

    override fun onHotReloading(param: XposedModuleInterface.HotReloadingParam): Boolean {
        log(
            android.util.Log.INFO,
            BRIDGE_LOG_PREFIX + "hot reload begin: process=$currentProcessName",
        )
        RuntimeEvents.recordHotReload(
            currentProcessName,
            "runtime",
            "begin",
            "hot reload begin: process=$currentProcessName",
        )
        param.setSavedInstanceState(arrayOf(
            currentProcessName,
            lastPackageReadyPackageName,
            lastPackageReadyClassLoader,
            lastPackageReadyApplicationInfo,
        ))
        return true
    }

    override fun onHotReloaded(param: XposedModuleInterface.HotReloadedParam) {
        val replayState = restoreHotReloadState(param.getSavedInstanceState())
        currentProcessName = replayState.processName ?: currentProcessName
        rememberPackageReady(
            replayState.packageName,
            replayState.classLoader,
            replayState.applicationInfo,
        )
        firstPackageReadyLogged = false
        appProcessInstallAttempted = false
        AppProcessHotReloadResetter.resetAll()
        val store = getOrCreateConfigStore()
        try {
            log(
                android.util.Log.INFO,
                BRIDGE_LOG_PREFIX + "hot reload replay: process=$currentProcessName, " +
                    "systemServerAttempted=$systemServerInstallAttempted, " +
                    "appAttempted=$appProcessInstallAttempted",
            )
            RuntimeEvents.recordHotReload(
                currentProcessName,
                "runtime",
                "replay",
                "hot reload replay: process=$currentProcessName, " +
                    "systemServerAttempted=$systemServerInstallAttempted, " +
                    "appAttempted=$appProcessInstallAttempted",
            )
            replaySystemServerAfterHotReload(store, currentProcessName)
            maybeInstallAppProcessFromModuleLoaded(store, currentProcessName)
            replayPackageReadySupplementsAfterHotReload(
                store,
                replayState.packageName,
                replayState.classLoader,
                replayState.applicationInfo,
            )
        } finally {
            log(
                android.util.Log.INFO,
                BRIDGE_LOG_PREFIX + "hot reload end: process=$currentProcessName",
            )
            RuntimeEvents.recordHotReload(
                currentProcessName,
                "runtime",
                "end",
                "hot reload end: process=$currentProcessName",
            )
        }
    }

    private fun replaySystemServerAfterHotReload(store: DpisConfigStore, processName: String) {
        if (!HostProcess.isSystemServer(processName, "")) {
            DpisLog.i("system_server hot reload skipped: process=$processName")
            return
        }
        bridgeHotReloadLog("system_server hot reload replay enter: process=$processName")
        HookInstaller.resetForHotReload()
        systemServerInstallAttempted = false
        val policy = resolveSystemServerRuntimePolicy(store, processName)
        maybeInstallSystemServerHooks(
            store,
            policy,
            processName,
            "android",
            null,
            "hot-reload",
        )
    }

    private fun rememberPackageReady(
        packageName: String?,
        classLoader: ClassLoader?,
        applicationInfo: ApplicationInfo?,
    ) {
        if (packageName == null || classLoader == null) {
            return
        }
        lastPackageReadyPackageName = packageName
        lastPackageReadyClassLoader = classLoader
        lastPackageReadyApplicationInfo = applicationInfo
    }

    private companion object {
        const val BRIDGE_LOG_PREFIX = "DPIS "
        const val PROP_CHROMIUM_VIEWPORT_PROBE_PACKAGE =
            "debug.dpis.webapk.chromium_probe_package"

        fun restoreHotReloadState(savedState: Any?): PackageReadyReplayState {
            if (savedState is Array<*>) {
                val processName = savedState.getOrNull(0) as? String
                val packageName = savedState.getOrNull(1) as? String
                val classLoader = savedState.getOrNull(2) as? ClassLoader
                val applicationInfo = savedState.getOrNull(3) as? ApplicationInfo
                return PackageReadyReplayState(processName, packageName, classLoader, applicationInfo)
            }
            return PackageReadyReplayState(savedState as? String, null, null, null)
        }

        fun isKnownProcessName(processName: String?): Boolean =
            !processName.isNullOrBlank() && processName != "unknown"

        fun runtimeProcessName(): String? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val processName = Application.getProcessName()
                if (isKnownProcessName(processName)) {
                    return processName
                }
            }
            return readProcSelfCmdline()
        }

        fun readProcSelfCmdline(): String? {
            return try {
                val bytes = java.nio.file.Files.readAllBytes(File("/proc/self/cmdline").toPath())
                val length = bytes.indexOf(0)
                    .takeIf { it >= 0 }
                    ?: bytes.size
                String(bytes, 0, length, StandardCharsets.UTF_8).trim()
            } catch (_: IOException) {
                null
            } catch (_: RuntimeException) {
                null
            }
        }

        fun packageNameFromProcessName(processName: String?): String? {
            if (processName == null) {
                return null
            }
            val separator = processName.indexOf(':')
            return if (separator > 0) processName.substring(0, separator) else processName
        }

        fun shouldSuppressSecondaryProcessViewport(
            processName: String?,
            packagePlan: ModulePackagePlan?,
        ): Boolean {
            if (processName.isNullOrBlank() || packagePlan?.packageName.isNullOrBlank()) {
                return false
            }
            return processName != packagePlan!!.packageName &&
                !processName.startsWith(packagePlan.packageName + ":") &&
                packagePlan.viewportEnabled
        }

        fun describeClassLoader(param: XposedModuleInterface.SystemServerStartingParam?): String =
            param?.classLoader?.javaClass?.name ?: "null"
    }

    private fun replayPackageReadySupplementsAfterHotReload(
        store: DpisConfigStore,
        packageName: String?,
        classLoader: ClassLoader?,
        applicationInfo: ApplicationInfo?,
    ) {
        if (packageName == null || classLoader == null) {
            bridgeHotReloadLog(
                "package-ready hot reload replay skipped: process=$currentProcessName, " +
                    "package=$packageName, classLoaderMissing=${classLoader == null}",
            )
            return
        }
        if (HostProcess.isSystemServer(currentProcessName, packageName)) {
            bridgeHotReloadLog(
                "package-ready hot reload replay skipped system process: " +
                    "process=$currentProcessName, package=$packageName",
            )
            return
        }
        bridgeHotReloadLog(
            "package-ready hot reload replay enter: process=$currentProcessName, " +
                "package=$packageName, classLoader=${classLoader.javaClass.name}",
        )
        val snapshot = ConfigSnapshotLoader.fromStore(store)
        ModernAppSpecificRouteInstaller.handlePackageReadyReplay(
            this,
            packageName,
            classLoader,
            applicationInfo,
            currentProcessName,
        )
        retryTypefaceHooksWithPackageReady(store, snapshot, packageName)
        installChromiumViewportProbe(packageName, classLoader)
        retryFlutterHooksWithAppClassLoader(store, snapshot, classLoader, packageName)
        retryTypefaceFlutterProbe(snapshot, classLoader, packageName)
    }

    private fun installChromiumViewportProbe(packageName: String?, classLoader: ClassLoader?) {
        if (WebApkRuntimeOwnerBridge.CHROME_PACKAGE != packageName ||
            classLoader == null ||
            !DebugPackageOverride.matches(PROP_CHROMIUM_VIEWPORT_PROBE_PACKAGE, packageName)
        ) {
            return
        }
        try {
            ChromiumViewportProbeHookInstaller.install(this, classLoader)
        } catch (throwable: Throwable) {
            DpisLog.e("DPIS_WEBAPK Chromium viewport probe failed", throwable)
        }
    }

    private fun maybeInstallAppProcessFromModuleLoaded(
        store: DpisConfigStore,
        processName: String,
    ) {
        rawBridgeLog("module-loaded app hook install enter: process=$processName")
        if (HostProcess.isSystemServer(processName, "")) {
            rawBridgeLog("module-loaded app hook install skipped system process: process=$processName")
            return
        }
        val packageName = packageNameFromProcessName(processName)
        var runtimeStore = createModuleLoadedAppProcessStore(store, packageName)
        var snapshot = ConfigSnapshotLoader.fromStore(runtimeStore)
        var source = "module-loaded"
        if (!snapshot.isConfigured(packageName)) {
            val fallbackSnapshot = ConfigSnapshotLoader.fromStore(store)
            if (!fallbackSnapshot.isConfigured(packageName)) {
                rawBridgeLog(
                    "module-loaded app config unavailable: process=$processName, package=$packageName, " +
                        "propertyPackages=${snapshot.getConfiguredPackages()}, " +
                        "fallbackPackages=${fallbackSnapshot.getConfiguredPackages()}",
                )
                return
            }
            runtimeStore = store
            snapshot = fallbackSnapshot
            source = "module-loaded-fallback"
            rawBridgeLog(
                "module-loaded app config fallback: process=$processName, package=$packageName, " +
                    "packages=${snapshot.getConfiguredPackages()}",
            )
        }
        val policy = resolveHookedRuntimePolicy(store)
        DpisLog.setLoggingEnabled(policy.globalLogEnabled)
        if (ModernAppSpecificRouteInstaller.shouldSuppressModuleLoadedGenericHooks(packageName, processName)) {
            return
        }
        installAppProcessHooksIfConfigured(runtimeStore, policy, snapshot, packageName, source)
    }

    private fun maybeInstallAppProcessFromPackageLoaded(
        store: DpisConfigStore,
        processName: String,
        packageName: String,
    ) {
        rawBridgeLog("package-loaded app hook install enter: process=$processName, package=$packageName")
        if (HostProcess.isSystemServer(processName, packageName)) {
            rawBridgeLog("package-loaded app hook install skipped system process: process=$processName, package=$packageName")
            return
        }
        val snapshot = ConfigSnapshotLoader.fromStore(store)
        val policy = resolveHookedRuntimePolicy(store)
        DpisLog.setLoggingEnabled(policy.globalLogEnabled)
        if (ModernAppSpecificRouteInstaller.shouldSuppressModuleLoadedGenericHooks(packageName, processName)) {
            return
        }
        installAppProcessHooksIfConfigured(store, policy, snapshot, packageName, "package-loaded")
    }

    private fun resolveCurrentProcessName(): String {
        val processName = currentProcessName
        if (isKnownProcessName(processName)) {
            return processName
        }
        val runtimeName = runtimeProcessName()
        if (isKnownProcessName(runtimeName)) {
            currentProcessName = runtimeName!!
            return runtimeName
        }
        return processName
    }

    private fun createModuleLoadedAppProcessStore(
        fallbackStore: DpisConfigStore,
        packageName: String?,
    ): DpisConfigStore {
        if (packageName.isNullOrBlank()) {
            return fallbackStore
        }
        return DpisConfigStore(
            RuntimePropertyConfigPreferences(
                packageName,
                RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ANY_ENABLED_TARGET,
            ),
        )
    }

    private fun installAppProcessHooksIfConfigured(
        store: DpisConfigStore,
        policy: HookRuntimePolicy,
        snapshot: ConfigSnapshot,
        requestedPackageName: String?,
        source: String,
    ) {
        if (appProcessInstallAttempted) {
            return
        }
        var packagePlan = ModulePackagePlan.resolve(snapshot, requestedPackageName)
        val packageName = packagePlan.packageName
        if (!snapshot.isConfigured(packageName)) {
            DpisLog.i("package not configured: package=$packageName")
            return
        }
        if (!packagePlan.targetDpisEnabled) {
            DpisLog.i("target app disabled by dpis toggle: package=$packageName")
            return
        }
        if (shouldSuppressSecondaryProcessViewport(currentProcessName, packagePlan)) {
            DpisLog.i(
                "secondary process viewport route suppressed: process=$currentProcessName, " +
                    "package=$packageName, viewportMode=${packagePlan.targetViewportMode}",
            )
            packagePlan = packagePlan.withoutViewportRoute()
            if (!packagePlan.hasSecondaryProcessSafeRoute()) {
                DpisLog.i("target app disabled after secondary process viewport suppression: process=$currentProcessName, package=$packageName")
                return
            }
        }
        if (!packagePlan.shouldInstallHooks()) {
            DpisLog.i("target app disabled: package=$packageName")
            return
        }
        DpisLog.i(
            "target app matched: package=$packageName, source=$source, " +
                "targetViewportSpec=${packagePlan.targetViewportSpec}, " +
                "targetViewportMode=${packagePlan.targetViewportMode}, " +
                "targetFontScalePercent=${packagePlan.targetFontScalePercent}, " +
                "targetFontMode=${packagePlan.targetFontMode}, typefaceActive=${packagePlan.typefaceActive}, " +
                "targetTypefaceId=${packagePlan.targetTypefaceId}, " +
                "flutterSettingsFont=${packagePlan.flutterSettingsFontEnabled}, " +
                "hyperOsNativeFlutterFont=${packagePlan.hyperOsNativeFlutterFontEnabled}",
        )
        RuntimeHotPathEvents.probe(
            packageName,
            "process_entry",
            "process-entry source=$source, process=$currentProcessName",
        )
        RuntimeBridgeEvents.setBridgeSink { message -> log(android.util.Log.INFO, message) }
        RuntimeBridgeEvents.emitSessionDiscovery(packageName, currentProcessName)
        appProcessInstallAttempted = true
        try {
            AppProcessHookInstaller.install(
                this,
                store,
                policy,
                packagePlan,
                getModernApiCapabilities(),
            )
        } catch (throwable: Throwable) {
            appProcessInstallAttempted = false
            DpisLog.e("failed to install app process hooks", throwable)
        }
    }

    private fun retryFlutterHooksWithAppClassLoader(
        store: DpisConfigStore,
        snapshot: ConfigSnapshot,
        classLoader: ClassLoader?,
        packageName: String?,
    ) {
        if (classLoader == null || packageName == null || HostProcess.isSystemServer(currentProcessName, packageName)) {
            return
        }
        val packagePlan = ModulePackagePlan.resolve(snapshot, packageName)
        if (!packagePlan.targetDpisEnabled || !packagePlan.fontScaleActive) {
            return
        }
        val policy = HookRuntimePolicy.fromSnapshot(snapshot)
        val debugOverride = AppProcessHookInstaller.resolveDebugFontOverrideForPackage(packageName)
        val executionPlan = packagePlan.buildExecutionPlan(policy, debugOverride)
        if (!executionPlan.flutterSettingsEnabled) {
            return
        }
        log(
            android.util.Log.INFO,
            BRIDGE_LOG_PREFIX + "flutter-retry proceeding: package=$packageName, " +
                "classLoader=${classLoader.javaClass.name}, fontPercent=${packagePlan.targetFontScalePercent}, " +
                "fontMode=${packagePlan.targetFontMode}",
        )
        FlutterSettingsFontHookInstaller.retryWithAppClassLoader(
            this,
            packageName,
            store,
            executionPlan.fontDomainPlan,
            classLoader,
        )
    }

    private fun retryTypefaceHooksWithPackageReady(
        store: DpisConfigStore,
        snapshot: ConfigSnapshot,
        packageName: String,
    ) {
        if (HostProcess.isSystemServer(currentProcessName, packageName)) {
            return
        }
        val packagePlan = ModulePackagePlan.resolve(snapshot, packageName)
        if (!packagePlan.targetDpisEnabled || !packagePlan.typefaceActive) {
            return
        }
        AppProcessHookInstaller.installTypefaceHooks(
            this,
            packagePlan.packageName,
            store,
            packagePlan.targetTypefaceId,
        )
    }

    private fun retryTypefaceFlutterProbe(
        snapshot: ConfigSnapshot,
        classLoader: ClassLoader?,
        packageName: String?,
    ) {
        if (classLoader == null || packageName == null || HostProcess.isSystemServer(currentProcessName, packageName)) {
            return
        }
        val packagePlan = ModulePackagePlan.resolve(snapshot, packageName)
        if (!packagePlan.targetDpisEnabled || !packagePlan.typefaceActive) {
            return
        }
        val replacementFont = PublishedFontFileResolver.resolve(packagePlan.targetTypefaceId)
        if (replacementFont == null || !replacementFont.isFile) {
            DpisLog.i("DPIS_FONT Flutter typeface probe skipped: package=$packageName, reason=published-font-unavailable")
            return
        }
        if (!HyperOsFlutterFontHookInstaller.isTypefaceAssetReplacementReady()) {
            DpisLog.i("DPIS_FONT Flutter typeface probe skipped: package=$packageName, reason=native-replacement-not-ready")
            return
        }
        FlutterSettingsFontHookInstaller.installTypefaceProbe(
            this,
            packageName,
            classLoader,
            replacementFont.absolutePath,
        )
    }

    private fun getOrCreateConfigStore(): DpisConfigStore {
        val local = configStore
        if (local != null) {
            return local
        }
        return ConfigStoreFactory.createForXposedHost(this).also { configStore = it }
    }

    private fun getModernApiCapabilities(): ModernApiCapabilities {
        val local = modernApiCapabilities
        if (local != null) {
            return local
        }
        return ModernApiCapabilitiesResolver.fromXposed(this).also { modernApiCapabilities = it }
    }

    private fun resolveSystemServerRuntimePolicy(
        store: DpisConfigStore,
        processName: String,
    ): HookRuntimePolicy {
        if (HostProcess.isSystemServer(processName, "")) {
            return HookRuntimePolicy.fromStore(store)
        }
        return HookRuntimePolicy.fromEffectiveSystemHookState(
            store,
            SystemScopeCoordinator.resolveSystemHookEffectiveEnabled(store),
        )
    }

    private fun resolveHookedRuntimePolicy(store: DpisConfigStore): HookRuntimePolicy {
        // A hooked process already proves module scope, even when XposedService is unavailable.
        return HookRuntimePolicy.fromStore(store)
    }

    private fun maybeInstallSystemServerHooks(
        store: DpisConfigStore,
        policy: HookRuntimePolicy,
        processName: String,
        packageName: String,
        systemServerClassLoader: ClassLoader?,
        source: String,
    ) {
        if (systemServerInstallAttempted ||
            !MutationPolicy.shouldInstallSystemServerHooks(processName, packageName, policy)
        ) {
            return
        }
        synchronized(this) {
            if (systemServerInstallAttempted) {
                return
            }
            systemServerInstallAttempted = true
            try {
                val result = HookInstaller.install(
                    this,
                    store,
                    getModernApiCapabilities(),
                    systemServerClassLoader,
                )
                systemServerInstallAttempted = result.isComplete()
                val message = "system_server installer ${if (result.hasInstalledHooks()) "ready" else "no-hooks"}: " +
                    "source=$source, process=$processName, package=$packageName, " +
                    "installed=${result.installedCount}, missing=${result.missingCount}, " +
                    "alreadyInstalled=${result.alreadyInstalled}, installedEntries=${result.installedEntries}, " +
                    "missingEntries=${result.missingEntries}"
                bridgeRuntimeLog(message)
                if (source == "hot-reload") {
                    bridgeHotReloadLog(
                        "system_server hot reload replay ${if (result.hasInstalledHooks()) "ready" else "no-hooks"}: " +
                            "process=$processName, package=$packageName, installed=${result.installedCount}, " +
                            "missing=${result.missingCount}, alreadyInstalled=${result.alreadyInstalled}, " +
                            "installedEntries=${result.installedEntries}, missingEntries=${result.missingEntries}",
                    )
                }
            } catch (throwable: Throwable) {
                systemServerInstallAttempted = false
                DpisLog.e("system_server installer failed", throwable)
                rawBridgeLog(
                    "system_server installer failed: source=$source, " +
                        "error=${throwable.javaClass.name}: ${throwable.message}",
                )
                if (source == "hot-reload") {
                    bridgeHotReloadLog(
                        "system_server hot reload replay failed: process=$processName, " +
                            "error=${throwable.javaClass.name}: ${throwable.message}",
                    )
                }
            }
        }
    }

    private fun maybeLogFirstPackageReady(packageName: String) {
        if (firstPackageReadyLogged) {
            return
        }
        synchronized(this) {
            if (firstPackageReadyLogged) {
                return
            }
            DpisLog.i(
                HookDiagnostics.buildPackageReadyStateLog(
                    currentProcessName,
                    packageName,
                    moduleLoadedObserved,
                    systemServerInstallAttempted,
                ),
            )
            firstPackageReadyLogged = true
        }
    }

    private fun log(level: Int, message: String) {
        log(level, "DPIS", message)
    }

    private fun rawBridgeLog(message: String) {
        android.util.Log.i("DPIS", BRIDGE_LOG_PREFIX + message)
    }

    private fun bridgeRuntimeLog(message: String) {
        log(android.util.Log.INFO, BRIDGE_LOG_PREFIX + message)
        rawBridgeLog(message)
    }

    private fun bridgeHotReloadLog(message: String) {
        log(android.util.Log.INFO, BRIDGE_LOG_PREFIX + message)
        rawBridgeLog(message)
    }

    private data class PackageReadyReplayState(
        val processName: String?,
        val packageName: String?,
        val classLoader: ClassLoader?,
        val applicationInfo: ApplicationInfo?,
    )
}
