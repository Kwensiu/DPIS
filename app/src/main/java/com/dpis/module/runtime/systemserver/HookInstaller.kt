package com.dpis.module.runtime.systemserver

import android.content.res.Configuration
import com.dpis.module.config.ConfigSnapshotLoader
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.config.PerAppDisplayConfig
import com.dpis.module.config.PerAppDisplayConfigSource
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.ConfigStoreFactory
import com.dpis.module.runtime.ProcessScopedInstallGate
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import com.dpis.module.runtime.hyperos.HyperOsRustProcessHookInstaller
import com.dpis.module.runtime.systemserver.hook.HookCallback
import com.dpis.module.runtime.systemserver.hook.HookCatalog
import com.dpis.module.runtime.systemserver.hook.HookSpec
import com.dpis.module.runtime.systemserver.mutation.LaunchActivityItemApply
import com.dpis.module.runtime.systemserver.selection.ConfigRefreshTtl
import com.dpis.module.runtime.systemserver.selection.PackageUidResolver
import com.dpis.module.runtime.systemserver.viewport.LaunchActivityItemViewport
import com.dpis.module.viewport.PerAppDisplayEnvironment
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method
import java.lang.reflect.Modifier

@Suppress("java:S1872")
class HookInstaller private constructor() {
    companion object {
        val CONFIGURATION_FIELD_NAMES = arrayOf(
            "mergedConfiguration",
            "mLastReportedConfiguration",
            "mTmpConfig",
            "configuration",
            "mConfiguration",
        )
        private val PACKAGE_UID_RESOLVER = PackageUidResolver(
            ConfigRefreshTtl.SYSTEM_SERVER_TTL_MILLIS,
        )
        private val installedEntries = LinkedHashSet<String>()
        private var installedPid = -1

        fun resetForHotReload() {
            synchronized(HookInstaller::class.java) {
                installedPid = -1
                installedEntries.clear()
            }
        }

        fun install(
            xposed: XposedInterface,
            store: DpisConfigStore?,
            apiCapabilities: ModernApiCapabilities,
        ): InstallResult = install(xposed, store, apiCapabilities, null)

        fun install(
            xposed: XposedInterface,
            store: DpisConfigStore?,
            apiCapabilities: ModernApiCapabilities,
            systemServerClassLoader: ClassLoader?,
        ): InstallResult {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                HookDiagnostics.recordPending(
                    "system_server install skipped: reason=already-installed-fast-path",
                )
                DpisLog.i(HookDiagnostics.buildInstallSkipLog("already-installed-fast-path"))
                return InstallResult(0, 0, true)
            }
            synchronized(HookInstaller::class.java) {
                if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                    HookDiagnostics.recordPending(
                        "system_server install skipped: reason=already-installed-synchronized",
                    )
                    DpisLog.i(
                        HookDiagnostics.buildInstallSkipLog("already-installed-synchronized"),
                    )
                    return InstallResult(0, 0, true)
                }
                HookDiagnostics.recordPending(
                    HookDiagnostics.buildInstallEnterLog(store == null, installedPid != -1),
                )
                DpisLog.i(HookDiagnostics.buildInstallEnterLog(store == null, installedPid != -1))
                try {
                    HookDiagnostics.recordPending(HookDiagnostics.buildBootstrapLog())
                    DpisLog.i(HookDiagnostics.buildBootstrapLog())
                    val policy = HookRuntimePolicy.fromStore(store)
                    if (!policy.systemServerHooksEnabled) {
                        HookDiagnostics.recordPending(
                            HookDiagnostics.buildGateDisabledLog(
                                false,
                                policy.systemServerSafeModeEnabled,
                            ),
                        )
                        DpisLog.i(
                            HookDiagnostics.buildGateDisabledLog(
                                false,
                                policy.systemServerSafeModeEnabled,
                            ),
                        )
                        installedPid = ProcessScopedInstallGate.currentPid()
                        return InstallResult(0, 0, false)
                    }
                    val source = PerAppDisplayConfigSource.withLegacyRuntimePropertyFallback(
                        PerAppDisplayConfigSource.RefreshingSnapshotProvider(
                            {
                                ConfigSnapshotLoader.fromStore(
                                    ConfigStoreFactory.createForXposedHost(xposed),
                                )
                            },
                            ConfigRefreshTtl.SYSTEM_SERVER_TTL_MILLIS,
                        ),
                    )
                    var installedCount = 0
                    var missingCount = 0
                    val installedEntrySummary = StringBuilder()
                    val missingEntrySummary = StringBuilder()
                    if (MutationPolicy.shouldInstallTarget(
                            "launch-activity-item",
                            policy.systemServerSafeModeEnabled,
                        )
                    ) {
                        if (isEntryInstalled("launch-activity-item")) {
                            installedCount++
                            appendEntry(installedEntrySummary, "launch-activity-item")
                        } else if (installLaunchActivityItemHook(
                                xposed,
                                source,
                                HookCatalog.LAUNCH_ACTIVITY_ITEM,
                                apiCapabilities,
                            )
                        ) {
                            markEntryInstalled("launch-activity-item")
                            installedCount++
                            appendEntry(installedEntrySummary, "launch-activity-item")
                        } else {
                            missingCount++
                            appendEntry(missingEntrySummary, "launch-activity-item")
                        }
                    }
                    if (MutationPolicy.shouldInstallTarget(
                            "hyperos-rust-process",
                            policy.systemServerSafeModeEnabled,
                        )
                    ) {
                        if (isEntryInstalled("hyperos-rust-process")) {
                            installedCount++
                            appendEntry(installedEntrySummary, "hyperos-rust-process")
                        } else if (HyperOsRustProcessHookInstaller.install(xposed, source)) {
                            markEntryInstalled("hyperos-rust-process")
                            installedCount++
                            appendEntry(installedEntrySummary, "hyperos-rust-process")
                        } else {
                            missingCount++
                            appendEntry(missingEntrySummary, "hyperos-rust-process")
                        }
                    }
                    for (hookSpec in HookCatalog.methodHookSpecs()) {
                        if (!MutationPolicy.shouldInstallTarget(
                                hookSpec.entryName,
                                policy.systemServerSafeModeEnabled,
                            )
                        ) {
                            continue
                        }
                        if (isEntryInstalled(hookSpec.entryName)) {
                            installedCount++
                            appendEntry(installedEntrySummary, hookSpec.entryName)
                        } else if (installTargetHooks(
                                xposed,
                                source,
                                hookSpec,
                                apiCapabilities,
                                systemServerClassLoader,
                            )
                        ) {
                            markEntryInstalled(hookSpec.entryName)
                            installedCount++
                            appendEntry(installedEntrySummary, hookSpec.entryName)
                        } else {
                            missingCount++
                            appendEntry(missingEntrySummary, hookSpec.entryName)
                        }
                    }
                    HookDiagnostics.recordPending(
                        HookDiagnostics.buildInstallSummaryLog(installedCount, missingCount),
                    )
                    DpisLog.i(
                        HookDiagnostics.buildInstallSummaryLog(installedCount, missingCount),
                    )
                    markInstalledWhenComplete(missingCount)
                    return InstallResult(
                        installedCount,
                        missingCount,
                        false,
                        installedEntrySummary.toString(),
                        missingEntrySummary.toString(),
                    )
                } catch (throwable: Throwable) {
                    HookDiagnostics.recordPending(
                        "system_server install failed: throwable=${throwable.javaClass.name}",
                    )
                    DpisLog.e("system_server install failed", throwable)
                    throw throwable
                }
            }
        }

        private fun markInstalledWhenComplete(missingCount: Int) {
            if (missingCount == 0) {
                installedPid = ProcessScopedInstallGate.currentPid()
            }
        }

        private fun isEntryInstalled(entryName: String): Boolean =
            installedEntries.contains(entryName)

        private fun markEntryInstalled(entryName: String) {
            installedEntries.add(entryName)
        }

        private fun appendEntry(builder: StringBuilder, entryName: String) {
            if (builder.isNotEmpty()) {
                builder.append(',')
            }
            builder.append(entryName)
        }

        private fun installTargetHooks(
            xposed: XposedInterface,
            source: PerAppDisplayConfigSource,
            hookSpec: HookSpec,
            apiCapabilities: ModernApiCapabilities,
            systemServerClassLoader: ClassLoader?,
        ): Boolean {
            var hooked = false
            for (classLoader in resolveCandidateClassLoaders(systemServerClassLoader)) {
                for (className in hookSpec.classNames) {
                    val clazz = resolveClass(className, classLoader) ?: continue
                    for (method in clazz.declaredMethods) {
                        if (!matchesAnyMethodName(method, hookSpec.methodNames) ||
                            Modifier.isAbstract(method.modifiers)
                        ) {
                            continue
                        }
                        apiCapabilities.applyStableHookId(
                            xposed.hook(method)
                                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            hookSpec.hookIdFor(method),
                        ).intercept { chain ->
                            HookCallback.handle(chain, source, hookSpec.entryName)
                        }
                        hooked = true
                    }
                    if (hooked) break
                }
                if (hooked) break
            }
            if (hooked) {
                HookDiagnostics.recordPending(
                    HookDiagnostics.buildHookReadyLog(
                        hookSpec.entryName,
                        hookSpec.describeClassNames(),
                        hookSpec.describeMethodNames(),
                    ),
                )
                DpisLog.i(
                    HookDiagnostics.buildHookReadyLog(
                        hookSpec.entryName,
                        hookSpec.describeClassNames(),
                        hookSpec.describeMethodNames(),
                    ),
                )
                return true
            }
            HookDiagnostics.recordPending(
                HookDiagnostics.buildHookMissingLog(
                    hookSpec.entryName,
                    hookSpec.describeClassNames(),
                    hookSpec.describeMethodNames(),
                ),
            )
            DpisLog.i(
                HookDiagnostics.buildHookMissingLog(
                    hookSpec.entryName,
                    hookSpec.describeClassNames(),
                    hookSpec.describeMethodNames(),
                ),
            )
            return false
        }

        private fun installLaunchActivityItemHook(
            xposed: XposedInterface,
            source: PerAppDisplayConfigSource,
            hookSpec: HookSpec,
            apiCapabilities: ModernApiCapabilities,
        ): Boolean {
            val clazz = resolveClass(hookSpec.classNames[0], null)
            if (clazz == null) {
                DpisLog.i(
                    "system_server hook missing: entry=launch-activity-item, class=LaunchActivityItem",
                )
                return false
            }
            var hooked = false
            for (constructor in clazz.declaredConstructors) {
                apiCapabilities.applyStableHookId(
                    xposed.hook(constructor)
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                    hookSpec.hookIdFor(constructor),
                ).intercept { chain ->
                    LaunchActivityItemApply.handleConstructor(source, chain)
                }
                hooked = true
            }
            if (hooked) {
                DpisLog.i(
                    "system_server hook ready: entry=launch-activity-item, class=LaunchActivityItem",
                )
            }
            return hooked
        }

        private fun resolveCandidateClassLoaders(systemServerClassLoader: ClassLoader?): Set<ClassLoader?> {
            val classLoaders = LinkedHashSet<ClassLoader?>()
            classLoaders.add(systemServerClassLoader)
            classLoaders.add(Thread.currentThread().contextClassLoader)
            classLoaders.add(HookInstaller::class.java.classLoader)
            classLoaders.add(ClassLoader.getSystemClassLoader())
            classLoaders.add(null)
            return classLoaders
        }

        private fun resolveClass(className: String, classLoader: ClassLoader?): Class<*>? = try {
            Class.forName(className, false, classLoader)
        } catch (_: ReflectiveOperationException) {
            null
        }

        private fun matchesAnyMethodName(method: Method, methodNames: Array<String>): Boolean =
            methodNames.any { it == method.name }

        fun resolveLaunchViewportEnvironment(
            packageName: String?,
            baseConfiguration: Configuration?,
            widthPx: Int,
            heightPx: Int,
            config: PerAppDisplayConfig?,
        ): PerAppDisplayEnvironment? = LaunchActivityItemViewport.resolve(
            packageName,
            baseConfiguration,
            widthPx,
            heightPx,
            config,
        )

        fun configuredUid(packageName: String?, callingUid: Int): Int =
            PACKAGE_UID_RESOLVER.resolve(packageName, callingUid)
    }

    class InstallResult(
        val installedCount: Int,
        val missingCount: Int,
        val alreadyInstalled: Boolean,
        val installedEntries: String,
        val missingEntries: String,
    ) {
        constructor(installedCount: Int, missingCount: Int, alreadyInstalled: Boolean) :
                this(installedCount, missingCount, alreadyInstalled, "", "")

        fun hasInstalledHooks(): Boolean = alreadyInstalled || installedCount > 0

        fun isComplete(): Boolean = alreadyInstalled || missingCount == 0
    }
}
