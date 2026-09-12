package com.dpis.module.ui.presentation

import android.view.View
import android.widget.FrameLayout
import com.dpis.module.MainActivity
import com.dpis.module.R
import com.dpis.module.appconfig.editor.ComposeAppEditorController
import com.dpis.module.appconfig.editor.ComposeAppEditorSaveWorkflow
import com.dpis.module.appconfig.editor.ComposeEditorScopeRequestCoordinator
import com.dpis.module.appconfig.presentation.AppConfigDialogActivityHost
import com.dpis.module.appconfig.presentation.ComposeAppEditorActivityGateway
import com.dpis.module.appconfig.presentation.ComposeAppEditorShell
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppListPage
import com.dpis.module.applist.AppWorkspace
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.quirks.presentation.WechatDpiHelp
import com.dpis.module.settings.presentation.SettingsWorkspaceSession
import com.dpis.module.settings.presentation.ToolsWorkspace
import com.dpis.module.ui.MainUiAction
import com.dpis.module.ui.MainViewModel
import com.dpis.module.ui.WatchWorkspaceChromeBinder

/**
 * Owns onCreate host construction for the Compose editor, catalogue, tools,
 * and settings workspaces. [com.dpis.module.MainActivity] assigns the
 * resulting hosts and continues lifecycle forwarding.
 */
class MainHostWiringSession(
    private val shell: Shell,
) {
    interface Shell {
        fun activity(): MainActivity

        fun requestEditorScope(item: AppListItem, onApproved: Runnable): Boolean

        fun refreshApps()

        fun refreshSettings()

        fun refreshTools()

        fun showToast(messageResId: Int)

        fun appConfigDialogHost(): AppConfigDialogActivityHost

        fun appConfigSaveHandler(): AppConfigSaveHandler

        fun wechatDpiHelp(): WechatDpiHelp

        fun dispatch(action: MainUiAction)

        fun setCurrentAppListPage(page: AppListPage, submit: Boolean)

        fun saveFilterState(filterState: AppListFilterState)

        fun onPageRefreshRequested(page: AppListPage)

        fun updateScrollPosition(page: AppListPage, index: Int, scrollOffset: Int)

        fun attachTemplateLegacyViews(
            workspaceContainer: View?,
            detailEmpty: View?,
            detailContent: FrameLayout?,
        )

        fun openLogs()
    }

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
    var appWorkspace: AppWorkspace? = null
        private set
    var settingsWorkspaceSession: SettingsWorkspaceSession? = null
        private set

    fun wire(viewModel: MainViewModel) {
        val activity = shell.activity()
        val scopeCoordinator = ComposeEditorScopeRequestCoordinator(
            viewModel,
            ComposeEditorScopeRequestCoordinator.ScopeRequester { item, onApproved ->
                shell.requestEditorScope(item, onApproved)
            },
            { shell.refreshApps() },
            { shell.showToast(R.string.save_scope_request_notice) },
        )
        val gateway = ComposeAppEditorActivityGateway(
            ComposeAppEditorShell(shell.activity()),
            shell.appConfigDialogHost(),
            shell.appConfigSaveHandler(),
            scopeCoordinator,
            shell.wechatDpiHelp(),
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
            { shell.refreshSettings() },
            { shell.openLogs() },
        )
        WatchWorkspaceChromeBinder.applyIfSupported(
            activity,
            settingsWorkspaceContainer,
        )
        landDetailPane = activity.findViewById(R.id.land_detail_pane)
        landDetailDivider = activity.findViewById(R.id.land_detail_divider)
        landDetailEmptyView = activity.findViewById(R.id.land_detail_empty)
        landDetailContent = activity.findViewById(R.id.land_detail_content)
        shell.attachTemplateLegacyViews(
            activity.findViewById(R.id.template_workspace_container),
            activity.findViewById(R.id.template_detail_empty),
            activity.findViewById(R.id.template_detail_content),
        )
        toolsWorkspace = ToolsWorkspace(
            activity,
            { shell.refreshTools() },
            { shell.showToast(R.string.system_settings_save_failed) },
        )
        appWorkspace = AppWorkspace(object : AppWorkspace.Host {
            override fun changeQuery(query: String) {
                shell.dispatch(MainUiAction.queryChanged(query))
            }

            override fun changePage(page: AppListPage) {
                shell.setCurrentAppListPage(page, true)
                shell.refreshApps()
            }

            override fun changeFilters(filterState: AppListFilterState) {
                shell.saveFilterState(filterState)
            }

            override fun refresh(page: AppListPage) {
                shell.onPageRefreshRequested(page)
            }

            override fun openApp(item: AppListItem) {
                composeAppEditorController?.open(item)
            }

            override fun updateScrollPosition(
                page: AppListPage,
                index: Int,
                scrollOffset: Int,
            ) {
                shell.updateScrollPosition(page, index, scrollOffset)
            }
        })
    }
}
