package com.dpis.module.ui.presentation

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.dpis.module.MainActivity
import com.dpis.module.appconfig.editor.EditorPresentation
import com.dpis.module.applist.AppWorkspacePresentation
import com.dpis.module.home.HomeWorkspaceState
import com.dpis.module.ui.MainUiAction
import com.dpis.module.ui.MainUiState
import com.dpis.module.ui.WatchUiMode

/**
 * Owns Compose workspace-shell install, workspace-mode visibility, and
 * editor restore for the current workspace. View hosts come from
 * [MainHostWiringSession]; [MainStartupSession] owns Activity lifecycle.
 */
class MainWorkspaceSession(
    private val activity: MainActivity,
    private val hostWiring: MainHostWiringSession,
) {
    private var composeShellHost: MainComposeShellHost? = null
    private var renderedWorkspaceMode: MainUiState.WorkspaceMode? = null

    fun composeShell(): MainComposeShellHost? = composeShellHost

    fun applyConfigurationInPlace() {
        composeShellHost?.applyConfigurationInPlace()
    }

    fun installComposeWorkspaceShell() {
        val composeRoot = ComposeView(activity)
        activity.setContentView(
            composeRoot,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        val workspacePresentationCoordinator = MainWorkspacePresentationCoordinator(
            object : MainWorkspacePresentationCoordinator.Content {
                override fun homeState(): HomeWorkspaceState =
                    activity.homeWorkspaceSession.createState()

                override fun appState(): AppWorkspacePresentation.State =
                    AppWorkspacePresentation.create(
                        activity.startupSession.requireUiState(),
                        activity.startupSession.currentAppListPage,
                        activity.startupSession.isSystemHookEnabledFromStore,
                        activity.scrollStateStore,
                        checkNotNull(hostWiring.appWorkspaceActions),
                    )

                override fun appEditorState(): EditorPresentation.State? =
                    hostWiring.composeAppEditorController?.createState()

                override fun toolsState() = hostWiring.toolsWorkspace?.state()

                override fun changeToolsPending(percent: Int) {
                    hostWiring.toolsWorkspace?.changePending(percent)
                }

                override fun applyTools() {
                    hostWiring.toolsWorkspace?.apply()
                }

                override fun restoreTools() {
                    hostWiring.toolsWorkspace?.restore()
                }

                override fun requestToolsPermission() {
                    hostWiring.toolsWorkspace?.requestPermission()
                }

                override fun settings() = checkNotNull(hostWiring.settingsWorkspaceSession)

                override fun templateWorkspace() =
                    activity.startupSession.ensureWorkspaceSession().presentationSource { query ->
                        activity.startupSession.dispatch(MainUiAction.queryChanged(query))
                    }
            },
        )
        composeShellHost = MainComposeShellHost(
            composeRoot,
            activity.startupSession.requireUiState(),
            WatchUiMode.shouldUseCompactUi(activity),
            workspacePresentationCoordinator,
            activity.secondaryNavigation,
            activity,
        ) { action ->
            activity.startupSession.dispatch(action)
        }
    }

    fun render(state: MainUiState?) {
        if (state == null) {
            return
        }
        composeShellHost?.render(state)
        applyWorkspaceMode(state.workspaceMode)
        restoreAppEditorForCurrentWorkspace()
    }

    fun applyWorkspaceMode(workspaceMode: MainUiState.WorkspaceMode?) {
        val mode = workspaceMode ?: MainUiState.WorkspaceMode.HOME
        val enteringToolsWorkspace = mode == MainUiState.WorkspaceMode.TOOLS
            && renderedWorkspaceMode != MainUiState.WorkspaceMode.TOOLS
        renderedWorkspaceMode = mode
        when (mode) {
            MainUiState.WorkspaceMode.TEMPLATE -> {
                bindWorkspaceSession()
                restoreWorkspaceEditorForCurrentConfiguration()
            }
            MainUiState.WorkspaceMode.TOOLS -> bindToolsWorkspace(enteringToolsWorkspace)
            MainUiState.WorkspaceMode.SETTINGS -> bindSettingsWorkspace()
            else -> Unit
        }
    }

    fun restoreAppEditorForCurrentWorkspace() {
        val viewModel = activity.startupSession.viewModel
        if (viewModel == null
            || activity.startupSession.requireUiState().workspaceMode != MainUiState.WorkspaceMode.APP
        ) {
            return
        }
        composeShellHost?.refreshApps()
    }

    fun bindForLifecycle(mode: MainUiState.WorkspaceMode?) {
        when (mode) {
            MainUiState.WorkspaceMode.TEMPLATE -> bindWorkspaceSession()
            MainUiState.WorkspaceMode.HOME -> bindHomeWorkspace()
            MainUiState.WorkspaceMode.TOOLS -> bindToolsWorkspace()
            MainUiState.WorkspaceMode.SETTINGS -> bindSettingsWorkspace()
            else -> Unit
        }
    }

    fun bindHomeWorkspace() {
        composeShellHost?.refreshHome()
    }

    fun bindWorkspaceSession() {
        activity.startupSession.ensureWorkspaceSession().present(
            activity.startupSession.requireUiState().currentQuery(),
        )
    }

    @JvmOverloads
    fun bindToolsWorkspace(resetExpandedState: Boolean = false) {
        val toolsWorkspace = hostWiring.toolsWorkspace ?: return
        toolsWorkspace.onResume()
        composeShellHost?.refreshTools(resetExpandedState)
    }

    fun bindSettingsWorkspace() {
        hostWiring.settingsWorkspaceSession?.ensureComposeController()
    }

    fun restoreWorkspaceEditorForCurrentConfiguration() {
        if (activity.startupSession.requireUiState().workspaceMode == MainUiState.WorkspaceMode.TEMPLATE) {
            activity.startupSession.ensureWorkspaceSession().restoreForConfiguration(
                activity.startupSession.requireUiState().currentQuery(),
            )
        }
    }

    fun refreshApps() {
        composeShellHost?.refreshApps()
    }

    fun refreshSettings() {
        composeShellHost?.refreshSettings()
    }

    fun refreshTools() {
        composeShellHost?.refreshTools()
    }

    fun refreshTemplates() {
        composeShellHost?.refreshTemplates()
    }
}
