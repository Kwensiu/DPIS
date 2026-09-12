package com.dpis.module.appconfig.presentation

import android.app.Activity
import android.view.View
import com.dpis.module.MainActivity
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.config.DpisConfigStore

/** Wires the portrait XML config sheet to MainActivity platform capabilities. */
class AppConfigSheetShell(
    private val activity: MainActivity,
) : AppConfigSheetSession.Shell {
    override fun activity(): Activity = activity

    override fun hookConfigStore(): DpisConfigStore? = activity.hookConfigStore

    override fun systemHooksEnabled(): Boolean = activity.isSystemHookEnabledFromStore

    override fun editingDraft(): EditorDraft? = activity.currentEditingDraft()

    override fun applyAppConfigDraft(root: View, draft: EditorDraft) {
        activity.applyAppConfigDraft(root, draft)
    }

    override fun rememberActiveEditor(root: View?, packageName: String?) {
        activity.rememberActiveEditor(root, packageName)
    }

    override fun currentEditorRoot(): View? = activity.currentEditorRoot()

    override fun isChangingConfigurations(): Boolean = activity.isChangingConfigurations

    override fun clearEditingSession() {
        activity.clearEditingSession()
    }

    override fun showToast(messageResId: Int) = activity.showToast(messageResId)
}
