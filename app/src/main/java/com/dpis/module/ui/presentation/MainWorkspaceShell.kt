package com.dpis.module.ui.presentation

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import com.dpis.module.MainActivity
import com.dpis.module.appconfig.editor.ComposeAppEditorController
import com.dpis.module.appconfig.landdetail.LandAppDetailSession
import com.dpis.module.appconfig.presentation.AppConfigSheetSession
import com.dpis.module.applist.AppListPage
import com.dpis.module.applist.AppWorkspace
import com.dpis.module.applist.AppWorkspaceScrollStateStore
import com.dpis.module.home.HomeWorkspaceState
import com.dpis.module.settings.presentation.SettingsWorkspaceSession
import com.dpis.module.settings.presentation.ToolsWorkspace
import com.dpis.module.templates.presentation.TemplateWorkspaceActivitySession
import com.dpis.module.ui.MainUiAction
import com.dpis.module.ui.MainUiState
import com.dpis.module.ui.MainViewModel

/** Wires workspace presentation to MainActivity platform capabilities. */
class MainWorkspaceShell(
    private val activity: MainActivity,
) : MainWorkspaceSession.Shell {
    override fun activity(): Activity = activity

    override fun requireUiState(): MainUiState = activity.requireUiState()

    override fun dispatch(action: MainUiAction) = activity.dispatchMainUiAction(action)

    override fun editorViewModel(): MainViewModel? = activity.startupSession.viewModel

    override fun composeAppEditorController(): ComposeAppEditorController? =
        activity.hostWiringSession.composeAppEditorController

    override fun appWorkspace(): AppWorkspace? = activity.hostWiringSession.appWorkspace

    override fun toolsWorkspace(): ToolsWorkspace? = activity.hostWiringSession.toolsWorkspace

    override fun settingsWorkspaceSession(): SettingsWorkspaceSession? =
        activity.hostWiringSession.settingsWorkspaceSession

    override fun landCurrentPage(): AppListPage = activity.currentAppListPage

    override fun appWorkspaceScrollStateStore(): AppWorkspaceScrollStateStore =
        activity.scrollStateStore

    override fun systemHooksEnabled(): Boolean = activity.isSystemHookEnabledFromStore

    override fun homeState(): HomeWorkspaceState = activity.homeWorkspaceSession.createState()

    override fun ensureTemplateWorkspace(): TemplateWorkspaceActivitySession =
        activity.ensureWorkspaceSession()

    override fun landAppDetailSession(): LandAppDetailSession = activity.landDetailSession

    override fun appConfigSheetSession(): AppConfigSheetSession = activity.sheetSession

    override fun topContainer(): View? = activity.hostWiringSession.topContainer

    override fun toolsWorkspaceContainer(): View? =
        activity.hostWiringSession.toolsWorkspaceContainer

    override fun settingsWorkspaceContainer(): View? =
        activity.hostWiringSession.settingsWorkspaceContainer

    override fun landDetailPane(): View? = activity.hostWiringSession.landDetailPane

    override fun landDetailDivider(): View? = activity.hostWiringSession.landDetailDivider

    override fun landDetailEmptyView(): View? = activity.hostWiringSession.landDetailEmptyView

    override fun landDetailContent(): FrameLayout? = activity.hostWiringSession.landDetailContent
}
