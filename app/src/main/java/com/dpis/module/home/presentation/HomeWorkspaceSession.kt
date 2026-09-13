package com.dpis.module.home.presentation

import android.content.Intent
import com.dpis.module.DpisApplication
import com.dpis.module.MainActivity
import com.dpis.module.applist.AppListPage
import com.dpis.module.applist.InstalledAppCatalogCoordinator
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.fonts.FontLibraryActivity
import com.dpis.module.home.DonateActivity
import com.dpis.module.home.HomeActivationStateResolver
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
 */
class HomeWorkspaceSession(
    private val activity: MainActivity,
) {
    fun createState(): HomeWorkspaceState {
        val configStore = activity.hookConfigStore
        val visibleConfiguredAppCount =
            InstalledAppCatalogCoordinator.countUserVisibleConfiguredPackages(
                configStore,
                activity.installedAppsLoadSession.loadScopeState(),
            )
        return HomeWorkspaceState(
            isActivatedForHome(),
            visibleConfiguredAppCount,
            ConfigStoreFactory.createLocalUiFontLibraryStore(
                activity,
                DpisApplication.xposedService,
            ).listFonts().size,
            activity.startupSession.ensureWorkspaceSession().quickItemCount(),
            RootAccessProbe.cachedResult(),
            activity.updateSession.homeUpdateUiState,
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
        return object : HomeWorkspaceActions {
            override fun checkForUpdates() {
                activity.updateSession.checkForUpdatesNow()
            }

            override fun openConfiguredAppsWorkspace() {
                activity.startupSession.setCurrentAppListPage(AppListPage.CONFIGURED_APPS, false)
                activity.startupSession.dispatch(
                    MainUiAction.workspaceModeChanged(MainUiState.WorkspaceMode.APP),
                )
            }

            override fun openFontLibrary() {
                activity.startActivity(Intent(activity, FontLibraryActivity::class.java))
            }

            override fun openTemplateWorkspace() {
                activity.startupSession.dispatch(
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
                activity.mainWorkspaceSession.bindHomeWorkspace()
            }
        }
    }
}
