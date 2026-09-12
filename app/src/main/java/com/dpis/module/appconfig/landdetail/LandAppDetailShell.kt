package com.dpis.module.appconfig.landdetail

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import com.dpis.module.MainActivity
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore

/** Wires the landscape detail session to MainActivity platform capabilities. */
class LandAppDetailShell(
    private val activity: MainActivity,
) : LandAppDetailSession.Shell {
    override fun activity(): Activity = activity

    override fun landDetailContent(): FrameLayout? = activity.landDetailContent()

    override fun landDetailEmptyView(): View? = activity.landDetailEmptyView()

    override fun hookConfigStore(): DpisConfigStore? = activity.hookConfigStore

    override fun systemHooksEnabled(): Boolean = activity.isSystemHookEnabledFromStore

    override fun showToast(messageResId: Int) = activity.showToast(messageResId)

    override fun requestAppsLoad() = activity.requestAppsLoad()

    override fun finalizeRuntimeSync(
        result: AppConfigSaveHandler.Result,
        configRoot: View?,
        packageName: String?,
        dpisEnabled: Boolean,
    ): AppConfigSaveHandler.Result = activity.finalizeAppConfigSaveWithRuntimeSync(
        result,
        configRoot,
        packageName,
        dpisEnabled,
        activity.hookConfigStore,
    )

    override fun editingDraft(): EditorDraft? = activity.currentEditingDraft()

    override fun applyAppConfigDraft(root: View, draft: EditorDraft) {
        activity.applyAppConfigDraft(root, draft)
    }

    override fun rememberActiveEditor(root: View?, packageName: String?) {
        activity.rememberActiveEditor(root, packageName)
    }

    override fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean =
        activity.setDpisEnabled(packageName, enabled)

    override fun executeProcessAction(
        item: AppListItem?,
        action: AppConfigDialogBinder.ProcessAction?,
    ) {
        if (item == null || action == null) {
            return
        }
        activity.executeDialogProcessAction(item, action)
    }

    override fun startFeedbackDiagnostic(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
    ) {
        activity.startFeedbackDiagnostic(item, state)
    }

    override fun updateEditingDraft(state: AppConfigDialogBinder.AppConfigDialogState?) {
        activity.updateEditingDraft(state)
    }
}
