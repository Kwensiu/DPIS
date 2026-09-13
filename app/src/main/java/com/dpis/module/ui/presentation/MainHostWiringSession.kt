package com.dpis.module.ui.presentation

import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import com.dpis.module.MainActivity
import com.dpis.module.R
import com.dpis.module.appconfig.editor.ComposeAppEditorController
import com.dpis.module.appconfig.editor.ComposeAppEditorSaveWorkflow
import com.dpis.module.appconfig.editor.ComposeEditorScopeRequestCoordinator
import com.dpis.module.appconfig.presentation.ComposeAppEditorActivityGateway
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppListPage
import com.dpis.module.applist.AppWorkspacePresentation
import com.dpis.module.diagnostics.LogActivity
import com.dpis.module.settings.presentation.SettingsWorkspaceSession
import com.dpis.module.settings.presentation.ToolsWorkspace
import com.dpis.module.ui.MainUiAction
import com.dpis.module.ui.MainViewModel
import com.dpis.module.ui.WatchWorkspaceChromeBinder

/**
 * Owns onCreate host construction for the Compose editor, catalogue, tools,
 * and settings workspaces.
 */
class MainHostWiringSession(
    private val activity: MainActivity,
) {
    var composeAppEditorController: ComposeAppEditorController? = null
        private set
    var composeAppEditorSaveWorkflow: ComposeAppEditorSaveWorkflow? = null
        private set
    var topContainer: View? = null
        private set
    var toolsWorkspaceContainer: View? = null
        private set
    var settingsWorkspaceContainer: View? = null
        private set
    var landDetailPane: View? = null
        private set
    var landDetailDivider: View? = null
        private set
    var landDetailEmptyView: View? = null
        private set
    var landDetailContent: FrameLayout? = null
        private set
    var toolsWorkspace: ToolsWorkspace? = null
        private set
    var appWorkspaceActions: AppWorkspacePresentation.Actions? = null
        private set
    var settingsWorkspaceSession: SettingsWorkspaceSession? = null
        private set

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
            activity.dialogHost,
            activity.saveHandler,
            scopeCoordinator,
            activity.wechatHelp,
        )
        val saveWorkflow = ComposeAppEditorSaveWorkflow(gateway)
        gateway.setSaveWorkflow(saveWorkflow)
        composeAppEditorSaveWorkflow = saveWorkflow
        composeAppEditorController = ComposeAppEditorController(viewModel, gateway)

        topContainer = activity.findViewById(R.id.top_container)
        toolsWorkspaceContainer = activity.findViewById(R.id.tools_workspace_container)
        settingsWorkspaceContainer = activity.findViewById(R.id.settings_workspace_container)
        settingsWorkspaceSession = SettingsWorkspaceSession.create(
            activity,
            { activity.mainWorkspaceSession.refreshSettings() },
            {
                activity.startActivity(Intent(activity, LogActivity::class.java))
            },
        )
        WatchWorkspaceChromeBinder.applyIfSupported(
            activity,
            settingsWorkspaceContainer,
        )
        landDetailPane = activity.findViewById(R.id.land_detail_pane)
        landDetailDivider = activity.findViewById(R.id.land_detail_divider)
        landDetailEmptyView = activity.findViewById(R.id.land_detail_empty)
        landDetailContent = activity.findViewById(R.id.land_detail_content)
        activity.startupSession.ensureWorkspaceSession().attachLegacyViews(
            activity.findViewById(R.id.template_workspace_container),
            activity.findViewById(R.id.template_detail_empty),
            activity.findViewById(R.id.template_detail_content),
        )
        toolsWorkspace = ToolsWorkspace(
            activity,
            { activity.mainWorkspaceSession.refreshTools() },
            { activity.showToast(R.string.system_settings_save_failed) },
        )
        appWorkspaceActions = object : AppWorkspacePresentation.Actions {
            override fun changeQuery(query: String) {
                activity.startupSession.dispatch(MainUiAction.queryChanged(query))
            }

            override fun changePage(page: AppListPage) {
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

            override fun updateScrollPosition(
                page: AppListPage,
                index: Int,
                scrollOffset: Int,
            ) {
                activity.scrollStateStore.update(page, index, scrollOffset)
            }
        }
    }
}
