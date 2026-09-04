package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.junit.Test

/** Guards the stable Compose shell and workspace routing boundaries. */
class ComposeShellSourceSmokeTest {
    @Test
    fun composeThemeAndShellKeepTheRequiredBoundaries() {
        val theme = read("src/main/java/com/dpis/module/ui/presentation/design/ComposeDesignSystem.kt")
        val shell = read("src/main/java/com/dpis/module/ui/presentation/workspace/WorkspaceShell.kt")
        val adapter = read("src/main/java/com/dpis/module/MainComposeWorkspaceAdapter.java")
        val mainShell = read("src/main/java/com/dpis/module/MainComposeWorkspaceShell.kt")
        val mainActivity = read("src/main/java/com/dpis/module/MainActivity.java")
        val coordinator = read("src/main/java/com/dpis/module/MainWorkspacePresentationCoordinator.kt")

        // Keep this smoke test limited to stable ownership and routing contracts. Detailed
        // behavior belongs in executable tests, so internal layout names are intentionally not
        // asserted here.
        assertTrue(theme.contains("fun ComposeDesignSystem("))
        assertTrue(theme.contains("ColorSchemeFactory.create("))
        assertTrue(shell.contains("WorkspaceDestination"))
        assertTrue(shell.contains("onDestinationSelected"))
        assertTrue(adapter.contains("MainUiState.WorkspaceMode"))
        assertTrue(mainShell.contains("MainUiAction.workspaceModeChanged"))
        assertTrue(mainShell.contains("MainComposeWorkspaceAdapter.destinationFor(state.workspaceMode)"))
        assertTrue(mainActivity.contains("installComposeWorkspaceShell()"))
        assertTrue(coordinator.contains("ComposeWorkspaceSurface"))
        assertTrue(coordinator.contains("TemplateWorkspaceContent"))
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
        assertTrue(home.contains("rememberClickAction"))
        assertTrue(home.contains(".clip(CircleShape)"))
        assertTrue(home.contains("PageBarBehavior.Collapsing"))
        assertTrue(home.contains("collapsedTitle = {"))
        assertTrue(home.contains("stringResource(R.string.app_name)"))
        assertTrue(home.contains("contentPadding = PaddingValues("))
        assertTrue(tools.contains("rememberClickAction"))
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
        assertTrue(home.contains("rememberClickAction"))
    }

    @Test
    fun standaloneSettingsPagesUseSharedSecondaryPageChrome() {
        val scaffold = read("src/main/java/com/dpis/module/ui/presentation/workspace/PageScaffold.kt")
        val topBar = read("src/main/java/com/dpis/module/ui/presentation/workspace/PageTopBar.kt")
        val theme = read("src/main/java/com/dpis/module/settings/presentation/ThemeSettingsContent.kt")
        val experimental = read(
                "src/main/java/com/dpis/module/settings/presentation/ExperimentalSettingsContent.kt")

        assertTrue(scaffold.contains("internal fun SecondaryPageScaffold("))
        assertTrue(scaffold.contains("internal fun PrimaryPageScaffold("))
        assertTrue(scaffold.contains("contentColor = MaterialTheme.colorScheme.onSurface"))
        assertTrue(topBar.contains("internal fun CollapsingPageTopBar("))
        assertTrue(topBar.contains("TwoRowsTopAppBar("))
        assertTrue(topBar.contains("MaterialTheme.typography.expandedPageTitle"))
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
        val colors = read("src/main/java/com/dpis/module/ui/presentation/design/ComposeDesignSystem.kt")

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
        val templateSheet = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateEditorSheet.kt")

        assertTrue(workspace.contains(
                "TemplateEditorSurface("))
        assertFalse(workspace.contains("TemplateEditorSurfaceKind"))
        assertTrue(workspace.contains("val draftRevision = editorDraft.observe()"))
        assertTrue(workspace.contains("draftRevision = draftRevision"))
        assertTrue(editor.contains("draftRevision: Int"))
        assertTrue(editor.contains("remember(draftRevision) { Unit }"))
        assertFalse(editor.contains("AppConfigEditorOverlay("))
        assertFalse(editor.contains("SheetVisualChrome("))
        assertTrue(editor.contains("destination = destination"))
        assertTrue(editor.contains("animateSize = animateDestinationSize"))
        assertTrue(editor.contains("edgeToEdgeContentBottomPadding("))
        assertTrue(workspace.contains("TemplateEditorSheet("))
        assertFalse(workspace.contains("rememberStandardBottomSheetState("))
        assertFalse(editor.contains("BottomSheetScaffold("))
        assertTrue(templateSheet.contains("rememberStandardBottomSheetState("))
        assertTrue(templateSheet.contains("EditorSheetScaffoldFrame("))
        assertTrue(templateSheet.contains("bottomSheetState.partialExpand()"))
        assertTrue(templateSheet.contains(
                "destination.isChildPage() && !sheetMotionInProgress"))
        assertFalse(workspace.substringAfter("val editorSheetBody")
                .substringBefore("Box(\n        modifier = Modifier")
                .contains("topSafePadding"))
        assertFalse(templateSheet.contains("AppConfigEditorOverlay("))
        assertFalse(templateSheet.contains("EditorBottomSheet("))
    }

    @Test
    fun appEditorTreatsGlobalPrefillAsAnUnsavedConfigurationDraft() {
        val shell = read("src/main/java/com/dpis/module/MainWorkspacePresentationCoordinator.kt")
        val item = read("src/main/java/com/dpis/module/applist/AppListItem.java")
        val editor = read("src/main/java/com/dpis/module/appconfig/editor/ComposeAppEditorController.kt")

        assertTrue(shell.contains("editorState.dirty || editorState.item.previewFromGlobalPrefill"))
        assertTrue(item.contains("appSpecificConfigActive,"))
        assertTrue(editor.contains("val editorItem = host.resolveEditorItem(item.packageName) ?: item"))
        assertTrue(editor.contains("if (editorItem.previewFromGlobalPrefill)"))
        assertTrue(editor.contains("EditorDraft.fromItem(editorItem)"))
    }

    @Test
    fun templateWorkspaceKeepsTheLegacySearchAndHeaderActionSemantics() {
        val template = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceList.kt")
        val tokens = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateUiTokens.kt")
        val search = read(
                "src/main/java/com/dpis/module/ui/presentation/workspace/WorkspaceSearchCard.kt")

        assertTrue(template.contains("WorkspaceSearchCard("))
        assertTrue(template.contains("hintRes = R.string.template_search_hint"))
        assertTrue(template.contains("onQueryChanged = onQueryChanged"))
        assertTrue(template.contains("rememberRestorableLazyListState("))
        assertTrue(search.contains("cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)"))
        assertTrue(search.contains("R.drawable.ic_close_24"))
        assertTrue(template.contains("role = Role.Button"))
        assertTrue(template.contains("TemplateUiTokens.DISABLED_ACTION_ALPHA"))
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
        assertTrue(tokens.contains("const val DISABLED_ACTION_ALPHA = 0.45f"))
    }

    @Test
    fun templateEditorKeepsValidationFromChangingItsMeasuredHeight() {
        val editor = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateEditorContent.kt")
        val appEditor = read(
                "src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorContent.kt")
        val controls = read(
                "src/main/java/com/dpis/module/ui/presentation/editor/EditorControls.kt")

        assertTrue(editor.contains("val viewportHasError = !form.isViewportInputValid()"))
        assertTrue(editor.contains("isError = viewportHasError"))
        assertTrue(editor.contains("isError = fontHasError"))
        assertFalse(editor.contains("TemplateEditorErrorMessage"))
        assertFalse(appEditor.contains("EditorInputError"))
        assertTrue(controls.contains("rememberEditorControlHeight()"))
        assertTrue(controls.contains("coerceAtLeast(AppConfigSheetUiTokens.ActionHeight)"))
        assertTrue(controls.contains("color = MaterialTheme.colorScheme.onSurface"))
        assertTrue(controls.contains("cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)"))
        assertTrue(controls.contains(".inputFocusFeedback(onFocused)"))
        val haptics = read("src/main/java/com/dpis/module/ui/presentation/design/ComposeHaptics.kt")
        assertTrue(haptics.contains("fun Modifier.inputFocusFeedback("))
        assertTrue(haptics.contains("focusState.isFocused && !wasFocused"))
        assertTrue(haptics.contains("confirmFocus()"))
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
        val sheet = read("src/main/java/com/dpis/module/ui/presentation/editor/EditorBottomSheet.kt")
        val coordinator = read(
                "src/main/java/com/dpis/module/MainWorkspacePresentationCoordinator.kt")
        val overlay = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorOverlay.kt")
        val sheetFrame = read(
                "src/main/java/com/dpis/module/ui/presentation/editor/EditorSheetScaffoldFrame.kt")
        val appEditor = read(
                "src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorContent.kt")
        val appWorkspace = read(
                "src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt")
        val controls = read(
                "src/main/java/com/dpis/module/ui/presentation/editor/EditorControls.kt")
        val catalog = read(
                "src/main/java/com/dpis/module/applist/InstalledAppCatalogCoordinator.kt")
        val typefacePicker = read(
                "src/main/java/com/dpis/module/appconfig/presentation/AppTypefacePickerPage.kt")
        val activity = read("src/main/java/com/dpis/module/MainActivity.java")
        val editorController = read(
                "src/main/java/com/dpis/module/appconfig/editor/EditorActions.kt")
        val editorSessionController = read(
                "src/main/java/com/dpis/module/appconfig/editor/ComposeAppEditorController.kt")

        assertTrue(sheet.contains("ModalBottomSheet("))
        assertTrue(sheet.contains("openPartiallyExpanded: Boolean = false"))
        assertTrue(overlay.contains("fun AppConfigEditorOverlay("))
        assertTrue(coordinator.contains("AppConfigSheetWizardStore.shouldShowAdvancedHint(context)"))
        assertTrue(coordinator.contains("AppConfigEditorOverlay("))
        assertTrue(coordinator.contains("RenderAppEditorOverlay(mode: MainUiState.WorkspaceMode, wear: Boolean = false)"))
        assertTrue(coordinator.contains("WearAppConfigEditorContent(it)"))
        assertTrue(coordinator.contains("appRevision"))
        assertTrue(coordinator.contains("if (mode != MainUiState.WorkspaceMode.APP) return"))
        assertTrue(coordinator.contains(
                "configuration.orientation == Configuration.ORIENTATION_LANDSCAPE"))
        assertTrue(coordinator.contains("if (twoPane) return@BoxWithConstraints"))
        assertTrue(overlay.contains("rememberBottomSheetScaffoldState("))
        assertTrue(overlay.contains("EditorSheetScaffoldFrame("))
        assertTrue(overlay.contains("bottomSheetState.hide()"))
        assertTrue(coordinator.contains("R.string.dialog_advanced_wizard_hint"))
        assertTrue(coordinator.contains("R.string.feedback_diagnostic_action"))
        assertTrue(coordinator.contains("AppConfigSheetUiTokens.TopChromeIndicatorWidth"))
        assertTrue(coordinator.contains(
                "editorState.dirty || editorState.item.previewFromGlobalPrefill"))
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
        assertTrue(sheetFrame.contains(
                ".clearTextInputFocusOutside(focusManager, inputFocusBoundary)"))
        assertTrue(sheetFrame.contains("sheetSwipeEnabled = sheetSwipeEnabled"))
        assertFalse(appEditor.contains(".clearTextInputFocusOutside(focusManager, inputFocusBoundary)"))
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
        assertTrue(page.contains("FeedbackSwitch("))
        assertTrue(page.contains("val selectDestination = rememberClickValueAction"))
        assertTrue(page.contains("val selectMode = rememberClickValueAction(onModeSelected)"))
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
        assertTrue(appOverlay.contains("onReturnToMain"))
        assertTrue(appOverlay.contains("bottomSheetState.partialExpand()"))
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
        val wechatEnd = editor.indexOf(
                "Spacer(Modifier.height(AppConfigSheetUiTokens.ControlGroupGap))", wechatStart)
        assertTrue(wechatStart > 0)
        assertTrue(wechatEnd > wechatStart)

        val wechatRow = editor.substring(wechatStart, wechatEnd)
        assertTrue(wechatRow.contains(
                ".height(AppConfigSheetUiTokens.FieldTopInset + rememberEditorControlHeight())"))
        assertTrue(wechatRow.contains("verticalAlignment = Alignment.Bottom"))
        assertTrue(wechatRow.contains("state.actions.showWechatDpiHelp()"))
        assertFalse(wechatRow.contains("EditorInputError"))
    }

    @Test
    fun searchPagesDismissInputWhenTheirNonInputContentIsTouched() {
        val controls = read(
                "src/main/java/com/dpis/module/ui/presentation/editor/EditorControls.kt")
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
        assertTrue(targets.contains(".clearTextInputFocusOutside(focusManager, inputFocusBoundary)"))
        assertTrue(targets.contains("target-search"))
        assertTrue(templates.contains(".clearTextInputFocusOnPointerDown(focusManager)"))
    }

    private fun read(relativePath: String): String {
        return String(
                Files.readAllBytes(File(relativePath).toPath()),
                StandardCharsets.UTF_8
        )
    }
}
