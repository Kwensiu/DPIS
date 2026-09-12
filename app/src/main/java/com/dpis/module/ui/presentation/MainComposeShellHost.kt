package com.dpis.module.ui.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.dpis.module.R
import com.dpis.module.ui.compose.ComposeDesignSystem
import com.dpis.module.ui.compose.FeedbackDiagnosticPreparationContent
import com.dpis.module.ui.compose.FeedbackDiagnosticPreparationPresentation
import com.dpis.module.ui.compose.MessageAlertDialog
import com.dpis.module.ui.dialog.ConfirmAlertDialog
import com.dpis.module.ui.dialog.StartupDisclaimerDialog
import com.dpis.module.ui.dialog.StartupDisclaimerGate
import com.dpis.module.ui.compose.resolveDarkTheme
import com.dpis.module.ui.compose.imeWindowPan
import com.dpis.module.ui.compose.rememberTextInputFocusBoundary
import com.dpis.module.ui.compose.LocalTextInputFocusBoundary
import java.util.function.BooleanSupplier
import com.dpis.module.ui.MainUiState
import com.dpis.module.ui.MainUiAction
import com.dpis.module.ui.MainComposeWorkspaceShell

/** Installs the Compose shell; domain state and actions remain Activity-owned. */
class MainComposeShellHost(
    composeView: ComposeView,
    initialState: MainUiState,
    private val isCompactUi: Boolean,
    private val workspacePresentation: MainWorkspacePresentationCoordinator,
    private val dispatch: (MainUiAction) -> Unit
) {
    private var state by mutableStateOf(initialState)
    private var diagnosticPreparation by mutableStateOf<FeedbackDiagnosticPreparationPresentation?>(null)
    private var startupDisclaimer by mutableStateOf<StartupDisclaimerRequest?>(null)
    private var dialog by mutableStateOf<MainShellDialog?>(null)

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
                    MainShellDialogHost(
                        dialog = dialog,
                        onDismiss = { dialog = null },
                    )
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

    fun showFeedbackStartConfirm(message: String, confirmLabel: String, onConfirm: Runnable) {
        dialog = MainShellDialog.FeedbackStart(message, confirmLabel, onConfirm)
    }

    fun showFeedbackExitConfirm(onConfirm: Runnable) {
        dialog = MainShellDialog.FeedbackExit(onConfirm)
    }

    fun showEnableLogsConfirm(onEnabled: Runnable, onCancelled: Runnable?) {
        dialog = MainShellDialog.EnableLogs(onEnabled, onCancelled)
    }

    fun showProcessActionConfirm(actionLabel: String, appLabel: String, onConfirm: Runnable) {
        dialog = MainShellDialog.ProcessAction(actionLabel, appLabel, onConfirm)
    }

    fun showWechatDpiHelp() {
        dialog = MainShellDialog.WechatDpiHelp
    }

    fun showLsposedExplanation(title: String, explanation: String) {
        dialog = MainShellDialog.Lsposed(title, explanation)
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

private sealed class MainShellDialog {
    class FeedbackStart(
        val message: String,
        val confirmLabel: String,
        val onConfirm: Runnable,
    ) : MainShellDialog()

    class FeedbackExit(val onConfirm: Runnable) : MainShellDialog()

    class EnableLogs(
        val onEnabled: Runnable,
        val onCancelled: Runnable?,
    ) : MainShellDialog()

    class ProcessAction(
        val actionLabel: String,
        val appLabel: String,
        val onConfirm: Runnable,
    ) : MainShellDialog()

    data object WechatDpiHelp : MainShellDialog()

    class Lsposed(
        val title: String,
        val explanation: String,
    ) : MainShellDialog()
}

@Composable
private fun MainShellDialogHost(
    dialog: MainShellDialog?,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        is MainShellDialog.FeedbackStart -> ConfirmAlertDialog(
            onDismissRequest = onDismiss,
            title = stringResource(R.string.feedback_diagnostic_action),
            message = dialog.message,
            cancelLabel = stringResource(android.R.string.cancel),
            confirmLabel = dialog.confirmLabel,
            onConfirm = {
                onDismiss()
                dialog.onConfirm.run()
            },
        )
        is MainShellDialog.FeedbackExit -> ConfirmAlertDialog(
            onDismissRequest = onDismiss,
            title = stringResource(R.string.feedback_diagnostic_action),
            message = stringResource(R.string.feedback_diagnostic_exit_confirm_message),
            cancelLabel = stringResource(android.R.string.cancel),
            confirmLabel = stringResource(R.string.feedback_diagnostic_exit_clear_action),
            onConfirm = {
                onDismiss()
                dialog.onConfirm.run()
            },
        )
        is MainShellDialog.EnableLogs -> ConfirmAlertDialog(
            onDismissRequest = {
                val cancelled = dialog.onCancelled
                onDismiss()
                cancelled?.run()
            },
            title = stringResource(R.string.diagnostic_log_required_title),
            message = stringResource(R.string.diagnostic_log_required_message),
            cancelLabel = stringResource(android.R.string.cancel),
            confirmLabel = stringResource(R.string.diagnostic_log_enable_action),
            onConfirm = {
                onDismiss()
                dialog.onEnabled.run()
            },
        )
        is MainShellDialog.ProcessAction -> ConfirmAlertDialog(
            onDismissRequest = onDismiss,
            title = stringResource(R.string.dialog_process_action_confirm_title),
            message = stringResource(
                R.string.dialog_process_action_confirm_message,
                dialog.actionLabel,
                dialog.appLabel,
            ),
            cancelLabel = stringResource(R.string.dialog_process_action_confirm_negative),
            confirmLabel = stringResource(R.string.dialog_process_action_confirm_positive),
            onConfirm = {
                onDismiss()
                dialog.onConfirm.run()
            },
        )
        MainShellDialog.WechatDpiHelp -> MessageAlertDialog(
            onDismissRequest = onDismiss,
            title = stringResource(R.string.dialog_wechat_dpi_help_title),
            message = stringResource(R.string.dialog_wechat_dpi_help_message),
            closeLabel = stringResource(R.string.dialog_close_button),
        )
        is MainShellDialog.Lsposed -> MessageAlertDialog(
            onDismissRequest = onDismiss,
            title = dialog.title,
            message = dialog.explanation,
            closeLabel = stringResource(R.string.dialog_close_button),
        )
        null -> Unit
    }
}
