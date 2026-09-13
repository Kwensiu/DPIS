package com.dpis.module.ui.presentation

import android.content.Intent
import android.os.Bundle
import com.dpis.module.DpisApplication
import com.dpis.module.MainActivity
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListFilterStateStore
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppListPage
import com.dpis.module.diagnostics.presentation.FeedbackDiagnosticActivitySession

import com.dpis.module.root.RootAccessProbe
import com.dpis.module.runtime.ModuleRuntimeReloadNoticeCoordinator
import com.dpis.module.settings.PageSettingsStore
import com.dpis.module.settings.SystemScopeCoordinator
import com.dpis.module.templates.presentation.TemplateWorkspaceActivitySession
import com.dpis.module.ui.ConfigEditorDestination
import com.dpis.module.ui.MainUiAction
import com.dpis.module.ui.MainUiState
import com.dpis.module.ui.MainViewModel
import com.dpis.module.updates.UpdatePromptRequest
import com.dpis.module.updates.presentation.MainUpdateSession
import java.util.EnumSet

/**
 * Owns MainActivity launch, lifecycle forwarding after attach, retained-state
 * capture, and the initial shell snapshot.
 */
class MainStartupSession(
    private val activity: MainActivity,
    private val updateSession: MainUpdateSession,
    private val hostWiringSession: MainHostWiringSession,
    private val mainWorkspaceSession: MainWorkspaceSession,
) {
    var filterStore: AppListFilterStateStore? = null
        private set
    var feedbackDiagnostic: FeedbackDiagnosticActivitySession? = null
        private set
    var viewModel: MainViewModel? = null
        private set
    var skipNextImmediateServiceReload = false
        private set
    var currentAppListPage = AppListPage.ALL_APPS
        private set
    var workspaceSession: TemplateWorkspaceActivitySession? = null
        private set
    var isSystemHookEnabledFromStore = false
        private set

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

    fun launch(savedInstanceState: Bundle?) {
        val filterStore = AppListFilterStateStore(activity)
        this.filterStore = filterStore
        @Suppress("DEPRECATION")
        val retainedState = activity.lastCustomNonConfigurationInstance as? MainRetainedState
        val restore = restore(
            savedInstanceState,
            retainedState,
            filterStore.load(),
            MainUiState.WorkspaceMode.valueOf(
                PageSettingsStore.getDefaultStartupPage(activity),
            ),
        )
        val feedbackDiagnostic = FeedbackDiagnosticActivitySession(
            activity,
            retainedState?.feedbackDiagnostic,
        )
        this.feedbackDiagnostic = feedbackDiagnostic
        if (retainedState != null) {
            updateSession.restorePendingPrompt(retainedState.pendingUpdatePrompt)
            activity.scrollStateStore.restore(retainedState.appListScrollPositions)
        }
        skipNextImmediateServiceReload = restore.skipNextImmediateServiceReload
        val viewModel = MainViewModel(
            MainUiState.initial(
                restore.query,
                restore.templateQuery,
                restore.filterState,
                restore.appsSnapshot,
                restore.refreshingPages,
                restore.workspaceMode,
            ),
        )
        this.viewModel = viewModel
        refreshSystemHookEffectiveEnabled()
        initializeWorkspaceSession(
            restore.workspaceSessionState,
            restore.templateQuery,
        )
        ensureWorkspaceSession().restore(savedInstanceState)
        hostWiringSession.wire(viewModel)
        val restoredPage = restoreCurrentPage(savedInstanceState, retainedState)
        if (restoredPage != null) {
            setCurrentAppListPage(restoredPage, false)
        }

        mainWorkspaceSession.render(requireUiState())
        mainWorkspaceSession.installComposeWorkspaceShell()
        feedbackDiagnostic.restorePage()
        feedbackDiagnostic.attachHost()
        // The service state callback is not guaranteed to fire on every Wear image.
        // Request the catalog explicitly; MainViewModel coalesces any later service reload.
        requestAppsLoad()
        if (restoreEditingSession(viewModel, retainedState)) {
            mainWorkspaceSession.restoreAppEditorForCurrentWorkspace()
        }
        mainWorkspaceSession.restoreWorkspaceEditorForCurrentConfiguration()
        if (updateSession.showPendingPromptIfAny()) {
            return
        }
        if (maybeShowModuleRuntimeReloadAdvice()) {
            return
        }
        continueStartupDialogs()
    }

    fun consumeSkipNextImmediateServiceReload(): Boolean {
        if (!skipNextImmediateServiceReload) {
            return false
        }
        skipNextImmediateServiceReload = false
        return true
    }

    fun maybeShowModuleRuntimeReloadAdvice(): Boolean {
        return ModuleRuntimeReloadNoticeCoordinator(activity)
            .maybeShow { continueStartupDialogs() }
    }

    fun continueStartupDialogs() {
        if (!updateSession.maybeShowStartupDisclaimerDialog()) {
            updateSession.maybeCheckForUpdatesOnStartup()
        }
    }

    fun requireUiState(): MainUiState {
        val viewModel = viewModel ?: return MainUiState.initial(
            "",
            AppListFilterState.defaultState(),
            emptyList(),
            emptySet(),
        )
        return viewModel.state
    }

    fun dispatch(action: MainUiAction?) {
        val viewModel = viewModel ?: return
        val requests = viewModel.dispatch(action)
        mainWorkspaceSession.render(viewModel.state)
        handleAppsLoadRequests(requests)
    }

    fun requestAppsLoad() {
        activity.installedAppsLoadSession.requestLoad(false)
    }

    fun dispatchInstalledAppsLoad(forceReload: Boolean) {
        dispatch(MainUiAction.requestAppsLoad(forceReload))
    }

    fun dispatchInstalledAppsLoadFinished(requestId: Int, loaded: List<AppListItem>?) {
        dispatch(MainUiAction.appsLoadFinished(requestId, loaded))
    }

    fun onPageRefreshRequested(page: AppListPage?) {
        dispatch(MainUiAction.markPageRefreshing(page))
        activity.installedAppsLoadSession.requestLoad(true)
    }

    fun setCurrentAppListPage(page: AppListPage?, submit: Boolean) {
        currentAppListPage = page ?: AppListPage.ALL_APPS
        if (submit) {
            mainWorkspaceSession.refreshApps()
        }
    }

    fun initializeWorkspaceSession(
        initialState: TemplateWorkspaceActivitySession.State?,
        initialQuery: String?,
    ) {
        if (workspaceSession == null) {
            workspaceSession = TemplateWorkspaceActivitySession(
                activity,
                initialQuery.orEmpty(),
                initialState,
            ) { mainWorkspaceSession.refreshTemplates() }
        }
    }

    fun ensureWorkspaceSession(): TemplateWorkspaceActivitySession {
        initializeWorkspaceSession(null, requireUiState().currentQuery())
        return checkNotNull(workspaceSession)
    }

    fun bindHomeWorkspaceIfVisible() {
        if (viewModel != null &&
            requireUiState().workspaceMode == MainUiState.WorkspaceMode.HOME
        ) {
            mainWorkspaceSession.bindHomeWorkspace()
        }
    }

    fun refreshSystemHookEffectiveEnabled() {
        isSystemHookEnabledFromStore =
            SystemScopeCoordinator.resolveSystemHookEffectiveEnabled(activity.hookConfigStore)
    }

    private fun handleAppsLoadRequests(requests: List<MainViewModel.AppsLoadRequest>?) {
        if (requests.isNullOrEmpty()) {
            return
        }
        for (request in requests) {
            activity.installedAppsLoadSession.start(request)
        }
    }

    fun onStart() {
        refreshSystemHookEffectiveEnabled()
        mainWorkspaceSession.bindForLifecycle(requireUiState().workspaceMode)
        hostWiringSession.toolsWorkspace?.onStart()
        hostWiringSession.settingsWorkspaceSession?.onStart()
        DpisApplication.addServiceStateListener(activity, true)
    }

    fun onResume() {
        maybeStartRootAccessProbe()
        hostWiringSession.toolsWorkspace?.onResume()
        hostWiringSession.settingsWorkspaceSession?.onResume()
    }

    fun onStop() {
        hostWiringSession.toolsWorkspace?.onStop()
        hostWiringSession.settingsWorkspaceSession?.onStop()
        DpisApplication.removeServiceStateListener(activity)
    }

    fun onDestroy() {
        feedbackDiagnostic?.onDestroy(activity.isChangingConfigurations)
        updateSession.shutdown()
        ensureWorkspaceSession().onDestroy()
        hostWiringSession.settingsWorkspaceSession?.onDestroy()
        activity.installedAppsLoadSession.shutdown()
    }

    fun onServiceStateChanged() {
        refreshSystemHookEffectiveEnabled()
        if (requireUiState().workspaceMode == MainUiState.WorkspaceMode.HOME) {
            mainWorkspaceSession.bindHomeWorkspace()
        }
        hostWiringSession.settingsWorkspaceSession?.onServiceStateChanged()
        if (consumeSkipNextImmediateServiceReload()) {
            return
        }
        requestAppsLoad()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        hostWiringSession.settingsWorkspaceSession?.onActivityResult(
            requestCode,
            resultCode,
            data,
        )
        hostWiringSession.toolsWorkspace?.onActivityResult(requestCode, resultCode, data)
        if (ensureWorkspaceSession().handleActivityResult(requestCode, data)) {
            return
        }
        feedbackDiagnostic?.handleActivityResult(requestCode, resultCode, data)
    }

    fun onSaveInstanceState(outState: Bundle) {
        saveInstanceState(
            outState,
            requireUiState(),
            currentAppListPage.position(),
        )
        ensureWorkspaceSession().saveState(outState)
    }

    fun onRequestPermissionsResult(requestCode: Int) {
        activity.installedAppsLoadSession.onRequestPermissionsResult(requestCode)
    }

    fun retainNonConfigurationInstance(): MainRetainedState {
        return retain(
            requireUiState(),
            currentAppListPage.position(),
            activity.scrollStateStore.snapshot(),
            null,
            viewModel,
            ensureWorkspaceSession().retainedState(),
            feedbackDiagnostic?.retainedState(),
            updateSession.pendingUpdatePrompt,
        )
    }

    private fun maybeStartRootAccessProbe() {
        RootAccessProbe.refreshAsync {
            activity.runOnUiThread {
                if (requireUiState().workspaceMode == MainUiState.WorkspaceMode.HOME) {
                    mainWorkspaceSession.bindHomeWorkspace()
                }
            }
        }
    }

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
