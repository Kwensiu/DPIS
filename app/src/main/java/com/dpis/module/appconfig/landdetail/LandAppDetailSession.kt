package com.dpis.module.appconfig.landdetail

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigInputValidation
import com.dpis.module.appconfig.AppConfigPrefillPreview
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.appconfig.presentation.AppConfigDialogActivityHost
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder
import com.dpis.module.appconfig.presentation.EditorDraftSession
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.quirks.WechatDpiEditor
import com.dpis.module.quirks.presentation.WechatDpiSheetBinder
import com.dpis.module.settings.SystemScopeCoordinator
import com.dpis.module.ui.WindowInsetsBinder
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/**
 * Owns landscape XML detail-pane bind, save, and land-detail scope/typeface/hook
 * actions. Portrait XML sheets live on [com.dpis.module.appconfig.presentation.AppConfigSheetSession].
 */
class LandAppDetailSession(
    private val shell: Shell,
    private val saveHandler: AppConfigSaveHandler,
    private val scopeCoordinator: SystemScopeCoordinator,
    private val dialogHost: AppConfigDialogActivityHost,
) : LandAppDetailPaneBinder.Actions {
    interface Shell {
        fun activity(): Activity

        fun landDetailContent(): FrameLayout?

        fun landDetailEmptyView(): View?

        fun hookConfigStore(): DpisConfigStore?

        fun systemHooksEnabled(): Boolean

        fun showToast(messageResId: Int)

        fun requestAppsLoad()

        fun finalizeRuntimeSync(
            result: AppConfigSaveHandler.Result,
            configRoot: View?,
            packageName: String?,
            dpisEnabled: Boolean,
        ): AppConfigSaveHandler.Result

        fun editingDraft(): EditorDraft?

        fun applyAppConfigDraft(root: View, draft: EditorDraft)

        fun rememberActiveEditor(root: View?, packageName: String?)

        fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean

        fun executeProcessAction(
            item: AppListItem?,
            action: AppConfigDialogBinder.ProcessAction?,
        )

        fun startFeedbackDiagnostic(
            item: AppListItem?,
            state: AppConfigDialogBinder.AppConfigDialogState?,
        )

        fun updateEditingDraft(state: AppConfigDialogBinder.AppConfigDialogState?)
    }

    fun show(item: AppListItem): Boolean {
        val landDetailContent = shell.landDetailContent() ?: return false
        val activity = shell.activity()
        val store = shell.hookConfigStore()
        val sheetItem = AppConfigPrefillPreview.resolveForEditor(activity, item, store) ?: item
        val systemHooksEnabled = shell.systemHooksEnabled()
        val dialogView = LayoutInflater.from(activity).inflate(
            R.layout.view_land_app_detail,
            landDetailContent,
            false,
        )
        applyContentInsets(dialogView)
        LandAppDetailPaneBinder(activity, this).bind(dialogView, sheetItem, systemHooksEnabled)
        landDetailContent.removeAllViews()
        landDetailContent.addView(
            dialogView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        val scrollView = dialogView.findViewById<View>(R.id.land_detail_scroll)
        if (scrollView != null) {
            ViewCompat.requestApplyInsets(scrollView)
        }
        setVisible(shell.landDetailEmptyView(), false)
        setVisible(landDetailContent, true)
        shell.rememberActiveEditor(dialogView, item.packageName)
        val draft = shell.editingDraft()
        if (draft != null) {
            shell.applyAppConfigDraft(dialogView, draft)
            LandAppDetailPaneBinder.applyRetainedDraft(
                activity,
                dialogView,
                sheetItem,
                draft.selectedTypefaceId,
                draft.draftFontHookDomainsRaw,
                draft.viewportApplyMode,
                draft.fontHookDomainsResetRequested,
                draft.viewportApplyModeResetRequested,
            )
            WechatDpiSheetBinder.applyDraft(dialogView, draft.wechatDpiInput)
        }
        return true
    }

    fun saveForDiagnostic(
        item: AppListItem,
        state: AppConfigDialogBinder.AppConfigDialogState?,
        root: View,
    ): AppListItem? {
        if (!WechatDpiSheetBinder.isInputValid(root)) {
            shell.showToast(R.string.status_save_invalid)
            return null
        }
        val fontInput = EditorDraftSession.findEditorInput(
            root,
            R.id.land_detail_font_scale_input,
            R.id.dialog_font_scale_input,
        )
        val saved = saveDraftInternal(
            item,
            state,
            AppConfigDialogBinder.resolveViewportMode(
                EditorDraftSession.findViewportModeToggle(root),
            ),
            parseEditorPercentOrNull(fontInput),
            AppConfigDialogBinder.resolveFontMode(
                EditorDraftSession.findFontModeToggle(root),
            ),
            state?.selectedTypefaceId,
            state?.draftFontHookDomainsRaw,
            state?.viewportApplyMode ?: ViewportApplyMode.OFF,
            state?.viewportApplyModeResetRequested == true,
            state?.fontHookDomainsResetRequested == true,
            state?.viewportScaleInput ?: "",
            state?.viewportAbsoluteInput ?: "",
            state?.dpisEnabled == true,
            root,
            root.findViewById(R.id.land_detail_save_button),
        )
        return if (saved) {
            item.withWechatDpi(readPersistedWechatDpiForDiagnostic(item.packageName))
        } else {
            null
        }
    }

    override fun saveDraft(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
        viewportValue: Int?,
        viewportTargetType: String?,
        fontPercent: Int?,
        fontMode: String?,
        selectedTypefaceId: String?,
        draftFontHookDomainsRaw: String?,
        viewportApplyMode: String?,
        viewportApplyModeResetRequested: Boolean,
        fontHookDomainsResetRequested: Boolean,
        viewportScaleInput: String?,
        viewportAbsoluteInput: String?,
        dpisEnabled: Boolean,
        root: View?,
        saveButton: MaterialButton?,
    ) {
        saveDraftInternal(
            item,
            state,
            viewportTargetType,
            fontPercent,
            fontMode,
            selectedTypefaceId,
            draftFontHookDomainsRaw,
            viewportApplyMode,
            viewportApplyModeResetRequested,
            fontHookDomainsResetRequested,
            viewportScaleInput,
            viewportAbsoluteInput,
            dpisEnabled,
            root,
            saveButton,
        )
    }

    override fun showTypefaceSelector(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
        onChanged: Runnable?,
    ) {
        if (item?.packageName.isNullOrBlank() || state == null) {
            return
        }
        val activity = shell.activity()
        val selectorAnchor = MaterialButton(activity)
        AppConfigDialogBinder(activity, dialogHost)
            .showTypefaceSelector(selectorAnchor, state, onChanged)
    }

    override fun showHookDomains(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
        onChanged: Runnable?,
    ) {
        if (item?.packageName.isNullOrBlank()) {
            return
        }
        dialogHost.showFontHookDomains(item, state, onChanged)
    }

    override fun toggleScope(
        item: AppListItem?,
        currentlyInScope: Boolean,
        onTurnedInScope: Runnable?,
        onTurnedOutScope: Runnable?,
    ) {
        if (item == null || !item.scopeKnown) {
            return
        }
        scopeCoordinator.toggleScope(
            item.packageName,
            item.label,
            currentlyInScope,
            {
                onTurnedInScope?.run()
                shell.requestAppsLoad()
            },
            {
                onTurnedOutScope?.run()
                shell.requestAppsLoad()
            },
        )
    }

    override fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean {
        val saved = shell.setDpisEnabled(packageName, enabled)
        if (saved && packageName != null) {
            WechatDpiEditor.publishForDpisState(packageName, enabled)
            shell.requestAppsLoad()
        }
        return saved
    }

    override fun executeProcessAction(
        item: AppListItem?,
        action: AppConfigDialogBinder.ProcessAction?,
    ) {
        shell.executeProcessAction(item, action)
    }

    override fun startFeedbackDiagnostic(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
    ) {
        shell.startFeedbackDiagnostic(item, state)
    }

    override fun onDraftStateChanged(state: AppConfigDialogBinder.AppConfigDialogState?) {
        shell.updateEditingDraft(state)
    }

    private fun saveDraftInternal(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
        viewportTargetType: String?,
        fontPercent: Int?,
        fontMode: String?,
        selectedTypefaceId: String?,
        draftFontHookDomainsRaw: String?,
        viewportApplyMode: String?,
        viewportApplyModeResetRequested: Boolean,
        fontHookDomainsResetRequested: Boolean,
        viewportScaleInput: String?,
        viewportAbsoluteInput: String?,
        dpisEnabled: Boolean,
        root: View?,
        saveButton: MaterialButton?,
    ): Boolean {
        if (item == null || item.packageName.isNullOrBlank()) {
            return false
        }
        val normalizedViewportTargetType = ViewportTargetType.normalize(viewportTargetType)
        val rawViewportInput = if (
            ViewportTargetType.ABSOLUTE_DP == normalizedViewportTargetType
        ) {
            viewportAbsoluteInput
        } else {
            viewportScaleInput
        }
        val spec = AppConfigInputValidation.parseViewportTargetSpec(
            rawViewportInput,
            normalizedViewportTargetType,
        )
        var result = saveResolvedConfig(
            item,
            spec,
            viewportTargetType,
            viewportApplyMode,
            fontPercent,
            fontMode,
            selectedTypefaceId,
            draftFontHookDomainsRaw,
            viewportApplyModeResetRequested,
            fontHookDomainsResetRequested,
            viewportScaleInput,
            viewportAbsoluteInput,
        )
        result = shell.finalizeRuntimeSync(
            result,
            root,
            item.packageName,
            dpisEnabled,
        )
        if (result.messageResId != 0) {
            shell.showToast(result.messageResId)
        }
        if (!result.success) {
            return false
        }
        AppConfigDialogBinder.showSaveButtonFeedback(saveButton)
        LandAppDetailPaneBinder.markDraftSaved(root, saveButton)
        requestScopeAfterSuccessfulSave(item, state)
        return true
    }

    private fun saveResolvedConfig(
        item: AppListItem,
        viewportTargetSpec: ViewportTargetSpec?,
        viewportTargetType: String?,
        viewportApplyMode: String?,
        fontScalePercent: Int?,
        fontMode: String?,
        selectedTypefaceId: String?,
        draftFontHookDomainsRaw: String?,
        viewportApplyModeResetRequested: Boolean,
        fontHookDomainsResetRequested: Boolean,
        viewportScaleInput: String?,
        viewportAbsoluteInput: String?,
    ): AppConfigSaveHandler.Result = saveHandler.saveResolved(
        item,
        viewportTargetSpec,
        viewportTargetType,
        viewportApplyMode,
        viewportApplyModeResetRequested,
        fontScalePercent,
        fontMode,
        selectedTypefaceId,
        draftFontHookDomainsRaw,
        fontHookDomainsResetRequested,
        viewportScaleInput,
        viewportAbsoluteInput,
        shell.systemHooksEnabled(),
        shell.hookConfigStore(),
        null,
    )

    private fun requestScopeAfterSuccessfulSave(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
    ) {
        if (item == null ||
            state == null ||
            !state.scopeKnown ||
            state.scopeSelected ||
            state.scopeRequestPending
        ) {
            return
        }
        state.scopeRequestPending = true
        val requestStarted = scopeCoordinator.requestScope(
            item.packageName,
            item.label,
            { state.scopeSelected = true },
            { state.scopeRequestPending = false },
            false,
        )
        if (requestStarted) {
            shell.showToast(R.string.save_scope_request_notice)
            return
        }
        state.scopeRequestPending = false
    }

    private fun applyContentInsets(detailView: View) {
        val scrollView = detailView.findViewById<View>(R.id.land_detail_scroll)
        WindowInsetsBinder.applySafeDrawingPadding(scrollView, false, true, false, true)
    }

    private fun readPersistedWechatDpiForDiagnostic(packageName: String?): Int? {
        if (!WechatDpiConfig.appliesTo(packageName)) {
            return null
        }
        return shell.hookConfigStore()?.getWechatDpi(packageName)
    }

    companion object {
        private fun setVisible(view: View?, visible: Boolean) {
            if (view != null) {
                view.visibility = if (visible) View.VISIBLE else View.GONE
            }
        }

        private fun parseEditorPercentOrNull(input: TextInputEditText?): Int? {
            val raw = input?.text?.toString()?.trim().orEmpty()
            if (raw.isEmpty()) {
                return null
            }
            return try {
                raw.toInt()
            } catch (_: NumberFormatException) {
                null
            }
        }
    }
}
