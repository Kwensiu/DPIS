package com.dpis.module.appconfig.presentation

import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.dpis.module.R
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.quirks.presentation.WechatDpiSheetBinder
import com.dpis.module.ui.FormInputFocusBinder
import com.dpis.module.viewport.ViewportTargetType

internal class AppConfigSheetModeValidationBinder(
    private val binder: AppConfigDialogBinder,
    private val host: AppConfigDialogBinder.Host,
) {
    fun bindDialogValidation(
        dialogView: View,
        item: AppListItem,
        views: AppConfigDialogBinder.AppConfigDialogViews,
        state: AppConfigDialogBinder.AppConfigDialogState,
        style: AppConfigDialogBinder.AppConfigDialogActionStyle,
        systemHooksEnabled: Boolean,
    ) {
        val doneListener = TextView.OnEditorActionListener { _, actionId, event ->
            val isDoneAction = actionId == EditorInfo.IME_ACTION_DONE
            val isEnterDown = event != null &&
                event.action == KeyEvent.ACTION_DOWN &&
                event.keyCode == KeyEvent.KEYCODE_ENTER
            if (!isDoneAction && !isEnterDown) {
                return@OnEditorActionListener false
            }
            FormInputFocusBinder.clearFocusAndHideIme(
                dialogView,
                views.viewportInputView,
                views.fontInputView,
                WechatDpiSheetBinder.inputViewForFocus(dialogView),
            )
            true
        }
        views.viewportInputView.setOnEditorActionListener(doneListener)
        views.fontInputView.setOnEditorActionListener(doneListener)
        WechatDpiSheetBinder.bindDoneAction(doneListener, dialogView)
        FormInputFocusBinder.bindDismissOnOutsideTouch(
            dialogView.findViewById(R.id.dialog_app_config_scroll),
            dialogView,
            views.viewportInputView,
            views.fontInputView,
            WechatDpiSheetBinder.inputViewForFocus(dialogView),
        )
        val viewportValidationWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                state.updateViewportInput(
                    AppConfigDialogBinder.resolveViewportMode(views.viewportModeToggle), s)
                AppConfigDialogBinder.updateSaveButtonState(dialogView, views)
                binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
                host.onDraftStateChanged(state)
            }

            override fun afterTextChanged(s: Editable) = Unit
        }
        val fontValidationWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                AppConfigDialogBinder.updateSaveButtonState(dialogView, views)
                binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
                host.onDraftStateChanged(state)
            }

            override fun afterTextChanged(s: Editable) = Unit
        }
        views.viewportInputView.addTextChangedListener(viewportValidationWatcher)
        views.fontInputView.addTextChangedListener(fontValidationWatcher)
    }

    fun bindModeToggles(
        dialogView: View,
        item: AppListItem,
        views: AppConfigDialogBinder.AppConfigDialogViews,
        state: AppConfigDialogBinder.AppConfigDialogState,
        style: AppConfigDialogBinder.AppConfigDialogActionStyle,
        systemHooksEnabled: Boolean,
    ) {
        views.viewportModeToggle.container.setOnClickListener {
            FormInputFocusBinder.clearFocusAndHideIme(
                dialogView, views.viewportInputView, views.fontInputView,
            )
            AppConfigDialogBinder.toggleViewportMode(
                views.viewportModeToggle, views.viewportInputView, state)
            binder.bindViewportInputHint(
                views.viewportInputLayout,
                AppConfigDialogBinder.resolveViewportMode(views.viewportModeToggle),
            )
            AppConfigDialogBinder.updateSaveButtonState(dialogView, views)
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
            host.onDraftStateChanged(state)
        }
        views.viewportModeToggle.emulationLabel.setOnClickListener {
            FormInputFocusBinder.clearFocusAndHideIme(
                dialogView, views.viewportInputView, views.fontInputView,
            )
            AppConfigDialogBinder.switchViewportTargetType(
                views.viewportModeToggle, views.viewportInputView, state,
                ViewportTargetType.RELATIVE_SCALE, true,
            )
            binder.bindViewportInputHint(views.viewportInputLayout, ViewportTargetType.RELATIVE_SCALE)
            AppConfigDialogBinder.updateSaveButtonState(dialogView, views)
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
            host.onDraftStateChanged(state)
        }
        views.viewportModeToggle.replaceLabel.setOnClickListener {
            FormInputFocusBinder.clearFocusAndHideIme(
                dialogView, views.viewportInputView, views.fontInputView,
            )
            AppConfigDialogBinder.switchViewportTargetType(
                views.viewportModeToggle, views.viewportInputView, state,
                ViewportTargetType.ABSOLUTE_DP, true,
            )
            binder.bindViewportInputHint(views.viewportInputLayout, ViewportTargetType.ABSOLUTE_DP)
            AppConfigDialogBinder.updateSaveButtonState(dialogView, views)
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
            host.onDraftStateChanged(state)
        }
        views.fontModeToggle.container.setOnClickListener {
            FormInputFocusBinder.clearFocusAndHideIme(
                dialogView, views.viewportInputView, views.fontInputView,
            )
            AppConfigDialogBinder.toggleFontMode(views.fontModeToggle)
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
            host.onDraftStateChanged(state)
        }
        views.fontModeToggle.emulationLabel.setOnClickListener {
            FormInputFocusBinder.clearFocusAndHideIme(
                dialogView, views.viewportInputView, views.fontInputView,
            )
            AppConfigDialogBinder.bindFontModeToggle(
                views.fontModeToggle, FontApplyMode.SYSTEM_EMULATION, true,
            )
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
            host.onDraftStateChanged(state)
        }
        views.fontModeToggle.replaceLabel.setOnClickListener {
            FormInputFocusBinder.clearFocusAndHideIme(
                dialogView, views.viewportInputView, views.fontInputView,
            )
            AppConfigDialogBinder.bindFontModeToggle(
                views.fontModeToggle, FontApplyMode.FIELD_REWRITE, true,
            )
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
            host.onDraftStateChanged(state)
        }
    }
}
