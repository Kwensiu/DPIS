package com.dpis.module

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dpis.module.appconfig.AppConfigSheetWizardStore
import com.dpis.module.appconfig.EditorPresentation
import com.dpis.module.home.HomeWorkspaceState
import com.dpis.module.settings.SystemFontScaleToolState
import com.dpis.module.templates.TemplateWorkspacePresentation
import com.dpis.module.templates.TemplateWorkspacePresentationSource
import com.dpis.module.templates.presentation.TemplateWorkspaceContent
import com.dpis.module.ui.compose.AppConfigEditorContent
import com.dpis.module.ui.compose.AppConfigEditorOverlay
import com.dpis.module.ui.compose.AppConfigSheetUiTokens
import com.dpis.module.ui.compose.AppHookChainEditorPage
import com.dpis.module.ui.compose.AppTypefacePickerPage
import com.dpis.module.ui.compose.AppWorkspaceContent
import com.dpis.module.ui.compose.ConfigEditorAnimatedContent
import com.dpis.module.home.presentation.HomeWorkspaceContent
import com.dpis.module.ui.compose.LocalWearWorkspaceContentPadding
import com.dpis.module.ui.compose.PageScrollPositionStore
import com.dpis.module.ui.compose.SettingsWorkspaceContent
import com.dpis.module.ui.compose.ToolsWorkspaceContent
import com.dpis.module.ui.presentation.WearAppConfigEditorContent
import com.dpis.module.ui.presentation.WearAppWorkspaceContent
import com.dpis.module.ui.presentation.WearHomeWorkspaceContent
import com.dpis.module.ui.presentation.WearSettingsWorkspaceContent
import com.dpis.module.ui.presentation.WearTemplateWorkspaceContent
import com.dpis.module.ui.presentation.WearToolsWorkspaceContent
import com.dpis.module.ui.compose.rememberEditorControlHeight

/** Compose workspace presentation boundary; domain actions remain in MainActivity. */
internal class MainWorkspacePresentationCoordinator(private val content: Content) {
    interface Content {
        fun homeState(): HomeWorkspaceState
        fun appState(): AppWorkspacePresentation.State
        fun appEditorState(): EditorPresentation.State?
        fun toolsState(): SystemFontScaleToolState?
        fun changeToolsPending(percent: Int)
        fun applyTools()
        fun restoreTools()
        fun requestToolsPermission()
        fun settingsState(): SettingsUiState?
        fun setSettingsHooks(enabled: Boolean)
        fun setSettingsSafeMode(enabled: Boolean)
        fun setSettingsGlobalLog(enabled: Boolean)
        fun openSettingsLogs()
        fun setSettingsLauncherHidden(hidden: Boolean)
        fun setSettingsScale(percent: Int)
        fun openSettingsScaleDetails()
        fun openSettingsFontDebug(); fun openSettingsFontLibrary(); fun openSettingsExperimental(); fun openThemeSettings()
        fun setSettingsLanguage(tag: String); fun openSettingsLanguage(); fun openSettingsBackup(); fun clearSettingsCache(); fun openSettingsAbout(); fun openSettingsDonate()
        fun templateWorkspace(): TemplateWorkspacePresentationSource
    }
    private var appRevision by mutableIntStateOf(0)
    private var homeRevision by mutableIntStateOf(0)
    private var toolsRevision by mutableIntStateOf(0)
    private var toolsExpanded by mutableStateOf(false)
    private var settingsRevision by mutableIntStateOf(0)
    private var templateRevision by mutableIntStateOf(0)
    @Composable fun render(mode: MainUiState.WorkspaceMode, padding: PaddingValues): Boolean {
        val pageScrollPositions = rememberSaveable(saver = PageScrollPositionStore.Saver) {
            PageScrollPositionStore()
        }
        val stateHolder = rememberSaveableStateHolder()
        Surface(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = fadeIn(animationSpec = tween(180)),
                        initialContentExit = fadeOut(animationSpec = tween(120)),
                        sizeTransform = null,
                    )
                },
                label = "workspace-page-transition",
            ) { targetMode ->
                stateHolder.SaveableStateProvider(targetMode.name) {
                    renderWorkspace(targetMode, padding, pageScrollPositions)
                }
            }
        }
        return true
    }

    @Composable
    private fun renderWorkspace(
        mode: MainUiState.WorkspaceMode,
        padding: PaddingValues,
        pageScrollPositions: PageScrollPositionStore,
    ) {
        when (mode) {
            MainUiState.WorkspaceMode.APP -> {
                appRevision
                ComposeWorkspaceSurface {
                    AppWorkspaceContent(
                        state = content.appState(),
                        padding = padding,
                        editorState = content.appEditorState()
                    )
                }
            }
            MainUiState.WorkspaceMode.HOME -> {
                homeRevision
                ComposeWorkspaceSurface {
                    HomeWorkspaceContent(content.homeState(), padding, pageScrollPositions)
                }
            }
            MainUiState.WorkspaceMode.TOOLS -> {
                toolsRevision
                ComposeWorkspaceSurface {
                    ToolsWorkspaceContent(
                        content.toolsState(),
                        padding,
                        toolsExpanded,
                        { toolsExpanded = !toolsExpanded },
                        content::changeToolsPending,
                        content::applyTools,
                        content::restoreTools,
                        content::requestToolsPermission,
                        pageScrollPositions,
                    )
                }
            }
            MainUiState.WorkspaceMode.SETTINGS -> {
                settingsRevision
                ComposeWorkspaceSurface {
                    SettingsWorkspaceContent(
                        content.settingsState(),
                        padding,
                        content::setSettingsHooks,
                        content::setSettingsSafeMode,
                        content::setSettingsGlobalLog,
                        content::openSettingsLogs,
                        content::setSettingsLauncherHidden,
                        content::openSettingsFontDebug,
                        content::openSettingsFontLibrary,
                        content::openSettingsExperimental,
                        content::openThemeSettings,
                        content::setSettingsLanguage,
                        content::openSettingsBackup,
                        content::clearSettingsCache,
                        content::openSettingsAbout,
                        content::openSettingsDonate,
                        pageScrollPositions,
                    )
                }
            }
            MainUiState.WorkspaceMode.TEMPLATE -> {
                templateRevision
                ComposeWorkspaceSurface {
                    TemplateWorkspaceContent(
                        state = content.templateWorkspace().state(),
                        padding = padding,
                        onQueryChanged = content.templateWorkspace()::changeQuery,
                        onEditorOpened = { quickTemplate, templateId ->
                            content.templateWorkspace().openEditor(quickTemplate, templateId)
                        },
                        onEditorChanged = content.templateWorkspace()::updateEditor,
                        onEditorDestinationChanged = content.templateWorkspace()::updateEditorDestination,
                        onEditorClosed = content.templateWorkspace()::closeEditor,
                        scrollStore = pageScrollPositions,
                    )
                }
            }
        }
    }
    @Composable fun renderWear(
        mode: MainUiState.WorkspaceMode,
        padding: PaddingValues
    ): Boolean {
        CompositionLocalProvider(LocalWearWorkspaceContentPadding provides padding) {
        when (mode) {
        MainUiState.WorkspaceMode.APP -> { appRevision; WearAppWorkspaceContent(content.appState()); true }
        MainUiState.WorkspaceMode.HOME -> { homeRevision; WearHomeWorkspaceContent(content.homeState()); true }
        MainUiState.WorkspaceMode.TOOLS -> {
            toolsRevision
            WearToolsWorkspaceContent(content.toolsState(), content::changeToolsPending, content::applyTools, content::restoreTools, content::requestToolsPermission)
            true
        }
        MainUiState.WorkspaceMode.SETTINGS -> {
            settingsRevision
            WearSettingsWorkspaceContent(
                content.settingsState(),
                content::setSettingsHooks,
                content::setSettingsSafeMode,
                content::setSettingsGlobalLog,
                content::openSettingsLogs,
                content::setSettingsLauncherHidden,
                content::openSettingsFontLibrary,
                content::openSettingsExperimental,
                content::openThemeSettings,
                content::openSettingsLanguage,
                content::openSettingsBackup,
                content::clearSettingsCache,
                content::openSettingsAbout
            )
            true
        }
        MainUiState.WorkspaceMode.TEMPLATE -> {
            templateRevision
            WearTemplateWorkspaceContent(
                state = content.templateWorkspace().state(),
                onEditorChanged = content.templateWorkspace()::updateEditor,
                onDestinationChanged = content.templateWorkspace()::updateEditorDestination,
                onEditorClosed = content.templateWorkspace()::closeEditor
            )
            true
        }
        }
        }
        return true
    }
    fun refreshApps() { appRevision++ }
    fun refreshHome() { homeRevision++ }
    fun refreshTools(collapse: Boolean = false) { if (collapse) toolsExpanded = false; toolsRevision++ }
    fun refreshSettings() { settingsRevision++ }
    fun refreshTemplates() { templateRevision++ }
    @Composable fun hasWearDetail(mode: MainUiState.WorkspaceMode): Boolean = when (mode) {
        MainUiState.WorkspaceMode.APP -> { appRevision; content.appEditorState() != null }
        MainUiState.WorkspaceMode.TEMPLATE -> {
            templateRevision
            content.templateWorkspace().state().detailKind != TemplateWorkspacePresentation.DetailKind.NONE
        }
        else -> false
    }
    @Composable fun RenderAppEditorOverlay(mode: MainUiState.WorkspaceMode, wear: Boolean = false) {
        // The editor session is Java-owned. Reading the same revision as the catalogue makes a
        // list-row click invalidate this root-level sibling as well as the list itself.
        appRevision
        if (mode != MainUiState.WorkspaceMode.APP) return
        if (wear) {
            content.appEditorState()?.let { WearAppConfigEditorContent(it) }
            return
        }
        BoxWithConstraints(androidx.compose.ui.Modifier.fillMaxSize()) {
            val configuration = LocalConfiguration.current
            val twoPane = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                maxWidth >= 600.dp
            if (twoPane) return@BoxWithConstraints
            val editorState = content.appEditorState() ?: return@BoxWithConstraints
            val editorControlHeight = rememberEditorControlHeight()
            val context = LocalContext.current
            var showAdvancedHint by remember(editorState.item.packageName) {
                mutableStateOf(AppConfigSheetWizardStore.shouldShowAdvancedHint(context))
            }
            AppConfigEditorOverlay(
                onDismissRequest = editorState.actions::close,
                destination = editorState.destination,
                onReturnToMain = {
                    editorState.actions.navigate(editorState.destination.backDestination())
                },
                topChrome = {
                    // This is deliberately visual-only chrome. The sheet owns its drag gesture;
                    // the short bar switches to the legacy unsaved badge without becoming an
                    // interactive state control.
                    Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .height(AppConfigSheetUiTokens.TopChromeHeight)
                    ) {
                        if (editorState.dirty) {
                            Surface(
                                modifier = androidx.compose.ui.Modifier.align(Alignment.Center),
                                shape = AppConfigSheetUiTokens.UnsavedBadgeShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    stringResource(R.string.sheet_unsaved_badge),
                                    modifier = androidx.compose.ui.Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 4.dp
                                    ),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        } else {
                            Box(
                                modifier = androidx.compose.ui.Modifier
                                    .align(Alignment.Center)
                                    .width(AppConfigSheetUiTokens.TopChromeIndicatorWidth)
                                    .height(AppConfigSheetUiTokens.TopChromeIndicatorHeight)
                                    .clip(AppConfigSheetUiTokens.TopChromeIndicatorShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        if (!editorState.destination.isChildPage) Box(
                            modifier = androidx.compose.ui.Modifier
                                // Keep the diagnostic action on the same center line as the
                                // visual white bar; it is not independently top-aligned chrome.
                                .align(Alignment.CenterEnd)
                                .padding(end = 20.dp)
                                .size(AppConfigSheetUiTokens.FeedbackActionSize)
                                .clip(AppConfigSheetUiTokens.FeedbackActionShape)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    AppConfigSheetUiTokens.FeedbackActionShape
                                )
                                .clickable(
                                    role = Role.Button,
                                    onClick = editorState.actions::startFeedbackDiagnostic
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_bug_report_24),
                                contentDescription = stringResource(R.string.feedback_diagnostic_action),
                                modifier = androidx.compose.ui.Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                },
                content = { onAdvancedAnchorMeasured, isExpanded, onReturnFromChild ->
                ConfigEditorAnimatedContent(
                    destination = editorState.destination,
                    // Expanded sheets ignore the peek anchor, so their destination content owns
                    // height. Partially expanded sheets keep that ownership in the peek anchor.
                    // Keep all child pages on the same transition path. The active destination
                    // owns pointer input while the outgoing page is clipped to shared bounds.
                    // This full-width sheet is the stable viewport for the transition. Clipping
                    // to AnimatedContent's changing size cuts off the outgoing page while its
                    // horizontal exit is still running and makes the sheet appear to jump.
                    clipContentToAnimatedBounds = false,
                    // Returning to MAIN must not animate the fixed child viewport down through
                    // AnimatedContent; the sheet already owns the return height transition.
                    // SheetState owns the vertical transition. Keeping AnimatedContent's size
                    // transform snapped prevents a child list remeasurement from becoming a
                    // second sheet-height animation during return.
                    animateSize = false,
                    mainContent = {
                        AppConfigEditorContent(
                            editorState,
                            onAdvancedAnchorMeasured = { measuredAnchor ->
                                // The main page can remain composed while a child page enters.
                                // Only MAIN may establish the collapsed editor anchor.
                                if (editorState.destination == ConfigEditorDestination.MAIN) {
                                    onAdvancedAnchorMeasured(
                                        measuredAnchor + if (!isExpanded) {
                                            (editorControlHeight -
                                                AppConfigSheetUiTokens.ActionHeight)
                                                .coerceAtLeast(0.dp)
                                        } else {
                                            0.dp
                                        }
                                    )
                                }
                            },
                            showInlineUnsavedBadge = false,
                        )
                    },
                    hookContent = {
                        AppHookChainEditorPage(
                            state = editorState,
                            // Hook pages use a stable viewport, so only the page transition itself
                            // animates; the outer sheet does not receive a transient height signal.
                            onBack = onReturnFromChild,
                        )
                    },
                    typefaceContent = {
                        AppTypefacePickerPage(
                            selectedTypefaceId = editorState.draft.selectedTypefaceId ?: "",
                            onTypefaceSelected = { typefaceId ->
                                editorState.actions.updateTypeface(typefaceId ?: "")
                                onReturnFromChild()
                            },
                            onBack = onReturnFromChild,
                        )
                    }
                )
            },
            overlayContent = {
                if (showAdvancedHint && !editorState.destination.isChildPage) {
                    Column(
                        modifier = androidx.compose.ui.Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = AppConfigSheetUiTokens.WizardHintTopOffset)
                            .padding(horizontal = 20.dp)
                            .zIndex(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.bg_app_config_wizard_arrow),
                            contentDescription = null,
                            modifier = androidx.compose.ui.Modifier
                                .size(width = 14.dp, height = 7.dp)
                                .rotate(180f),
                            tint = Color.Unspecified
                        )
                        Surface(
                            shape = AppConfigSheetUiTokens.WizardHintShape,
                            color = colorResource(R.color.app_config_wizard_bubble_container),
                            contentColor = colorResource(R.color.app_config_wizard_bubble_text)
                        ) {
                            Row(
                                modifier = androidx.compose.ui.Modifier.padding(
                                    start = 14.dp, top = 6.dp, end = 6.dp, bottom = 6.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.dialog_advanced_wizard_hint),
                                    modifier = androidx.compose.ui.Modifier.weight(1f, fill = false),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Box(
                                    modifier = androidx.compose.ui.Modifier
                                        .padding(start = 8.dp)
                                        .size(AppConfigSheetUiTokens.WizardHintCloseSize)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .clickable(role = Role.Button, onClick = {
                                            AppConfigSheetWizardStore.markAdvancedHintDismissed(context)
                                            showAdvancedHint = false
                                        }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close_24),
                                        contentDescription = stringResource(
                                            R.string.dialog_advanced_wizard_close
                                        ),
                                        modifier = androidx.compose.ui.Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            )
        }
    }
}

/** Supplies the Material content color for unframed workspace titles and empty states. */
@Composable
private fun ComposeWorkspaceSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        // The workspace transition owns only page content. Keeping this carrier
        // transparent prevents the shared page background from fading with it.
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = content
    )
}
