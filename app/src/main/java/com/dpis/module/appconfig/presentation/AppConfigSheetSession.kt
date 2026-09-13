package com.dpis.module.appconfig.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dpis.module.MainActivity
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigDialogCoordinator
import com.dpis.module.appconfig.AppConfigPrefillPreview
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.applist.AppListItem
import com.dpis.module.quirks.presentation.WechatDpiSheetBinder
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Owns portrait XML app-config sheet bind, show, dismiss, and diagnostic save.
 */
class AppConfigSheetSession(
    private val activity: MainActivity,
    private val dialogHost: AppConfigDialogActivityHost,
) {

    private var dialog: BottomSheetDialog? = null

    fun show(item: AppListItem) {
        if (dialog?.isShowing == true) {
            return
        }
        val store = activity.hookConfigStore
        val sheetItem = AppConfigPrefillPreview.resolveForEditor(activity, item, store) ?: item
        val systemHooksEnabled = activity.startupSession.isSystemHookEnabledFromStore
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        val dialogView = LayoutInflater.from(activity).inflate(
            R.layout.dialog_app_config,
            root,
            false,
        )
        val binder = AppConfigDialogBinder(activity, dialogHost)
        binder.bind(dialogView, sheetItem, systemHooksEnabled)
        val draft = activity.editorDraftSession.currentEditingDraft()
        if (draft != null) {
            activity.editorDraftSession.applyAppConfigDraft(dialogView, draft)
            binder.applyRetainedDraft(
                dialogView,
                sheetItem,
                systemHooksEnabled,
                draft.selectedTypefaceId,
                draft.draftFontHookDomainsRaw,
                draft.viewportApplyMode,
                draft.fontHookDomainsResetRequested,
                draft.viewportApplyModeResetRequested,
            )
            WechatDpiSheetBinder.applyDraft(dialogView, draft.wechatDpiInput)
        }
        activity.editorDraftSession.rememberActiveEditor(dialogView, item.packageName)
        val shown = AppConfigDialogCoordinator(activity).show(dialogView)
        dialog = shown
        shown.setOnDismissListener {
            if (activity.editorDraftSession.currentEditorRoot() === dialogView) {
                activity.editorDraftSession.rememberActiveEditor(null, null)
            }
            if (dialog === shown) {
                dialog = null
            }
            if (!activity.isChangingConfigurations) {
                activity.editorDraftSession.clearEditingSession()
            }
        }
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    fun saveForDiagnostic(item: AppListItem, root: View): AppListItem? {
        val views = AppConfigDialogBinder.viewsFor(root)
        val state = AppConfigDialogBinder.stateFor(root)
        if (views == null || state == null) {
            return item
        }
        if (!AppConfigDialogBinder.updateSaveButtonState(root, views)) {
            activity.showToast(R.string.status_save_invalid)
            return null
        }
        val result = dialogHost.saveAppConfig(
            root,
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
        ) ?: return item
        if (result.messageResId != 0) {
            activity.showToast(result.messageResId)
        }
        if (!result.success) {
            return null
        }
        // Keep feedback diagnostic on the same save aftermath as the sheet save button.
        // Otherwise this side path can persist config but skip scope/proxy preparation.
        state.previewFromGlobalPrefill = false
        state.draftFontHookDomainsRaw = null
        state.fontHookDomainsResetRequested = false
        state.viewportApplyModeResetRequested = false
        state.captureSavedDraft(views, false)
        AppConfigDialogBinder.showSaveButtonFeedback(views.saveButton)
        val binder = AppConfigDialogBinder(activity, dialogHost)
        val systemHooksEnabled = activity.startupSession.isSystemHookEnabledFromStore
        val style = AppConfigDialogBinder.captureDialogActionStyle(views.scopeButton)
        binder.refreshDialogState(views, state, style, systemHooksEnabled, item)
        binder.syncHyperOsNativeProxyAfterSave(item, views, state)
        binder.requestScopeAfterSuccessfulSave(root, item, views, state, style, systemHooksEnabled)
        return item.withWechatDpi(readPersistedWechatDpiForDiagnostic(item.packageName))
    }

    private fun readPersistedWechatDpiForDiagnostic(packageName: String?): Int? {
        if (!WechatDpiConfig.appliesTo(packageName)) {
            return null
        }
        return activity.hookConfigStore?.getWechatDpi(packageName)
    }
}
