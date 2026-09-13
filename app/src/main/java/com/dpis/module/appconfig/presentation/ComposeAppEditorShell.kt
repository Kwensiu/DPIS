package com.dpis.module.appconfig.presentation

import android.app.Activity
import com.dpis.module.MainActivity
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore

/** Wires the Compose app editor to MainActivity platform capabilities. */
class ComposeAppEditorShell(
    private val activity: MainActivity,
) : ComposeAppEditorActivityGateway.Shell {
    override fun activity(): Activity = activity

    override fun appsSnapshot(): List<AppListItem> =
        activity.startupSession.requireUiState().appsSnapshot()

    override fun hookConfigStore(): DpisConfigStore? = activity.hookConfigStore

    override fun resolvePackageVersionName(packageName: String): String =
        activity.resolvePackageVersionName(packageName)

    override fun systemHooksEnabled(): Boolean =
        activity.startupSession.isSystemHookEnabledFromStore

    override fun refreshEditor() = activity.mainWorkspaceSession.refreshApps()

    override fun requestAppsLoad() = activity.startupSession.requestAppsLoad()

    override fun toggleScope(
        item: AppListItem,
        currentlySelected: Boolean,
        onSelected: Runnable,
        onDeselected: Runnable,
    ) = activity.landDetailSession.toggleScope(
        item,
        currentlySelected,
        onSelected,
        onDeselected,
    )

    override fun setDpisEnabled(packageName: String, enabled: Boolean): Boolean =
        activity.runtimeLaunchSession.setDpisEnabled(packageName, enabled)

    override fun executeProcessAction(
        item: AppListItem,
        action: AppConfigDialogBinder.ProcessAction,
    ) = activity.runtimeLaunchSession.executeDialogProcessAction(item, action)

    override fun startFeedbackDiagnostic(item: AppListItem, draft: EditorDraft) {
        activity.startupSession.feedbackDiagnostic?.showPreparation(item, draft)
    }

    override fun postDelayed(delayMillis: Long, action: Runnable) {
        activity.window.decorView.postDelayed(action, delayMillis)
    }

    override fun finalizeRuntimeSync(
        result: AppConfigSaveHandler.Result,
        wechatDpiInput: String,
        packageName: String,
        dpisEnabled: Boolean,
    ): AppConfigSaveHandler.Result =
        activity.runtimeLaunchSession.finalizeAppConfigSaveWithRuntimeSync(
            result,
            wechatDpiInput,
            packageName,
            dpisEnabled,
            activity.hookConfigStore,
        )

    override fun showToast(messageResId: Int) = activity.showToast(messageResId)

    override fun isHyperOsNativeProxyCandidate(item: AppListItem?): Boolean =
        activity.runtimeLaunchSession.isHyperOsNativeProxyCandidate(item)

    override fun executeHyperOsNativeProxyMount(
        item: AppListItem?,
        apply: Boolean,
        onFinished: Runnable?,
    ) = activity.runtimeLaunchSession.executeHyperOsNativeProxyMount(item, apply, onFinished)
}
