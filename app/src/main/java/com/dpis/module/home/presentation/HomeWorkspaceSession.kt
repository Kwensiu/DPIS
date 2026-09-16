package com.dpis.module.home.presentation

import com.dpis.module.DpisApplication
import com.dpis.module.MainActivity
import com.dpis.module.applist.AppListPage
import com.dpis.module.applist.InstalledAppCatalogCoordinator
import com.dpis.module.applist.ScopeState
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.home.HomeActivationStateResolver
import com.dpis.module.home.HomeUpdateUiState
import com.dpis.module.home.HomeWorkspaceActions
import com.dpis.module.home.HomeWorkspaceLayout
import com.dpis.module.home.HomeWorkspaceLayoutStore
import com.dpis.module.home.HomeWorkspaceState
import com.dpis.module.root.RootAccessProbe
import com.dpis.module.runtime.ConfigStoreFactory
import com.dpis.module.settings.PageSettingsStore
import com.dpis.module.settings.presentation.isHomeActivationDetectionEnabled
import com.dpis.module.settings.presentation.isHomeEditButtonVisible
import com.dpis.module.ui.MainUiAction
import com.dpis.module.ui.MainUiState
import com.dpis.module.ui.SecondaryDestination
import com.dpis.module.ui.presentation.SecondaryNavigation

/**
 * Owns Home workspace snapshots and navigation actions.
 */
class HomeWorkspaceSession(
    private val activity: MainActivity,
    private val loadScopeState: () -> ScopeState,
    private val quickItemCount: () -> Int,
    private val homeUpdateUiState: () -> HomeUpdateUiState,
    private val checkForUpdatesNow: () -> Unit,
    private val setCurrentAppListPage: (AppListPage, Boolean) -> Unit,
    private val dispatch: (MainUiAction) -> Unit,
    private val bindHomeWorkspace: () -> Unit,
    private val secondaryNavigation: SecondaryNavigation,
) {
    fun createState(): HomeWorkspaceState {
        val configStore = activity.hookConfigStore
        val visibleConfiguredAppCount =
            InstalledAppCatalogCoordinator.countUserVisibleConfiguredPackages(
                configStore,
                loadScopeState(),
            )
        return HomeWorkspaceState(
            isActivatedForHome(),
            visibleConfiguredAppCount,
            ConfigStoreFactory.createLocalUiFontLibraryStore(
                activity,
                DpisApplication.xposedService,
            ).listFonts().size,
            quickItemCount(),
            RootAccessProbe.cachedResult(),
            homeUpdateUiState(),
            HomeWorkspaceLayoutStore(activity).load(),
            createActions(),
            PageSettingsStore.isHomeEditButtonVisible(activity),
        )
    }

    private fun isActivatedForHome(): Boolean {
        val detectionEnabled = PageSettingsStore.isHomeActivationDetectionEnabled(activity)
        val libXposedService = HomeActivationStateResolver.hasModernLibXposedService(
            DpisApplication.xposedService?.let { service -> { service.apiVersion } },
        )
        val selfLoaded = DpisApplication.isXposedSelfLoaded()
        val activated = HomeActivationStateResolver.isActivatedForHome(
            detectionEnabled,
            libXposedService,
            selfLoaded,
        )
        DpisLog.i(
            "home activation resolved: detectionEnabled=" + detectionEnabled
                + ", libxposedService=" + libXposedService
                + ", selfLoaded=" + selfLoaded
                + ", activated=" + activated,
        )
        return activated
    }

    private fun createActions(): HomeWorkspaceActions {
        return object : HomeWorkspaceActions {
            override fun checkForUpdates() {
                checkForUpdatesNow()
            }

            override fun openConfiguredAppsWorkspace() {
                setCurrentAppListPage(AppListPage.CONFIGURED_APPS, false)
                dispatch(
                    MainUiAction.workspaceModeChanged(MainUiState.WorkspaceMode.APP),
                )
            }

            override fun openFontLibrary() {
                secondaryNavigation.open(SecondaryDestination.FontLibrary)
            }

            override fun openTemplateWorkspace() {
                dispatch(
                    MainUiAction.workspaceModeChanged(MainUiState.WorkspaceMode.TEMPLATE),
                )
            }

            override fun openModeHelp() {
                secondaryNavigation.open(SecondaryDestination.ModeHelp)
            }

            override fun openDonate() {
                secondaryNavigation.open(SecondaryDestination.Donate)
            }

            override fun saveHomeWorkspaceLayout(layout: HomeWorkspaceLayout) {
                HomeWorkspaceLayoutStore(activity).save(layout)
                bindHomeWorkspace()
            }
        }
    }
}
