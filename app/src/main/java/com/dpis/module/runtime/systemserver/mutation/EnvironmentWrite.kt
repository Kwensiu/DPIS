package com.dpis.module.runtime.systemserver.mutation

import android.content.res.Configuration
import android.graphics.Rect
import com.dpis.module.config.PerAppDisplayConfig
import com.dpis.module.runtime.font.HyperOsFlutterFontBridge
import com.dpis.module.runtime.systemserver.HookInstaller
import com.dpis.module.runtime.systemserver.MutationPolicy
import com.dpis.module.runtime.systemserver.hook.EntryRoute
import com.dpis.module.runtime.systemserver.reflection.Reflection
import com.dpis.module.runtime.systemserver.selection.ConfigSelection
import com.dpis.module.runtime.window.WindowFrameOverride
import com.dpis.module.viewport.PerAppDisplayEnvironment
import com.dpis.module.viewport.ViewportOverride

/**
 * Writes a resolved viewport, and launch-only font scale, onto system_server
 * configuration, frame, and DisplayInfo objects.
 */
object EnvironmentWrite {
    private val WINDOW_CONTAINER_CONFIGURATION_FIELD_NAMES = arrayOf(
        "mFullConfiguration",
        "mResolvedOverrideConfiguration",
        "mMergedOverrideConfiguration",
        "mTmpConfig",
        "mConfiguration",
        "configuration",
    )
    private val MERGED_CONFIGURATION_FIELD_NAMES = arrayOf(
        "mGlobalConfig",
        "mOverrideConfig",
        "mMergedConfig",
    )

    @JvmStatic
    fun apply(
        entryName: String?,
        configuration: Configuration?,
        frame: Rect?,
        displayInfo: Any?,
        environment: PerAppDisplayEnvironment?,
        config: PerAppDisplayConfig?,
        userId: Int,
    ): Boolean {
        var changed = false
        // Field baseline is owned by MutationPolicy.shouldApplyMutationField.
        // VIEWPORT uses a marker-gated baseline model and can be applied
        // across multiple lifecycle entries; FONT_SCALE is launch-only here
        // because changing Configuration.fontScale during later config dispatch
        // can produce CONFIG_FONT_SCALE relaunches.
        val applyViewport = environment != null &&
                MutationPolicy.shouldApplyMutationField(
                    entryName, MutationField.VIEWPORT,
                ) &&
                ConfigSelection.hasSystemServerViewportOverride(config)
        if (configuration != null && applyViewport) {
            changed = changed or applyConfiguration(configuration, environment)
        }
        if (configuration != null &&
            MutationPolicy.shouldApplyMutationField(
                entryName, MutationField.FONT_SCALE,
            )
        ) {
            val fontChanged = FontScale.apply(configuration, config)
            if (fontChanged && config != null) {
                HyperOsFlutterFontBridge.publishTarget(config.packageName, config, userId)
            }
            changed = changed or fontChanged
        }
        if (applyViewport && shouldApplyFrame(entryName) && frame != null) {
            changed = changed or applyFrame(frame, environment.widthPx, environment.heightPx)
        }
        if (applyViewport && shouldApplyDisplayInfo(entryName) && displayInfo != null) {
            changed = changed or applyDisplayInfo(displayInfo, environment)
        }
        return changed
    }

    @JvmStatic
    fun applyConfigDispatch(
        entryName: String?,
        target: Any?,
        environment: PerAppDisplayEnvironment?,
    ): Boolean {
        if (!EntryRoute.isConfigDispatch(entryName) || target == null || environment == null) {
            return false
        }
        var changed = false
        for (fieldName in WINDOW_CONTAINER_CONFIGURATION_FIELD_NAMES) {
            changed = changed or applyConfigurationField(target, fieldName, environment)
        }
        for (fieldName in HookInstaller.CONFIGURATION_FIELD_NAMES) {
            val value = Reflection.readField(target, fieldName)
            changed = changed or applyMergedConfigurationFields(value, environment)
        }
        return changed
    }

    @JvmStatic
    fun applyConfiguration(
        configuration: Configuration,
        environment: PerAppDisplayEnvironment,
    ): Boolean {
        val changed = configuration.screenWidthDp != environment.widthDp ||
                configuration.screenHeightDp != environment.heightDp ||
                configuration.smallestScreenWidthDp != environment.smallestWidthDp ||
                configuration.densityDpi != environment.densityDpi
        if (!changed) {
            return false
        }
        ViewportOverride.apply(
            configuration,
            ViewportOverride.Result(
                environment.widthDp,
                environment.heightDp,
                environment.smallestWidthDp,
                environment.densityDpi,
            ),
        )
        return true
    }

    @JvmStatic
    fun applyDisplayInfo(displayInfo: Any?, environment: PerAppDisplayEnvironment?): Boolean {
        val target = environment!!
        var changed = false
        if (target.widthPx > 0) {
            changed = changed or Reflection.writeIntField(
                displayInfo, "logicalWidth", target.widthPx,
            )
        }
        if (target.heightPx > 0) {
            changed = changed or Reflection.writeIntField(
                displayInfo, "logicalHeight", target.heightPx,
            )
        }
        changed = changed or Reflection.writeIntField(
            displayInfo, "logicalDensityDpi", target.densityDpi,
        )
        return changed
    }

    private fun shouldApplyFrame(entryName: String?): Boolean =
        EntryRoute.isRelayoutDispatch(entryName) ||
                EntryRoute.isDisplayPolicyLayout(entryName)

    private fun shouldApplyDisplayInfo(entryName: String?): Boolean =
        EntryRoute.isDisplayContentConfig(entryName)

    private fun applyFrame(frame: Rect, targetWidthPx: Int, targetHeightPx: Int): Boolean {
        if (targetWidthPx <= 0 || targetHeightPx <= 0) {
            return false
        }
        if (frame.width() == targetWidthPx && frame.height() == targetHeightPx) {
            return false
        }
        WindowFrameOverride.apply(frame, targetWidthPx, targetHeightPx)
        return true
    }

    private fun applyMergedConfigurationFields(
        target: Any?,
        environment: PerAppDisplayEnvironment,
    ): Boolean {
        if (target == null) {
            return false
        }
        var changed = false
        for (fieldName in MERGED_CONFIGURATION_FIELD_NAMES) {
            changed = changed or applyConfigurationField(target, fieldName, environment)
        }
        return changed
    }

    private fun applyConfigurationField(
        target: Any?,
        fieldName: String,
        environment: PerAppDisplayEnvironment,
    ): Boolean {
        val value = Reflection.readField(target, fieldName)
        if (value !is Configuration) {
            return false
        }
        return applyConfiguration(value, environment)
    }
}
