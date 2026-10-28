package com.dpis.module.runtime.systemserver.diagnostics

import android.content.res.Configuration
import android.graphics.Rect
import com.dpis.module.config.ConfigLookupDiagnostics
import com.dpis.module.config.PerAppDisplayConfig
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.diagnostics.device.RuntimeTransport
import com.dpis.module.runtime.systemserver.HookDiagnostics
import com.dpis.module.runtime.systemserver.reflection.Reflection
import com.dpis.module.runtime.systemserver.selection.PackageNames
import com.dpis.module.viewport.PerAppDisplayEnvironment
import java.util.StringJoiner

/**
 * Diagnostic text for system_server hooks. Sampling stays on
 * [HookLogGate] so a diagnostic session does not open a second stream.
 */
object HookLog {
    @Volatile
    private var bridgeLogger: ((String) -> Unit)? = null

    @JvmStatic
    fun setBridgeLogger(logger: ((String) -> Unit)?) {
        bridgeLogger = logger
    }

    @JvmStatic
    fun logHookDispatch(entryName: String?, methodName: String) {
        val message = "system_server hook dispatch: entry=" + safeToString(entryName) +
                ", method=" + methodName
        if (logIfChanged(
                "hook-dispatch|$entryName|$methodName",
                message,
                resolveLogMinIntervalMs(entryName),
            )
        ) {
            emitBridge(message)
        }
    }

    @JvmStatic
    fun logHookConstructorDispatch(entryName: String?, constructorName: String) {
        val message = "system_server hook dispatch: entry=" + safeToString(entryName) +
                ", constructor=" + constructorName
        if (logIfChanged(
                "hook-dispatch|$entryName|$constructorName",
                message,
                resolveLogMinIntervalMs(entryName),
            )
        ) {
            emitBridge(message)
        }
    }

    @JvmStatic
    fun logHookSelection(message: String) {
        emitBridge(message)
    }

    private fun emitBridge(message: String) {
        bridgeLogger?.let { logger ->
            try {
                logger(message)
            } catch (_: Throwable) {
                // Diagnostic bridge output must not affect hook behavior.
            }
        }
    }

    private fun logBridgeIfChanged(key: String, message: String, minIntervalMs: Long) {
        if (logIfChanged(key, message, minIntervalMs)) {
            emitBridge(message)
        }
    }

    @JvmStatic
    fun callbackStageKey(
        entryName: String?,
        stage: String,
        packageName: String?,
        userId: Int?,
    ): String =
        "callback-stage|$entryName|$stage|${packageName ?: "unresolved"}|${userId ?: "unknown"}"

    @JvmStatic
    fun logCallbackStage(
        entryName: String?,
        stage: String,
        packageName: String?,
        userId: Int?,
        detail: String,
    ) {
        val message = "system_server callback stage: entry=" + safeToString(entryName) +
                ", stage=" + stage + ", package=" + safeToString(packageName) +
                ", userId=" + safeToString(userId) + ", " + detail
        if (PackageNames.isLikely(packageName)) {
            logDiagnosticIfChanged(
                callbackStageKey(entryName, stage, packageName, userId),
                message,
                resolveLogMinIntervalMs(entryName),
                packageName,
                stage,
            )
        } else {
            logBridgeIfChanged(
                callbackStageKey(entryName, stage, null, null),
                message,
                resolveLogMinIntervalMs(entryName),
            )
        }
    }

    @JvmStatic
    fun logMutationOutcome(
        entryName: String?,
        packageName: String,
        userId: Int,
        status: String,
        fields: String,
        before: String?,
        after: String?,
        target: String,
    ) {
        val message = "system_server mutation outcome: entry=" + safeToString(entryName) +
                ", package=" + packageName + ", userId=" + userId +
                ", status=" + status + ", fields=" + fields +
                ", before=" + safeToString(before) + ", after=" + safeToString(after) +
                ", target=" + target
        logDiagnosticIfChanged(
            "mutation-outcome|$entryName|$packageName|$userId|$status|$fields",
            message,
            resolveLogMinIntervalMs(entryName),
            packageName,
            status,
        )
    }

    @JvmStatic
    fun logCallbackEntry(entryName: String?, loggingEnabled: Boolean, hooksEnabled: Boolean) {
        logBridgeIfChanged(
            "callback-entry|$entryName",
            "system_server callback entry: entry=" + safeToString(entryName) +
                    ", logging_enabled=" + loggingEnabled +
                    ", hooks_enabled=" + hooksEnabled,
            resolveLogMinIntervalMs(entryName),
        )
    }

    @JvmStatic
    fun logDisplayManagerBranch(entryName: String?) {
        logBridgeIfChanged(
            "callback-display-manager|$entryName",
            "system_server display-manager branch: entry=" + safeToString(entryName),
            resolveLogMinIntervalMs(entryName),
        )
    }

    @JvmStatic
    fun logIdentityMissing(
        entryName: String?,
        packageName: String?,
        reason: String,
    ) {
        val message = "system_server identity missing: entry=" + safeToString(entryName) +
                ", package=" + safeToString(packageName) + ", reason=" + reason
        logDiagnosticIfChanged(
            "identity-missing|$entryName|$packageName|$reason",
            message,
            resolveLogMinIntervalMs(entryName),
            packageName,
            "identity_missing",
        )
    }

    @JvmStatic
    fun logIdentityResolved(
        entryName: String?,
        packageName: String,
        uid: Int,
        userId: Int,
        source: String,
    ) {
        val message = "system_server identity resolved: entry=" + safeToString(entryName) +
                ", package=" + packageName + ", uid=" + uid + ", userId=" + userId +
                ", source=" + source
        logDiagnosticIfChanged(
            "identity-resolved|$entryName|$packageName|$userId",
            message,
            resolveLogMinIntervalMs(entryName),
            packageName,
            "identity_resolved",
        )
    }

    @JvmStatic
    fun logSourceLookup(entryName: String?, diagnostics: ConfigLookupDiagnostics) {
        val message = "system_server source lookup: entry=" + safeToString(entryName) +
                ", package=" + diagnostics.identity.packageName +
                ", userId=" + diagnostics.identity.userId +
                ", snapshotOwner=" + diagnostics.snapshotOwnerUserId +
                ", ownerMatches=" + diagnostics.snapshotOwnerMatches +
                ", snapshotPackageHit=" + diagnostics.snapshotPackageHit +
                ", fallbackAttempted=" + diagnostics.fallbackAttempted +
                ", fallbackHit=" + diagnostics.fallbackHit +
                ", result=" + diagnostics.reason +
                ", viewport=" + safeToString(diagnostics.targetViewportSpec) +
                ", mode=" + safeToString(diagnostics.targetViewportMode)
        logDiagnosticIfChanged(
            "source-lookup|$entryName|${diagnostics.identity.packageName}|${diagnostics.identity.userId}",
            message,
            resolveLogMinIntervalMs(entryName),
            diagnostics.identity.packageName,
            "source_lookup",
        )
    }

    @JvmStatic
    fun logPolicySkip(entryName: String?, packageName: String, userId: Int) {
        logDiagnosticIfChanged(
            "policy-skip|$entryName|$packageName|$userId",
            "system_server config policy skip: entry=" + safeToString(entryName) +
                    ", package=" + packageName + ", userId=" + userId,
            resolveLogMinIntervalMs(entryName),
            packageName,
            "policy_skip",
        )
    }

    @JvmStatic
    fun logMutationUnchanged(
        entryName: String?,
        packageName: String,
        field: String,
    ) {
        logDiagnosticIfChanged(
            "mutation-unchanged|$entryName|$packageName|$field",
            "system_server mutation unchanged: entry=" + safeToString(entryName) +
                    ", package=" + packageName + ", field=" + field,
            resolveLogMinIntervalMs(entryName),
            packageName,
            "unchanged",
        )
    }

    @JvmStatic
    fun logProbe(
        entryName: String?,
        packageName: String?,
        originalSummary: String?,
        targetSummary: String?,
        actualSummary: String?,
    ) {
        val message = HookDiagnostics.buildProbeLog(
            entryName, packageName, originalSummary, targetSummary, actualSummary,
        )
        val key = "$entryName|$packageName"
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName))
    }

    @JvmStatic
    fun logPackageResolveMiss(entryName: String?, self: Any?, args: List<*>?) {
        val selfClass = describeClassName(self)
        val argClasses = describeArgClasses(args, 6)
        val argPreview = describeArgPreview(args, 3)
        val textPackages = describeTextPackages(self, args, 4)
        val message = HookDiagnostics.buildPackageResolveMissLog(
            entryName, selfClass, argClasses, argPreview, textPackages,
        )
        // Keep unresolved log keys low-cardinality to avoid burst noise on hot paths.
        val key = "unresolved|$entryName|$selfClass"
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName))
    }

    @JvmStatic
    fun logConfigMiss(
        entryName: String?,
        packageName: String?,
        candidatePackages: String?,
        configuredPackageCount: Int,
    ) {
        val sourceState = "configuredPackages=$configuredPackageCount, configNull=true"
        val message = HookDiagnostics.buildConfigMissLog(
            entryName, packageName, candidatePackages, sourceState,
        )
        // Candidate list may fluctuate frequently; key by entry+package for stable sampling.
        val key = "cfg-miss|$entryName|$packageName"
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName))
    }

    @JvmStatic
    fun logConfigFallback(
        entryName: String?,
        fromPackageName: String?,
        selectedPackageName: String?,
        candidatePackages: String?,
    ) {
        val message = HookDiagnostics.buildConfigFallbackLog(
            entryName, fromPackageName, selectedPackageName, candidatePackages,
        )
        val key = "cfg-fallback|$entryName|$fromPackageName|$selectedPackageName"
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName))
    }

    @JvmStatic
    fun logEnvironmentNull(
        entryName: String?,
        packageName: String?,
        beforeSummary: String?,
        afterSummary: String?,
    ) {
        val message = "system_server environment null: entry=" + safeToString(entryName) +
                ", package=" + safeToString(packageName) +
                ", before=" + safeToString(beforeSummary) +
                ", after=" + safeToString(afterSummary)
        val key = "env-null|$entryName|$packageName"
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName))
    }

    @JvmStatic
    fun logApplySkipped(entryName: String?, packageName: String?, stateSummary: String?) {
        val message = "system_server apply skipped: entry=" + safeToString(entryName) +
                ", package=" + safeToString(packageName) +
                ", state=" + safeToString(stateSummary)
        val key = "apply-skipped|$entryName|$packageName"
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName))
    }

    @JvmStatic
    fun logTargetComputation(
        entryName: String?,
        packageName: String?,
        preEnvironment: PerAppDisplayEnvironment?,
        postEnvironment: PerAppDisplayEnvironment?,
        effectiveEnvironment: PerAppDisplayEnvironment?,
    ) {
        val message = "system_server target env: entry=" + safeToString(entryName) +
                ", package=" + safeToString(packageName) +
                ", pre=" + describeEnvironment(preEnvironment) +
                ", post=" + describeEnvironment(postEnvironment) +
                ", effective=" + describeEnvironment(effectiveEnvironment)
        val key = "target-env|$entryName|$packageName"
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName))
    }

    @JvmStatic
    fun describeEnvironment(environment: PerAppDisplayEnvironment?): String {
        if (environment == null) {
            return "null"
        }
        return "wDp=" + environment.widthDp +
                ",hDp=" + environment.heightDp +
                ",swDp=" + environment.smallestWidthDp +
                ",dpi=" + environment.densityDpi +
                ",wPx=" + environment.widthPx +
                ",hPx=" + environment.heightPx
    }

    @JvmStatic
    fun logInterceptEnter(entryName: String?, self: Any?, args: List<*>?) {
        val selfClass = describeClassName(self)
        val argClasses = describeArgClasses(args, 6)
        val argPreview = describeArgPreview(args, 2)
        val message = HookDiagnostics.buildInterceptEnterLog(
            entryName, selfClass, argClasses, argPreview,
        )
        // Do not key by arg classes to avoid excessive cardinality.
        val key = "enter|$entryName|$selfClass"
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName))
    }

    /**
     * Reuses the existing system_server log gate before publishing diagnostic
     * transport evidence. The hook can run on hot lifecycle entries, so a
     * diagnostic session must not add a second, independently sampled stream.
     */
    @JvmStatic
    fun logDiagnosticIfChanged(
        key: String?,
        message: String?,
        minIntervalMs: Long,
        packageName: String?,
        stage: String?,
    ) {
        if (!logIfChanged(key, message, minIntervalMs) ||
            !PackageNames.isLikely(packageName)
        ) {
            return
        }
        bridgeLogger?.let { logger ->
            try {
                logger(message.orEmpty())
            } catch (_: Throwable) {
                // Diagnostic bridge output must not affect hook behavior.
            }
        }
        RuntimeTransport.record(
            "runtime",
            "system_server",
            stage,
            packageName,
            message,
        )
    }

    @JvmStatic
    fun logRelayoutMutationSkip(entryName: String?, packageName: String?, target: Any?) {
        val message = "system_server relayout mutation skipped: reason=non-app-window" +
                ", entry=" + safeToString(entryName) +
                ", package=" + safeToString(packageName) +
                ", target=" + summarizeValue(target, 180)
        logDiagnosticIfChanged(
            "relayout-skip|$packageName",
            message,
            resolveLogMinIntervalMs(entryName),
            packageName,
            "skipped",
        )
    }

    @JvmStatic
    fun resolveLogMinIntervalMs(entryName: String?): Long =
        HookLogGate.resolveLogMinIntervalMs(entryName)

    @JvmStatic
    fun shouldEmitLog(
        previousMessage: String?,
        currentMessage: String?,
        nowMs: Long,
        lastLogMs: Long?,
        minIntervalMs: Long,
    ): Boolean = HookLogGate.shouldEmitLog(
        previousMessage, currentMessage, nowMs, lastLogMs, minIntervalMs,
    )

    @JvmStatic
    fun logRelativeMarkerPublishFailed(entryName: String?, packageName: String?, target: String?) {
        if (!DpisLog.isLoggingEnabled()) {
            return
        }
        logDiagnosticIfChanged(
            "marker-gate|$entryName|$packageName",
            "system_server viewport skip: reason=marker-publish-failed" +
                    ", entry=" + entryName +
                    ", package=" + safeToString(packageName) +
                    ", target=" + target,
            resolveLogMinIntervalMs(entryName),
            packageName,
            "skipped",
        )
    }

    @JvmStatic
    fun logDisplayInfoProbe(
        entryName: String?,
        packageName: String?,
        displayInfo: Any?,
        frame: Rect?,
        configuration: Configuration?,
    ) {
        val displayInfoSummary = describeDisplayInfo(displayInfo) ?: return
        val message = HookDiagnostics.buildDisplayInfoProbeLog(
            entryName,
            packageName,
            displayInfoSummary,
            HookDiagnostics.describeFrame(frame),
            HookDiagnostics.describeConfiguration(configuration),
        )
        val key = "display|$entryName|$packageName"
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName))
    }

    @JvmStatic
    fun describeDisplayInfo(displayInfo: Any?): String? {
        if (displayInfo == null) {
            return null
        }
        val logicalWidth = Reflection.readIntField(displayInfo, "logicalWidth")
        val logicalHeight = Reflection.readIntField(displayInfo, "logicalHeight")
        val logicalDensityDpi = Reflection.readIntField(
            displayInfo, "logicalDensityDpi",
        )
        if (logicalWidth == null && logicalHeight == null && logicalDensityDpi == null) {
            return displayInfo.javaClass.name
        }
        return "displayInfo{logicalWidth=" + safeInt(logicalWidth) +
                ",logicalHeight=" + safeInt(logicalHeight) +
                ",logicalDensityDpi=" + safeInt(logicalDensityDpi) + "}"
    }

    @JvmStatic
    fun buildTargetSummary(
        environment: PerAppDisplayEnvironment?,
        frame: Rect?,
        config: PerAppDisplayConfig?,
    ): String {
        if (environment == null || config == null) {
            return "targetWidthDp=" + (config?.targetViewportWidthDp() ?: -1)
        }
        return HookDiagnostics.describeState(toConfiguration(environment), frame)!!
    }

    @JvmStatic
    fun safeToString(value: Any?): String {
        if (value == null) {
            return "null"
        }
        return try {
            value.toString()
        } catch (throwable: Throwable) {
            "toString-failed:" + throwable.javaClass.name
        }
    }

    @JvmStatic
    fun collectTextPackages(self: Any?, args: List<*>?, maxCount: Int): MutableSet<String> {
        val packages = LinkedHashSet<String>()
        if (maxCount <= 0) {
            return packages
        }
        PackageNames.collectWebApkOwners(safeToString(self), packages, maxCount)
        PackageNames.collectFromText(safeToString(self), packages, maxCount)
        if (args != null) {
            for (arg in args) {
                PackageNames.collectWebApkOwners(safeToString(arg), packages, maxCount)
                if (packages.size >= maxCount) {
                    break
                }
                PackageNames.collectFromText(safeToString(arg), packages, maxCount)
                if (packages.size >= maxCount) {
                    break
                }
            }
        }
        return packages
    }

    @JvmStatic
    fun describePackageCandidates(packages: Set<String>?): String {
        if (packages.isNullOrEmpty()) {
            return "none"
        }
        return packages.joinToString("|")
    }

    private fun logIfChanged(key: String?, message: String?, minIntervalMs: Long): Boolean =
        HookLogGate.logIfChanged(key, message, minIntervalMs)

    private fun summarizeValue(value: Any?, maxLength: Int): String {
        val summary = safeToString(value).replace('\n', ' ').replace('\r', ' ')
        if (summary.length <= maxLength) {
            return summary
        }
        val clippedLength = (maxLength - 3).coerceAtLeast(0)
        return summary.substring(0, clippedLength) + "..."
    }

    private fun describeClassName(value: Any?): String =
        if (value != null) value.javaClass.name else "null"

    private fun describeArgClasses(args: List<*>?, maxArgs: Int): String {
        if (args.isNullOrEmpty()) {
            return "none"
        }
        val joiner = StringJoiner("|")
        val count = minOf(args.size, maxArgs.coerceAtLeast(0))
        for (i in 0 until count) {
            joiner.add("$i:" + describeClassName(args[i]))
        }
        if (args.size > count) {
            joiner.add("more=" + (args.size - count))
        }
        return joiner.toString()
    }

    private fun describeArgPreview(args: List<*>?, maxArgs: Int): String {
        if (args.isNullOrEmpty()) {
            return "none"
        }
        val joiner = StringJoiner(" | ")
        val count = minOf(args.size, maxArgs.coerceAtLeast(0))
        for (i in 0 until count) {
            joiner.add("$i:" + summarizeValue(args[i], 140))
        }
        if (args.size > count) {
            joiner.add("more=" + (args.size - count))
        }
        return joiner.toString()
    }

    private fun describeTextPackages(self: Any?, args: List<*>?, maxCount: Int): String =
        describePackageCandidates(collectTextPackages(self, args, maxCount))

    private fun toConfiguration(environment: PerAppDisplayEnvironment): Configuration {
        val configuration = Configuration()
        configuration.screenWidthDp = environment.widthDp
        configuration.screenHeightDp = environment.heightDp
        configuration.smallestScreenWidthDp = environment.smallestWidthDp
        configuration.densityDpi = environment.densityDpi
        return configuration
    }

    private fun safeInt(value: Int?): Int = value ?: -1
}
