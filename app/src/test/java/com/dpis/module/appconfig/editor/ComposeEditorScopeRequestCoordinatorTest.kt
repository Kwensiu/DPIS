package com.dpis.module

import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeEditorScopeRequestCoordinatorTest {
    @Test
    fun approvalUpdatesMatchingEditorDraftAndRefreshesEditor() {
        val viewModel = MainViewModel(emptyState())
        val current = editorDraft("com.example.app", "125").withScopeSelected(false)
        val saved = editorDraft("com.example.app", "110").withScopeSelected(false)
        viewModel.restoreEditingSession(current.packageName, current, saved, ConfigEditorDestination.MAIN)
        val requester = RecordingScopeRequester()
        var refreshes = 0
        var notices = 0
        val coordinator = ComposeEditorScopeRequestCoordinator(
            viewModel,
            requester,
            Runnable { refreshes++ },
            Runnable { notices++ },
        )

        coordinator.requestAfterSuccessfulSave(item("com.example.app", true))

        assertEquals(1, requester.requestCount)
        assertEquals(1, notices)
        assertNotNull(requester.approved)
        requester.approved!!.run()

        assertTrue(viewModel.editingDraft!!.scopeSelected)
        assertTrue(viewModel.savedEditingDraft!!.scopeSelected)
        assertEquals("125", viewModel.editingDraft!!.fontInput)
        assertEquals(1, refreshes)
    }

    @Test
    fun ignoresUnknownSelectedAndMismatchedEditorSessions() {
        val viewModel = MainViewModel(emptyState())
        viewModel.restoreEditingSession(
            "com.example.active",
            editorDraft("com.example.active", "125").withScopeSelected(false),
            null,
            ConfigEditorDestination.MAIN,
        )
        val requester = RecordingScopeRequester()
        val coordinator = ComposeEditorScopeRequestCoordinator(viewModel, requester, Runnable {}, Runnable {})

        coordinator.requestAfterSuccessfulSave(item("com.example.other", true))
        coordinator.requestAfterSuccessfulSave(item("com.example.active", false))
        assertEquals(0, requester.requestCount)

        viewModel.editingDraft = viewModel.editingDraft!!.withScopeSelected(true)
        coordinator.requestAfterSuccessfulSave(item("com.example.active", true))
        assertEquals(0, requester.requestCount)
        assertFalse(viewModel.markEditingScopeSelected("com.example.other"))
    }

    @Test
    fun staleApprovalDoesNotUpdateAnotherEditorSession() {
        val viewModel = MainViewModel(emptyState())
        viewModel.restoreEditingSession(
            "com.example.first",
            editorDraft("com.example.first", "125").withScopeSelected(false),
            null,
            ConfigEditorDestination.MAIN,
        )
        val requester = RecordingScopeRequester()
        var refreshes = 0
        val coordinator = ComposeEditorScopeRequestCoordinator(
            viewModel,
            requester,
            Runnable { refreshes++ },
            Runnable {},
        )

        coordinator.requestAfterSuccessfulSave(item("com.example.first", true))
        viewModel.restoreEditingSession(
            "com.example.second",
            editorDraft("com.example.second", "110").withScopeSelected(false),
            null,
            ConfigEditorDestination.MAIN,
        )
        requester.approved!!.run()

        assertFalse(viewModel.editingDraft!!.scopeSelected)
        assertEquals(0, refreshes)
    }

    private fun emptyState() = MainUiState.initial(
        "", AppListFilterState.defaultState(), emptyList(), emptySet(),
    )

    private fun editorDraft(packageName: String, fontInput: String) = EditorDraft(
        packageName, "", "", "", "relative_scale", fontInput,
        FontApplyMode.SYSTEM_EMULATION, null, null, ViewportApplyMode.OFF, false, false, "", true, true,
    )

    private fun item(packageName: String, scopeKnown: Boolean) = AppListItem(
        packageName, packageName, false, scopeKnown, null, ViewportApplyMode.OFF, null,
        FontApplyMode.OFF, true, false, false, null,
    )

    private class RecordingScopeRequester : ComposeEditorScopeRequestCoordinator.ScopeRequester {
        var requestCount = 0
        var approved: Runnable? = null

        override fun requestScope(item: AppListItem, onApproved: Runnable): Boolean {
            requestCount++
            approved = onApproved
            return true
        }
    }
}
