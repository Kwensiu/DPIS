package com.dpis.module

import com.dpis.module.appconfig.AppConfigDialogBinder
import com.dpis.module.appconfig.AppConfigEditorSession
import com.dpis.module.appconfig.EditorActions
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.appconfig.EditorPresentation
import com.dpis.module.appconfig.EditorPresentationFactory
import com.dpis.module.applist.AppListItem
import com.dpis.module.templates.TemplateConfigValue

/**
 * Session and action owner for the primary-workspace Compose app editor.
 *
 * The Activity supplies Android-bound capabilities; this controller owns editor-session
 * transitions so opening, saving, closing, and asynchronous presentation feedback do not grow
 * another feature workflow inside the app shell.
 */
internal class ComposeAppEditorController(
    private val session: MainViewModel,
    private val host: Host,
) {
    interface Host {
        fun resolveEditorItem(packageName: String): AppListItem?
        fun hasSavedPackageConfig(packageName: String): Boolean
        fun resolveGlobalPrefill(): TemplateConfigValue?
        fun resolvePackageVersionName(packageName: String): String
        fun createDialogState(item: AppListItem, draft: EditorDraft): AppConfigDialogBinder.AppConfigDialogState
        fun typefaceSelectorText(typefaceId: String?): String
        fun hookChainText(
            item: AppListItem,
            state: AppConfigDialogBinder.AppConfigDialogState,
        ): String
        fun systemHooksEnabled(): Boolean
        fun automaticFontHookDomains(): Set<String>
        fun restoreClosedDraft(item: AppListItem, draft: EditorDraft?): EditorDraft?
        fun refreshEditor()
        fun requestAppsLoad()
        fun showWechatDpiHelp()
        fun toggleScope(
            item: AppListItem,
            currentlySelected: Boolean,
            onSelected: Runnable,
            onDeselected: Runnable,
        )
        fun setDpisEnabled(packageName: String, enabled: Boolean): Boolean
        fun executeProcessAction(item: AppListItem, action: AppConfigDialogBinder.ProcessAction)
        fun startFeedbackDiagnostic(item: AppListItem, draft: EditorDraft)
        fun save(item: AppListItem, draft: EditorDraft): Boolean
        fun postDelayed(delayMillis: Long, action: Runnable)
    }

    fun createState(): EditorPresentation.State? {
        val packageName = session.editingPackageName ?: return null
        val item = host.resolveEditorItem(packageName) ?: return null
        val editorSession = session.editorSession ?: return null
        if (editorSession.draft.packageName != item.packageName) return null
        val draft = editorSession.draft
        val dialogState = host.createDialogState(item, draft)
        return EditorPresentationFactory.create(
            item,
            host.resolvePackageVersionName(item.packageName),
            draft,
            host.typefaceSelectorText(draft.selectedTypefaceId),
            host.hookChainText(item, dialogState),
            editorSession.persistedBaseline,
            session.isEditingSaveFeedback,
            host.systemHooksEnabled(),
            host.automaticFontHookDomains(),
            session.editingDestination,
            createActions(item, draft),
            editorSession,
        )
    }

    fun open(item: AppListItem?) {
        item ?: return
        val editorItem = host.resolveEditorItem(item.packageName) ?: item
        session.editingPackageName = editorItem.packageName
        session.editingDestination = ConfigEditorDestination.MAIN
        val current = session.editorSession
        if (current == null || current.draft.packageName != editorItem.packageName) {
            session.editorSession = openSession(editorItem)
        }
        host.refreshEditor()
    }

    fun updateDraft(draft: EditorDraft?) {
        draft ?: return
        val current = session.editorSession ?: return
        session.editorSession = current.withDraft(draft)
        host.refreshEditor()
    }

    fun resetDraft() {
        val current = session.editorSession ?: return
        session.editorSession = current.reset()
        host.refreshEditor()
    }

    fun updateAdvancedDraft(
        draft: EditorDraft,
        state: AppConfigDialogBinder.AppConfigDialogState,
    ) {
        updateDraft(draft.withAdvancedConfig(
            state.selectedTypefaceId,
            state.draftFontHookDomainsRaw,
            state.viewportApplyMode,
            state.fontHookDomainsResetRequested,
            state.viewportApplyModeResetRequested,
        ))
    }

    fun refresh() {
        host.requestAppsLoad()
        host.refreshEditor()
    }

    fun close() {
        session.clearEditingDraft()
        session.clearEditingPackageName()
        host.refreshEditor()
    }

    fun markSaved(draft: EditorDraft?) {
        draft ?: return
        val current = session.editorSession?.withDraft(draft) ?: return
        session.editorSession = current.afterSave()
        session.isEditingSaveFeedback = true
        host.refreshEditor()
        host.postDelayed(1500L, Runnable {
            if (session.isEditingSaveFeedback) {
                session.isEditingSaveFeedback = false
                host.refreshEditor()
            }
        })
    }

    private fun createActions(item: AppListItem, draft: EditorDraft): EditorPresentation.Actions =
        EditorActions.create(object : EditorActions.Host {
            override fun updateDraft(draft: EditorDraft) {
                this@ComposeAppEditorController.updateDraft(draft)
            }
            override fun resetDraft() {
                this@ComposeAppEditorController.resetDraft()
            }
            override fun showWechatDpiHelp() = host.showWechatDpiHelp()
            override fun navigate(destination: ConfigEditorDestination) {
                session.editingDestination = destination
                host.refreshEditor()
            }
            override fun toggleScope(currentlySelected: Boolean, onSelected: Runnable, onDeselected: Runnable) =
                host.toggleScope(item, currentlySelected, onSelected, onDeselected)
            override fun setDpisEnabled(enabled: Boolean): Boolean =
                host.setDpisEnabled(item.packageName, enabled)
            override fun executeProcessAction(action: AppConfigDialogBinder.ProcessAction) =
                host.executeProcessAction(item, action)
            override fun startFeedbackDiagnostic(draft: EditorDraft) =
                host.startFeedbackDiagnostic(item, draft)
            override fun save(draft: EditorDraft) {
                if (host.save(item, draft)) markSaved(draft)
            }
            override fun close() {
                this@ComposeAppEditorController.close()
            }
        }, item, draft)

    private fun openSession(editorItem: AppListItem): AppConfigEditorSession {
        val hasSaved = host.hasSavedPackageConfig(editorItem.packageName)
        val prefill = if (hasSaved) null else host.resolveGlobalPrefill()
        val opened = AppConfigEditorSession.open(editorItem, hasSaved, prefill)
        if (!hasSaved) return opened
        val restored = host.restoreClosedDraft(
            editorItem,
            session.getLastClosedEditingDraft(editorItem.packageName),
        ) ?: return opened
        return AppConfigEditorSession(restored, null, restored, false)
    }
}
