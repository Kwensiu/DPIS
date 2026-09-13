package com.dpis.module.applist.presentation

import android.app.Activity
import com.dpis.module.MainActivity
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.ui.MainUiAction
import com.dpis.module.ui.MainUiState

/** Wires catalogue filter changes to MainActivity platform capabilities. */
class AppListFilterShell(
    private val activity: MainActivity,
) : AppListFilterSession.Shell {
    override fun activity(): Activity = activity

    override fun requireUiState(): MainUiState = activity.startupSession.requireUiState()

    override fun saveFilterState(filterState: AppListFilterState) {
        activity.startupSession.filterStore?.save(filterState)
    }

    override fun dispatch(action: MainUiAction) = activity.startupSession.dispatch(action)
}
