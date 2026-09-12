package com.dpis.module.appconfig.landdetail

import android.view.View
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder
import com.dpis.module.applist.AppListItem
import com.dpis.module.quirks.WechatDpiEditor
import com.google.android.material.button.MaterialButton
import com.dpis.module.MainActivity

/**
 * Landscape detail-pane callbacks owned outside [MainActivity].
 *
 * The activity still performs save/scope/process work; this class is only the binder-facing
 * adapter so the app shell does not grow another nested editor workflow.
 */
class LandAppDetailActivityActions(
    private val activity: MainActivity,
) : LandAppDetailPaneBinder.Actions {
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
        activity.saveAppConfigDraft(
            item,
            state,
            viewportValue,
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
        activity.showLandDetailTypefaceSelector(item, state, onChanged)
    }

    override fun showHookDomains(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
        onChanged: Runnable?,
    ) {
        activity.showLandDetailHookDomains(item, state, onChanged)
    }

    override fun toggleScope(
        item: AppListItem?,
        currentlyInScope: Boolean,
        onTurnedInScope: Runnable?,
        onTurnedOutScope: Runnable?,
    ) {
        activity.toggleLandDetailScope(
            item,
            currentlyInScope,
            onTurnedInScope,
            onTurnedOutScope,
        )
    }

    override fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean {
        val saved = activity.setDpisEnabled(packageName, enabled)
        if (saved && packageName != null) {
            WechatDpiEditor.publishForDpisState(packageName, enabled)
            activity.requestAppsLoad()
        }
        return saved
    }

    override fun executeProcessAction(
        item: AppListItem?,
        action: AppConfigDialogBinder.ProcessAction?,
    ) {
        activity.executeDialogProcessAction(item, action)
    }

    override fun startFeedbackDiagnostic(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
    ) {
        activity.startFeedbackDiagnostic(item, state)
    }

    override fun onDraftStateChanged(state: AppConfigDialogBinder.AppConfigDialogState?) {
        activity.updateEditingDraft(state)
    }
}
