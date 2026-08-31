package com.dpis.module.appconfig

import android.widget.TextView
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType

object AppConfigDialogModeLogic {
    @JvmStatic fun resolveFontMode(toggle: AppConfigDialogBinder.ModeToggle) =
        if (toggle.container.tag == FontApplyMode.SYSTEM_EMULATION) FontApplyMode.SYSTEM_EMULATION else FontApplyMode.FIELD_REWRITE
    @JvmStatic fun bindFontModeToggle(toggle: AppConfigDialogBinder.ModeToggle, mode: String?, animate: Boolean) {
        val resolved = if (mode == FontApplyMode.SYSTEM_EMULATION) FontApplyMode.SYSTEM_EMULATION else FontApplyMode.FIELD_REWRITE
        toggle.container.tag = resolved
        AppConfigDialogBinder.updateModeToggleVisual(toggle, resolved == FontApplyMode.SYSTEM_EMULATION, animate)
    }
    @JvmStatic fun toggleFontMode(toggle: AppConfigDialogBinder.ModeToggle) = bindFontModeToggle(toggle, if (resolveFontMode(toggle) == FontApplyMode.FIELD_REWRITE) FontApplyMode.SYSTEM_EMULATION else FontApplyMode.FIELD_REWRITE, true)
    @JvmStatic fun resolveViewportMode(toggle: AppConfigDialogBinder.ModeToggle) =
        if (toggle.container.tag == ViewportTargetType.ABSOLUTE_DP) ViewportTargetType.ABSOLUTE_DP else ViewportTargetType.RELATIVE_SCALE
    @JvmStatic fun bindViewportModeToggle(toggle: AppConfigDialogBinder.ModeToggle, targetType: String?, animate: Boolean) {
        val resolved = if (ViewportTargetType.ABSOLUTE_DP == ViewportTargetType.normalize(targetType)) ViewportTargetType.ABSOLUTE_DP else ViewportTargetType.RELATIVE_SCALE
        toggle.container.tag = resolved
        AppConfigDialogBinder.updateModeToggleVisual(toggle, resolved == ViewportTargetType.RELATIVE_SCALE, animate)
    }
    @JvmStatic fun toggleViewportMode(toggle: AppConfigDialogBinder.ModeToggle, inputView: TextView, state: AppConfigDialogBinder.AppConfigDialogState) {
        val next = if (resolveViewportMode(toggle) == ViewportTargetType.ABSOLUTE_DP) ViewportTargetType.RELATIVE_SCALE else ViewportTargetType.ABSOLUTE_DP
        switchViewportTargetType(toggle, inputView, state, next, true)
    }
    @JvmStatic fun switchViewportTargetType(toggle: AppConfigDialogBinder.ModeToggle, inputView: TextView, state: AppConfigDialogBinder.AppConfigDialogState, nextType: String?, animate: Boolean) {
        state.updateViewportInput(resolveViewportMode(toggle), inputView.text)
        bindViewportModeToggle(toggle, nextType, animate)
        inputView.text = state.viewportInputFor(nextType)
    }
}

object AppConfigDialogInputLogic {
    @JvmStatic fun parsePositiveIntOrNull(inputView: TextView): Int? = AppConfigInputValidation.parsePositiveIntOrNull(inputView.text?.toString().orEmpty())
    @JvmStatic fun parseViewportTargetSpecOrNull(inputView: TextView, targetType: String?): ViewportTargetSpec = AppConfigInputValidation.parseViewportTargetSpec(inputView.text?.toString().orEmpty(), targetType)
    @JvmStatic fun parseFontScalePercentOrNull(inputView: TextView): Int? = AppConfigInputValidation.parseFontScalePercentOrNull(inputView.text?.toString().orEmpty())
    @JvmStatic fun initialFontMode(fontMode: String?): String = AppConfigInputValidation.initialFontMode(fontMode)
    @JvmStatic fun initialViewportTargetType(item: AppListItem?): String {
        if (item != null && item.viewportTargetSpec != null && !item.viewportTargetSpec.isEnabled && ViewportTargetType.OFF != ViewportTargetType.normalize(item.viewportTargetType)) return ViewportTargetType.normalize(item.viewportTargetType)
        return AppConfigInputValidation.initialViewportTargetType(item?.viewportTargetSpec)
    }
}
