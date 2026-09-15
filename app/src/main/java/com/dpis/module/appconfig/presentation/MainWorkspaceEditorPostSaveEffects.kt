package com.dpis.module.appconfig.presentation

import com.dpis.module.MainActivity
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.editor.ComposeAppEditorSaveWorkflow
import com.dpis.module.appconfig.editor.ComposeEditorScopeRequestCoordinator
import com.dpis.module.appconfig.editor.EditorDraft
import com.dpis.module.applist.AppListItem

/**
 * Main-workspace post-save effects: runtime sync (including WeChat), scope request,
 * HyperOS native-proxy mount, and editor refresh.
 */
class MainWorkspaceEditorPostSaveEffects(
    private val activity: MainActivity,
    private val scopeCoordinator: ComposeEditorScopeRequestCoordinator,
) : ComposeAppEditorSaveWorkflow.PostSaveEffects {
    override fun afterPersist(
        result: AppConfigSaveHandler.Result,
        item: AppListItem,
        draft: EditorDraft,
    ): AppConfigSaveHandler.Result =
        activity.runtimeLaunchSession.finalizeAppConfigSaveWithRuntimeSync(
            result,
            draft.wechatDpiInput,
            item.packageName,
            draft.dpisEnabled,
            activity.hookConfigStore,
        )

    override fun showMessage(messageResId: Int) = activity.showToast(messageResId)

    override fun afterSuccessfulSave(item: AppListItem, draft: EditorDraft) {
        activity.showToast(R.string.status_save_success_inline)
        scopeCoordinator.requestAfterSuccessfulSave(item)
        activity.runtimeLaunchSession.syncHyperOsNativeProxyAfterSave(item)
        activity.mainWorkspaceSession.refreshApps()
    }
}
