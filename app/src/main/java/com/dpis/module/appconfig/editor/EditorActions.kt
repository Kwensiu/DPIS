package com.dpis.module.appconfig.editor

import com.dpis.module.appconfig.AppConfigProcessAction
import com.dpis.module.applist.AppListItem
import com.dpis.module.ui.ConfigEditorDestination

/** Builds immutable-draft actions without owning Activity state or side effects. */
object EditorActions {
    interface Host {
        fun updateDraft(draft: EditorDraft)
        fun resetDraft()
        fun showWechatDpiHelp()
        fun navigate(destination: ConfigEditorDestination)
        fun toggleScope(currentlySelected: Boolean, onSelected: Runnable, onDeselected: Runnable)
        fun setDpisEnabled(enabled: Boolean): Boolean
        fun executeProcessAction(action: AppConfigProcessAction)
        fun startFeedbackDiagnostic(draft: EditorDraft)
        fun save(draft: EditorDraft)
        fun close()
    }

    @JvmStatic
    fun create(host: Host, item: AppListItem, draft: EditorDraft): EditorPresentation.Actions =
        object : EditorPresentation.Actions {
            override fun updateViewportInput(value: String) =
                host.updateDraft(draft.withViewportInput(draft.viewportMode, value))

            override fun changeViewportMode(targetType: String) =
                host.updateDraft(draft.withViewportMode(targetType))

            override fun updateFontInput(value: String) = host.updateDraft(draft.withFontInput(value))
            override fun changeFontMode(mode: String) = host.updateDraft(draft.withFontMode(mode))
            override fun updateWechatDpiInput(value: String) =
                host.updateDraft(draft.withWechatDpiInput(value))

            override fun showWechatDpiHelp() = host.showWechatDpiHelp()
            override fun updateTypeface(typefaceId: String) = host.updateDraft(draft.withAdvancedConfig(
                typefaceId, draft.draftFontHookDomainsRaw, draft.viewportApplyMode,
                draft.fontHookDomainsResetRequested, draft.viewportApplyModeResetRequested,
            ))

            override fun updateHookChain(
                rawDomains: String,
                resetDomains: Boolean,
                viewportApplyMode: String,
                resetViewportApplyMode: Boolean,
            ) = host.updateDraft(draft.withAdvancedConfig(
                draft.selectedTypefaceId, rawDomains, viewportApplyMode,
                resetDomains, resetViewportApplyMode,
            ))

            override fun navigate(destination: ConfigEditorDestination) = host.navigate(destination)
            override fun reset() = host.resetDraft()
            override fun toggleScope() = host.toggleScope(
                draft.scopeSelected,
                Runnable { host.updateDraft(draft.withScopeSelected(true)) },
                Runnable { host.updateDraft(draft.withScopeSelected(false)) },
            )
            override fun toggleDpisEnabled() {
                val enabled = !draft.dpisEnabled
                if (host.setDpisEnabled(enabled)) host.updateDraft(draft.withDpisEnabled(enabled))
            }
            override fun startProcess() = host.executeProcessAction(AppConfigProcessAction.START)
            override fun restartProcess() = host.executeProcessAction(AppConfigProcessAction.RESTART)
            override fun stopProcess() = host.executeProcessAction(AppConfigProcessAction.STOP)
            override fun startFeedbackDiagnostic() = host.startFeedbackDiagnostic(draft)
            override fun save() = host.save(draft)
            override fun close() = host.close()
        }
}
