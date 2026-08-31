package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.junit.Test

/** Guards the Theme 1 baseline while individual XML workspaces migrate later. */
class DpisComposeShellSourceSmokeTest {
    @Test
    fun composeThemeAndShellKeepTheRequiredBoundaries() {
        val theme = read("src/main/java/com/dpis/module/ui/presentation/DpisTheme.kt")
        val shell = read("src/main/java/com/dpis/module/ui/presentation/DpisWorkspaceShell.kt")
        val adapter = read("src/main/java/com/dpis/module/MainComposeWorkspaceAdapter.java")
        val mainShell = read("src/main/java/com/dpis/module/MainComposeWorkspaceShell.kt")
        val haptics = read("src/main/java/com/dpis/module/ui/presentation/DpisComposeHaptics.kt")
        val previews = read("src/main/java/com/dpis/module/ui/presentation/DpisWorkspaceShellPreviews.kt")
        val mainActivity = read("src/main/java/com/dpis/module/MainActivity.java")
        val localizedActivity = read("src/main/java/com/dpis/module/LocalizedActivity.kt")
        val presentation = read(
                "src/main/java/com/dpis/module/templates/TemplateWorkspacePresentation.kt")
        val coordinator = read(
                "src/main/java/com/dpis/module/MainWorkspacePresentationCoordinator.kt")
        val appWorkspace = read(
                "src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt")
        val wearWorkspace = read(
                "src/main/java/com/dpis/module/ui/presentation/WearWorkspaceContent.kt")

        assertTrue(theme.contains("fun DpisTheme("))
        assertTrue(theme.contains("DpisColorSchemeFactory.create("))
        assertTrue(theme.contains("system_accent1_500"))
        assertTrue(shell.contains("APP(R.string.workspace_app"))
        assertTrue(shell.contains("TEMPLATE(R.string.workspace_template"))
        assertTrue(shell.contains("HOME(R.string.workspace_home"))
        assertTrue(shell.contains("TOOLS(R.string.workspace_tools"))
        assertTrue(shell.contains("SETTINGS(R.string.workspace_settings"))
        assertTrue(shell.contains("stringResource(destination.labelRes)"))
        assertTrue(shell.contains("R.drawable.ic_apps_24"))
        assertTrue(shell.contains("R.drawable.ic_template_24"))
        assertTrue(shell.contains("R.drawable.ic_home_24"))
        assertTrue(shell.contains("R.drawable.ic_build_24"))
        assertTrue(shell.contains("R.drawable.ic_settings_24"))
        assertTrue(shell.contains("painterResource(destination.iconRes)"))
        assertFalse(shell.contains("Icons.Outlined"))
        assertTrue(shell.contains("alwaysShowLabel = false"))
        assertTrue(shell.contains("val navigationContainerColor = MaterialTheme.colorScheme.surfaceBright"))
        assertTrue(shell.contains("containerColor = navigationContainerColor"))
        assertTrue(shell.contains("background(navigationContainerColor)"))
        assertTrue(shell.contains("drawerContainerColor = navigationContainerColor"))
        assertTrue(shell.contains("windowInsets = navigationSurfaceInsets()"))
        assertTrue(shell.contains("WindowInsetsSides.Start + WindowInsetsSides.Vertical"))
        assertTrue(shell.contains("legacyWorkspaceInsetsFor(selectedDestination)"))
        assertTrue(shell.contains("legacyBottomNavigationPadding(scaffoldPadding)"))
        assertTrue(shell.contains("DpisWorkspaceDestination.SETTINGS -> PaddingValues(end = endPadding)"))
        assertTrue(shell.contains("stringResource(R.string.app_name)"))
        assertTrue(shell.contains("DpisWorkspaceDestination.HOME"))
        assertTrue(shell.contains("DpisWorkspaceDestination.TOOLS"))
        assertTrue(shell.contains("DpisWorkspaceDestination.SETTINGS"))
        assertTrue(shell.contains("BOTTOM_BAR"))
        assertTrue(shell.contains("NAVIGATION_RAIL"))
        assertTrue(shell.contains("NAVIGATION_DRAWER"))
        assertTrue(shell.contains("WorkspaceDrawerMinWindowWidth ="
                + " WorkspaceTwoPaneMinWidth + WorkspaceDrawerWidth"))
        assertTrue(shell.contains("Modifier.width(WorkspaceDrawerWidth)"))
        assertTrue(shell.contains("verticalScroll(rememberScrollState())"))
        assertTrue(shell.contains("isCompactUi -> DpisWorkspaceNavigationLayout.COMPACT_RADIAL"))
        assertTrue(shell.contains("CompactWearWorkspaceNavigation("))
        assertTrue(shell.contains("WearMaterialTheme"))
        assertTrue(shell.contains("AppScaffold"))
        assertTrue(shell.contains("ScreenScaffold(scrollState = listState)"))
        assertTrue(shell.contains("TransformingLazyColumn("))
        assertTrue(shell.contains("transformedHeight(this, transformationSpec)"))
        assertTrue(shell.contains("WearButtonDefaults.minimumVerticalListContentPadding"))
        assertTrue(shell.contains("WearCompactButton("))
        assertTrue(shell.contains("BackHandler(enabled = expanded)"))
        assertFalse(shell.contains("COMPACT_MENU_ARC_RADIUS_DP"))
        assertTrue(shell.contains("selectedDestination: DpisWorkspaceDestination"))
        assertTrue(shell.contains("onDestinationSelected: (DpisWorkspaceDestination) -> Unit"))
        assertTrue(adapter.contains("MainUiState.WorkspaceMode"))
        assertTrue(mainShell.contains("MainUiAction.workspaceModeChanged"))
        assertTrue(mainShell.contains("MainComposeWorkspaceAdapter.destinationFor(state.workspaceMode)"))
        assertTrue(previews.contains("Phone"))
        assertTrue(previews.contains("Tablet"))
        assertTrue(previews.contains("Desktop"))
        assertTrue(mainActivity.contains("installComposeWorkspaceShell()"));
        assertTrue(mainActivity.contains("new MainComposeShellHost("))
        assertFalse(read("src/main/java/com/dpis/module/MainComposeShellHost.kt")
                .contains("DpisLegacyWorkspaceHost"))
        assertTrue(mainActivity.contains("WatchUiMode.shouldUseCompactUi(this),"))
        val shellHost = read("src/main/java/com/dpis/module/MainComposeShellHost.kt")
        assertTrue(shellHost.contains("if (isCompactUi)"))
        assertTrue(shellHost.contains("workspacePresentation.renderWear(state.workspaceMode, padding)"))
        assertTrue(coordinator.contains("@Composable fun renderWear"))
        assertTrue(wearWorkspace.contains("TransformingLazyColumn("))
        assertTrue(wearWorkspace.contains("ScreenScaffold("))
        assertTrue(wearWorkspace.contains("SwitchButton("))
        assertTrue(wearWorkspace.contains("SurfaceTransformation(transformationSpec)"))
        assertTrue(wearWorkspace.contains("WearAppConfigEditorContent"))
        assertTrue(wearWorkspace.contains("WearTemplateEditorContent"))
        assertTrue(wearWorkspace.contains("WearTypefacePickerPage"))
        assertTrue(wearWorkspace.contains("WearHookChainEditorPage"))
        assertTrue(wearWorkspace.contains("BasicTextField("))
        assertFalse(wearWorkspace.contains("                AppConfigEditorContent("))
        assertFalse(wearWorkspace.contains("TemplateEditorSurface("))
        assertTrue(shell.contains("WearMaterialTheme(colorScheme = wearColors)"))
        assertTrue(shell.contains("background = phoneColors.background"))
        assertTrue(mainActivity.contains("getLastCustomNonConfigurationInstance()"))
        assertTrue(mainActivity.contains("onRetainCustomNonConfigurationInstance()"))
        assertTrue(localizedActivity.contains("import androidx.activity.ComponentActivity"))
        assertTrue(localizedActivity.contains(": ComponentActivity()"))
        assertFalse(mainActivity.contains("removeView(landDetailPane)"))
        assertTrue(shell.contains("rememberConfirmAction"))
        assertTrue(haptics.contains("HapticFeedbackType.Confirm"))
        assertTrue(presentation.contains("object TemplateWorkspacePresentation"))
        assertTrue(presentation.contains("QuickTemplateStore(context).readAll()"))
        assertTrue(presentation.contains("GlobalPrefillStore(preferences).read()"))
        assertTrue(presentation.contains("fun applyTemplate(id: String)"))
        assertTrue(presentation.contains("fun selectTargets(id: String)"))
        assertTrue(mainActivity.contains("TemplateWorkspacePresentationSource templateWorkspace()"))
        assertTrue(coordinator.contains("state = content.templateWorkspace().state()"))
        assertTrue(coordinator.contains("onQueryChanged = content.templateWorkspace()::changeQuery"))
        assertTrue(coordinator.contains("onEditorOpened ="))
        assertTrue(coordinator.contains("onEditorChanged = content.templateWorkspace()::updateEditor"))
        assertTrue(coordinator.contains("onEditorClosed = content.templateWorkspace()::closeEditor"))
        assertTrue(coordinator.contains("fun templateWorkspace(): TemplateWorkspacePresentationSource"))
        assertFalse(coordinator.contains("usesComposeTemplateWorkspace"))
        assertTrue(appWorkspace.contains("fun AppWorkspaceContent("))
        assertTrue(coordinator.contains("appRevision"))
        assertTrue(coordinator.contains("fun refreshApps()"))
        assertTrue(appWorkspace.contains("PullToRefreshBox("))
        assertTrue(appWorkspace.contains("allAppsListState"))
        assertTrue(appWorkspace.contains("configuredAppsListState"))
        assertTrue(appWorkspace.contains("actions.openApp(item)"))
        assertTrue(appWorkspace.contains("val focusManager = LocalFocusManager.current"))
        assertTrue(appWorkspace.contains("inputFocusManager = focusManager"))
        assertTrue(appWorkspace.contains(
                "configuration.orientation == Configuration.ORIENTATION_LANDSCAPE"))
        assertTrue(appWorkspace.contains(
                "val twoPane = compactVerticalChrome && maxWidth >= WorkspaceTwoPaneMinWidth"))
        assertFalse(appWorkspace.contains("state.actions::requestIcon"))
        assertTrue(appWorkspace.contains("@Preview(showBackground = true"))
        assertTrue(coordinator.contains("private fun ComposeWorkspaceSurface("))
        assertTrue(coordinator.contains("contentColor = MaterialTheme.colorScheme.onSurface"))
        assertTrue(coordinator.contains("ComposeWorkspaceSurface {"))
        assertTrue(coordinator.contains("HomeWorkspaceContent(content.homeState(), padding, pageScrollPositions)"))
        assertTrue(coordinator.contains("TemplateWorkspaceContent("))
    }

    @Test
    fun interfaceScaleSliderShowsOnlyEndpointMarkers() {
        val settings = read("src/main/java/com/dpis/module/settings/presentation/SettingsWorkspaceContent.kt")
        val theme = read("src/main/java/com/dpis/module/settings/presentation/ThemeSettingsContent.kt")
        val home = read("src/main/java/com/dpis/module/home/presentation/HomeWorkspaceContent.kt")
        val tools = read("src/main/java/com/dpis/module/tools/presentation/ToolsWorkspaceContent.kt")

        assertTrue(theme.contains("steps = 0"))
        assertTrue(theme.contains("SliderDefaults.Track"))
        assertTrue(theme.contains("latestGestureValue.floatValue"))
        assertTrue(theme.contains("AppUiScaleManager.normalizeSliderPercent(changedValue)"))
        assertTrue(theme.contains("HapticFeedbackConstants.CLOCK_TICK"))
        assertTrue(theme.contains("style = MaterialTheme.typography.titleMedium"))
        assertFalse(settings.contains("SettingsScaleRow("))
        assertTrue(settings.contains(
                "state?.languageLabel ?: stringResource(R.string.settings_language_follow_system)"))
        assertTrue(settings.contains("val systemContext = context.applicationContext"))
        assertTrue(settings.contains("it.tag == AppLocaleManager.TAG_FOLLOW_SYSTEM"))
        assertTrue(settings.contains(
                "enabled = state?.storeAvailable == true && state.cacheClearInProgress != true"))
        assertTrue(home.contains("rememberConfirmAction"))
        assertTrue(home.contains(".clip(CircleShape)"))
        assertTrue(home.contains("PageBarBehavior.Collapsing"))
        assertTrue(home.contains("collapsedTitle = {"))
        assertTrue(home.contains("stringResource(R.string.app_name)"))
        assertTrue(home.contains("contentPadding = PaddingValues("))
        assertTrue(tools.contains("rememberConfirmAction"))
        assertTrue(tools.contains("SystemFontScaleBadge(state)"))
        assertTrue(tools.contains("if (!state.canWrite && !state.unavailable)"))
        assertTrue(tools.contains("else if (state.unavailable)"))
        assertTrue(tools.contains("state.canDecrement()"))
        assertTrue(tools.contains("state.canRestore()"))
        assertTrue(tools.contains("SystemFontScaleToolState.normalizeSliderPercent(it)"))
        assertTrue(tools.contains("LocalDensity provides Density(displayDensity.density, fontScale = 1f)"))
        assertTrue(tools.contains("PageBarBehavior.Collapsing"))
        assertFalse(tools.contains("TopAppBar("))
        assertTrue(settings.contains("PageBarBehavior.Collapsing"))
        assertFalse(settings.contains("TopAppBar("))
        assertTrue(home.contains("rememberConfirmAction"))
    }

    @Test
    fun standaloneSettingsPagesUseSharedSecondaryPageChrome() {
        val scaffold = read("src/main/java/com/dpis/module/ui/presentation/PageScaffold.kt")
        val topBar = read("src/main/java/com/dpis/module/ui/presentation/PageTopBar.kt")
        val theme = read("src/main/java/com/dpis/module/settings/presentation/ThemeSettingsContent.kt")
        val experimental = read(
                "src/main/java/com/dpis/module/settings/presentation/ExperimentalSettingsContent.kt")

        assertTrue(scaffold.contains("internal fun SecondaryPageScaffold("))
        assertTrue(scaffold.contains("internal fun PrimaryPageScaffold("))
        assertTrue(scaffold.contains("contentColor = MaterialTheme.colorScheme.onSurface"))
        assertTrue(topBar.contains("internal fun CollapsingPageTopBar("))
        assertTrue(topBar.contains("TwoRowsTopAppBar("))
        assertTrue(topBar.contains("MaterialTheme.typography.dpisExpandedPageTitle"))
        assertFalse(topBar.contains("fontSize = 34.sp"))
        assertTrue(topBar.contains("internal fun InFlowPageHeader("))
        assertTrue(theme.contains("SecondaryPageScaffold("))
        assertTrue(experimental.contains("SecondaryPageScaffold("))
        assertFalse(theme.contains("TopAppBar("))
        assertFalse(experimental.contains("CenterAlignedTopAppBar("))
    }

    @Test
    fun themeSettingsExpandsStaticColorOptionsWhenDynamicColorIsDisabled() {
        val theme = read("src/main/java/com/dpis/module/settings/presentation/ThemeSettingsContent.kt")
        val support = read("src/main/java/com/dpis/module/about/presentation/SupportActivityContent.kt")
        val colors = read("src/main/java/com/dpis/module/ui/presentation/DpisTheme.kt")

        assertTrue(theme.contains("ThemeDynamicColorRow("))
        assertTrue(theme.contains("AnimatedConditionalItem(visible = !dynamicColorEnabled)"))
        assertTrue(theme.contains("R.string.settings_theme_color_label"))
        assertTrue(theme.contains("R.string.settings_theme_palette_style_label"))
        assertTrue(theme.contains("R.string.settings_theme_color_spec_label"))
        assertTrue(theme.contains("ThemeColorOption(ThemeModeStore.DEFAULT_STATIC_THEME_COLOR)"))
        assertFalse(theme.contains("ThemeColorOption(\"default\")"))
        assertTrue(theme.contains("private fun ThemeSegmentedSurfaceRow("))
        assertFalse(theme.contains("SegmentedListItem("))
        assertTrue(theme.contains("edgeColor = MaterialTheme.colorScheme.surfaceContainerHigh"))
        assertFalse(theme.contains("edgeColor = MaterialTheme.colorScheme.surfaceVariant"))
        assertTrue(support.contains("ThemeModeStore.setDynamicColorEnabled(activity, enabled)"))
        assertTrue(support.contains("dynamicColorEnabled = enabled"))
        assertTrue(support.contains("var mode by remember"))
        assertTrue(support.contains("ThemeModeStore.resolveDarkTheme(mode, isSystemInDarkTheme())"))
        assertTrue(support.contains("activity.markAppearanceAppliedInPlace()"))
        assertFalse(support.substringAfter("onModeSelected = { selectedMode ->")
            .substringBefore("onDynamicColorChanged")
            .contains("activity.recreate()"))
        assertTrue(colors.contains("ThemeModeStore.isDynamicColorEnabled(context)"))
    }

    @Test
    fun templateEditorUsesExpandedModalStateAndObservesDraftRevision() {
        val workspace = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceContent.kt")
        val editor = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateEditorContent.kt")

        assertTrue(workspace.contains(
                "TemplateEditorSurface("))
        assertTrue(workspace.contains(
                "surface = TemplateEditorSurfaceKind.PORTRAIT_SHEET"))
        assertTrue(workspace.contains("onDismissRequest = ::closeEditor"))
        assertTrue(workspace.contains("val draftRevision = editorDraft.observe()"))
        assertTrue(workspace.contains("draftRevision = draftRevision"))
        assertTrue(editor.contains("draftRevision: Int"))
        assertTrue(editor.contains("DpisEditorBottomSheet("))
        assertTrue(editor.contains("DpisSheetVisualChrome(showUnsaved = form.isDirty)"))
        assertTrue(editor.contains("contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) }"))
        assertTrue(editor.contains(
                "padding(bottom = edgeToEdgeContentBottomPadding(extraBottomPadding))"))
    }

    @Test
    fun templateWorkspaceKeepsTheLegacySearchAndHeaderActionSemantics() {
        val template = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceList.kt")
        val tokens = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateUiTokens.kt")
        val search = read(
                "src/main/java/com/dpis/module/ui/presentation/WorkspaceSearchCard.kt")

        assertTrue(template.contains("WorkspaceSearchCard("))
        assertTrue(template.contains("hintRes = R.string.template_search_hint"))
        assertTrue(template.contains("onQueryChanged = onQueryChanged"))
        assertTrue(template.contains("rememberRestorableLazyListState("))
        assertTrue(search.contains("cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)"))
        assertTrue(search.contains("R.drawable.ic_close_24"))
        assertTrue(template.contains("role = Role.Button"))
        assertTrue(template.contains("TemplateUiTokens.DisabledActionAlpha"))
        assertTrue(template.contains("TemplateUiTokens.EmptySummaryTopGap"))
        assertTrue(template.contains("TemplateUiTokens.CardActionVisualSize"))
        assertTrue(template.contains("TemplateApplyAction("))
        assertTrue(template.contains("R.drawable.ic_done_all_24"))
        assertTrue(template.contains("R.string.template_workspace_action_apply"))
        assertTrue(template.contains("TemplateActionButtonStyle.Plain"))
        assertTrue(template.contains("TemplateActionButtonStyle.Primary"))
        assertFalse(template.contains("SearchCardBorderWidth"))
        assertTrue(tokens.contains("val WorkspaceTopPadding = 14.dp"))
        assertTrue(tokens.contains("val SectionTitleInset = 12.dp"))
        assertTrue(tokens.contains("val SectionActionInset = 12.dp"))
        assertTrue(tokens.contains("val HeaderActionVisualSize = 36.dp"))
        assertTrue(tokens.contains("val CardActionVisualSize = 28.dp"))
        assertTrue(tokens.contains("val ApplyActionVisualSize = CardActionVisualSize"))
        assertTrue(tokens.contains("const val DisabledActionAlpha = 0.45f"))
    }

    @Test
    fun templateEditorKeepsErrorsOutsideTheFixedInputOutline() {
        val editor = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateEditorContent.kt")
        val controls = read(
                "src/main/java/com/dpis/module/ui/presentation/DpisEditorControls.kt")

        assertTrue(editor.contains("val viewportError = if (form.isViewportInputValid())"))
        assertTrue(editor.contains("isError = viewportError != null"))
        assertTrue(editor.contains("isError = fontError != null"))
        assertTrue(editor.contains("TemplateEditorErrorMessage(it)"))
        assertTrue(editor.contains("modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 8.dp)"))
        assertTrue(controls.contains("rememberEditorControlHeight()"))
        assertTrue(controls.contains("coerceAtLeast(AppConfigSheetUiTokens.ActionHeight)"))
        assertTrue(controls.contains("color = MaterialTheme.colorScheme.onSurface"))
        assertTrue(controls.contains("cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)"))
        assertTrue(editor.contains("val hookDomainsButtonText = FontHookDomainPresentation"))
        assertTrue(editor.contains("forAutomaticDomainsRaw(form.fontHookDomainsRaw)"))
        assertTrue(editor.contains(".buttonText(context)"))
        assertFalse(editor.contains("dialog_font_hook_domains_title_with_count, 1, 1"))
    }

    @Test
    fun composeAppEditorRestoreDoesNotOpenLegacySheet() {
        val activity = read("src/main/java/com/dpis/module/MainActivity.java")
        val restoreStart = activity.indexOf(
                "private void restoreAppEditorForCurrentWorkspace()")
        val restoreEnd = activity.indexOf(
                "private void applyLandscapeDetailVisibility", restoreStart)
        val restore = activity.substring(restoreStart, restoreEnd)

        val composeGuard = restore.indexOf("if (composeShellHost != null)")
        val legacySheet = restore.indexOf("showEditBottomSheet(appItem)")
        assertTrue(composeGuard >= 0)
        assertTrue(legacySheet > composeGuard)
        assertTrue(restore.substring(composeGuard, legacySheet).contains("return"));
    }

    @Test
    fun composeAppSheetPreservesPartialExpandAndLegacyChromeSemantics() {
        val sheet = read("src/main/java/com/dpis/module/ui/presentation/DpisEditorBottomSheet.kt")
        val coordinator = read(
                "src/main/java/com/dpis/module/MainWorkspacePresentationCoordinator.kt")
        val overlay = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorOverlay.kt")
        val appEditor = read(
                "src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorContent.kt")
        val appWorkspace = read(
                "src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt")
        val controls = read(
                "src/main/java/com/dpis/module/ui/presentation/DpisEditorControls.kt")
        val catalog = read(
                "src/main/java/com/dpis/module/applist/InstalledAppCatalogCoordinator.kt")
        val typefacePicker = read(
                "src/main/java/com/dpis/module/appconfig/presentation/AppTypefacePickerPage.kt")
        val activity = read("src/main/java/com/dpis/module/MainActivity.java")
        val editorController = read(
                "src/main/java/com/dpis/module/appconfig/editor/EditorActions.kt")
        val editorSessionController = read(
                "src/main/java/com/dpis/module/appconfig/editor/ComposeAppEditorController.kt")

        assertTrue(sheet.contains("skipPartiallyExpanded: Boolean = true"))
        assertTrue(sheet.contains("rememberBottomSheetState("))
        assertTrue(sheet.contains("SheetValue.PartiallyExpanded"))
        assertTrue(coordinator.contains("AppConfigSheetWizardStore.shouldShowAdvancedHint(context)"))
        assertTrue(coordinator.contains("AppConfigEditorOverlay("))
        assertTrue(coordinator.contains("RenderAppEditorOverlay(mode: MainUiState.WorkspaceMode, wear: Boolean = false)"))
        assertTrue(coordinator.contains("WearAppConfigEditorContent(it)"))
        assertTrue(coordinator.contains("appRevision"))
        assertTrue(coordinator.contains("if (mode != MainUiState.WorkspaceMode.APP) return"))
        assertTrue(coordinator.contains(
                "configuration.orientation == Configuration.ORIENTATION_LANDSCAPE"))
        assertTrue(coordinator.contains("if (twoPane) return@BoxWithConstraints"))
        assertTrue(overlay.contains("initialValue = SheetValue.Hidden"))
        assertTrue(overlay.contains("skipHiddenState = false"))
        assertTrue(overlay.contains("bottomSheetState.partialExpand()"))
        assertTrue(overlay.contains("targetValue == SheetValue.Hidden"))
        assertTrue(overlay.contains("app-config-sheet-scrim"))
        assertTrue(overlay.contains("LaunchedEffect(advancedAnchor)"))
        assertTrue(overlay.contains("value == SheetValue.Hidden"))
        assertTrue(overlay.contains("sheetDragHandle = null"))
        assertTrue(overlay.contains("sheetPeekHeight = measuredPeekHeight"))
        assertTrue(overlay.contains("returnToMainPending = false"))
        assertTrue(overlay.contains("wasChild && !isChild"))
        assertTrue(overlay.contains("advancedAnchor"))
        assertTrue(overlay.contains("maxHeight * 0.75f"))
        assertTrue(coordinator.contains("R.string.dialog_advanced_wizard_hint"))
        assertTrue(coordinator.contains("R.string.feedback_diagnostic_action"))
        assertTrue(coordinator.contains("AppConfigSheetUiTokens.TopChromeIndicatorWidth"))
        assertTrue(coordinator.contains("if (editorState.dirty)"))
        assertTrue(coordinator.contains("R.string.sheet_unsaved_badge"))
        assertTrue(coordinator.contains("showInlineUnsavedBadge = false"))
        assertTrue(appEditor.contains("showInlineUnsavedBadge: Boolean = true"))
        assertTrue(appEditor.contains("coordinates.positionInParent().y.toDp()"))
        assertTrue(appEditor.contains(
                "padding(bottom = edgeToEdgeContentBottomPadding(0.dp))"))
        assertFalse(appEditor.contains("navigationBarsPadding()"))
        assertTrue(appWorkspace.contains("VerticalDivider("))
        assertFalse(appWorkspace.contains("alwaysFloatInputLabels"))
        assertTrue(controls.contains("label: String"))
        assertTrue(controls.contains("maxLines = 1"))
        assertTrue(controls.contains("softWrap = false"))
        assertTrue(controls.contains("overflow = TextOverflow.Ellipsis"))
        assertTrue(appEditor.contains("rememberInstalledAppIcon"))
        assertTrue(appEditor.contains("AppConfigSheetUiTokens.ContentPadding"))
        assertTrue(appEditor.contains("val focusManager = LocalFocusManager.current"))
        assertTrue(appEditor.contains("focusManager.clearFocus(force = true)"))
        assertTrue(appEditor.contains(".clearTextInputFocusOutside(focusManager, inputFocusBoundary)"))
        assertTrue(appEditor.contains(".reportTextInputFocusBounds(inputFocusBoundary, \"viewport\")"))
        assertTrue(appEditor.contains(".reportTextInputFocusBounds(inputFocusBoundary, \"font\")"))
        val typefaceLibraryOpen = typefacePicker.indexOf(
                "context.startActivity(Intent(context, FontLibraryActivity::class.java))")
        assertTrue(typefaceLibraryOpen > 0)
        assertTrue(typefacePicker.substring(0, typefaceLibraryOpen).contains("onBack()"))
        assertTrue(typefacePicker.contains("fun AppTypefacePickerPage("))
        assertFalse(typefacePicker.contains("Dialog("))
        assertTrue(typefacePicker.contains("TypefacePickerUiTokens.TypefaceOptionHeight"))
        assertTrue(typefacePicker.contains("TypefaceCatalogCache.Catalog"))
        assertTrue(typefacePicker.contains("TypefaceCatalogCache.cached()"))
        assertTrue(typefacePicker.contains("withContext(Dispatchers.IO)"))
        assertTrue(typefacePicker.contains(".fillMaxSize()"))
        assertTrue(typefacePicker.contains("dialogListContentFade("))
        assertTrue(typefacePicker.contains("EditorSheetChildPageHeader("))
        assertTrue(typefacePicker.contains("containerColor = MaterialTheme.colorScheme.surfaceContainer"))
        assertTrue(typefacePicker.contains("MaterialTheme.colorScheme.surfaceBright"))
        assertTrue(sheet.contains("internal fun EditorSheetChildPageHeader("))
        assertTrue(sheet.contains("ChildPageHeaderHeight"))
        assertTrue(editorSessionController.contains("EditorActions.create("))
        assertTrue(activity.contains("requestAppsLoad()"));
        assertTrue(editorController.contains("host.setDpisEnabled(enabled)"))
        assertTrue(editorController.contains("draft.withDpisEnabled(enabled)"))
    }

    @Test
    fun hookChainIsARecoverableChildPageOfBothEditorSessions() {
        val page = read("src/main/java/com/dpis/module/fonts/presentation/HookChainEditorPage.kt")
        val destination = read("src/main/java/com/dpis/module/ConfigEditorDestination.java")
        val appEditor = read(
                "src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorContent.kt")
        val appWorkspace = read(
                "src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt")
        val coordinator = read(
                "src/main/java/com/dpis/module/MainWorkspacePresentationCoordinator.kt")
        val viewModel = read("src/main/java/com/dpis/module/MainViewModel.kt")
        val activity = read("src/main/java/com/dpis/module/MainActivity.java")
        val templates = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceContent.kt")
        val templatePresentation = read(
                "src/main/java/com/dpis/module/templates/TemplateWorkspacePresentation.kt")

        assertTrue(page.contains("fun HookChainEditorPage("))
        assertTrue(page.contains("BackHandler(onBack = onBack)"))
        assertTrue(page.contains("RadioButton("))
        assertTrue(page.contains("SegmentedListItem("))
        assertTrue(page.contains("Switch("))
        assertFalse(page.contains("Dialog("))
        assertFalse(page.contains("Scaffold("))
        assertTrue(page.contains("fun ConfigEditorAnimatedContent("))
        assertTrue(page.contains("clipContentToAnimatedBounds"))
        assertTrue(page.contains("slideInHorizontally("))
        assertTrue(page.contains("if (animateSize)"))
        assertTrue(page.contains("Modifier.zIndex"))
        assertTrue(page.contains("targetPage == editorPage"))
        assertTrue(page.contains("clipToBounds()"))
        assertTrue(page.contains("EditorDestinationHeightDurationMillis = 180"))
        assertTrue(page.contains(".fillMaxHeight()"))
        assertTrue(page.contains("EditorSheetChildPageHeader("))
        assertTrue(page.contains("containerColor = MaterialTheme.colorScheme.surfaceContainer"))
        assertTrue(page.contains("MaterialTheme.colorScheme.surfaceBright"))
        assertTrue(page.contains("verticalScroll(rememberScrollState())"))
        assertTrue(destination.contains("HOOK_CHAIN_INTERFACE"))
        assertTrue(destination.contains("HOOK_CHAIN_FONT"))
        assertTrue(destination.contains("TYPEFACE"))
        assertTrue(destination.contains("isChildPage()"))
        assertTrue(appEditor.contains("state.actions.navigate("))
        assertTrue(appEditor.contains("ConfigEditorDestination.TYPEFACE"))
        assertFalse(appEditor.contains("hookChainEditorVisible"))
        assertTrue(appWorkspace.contains("ConfigEditorAnimatedContent("))
        assertTrue(appWorkspace.contains(
                "bottomPadding = padding.calculateBottomPadding()"))
        assertTrue(coordinator.contains("ConfigEditorAnimatedContent("))
        assertTrue(coordinator.contains("animateSize = false"))
        assertTrue(coordinator.contains("clipContentToAnimatedBounds = false"))
        assertTrue(coordinator.contains(
                "editorState.destination == ConfigEditorDestination.MAIN"))
        val appOverlay = read(
                "src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorOverlay.kt")
        assertTrue(appOverlay.contains("mainCollapsedAnchor"))
        assertTrue(appOverlay.contains("fun returnToMainCollapsed()"))
        assertTrue(appOverlay.contains("bottomSheetState.partialExpand()"))
        assertTrue(appOverlay.contains("bottomSheetState.currentValue == SheetValue.PartiallyExpanded"))
        assertTrue(appOverlay.contains("sheetSwipeEnabled = !sheetMotionInProgress"))
        assertTrue(appOverlay.contains("awaitPointerEvent(PointerEventPass.Initial)"))
        assertFalse(appOverlay.contains("bottomSheetState.expand()"))
        assertTrue(viewModel.contains("var editingDestination: ConfigEditorDestination"))
        assertTrue(activity.contains("mainViewModel.getEditingDestination()"))
        assertTrue(activity.contains("retainedState.editingDestination"))
        assertTrue(templates.contains("val editorDestination = state.editorDestination"))
        assertTrue(templatePresentation.contains(
                "val editorDestination: ConfigEditorDestination"))
        assertTrue(activity.contains("retainedState.workspaceSessionState"))
        assertTrue(templatePresentation.contains("val editorDestination: ConfigEditorDestination"))
        assertTrue(activity.contains("ensureWorkspaceSession().saveState(outState)"))
        assertTrue(templates.contains("HookChainEditorPage("))
        assertTrue(templates.contains("AppTypefacePickerPage("))
        assertTrue(templates.contains("destination = editorDestination"))
        assertTrue(templates.contains(
                "bottomSafePadding = padding.calculateBottomPadding()"))
        assertFalse(templates.contains("hookChainDialogVisible"))
        assertFalse(File(
                "src/main/java/com/dpis/module/ui/compose/AppHookChainEditorDialog.kt").exists())
    }

    @Test
    fun wechatEditorRowUsesTheSameVerticalRhythmAsValueModeRows() {
        val editor = read(
                "src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorContent.kt")

        val wechatStart = editor.indexOf("if (state.showsWechatDpi())")
        val wechatEnd = editor.indexOf("if (!state.wechatDpiInputValid)", wechatStart)
        assertTrue(wechatStart > 0)
        assertTrue(wechatEnd > wechatStart)

        val wechatRow = editor.substring(wechatStart, wechatEnd)
        assertTrue(wechatRow.contains(
                ".height(AppConfigSheetUiTokens.FieldTopInset + rememberEditorControlHeight())"))
        assertTrue(wechatRow.contains("verticalAlignment = Alignment.Bottom"))
        assertTrue(wechatRow.contains("state.actions.showWechatDpiHelp()"))
    }

    @Test
    fun searchPagesDismissInputWhenTheirNonInputContentIsTouched() {
        val controls = read(
                "src/main/java/com/dpis/module/ui/presentation/DpisEditorControls.kt")
        val apps = read(
                "src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt")
        val targets = read(
                "src/main/java/com/dpis/module/templates/presentation/QuickTemplateTargetsContent.kt")
        val templates = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceList.kt")

        assertTrue(controls.contains("fun Modifier.clearTextInputFocusOnPointerDown("))
        assertTrue(controls.contains("focusManager.clearFocus(force = true)"))
        assertTrue(controls.contains("fun Modifier.clearTextInputFocusOutside("))
        assertTrue(apps.contains(".clearTextInputFocusOnPointerDown(inputFocusManager)"))
        assertTrue(targets.contains(".clearTextInputFocusOnPointerDown(focusManager)"))
        assertTrue(templates.contains(".clearTextInputFocusOnPointerDown(focusManager)"))
    }

    private fun read(relativePath: String): String {
        return String(
                Files.readAllBytes(File(relativePath).toPath()),
                StandardCharsets.UTF_8
        )
    }
}
