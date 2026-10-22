package com.dpis.module.runtime.systemserver.mutation

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import com.dpis.module.config.PackageIdentity
import com.dpis.module.config.PerAppDisplayConfigSource
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.runtime.font.HyperOsFlutterFontBridge
import com.dpis.module.runtime.systemserver.HookDiagnostics
import com.dpis.module.runtime.systemserver.HookInstaller
import com.dpis.module.runtime.systemserver.MutationPolicy
import com.dpis.module.runtime.systemserver.diagnostics.HookLog
import com.dpis.module.runtime.systemserver.reflection.Reflection
import com.dpis.module.runtime.systemserver.selection.ConfigSelection
import com.dpis.module.runtime.systemserver.selection.ConfiguredPackage
import com.dpis.module.runtime.systemserver.selection.PackageNames
import com.dpis.module.runtime.systemserver.selection.PackageUidResolver
import com.dpis.module.runtime.systemserver.viewport.DisplayPixels
import com.dpis.module.viewport.PerAppDisplayEnvironment
import io.github.libxposed.api.XposedInterface
import java.util.StringJoiner

/** Viewport and font written onto LaunchActivityItem before and after it is constructed. */
object LaunchActivityItemApply {
    @JvmStatic
    fun handleConstructor(source: PerAppDisplayConfigSource, chain: XposedInterface.Chain): Any? {
        try {
            if (source.isSystemServerHooksEnabled()) {
                applyArgs(source, chain.args)
            }
        } catch (throwable: Throwable) {
            DpisLog.e("system_server launch-activity-item pre-init failed", throwable)
        }
        val result = chain.proceed()
        try {
            if (source.isSystemServerHooksEnabled()) {
                applyObject(source, chain.thisObject)
            }
        } catch (throwable: Throwable) {
            DpisLog.e("system_server launch-activity-item post-init failed", throwable)
        }
        return result
    }

    @JvmStatic
    fun applyArgs(source: PerAppDisplayConfigSource?, args: List<*>?) {
        if (source == null) return
        val arguments = args ?: return
        val packageName = ConfiguredPackage.fromSource(source, arguments)
        if (packageName == null) {
            if (DpisLog.isLoggingEnabled()) {
                HookLog.logIdentityMissing("launch-activity-item", null, "package_or_uid_missing")
            }
            return
        }
        val evidence = PackageNames.findUidEvidence(packageName, null, arguments)
        if (evidence == null) {
            if (DpisLog.isLoggingEnabled()) {
                HookLog.logIdentityMissing("launch-activity-item", packageName, "uid_missing")
            }
            return
        }
        val userId = PackageUidResolver.userIdFromUid(evidence.uid) ?: return
        if (DpisLog.isLoggingEnabled()) {
            HookLog.logIdentityResolved(
                "launch-activity-item",
                packageName,
                evidence.uid,
                userId,
                evidence.source,
            )
        }
        val lookup = source.inspect(PackageIdentity(packageName, userId))
        if (DpisLog.isLoggingEnabled()) {
            HookLog.logSourceLookup("launch-activity-item", lookup)
        }
        val config = ConfigSelection.selectConfigForSystemServerEntry(
            "launch-activity-item",
            lookup.config,
        )
        val baseConfiguration = findFirstConfiguration(arguments) ?: return
        var environment: PerAppDisplayEnvironment? = null
        if (ConfigSelection.hasSystemServerViewportOverride(config)) {
            environment = HookInstaller.resolveLaunchViewportEnvironment(
                packageName,
                baseConfiguration,
                DisplayPixels.widthPx(baseConfiguration, null),
                DisplayPixels.heightPx(baseConfiguration, null),
                config,
            )
        }
        val beforeSummary =
            if (DpisLog.isLoggingEnabled()) describeConfigurationArgs(arguments) else null
        val beforeFontScale = baseConfiguration.fontScale
        var changed = false
        var fontChanged = false
        for (arg in arguments) {
            if (arg is Configuration) {
                val configuration = arg
                if (environment != null &&
                    MutationPolicy.shouldApplyMutationField(
                        "launch-activity-item",
                        MutationField.VIEWPORT,
                    )
                ) {
                    changed = changed or
                            EnvironmentWrite.applyConfiguration(configuration, environment)
                }
                val appliedFont = MutationPolicy.shouldApplyMutationField(
                    "launch-activity-item",
                    MutationField.FONT_SCALE,
                ) && FontScale.apply(configuration, config)
                fontChanged = fontChanged or appliedFont
                changed = changed or appliedFont
            }
        }
        val activityItem = findLaunchActivityItem(arguments)
        if (activityItem != null && environment != null) {
            changed =
                changed or applyLaunchActivityItemConfigurationFields(activityItem, environment)
        }
        if (fontChanged) {
            HyperOsFlutterFontBridge.publishTarget(packageName, config, userId)
            FontScale.report(packageName, beforeFontScale, baseConfiguration.fontScale)
        }
        if (changed && DpisLog.isLoggingEnabled()) {
            val message = "system_server launch-activity-item apply: package=" + packageName +
                    ", before=" + HookLog.safeToString(beforeSummary) +
                    ", after=" + describeConfigurationArgs(arguments) +
                    ", target=" + HookLog.describeEnvironment(environment)
            HookLog.logDiagnosticIfChanged(
                "launch-activity-item|$packageName",
                message,
                HookLog.resolveLogMinIntervalMs("launch-activity-item"),
                packageName,
                "mutation_applied",
            )
        }
    }

    @JvmStatic
    fun applyObject(source: PerAppDisplayConfigSource?, launchActivityItem: Any?) {
        if (source == null || launchActivityItem == null) {
            return
        }
        val activityInfo = readLaunchActivityInfo(launchActivityItem)
        val packageName = activityInfo?.packageName
            ?.takeIf(PackageNames::isLikely) ?: return
        val uid = activityInfo.applicationInfo?.uid?.takeIf { it > 0 } ?: return
        val userId = PackageUidResolver.userIdFromUid(uid) ?: return
        val resolvedPackage = ConfiguredPackage.launchPackage(
            ConfiguredPackage.Lookup { candidatePackage ->
                candidatePackage?.let { source.get(PackageIdentity(it, userId)) }
            },
            packageName,
            launchActivityItem,
        ) ?: return
        val config = ConfigSelection.selectConfigForSystemServerEntry(
            "launch-activity-item",
            source.get(PackageIdentity(resolvedPackage, userId)),
        )
        if (!ConfigSelection.hasSystemServerViewportOverride(config)) {
            return
        }
        val baseConfiguration = readLaunchActivityConfiguration(launchActivityItem) ?: return
        val environment = HookInstaller.resolveLaunchViewportEnvironment(
            resolvedPackage,
            baseConfiguration,
            DisplayPixels.widthPx(baseConfiguration, null),
            DisplayPixels.heightPx(baseConfiguration, null),
            config,
        ) ?: return
        if (applyLaunchActivityItemConfigurationFields(launchActivityItem, environment) &&
            DpisLog.isLoggingEnabled()
        ) {
            HookLog.logDiagnosticIfChanged(
                "launch-activity-item-object|$packageName",
                "system_server launch-activity-item object apply: package=" + resolvedPackage +
                        ", target=" + HookLog.describeEnvironment(environment),
                HookLog.resolveLogMinIntervalMs("launch-activity-item"),
                packageName,
                "mutation_applied",
            )
        }
    }

    private fun findFirstConfiguration(args: List<*>?): Configuration? {
        if (args == null) return null
        return args.firstNotNullOfOrNull { it as? Configuration }
    }

    private fun readLaunchActivityInfo(launchActivityItem: Any): ActivityInfo? =
        Reflection.readField(launchActivityItem, "mInfo") as? ActivityInfo

    private fun readLaunchActivityConfiguration(launchActivityItem: Any): Configuration? {
        val override = Reflection.readField(
            launchActivityItem,
            "mOverrideConfig",
        )
        if (override is Configuration) {
            return override
        }
        val current = Reflection.readField(launchActivityItem, "mCurConfig")
        return current as? Configuration
    }

    private fun findLaunchActivityItem(args: List<*>?): Any? {
        if (args.isNullOrEmpty()) {
            return null
        }
        for (arg in args) {
            if (arg != null &&
                arg.javaClass.name == "android.app.servertransaction.LaunchActivityItem"
            ) {
                return arg
            }
        }
        return null
    }

    private fun applyLaunchActivityItemConfigurationFields(
        launchActivityItem: Any?,
        environment: PerAppDisplayEnvironment?,
    ): Boolean {
        if (launchActivityItem == null || environment == null) {
            return false
        }
        var changed = false
        changed = changed or applyConfigurationField(launchActivityItem, "mCurConfig", environment)
        changed =
            changed or applyConfigurationField(launchActivityItem, "mOverrideConfig", environment)
        return changed
    }

    private fun applyConfigurationField(
        target: Any,
        fieldName: String,
        environment: PerAppDisplayEnvironment,
    ): Boolean {
        val value = Reflection.readField(target, fieldName)
        if (value !is Configuration) {
            return false
        }
        return EnvironmentWrite.applyConfiguration(value, environment)
    }

    private fun describeConfigurationArgs(args: List<*>?): String {
        val joiner = StringJoiner(",")
        if (args == null) {
            return joiner.toString()
        }
        for ((index, arg) in args.withIndex()) {
            if (arg is Configuration) {
                joiner.add(
                    "#$index=" + HookDiagnostics.describeConfiguration(arg),
                )
            }
        }
        return joiner.toString()
    }
}
