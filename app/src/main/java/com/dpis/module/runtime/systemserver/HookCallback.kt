package com.dpis.module.runtime.systemserver

import android.content.res.Configuration
import android.graphics.Rect
import com.dpis.module.config.PerAppDisplayConfig
import com.dpis.module.config.PerAppDisplayConfigSource
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.viewport.PerAppDisplayEnvironment
import com.dpis.module.viewport.PerAppDisplayOverrideCalculator
import com.dpis.module.viewport.ViewportRuntimeMarkerProbe
import io.github.libxposed.api.XposedInterface

/**
 * What a system_server method hook does after it is installed.
 * If the original method was already invoked, a later failure is not retried.
 */
object HookCallback {
    private class Captured(
        val configuration: Configuration?,
        val frame: Rect?,
        val displayInfo: Any?,
    )

    @JvmStatic
    fun handle(
        chain: XposedInterface.Chain,
        source: PerAppDisplayConfigSource,
        entryName: String?,
    ): Any? {
        var result: Any? = null
        var proceedAttempted = false
        var proceeded = false
        try {
            val thisObject = chain.thisObject
            val args = chain.args
            if (!source.isSystemServerHooksEnabled()) {
                return chain.proceed()
            }
            if (EntryRoute.isDisplayManagerInfo(entryName)) {
                val displayManagerResult = chain.proceed()
                DisplayManagerInfo.apply(source, entryName, displayManagerResult)
                return displayManagerResult
            }
            val loggingEnabled = DpisLog.isLoggingEnabled()

            @Suppress("UNCHECKED_CAST")
            val currentConfiguredPackages =
                source.getConfiguredPackages() as Set<String>
            val contextObject = contextObject(entryName, thisObject, args)
            val contextArgs = contextArgs(contextObject, thisObject, args)
            val shouldInspectHotEntry = RelayoutWindow.shouldInspect(
                entryName,
                contextObject,
                contextArgs,
                contextObject !== thisObject,
                currentConfiguredPackages,
            )
            if (!shouldInspectHotEntry) {
                return chain.proceed()
            }
            if (loggingEnabled && HookLogGate.shouldLogInterceptEnter(entryName)) {
                HookLog.logInterceptEnter(entryName, contextObject, contextArgs)
            }
            val resolvedPackage = ConfiguredPackage.resolveForEntry(
                entryName,
                contextObject,
                contextArgs,
                ConfiguredPackage.Lookup { packageName ->
                    ConfigSelection.selectConfigForSystemServerEntry(
                        entryName,
                        source.get(packageName),
                    )
                },
            )
            val packageName = resolvedPackage.packageName
            if (packageName == null) {
                if (loggingEnabled) {
                    HookLog.logPackageResolveMiss(entryName, contextObject, contextArgs)
                }
                return chain.proceed()
            }
            val config = resolvedPackage.config
            if (config == null) {
                if (loggingEnabled) {
                    HookLog.logConfigMiss(
                        entryName,
                        packageName,
                        resolvedPackage.candidatePackagesSummary,
                        currentConfiguredPackages.size,
                    )
                }
                return chain.proceed()
            }
            if (loggingEnabled && resolvedPackage.fallbackFromPackage != null) {
                HookLog.logConfigFallback(
                    entryName,
                    resolvedPackage.fallbackFromPackage,
                    packageName,
                    resolvedPackage.candidatePackagesSummary,
                )
            }
            if (EntryRoute.isRelayoutDispatch(entryName) &&
                !RelayoutWindow.canApply(entryName, contextObject, packageName)
            ) {
                if (loggingEnabled) {
                    HookLog.logRelayoutMutationSkip(entryName, packageName, contextObject)
                }
                return chain.proceed()
            }
            val before = capture(contextObject, contextArgs)
            val preEnvironment = resolveTarget(packageName, before, before, config)
            if (MutationPolicy.shouldApplyPreProceedMutations(entryName)) {
                val applyEnvironment = RelativeViewport.resolveMarkerGatedEnvironment(
                    entryName,
                    packageName,
                    before.configuration,
                    before.displayInfo,
                    preEnvironment,
                    config,
                )
                var changed = EnvironmentWrite.apply(
                    entryName,
                    before.configuration,
                    before.frame,
                    before.displayInfo,
                    applyEnvironment,
                    config,
                )
                changed = changed or EnvironmentWrite.applyConfigDispatch(
                    entryName,
                    contextObject,
                    applyEnvironment,
                )
                if (changed) {
                    logViewportMarkerProbe(entryName, packageName, before, applyEnvironment, config)
                }
            }
            proceedAttempted = true
            result = chain.proceed()
            proceeded = true
            val after = capture(contextObject, contextArgs)
            val environment = resolveTarget(packageName, before, after, config)
            val effectiveEnvironment = afterEnvironment(preEnvironment, environment)
            if (EntryRoute.isConfigDispatch(entryName)) {
                EnvironmentWrite.applyConfigDispatch(
                    entryName,
                    contextObject,
                    effectiveEnvironment,
                )
            }
            if (loggingEnabled) {
                HookLog.logTargetComputation(
                    entryName,
                    packageName,
                    preEnvironment,
                    environment,
                    effectiveEnvironment,
                )
            }
            if (effectiveEnvironment == null && loggingEnabled) {
                HookLog.logEnvironmentNull(
                    entryName,
                    packageName,
                    HookDiagnostics.describeState(before.configuration, before.frame),
                    HookDiagnostics.describeState(after.configuration, after.frame),
                )
            }
            if (MutationPolicy.shouldApplyPostProceedMutations(entryName)) {
                val beforeApplySummary = if (loggingEnabled) {
                    HookDiagnostics.describeState(after.configuration, after.frame)
                } else {
                    null
                }
                val applyEnvironment = RelativeViewport.resolveMarkerGatedEnvironment(
                    entryName,
                    packageName,
                    after.configuration,
                    after.displayInfo,
                    effectiveEnvironment,
                    config,
                )
                if (EnvironmentWrite.apply(
                        entryName,
                        after.configuration,
                        after.frame,
                        after.displayInfo,
                        applyEnvironment,
                        config,
                    )
                ) {
                    logViewportMarkerProbe(entryName, packageName, after, applyEnvironment, config)
                    if (loggingEnabled) {
                        val afterApplySummary = HookDiagnostics.describeState(
                            after.configuration,
                            after.frame,
                        )
                        val message = HookDiagnostics.buildApplyLog(
                            entryName,
                            packageName,
                            beforeApplySummary,
                            afterApplySummary,
                        )
                        HookLog.logDiagnosticIfChanged(
                            "apply|$entryName|$packageName",
                            message,
                            HookLog.resolveLogMinIntervalMs(entryName),
                            packageName,
                            "mutation_applied",
                        )
                    }
                } else if (loggingEnabled) {
                    HookLog.logApplySkipped(entryName, packageName, beforeApplySummary)
                }
            }
            if (loggingEnabled) {
                val originalSummary = HookDiagnostics.describeState(
                    before.configuration,
                    before.frame,
                )
                val actualSummary = HookDiagnostics.describeState(
                    after.configuration,
                    after.frame,
                )
                val targetSummary = HookLog.buildTargetSummary(
                    effectiveEnvironment,
                    after.frame ?: before.frame,
                    config,
                )
                HookLog.logProbe(
                    entryName,
                    packageName,
                    originalSummary,
                    targetSummary,
                    actualSummary,
                )
                HookLog.logDisplayInfoProbe(
                    entryName,
                    packageName,
                    after.displayInfo,
                    after.frame,
                    after.configuration,
                )
            }
            return result
        } catch (throwable: Throwable) {
            DpisLog.e(HookDiagnostics.buildInterceptErrorLog(entryName, throwable), throwable)
            if (proceeded) {
                return result
            }
            if (proceedAttempted) {
                throw throwable
            }
            return chain.proceed()
        }
    }

    private fun contextObject(entryName: String?, self: Any?, args: List<*>?): Any? {
        if (EntryRoute.isRelayoutDispatch(entryName)) {
            return RelayoutWindow.target(self, args)
        }
        return self
    }

    private fun contextArgs(contextObject: Any?, self: Any?, args: List<*>?): List<*>? {
        if (contextObject == null || contextObject === self) {
            return args
        }
        val contextArgs = ArrayList<Any?>()
        contextArgs.add(contextObject)
        if (args != null) {
            contextArgs.addAll(args)
        }
        return contextArgs
    }

    private fun capture(self: Any?, args: List<*>?): Captured = Captured(
        SnapshotProbe.configuration(self, args),
        SnapshotProbe.frame(self, args),
        SnapshotProbe.displayInfo(self, args),
    )

    private fun resolveTarget(
        packageName: String,
        before: Captured,
        after: Captured,
        config: PerAppDisplayConfig,
    ): PerAppDisplayEnvironment? {
        if (!ConfigSelection.hasSystemServerViewportOverride(config)) {
            return null
        }
        val configuration = after.configuration ?: before.configuration ?: return null
        val frame = after.frame ?: before.frame
        val widthPx = DisplayPixels.widthPx(configuration, frame)
        val heightPx = DisplayPixels.heightPx(configuration, frame)
        if (config.targetViewportSpec.isRelativeScale()) {
            return RelativeViewport.resolve(
                packageName, configuration, widthPx, heightPx, config,
            )
        }
        return PerAppDisplayOverrideCalculator.calculate(
            configuration, widthPx, heightPx, config.targetViewportSpec,
        )
    }

    @JvmStatic
    fun chooseEffectiveEnvironment(
        preEnvironment: PerAppDisplayEnvironment?,
        postEnvironment: PerAppDisplayEnvironment?,
    ): PerAppDisplayEnvironment? = postEnvironment ?: preEnvironment

    private fun afterEnvironment(
        preEnvironment: PerAppDisplayEnvironment?,
        postEnvironment: PerAppDisplayEnvironment?,
    ): PerAppDisplayEnvironment? = chooseEffectiveEnvironment(preEnvironment, postEnvironment)

    private fun logViewportMarkerProbe(
        entryName: String?,
        packageName: String?,
        source: Captured?,
        environment: PerAppDisplayEnvironment?,
        config: PerAppDisplayConfig?,
    ) {
        if (source == null || config == null || environment == null) {
            return
        }
        val markerSourceConfiguration = source.configuration?.let { Configuration(it) }
        ViewportRuntimeMarkerProbe.publishSystemServerProbe(
            packageName,
            markerSourceConfiguration,
            environment,
            config.targetViewportSpec,
            environment.smallestWidthDp,
            entryName,
        )
    }
}
