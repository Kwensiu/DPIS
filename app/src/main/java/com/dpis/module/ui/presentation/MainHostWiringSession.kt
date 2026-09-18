package com.dpis.module.ui.presentation

import com.dpis.module.MainActivity
import com.dpis.module.R
import com.dpis.module.appconfig.editor.AppConfigEditorPersister
import com.dpis.module.appconfig.editor.ComposeAppEditorController
import com.dpis.module.appconfig.editor.ComposeAppEditorSaveWorkflow
import com.dpis.module.appconfig.editor.ComposeEditorScopeRequestCoordinator
import com.dpis.module.appconfig.presentation.ComposeAppEditorActivityGateway
import com.dpis.module.appconfig.presentation.MainWorkspaceEditorPostSaveEffects
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppListPage
import com.dpis.module.applist.AppListScopeTarget
import com.dpis.module.applist.AppListSelectionController
import com.dpis.module.applist.AppWorkspacePresentation
import com.dpis.module.applist.RestoreScopePromptPolicy
import com.dpis.module.applist.RestoreScopePromptStore
import com.dpis.module.applist.presentation.AppListBatchActionCoordinator
import com.dpis.module.settings.presentation.SettingsWorkspaceSession
import com.dpis.module.templates.BatchScopeRequestCoordinator
import com.dpis.module.tools.presentation.ToolsWorkspace
import com.dpis.module.ui.MainUiAction
import com.dpis.module.ui.MainViewModel

/**
 * Owns onCreate host construction for the Compose editor, catalogue, tools,
 * and settings workspaces.
 */
class MainHostWiringSession(
    private val activity: MainActivity,
    private val secondaryNavigation: SecondaryNavigation,
) {
    var composeAppEditorController: ComposeAppEditorController? = null
        private set
    var composeAppEditorSaveWorkflow: ComposeAppEditorSaveWorkflow? = null
        private set
    var toolsWorkspace: ToolsWorkspace? = null
        private set
    var appWorkspaceActions: AppWorkspacePresentation.Actions? = null
        private set
    val appListSelectionController = AppListSelectionController()
    var settingsWorkspaceSession: SettingsWorkspaceSession? = null
        private set
    val restoreScopePromptStore by lazy { RestoreScopePromptStore(activity) }

    fun wire(viewModel: MainViewModel) {
        val scopeCoordinator = ComposeEditorScopeRequestCoordinator(
            viewModel,
            ComposeEditorScopeRequestCoordinator.ScopeRequester { item, onApproved ->
                activity.systemScopeCoordinator.requestScope(
                    item.packageName,
                    item.label,
                    onApproved,
                    null,
                    false,
                )
            },
            { activity.mainWorkspaceSession.refreshApps() },
            { activity.showToast(R.string.save_scope_request_notice) },
        )
        val gateway = ComposeAppEditorActivityGateway(
            activity,
            activity.wechatHelp,
        )
        val saveWorkflow = ComposeAppEditorSaveWorkflow(
            AppConfigEditorPersister(
                activity.saveHandler,
                gateway::systemHooksEnabled,
                activity::hookConfigStore,
            ),
            MainWorkspaceEditorPostSaveEffects(activity, scopeCoordinator),
        )
        composeAppEditorSaveWorkflow = saveWorkflow
        composeAppEditorController = ComposeAppEditorController(viewModel, gateway, saveWorkflow)

        settingsWorkspaceSession = SettingsWorkspaceSession.create(
            activity,
            { activity.mainWorkspaceSession.refreshSettings() },
            secondaryNavigation,
        )
        toolsWorkspace = ToolsWorkspace(
            activity,
            { activity.mainWorkspaceSession.refreshTools() },
            { activity.showToast(R.string.system_settings_save_failed) },
        )
        val batchActions = AppListBatchActionCoordinator(
            object : AppListBatchActionCoordinator.Host {
                override fun configStore() = activity.hookConfigStore

                override fun showToast(messageResId: Int, vararg formatArgs: Any?) {
                    activity.showToast(messageResId, *formatArgs)
                }

                override fun refreshApps() {
                    activity.startupSession.requestAppsLoad()
                    activity.mainWorkspaceSession.refreshApps()
                }

                override fun publishSelection() {
                    activity.mainWorkspaceSession.refreshApps()
                }

                override fun publishDpisEnabled(
                    packageNames: Collection<String>,
                    enabled: Boolean
                ) {
                    activity.startupSession.publishDpisEnabled(packageNames, enabled)
                }

                override fun runOnUiThread(action: Runnable) {
                    activity.runOnUiThread(action)
                }

                override fun runInBackground(action: Runnable) {
                    // Preference commits and property cleanup can grow with the selected set.
                    Thread(action, "dpis-app-list-reset").start()
                }
            },
            appListSelectionController,
        )
        appWorkspaceActions = object : AppWorkspacePresentation.Actions,
            AppWorkspacePresentation.SelectionActions {
            override fun changeQuery(query: String) {
                activity.startupSession.dispatch(MainUiAction.queryChanged(query))
            }

            override fun changePage(page: AppListPage) {
                if (appListSelectionController.isBatchOperationRunning()) return
                appListSelectionController.exit()
                activity.startupSession.setCurrentAppListPage(page, true)
                activity.mainWorkspaceSession.refreshApps()
            }

            override fun changeFilters(filterState: AppListFilterState) {
                activity.startupSession.filterStore?.save(filterState)
                activity.startupSession.dispatch(MainUiAction.filterChanged(filterState))
            }

            override fun refresh(page: AppListPage) {
                activity.startupSession.onPageRefreshRequested(page)
            }

            override fun openApp(item: AppListItem) {
                composeAppEditorController?.open(item)
            }

            override fun beginSelection(page: AppListPage, item: AppListItem) {
                appListSelectionController.begin(page, item.packageName)
                activity.mainWorkspaceSession.refreshApps()
            }

            override fun toggleSelection(page: AppListPage, item: AppListItem) {
                appListSelectionController.toggle(page, item.packageName)
                activity.mainWorkspaceSession.refreshApps()
            }

            override fun exitSelection() {
                appListSelectionController.exit()
                activity.mainWorkspaceSession.refreshApps()
            }

            override fun selectAll(page: AppListPage, items: List<AppListItem>) {
                appListSelectionController.selectAll(page, items.map { it.packageName })
                activity.mainWorkspaceSession.refreshApps()
            }

            override fun invertSelection(page: AppListPage, items: List<AppListItem>) {
                appListSelectionController.invert(page, items.map { it.packageName })
                activity.mainWorkspaceSession.refreshApps()
            }

            override fun changeSelectedScope(target: AppListScopeTarget, items: List<AppListItem>) {
                batchActions.changeScope(target, items)
            }

            override fun setSelectedConfigsEnabled(enabled: Boolean, items: List<AppListItem>) {
                batchActions.setConfigsEnabled(enabled, items)
            }

            override fun resetSelectedConfigs(items: List<AppListItem>) {
                batchActions.reset(items)
            }

            override fun updateScrollPosition(
                page: AppListPage,
                index: Int,
                scrollOffset: Int,
            ) {
                activity.scrollStateStore.update(page, index, scrollOffset)
            }

            override fun dismissRestoreScopePrompt() {
                restoreScopePromptStore.clear()
                activity.mainWorkspaceSession.refreshApps()
            }

            override fun requestRestoreScope() {
                val snapshot = activity.startupSession.requireUiState().appsSnapshot()
                val packages = RestoreScopePromptPolicy.candidatePackages(
                    snapshot,
                    restoreScopePromptStore.scopePackages(),
                )
                if (packages.isEmpty()) {
                    restoreScopePromptStore.clear()
                    activity.mainWorkspaceSession.refreshApps()
                    return
                }
                val result = BatchScopeRequestCoordinator(
                    object : BatchScopeRequestCoordinator.Host {
                        override fun showToast(messageResId: Int, vararg formatArgs: Any?) {
                            activity.showToast(messageResId, *formatArgs)
                        }

                        override fun requestAppsLoad() {
                            activity.startupSession.onPageRefreshRequested(
                                AppListPage.CONFIGURED_APPS,
                            )
                        }

                        override fun runOnUiThread(runnable: Runnable) {
                            activity.runOnUiThread(runnable)
                        }
                    },
                ).requestMissingScope(
                    packages,
                    BatchScopeRequestCoordinator.RESTORE_BACKUP_SCOPE_SOURCE,
                )
                if (RestoreScopePromptPolicy.shouldClearAfterRequest(
                        result.requestStarted,
                        result.manualRequired,
                        result.affectedPackageCount,
                    )
                ) {
                    restoreScopePromptStore.clear()
                }
                activity.mainWorkspaceSession.refreshApps()
            }
        }
    }
}
