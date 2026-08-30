package com.dpis.module

import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppListPage
import com.dpis.module.applist.AppLoadCoordinator

/**
 * Stateful reducer for the main Compose shell and its active app-editor session.
 *
 * Android work stays outside this class: callers execute the returned load effects and dispatch
 * their results back here. The editor fields deliberately remain alongside shell state because
 * editor presentation must survive asynchronous catalog refreshes without making Activity own it.
 */
internal class MainViewModel(initialState: MainUiState?) {
    class AppsLoadRequest(
        @JvmField val requestId: Int,
        @JvmField val forceInstalledAppCatalogReload: Boolean,
    )

    private val loadCoordinator = AppLoadCoordinator()
    private var forceInstalledAppCatalogReloadRequested: Boolean

    var state: MainUiState = initialState ?: MainUiState.initial(
        "",
        AppListFilterState.defaultState(),
        emptyList(),
        emptySet(),
    )
        private set

    var editingPackageName: String? = null
    var editingDraft: EditorDraft? = null
    var savedEditingDraft: EditorDraft? = null

    // Keep the last committed editor snapshot while catalog refresh is asynchronous. Reopening the
    // same package restores the committed mode, while clearEditingDraft() still discards edits.
    private var lastClosedEditingPackageName: String? = null
    private var lastClosedEditingDraft: EditorDraft? = null

    var editingDestination: ConfigEditorDestination = ConfigEditorDestination.MAIN
    var isEditingSaveFeedback: Boolean = false

    init {
        forceInstalledAppCatalogReloadRequested = state.appsSnapshot().isEmpty()
    }

    fun clearEditingPackageName() {
        editingPackageName = null
    }

    /** Applies scope approval to the active Compose editor without replacing its unsaved fields. */
    fun markEditingScopeSelected(packageName: String?): Boolean {
        val draft = editingDraft
        if (packageName == null
            || draft == null
            || packageName != draft.packageName
            || draft.scopeSelected
        ) {
            return false
        }
        editingDraft = draft.withScopeSelected(true)
        val savedDraft = savedEditingDraft
        if (savedDraft != null && packageName == savedDraft.packageName) {
            savedEditingDraft = savedDraft.withScopeSelected(true)
        }
        return true
    }

    fun clearEditingDraft() {
        if (editingPackageName != null && savedEditingDraft != null) {
            lastClosedEditingPackageName = editingPackageName
            lastClosedEditingDraft = savedEditingDraft
        }
        editingDraft = null
        savedEditingDraft = null
        editingDestination = ConfigEditorDestination.MAIN
        isEditingSaveFeedback = false
    }

    fun getLastClosedEditingDraft(packageName: String?): EditorDraft? =
        if (packageName != null && packageName == lastClosedEditingPackageName) {
            lastClosedEditingDraft
        } else {
            null
        }

    fun restoreEditingSession(
        packageName: String?,
        draft: EditorDraft?,
        savedDraft: EditorDraft?,
        destination: ConfigEditorDestination?,
    ) {
        editingPackageName = packageName
        editingDraft = draft
        savedEditingDraft = savedDraft ?: draft
        editingDestination = destination ?: ConfigEditorDestination.MAIN
        isEditingSaveFeedback = false
    }

    fun dispatch(action: MainUiAction?): List<AppsLoadRequest> = when (action) {
        null -> emptyList()
        is MainUiAction.QueryChanged -> {
            state = state.withQuery(action.query)
            emptyList()
        }
        is MainUiAction.FilterChanged -> {
            state = state.withFilterState(action.filterState)
            emptyList()
        }
        is MainUiAction.MarkPageRefreshing -> {
            state = state.withRefreshingPage(action.page, true)
            emptyList()
        }
        is MainUiAction.WorkspaceModeChanged -> {
            state = state.withWorkspaceMode(action.workspaceMode)
            emptyList()
        }
        is MainUiAction.RequestAppsLoad -> requestAppsLoad(action.forceInstalledAppCatalogReload)
        is MainUiAction.AppsLoadFinished -> onAppsLoadFinished(action.requestId, action.loadedApps)
        else -> emptyList()
    }

    private fun requestAppsLoad(forceInstalledAppCatalogReload: Boolean): List<AppsLoadRequest> {
        if (forceInstalledAppCatalogReload) forceInstalledAppCatalogReloadRequested = true
        val requestId = loadCoordinator.onLoadRequested()
        if (requestId == AppLoadCoordinator.NO_REQUEST) return emptyList()

        // An empty snapshot is not a settled empty state while the initial catalog request runs.
        // Explicit refreshes already mark their selected page and must not flash the other page.
        if (state.appsSnapshot().isEmpty()) {
            state = state.withRefreshingPage(AppListPage.ALL_APPS, true)
                .withRefreshingPage(AppListPage.CONFIGURED_APPS, true)
        }
        return listOf(createAppsLoadRequest(requestId))
    }

    private fun onAppsLoadFinished(
        requestId: Int,
        loadedApps: List<AppListItem>?,
    ): List<AppsLoadRequest> {
        val completion = loadCoordinator.onLoadFinished(requestId)
        if (completion.shouldApplyResult && loadedApps != null) {
            state = state.withApps(loadedApps)
            val lastClosedPackageName = lastClosedEditingPackageName
            if (lastClosedPackageName != null && loadedApps.any { it.packageName == lastClosedPackageName }) {
                lastClosedEditingPackageName = null
                lastClosedEditingDraft = null
            }
        }
        if (completion.nextRequestId != AppLoadCoordinator.NO_REQUEST) {
            return listOf(createAppsLoadRequest(completion.nextRequestId))
        }
        state = state.clearRefreshingPages()
        return emptyList()
    }

    private fun createAppsLoadRequest(requestId: Int) = AppsLoadRequest(
        requestId,
        consumeForceInstalledAppCatalogReloadRequested(),
    )

    private fun consumeForceInstalledAppCatalogReloadRequested(): Boolean =
        forceInstalledAppCatalogReloadRequested.also {
            forceInstalledAppCatalogReloadRequested = false
        }
}
