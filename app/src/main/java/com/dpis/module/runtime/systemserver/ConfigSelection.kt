package com.dpis.module.runtime.systemserver

import com.dpis.module.config.PerAppDisplayConfig
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode

/**
 * Which saved package config system_server may apply.
 * Explicit compat stays in the app. See PerAppDisplayConfigSource.
 */
object ConfigSelection {
    @JvmStatic
    fun selectConfigForSystemServerEntry(
        entryName: String?,
        config: PerAppDisplayConfig?,
    ): PerAppDisplayConfig? {
        if (config == null) {
            return null
        }
        val applyViewport = hasMutationForEntry(
            entryName, config, MutationField.VIEWPORT,
        )
        val applyFont = hasMutationForEntry(
            entryName, config, MutationField.FONT_SCALE,
        )
        if (!applyViewport && !applyFont) {
            return null
        }
        return config
    }

    @JvmStatic
    fun hasSystemServerViewportOverride(config: PerAppDisplayConfig?): Boolean {
        if (config == null || !config.hasViewportOverride()) {
            return false
        }
        val mode = ViewportApplyMode.normalize(config.targetViewportMode)
        if (ViewportApplyMode.SYSTEM == mode) {
            return true
        }
        // AUTO, including relative scale, launches from the physical display.
        // Explicit compat does not. See PerAppDisplayConfigSource.
        return ViewportApplyMode.AUTO == mode
    }

    @JvmStatic
    fun hasSystemServerFontOverride(config: PerAppDisplayConfig?): Boolean {
        val percent = config?.targetFontScalePercent
        return config != null &&
                FontApplyMode.SYSTEM_EMULATION == config.targetFontMode &&
                percent != null &&
                percent > 0
    }

    private fun hasMutationForEntry(
        entryName: String?,
        config: PerAppDisplayConfig,
        field: MutationField,
    ): Boolean {
        if (entryName != null &&
            !MutationPolicy.shouldApplyMutationField(entryName, field)
        ) {
            return false
        }
        return when (field) {
            MutationField.VIEWPORT -> hasSystemServerViewportOverride(config)
            MutationField.FONT_SCALE -> hasSystemServerFontOverride(config)
        }
    }
}
