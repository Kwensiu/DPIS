package com.dpis.module.appconfig.presentation

import android.view.View
import com.dpis.module.R
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.quirks.presentation.WechatDpiSheetBinder
import com.dpis.module.ui.FormInputFocusBinder
import com.dpis.module.viewport.ViewportTargetType

internal class AppConfigSheetActionBinder(
    private val binder: AppConfigDialogBinder,
    private val host: AppConfigDialogBinder.Host,
) {
    fun bindDialogActions(
        dialogView: View,
        item: AppListItem,
        views: AppConfigDialogBinder.AppConfigDialogViews,
        state: AppConfigDialogBinder.AppConfigDialogState,
        style: AppConfigDialogBinder.AppConfigDialogActionStyle,
        systemHooksEnabled: Boolean,
    ) {
        dialogView.isFocusable = true
        dialogView.isFocusableInTouchMode = true
        dialogView.isClickable = true
        dialogView.setOnClickListener { clearInputFocus(dialogView, views) }
        views.scopeButton.setOnClickListener {
            clearInputFocus(dialogView, views)
            host.toggleScope(item, state.scopeSelected,
                {
                    state.scopeSelected = true
                    binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
                },
                {
                    state.scopeSelected = false
                    binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
                })
        }
        views.startButton.setOnClickListener {
            clearInputFocus(dialogView, views)
            host.executeProcessAction(item, AppConfigDialogBinder.ProcessAction.START)
        }
        views.restartButton.setOnClickListener {
            clearInputFocus(dialogView, views)
            host.executeProcessAction(item, AppConfigDialogBinder.ProcessAction.RESTART)
        }
        views.stopButton.setOnClickListener {
            clearInputFocus(dialogView, views)
            host.executeProcessAction(item, AppConfigDialogBinder.ProcessAction.STOP)
        }
        views.feedbackDiagnosticButton.setOnClickListener {
            clearInputFocus(dialogView, views)
            host.startFeedbackDiagnostic(item, state)
        }
        // Advanced actions are real per-app state even when DPI/font fields are
        // only a global-prefill preview; they must not save previewed config.
        views.dpisToggleButton.setOnClickListener {
            clearInputFocus(dialogView, views)
            val nextEnabled = !state.dpisEnabled
            if (host.setDpisEnabled(item.packageName, nextEnabled)) {
                state.dpisEnabled = nextEnabled
                binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
            }
        }
        views.fontHookDomainsButton.setOnClickListener {
            clearInputFocus(dialogView, views)
            host.showFontHookDomains(item, state) {
                binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
                host.onDraftStateChanged(state)
            }
        }
        views.disableButton.setOnClickListener {
            clearInputFocus(dialogView, views)
            views.viewportInputView.setText("")
            views.fontInputView.setText("")
            WechatDpiSheetBinder.clearDraft(dialogView)
            state.selectedTypefaceId = null
            state.clearViewportInputs()
            state.clearHookChainStateForReset()
            binder.bindTypefaceSelector(views.typefaceSelectorButton, state.selectedTypefaceId)
            AppConfigDialogBinder.bindViewportModeToggle(
                views.viewportModeToggle, ViewportTargetType.RELATIVE_SCALE, true)
            AppConfigDialogBinder.bindFontModeToggle(
                views.fontModeToggle, FontApplyMode.SYSTEM_EMULATION, true)
            AppConfigDialogBinder.updateSaveButtonState(dialogView, views)
            binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
            host.onDraftStateChanged(state)
        }
        views.saveButton.setOnClickListener {
            clearInputFocus(dialogView, views)
            if (!WechatDpiSheetBinder.isInputValid(dialogView)) {
                host.showToast(R.string.status_save_invalid)
                return@setOnClickListener
            }
            val result = host.saveAppConfig(
                dialogView,
                item,
                state.dpisEnabled,
                views.viewportInputView,
                views.fontInputView,
                AppConfigDialogBinder.resolveViewportMode(views.viewportModeToggle),
                state.viewportApplyMode,
                state.viewportApplyModeResetRequested,
                AppConfigDialogBinder.resolveFontMode(views.fontModeToggle),
                state.selectedTypefaceId,
                state.draftFontHookDomainsRaw,
                state.fontHookDomainsResetRequested,
                state.viewportScaleInput,
                state.viewportAbsoluteInput,
            ) ?: return@setOnClickListener
            if (result.success) {
                state.previewFromGlobalPrefill = false
                state.draftFontHookDomainsRaw = null
                state.fontHookDomainsResetRequested = false
                state.viewportApplyModeResetRequested = false
                state.captureSavedDraft(views, false)
                AppConfigDialogBinder.showSaveButtonFeedback(views.saveButton)
                binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
                binder.syncHyperOsNativeProxyAfterSave(item, views, state)
                binder.requestScopeAfterSuccessfulSave(
                    dialogView, item, views, state, style, systemHooksEnabled)
            }
            if (result.messageResId != 0) {
                host.showToast(result.messageResId)
            }
        }
    }

    fun bindTypefaceSelectorAction(
        dialogView: View,
        item: AppListItem,
        views: AppConfigDialogBinder.AppConfigDialogViews,
        state: AppConfigDialogBinder.AppConfigDialogState,
        style: AppConfigDialogBinder.AppConfigDialogActionStyle,
        systemHooksEnabled: Boolean,
    ) {
        views.typefaceSelectorButton.setOnClickListener {
            clearInputFocus(dialogView, views)
            binder.showTypefaceSelector(views.typefaceSelectorButton, state) {
                binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
                host.onDraftStateChanged(state)
            }
        }
    }

    private fun clearInputFocus(
        dialogView: View,
        views: AppConfigDialogBinder.AppConfigDialogViews,
    ) {
        FormInputFocusBinder.clearFocusAndHideIme(
            dialogView,
            views.viewportInputView,
            views.fontInputView,
            WechatDpiSheetBinder.inputViewForFocus(dialogView),
        )
    }
}
