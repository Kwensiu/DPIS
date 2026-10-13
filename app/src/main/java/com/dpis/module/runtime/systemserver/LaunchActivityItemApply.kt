package com.dpis.module.runtime.systemserver

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import com.dpis.module.config.PerAppDisplayConfigSource
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.runtime.font.HyperOsFlutterFontBridge
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
        val packageName = ConfiguredPackage.fromSource(source, args) ?: return
        val arguments = args ?: return
        val config = ConfigSelection.selectConfigForSystemServerEntry(
            "launch-activity-item",
            source!!.get(packageName),
        ) ?: return
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
            HyperOsFlutterFontBridge.publishTarget(packageName, config)
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
        var packageName = activityInfo?.packageName
        if (!PackageNames.isLikely(packageName)) {
            return
        }
        packageName = ConfiguredPackage.launchPackage(
            ConfiguredPackage.Lookup { source.get(it) },
            packageName,
            launchActivityItem,
        )
        val config = ConfigSelection.selectConfigForSystemServerEntry(
            "launch-activity-item",
            source.get(packageName),
        )
        if (!ConfigSelection.hasSystemServerViewportOverride(config)) {
            return
        }
        val baseConfiguration = readLaunchActivityConfiguration(launchActivityItem) ?: return
        val environment = HookInstaller.resolveLaunchViewportEnvironment(
            packageName,
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
                "system_server launch-activity-item object apply: package=" + packageName +
                        ", target=" + HookLog.describeEnvironment(environment),
                HookLog.resolveLogMinIntervalMs("launch-activity-item"),
                packageName,
                "mutation_applied",
            )
        }
    }

    private fun findFirstConfiguration(args: List<*>?): Configuration? {
        if (args == null) {
            return null
        }
        for (arg in args) {
            if (arg is Configuration) {
                return arg
            }
        }
        return null
    }

    private fun readLaunchActivityInfo(launchActivityItem: Any): ActivityInfo? {
        val value = Reflection.readField(launchActivityItem, "mInfo")
        return value as? ActivityInfo
    }

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
