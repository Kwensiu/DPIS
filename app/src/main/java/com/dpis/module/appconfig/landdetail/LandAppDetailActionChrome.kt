package com.dpis.module.appconfig.landdetail

import android.app.Activity
import android.content.res.ColorStateList
import android.view.View
import com.dpis.module.R
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder.AppConfigDialogState
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors

/** Scope / DPIS button chrome for the landscape detail pane. */
internal object LandAppDetailActionChrome {
    class Style(
        val defaultBgTint: ColorStateList?,
        val defaultStrokeWidth: Int,
        val defaultTextColor: Int,
    )

    fun capture(button: MaterialButton?): Style? {
        if (button == null) {
            return null
        }
        return Style(
            button.backgroundTintList,
            button.strokeWidth,
            MaterialColors.getColor(button, androidx.appcompat.R.attr.colorPrimary),
        )
    }

    fun refreshScope(
        activity: Activity,
        scopeButton: MaterialButton?,
        state: AppConfigDialogState?,
        style: Style?,
    ) {
        if (scopeButton == null || state == null || style == null) {
            return
        }
        val activeBgColor = MaterialColors.getColor(
            scopeButton,
            com.google.android.material.R.attr.colorSecondaryContainer,
        )
        val activeFgColor = MaterialColors.getColor(
            scopeButton,
            com.google.android.material.R.attr.colorOnSecondaryContainer,
        )
        val scopeTextRes = if (state.scopeKnown && state.scopeSelected) {
            R.string.dialog_scope_in_scope
        } else {
            R.string.dialog_scope_apply
        }
        val activeScopeStyle = state.scopeKnown && state.scopeSelected
        scopeButton.icon = null
        scopeButton.setText(scopeTextRes)
        scopeButton.backgroundTintList = if (activeScopeStyle) {
            ColorStateList.valueOf(activeBgColor)
        } else {
            style.defaultBgTint
        }
        scopeButton.setTextColor(if (activeScopeStyle) activeFgColor else style.defaultTextColor)
        scopeButton.strokeWidth = if (activeScopeStyle) 0 else style.defaultStrokeWidth
        scopeButton.contentDescription = activity.getString(scopeTextRes)
        scopeButton.isEnabled = state.scopeKnown
        scopeButton.alpha = if (state.scopeKnown) 1f else 0.6f
    }

    fun refreshDpisToggle(
        activity: Activity,
        root: View?,
        dpisToggleButton: MaterialButton?,
        state: AppConfigDialogState?,
        style: Style?,
    ) {
        if (dpisToggleButton == null || state == null || style == null) {
            return
        }
        val activeBgColor = MaterialColors.getColor(
            dpisToggleButton,
            com.google.android.material.R.attr.colorSecondaryContainer,
        )
        val activeFgColor = MaterialColors.getColor(
            dpisToggleButton,
            com.google.android.material.R.attr.colorOnSecondaryContainer,
        )
        val buttonText = activity.getString(
            if (state.dpisEnabled) R.string.dialog_config_disable else R.string.dialog_config_disabled,
        )
        val enabledActive = state.dpisEnabled
        dpisToggleButton.icon = null
        dpisToggleButton.text = buttonText
        dpisToggleButton.backgroundTintList = if (enabledActive) {
            style.defaultBgTint
        } else {
            ColorStateList.valueOf(activeBgColor)
        }
        dpisToggleButton.setTextColor(
            if (enabledActive) style.defaultTextColor else activeFgColor,
        )
        dpisToggleButton.strokeWidth = if (enabledActive) style.defaultStrokeWidth else 0
        dpisToggleButton.contentDescription = buttonText
        val prefillChip = LandAppDetailEditorSession.isPrefillChip(root)
        dpisToggleButton.isEnabled = !prefillChip
        dpisToggleButton.alpha = if (prefillChip) 0.6f else 1f
    }
}
