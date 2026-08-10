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
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");
        String localizedActivity = read("src/main/java/com/dpis/module/LocalizedActivity.kt");
        String presentation = read(
                "src/main/java/com/dpis/module/templates/TemplateWorkspacePresentation.kt");
        String coordinator = read(
                "src/main/java/com/dpis/module/MainWorkspacePresentationCoordinator.kt");
        String appWorkspace = read(
                "src/main/java/com/dpis/module/ui/compose/AppWorkspaceContent.kt");
        String wearWorkspace = read(
                "src/main/java/com/dpis/module/ui/compose/WearWorkspaceContent.kt");

        assertTrue(theme.contains("fun DpisTheme("));
        assertTrue(theme.contains("DpisColorSchemeFactory.create("));
        assertTrue(theme.contains("system_accent1_500"));
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
        assertFalse(shell.contains("Icons.Outlined"));
        assertTrue(shell.contains("alwaysShowLabel = false"));
        assertTrue(shell.contains("NavigationBar(windowInsets = bottomNavigationSurfaceInsets())"));
        assertTrue(shell.contains("background(MaterialTheme.colorScheme.surfaceContainer)"));
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
        assertTrue(shell.contains("WorkspaceDrawerMinWindowWidth ="
                + " WorkspaceTwoPaneMinWidth + WorkspaceDrawerWidth"));
        assertTrue(shell.contains("Modifier.width(WorkspaceDrawerWidth)"));
        assertTrue(shell.contains("verticalScroll(rememberScrollState())"));
        assertTrue(shell.contains("isCompactUi -> DpisWorkspaceNavigationLayout.COMPACT_RADIAL"));
        assertTrue(shell.contains("CompactWearWorkspaceNavigation("));
        assertTrue(shell.contains("WearMaterialTheme"));
        assertTrue(shell.contains("AppScaffold"));
        assertTrue(shell.contains("ScreenScaffold(scrollState = listState)"));
        assertTrue(shell.contains("TransformingLazyColumn("));
        assertTrue(shell.contains("transformedHeight(this, transformationSpec)"));
        assertTrue(shell.contains("WearButtonDefaults.minimumVerticalListContentPadding"));
        assertTrue(shell.contains("WearCompactButton("));
        assertTrue(shell.contains("BackHandler(enabled = expanded)"));
        assertFalse(shell.contains("COMPACT_MENU_ARC_RADIUS_DP"));
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
        assertFalse(read("src/main/java/com/dpis/module/MainComposeShellHost.kt")
                .contains("DpisLegacyWorkspaceHost"));
        assertTrue(mainActivity.contains("WatchUiMode.shouldUseCompactUi(this),"));
        String shellHost = read("src/main/java/com/dpis/module/MainComposeShellHost.kt");
        assertTrue(shellHost.contains("if (isCompactUi)"));
        assertTrue(shellHost.contains("workspacePresentation.renderWear(state.workspaceMode, padding)"));
        assertTrue(coordinator.contains("@Composable fun renderWear"));
        assertTrue(wearWorkspace.contains("TransformingLazyColumn("));
        assertTrue(wearWorkspace.contains("ScreenScaffold("));
        assertTrue(wearWorkspace.contains("SwitchButton("));
        assertTrue(wearWorkspace.contains("SurfaceTransformation(transformationSpec)"));
        assertTrue(wearWorkspace.contains("WearAppConfigEditorContent"));
        assertTrue(wearWorkspace.contains("WearTemplateEditorContent"));
        assertTrue(wearWorkspace.contains("WearTypefacePickerPage"));
        assertTrue(wearWorkspace.contains("WearHookChainEditorPage"));
        assertTrue(wearWorkspace.contains("BasicTextField("));
        assertFalse(wearWorkspace.contains("                AppConfigEditorContent("));
        assertFalse(wearWorkspace.contains("TemplateEditorSurface("));
        assertTrue(shell.contains("WearMaterialTheme(colorScheme = wearColors)"));
        assertTrue(shell.contains("background = phoneColors.background"));
        assertTrue(mainActivity.contains("getLastCustomNonConfigurationInstance()"));
        assertTrue(mainActivity.contains("onRetainCustomNonConfigurationInstance()"));
        assertTrue(localizedActivity.contains("import androidx.activity.ComponentActivity"));
        assertTrue(localizedActivity.contains(": ComponentActivity()"));
        assertFalse(mainActivity.contains("removeView(landDetailPane)"));
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
        assertFalse(coordinator.contains("usesComposeTemplateWorkspace"));
        assertTrue(appWorkspace.contains("fun AppWorkspaceContent("));
        assertTrue(coordinator.contains("appRevision"));
        assertTrue(coordinator.contains("fun refreshApps()"));
        assertTrue(appWorkspace.contains("PullToRefreshBox("));
        assertTrue(appWorkspace.contains("allAppsListState"));
        assertTrue(appWorkspace.contains("configuredAppsListState"));
        assertTrue(appWorkspace.contains("actions.openApp(item)"));
        assertTrue(appWorkspace.contains(
                "configuration.orientation == Configuration.ORIENTATION_LANDSCAPE"));
        assertTrue(appWorkspace.contains(
                "val twoPane = compactVerticalChrome && maxWidth >= WorkspaceTwoPaneMinWidth"));
        assertFalse(appWorkspace.contains("state.actions::requestIcon"));
        assertTrue(appWorkspace.contains("@Preview(showBackground = true"));
        assertTrue(coordinator.contains("private fun ComposeWorkspaceSurface("));
        assertTrue(coordinator.contains("contentColor = MaterialTheme.colorScheme.onSurface"));
        assertTrue(coordinator.contains("ComposeWorkspaceSurface { HomeWorkspaceContent("));
        assertTrue(coordinator.contains("TemplateWorkspaceContent("));
    }

    @Test
    public void interfaceScaleSliderShowsOnlyEndpointMarkers() throws IOException {
        String settings = read("src/main/java/com/dpis/module/ui/compose/SettingsWorkspaceContent.kt");
        String theme = read("src/main/java/com/dpis/module/ui/compose/ThemeSettingsContent.kt");
        String home = read("src/main/java/com/dpis/module/ui/compose/HomeWorkspaceContent.kt");
        String tools = read("src/main/java/com/dpis/module/ui/compose/ToolsWorkspaceContent.kt");
        String support = read("src/main/java/com/dpis/module/ui/compose/SupportPages.kt");

        assertTrue(theme.contains("steps = 0"));
        assertTrue(theme.contains("SliderDefaults.Track"));
        assertTrue(theme.contains("latestGestureValue.floatValue"));
        assertTrue(theme.contains("AppUiScaleManager.normalizeSliderPercent(changedValue)"));
        assertTrue(theme.contains("HapticFeedbackConstants.CLOCK_TICK"));
        assertTrue(theme.contains("style = MaterialTheme.typography.titleMedium"));
        assertFalse(settings.contains("SettingsScaleRow("));
        assertTrue(settings.contains(
                "state?.languageLabel ?: stringResource(R.string.settings_language_follow_system)"));
        assertTrue(settings.contains(
                "enabled = state?.storeAvailable == true && state.cacheClearInProgress != true"));
        assertTrue(home.contains("rememberDpisConfirmAction"));
        assertTrue(home.contains(".clip(CircleShape)"));
        assertTrue(home.contains("PrimaryPageScaffold("));
        assertTrue(home.contains("contentPadding = PaddingValues("));
        assertTrue(tools.contains("rememberDpisConfirmAction"));
        assertTrue(tools.contains("SystemFontScaleBadge(state)"));
        assertTrue(tools.contains("if (!state.canWrite && !state.unavailable)"));
        assertTrue(tools.contains("else if (state.unavailable)"));
        assertTrue(tools.contains("state.canDecrement()"));
        assertTrue(tools.contains("state.canRestore()"));
        assertTrue(tools.contains("SystemFontScaleToolState.normalizeSliderPercent(it)"));
        assertTrue(tools.contains("LocalDensity provides Density(displayDensity.density, fontScale = 1f)"));
        assertTrue(tools.contains("PrimaryPageScaffold("));
        assertFalse(tools.contains("TopAppBar("));
        assertTrue(settings.contains("PrimaryPageScaffold("));
        assertFalse(settings.contains("TopAppBar("));
        assertTrue(support.contains("rememberDpisConfirmAction"));
    }

    @Test
    public void standaloneSettingsPagesUseSharedSecondaryPageChrome() throws IOException {
        String support = read("src/main/java/com/dpis/module/ui/compose/SupportPages.kt");
        String theme = read("src/main/java/com/dpis/module/ui/compose/ThemeSettingsContent.kt");
        String experimental = read(
                "src/main/java/com/dpis/module/ui/compose/ExperimentalSettingsContent.kt");

        assertTrue(support.contains("internal fun SecondaryPageScaffold("));
        assertTrue(support.contains("internal fun PrimaryPageScaffold("));
        assertTrue(support.contains("SecondaryPageTopBar("));
        assertTrue(theme.contains("SecondaryPageScaffold("));
        assertTrue(experimental.contains("SecondaryPageScaffold("));
        assertFalse(theme.contains("TopAppBar("));
        assertFalse(experimental.contains("CenterAlignedTopAppBar("));
    }

    @Test
    public void themeSettingsExpandsStaticColorOptionsWhenDynamicColorIsDisabled()
            throws IOException {
        String theme = read("src/main/java/com/dpis/module/ui/compose/ThemeSettingsContent.kt");
        String support = read("src/main/java/com/dpis/module/ui/compose/SupportActivityContent.kt");
        String colors = read("src/main/java/com/dpis/module/ui/compose/DpisTheme.kt");

        assertTrue(theme.contains("ThemeDynamicColorRow("));
        assertTrue(theme.contains("AnimatedConditionalItem(visible = !dynamicColorEnabled)"));
        assertTrue(theme.contains("R.string.settings_theme_color_label"));
        assertTrue(theme.contains("R.string.settings_theme_palette_style_label"));
        assertTrue(theme.contains("R.string.settings_theme_color_spec_label"));
        assertTrue(theme.contains("ThemeColorOption(ThemeModeStore.DEFAULT_STATIC_THEME_COLOR)"));
        assertFalse(theme.contains("ThemeColorOption(\"default\")"));
        assertTrue(support.contains("ThemeModeStore.setDynamicColorEnabled(activity, enabled)"));
        assertTrue(support.contains("dynamicColorEnabled = enabled"));
        assertTrue(colors.contains("ThemeModeStore.isDynamicColorEnabled(context)"));
    }

    @Test
    public void templateEditorUsesExpandedModalStateAndObservesDraftRevision() throws IOException {
        String workspace = read(
                "src/main/java/com/dpis/module/ui/compose/TemplateWorkspaceContent.kt");
        String editor = read(
                "src/main/java/com/dpis/module/ui/compose/TemplateEditorContent.kt");

        assertTrue(workspace.contains(
                "TemplateEditorSurface("));
        assertTrue(workspace.contains(
                "surface = TemplateEditorSurfaceKind.PORTRAIT_SHEET"));
        assertTrue(workspace.contains("onDismissRequest = ::closeEditor"));
        assertTrue(workspace.contains("val draftRevision = editorDraft.observe()"));
        assertTrue(workspace.contains("draftRevision = draftRevision"));
        assertTrue(editor.contains("draftRevision: Int"));
        assertTrue(editor.contains("DpisEditorBottomSheet("));
        assertTrue(editor.contains("DpisSheetVisualChrome(showUnsaved = form.isDirty())"));
        assertTrue(editor.contains("contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) }"));
        assertTrue(editor.contains(
                "padding(bottom = edgeToEdgeContentBottomPadding(extraBottomPadding))"));
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
                "val twoPane = isLandscape && maxWidth >= WorkspaceTwoPaneMinWidth"));
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
        String controls = read(
                "src/main/java/com/dpis/module/ui/compose/DpisEditorControls.kt");

        assertTrue(editor.contains("val viewportError = if (form.isViewportInputValid())"));
        assertTrue(editor.contains("isError = viewportError != null"));
        assertTrue(editor.contains("isError = fontError != null"));
        assertTrue(editor.contains("TemplateEditorErrorMessage(it)"));
        assertTrue(editor.contains("modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 8.dp)"));
        assertTrue(controls.contains(".height(AppConfigSheetUiTokens.ActionHeight)"));
        assertTrue(controls.contains("color = MaterialTheme.colorScheme.onSurface"));
        assertTrue(controls.contains("cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)"));
        assertTrue(editor.contains("val hookDomainsButtonText = FontHookDomainPresentation"));
        assertTrue(editor.contains("forRecommendedTemplateRaw(form.fontHookDomainsRaw)"));
        assertTrue(editor.contains(".buttonText(context)"));
        assertFalse(editor.contains("dialog_font_hook_domains_title_with_count, 1, 1"));
    }

    @Test
    public void composeAppEditorRestoreDoesNotOpenLegacySheet() throws IOException {
        String activity = read("src/main/java/com/dpis/module/MainActivity.java");
        int restoreStart = activity.indexOf(
                "private void restoreAppEditorForCurrentWorkspace()");
        int restoreEnd = activity.indexOf(
                "private void applyLandscapeDetailVisibility", restoreStart);
        String restore = activity.substring(restoreStart, restoreEnd);

        int composeGuard = restore.indexOf("if (composeShellHost != null)");
        int legacySheet = restore.indexOf("showEditBottomSheet(appItem)");
        assertTrue(composeGuard >= 0);
        assertTrue(legacySheet > composeGuard);
        assertTrue(restore.substring(composeGuard, legacySheet).contains("return;"));
    }

    @Test
    public void composeAppSheetPreservesPartialExpandAndLegacyChromeSemantics()
            throws IOException {
        String sheet = read("src/main/java/com/dpis/module/ui/compose/DpisEditorBottomSheet.kt");
        String coordinator = read(
                "src/main/java/com/dpis/module/MainWorkspacePresentationCoordinator.kt");
        String overlay = read("src/main/java/com/dpis/module/ui/compose/AppConfigEditorOverlay.kt");
        String appEditor = read(
                "src/main/java/com/dpis/module/ui/compose/AppConfigEditorContent.kt");
        String appWorkspace = read(
                "src/main/java/com/dpis/module/ui/compose/AppWorkspaceContent.kt");
        String controls = read(
                "src/main/java/com/dpis/module/ui/compose/DpisEditorControls.kt");
        String catalog = read(
                "src/main/java/com/dpis/module/applist/InstalledAppCatalogCoordinator.kt");
        String typefacePicker = read(
                "src/main/java/com/dpis/module/ui/compose/AppTypefacePickerPage.kt");
        String activity = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(sheet.contains("skipPartiallyExpanded: Boolean = true"));
        assertTrue(sheet.contains("rememberBottomSheetState("));
        assertTrue(sheet.contains("SheetValue.PartiallyExpanded"));
        assertTrue(coordinator.contains("AppConfigSheetWizardStore.shouldShowAdvancedHint(context)"));
        assertTrue(coordinator.contains("AppConfigEditorOverlay("));
        assertTrue(coordinator.contains("RenderAppEditorOverlay(mode: MainUiState.WorkspaceMode, wear: Boolean = false)"));
        assertTrue(coordinator.contains("WearAppConfigEditorContent(it)"));
        assertTrue(coordinator.contains("appRevision"));
        assertTrue(coordinator.contains("if (mode != MainUiState.WorkspaceMode.APP) return"));
        assertTrue(coordinator.contains(
                "configuration.orientation == Configuration.ORIENTATION_LANDSCAPE"));
        assertTrue(coordinator.contains("if (twoPane) return@BoxWithConstraints"));
        assertTrue(overlay.contains("initialValue = SheetValue.Hidden"));
        assertTrue(overlay.contains("skipHiddenState = false"));
        assertTrue(overlay.contains("bottomSheetState.partialExpand()"));
        assertTrue(overlay.contains("targetValue == SheetValue.Hidden"));
        assertTrue(overlay.contains("app-config-sheet-scrim"));
        assertTrue(overlay.contains("LaunchedEffect(advancedAnchor)"));
        assertTrue(overlay.contains("value == SheetValue.Hidden"));
        assertTrue(overlay.contains("sheetDragHandle = null"));
        assertTrue(overlay.contains("sheetPeekHeight = measuredPeekHeight"));
        assertTrue(overlay.contains("returnToMainPending = false"));
        assertTrue(overlay.contains("wasChild && !isChild"));
        assertTrue(overlay.contains("advancedAnchor"));
        assertTrue(overlay.contains("maxHeight * 0.75f"));
        assertTrue(coordinator.contains("R.string.dialog_advanced_wizard_hint"));
        assertTrue(coordinator.contains("R.string.feedback_diagnostic_action"));
        assertTrue(coordinator.contains("AppConfigSheetUiTokens.TopChromeIndicatorWidth"));
        assertTrue(coordinator.contains("if (editorState.dirty)"));
        assertTrue(coordinator.contains("R.string.sheet_unsaved_badge"));
        assertTrue(coordinator.contains("showInlineUnsavedBadge = false"));
        assertTrue(appEditor.contains("showInlineUnsavedBadge: Boolean = true"));
        assertTrue(appEditor.contains("coordinates.positionInParent().y.toDp()"));
        assertTrue(appEditor.contains(
                "padding(bottom = edgeToEdgeContentBottomPadding(0.dp))"));
        assertFalse(appEditor.contains("navigationBarsPadding()"));
        assertTrue(appWorkspace.contains("VerticalDivider("));
        assertFalse(appWorkspace.contains("alwaysFloatInputLabels"));
        assertTrue(controls.contains("label: String"));
        assertTrue(controls.contains("maxLines = 1"));
        assertTrue(controls.contains("softWrap = false"));
        assertTrue(controls.contains("overflow = TextOverflow.Ellipsis"));
        assertTrue(appEditor.contains("rememberInstalledAppIcon"));
        assertTrue(appEditor.contains("AppConfigSheetUiTokens.ContentPadding"));
        int typefaceLibraryOpen = typefacePicker.indexOf(
                "context.startActivity(Intent(context, FontLibraryActivity::class.java))");
        assertTrue(typefaceLibraryOpen > 0);
        assertTrue(typefacePicker.substring(0, typefaceLibraryOpen).contains("onBack()"));
        assertTrue(typefacePicker.contains("fun AppTypefacePickerPage("));
        assertFalse(typefacePicker.contains("Dialog("));
        assertTrue(typefacePicker.contains("TypefacePickerUiTokens.TypefaceOptionHeight"));
        assertTrue(typefacePicker.contains("TypefaceCatalogCache.Catalog"));
        assertTrue(typefacePicker.contains("TypefaceCatalogCache.cached()"));
        assertTrue(typefacePicker.contains("withContext(Dispatchers.IO)"));
        assertTrue(typefacePicker.contains("height(typefacePageHeight())"));
        assertTrue(typefacePicker.contains("animateContentSize(tween(180))"));
        int dpisToggleStart = activity.indexOf("@Override public void toggleDpisEnabled()");
        int dpisToggleEnd = activity.indexOf("@Override public void startProcess()", dpisToggleStart);
        assertTrue(dpisToggleStart > 0);
        assertTrue(dpisToggleEnd > dpisToggleStart);
        assertTrue(activity.substring(dpisToggleStart, dpisToggleEnd).contains("requestAppsLoad();"));
    }

    @Test
    public void hookChainIsARecoverableChildPageOfBothEditorSessions() throws IOException {
        String page = read("src/main/java/com/dpis/module/ui/compose/HookChainEditorPage.kt");
        String destination = read("src/main/java/com/dpis/module/ConfigEditorDestination.java");
        String appEditor = read(
                "src/main/java/com/dpis/module/ui/compose/AppConfigEditorContent.kt");
        String appWorkspace = read(
                "src/main/java/com/dpis/module/ui/compose/AppWorkspaceContent.kt");
        String coordinator = read(
                "src/main/java/com/dpis/module/MainWorkspacePresentationCoordinator.kt");
        String viewModel = read("src/main/java/com/dpis/module/MainViewModel.java");
        String activity = read("src/main/java/com/dpis/module/MainActivity.java");
        String templates = read(
                "src/main/java/com/dpis/module/ui/compose/TemplateWorkspaceContent.kt");
        String templatePresentation = read(
                "src/main/java/com/dpis/module/templates/TemplateWorkspacePresentation.kt");

        assertTrue(page.contains("fun HookChainEditorPage("));
        assertTrue(page.contains("BackHandler(onBack = onBack)"));
        assertTrue(page.contains("RadioButton("));
        assertTrue(page.contains("SegmentedListItem("));
        assertTrue(page.contains("Switch("));
        assertFalse(page.contains("Dialog("));
        assertFalse(page.contains("Scaffold("));
        assertTrue(page.contains("fun ConfigEditorAnimatedContent("));
        assertTrue(page.contains("clipContentToAnimatedBounds"));
        assertTrue(page.contains("slideInHorizontally("));
        assertTrue(page.contains("if (animateSize)"));
        assertTrue(page.contains("Modifier.zIndex"));
        assertTrue(page.contains("targetPage == editorPage"));
        assertTrue(page.contains("clipToBounds()"));
        assertTrue(page.contains("EditorDestinationHeightDurationMillis = 180"));
        assertTrue(page.contains("HookTabHeightDurationMillis = 180"));
        assertTrue(page.contains("verticalScroll(rememberScrollState())"));
        assertTrue(destination.contains("HOOK_CHAIN_INTERFACE"));
        assertTrue(destination.contains("HOOK_CHAIN_FONT"));
        assertTrue(destination.contains("TYPEFACE"));
        assertTrue(destination.contains("isChildPage()"));
        assertTrue(appEditor.contains("state.actions.navigate("));
        assertTrue(appEditor.contains("ConfigEditorDestination.TYPEFACE"));
        assertFalse(appEditor.contains("hookChainEditorVisible"));
        assertTrue(appWorkspace.contains("ConfigEditorAnimatedContent("));
        assertTrue(appWorkspace.contains(
                "bottomPadding = padding.calculateBottomPadding()"));
        assertTrue(coordinator.contains("ConfigEditorAnimatedContent("));
        assertTrue(coordinator.contains("animateTabSize = true"));
        assertTrue(coordinator.contains("clipContentToAnimatedBounds = false"));
        assertTrue(coordinator.contains(
                "editorState.destination == ConfigEditorDestination.MAIN"));
        assertTrue(coordinator.contains("editorState.destination.isHookChain()"));
        assertTrue(coordinator.contains(
                "editorState.destination == ConfigEditorDestination.TYPEFACE"));
        String appOverlay = read(
                "src/main/java/com/dpis/module/ui/compose/AppConfigEditorOverlay.kt");
        assertTrue(appOverlay.contains("mainCollapsedAnchor"));
        assertTrue(appOverlay.contains("fun returnToMainCollapsed()"));
        assertTrue(appOverlay.contains("bottomSheetState.partialExpand()"));
        assertTrue(appOverlay.contains("bottomSheetState.currentValue == SheetValue.PartiallyExpanded"));
        assertTrue(appOverlay.contains("sheetSwipeEnabled = !sheetMotionInProgress"));
        assertTrue(appOverlay.contains("awaitPointerEvent(PointerEventPass.Initial)"));
        assertFalse(appOverlay.contains("bottomSheetState.expand()"));
        assertTrue(viewModel.contains("ConfigEditorDestination editingDestination"));
        assertTrue(activity.contains("mainViewModel.getEditingDestination()"));
        assertTrue(activity.contains("retainedState.editingDestination"));
        assertTrue(templates.contains("val editorDestination = state.editorDestination"));
        assertTrue(templatePresentation.contains(
                "val editorDestination: ConfigEditorDestination"));
        assertTrue(activity.contains("retainedState.templateEditorDestination"));
        assertTrue(activity.contains("STATE_TEMPLATE_EDITOR_DESTINATION"));
        assertTrue(templates.contains("HookChainEditorPage("));
        assertTrue(templates.contains("AppTypefacePickerPage("));
        assertTrue(templates.contains("destination = editorDestination"));
        assertTrue(templates.contains(
                "bottomSafePadding = padding.calculateBottomPadding()"));
        assertFalse(templates.contains("hookChainDialogVisible"));
        assertFalse(new File(
                "src/main/java/com/dpis/module/ui/compose/AppHookChainEditorDialog.kt").exists());
    }

    private static String read(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(new File(relativePath).toPath()),
                StandardCharsets.UTF_8
        );
    }
}
