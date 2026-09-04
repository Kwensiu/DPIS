package com.dpis.module

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.dpis.module.ui.compose.ComposeDesignSystem
import com.dpis.module.ui.compose.FeedbackDiagnosticPreparationContent
import com.dpis.module.ui.compose.FeedbackDiagnosticPreparationPresentation
import com.dpis.module.ui.dialog.StartupDisclaimerDialog
import com.dpis.module.ui.dialog.StartupDisclaimerGate
import com.dpis.module.ui.compose.resolveDarkTheme
import com.dpis.module.ui.compose.imeWindowPan
import com.dpis.module.ui.compose.rememberTextInputFocusBoundary
import com.dpis.module.ui.compose.LocalTextInputFocusBoundary
import java.util.function.BooleanSupplier

/** Installs the Compose shell; domain state and actions remain Activity-owned. */
internal class MainComposeShellHost(
    composeView: ComposeView,
    initialState: MainUiState,
    private val isCompactUi: Boolean,
    private val workspacePresentation: MainWorkspacePresentationCoordinator,
    private val dispatch: (MainUiAction) -> Unit
) {
    private var state by mutableStateOf(initialState)
    private var diagnosticPreparation by mutableStateOf<FeedbackDiagnosticPreparationPresentation?>(null)
    private var startupDisclaimer by mutableStateOf<StartupDisclaimerRequest?>(null)

    init {
        composeView.setContent {
            val inputFocusBoundary = rememberTextInputFocusBoundary()
            val disclaimerPresenter = remember {
                StartupDisclaimerGate.Presenter {
                        markAccepted,
                        onSaveFailed,
                        onAccepted,
                        onBack,
                    -> showStartupDisclaimer(markAccepted, onSaveFailed, onAccepted, onBack) }
            }
            DisposableEffect(disclaimerPresenter) {
                StartupDisclaimerGate.bind(disclaimerPresenter)
                onDispose { StartupDisclaimerGate.clear(disclaimerPresenter) }
            }
            ComposeDesignSystem(darkTheme = resolveDarkTheme()) {
                androidx.compose.runtime.DisposableEffect(composeView, inputFocusBoundary) {
                    ViewCompat.setWindowInsetsAnimationCallback(
                        composeView,
                        object : WindowInsetsAnimationCompat.Callback(
                            WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE,
                        ) {
                            override fun onProgress(
                                insets: WindowInsetsCompat,
                                runningAnimations: MutableList<WindowInsetsAnimationCompat>,
                            ): WindowInsetsCompat {
                                inputFocusBoundary.updateAnimatedImeBottom(
                                    insets.getInsets(WindowInsetsCompat.Type.ime()).bottom,
                                )
                                return insets
                            }

                            override fun onStart(
                                animation: WindowInsetsAnimationCompat,
                                bounds: WindowInsetsAnimationCompat.BoundsCompat,
                            ): WindowInsetsAnimationCompat.BoundsCompat {
                                if ((animation.typeMask and WindowInsetsCompat.Type.ime()) != 0) {
                                    inputFocusBoundary.beginImeAnimation(bounds.upperBound.bottom)
                                }
                                return bounds
                            }

                            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                                // Keep the final animation frame as the stable value. Clearing it
                                // here makes Compose switch from the frame-driven path back to the
                                // settled-inset path during the same frame, which causes a visible
                                // snap. The next IME animation replaces this value on its first
                                // progress callback.
                                if ((animation.typeMask and WindowInsetsCompat.Type.ime()) != 0) {
                                    inputFocusBoundary.finishImeAnimation()
                                }
                            }
                        },
                    )
                    onDispose { ViewCompat.setWindowInsetsAnimationCallback(composeView, null) }
                }
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalTextInputFocusBoundary provides inputFocusBoundary,
                ) {
                Box(Modifier.fillMaxSize().imeWindowPan(inputFocusBoundary)) {
                    val preparation = diagnosticPreparation
                    BackHandler(enabled = preparation != null) {
                        preparation?.back()
                    }
                    AnimatedContent(
                        targetState = preparation,
                        transitionSpec = {
                            (fadeIn() + slideInHorizontally { it / 4 }) togetherWith
                                (fadeOut() + slideOutHorizontally { -it / 6 })
                        },
                        label = "diagnostic-page-transition",
                        modifier = Modifier.fillMaxSize(),
                    ) { currentPreparation ->
                        if (currentPreparation != null) {
                            FeedbackDiagnosticPreparationContent(currentPreparation)
                        } else {
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
                            workspacePresentation.RenderTemplateEditorOverlay(state.workspaceMode, isCompactUi)
                        }
                    }
                    startupDisclaimer?.let { request ->
                        StartupDisclaimerDialog(
                            onAccept = { acceptStartupDisclaimer(request) },
                            onBack = request.onBack,
                        )
                    }
                }
                }
            }
        }
    }

    fun render(nextState: MainUiState) {
        state = nextState
    }

    fun showDiagnosticPreparation(presentation: FeedbackDiagnosticPreparationPresentation) {
        diagnosticPreparation = presentation
    }

    fun dismissDiagnosticPreparation() {
        diagnosticPreparation = null
    }

    /**
     * Shows one mandatory first-start dialog at a time. Its view lifetime belongs to the root
     * composition, so activity recreation cannot leave a detached platform dialog behind.
     */
    fun showStartupDisclaimer(
        markAccepted: BooleanSupplier,
        onSaveFailed: () -> Unit,
        onAccepted: () -> Unit,
        onBack: () -> Unit,
    ): Boolean {
        if (startupDisclaimer == null) {
            startupDisclaimer = StartupDisclaimerRequest(markAccepted, onSaveFailed, onAccepted, onBack)
        }
        return true
    }

    private fun acceptStartupDisclaimer(request: StartupDisclaimerRequest) {
        if (!request.markAccepted.asBoolean) {
            request.onSaveFailed()
            return
        }
        // Clear before continuing startup work, which may synchronously request another dialog.
        startupDisclaimer = null
        request.onAccepted()
    }

    fun refreshApps() = workspacePresentation.refreshApps()

    fun refreshHome() = workspacePresentation.refreshHome()

    @JvmOverloads
    fun refreshTools(collapse: Boolean = false) {
        workspacePresentation.refreshTools(collapse)
    }

    fun refreshSettings() = workspacePresentation.refreshSettings()

    fun refreshTemplates() = workspacePresentation.refreshTemplates()

    private data class StartupDisclaimerRequest(
        val markAccepted: BooleanSupplier,
        val onSaveFailed: () -> Unit,
        val onAccepted: () -> Unit,
        val onBack: () -> Unit,
    )
}
