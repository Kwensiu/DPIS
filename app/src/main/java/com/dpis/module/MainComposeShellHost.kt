package com.dpis.module

import android.view.View
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.ViewCompat
import com.dpis.module.ui.compose.DpisLegacyWorkspaceHost
import com.dpis.module.ui.compose.DpisTheme
import kotlin.math.roundToInt

/**
 * Installs the Theme 1 Compose shell around the legacy workspace root.
 *
 * The root View remains a temporary content implementation until each
 * workspace moves to Compose. This host is the only place where the two UI
 * toolkits meet, keeping MainActivity an assembly surface rather than a new
 * navigation state machine.
 */
internal class MainComposeShellHost(
    composeView: ComposeView,
    private val legacyWorkspaceRoot: View,
    initialState: MainUiState,
    private val isCompactUi: Boolean,
    private val onContentBottomPaddingChanged: (Int) -> Unit,
    private val dispatch: (MainUiAction) -> Unit
) {
    private var state by mutableStateOf(initialState)
    private var lastInsetsReplayWorkspace: MainUiState.WorkspaceMode? = null

    init {
        composeView.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                MainComposeWorkspaceShell(
                    state = state,
                    isCompactUi = isCompactUi,
                    dispatch = dispatch
                ) { padding ->
                    val contentBottomPadding = (
                        padding.calculateBottomPadding().value * LocalDensity.current.density
                    ).roundToInt()
                    SideEffect {
                        onContentBottomPaddingChanged(contentBottomPadding)
                    }
                    DpisLegacyWorkspaceHost(
                        createView = { legacyWorkspaceRoot },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }

    fun render(nextState: MainUiState) {
        state = nextState
    }

    /**
     * Replays raw window insets after a lazily bound legacy workspace becomes visible.
     * Its View listeners are the system-bar owners until that workspace is migrated.
     */
    fun replayLegacyWorkspaceInsets(workspaceMode: MainUiState.WorkspaceMode) {
        if (lastInsetsReplayWorkspace == workspaceMode) {
            return
        }
        lastInsetsReplayWorkspace = workspaceMode
        legacyWorkspaceRoot.post {
            val rootInsets = ViewCompat.getRootWindowInsets(legacyWorkspaceRoot)
            if (rootInsets != null) {
                ViewCompat.dispatchApplyWindowInsets(legacyWorkspaceRoot, rootInsets)
            } else {
                ViewCompat.requestApplyInsets(legacyWorkspaceRoot)
            }
        }
    }
}
