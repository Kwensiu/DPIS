package com.dpis.module.home.presentation

import android.app.Activity
import android.content.Intent
import com.dpis.module.DpisApplication
import com.dpis.module.applist.AppListPage
import com.dpis.module.applist.InstalledAppCatalogCoordinator
import com.dpis.module.applist.ScopeState
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.fonts.FontLibraryActivity
import com.dpis.module.home.DonateActivity
import com.dpis.module.home.HomeActivationStateResolver
import com.dpis.module.home.HomeUpdateUiState
import com.dpis.module.home.HomeWorkspaceActions
import com.dpis.module.home.HomeWorkspaceLayout
import com.dpis.module.home.HomeWorkspaceLayoutStore
import com.dpis.module.home.HomeWorkspaceState
import com.dpis.module.home.ModeHelpActivity
import com.dpis.module.root.RootAccessProbe
import com.dpis.module.runtime.ConfigStoreFactory
import com.dpis.module.settings.PageSettingsStore
import com.dpis.module.ui.MainUiAction
import com.dpis.module.ui.MainUiState

/**
 * Owns Home workspace snapshots and navigation actions.
 * [com.dpis.module.MainActivity] only forwards remaining host calls.
 */
class HomeWorkspaceSession(
    private val shell: Shell,
) {
    interface Shell {
        fun activity(): Activity

        fun hookConfigStore(): DpisConfigStore?

        fun loadScopeState(): ScopeState

        fun quickItemCount(): Int

        fun homeUpdateUiState(): HomeUpdateUiState

        fun checkForUpdatesNow()

        fun setCurrentAppListPage(page: AppListPage, submit: Boolean)

        fun dispatch(action: MainUiAction)

        fun bindHomeWorkspace()
    }

    fun createState(): HomeWorkspaceState {
        val activity = shell.activity()
        val configStore = shell.hookConfigStore()
        val visibleConfiguredAppCount =
            InstalledAppCatalogCoordinator.countUserVisibleConfiguredPackages(
                configStore,
                shell.loadScopeState(),
            )
        return HomeWorkspaceState(
            isActivatedForHome(),
            visibleConfiguredAppCount,
            ConfigStoreFactory.createLocalUiFontLibraryStore(
                activity,
                DpisApplication.xposedService,
            ).listFonts().size,
            shell.quickItemCount(),
            RootAccessProbe.cachedResult(),
            shell.homeUpdateUiState(),
            HomeWorkspaceLayoutStore(activity).load(),
            createActions(),
            PageSettingsStore.isHomeEditButtonVisible(activity),
        )
    }

    private fun isActivatedForHome(): Boolean {
        val libXposedService = HomeActivationStateResolver
            .hasModernLibXposedService(DpisApplication.xposedService)
        val selfLoaded = DpisApplication.isXposedSelfLoaded()
        val activated = HomeActivationStateResolver.isActivatedForHome(
            libXposedService,
            selfLoaded,
        )
        DpisLog.i(
            "home activation resolved: libxposedService=" + libXposedService
                + ", selfLoaded=" + selfLoaded
                + ", activated=" + activated,
        )
        return activated
    }

    private fun createActions(): HomeWorkspaceActions {
        val activity = shell.activity()
        return object : HomeWorkspaceActions {
            override fun checkForUpdates() {
                shell.checkForUpdatesNow()
            }

            override fun openConfiguredAppsWorkspace() {
                shell.setCurrentAppListPage(AppListPage.CONFIGURED_APPS, false)
                shell.dispatch(
                    MainUiAction.workspaceModeChanged(MainUiState.WorkspaceMode.APP),
                )
            }

            override fun openFontLibrary() {
                activity.startActivity(Intent(activity, FontLibraryActivity::class.java))
            }

            override fun openTemplateWorkspace() {
                shell.dispatch(
                    MainUiAction.workspaceModeChanged(MainUiState.WorkspaceMode.TEMPLATE),
                )
            }

            override fun openModeHelp() {
                activity.startActivity(Intent(activity, ModeHelpActivity::class.java))
            }

            override fun openDonate() {
                activity.startActivity(DonateActivity.createIntent(activity))
            }

            override fun saveHomeWorkspaceLayout(layout: HomeWorkspaceLayout) {
                HomeWorkspaceLayoutStore(activity).save(layout)
                shell.bindHomeWorkspace()
            }
        }
    }
}
