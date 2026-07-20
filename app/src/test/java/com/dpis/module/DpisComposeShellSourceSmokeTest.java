package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

/** Guards the Theme 1 baseline while individual XML workspaces migrate later. */
public final class DpisComposeShellSourceSmokeTest {
    @Test
    public void composeThemeAndShellKeepTheRequiredBoundaries() throws IOException {
        String theme = read("src/main/java/com/dpis/module/ui/compose/DpisTheme.kt");
        String shell = read("src/main/java/com/dpis/module/ui/compose/DpisWorkspaceShell.kt");
        String adapter = read("src/main/java/com/dpis/module/MainComposeWorkspaceAdapter.java");
        String mainShell = read("src/main/java/com/dpis/module/MainComposeWorkspaceShell.kt");
        String haptics = read("src/main/java/com/dpis/module/ui/compose/DpisComposeHaptics.kt");
        String previews = read("src/main/java/com/dpis/module/ui/compose/DpisWorkspaceShellPreviews.kt");
        String legacyHost = read("src/main/java/com/dpis/module/ui/compose/DpisLegacyWorkspaceHost.kt");
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");
        String localizedActivity = read("src/main/java/com/dpis/module/LocalizedActivity.java");
        String presentation = read(
                "src/main/java/com/dpis/module/templates/TemplateWorkspacePresentation.kt");
        String coordinator = read(
                "src/main/java/com/dpis/module/MainWorkspacePresentationCoordinator.kt");

        assertTrue(theme.contains("fun DpisTheme("));
        assertTrue(theme.contains("dynamicLightColorScheme"));
        assertTrue(theme.contains("dynamicDarkColorScheme"));
        assertTrue(shell.contains("APP(R.string.workspace_app"));
        assertTrue(shell.contains("TEMPLATE(R.string.workspace_template"));
        assertTrue(shell.contains("HOME(R.string.workspace_home"));
        assertTrue(shell.contains("TOOLS(R.string.workspace_tools"));
        assertTrue(shell.contains("SETTINGS(R.string.workspace_settings"));
        assertTrue(shell.contains("stringResource(destination.labelRes)"));
        assertTrue(shell.contains("R.drawable.ic_apps_24"));
        assertTrue(shell.contains("R.drawable.ic_template_24"));
        assertTrue(shell.contains("R.drawable.ic_home_24"));
        assertTrue(shell.contains("R.drawable.ic_build_24"));
        assertTrue(shell.contains("R.drawable.ic_settings_24"));
        assertTrue(shell.contains("painterResource(destination.iconRes)"));
        assertTrue(!shell.contains("Icons.Outlined"));
        assertTrue(shell.contains("alwaysShowLabel = false"));
        assertTrue(shell.contains("NavigationBar(windowInsets = WindowInsets.navigationBars)"));
        assertTrue(shell.contains("containerColor = MaterialTheme.colorScheme.surfaceContainer"));
        assertTrue(shell.contains("drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer"));
        assertTrue(shell.contains("windowInsets = navigationSurfaceInsets()"));
        assertTrue(shell.contains("WindowInsetsSides.Start + WindowInsetsSides.Vertical"));
        assertTrue(shell.contains("legacyWorkspaceInsetsFor(selectedDestination)"));
        assertTrue(shell.contains("legacyBottomNavigationPadding(scaffoldPadding)"));
        assertTrue(shell.contains("DpisWorkspaceDestination.SETTINGS -> PaddingValues(end = endPadding)"));
        assertTrue(shell.contains("stringResource(R.string.app_name)"));
        assertTrue(shell.contains("DpisWorkspaceDestination.HOME"));
        assertTrue(shell.contains("DpisWorkspaceDestination.TOOLS"));
        assertTrue(shell.contains("DpisWorkspaceDestination.SETTINGS"));
        assertTrue(shell.contains("BOTTOM_BAR"));
        assertTrue(shell.contains("NAVIGATION_RAIL"));
        assertTrue(shell.contains("NAVIGATION_DRAWER"));
        assertTrue(shell.contains("isCompactUi -> DpisWorkspaceNavigationLayout.BOTTOM_BAR"));
        assertTrue(shell.contains("selectedDestination: DpisWorkspaceDestination"));
        assertTrue(shell.contains("onDestinationSelected: (DpisWorkspaceDestination) -> Unit"));
        assertTrue(adapter.contains("MainUiState.WorkspaceMode"));
        assertTrue(mainShell.contains("MainUiAction.workspaceModeChanged"));
        assertTrue(mainShell.contains("MainComposeWorkspaceAdapter.destinationFor(state.workspaceMode)"));
        assertTrue(previews.contains("Phone"));
        assertTrue(previews.contains("Tablet"));
        assertTrue(previews.contains("Desktop"));
        assertTrue(mainActivity.contains("installComposeWorkspaceShell();"));
        assertTrue(mainActivity.contains("new MainComposeShellHost("));
        assertTrue(mainActivity.contains("composeShellContentBottomPadding"));
        assertTrue(mainActivity.contains("composeShellHost.replayLegacyWorkspaceInsets(mode)"));
        assertTrue(mainActivity.contains("WindowInsetsBinder.refreshNavigationBarMargins(searchFocusFab)"));
        assertTrue(mainActivity.contains("if (WatchUiMode.shouldUseCompactUi(this))"));
        assertTrue(mainActivity.contains("getLastCustomNonConfigurationInstance()"));
        assertTrue(mainActivity.contains("onRetainCustomNonConfigurationInstance()"));
        assertTrue(localizedActivity.contains("import androidx.activity.ComponentActivity;"));
        assertTrue(localizedActivity.contains("extends ComponentActivity"));
        assertTrue(legacyHost.contains("ViewCompat.getRootWindowInsets(view)"));
        assertTrue(legacyHost.contains("ViewCompat.dispatchApplyWindowInsets(view, rootInsets)"));
        assertTrue(shell.contains("rememberDpisConfirmAction"));
        assertTrue(haptics.contains("HapticFeedbackType.Confirm"));
        assertTrue(presentation.contains("object TemplateWorkspacePresentation"));
        assertTrue(presentation.contains("QuickTemplateStore(preferences).readAll()"));
        assertTrue(presentation.contains("GlobalPrefillStore(preferences).read()"));
        assertTrue(presentation.contains("fun applyTemplate(id: String)"));
        assertTrue(presentation.contains("fun selectTargets(id: String)"));
        assertTrue(mainActivity.contains("ensureComposeTemplateWorkspacePresentation().state()"));
        assertTrue(coordinator.contains("state = content.templateState()"));
        assertTrue(coordinator.contains("onQueryChanged = content::changeTemplateQuery"));
        assertTrue(coordinator.contains("onEditorOpened ="));
        assertTrue(coordinator.contains("onEditorChanged = content::updateTemplateEditor"));
        assertTrue(coordinator.contains("onEditorClosed = content::closeTemplateEditor"));
        assertTrue(coordinator.contains("fun changeTemplateQuery(query: String)"));
        assertTrue(coordinator.contains("content.usesComposeTemplateWorkspace()"));
        assertTrue(coordinator.contains("private fun ComposeWorkspaceSurface("));
        assertTrue(coordinator.contains("contentColor = MaterialTheme.colorScheme.onSurface"));
        assertTrue(coordinator.contains("ComposeWorkspaceSurface { HomeWorkspaceContent("));
        assertTrue(coordinator.contains("ComposeWorkspaceSurface {\n                    TemplateWorkspaceContent("));
    }

    @Test
    public void interfaceScaleSliderShowsOnlyEndpointMarkers() throws IOException {
        String settings = read("src/main/java/com/dpis/module/ui/compose/SettingsWorkspaceContent.kt");
        String home = read("src/main/java/com/dpis/module/ui/compose/HomeWorkspaceContent.kt");
        String tools = read("src/main/java/com/dpis/module/ui/compose/ToolsWorkspaceContent.kt");
        String support = read("src/main/java/com/dpis/module/ui/compose/SupportPages.kt");

        assertTrue(settings.contains("steps = 0"));
        assertTrue(settings.contains("SliderDefaults.Track"));
        assertTrue(settings.contains("latestGestureValue.floatValue"));
        assertTrue(settings.contains("AppUiScaleManager.normalizeSliderPercent(changedValue)"));
        assertTrue(settings.contains("HapticFeedbackConstants.CLOCK_TICK"));
        assertTrue(settings.contains("rememberDpisConfirmAction(onDetails)"));
        assertTrue(settings.contains("Surface owns the entire-card ripple"));
        assertTrue(settings.contains(
                "state?.languageLabel ?: stringResource(R.string.settings_language_follow_system),\n"
                        + "                    enabled = state?.storeAvailable == true"));
        assertTrue(settings.contains(
                "enabled = state?.storeAvailable == true && state.cacheClearInProgress != true"));
        assertTrue(home.contains("rememberDpisConfirmAction"));
        assertTrue(home.contains(".clip(CircleShape)"));
        assertTrue(home.contains("Spacer(Modifier.height(24.dp))"));
        assertTrue(tools.contains("rememberDpisConfirmAction"));
        assertTrue(tools.contains("SystemFontScaleBadge(state)"));
        assertTrue(tools.contains("if (!state.canWrite && !state.unavailable)"));
        assertTrue(tools.contains("else if (state.unavailable)"));
        assertTrue(tools.contains("state.canDecrement()"));
        assertTrue(tools.contains("state.canRestore()"));
        assertTrue(tools.contains("SystemFontScaleToolState.normalizeSliderPercent(it)"));
        assertTrue(tools.contains("LocalDensity provides Density(displayDensity.density, fontScale = 1f)"));
        assertTrue(tools.contains(
                "contentWindowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top)"));
        assertTrue(tools.contains(
                "windowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top)"));
        assertTrue(settings.contains(
                "contentWindowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top)"));
        assertTrue(settings.contains(
                "windowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top)"));
        assertTrue(support.contains("rememberDpisConfirmAction"));
    }

    @Test
    public void templateEditorUsesExpandedModalStateAndObservesDraftRevision() throws IOException {
        String workspace = read(
                "src/main/java/com/dpis/module/ui/compose/TemplateWorkspaceContent.kt");
        String editor = read(
                "src/main/java/com/dpis/module/ui/compose/TemplateEditorContent.kt");

        assertTrue(workspace.contains(
                "rememberModalBottomSheetState(skipPartiallyExpanded = true)"));
        assertTrue(workspace.contains("val draftRevision = editorDraft.observe()"));
        assertTrue(workspace.contains("draftRevision = draftRevision"));
        assertTrue(editor.contains("draftRevision: Int"));
        assertTrue(editor.contains("TemplateSheetDragHandle("));
    }

    @Test
    public void templateWorkspaceKeepsTheLegacySearchAndHeaderActionSemantics() throws IOException {
        String template = read(
                "src/main/java/com/dpis/module/ui/compose/TemplateWorkspaceContent.kt");
        String tokens = read(
                "src/main/java/com/dpis/module/ui/compose/TemplateUiTokens.kt");

        assertTrue(template.contains("TemplateWorkspaceSearchCard("));
        assertTrue(template.contains("onQueryChanged = onQueryChanged"));
        assertTrue(template.contains(
                "cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)"));
        assertTrue(template.contains("R.drawable.ic_close_24"));
        assertTrue(template.contains("role = Role.Button"));
        assertTrue(template.contains("TemplateUiTokens.DisabledActionAlpha"));
        assertTrue(template.contains("TemplateUiTokens.EmptySummaryTopGap"));
        assertTrue(template.contains("TemplateUiTokens.CardActionVisualSize"));
        assertTrue(template.contains("TemplateApplyAction("));
        assertTrue(template.contains("R.drawable.ic_done_all_24"));
        assertTrue(template.contains("R.string.template_workspace_action_apply"));
        assertTrue(template.contains("TemplateActionButtonStyle.Plain"));
        assertTrue(template.contains("TemplateActionButtonStyle.Primary"));
        assertFalse(template.contains("SearchCardBorderWidth"));
        assertTrue(tokens.contains("val WorkspaceTopPadding = 14.dp"));
        assertTrue(tokens.contains("val SectionTitleInset = 12.dp"));
        assertTrue(tokens.contains("val SectionActionInset = 12.dp"));
        assertTrue(tokens.contains("val HeaderActionVisualSize = 36.dp"));
        assertTrue(tokens.contains("val CardActionVisualSize = 28.dp"));
        assertTrue(tokens.contains("val ApplyActionVisualSize = CardActionVisualSize"));
        assertTrue(tokens.contains("const val DisabledActionAlpha = 0.45f"));
    }

    @Test
    public void templateEditorKeepsErrorsOutsideTheFixedInputOutline() throws IOException {
        String editor = read(
                "src/main/java/com/dpis/module/ui/compose/TemplateEditorContent.kt");

        assertTrue(editor.contains("val viewportError = if (form.isViewportInputValid())"));
        assertTrue(editor.contains("isError = viewportError != null"));
        assertTrue(editor.contains("isError = fontError != null"));
        assertTrue(editor.contains("TemplateEditorErrorMessage(it)"));
        assertTrue(editor.contains("modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 8.dp)"));
        assertTrue(editor.contains("modifier = modifier.height(TemplateUiTokens.SheetInputHeight)"));
        assertTrue(editor.contains("color = MaterialTheme.colorScheme.onSurface"));
        assertTrue(editor.contains("cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)"));
        assertTrue(editor.contains("val hookDomainsButtonText = FontHookDomainPresentation"));
        assertTrue(editor.contains("forRecommendedTemplateRaw(form.fontHookDomainsRaw)"));
        assertTrue(editor.contains(".buttonText(LocalContext.current)"));
        assertFalse(editor.contains("dialog_font_hook_domains_title_with_count, 1, 1"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(new File(relativePath).toPath()),
                StandardCharsets.UTF_8
        );
    }
}
