package com.dpis.module.ui.presentation

import android.os.Bundle
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppListPage
import com.dpis.module.diagnostics.presentation.FeedbackDiagnosticActivitySession
import com.dpis.module.templates.presentation.TemplateWorkspaceActivitySession
import com.dpis.module.ui.ConfigEditorDestination
import com.dpis.module.ui.MainUiState
import com.dpis.module.ui.MainViewModel
import com.dpis.module.updates.UpdatePromptRequest
import java.util.EnumSet

/**
 * Owns MainActivity retained-state capture, bundle restore, and the initial
 * shell snapshot. [com.dpis.module.MainActivity] wires hosts after restore.
 */
class MainStartupSession {
    class Restore(
        @JvmField val query: String,
        @JvmField val templateQuery: String,
        @JvmField val filterState: AppListFilterState,
        @JvmField val workspaceMode: MainUiState.WorkspaceMode,
        @JvmField val appsSnapshot: List<AppListItem>,
        @JvmField val refreshingPages: Set<AppListPage>,
        @JvmField val workspaceSessionState: TemplateWorkspaceActivitySession.State?,
        @JvmField val skipNextImmediateServiceReload: Boolean,
    )

    fun restore(
        savedInstanceState: Bundle?,
        retained: MainRetainedState?,
        defaultFilterState: AppListFilterState,
        defaultWorkspaceMode: MainUiState.WorkspaceMode,
    ): Restore {
        var query = ""
        var templateQuery = ""
        var filterState = defaultFilterState
        var workspaceMode = defaultWorkspaceMode
        var workspaceSessionState: TemplateWorkspaceActivitySession.State? = null
        var appsSnapshot: List<AppListItem> = emptyList()
        var refreshingPages: Set<AppListPage> = EnumSet.noneOf(AppListPage::class.java)
        var skipNextImmediateServiceReload = false
        if (retained != null) {
            query = retained.query
            templateQuery = retained.templateQuery
            filterState = retained.filterState
            workspaceMode = retained.workspaceMode
            workspaceSessionState = retained.workspaceSessionState
            refreshingPages = decodeRefreshingPages(retained.refreshingPagePositions)
            appsSnapshot = ArrayList(retained.appsSnapshot)
            skipNextImmediateServiceReload = appsSnapshot.isNotEmpty()
        }
        if (savedInstanceState != null) {
            query = savedInstanceState.getString(STATE_CURRENT_QUERY, "") ?: ""
            templateQuery = savedInstanceState.getString(STATE_TEMPLATE_QUERY, "") ?: ""
            filterState = AppListFilterState(
                parseAppType(
                    savedInstanceState.getString(STATE_FILTER_APP_TYPE),
                    savedInstanceState.getBoolean(STATE_FILTER_SHOW_SYSTEM, false),
                ),
                savedInstanceState.getBoolean(STATE_FILTER_INJECTED_ONLY, false),
                savedInstanceState.getBoolean(STATE_FILTER_DISABLED_ONLY, false),
                savedInstanceState.getBoolean(STATE_FILTER_WIDTH_ONLY, false),
                savedInstanceState.getBoolean(STATE_FILTER_FONT_ONLY, false),
                savedInstanceState.getBoolean(STATE_FILTER_TYPEFACE_ONLY, false),
                savedInstanceState.getBoolean(STATE_FILTER_HOOK_ONLY, false),
                parseSortOrder(savedInstanceState.getString(STATE_FILTER_SORT_ORDER)),
                savedInstanceState.getBoolean(STATE_FILTER_REVERSE, false),
            )
            workspaceMode = MainUiState.WorkspaceMode.fromName(
                savedInstanceState.getString(STATE_WORKSPACE_MODE),
            )
            refreshingPages = decodeRefreshingPages(
                savedInstanceState.getIntArray(STATE_REFRESHING_PAGES),
            )
        }
        return Restore(
            query,
            templateQuery,
            filterState,
            workspaceMode,
            appsSnapshot,
            refreshingPages,
            workspaceSessionState,
            skipNextImmediateServiceReload,
        )
    }

    fun restoreCurrentPage(
        savedInstanceState: Bundle?,
        retained: MainRetainedState?,
    ): AppListPage? {
        if (savedInstanceState != null) {
            return AppListPage.fromPosition(
                savedInstanceState.getInt(STATE_CURRENT_PAGE, 0),
            )
        }
        if (retained != null) {
            return AppListPage.fromPosition(retained.currentPage)
        }
        return null
    }

    fun restoreEditingSession(
        viewModel: MainViewModel,
        retained: MainRetainedState?,
    ): Boolean {
        if (retained?.editingPackageName == null) {
            return false
        }
        viewModel.restoreEditingSession(
            retained.editingPackageName,
            retained.editingDraft,
            retained.savedEditingDraft,
            retained.editingDestination,
            retained.prefillSnapshot,
            retained.prefillInvalidated,
        )
        return true
    }

    fun retain(
        state: MainUiState,
        currentPage: Int,
        scrollPositions: IntArray,
        draft: EditorDraft?,
        viewModel: MainViewModel?,
        workspaceSessionState: TemplateWorkspaceActivitySession.State?,
        feedbackDiagnostic: FeedbackDiagnosticActivitySession.State?,
        pendingUpdatePrompt: UpdatePromptRequest?,
    ): MainRetainedState {
        val snapshot = state.appsSnapshot()
        val editingDraft = draft ?: viewModel?.editingDraft
        val editorSession = viewModel?.editorSession
        return MainRetainedState(
            snapshot,
            state.appQuery,
            state.templateQuery,
            state.filterState,
            state.workspaceMode,
            currentPage,
            scrollPositions,
            captureRefreshingPagePositions(state),
            viewModel?.editingPackageName,
            editingDraft,
            viewModel?.savedEditingDraft,
            editorSession?.prefillSnapshot,
            editorSession?.prefillInvalidated == true,
            viewModel?.editingDestination ?: ConfigEditorDestination.MAIN,
            workspaceSessionState,
            feedbackDiagnostic,
            pendingUpdatePrompt,
        )
    }

    fun saveInstanceState(
        outState: Bundle,
        state: MainUiState,
        currentPage: Int,
    ) {
        outState.putString(STATE_CURRENT_QUERY, state.appQuery)
        outState.putString(STATE_TEMPLATE_QUERY, state.templateQuery)
        outState.putString(STATE_WORKSPACE_MODE, state.workspaceMode.name)
        outState.putBoolean(STATE_FILTER_SHOW_SYSTEM, state.filterState.showSystemApps())
        outState.putBoolean(STATE_FILTER_INJECTED_ONLY, state.filterState.injectedOnly())
        outState.putBoolean(STATE_FILTER_WIDTH_ONLY, state.filterState.widthConfiguredOnly())
        outState.putBoolean(STATE_FILTER_FONT_ONLY, state.filterState.fontConfiguredOnly())
        outState.putBoolean(STATE_FILTER_DISABLED_ONLY, state.filterState.disabledOnly())
        outState.putBoolean(STATE_FILTER_TYPEFACE_ONLY, state.filterState.typefaceConfiguredOnly())
        outState.putBoolean(STATE_FILTER_HOOK_ONLY, state.filterState.hookConfiguredOnly())
        outState.putString(STATE_FILTER_APP_TYPE, state.filterState.appType().name)
        outState.putString(STATE_FILTER_SORT_ORDER, state.filterState.sortOrder().name)
        outState.putBoolean(STATE_FILTER_REVERSE, state.filterState.reverseOrder())
        outState.putInt(STATE_CURRENT_PAGE, currentPage)
        outState.putIntArray(
            STATE_REFRESHING_PAGES,
            captureRefreshingPagePositions(state),
        )
    }

    fun captureRefreshingPagePositions(state: MainUiState): IntArray {
        val refreshingPages = state.refreshingPages()
        val positions = IntArray(refreshingPages.size)
        var index = 0
        for (page in refreshingPages) {
            positions[index++] = page.position()
        }
        return positions
    }

    companion object {
        const val STATE_CURRENT_QUERY = "state.current_query"
        const val STATE_TEMPLATE_QUERY = "state.template_query"
        const val STATE_CURRENT_PAGE = "state.current_page"
        const val STATE_WORKSPACE_MODE = "state.workspace_mode"
        const val STATE_FILTER_SHOW_SYSTEM = "state.filter.show_system"
        const val STATE_FILTER_INJECTED_ONLY = "state.filter.injected_only"
        const val STATE_FILTER_WIDTH_ONLY = "state.filter.width_only"
        const val STATE_FILTER_FONT_ONLY = "state.filter.font_only"
        const val STATE_FILTER_DISABLED_ONLY = "state.filter.disabled_only"
        const val STATE_FILTER_TYPEFACE_ONLY = "state.filter.typeface_only"
        const val STATE_FILTER_HOOK_ONLY = "state.filter.hook_only"
        const val STATE_FILTER_APP_TYPE = "state.filter.app_type"
        const val STATE_FILTER_SORT_ORDER = "state.filter.sort_order"
        const val STATE_FILTER_REVERSE = "state.filter.reverse"
        const val STATE_REFRESHING_PAGES = "state.refreshing_pages"

        fun decodeRefreshingPages(pagePositions: IntArray?): Set<AppListPage> {
            val refreshingPages = EnumSet.noneOf(AppListPage::class.java)
            if (pagePositions == null) {
                return refreshingPages
            }
            for (pagePosition in pagePositions) {
                refreshingPages.add(AppListPage.fromPosition(pagePosition))
            }
            return refreshingPages
        }

        fun parseAppType(
            value: String?,
            legacyShowSystem: Boolean,
        ): AppListFilterState.AppType {
            if (value != null) {
                try {
                    return AppListFilterState.AppType.valueOf(value)
                } catch (_: IllegalArgumentException) {
                    // Fall through to the legacy boolean representation.
                }
            }
            return if (legacyShowSystem) {
                AppListFilterState.AppType.ALL
            } else {
                AppListFilterState.AppType.USER
            }
        }

        fun parseSortOrder(value: String?): AppListFilterState.SortOrder {
            if (value != null) {
                try {
                    return AppListFilterState.SortOrder.valueOf(value)
                } catch (_: IllegalArgumentException) {
                    // Older saved state did not include ordering.
                }
            }
            return AppListFilterState.SortOrder.NAME
        }
    }
}
