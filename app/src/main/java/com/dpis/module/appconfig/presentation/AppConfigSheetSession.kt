package com.dpis.module.appconfig.presentation

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigDialogCoordinator
import com.dpis.module.appconfig.AppConfigPrefillPreview
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.quirks.presentation.WechatDpiSheetBinder
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Owns portrait XML app-config sheet bind, show, dismiss, and diagnostic save.
 * [com.dpis.module.MainActivity] keeps landscape routing and shared runtime hosts.
 */
class AppConfigSheetSession(
    private val shell: Shell,
    private val dialogHost: AppConfigDialogActivityHost,
) {
    interface Shell {
        fun activity(): Activity

        fun hookConfigStore(): DpisConfigStore?

        fun systemHooksEnabled(): Boolean

        fun editingDraft(): EditorDraft?

        fun applyAppConfigDraft(root: View, draft: EditorDraft)

        fun rememberActiveEditor(root: View?, packageName: String?)

        fun currentEditorRoot(): View?

        fun isChangingConfigurations(): Boolean

        fun clearEditingSession()

        fun showToast(messageResId: Int)
    }

    private var dialog: BottomSheetDialog? = null

    fun show(item: AppListItem) {
        if (dialog?.isShowing == true) {
            return
        }
        val activity = shell.activity()
        val store = shell.hookConfigStore()
        val sheetItem = AppConfigPrefillPreview.resolveForEditor(activity, item, store) ?: item
        val systemHooksEnabled = shell.systemHooksEnabled()
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        val dialogView = LayoutInflater.from(activity).inflate(
            R.layout.dialog_app_config,
            root,
            false,
        )
        val binder = AppConfigDialogBinder(activity, dialogHost)
        binder.bind(dialogView, sheetItem, systemHooksEnabled)
        val draft = shell.editingDraft()
        if (draft != null) {
            shell.applyAppConfigDraft(dialogView, draft)
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
        shell.rememberActiveEditor(dialogView, item.packageName)
        val shown = AppConfigDialogCoordinator(activity).show(dialogView)
        dialog = shown
        shown.setOnDismissListener {
            if (shell.currentEditorRoot() === dialogView) {
                shell.rememberActiveEditor(null, null)
            }
            if (dialog === shown) {
                dialog = null
            }
            if (!shell.isChangingConfigurations()) {
                shell.clearEditingSession()
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
            shell.showToast(R.string.status_save_invalid)
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
            shell.showToast(result.messageResId)
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
        val binder = AppConfigDialogBinder(shell.activity(), dialogHost)
        val systemHooksEnabled = shell.systemHooksEnabled()
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
        return shell.hookConfigStore()?.getWechatDpi(packageName)
    }
}
