package com.dpis.module

import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListItem

/**
 * Owns the post-save scope request for the Compose app editor.
 *
 * Approval updates the editor's immutable draft rather than a transient dialog state, so the
 * Compose scope action reflects the LSPosed result even when the request started from Save.
 */
internal class ComposeEditorScopeRequestCoordinator(
    private val mainViewModel: MainViewModel,
    private val scopeRequester: ScopeRequester,
    private val refreshEditor: Runnable,
    private val showRequestNotice: Runnable,
) {
    fun interface ScopeRequester {
        fun requestScope(item: AppListItem, onApproved: Runnable): Boolean
    }

    fun requestAfterSuccessfulSave(item: AppListItem?) {
        val requestItem = item ?: return
        if (!shouldRequestScope(requestItem)) return
        val requestStarted = scopeRequester.requestScope(requestItem) {
            onScopeApproved(requestItem.packageName)
        }
        if (requestStarted) showRequestNotice.run()
    }

    private fun shouldRequestScope(item: AppListItem?): Boolean {
        val draft: EditorDraft = mainViewModel.editingDraft ?: return false
        return item != null
            && item.scopeKnown
            && item.packageName == draft.packageName
            && !draft.scopeSelected
    }

    private fun onScopeApproved(packageName: String) {
        if (mainViewModel.markEditingScopeSelected(packageName)) {
            refreshEditor.run()
        }
    }
}
