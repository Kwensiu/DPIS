package com.dpis.module

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.dpis.module.ui.compose.DpisTheme
import com.dpis.module.ui.compose.dpisDarkTheme

/** Installs the Compose shell; domain state and actions remain Activity-owned. */
internal class MainComposeShellHost(
    composeView: ComposeView,
    initialState: MainUiState,
    private val isCompactUi: Boolean,
    private val workspacePresentation: MainWorkspacePresentationCoordinator,
    private val dispatch: (MainUiAction) -> Unit
) {
    private var state by mutableStateOf(initialState)

    init {
        composeView.setContent {
            DpisTheme(darkTheme = dpisDarkTheme()) {
                Box(Modifier.fillMaxSize()) {
                    MainComposeWorkspaceShell(
                        state = state,
                        isCompactUi = isCompactUi,
                        showCompactNavigation = !isCompactUi ||
                            !workspacePresentation.hasWearDetail(state.workspaceMode),
                        dispatch = dispatch
                    ) { padding ->
                        if (isCompactUi) {
                            workspacePresentation.renderWear(state.workspaceMode, padding)
                            // Wear screens own their ScreenScaffold padding and round-screen shape.
                        } else {
                            workspacePresentation.render(state.workspaceMode, padding)
                            // Home domain state lives in MainActivity. This revision only invalidates
                            // the Compose presentation after its existing coordinator updates it.
                        }
                    }
                    workspacePresentation.RenderAppEditorOverlay(state.workspaceMode, isCompactUi)
                }
            }
        }
    }

    fun render(nextState: MainUiState) {
        state = nextState
    }

    fun refreshApps() = workspacePresentation.refreshApps()

    fun refreshHome() = workspacePresentation.refreshHome()

    @JvmOverloads
    fun refreshTools(collapse: Boolean = false) {
        workspacePresentation.refreshTools(collapse)
    }

    fun refreshSettings() = workspacePresentation.refreshSettings()

    fun refreshTemplates() = workspacePresentation.refreshTemplates()
}
