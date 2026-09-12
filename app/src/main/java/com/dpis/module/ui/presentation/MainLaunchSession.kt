package com.dpis.module.ui.presentation

import android.os.Bundle
import com.dpis.module.MainActivity
import com.dpis.module.applist.AppListFilterStateStore
import com.dpis.module.applist.AppListPage
import com.dpis.module.diagnostics.presentation.FeedbackDiagnosticActivitySession
import com.dpis.module.diagnostics.presentation.FeedbackDiagnosticShell
import com.dpis.module.runtime.ModuleRuntimeReloadNoticeCoordinator
import com.dpis.module.settings.PageSettingsStore
import com.dpis.module.templates.presentation.TemplateWorkspaceActivitySession
import com.dpis.module.ui.MainUiState
import com.dpis.module.ui.MainViewModel
import com.dpis.module.updates.presentation.MainUpdateSession

/**
 * Owns the MainActivity onCreate launch sequence after the layout is attached:
 * retained restore, host wiring, first paint, catalogue load, editor restore,
 * and startup dialogs. [com.dpis.module.MainActivity] keeps lifecycle fields.
 */
class MainLaunchSession(
    private val shell: Shell,
    private val startupSession: MainStartupSession,
    private val updateSession: MainUpdateSession,
    private val hostWiringSession: MainHostWiringSession,
    private val mainWorkspaceSession: MainWorkspaceSession,
) {
    interface Shell {
        fun activity(): MainActivity

        fun lastRetained(): MainRetainedState?

        fun attachFilterStore(store: AppListFilterStateStore)

        fun setFeedbackDiagnostic(session: FeedbackDiagnosticActivitySession)

        fun restoreAppListScroll(positions: IntArray)

        fun setSkipNextImmediateServiceReload(skip: Boolean)

        fun setMainViewModel(viewModel: MainViewModel)

        fun initializeWorkspaceSession(
            initialState: TemplateWorkspaceActivitySession.State?,
            initialQuery: String,
        )

        fun restoreWorkspaceSession(savedInstanceState: Bundle?)

        fun setCurrentAppListPage(page: AppListPage, submit: Boolean)

        fun requireUiState(): MainUiState

        fun requestAppsLoad()

        fun restoreFeedbackPage()

        fun attachFeedbackHost()
    }

    fun launch(savedInstanceState: Bundle?) {
        val activity = shell.activity()
        val filterStore = AppListFilterStateStore(activity)
        shell.attachFilterStore(filterStore)

        val retainedState = shell.lastRetained()
        val restore = startupSession.restore(
            savedInstanceState,
            retainedState,
            filterStore.load(),
            MainUiState.WorkspaceMode.valueOf(
                PageSettingsStore.getDefaultStartupPage(activity),
            ),
        )
        shell.setFeedbackDiagnostic(
            FeedbackDiagnosticActivitySession(
                FeedbackDiagnosticShell(activity),
                retainedState?.feedbackDiagnostic,
            ),
        )
        if (retainedState != null) {
            updateSession.restorePendingPrompt(retainedState.pendingUpdatePrompt)
            shell.restoreAppListScroll(retainedState.appListScrollPositions)
        }
        shell.setSkipNextImmediateServiceReload(restore.skipNextImmediateServiceReload)
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
        shell.setMainViewModel(viewModel)
        shell.initializeWorkspaceSession(
            restore.workspaceSessionState,
            restore.templateQuery,
        )
        shell.restoreWorkspaceSession(savedInstanceState)
        hostWiringSession.wire(viewModel)
        val restoredPage = startupSession.restoreCurrentPage(
            savedInstanceState,
            retainedState,
        )
        if (restoredPage != null) {
            shell.setCurrentAppListPage(restoredPage, false)
        }

        mainWorkspaceSession.render(shell.requireUiState())
        mainWorkspaceSession.installComposeWorkspaceShell()
        shell.restoreFeedbackPage()
        shell.attachFeedbackHost()
        // The service state callback is not guaranteed to fire on every Wear image.
        // Request the catalog explicitly; MainViewModel coalesces any later service reload.
        shell.requestAppsLoad()
        if (startupSession.restoreEditingSession(viewModel, retainedState)) {
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

    fun maybeShowModuleRuntimeReloadAdvice(): Boolean {
        return ModuleRuntimeReloadNoticeCoordinator(shell.activity())
            .maybeShow { continueStartupDialogs() }
    }

    fun continueStartupDialogs() {
        if (!updateSession.maybeShowStartupDisclaimerDialog()) {
            updateSession.maybeCheckForUpdatesOnStartup()
        }
    }
}
