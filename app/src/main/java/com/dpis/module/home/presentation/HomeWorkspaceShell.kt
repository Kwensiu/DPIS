package com.dpis.module.home.presentation

import android.app.Activity
import com.dpis.module.MainActivity
import com.dpis.module.applist.AppListPage
import com.dpis.module.applist.ScopeState
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.home.HomeUpdateUiState
import com.dpis.module.ui.MainUiAction

/** Wires Home workspace snapshots to MainActivity platform capabilities. */
class HomeWorkspaceShell(
    private val activity: MainActivity,
) : HomeWorkspaceSession.Shell {
    override fun activity(): Activity = activity

    override fun hookConfigStore(): DpisConfigStore? = activity.hookConfigStore

    override fun loadScopeState(): ScopeState = activity.loadInstalledAppScopeState()

    override fun quickItemCount(): Int = activity.ensureWorkspaceSession().quickItemCount()

    override fun homeUpdateUiState(): HomeUpdateUiState = activity.homeUpdateUiState()

    override fun checkForUpdatesNow() = activity.checkForUpdatesNow()

    override fun setCurrentAppListPage(page: AppListPage, submit: Boolean) =
        activity.setCurrentAppListPage(page, submit)

    override fun dispatch(action: MainUiAction) = activity.dispatchMainUiAction(action)

    override fun bindHomeWorkspace() = activity.bindHomeWorkspace()
}
