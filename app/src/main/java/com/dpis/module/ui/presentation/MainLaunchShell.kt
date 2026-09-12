package com.dpis.module.ui.presentation

import android.os.Bundle
import com.dpis.module.MainActivity
import com.dpis.module.applist.AppListFilterStateStore
import com.dpis.module.applist.AppListPage
import com.dpis.module.diagnostics.presentation.FeedbackDiagnosticActivitySession
import com.dpis.module.templates.presentation.TemplateWorkspaceActivitySession
import com.dpis.module.ui.MainUiState
import com.dpis.module.ui.MainViewModel

/** Wires the onCreate launch sequence to MainActivity platform capabilities. */
class MainLaunchShell(
    private val activity: MainActivity,
) : MainLaunchSession.Shell {
    override fun activity(): MainActivity = activity

    override fun lastRetained(): MainRetainedState? = activity.lastRetainedState()

    override fun attachFilterStore(store: AppListFilterStateStore) =
        activity.attachAppListFilterStateStore(store)

    override fun setFeedbackDiagnostic(session: FeedbackDiagnosticActivitySession) =
        activity.setFeedbackDiagnostic(session)

    override fun restoreAppListScroll(positions: IntArray) =
        activity.restoreAppListScrollPositions(positions)

    override fun setSkipNextImmediateServiceReload(skip: Boolean) =
        activity.setSkipNextImmediateServiceReload(skip)

    override fun setMainViewModel(viewModel: MainViewModel) =
        activity.setMainViewModel(viewModel)

    override fun initializeWorkspaceSession(
        initialState: TemplateWorkspaceActivitySession.State?,
        initialQuery: String,
    ) = activity.initializeWorkspaceSession(initialState, initialQuery)

    override fun restoreWorkspaceSession(savedInstanceState: Bundle?) {
        activity.ensureWorkspaceSession().restore(savedInstanceState)
    }

    override fun setCurrentAppListPage(page: AppListPage, submit: Boolean) =
        activity.setCurrentAppListPage(page, submit)

    override fun requireUiState(): MainUiState = activity.requireUiState()

    override fun requestAppsLoad() = activity.requestAppsLoad()

    override fun restoreFeedbackPage() = activity.restoreFeedbackDiagnosticPage()

    override fun attachFeedbackHost() = activity.attachFeedbackDiagnosticHost()
}
