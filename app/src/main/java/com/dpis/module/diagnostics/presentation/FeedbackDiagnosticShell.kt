package com.dpis.module.diagnostics.presentation

import com.dpis.module.appconfig.presentation.AppConfigDialogBinder
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListItem
import com.dpis.module.ui.presentation.MainComposeShellHost
import com.dpis.module.MainActivity
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.config.DpisConfigStore

/** Wires the diagnostic session to MainActivity platform capabilities. */
class FeedbackDiagnosticShell(
    private val activity: MainActivity,
) : FeedbackDiagnosticActivitySession.Shell {
    override fun activity(): LocalizedActivity = activity

    override fun composeShell(): MainComposeShellHost? = activity.composeShell()

    override fun showToast(messageResId: Int) = activity.showToast(messageResId)

    override fun runOnUiThread(action: Runnable) {
        activity.runOnUiThread(action)
    }

    override fun persistComposeEditor(item: AppListItem, draft: EditorDraft): Boolean {
        if (!activity.saveComposeEditorForDiagnostic(item, draft)) return false
        activity.markComposeEditorSaved(draft)
        return true
    }

    override fun persistViewEditor(
        item: AppListItem,
        state: AppConfigDialogBinder.AppConfigDialogState?,
    ): AppListItem? = activity.saveCurrentEditorConfigForDiagnostic(item, state)

    override fun hookConfigStore(): DpisConfigStore = activity.hookConfigStore

    override fun packageVersionName(packageName: String): String =
        activity.resolvePackageVersionName(packageName)

    override fun systemHooksEnabled(): Boolean = activity.isSystemHookEnabledFromStore

    override fun syncRuntimeForLaunch(packageName: String) {
        activity.syncRuntimePropertiesForTargetLaunch(packageName)
    }

    override fun dismissActiveEditorDialog() {
        activity.dismissActiveEditorDialog()
    }
}
