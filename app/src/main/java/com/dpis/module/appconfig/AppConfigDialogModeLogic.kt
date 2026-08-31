package com.dpis.module.appconfig

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportTargetType

/** Mode decisions and draft handoff shared by the dialog's two mode toggles. */
object AppConfigDialogModeLogic {
    @JvmStatic
    fun resolveFontMode(toggle: AppConfigDialogBinder.ModeToggle): String =
        if (toggle.container.tag == FontApplyMode.SYSTEM_EMULATION) {
            FontApplyMode.SYSTEM_EMULATION
        } else {
            FontApplyMode.FIELD_REWRITE
        }

    @JvmStatic
    fun bindFontModeToggle(toggle: AppConfigDialogBinder.ModeToggle, mode: String?, animate: Boolean) {
        val resolved = if (mode == FontApplyMode.SYSTEM_EMULATION) {
            FontApplyMode.SYSTEM_EMULATION
        } else {
            FontApplyMode.FIELD_REWRITE
        }
        toggle.container.tag = resolved
        AppConfigDialogBinder.updateModeToggleVisual(
            toggle,
            resolved == FontApplyMode.SYSTEM_EMULATION,
            animate,
        )
    }

    @JvmStatic
    fun toggleFontMode(toggle: AppConfigDialogBinder.ModeToggle) {
        bindFontModeToggle(
            toggle,
            if (resolveFontMode(toggle) == FontApplyMode.FIELD_REWRITE) {
                FontApplyMode.SYSTEM_EMULATION
            } else {
                FontApplyMode.FIELD_REWRITE
            },
            true,
        )
    }

    @JvmStatic
    fun resolveViewportMode(toggle: AppConfigDialogBinder.ModeToggle): String =
        if (toggle.container.tag == ViewportTargetType.ABSOLUTE_DP) {
            ViewportTargetType.ABSOLUTE_DP
        } else {
            ViewportTargetType.RELATIVE_SCALE
        }

    @JvmStatic
    fun bindViewportModeToggle(
        toggle: AppConfigDialogBinder.ModeToggle,
        targetType: String?,
        animate: Boolean,
    ) {
        val resolved = if (ViewportTargetType.ABSOLUTE_DP == ViewportTargetType.normalize(targetType)) {
            ViewportTargetType.ABSOLUTE_DP
        } else {
            ViewportTargetType.RELATIVE_SCALE
        }
        toggle.container.tag = resolved
        AppConfigDialogBinder.updateModeToggleVisual(
            toggle,
            resolved == ViewportTargetType.RELATIVE_SCALE,
            animate,
        )
    }

    @JvmStatic
    fun toggleViewportMode(
        toggle: AppConfigDialogBinder.ModeToggle,
        inputView: android.widget.TextView,
        state: AppConfigDialogBinder.AppConfigDialogState,
    ) {
        val next = if (resolveViewportMode(toggle) == ViewportTargetType.ABSOLUTE_DP) {
            ViewportTargetType.RELATIVE_SCALE
        } else {
            ViewportTargetType.ABSOLUTE_DP
        }
        switchViewportTargetType(toggle, inputView, state, next, true)
    }

    @JvmStatic
    fun switchViewportTargetType(
        toggle: AppConfigDialogBinder.ModeToggle,
        inputView: android.widget.TextView,
        state: AppConfigDialogBinder.AppConfigDialogState,
        nextType: String?,
        animate: Boolean,
    ) {
        state.updateViewportInput(resolveViewportMode(toggle), inputView.text)
        bindViewportModeToggle(toggle, nextType, animate)
        inputView.text = state.viewportInputFor(nextType)
    }
}
