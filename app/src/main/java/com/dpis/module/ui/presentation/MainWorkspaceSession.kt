package com.dpis.module.ui.presentation

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.dpis.module.appconfig.EditorPresentation
import com.dpis.module.appconfig.editor.ComposeAppEditorController
import com.dpis.module.appconfig.landdetail.LandAppDetailSession
import com.dpis.module.appconfig.presentation.AppConfigSheetSession
import com.dpis.module.applist.AppListPage
import com.dpis.module.applist.AppWorkspace
import com.dpis.module.applist.AppWorkspacePresentation
import com.dpis.module.applist.AppWorkspaceScrollStateStore
import com.dpis.module.home.HomeWorkspaceState
import com.dpis.module.settings.presentation.SettingsWorkspaceSession
import com.dpis.module.settings.presentation.ToolsWorkspace
import com.dpis.module.templates.presentation.TemplateWorkspaceActivitySession
import com.dpis.module.ui.MainUiAction
import com.dpis.module.ui.MainUiState
import com.dpis.module.ui.MainViewModel
import com.dpis.module.ui.WatchUiMode

/**
 * Owns Compose workspace-shell install, workspace-mode visibility, and
 * editor restore for the current workspace. [com.dpis.module.MainActivity]
 * keeps startup wiring and remaining domain hosts.
 */
class MainWorkspaceSession(
    private val shell: Shell,
) {
    interface Shell {
        fun activity(): Activity

        fun requireUiState(): MainUiState

        fun dispatch(action: MainUiAction)

        fun editorViewModel(): MainViewModel?

        fun composeAppEditorController(): ComposeAppEditorController?

        fun appWorkspace(): AppWorkspace?

        fun toolsWorkspace(): ToolsWorkspace?

        fun settingsWorkspaceSession(): SettingsWorkspaceSession?

        fun landCurrentPage(): AppListPage

        fun appWorkspaceScrollStateStore(): AppWorkspaceScrollStateStore

        fun systemHooksEnabled(): Boolean

        fun homeState(): HomeWorkspaceState

        fun ensureTemplateWorkspace(): TemplateWorkspaceActivitySession

        fun landAppDetailSession(): LandAppDetailSession

        fun appConfigSheetSession(): AppConfigSheetSession

        fun topContainer(): View?

        fun toolsWorkspaceContainer(): View?

        fun settingsWorkspaceContainer(): View?

        fun landDetailPane(): View?

        fun landDetailDivider(): View?

        fun landDetailEmptyView(): View?

        fun landDetailContent(): FrameLayout?
    }

    private var composeShellHost: MainComposeShellHost? = null
    private var renderedWorkspaceMode: MainUiState.WorkspaceMode? = null

    fun composeShell(): MainComposeShellHost? = composeShellHost

    fun installComposeWorkspaceShell() {
        val activity = shell.activity()
        val activityContent = activity.findViewById<ViewGroup>(android.R.id.content)
        if (activityContent == null || activityContent.childCount == 0) {
            return
        }
        val legacyWorkspaceRoot = activityContent.getChildAt(0) ?: return
        activityContent.removeView(legacyWorkspaceRoot)
        val composeRoot = ComposeView(activity)
        activityContent.addView(
            composeRoot,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        val workspacePresentationCoordinator = MainWorkspacePresentationCoordinator(
            object : MainWorkspacePresentationCoordinator.Content {
                override fun homeState(): HomeWorkspaceState = shell.homeState()

                override fun appState(): AppWorkspacePresentation.State =
                    AppWorkspacePresentation.create(
                        shell.requireUiState(),
                        shell.landCurrentPage(),
                        shell.systemHooksEnabled(),
                        shell.appWorkspaceScrollStateStore(),
                        shell.appWorkspace()!!.actions(),
                    )

                override fun appEditorState(): EditorPresentation.State? =
                    shell.composeAppEditorController()?.createState()

                override fun toolsState() = shell.toolsWorkspace()?.state()

                override fun changeToolsPending(percent: Int) {
                    shell.toolsWorkspace()?.changePending(percent)
                }

                override fun applyTools() {
                    shell.toolsWorkspace()?.apply()
                }

                override fun restoreTools() {
                    shell.toolsWorkspace()?.restore()
                }

                override fun requestToolsPermission() {
                    shell.toolsWorkspace()?.requestPermission()
                }

                override fun settings() = checkNotNull(shell.settingsWorkspaceSession())

                override fun templateWorkspace() =
                    shell.ensureTemplateWorkspace().presentationSource { query ->
                        shell.dispatch(MainUiAction.queryChanged(query))
                    }
            },
        )
        composeShellHost = MainComposeShellHost(
            composeRoot,
            shell.requireUiState(),
            WatchUiMode.shouldUseCompactUi(activity),
            workspacePresentationCoordinator,
        ) { action ->
            shell.dispatch(action)
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
        val appWorkspace = mode == MainUiState.WorkspaceMode.APP
        val templateWorkspace = mode == MainUiState.WorkspaceMode.TEMPLATE
        val toolsWorkspace = mode == MainUiState.WorkspaceMode.TOOLS
        val settingsWorkspace = mode == MainUiState.WorkspaceMode.SETTINGS
        setVisible(shell.topContainer(), appWorkspace || templateWorkspace)
        val animateWorkspace = renderedWorkspaceMode != null && renderedWorkspaceMode != mode
        renderedWorkspaceMode = mode
        setVisible(shell.toolsWorkspaceContainer(), toolsWorkspace)
        setVisible(shell.settingsWorkspaceContainer(), settingsWorkspace)
        resetHiddenWorkspacePresentation(mode)
        if (animateWorkspace) {
            animateVisibleWorkspaceContent(mode)
        }
        applyLandscapeDetailVisibility(appWorkspace, templateWorkspace)
        if (templateWorkspace) {
            bindWorkspaceSession()
            restoreWorkspaceEditorForCurrentConfiguration()
        } else if (toolsWorkspace) {
            bindToolsWorkspace(enteringToolsWorkspace)
        } else if (settingsWorkspace) {
            bindSettingsWorkspace()
        }
    }

    fun restoreAppEditorForCurrentWorkspace() {
        val viewModel = shell.editorViewModel()
        if (viewModel == null
            || shell.requireUiState().workspaceMode != MainUiState.WorkspaceMode.APP
        ) {
            return
        }
        // The Compose app workspace restores its editor directly from MainViewModel. Re-entering
        // the legacy route here would stack a View BottomSheetDialog over the Compose sheet after
        // any state render, including the catalog refresh triggered by a successful save.
        val composeShellHost = composeShellHost
        if (composeShellHost != null) {
            composeShellHost.refreshApps()
            return
        }
        val editingPackage = viewModel.editingPackageName
        if (editingPackage.isNullOrBlank()) {
            return
        }
        val landDetailContent = shell.landDetailContent()
        if (isLandscapeDetailMode() && landDetailContent != null && landDetailContent.childCount > 0) {
            return
        }
        for (appItem in shell.requireUiState().visibleItems(shell.landCurrentPage())) {
            if (editingPackage == appItem.packageName) {
                if (isLandscapeDetailMode()) {
                    shell.landAppDetailSession().show(appItem)
                } else {
                    shell.appConfigSheetSession().show(appItem)
                }
                break
            }
        }
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
        shell.ensureTemplateWorkspace().present(
            shell.requireUiState().currentQuery(),
            composeShellHost != null,
        )
    }

    @JvmOverloads
    fun bindToolsWorkspace(resetExpandedState: Boolean = false) {
        val toolsWorkspace = shell.toolsWorkspace()
        val composeShellHost = composeShellHost
        if (composeShellHost != null && toolsWorkspace != null) {
            toolsWorkspace.onResume()
            composeShellHost.refreshTools(resetExpandedState)
            return
        }
        if (toolsWorkspace != null) {
            toolsWorkspace.bind(shell.toolsWorkspaceContainer())
            if (resetExpandedState) {
                toolsWorkspace.onShown()
            }
        }
    }

    fun bindSettingsWorkspace() {
        val settingsWorkspaceSession = shell.settingsWorkspaceSession()
        if (composeShellHost != null) {
            settingsWorkspaceSession?.ensureComposeController()
            return
        }
        val settingsWorkspaceContainer = shell.settingsWorkspaceContainer()
        if (settingsWorkspaceContainer == null || settingsWorkspaceSession == null) {
            return
        }
        settingsWorkspaceSession.bindLegacy(settingsWorkspaceContainer)
    }

    fun restoreWorkspaceEditorForCurrentConfiguration() {
        if (shell.requireUiState().workspaceMode == MainUiState.WorkspaceMode.TEMPLATE) {
            shell.ensureTemplateWorkspace().restoreForConfiguration(
                shell.requireUiState().currentQuery(),
                composeShellHost != null,
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

    fun isLandscapeDetailMode(): Boolean =
        shell.landDetailContent() != null && shell.landDetailEmptyView() != null

    private fun applyLandscapeDetailVisibility(
        appWorkspace: Boolean,
        templateWorkspace: Boolean,
    ) {
        val landDetailContent = shell.landDetailContent()
        val showDetailPane = isLandscapeDetailMode() && (appWorkspace || templateWorkspace)
        setVisible(shell.landDetailPane(), showDetailPane)
        setVisible(shell.landDetailDivider(), showDetailPane)
        setVisible(
            shell.landDetailEmptyView(),
            appWorkspace && landDetailContent != null && landDetailContent.childCount == 0,
        )
        setVisible(
            landDetailContent,
            appWorkspace && landDetailContent != null && landDetailContent.childCount > 0,
        )
        shell.ensureTemplateWorkspace().updateLegacyDetailVisibility(templateWorkspace)
    }

    private fun resetHiddenWorkspacePresentation(visibleMode: MainUiState.WorkspaceMode) {
        resetWorkspacePresentationUnlessMode(
            shell.toolsWorkspaceContainer(),
            visibleMode,
            MainUiState.WorkspaceMode.TOOLS,
        )
        resetWorkspacePresentationUnlessMode(
            shell.settingsWorkspaceContainer(),
            visibleMode,
            MainUiState.WorkspaceMode.SETTINGS,
        )
    }

    private fun animateVisibleWorkspaceContent(mode: MainUiState.WorkspaceMode) {
        val target = workspaceViewForMode(mode) ?: return
        target.animate().cancel()
        target.alpha = 0f
        target.scaleX = WORKSPACE_CONTENT_ENTER_START_SCALE
        target.scaleY = WORKSPACE_CONTENT_ENTER_START_SCALE
        target.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(WORKSPACE_TRANSITION_DURATION_MS)
            .setInterpolator(WORKSPACE_CONTENT_ENTER_INTERPOLATOR)
            .withEndAction {
                target.alpha = 1f
                target.scaleX = 1f
                target.scaleY = 1f
            }
            .start()
    }

    private fun workspaceViewForMode(mode: MainUiState.WorkspaceMode): View? = when (mode) {
        MainUiState.WorkspaceMode.TOOLS -> shell.toolsWorkspaceContainer()
        MainUiState.WorkspaceMode.SETTINGS -> shell.settingsWorkspaceContainer()
        else -> null
    }

    companion object {
        private const val WORKSPACE_TRANSITION_DURATION_MS = 300L
        private const val WORKSPACE_CONTENT_ENTER_START_SCALE = 0.96f
        private val WORKSPACE_CONTENT_ENTER_INTERPOLATOR =
            AccelerateDecelerateInterpolator()

        private fun setVisible(view: View?, visible: Boolean) {
            if (view != null) {
                view.visibility = if (visible) View.VISIBLE else View.GONE
            }
        }

        private fun resetWorkspacePresentationUnlessMode(
            view: View?,
            visibleMode: MainUiState.WorkspaceMode,
            viewMode: MainUiState.WorkspaceMode,
        ) {
            if (visibleMode != viewMode) {
                resetWorkspacePresentation(view)
            }
        }

        private fun resetWorkspacePresentation(view: View?) {
            if (view == null) {
                return
            }
            view.animate().cancel()
            view.alpha = 1f
            view.scaleX = 1f
            view.scaleY = 1f
        }
    }
}
