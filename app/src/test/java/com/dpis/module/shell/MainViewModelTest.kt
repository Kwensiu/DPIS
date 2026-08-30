package com.dpis.module

import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppListPage
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelTest {
    @Test
    fun restoredEditorSessionRetainsDraftBaselineAndDestination() {
        val viewModel = MainViewModel(emptyState())
        val draft = editorDraft("com.example.app", "125")
        val savedDraft = editorDraft("com.example.app", "110")

        viewModel.restoreEditingSession(
            "com.example.app",
            draft,
            savedDraft,
            ConfigEditorDestination.HOOK_CHAIN_FONT,
        )

        assertEquals("com.example.app", viewModel.editingPackageName)
        assertSame(draft, viewModel.editingDraft)
        assertSame(savedDraft, viewModel.savedEditingDraft)
        assertEquals(ConfigEditorDestination.HOOK_CHAIN_FONT, viewModel.editingDestination)
    }

    @Test
    fun closingEditorSessionClearsDraftBaselineAndChildDestination() {
        val viewModel = MainViewModel(emptyState())
        val draft = editorDraft("com.example.app", "125")
        viewModel.restoreEditingSession(
            "com.example.app",
            draft,
            null,
            ConfigEditorDestination.HOOK_CHAIN_INTERFACE,
        )
        viewModel.isEditingSaveFeedback = true

        viewModel.clearEditingDraft()
        viewModel.clearEditingPackageName()

        assertNull(viewModel.editingPackageName)
        assertNull(viewModel.editingDraft)
        assertNull(viewModel.savedEditingDraft)
        assertEquals(ConfigEditorDestination.MAIN, viewModel.editingDestination)
        assertFalse(viewModel.isEditingSaveFeedback)
    }

    @Test
    fun remembersLastSavedDraftForSamePackageUntilCatalogRefreshes() {
        val viewModel = MainViewModel(emptyState())
        val saved = editorDraft("com.example.app", "125")
            .withAdvancedConfig(null, null, ViewportApplyMode.SYSTEM, false, false)
        viewModel.restoreEditingSession(
            saved.packageName,
            saved,
            saved,
            ConfigEditorDestination.HOOK_CHAIN_INTERFACE,
        )

        viewModel.clearEditingDraft()

        assertEquals(
            ViewportApplyMode.SYSTEM,
            viewModel.getLastClosedEditingDraft(saved.packageName)?.viewportApplyMode,
        )
        assertNull(viewModel.getLastClosedEditingDraft("com.other.app"))
    }

    @Test
    fun approvedScopeUpdatesOnlyTheMatchingEditorSession() {
        val viewModel = MainViewModel(emptyState())
        val current = editorDraft("com.example.app", "125").withScopeSelected(false)
        val saved = editorDraft("com.example.app", "110").withScopeSelected(false)
        viewModel.restoreEditingSession(current.packageName, current, saved, ConfigEditorDestination.MAIN)

        assertTrue(viewModel.markEditingScopeSelected("com.example.app"))
        assertTrue(viewModel.editingDraft!!.scopeSelected)
        assertTrue(viewModel.savedEditingDraft!!.scopeSelected)
        assertEquals("125", viewModel.editingDraft!!.viewportInput)
        assertFalse(viewModel.markEditingScopeSelected("com.example.other"))
    }

    @Test
    fun requestLoadEmitsStartEffectWithForceReload() {
        val requests = MainViewModel(emptyState()).dispatch(MainUiAction.requestAppsLoad(true))

        assertEquals(1, requests.size)
        assertEquals(1, requests[0].requestId)
        assertTrue(requests[0].forceInstalledAppCatalogReload)
    }

    @Test
    fun restoredSnapshotFirstBackgroundRefreshDoesNotForceCatalogReload() {
        val restoredState = MainUiState.initial(
            "",
            AppListFilterState.defaultState(),
            listOf(app("Restored", "com.example.restored", true, false)),
            emptySet(),
        )
        val requests = MainViewModel(restoredState).dispatch(MainUiAction.requestAppsLoad(false))

        assertEquals(1, requests.size)
        assertFalse(requests[0].forceInstalledAppCatalogReload)
    }

    @Test
    fun queuedLoadEmitsFollowUpEffectAndAppliesLatestResult() {
        val viewModel = MainViewModel(emptyState())
        val firstRequest = viewModel.dispatch(MainUiAction.requestAppsLoad(false)).single()

        assertTrue(viewModel.dispatch(MainUiAction.requestAppsLoad(true)).isEmpty())

        val followUp = viewModel.dispatch(MainUiAction.appsLoadFinished(
            firstRequest.requestId,
            listOf(app("Old", "com.example.old", true, false)),
        ))
        assertEquals(1, followUp.size)
        val secondRequest = followUp.single()
        assertEquals(2, secondRequest.requestId)
        assertTrue(secondRequest.forceInstalledAppCatalogReload)
        assertTrue(viewModel.state.appsSnapshot().isEmpty())

        assertTrue(viewModel.dispatch(MainUiAction.appsLoadFinished(
            secondRequest.requestId,
            listOf(app("Latest", "com.example.latest", true, false)),
        )).isEmpty())
        assertEquals(1, viewModel.state.appsSnapshot().size)
        assertEquals("com.example.latest", viewModel.state.appsSnapshot()[0].packageName)
    }

    @Test
    fun queryAndFilterUpdatesVisibleSections() {
        val initial = MainUiState.initial(
            "",
            AppListFilterState.noAdditionalConstraints(),
            listOf(
                app("Alpha Tool", "com.example.alpha", true, false),
                app("System Alpha", "com.example.system", true, true),
            ),
            emptySet(),
        )
        val viewModel = MainViewModel(initial)

        viewModel.dispatch(MainUiAction.queryChanged("alpha"))
        assertEquals(2, viewModel.state.visibleItems(AppListPage.ALL_APPS).size)

        viewModel.dispatch(MainUiAction.filterChanged(AppListFilterState(false, false, false, false)))
        val filtered = viewModel.state.visibleItems(AppListPage.ALL_APPS)
        assertEquals(1, filtered.size)
        assertEquals("com.example.alpha", filtered[0].packageName)
    }

    @Test
    fun workspaceModeKeepsSeparateAppAndTemplateSearchQueries() {
        val initial = MainUiState.initial(
            "alpha",
            AppListFilterState(false, true, false, false),
            listOf(
                app("Alpha Tool", "com.example.alpha", true, false),
                app("Beta Tool", "com.example.beta", true, false),
            ),
            emptySet(),
        )
        val viewModel = MainViewModel(initial)

        viewModel.dispatch(MainUiAction.workspaceModeChanged(MainUiState.WorkspaceMode.TEMPLATE))
        val templateState = viewModel.state
        assertEquals(MainUiState.WorkspaceMode.TEMPLATE, templateState.workspaceMode)
        assertEquals("", templateState.currentQuery())
        assertEquals("alpha", templateState.appQuery)
        assertTrue(templateState.filterState.injectedOnly())
        assertEquals(1, templateState.visibleItems(AppListPage.ALL_APPS).size)

        viewModel.dispatch(MainUiAction.queryChanged("template"))
        assertEquals("template", viewModel.state.currentQuery())
        assertEquals("alpha", viewModel.state.appQuery)
        assertEquals("template", viewModel.state.templateQuery)

        viewModel.dispatch(MainUiAction.workspaceModeChanged(MainUiState.WorkspaceMode.APP))
        val appState = viewModel.state
        assertEquals(MainUiState.WorkspaceMode.APP, appState.workspaceMode)
        assertEquals("alpha", appState.currentQuery())
        assertEquals("template", appState.templateQuery)
        assertTrue(appState.filterState.injectedOnly())
        assertEquals("com.example.alpha", appState.visibleItems(AppListPage.ALL_APPS)[0].packageName)
    }

    @Test
    fun pageRefreshSetsRefreshingStateUntilLoadSettles() {
        val viewModel = MainViewModel(emptyState())
        viewModel.dispatch(MainUiAction.markPageRefreshing(AppListPage.ALL_APPS))
        val request = viewModel.dispatch(MainUiAction.requestAppsLoad(true)).single()
        assertTrue(viewModel.state.isRefreshing(AppListPage.ALL_APPS))

        viewModel.dispatch(MainUiAction.appsLoadFinished(request.requestId, emptyList()))
        assertFalse(viewModel.state.isRefreshing(AppListPage.ALL_APPS))
    }

    private fun editorDraft(packageName: String, viewportInput: String) = EditorDraft(
        packageName, viewportInput, viewportInput, "", "relative_scale", "",
        FontApplyMode.SYSTEM_EMULATION, null, null, ViewportApplyMode.OFF, false, false, "", true, true,
    )

    private fun emptyState() = MainUiState.initial(
        "", AppListFilterState.defaultState(), emptyList(), emptySet(),
    )

    private fun app(label: String, packageName: String, inScope: Boolean, systemApp: Boolean) = AppListItem(
        label, packageName, inScope, true, null, ViewportApplyMode.OFF, null,
        FontApplyMode.OFF, true, systemApp, false, null,
    )
}
