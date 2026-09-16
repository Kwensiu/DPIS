package com.dpis.module.shell

import com.dpis.module.SourceSmokeTestPaths
import com.dpis.module.ui.DialogWindowSizer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dpis.module.ui.presentation.MainComposeShellHost
import com.dpis.module.ui.presentation.MainWorkspacePresentationCoordinator
import com.dpis.module.appconfig.presentation.ComposeAppEditorActivityGateway

import com.dpis.module.updates.presentation.UpdateAvailableDialog
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.ui.MainUiAction
import com.dpis.module.ui.MainUiState
import com.dpis.module.ui.MainViewModel
import com.dpis.module.settings.SettingsUiState
import com.dpis.module.appconfig.editor.ComposeEditorScopeRequestCoordinator

class MainActivitySourceSmokeTest {

    @Test
    fun templateWorkspaceImplementationLivesBehindTheActivitySession() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val coordinator = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceCoordinator.kt")
        val session = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceActivitySession.kt")

        val startup = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )
        assertTrue(startup.contains("var workspaceSession: TemplateWorkspaceActivitySession?"))
        assertTrue(startup.contains(".handleActivityResult(requestCode, data)"))
        assertTrue(startup.contains(".saveState(outState)"))
        assertTrue(startup.contains(".onDestroy()"))
        assertFalse(source.contains("TemplateWorkspaceBinder"))
        assertFalse(source.contains("TemplateDetailPaneController"))
        assertFalse(source.contains("QuickTemplateTargetsBinder"))
        assertFalse(source.contains("QuickTemplateTargetSelectionActivity"))
        assertFalse(source.contains("TemplateEditorForm"))
        assertFalse(source.contains("QuickTemplateStore"))
        assertFalse(source.contains("REQUEST_QUICK_TEMPLATE_TARGETS"))
        assertFalse(coordinator.contains("fun attachLegacyViews("))
        assertTrue(coordinator.contains("fun presentationSource("))
        assertTrue(coordinator.contains("fun handleActivityResult("))
        assertFalse(session.contains("fun attachLegacyViews("))
        assertTrue(session.contains("fun saveState("))
    }

    @Test
    fun homeStatusReflectsUpdateCheckProgressWhilePromptOwnsUpdateActions() {
        val activity = read("src/main/java/com/dpis/module/MainActivity.kt")
        val updateSession = read(
                "src/main/java/com/dpis/module/updates/presentation/MainUpdateSession.kt"
        )
        val homeState = read("src/main/java/com/dpis/module/home/HomeUpdateUiState.java")
        val composeHome = read(
                "src/main/java/com/dpis/module/home/presentation/HomeWorkspaceContent.kt"
        )

        assertTrue(activity.contains("MainUpdateSession(this) {"))
        assertTrue(activity.contains("startupSession.bindHomeWorkspaceIfVisible()"))
        assertTrue(updateSession.contains("private val updateStateStore by lazy"))
        assertTrue(updateSession.contains("private val releaseNotesController by lazy"))
        assertTrue(updateSession.contains("private val promptCoordinator by lazy"))
        assertTrue(updateSession.contains("applyHomeUpdateState(HomeUpdateUiState.CHECKING)"))
        assertTrue(updateSession.contains("applyHomeUpdateState(HomeUpdateUiState.available(manifest))"))
        assertTrue(updateSession.contains("showUpdateAvailableDialog("))
        assertTrue(updateSession.contains("applyHomeUpdateState(HomeUpdateUiState.UP_TO_DATE)"))
        assertTrue(updateSession.contains("applyHomeUpdateState(HomeUpdateUiState.FAILED)"))
        assertTrue(homeState.contains("CHECKING,"))
        assertTrue(homeState.contains("AVAILABLE -> context.getString"))
        assertTrue(composeHome.contains("state.updateState.subtitle(context)"))
    }

    @Test
    fun composeOwnsMainWorkspaceSearchAndNavigationControls() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val composeWorkspace = read(
                "src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt")

        assertFalse(source.contains("searchFocusFab = findViewById"))
        assertFalse(source.contains("workspaceSwitch.setOnItemSelectedListener"))
        assertFalse(source.contains("searchFilterButton.setOnClickListener"))
        assertTrue(composeWorkspace.contains("WorkspaceSearchCard("))
        assertTrue(composeWorkspace.contains("AppFilterSheet("))
    }

    @Test
    fun formInputFocusCanMoveFocusToFallbackView() {
        val source = read("src/main/java/com/dpis/module/ui/FormInputFocusBinder.java")

        assertTrue(source.contains("fallbackFocusView.setFocusable(true)"))
        assertTrue(source.contains("fallbackFocusView.setFocusableInTouchMode(true)"))
        assertTrue(source.contains("fallbackFocusView.requestFocus()"))
        assertTrue(source.contains("hideSoftInputFromWindow("))
    }

    @Test
    fun composeSaveRequestsScopeAfterSuccessfulSave() {
        val coordinator = read(
            "src/main/java/com/dpis/module/appconfig/editor/ComposeEditorScopeRequestCoordinator.kt",
        )
        val effects = read(
            "src/main/java/com/dpis/module/appconfig/presentation/MainWorkspaceEditorPostSaveEffects.kt",
        )

        assertTrue(effects.contains("scopeCoordinator.requestAfterSuccessfulSave(item)"))
        assertTrue(coordinator.contains("fun requestAfterSuccessfulSave("))
        assertTrue(coordinator.contains("scopeRequester.requestScope("))
    }

    @Test
    fun composeSavePromotesApprovedScopeIntoCurrentEditorDraft() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val coordinator = read(
                "src/main/java/com/dpis/module/appconfig/editor/ComposeEditorScopeRequestCoordinator.kt"
        )
        val effects = read(
                "src/main/java/com/dpis/module/appconfig/presentation/MainWorkspaceEditorPostSaveEffects.kt"
        )

        val hostWiring = read(
            "src/main/java/com/dpis/module/ui/presentation/MainHostWiringSession.kt"
        )
        assertTrue(hostWiring.contains("ComposeEditorScopeRequestCoordinator("))
        assertTrue(hostWiring.contains("ComposeAppEditorActivityGateway("))
        assertTrue(hostWiring.contains("MainWorkspaceEditorPostSaveEffects("))
        assertTrue(effects.contains("scopeCoordinator.requestAfterSuccessfulSave(item)"))
        assertTrue(coordinator.contains("mainViewModel.markEditingScopeSelected(packageName)"))
    }

    @Test
    fun composeWorkspaceOwnsFilterEntry() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val composeWorkspace = read(
                "src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt")

        assertFalse(source.contains("focusSearchInputAndShowKeyboard()"))
        assertFalse(source.contains("hideSearchFocusFab()"))
        assertFalse(source.contains("showSearchFocusFab()"))
        val hostWiring = read(
            "src/main/java/com/dpis/module/ui/presentation/MainHostWiringSession.kt"
        )
        assertTrue(hostWiring.contains("MainUiAction.filterChanged(filterState)"))
        assertTrue(composeWorkspace.contains("AppFilterSheet("))
    }

    @Test
    fun composeShellOwnsWorkspaceSelection() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val workspace = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt"
        )
        val startup = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )

        assertTrue(startup.contains("STATE_WORKSPACE_MODE"))
        assertTrue(startup.contains("MainUiState.WorkspaceMode.fromName("))
        assertFalse(source.contains("bindWorkspaceSwitch()"))
        assertFalse(source.contains("workspaceSwitch.setOnItemSelectedListener"))
        assertFalse(source.contains("private boolean updatingWorkspaceSelection"))
        val homeSession = read(
            "src/main/java/com/dpis/module/home/presentation/HomeWorkspaceSession.kt"
        )
        assertTrue(homeSession.contains("MainUiAction.workspaceModeChanged("))
        assertTrue(workspace.contains("applyWorkspaceMode(state.workspaceMode)"))
        assertTrue(workspace.contains("MainUiState.WorkspaceMode.TEMPLATE"))
        assertFalse(source.contains("private void updateWatchFilterTabsScrollOffset(int dy)"))
        assertFalse(workspace.contains("setVisible(hostWiring.toolsWorkspaceContainer, toolsWorkspace)"))
        assertFalse(source.contains("setSearchFocusFabVisible("))
        val hostWiring = read(
            "src/main/java/com/dpis/module/ui/presentation/MainHostWiringSession.kt"
        )
        assertFalse(hostWiring.contains("attachLegacyViews("))
        assertFalse(source.contains("TemplateWorkspaceBinder"))
        assertFalse(source.contains("GlobalPrefillActionsAdapter"))
        assertFalse(source.contains("QuickTemplateActionsAdapter"))
        assertTrue(workspace.contains("fun bindWorkspaceSession()"))
        assertTrue(workspace.contains("activity.startupSession.ensureWorkspaceSession().present("))
        assertTrue(startup.contains("STATE_TEMPLATE_QUERY"))
        assertFalse(source.contains("searchFilterButton.setEnabled(appWorkspace)"))
        assertFalse(source.contains("applySearchClearButtonPosition(appWorkspace)"))
        assertFalse(source.contains("workspaceModeForButtonId(int checkedId)"))
        assertTrue(workspace.contains("MainComposeShellHost("))
    }

    @Test
    fun settingsPresentationUsesOneWorkspaceCapability() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val coordinator = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspacePresentationCoordinator.kt"
        )
        val actions = read("src/main/java/com/dpis/module/settings/SettingsActions.kt")

        val workspace = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt"
        )
        assertTrue(workspace.contains("override fun settings()"))
        assertFalse(source.contains("public SettingsUiState settingsState()"))
        assertFalse(source.contains("public void setSettingsHooks(boolean enabled)"))
        assertFalse(source.contains("public void openSettingsBackup()"))
        assertTrue(coordinator.contains("fun settings(): SettingsActions"))
        assertTrue(actions.contains("interface SettingsActions"))
    }

    @Test
    fun appAndToolsWorkspacesOwnPresentationActionBlocks() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val toolsWorkspace = read("src/main/java/com/dpis/module/tools/presentation/ToolsWorkspace.kt")

        val workspace = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt"
        )
        val hostWiring = read(
            "src/main/java/com/dpis/module/ui/presentation/MainHostWiringSession.kt"
        )
        assertTrue(hostWiring.contains("var appWorkspaceActions: AppWorkspacePresentation.Actions?"))
        assertTrue(hostWiring.contains("object : AppWorkspacePresentation.Actions"))
        assertTrue(workspace.contains("checkNotNull(hostWiring.appWorkspaceActions)"))
        assertFalse(source.contains("createComposeAppWorkspaceActions()"))
        assertTrue(toolsWorkspace.contains("class ToolsWorkspace("))
        assertTrue(toolsWorkspace.contains("SystemFontScaleToolPresenter("))
        assertFalse(toolsWorkspace.contains("ToolsWorkspaceBinder"))
    }

    @Test
    fun restoreSnapshot_isNotBlockedBySavedStateBranch() {
        val source = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )

        val restoreSnapshotLine = source.indexOf(
            "appsSnapshot = ArrayList(retained.appsSnapshot)"
        )
        assertTrue(restoreSnapshotLine > 0)
        val beforeRestoreSnapshot = source.substring(0, restoreSnapshotLine)

        assertTrue(
            beforeRestoreSnapshot.contains("if (retained != null) {")
        )
        assertFalse(
            beforeRestoreSnapshot.contains("else if (retained != null)")
        )
        val afterRestoreSnapshot = source.substring(restoreSnapshotLine)
        assertTrue(afterRestoreSnapshot.contains("if (savedInstanceState != null)"))
        assertFalse(afterRestoreSnapshot.contains("else if (savedInstanceState != null)"))
    }

    @Test
    fun composeTemplateWorkspaceKeepsTargetSelectionFallbackOnly() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val hostWiring = read(
            "src/main/java/com/dpis/module/ui/presentation/MainHostWiringSession.kt"
        )
        assertFalse(hostWiring.contains("var landDetailPane: View?"))
        val templateCoordinator = read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceCoordinator.kt")
        val workspace = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt"
        )
        val composeContent = read(
            "src/main/java/com/dpis/module/templates/presentation/QuickTemplateTargetsContent.kt"
        )
        assertFalse(source.contains("templateDetailContent"))
        assertFalse(source.contains("TemplateDetailPaneController"))
        assertTrue(workspace.contains("restoreForConfiguration("))
        assertFalse(templateCoordinator.contains("fun attachLegacyViews("))
        assertFalse(templateCoordinator.contains("TemplateDetailPaneController("))
        assertTrue(templateCoordinator.contains("startPortraitTargetSelection("))
        assertFalse(source.contains("GlobalPrefillEditorBinder"))
        assertFalse(source.contains("QuickTemplateEditorBinder"))
        assertFalse(source.contains("GlobalPrefillSheetDialog"))
        assertFalse(source.contains("QuickTemplateEditSheetDialog"))
        assertTrue(templateCoordinator.contains("TemplateDetailKind.QUICK_TEMPLATE_TARGETS"))
        assertFalse(templateCoordinator.contains("legacyDetailController"))
        assertFalse(source.contains("? R.layout.dialog_global_prefill_sheet"))
        assertFalse(source.contains(": R.layout.dialog_quick_template_edit_sheet"))
        assertTrue(composeContent.contains("SecondaryPageTopBar("))
        assertTrue(composeContent.contains("SplitPaneHeader("))
    }

    @Test
    fun appEditorRestoreIsScopedToAppWorkspace() {
        val startup = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )
        val workspace = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt"
        )

        assertTrue(startup.contains("restoreAppEditorForCurrentWorkspace()"))
        assertTrue(workspace.contains("fun restoreAppEditorForCurrentWorkspace()"))
        assertTrue(
            workspace.contains(
                "activity.startupSession.requireUiState().workspaceMode != MainUiState.WorkspaceMode.APP",
            )
        )
        assertTrue(workspace.contains("composeShellHost?.refreshApps()"))
        assertFalse(workspace.contains("activity.sheetSession.show(appItem)"))
        assertFalse(workspace.contains("activity.landDetailSession.show(appItem)"))
    }

    @Test
    fun landscapeWorkspaceRailUsesCompactMaterialItemHeightAndScrollsWhenNeeded() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val shell = read(
            "src/main/java/com/dpis/module/ui/presentation/workspace/WorkspaceShell.kt"
        )
        val dimensions = read("src/main/res/values/dimens.xml")
        val roundDimensions = read("src/main/res/values-round/dimens.xml")

        assertTrue(shell.contains("NAVIGATION_RAIL"))
        assertFalse(source.contains("bindLandscapeWorkspaceRailItemHeight()"))
        assertFalse(source.contains("workspaceSwitch instanceof NavigationRailView"))
        assertTrue(dimensions.contains("main_land_workspace_rail_item_min_height\">64dp"))
        assertTrue(roundDimensions.contains("main_land_workspace_rail_item_min_height\">56dp"))
        assertFalse(source.contains("NavigationRailMenuView"))
    }

    @Test
    fun templateEditorDraftMigratesBetweenSheetAndPane() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val draft = read("src/main/java/com/dpis/module/templates/TemplateEditorDraft.kt")
        val workspace = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateWorkspacePresentation.kt")
        val coordinator = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceCoordinator.kt")

        val startup = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )
        assertTrue(startup.contains("val workspaceSessionState: TemplateWorkspaceActivitySession.State?"))
        assertTrue(startup.contains("workspaceSessionState = retained.workspaceSessionState"))
        assertTrue(coordinator.contains("routeState.globalPrefillDraft()"))
        assertTrue(coordinator.contains("routeState.quickTemplateDraft()"))
        val startupLaunch = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )
        assertTrue(startupLaunch.contains("ensureWorkspaceSession().restore(savedInstanceState)"))
        assertTrue(startupLaunch.contains("TemplateWorkspaceActivitySession("))
        assertTrue(draft.contains("viewportScaleInput"))
        assertTrue(draft.contains("viewportAbsoluteInput"))
        assertTrue(workspace.contains("globalPrefillDraft: TemplateEditorDraft?"))
        assertTrue(workspace.contains("quickTemplateDraft: TemplateEditorDraft?"))
        assertFalse(source.contains("GlobalPrefillEditorBinder"))
        assertFalse(source.contains("QuickTemplateEditorBinder"))
    }

    @Test
    fun loadInstalledApps_publishesRowsBeforeIcons() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val coordinatorSource = read(
            "src/main/java/com/dpis/module/applist/InstalledAppCatalogCoordinator.kt"
        )
        val iconSource = read("src/main/java/com/dpis/module/applist/presentation/InstalledAppIcon.kt")
        val workspaceSource = read("src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt")

        val runtimeLaunch = read(
            "src/main/java/com/dpis/module/runtime/presentation/RuntimeLaunchSession.kt"
        )
        val loadSession = read(
            "src/main/java/com/dpis/module/applist/presentation/InstalledAppsLoadSession.kt"
        )
        assertTrue(
            loadSession.contains("catalogCoordinator.loadInstalledApps(")
        )
        assertTrue(coordinatorSource.contains("item.hyperOsNativeProxyCandidate,"))
        assertTrue(coordinatorSource.contains("ApplicationInfoFlags.of(0L)"))
        assertTrue(coordinatorSource.contains("getInstalledApplications(0)"))
        assertFalse(coordinatorSource.contains("GET_META_DATA"))
        assertTrue(
            read("src/main/java/com/dpis/module/runtime/hyperos/HyperOsNativeProxyFacade.kt")
                .contains("HyperOsNativeAppDetector.isNativeProxyCandidate("),
        )
        assertTrue(iconSource.contains("InstalledAppIconCache.loadBitmap"))
        assertTrue(workspaceSource.contains("rememberInstalledAppIconBitmap("))
        assertTrue(workspaceSource.contains("PrefetchVisibleAppIcons("))
        assertFalse(workspaceSource.contains("AndroidView("))
        assertTrue(workspaceSource.contains("if (icon == null)"))
        assertTrue(workspaceSource.contains("surfaceContainerHighest"))
        assertFalse(workspaceSource.contains("preloadIcons("))
        assertFalse(coordinatorSource.contains("getDefaultActivityIcon()"))
    }

    @Test
    fun appLoad_requestsXiaomiInstalledAppsPermissionBeforeQueryingPackages() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val loadSession = read(
            "src/main/java/com/dpis/module/applist/presentation/InstalledAppsLoadSession.kt"
        )

        assertTrue(loadSession.contains("XIAOMI_GET_INSTALLED_APPS_PERMISSION"))
        assertTrue(
            loadSession.contains("com.android.permission.GET_INSTALLED_APPS")
        )
        assertTrue(loadSession.contains("activity.requestPermissions("))
        assertTrue(loadSession.contains("REQUEST_XIAOMI_GET_INSTALLED_APPS"))
        assertTrue(source.contains("onRequestPermissionsResult("))
        val startupPermissions = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )
        assertTrue(
            startupPermissions.contains(
                "installedAppsLoadSession.onRequestPermissionsResult(requestCode)",
            )
        )
        assertTrue(loadSession.contains("permissionRequestCompleted"))
        assertTrue(
            loadSession.contains("isXiaomiPermissionDeclared()")
        )
        assertTrue(loadSession.contains("getPermissionInfo("))
        assertTrue(
            loadSession.contains("dispatchInstalledAppsLoad(true)")
        )
        val requestLoadStart = loadSession.indexOf(
            "fun requestLoad(forceInstalledAppCatalogReload: Boolean)"
        )
        val requestLoadEnd = loadSession.indexOf(
            "fun onRequestPermissionsResult(requestCode: Int)",
            requestLoadStart
        )
        assertTrue(requestLoadStart >= 0)
        assertTrue(requestLoadEnd > requestLoadStart)
        val requestLoadBody = loadSession.substring(
            requestLoadStart,
            requestLoadEnd
        )
        assertTrue(
            compact(requestLoadBody).indexOf(
                "ensurePermissionBeforeLoad()"
            ) <
                compact(requestLoadBody).indexOf(
                    "dispatchInstalledAppsLoad("
                )
        )
    }

    @Test
    fun savesAndRestoresPageScrollStatesForRotation() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val compose = read(
                "src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt")

        val startup = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )
        assertTrue(startup.contains("scrollStateStore.snapshot()"))
        assertTrue(startup.contains("scrollStateStore.restore("))
        assertFalse(source.contains("STATE_APP_LIST_SCROLL_POSITIONS"))
        assertFalse(source.contains("putIntArray(\n                STATE_APP_LIST_SCROLL_POSITIONS"))
        assertTrue(compose.contains("PersistAppListScrollPosition("))
        assertTrue(compose.contains("snapshotFlow"))
        assertTrue(compose.contains("latestActions.updateScrollPosition(page, index, offset)"))
    }

    @Test
    fun appWorkspaceSupportsTabClicksAndHorizontalPageSwipes() {
        val compose = read(
                "src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt")

        assertTrue(compose.contains("rememberPagerState("))
        assertTrue(compose.contains("HorizontalPager("))
        assertTrue(compose.contains("pagerState.animateScrollToPage(page.position())"))
        assertTrue(compose.contains("snapshotFlow { pagerState.settledPage }"))
        assertTrue(compose.contains(".drop(1)"))
        assertTrue(compose.contains("latestActions.changePage(page)"))
        assertTrue(compose.contains("pageItems = state.itemsFor(page)"))
    }

    @Test
    fun startupDisclaimerUsesMaterialDialogAndPersistsConsent() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val launch = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )
        val runtimeLayout = read(
            "src/main/java/com/dpis/module/tools/presentation/LocalToolDialogs.kt"
        )
        val strings = read("src/main/res/values/strings.xml")
        val zhStrings = read("src/main/res/values-zh-rCN/strings.xml")

        assertTrue(launch.contains("fun maybeShowModuleRuntimeReloadAdvice(): Boolean"))
        assertTrue(launch.contains("ModuleRuntimeReloadNoticeCoordinator(activity)"))
        assertTrue(launch.contains("maybeShow { continueStartupDialogs() }"))
        assertTrue(runtimeLayout.contains("DialogWindowSizer.applyStandardWidth(dialog, activity)"))
        assertFalse(source.contains("ModuleRuntimeReloader.softReloadAsync("))
        assertFalse(source.contains("module_runtime_reload_now_button"))
        assertFalse(source.contains("module_runtime_reload_later_button"))
        assertTrue(runtimeLayout.contains("R.drawable.ic_error_outline_24"))
        assertTrue(runtimeLayout.contains("module_runtime_reload_title"))
        assertTrue(runtimeLayout.contains("module_runtime_reload_message"))
        assertTrue(runtimeLayout.contains("module_runtime_reload_ack_button"))
        assertTrue(runtimeLayout.contains("R.dimen.dialog_status_icon_padding"))
        assertTrue(
            runtimeLayout.contains("R.dimen.dialog_surface_padding_horizontal")
        )
        assertTrue(runtimeLayout.contains("R.dimen.dialog_body_spacing"))
        assertTrue(runtimeLayout.contains("R.dimen.dialog_action_spacing_top"))
        val runtimeMessage = stringEntry(
            strings,
            "module_runtime_reload_message"
        )
        val zhRuntimeTitle = stringEntry(
            zhStrings,
            "module_runtime_reload_title"
        )
        val zhRuntimeMessage = stringEntry(
            zhStrings,
            "module_runtime_reload_message"
        )
        assertTrue(zhRuntimeTitle.contains("建议重启设备"))
        assertTrue(
            zhRuntimeMessage.contains("部分修改可能需要重启设备后才能完全生效")
        )
        assertFalse(runtimeMessage.contains("HyperOS"))
        assertFalse(runtimeMessage.contains("Rust"))
        assertFalse(zhRuntimeMessage.contains("HyperOS"))
        assertFalse(zhRuntimeMessage.contains("Rust"))
        assertTrue(launch.contains("updateSession.maybeShowStartupDisclaimerDialog()"))
        assertTrue(
            launch.contains("if (!updateSession.maybeShowStartupDisclaimerDialog()) {")
        )
        val updateSession = read(
            "src/main/java/com/dpis/module/updates/presentation/MainUpdateSession.kt"
        )
        assertTrue(
            updateSession.contains("fun maybeShowStartupDisclaimerDialog(): Boolean")
        )
        assertTrue(
            updateSession.contains(
                "object : UpdatePromptDialogCoordinator.StartupDisclaimerAcceptance"
            )
        )
        assertTrue(updateSession.contains("startupDisclaimerStore(activity)"))
        assertTrue(updateSession.contains("store.isAccepted"))
        assertTrue(updateSession.contains("store.setAccepted(true)"))
        assertTrue(updateSession.contains("fun applyLargeDialogWidth("))
        assertTrue(
            updateSession.contains("DialogWindowSizer.applyLargeWidth(dialog, activity)")
        )
        val disclaimerBlock = updateSession.substring(
            updateSession.indexOf("fun maybeShowStartupDisclaimerDialog(): Boolean"),
            updateSession.indexOf("fun maybeCheckForUpdatesOnStartup()")
        )
        assertFalse(disclaimerBlock.contains("DpisConfigStore"))
    }

    @Test
    fun dialogWindowSizerUsesResponsivePresetConstraints() {
        val source = read(
            "src/main/java/com/dpis/module/ui/DialogWindowSizer.java"
        )
        val dimens = read("src/main/res/values/dimens.xml")
        val integers = read("src/main/res/values/integers.xml")

        assertTrue(
            source.contains(
                "applyCompactWidth(AlertDialog dialog, Context context)"
            )
        )
        assertTrue(
            source.contains(
                "applyStandardWidth(AlertDialog dialog, Context context)"
            )
        )
        assertTrue(
            source.contains(
                "applyLargeWidth(AlertDialog dialog, Context context)"
            )
        )
        assertTrue(source.contains("dialog_window_margin_horizontal"))
        assertTrue(source.contains("resolvePreset(context, preset)"))
        assertTrue(
            source.contains("R.integer.dialog_window_large_min_width_dp")
        )
        assertTrue(source.contains("? Preset.STANDARD"))
        assertTrue(source.contains("calculateWindowWidth(screenWidth"))
        assertTrue(source.contains("screenWidth - horizontalMargin * 2"))
        assertTrue(
            source.contains(
                "COMPACT(R.dimen.dialog_window_compact_max_width, 0.88f)"
            )
        )
        assertTrue(
            source.contains(
                "STANDARD(R.dimen.dialog_window_standard_max_width, 0.90f)"
            )
        )
        assertTrue(
            source.contains(
                "LARGE(R.dimen.dialog_window_large_max_width, 0.92f)"
            )
        )
        assertTrue(dimens.contains("dialog_window_margin_horizontal\">16dp"))
        assertTrue(dimens.contains("dialog_window_compact_max_width\">360dp"))
        assertTrue(dimens.contains("dialog_window_standard_max_width\">420dp"))
        assertTrue(dimens.contains("dialog_window_large_max_width\">560dp"))
        assertTrue(integers.contains("dialog_window_large_min_width_dp\">600"))
    }

    @Test
    fun dialogWindowSizerTreatsHorizontalMarginAsPerSideInset() {
        assertTrue(
            DialogWindowSizer.calculateWindowWidth(360, 16, 420, 0.90f) == 324
        )
        assertTrue(
            DialogWindowSizer.calculateWindowWidth(1000, 16, 560, 0.92f) == 560
        )
        assertTrue(
            DialogWindowSizer.calculateWindowWidth(24, 16, 420, 0.90f) == 0
        )
    }

    @Test
    fun homeStatusCardRetainsManualUpdateCheckEntry() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val homeSession = read(
            "src/main/java/com/dpis/module/home/presentation/HomeWorkspaceSession.kt"
        )
        val homeState = read(
            "src/main/java/com/dpis/module/home/HomeWorkspaceState.kt"
        )
        val compose = read(
            "src/main/java/com/dpis/module/home/presentation/HomeWorkspaceContent.kt"
        )

        assertTrue(homeSession.contains("checkForUpdatesNow()"))
        assertTrue(homeState.contains("interface HomeWorkspaceActions"))
        val primaryStatus = compose
            .substringAfter("private fun HomePrimaryStatus")
            .substringBefore("private fun HomeCountCard")
        assertTrue(primaryStatus.contains("state.updateState.subtitle(context)"))
        assertFalse(primaryStatus.contains("if (!disabled) {\n                    Text("))
        assertTrue(compose.contains("state.actions.checkForUpdates()"))
    }

    @Test
    fun startupDisclaimerComposeKeepsMandatoryAcceptContract() {
        val source = read("src/main/java/com/dpis/module/ui/dialog/StartupDisclaimerDialog.kt")
        val dimensions = read("src/main/res/values/dimens.xml")

        assertTrue(source.contains("fun StartupDisclaimerDialog("))
        assertTrue(source.contains("R.string.startup_disclaimer_title"))
        assertTrue(source.contains("R.string.startup_disclaimer_message"))
        assertTrue(source.contains("R.string.startup_disclaimer_checkbox_text"))
        assertTrue(source.contains("R.string.startup_disclaimer_accept_button"))
        assertTrue(source.contains("dismissOnBackPress = false"))
        assertTrue(source.contains("dismissOnClickOutside = false"))
        assertTrue(source.contains("R.dimen.dialog_surface_padding_horizontal"))
        assertFalse(source.contains("startup_disclaimer_exit_button"))
        assertTrue(dimensions.contains("dialog_round_surface_padding_horizontal"))
        assertTrue(dimensions.contains("dialog_round_action_spacing_top"))
    }

    @Test
    fun composeEditorUsesScrollableSheetContent() {
        val overlay = read(
            "src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorOverlay.kt",
        )
        val content = read(
            "src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorContent.kt",
        )

        assertTrue(overlay.contains("fun AppConfigEditorOverlay("))
        assertTrue(content.contains("fun AppConfigEditorContent("))
    }

    @Test
    fun pageRefresh_forcesInstalledAppCatalogReload() {
        val source = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )

        val refreshStart = source.indexOf(
            "fun onPageRefreshRequested(page: AppListPage?) {"
        )
        val refreshEnd = source.indexOf(
            "fun setCurrentAppListPage(",
            refreshStart
        )
        assertTrue(refreshStart >= 0)
        assertTrue(refreshEnd > refreshStart)

        val refreshBody = source.substring(refreshStart, refreshEnd)
        assertTrue(refreshBody.contains("installedAppsLoadSession.requestLoad(true)"))
    }

    @Test
    fun appLoad_reusesInstalledAppCatalogBetweenRefreshes() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val viewModelSource = read(
            "src/main/java/com/dpis/module/ui/MainViewModel.kt"
        )
        val coordinatorSource = read(
            "src/main/java/com/dpis/module/applist/InstalledAppCatalogCoordinator.kt"
        )

        val loadSession = read(
            "src/main/java/com/dpis/module/applist/presentation/InstalledAppsLoadSession.kt"
        )
        assertTrue(loadSession.contains("INSTALLED_APP_CATALOG_TTL_MS"))
        assertTrue(loadSession.contains("InstalledAppCatalogCoordinator("))
        assertTrue(coordinatorSource.contains("getInstalledAppCatalog("))
        assertTrue(
            viewModelSource.contains("forceInstalledAppCatalogReloadRequested")
        )
        assertTrue(coordinatorSource.contains("isCatalogCacheFresh"))
    }

    @Test
    fun retainedAppListSkipsImmediateServiceReloadOnRotation() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")

        val startup = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )
        assertTrue(startup.contains("var skipNextImmediateServiceReload = false"))
        assertTrue(
            startup.contains(
                "skipNextImmediateServiceReload = appsSnapshot.isNotEmpty()"
            )
        )
        assertTrue(
            startup.contains(
                "DpisApplication.addServiceStateListener(activity, true)"
            )
        )
        assertTrue(startup.contains("fun consumeSkipNextImmediateServiceReload()"))
    }

    @Test
    fun composeEditorOwnsAppConfigOpenAndSave() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val hostWiring = read(
            "src/main/java/com/dpis/module/ui/presentation/MainHostWiringSession.kt",
        )
        val gateway = read(
            "src/main/java/com/dpis/module/appconfig/presentation/ComposeAppEditorActivityGateway.kt",
        )
        val overlay = read(
            "src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorOverlay.kt",
        )

        assertTrue(hostWiring.contains("composeAppEditorController?.open(item)"))
        assertTrue(hostWiring.contains("ComposeAppEditorController(viewModel, gateway, saveWorkflow)"))
        assertTrue(hostWiring.contains("AppConfigEditorPersister("))
        assertTrue(hostWiring.contains("gateway::systemHooksEnabled"))
        assertTrue(hostWiring.contains("activity::hookConfigStore"))
        assertTrue(hostWiring.contains("MainWorkspaceEditorPostSaveEffects(activity, scopeCoordinator)"))
        assertFalse(hostWiring.contains("setSaveWorkflow"))
        assertFalse(gateway.contains("setSaveWorkflow"))
        assertFalse(gateway.contains("fun save(item: AppListItem, draft: EditorDraft)"))
        assertFalse(gateway.contains("syncHyperOsNativeProxy"))
        assertTrue(gateway.contains("AppConfigPrefillPreview.resolveForEditor(activity, item, store)"))
        assertTrue(overlay.contains("fun AppConfigEditorOverlay("))
        assertFalse(source.contains("internal val dialogHost"))
        assertFalse(source.contains("AppConfigDialogActivityHost("))
        assertFalse(source.contains("AppConfigSheetSession("))
        assertFalse(source.contains("LandAppDetailSession("))
        assertFalse(source.contains("EditorDraftSession("))
        assertTrue(
            read(
                "src/main/java/com/dpis/module/runtime/presentation/RuntimeLaunchSession.kt"
            ).contains(
                "fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean"
            )
        )
        val startup = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )
        assertTrue(startup.contains("val editingDraft = draft ?: viewModel?.editingDraft"))
        assertFalse(source.contains("private void bindDialogValidation("))
        assertFalse(source.contains("private void bindDialogActions("))
        assertFalse(source.contains("private void refreshDialogState("))
    }

    @Test
    fun showEditDialog_doesNotRefreshListRowsBeforeOpeningDetail() {
        val workspace = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt",
        )
        val methodStart = workspace.indexOf(
            "fun restoreAppEditorForCurrentWorkspace()"
        )
        val methodEnd = workspace.indexOf("fun bindForLifecycle(", methodStart)
        assertTrue(methodStart >= 0)
        assertTrue(methodEnd > methodStart)

        val methodBody = workspace.substring(methodStart, methodEnd)
        assertFalse(methodBody.contains("refreshVisibleStatuses"))
    }

    @Test
    fun landscapeStatusLayout_usesFlatDetailPane() {
        val workspace = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt"
        )
        assertTrue(workspace.contains("ComposeView(activity)"))
        assertFalse(workspace.contains("applyLandscapeDetailVisibility("))
    }

    @Test
    fun composeEditorOwnsLandscapeAndPortraitAppConfig() {
        val overlay = read(
            "src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorOverlay.kt",
        )
        val content = read(
            "src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt",
        )
        assertTrue(overlay.contains("fun AppConfigEditorOverlay("))
        assertTrue(content.contains("editorState"))
    }

    @Test
    fun appConfigAndProcessActions_delegateToDedicatedHandlers() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val runtimeLaunch = read(
            "src/main/java/com/dpis/module/runtime/presentation/RuntimeLaunchSession.kt"
        )
        assertTrue(source.contains("RuntimeLaunchSession("))
        assertTrue(source.contains("requestAppsLoad = { startupSession.requestAppsLoad() }"))
        assertTrue(runtimeLaunch.contains("ProcessActionHandler("))
        assertTrue(runtimeLaunch.contains("syncRuntimePropertiesForTargetLaunch(packageName)"))
        assertTrue(
            runtimeLaunch.contains(
                "ProcessActionConfirm(activity) { composeShell() }",
            )
        )
        assertTrue(source.contains("AppConfigSaveHandler()"))
        assertTrue(
            runtimeLaunch.contains("processActionHandler.execute(item, mappedAction)")
        )
        assertTrue(
            read("src/main/java/com/dpis/module/appconfig/editor/AppConfigEditorPersister.kt")
                .contains("saveHandler.saveResolved(")
        )
        assertTrue(
            runtimeLaunch.contains(
                "FontRuntimePropertySyncer.clearTargetAsync(packageName)"
            )
        )
        assertFalse(
            source.contains("private void runProcessAction(String packageName")
        )
        assertFalse(
            source.contains("private int[] saveAppConfig(AppListItem item")
        )
    }

    @Test
    fun installedCatalog_defersIconsUntilRowsAreVisible() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val coordinatorSource = read(
            "src/main/java/com/dpis/module/applist/InstalledAppCatalogCoordinator.kt"
        )

        val loadSession = read(
            "src/main/java/com/dpis/module/applist/presentation/InstalledAppsLoadSession.kt"
        )
        assertTrue(loadSession.contains("loadInstalledApps(forceInstalledAppCatalogReload)"))
        assertTrue(coordinatorSource.contains("val catalog = loadInstalledAppCatalog("))
        assertTrue(coordinatorSource.contains("applicationInfo.loadIcon(packageManager)"))
        assertFalse(coordinatorSource.contains("icon = loadApplicationIcon(packageManager, applicationInfo)"))
        assertFalse(coordinatorSource.contains("maybeScheduleFirstScreenIconWarmup("))
        assertFalse(coordinatorSource.contains("ExecutorService"))
        assertFalse(coordinatorSource.contains("onIconsLoaded("))
        assertFalse(coordinatorSource.contains("getDefaultActivityIcon()"))
    }

    @Test
    fun systemScopeAndHookStatus_delegateToCoordinator() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")

        assertTrue(
            source.contains("SystemScopeCoordinator(")
        )
        val gateway = read(
            "src/main/java/com/dpis/module/appconfig/presentation/ComposeAppEditorActivityGateway.kt",
        )
        assertTrue(gateway.contains("activity.systemScopeCoordinator.toggleScope("))
        val startup = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )
        assertTrue(
            startup.contains(
                "SystemScopeCoordinator.resolveSystemHookEffectiveEnabled("
            )
        )
        assertFalse(source.contains("openLsposedModuleSettings()"))
        assertFalse(
            source.contains(
                "de.robv.android.xposed.intent.action.MODULE_SETTINGS"
            )
        )
        assertFalse(
            source.contains("private void toggleScope(String packageName")
        )
    }

    @Test
    fun touchFeedbackBinderProvidesSharedHapticAndScaleBehavior() {
        val source = read(
            "src/main/java/com/dpis/module/ui/TouchFeedbackBinder.java"
        )

        assertTrue(source.contains("public final class TouchFeedbackBinder"))
        assertTrue(source.contains("bindPressScaleAndHaptic(View view)"))
        assertTrue(
            source.contains(
                "performHapticFeedback(resolvePressHapticConstant())"
            )
        )
        assertTrue(source.contains("HapticFeedbackConstants.CONFIRM"))
        assertTrue(source.contains("HapticFeedbackConstants.VIRTUAL_KEY"))
    }

    @Test
    fun applicationSyncsHyperOsNativeFontTargetsOnStartup() {
        val source = read(
            "src/main/java/com/dpis/module/DpisApplication.kt"
        )

        assertTrue(
            source.contains(
                "RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore)"
            )
        )
        assertTrue(
            source.contains(
                "RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(refreshedStore)"
            )
        )
        assertFalse(
            source.contains(
                "HyperOsNativeProxyRefreshCoordinator.refreshConfiguredTargetsAsync(this, configStore)"
            )
        )
        assertFalse(
            source.contains(
                "HyperOsNativeProxyRefreshCoordinator.refreshConfiguredTargetsAsync(this, runtimeDeliveryStore)"
            )
        )
    }

    @Test
    fun appReceivesPackageReplacementWithoutAutoMountingHyperOsNativeProxy() {
        val manifest = read("src/main/AndroidManifest.xml")
        val receiver = read(
            "src/main/java/com/dpis/module/runtime/lifecycle/DpisPackageLifecycleReceiver.java"
        )

        assertTrue(manifest.contains(".runtime.lifecycle.DpisPackageLifecycleReceiver"))
        assertTrue(
            manifest.contains("android.intent.action.MY_PACKAGE_REPLACED")
        )
        assertTrue(receiver.contains("Intent.ACTION_MY_PACKAGE_REPLACED"))
        assertTrue(
            receiver.contains(
                "HyperOsNativeProxyAssetExporter.exportBundledNativeProxyLibrary(context, DpisLog::e)"
            )
        )
        assertFalse(
            receiver.contains(
                "HyperOsNativeProxyRefreshCoordinator.refreshConfiguredTargetsAsync(context, store)"
            )
        )
        assertTrue(
            receiver.contains(
                "RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(store)"
            )
        )
    }

    @Test
    fun appConfigHostWiresFontHookDomainEditor() {
        val source = read("src/main/java/com/dpis/module/MainActivity.kt")
        val host = read(
            "src/main/java/com/dpis/module/appconfig/presentation/ComposeAppEditorActivityGateway.kt"
        )

        val templateHost = read(
            "src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceActivityHost.kt"
        )
        assertFalse(templateHost.contains("activity.dialogHost"))
        assertTrue(host.contains("fun hookChainText("))
        assertFalse(host.contains("fun getFontHookDomainsButtonText("))
        assertFalse(host.contains("fun fontHookDomainsButtonText("))
        assertFalse(host.contains("fun showFontHookDomains("))
        assertTrue(host.contains("resolveFontHookDomainsForDraft(item, draft)"))
        assertTrue(host.contains("HookDomainOverrideStore(activity.hookConfigStore).read("))
        assertFalse(host.contains("FontHookDomainDialog.show("))
        val saveSource = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.kt")
        assertFalse(
            saveSource.contains("FontRuntimePropertySyncer.publishTargetAsync(")
        )
        val runtimeLaunch = read(
            "src/main/java/com/dpis/module/runtime/presentation/RuntimeLaunchSession.kt"
        )
        assertTrue(runtimeLaunch.contains("scheduleRuntimePropertiesForTargetLaunch(packageName)"))
        assertTrue(runtimeLaunch.contains("FontRuntimePropertySyncer.syncTarget(packageName, store)"))
        assertTrue(host.contains("FontHookDomainRegistry.automaticCustomizableDomains()"))
        assertFalse(source.contains("AppProcessHookInstaller.resolveDebugFontOverrideForPackage("))
        assertTrue(host.contains("FontHookDomainPresentation.forOverride("))
        assertTrue(host.contains(".buttonText(activity)"))
        assertFalse(source.contains("item.fontScalePercent != null && item.fontScalePercent > 0"))
        assertFalse(source.contains("publishFontRuntimeTarget("))
    }

    @Test
    fun fontHookDomainEditorUsesDraftStateOnly() {
        val source = read(
            "src/main/java/com/dpis/module/appconfig/presentation/ComposeAppEditorActivityGateway.kt"
        )
        val hookPage = read("src/main/java/com/dpis/module/fonts/presentation/HookChainEditorPage.kt")
        val methodStart = source.indexOf("private fun resolveFontHookDomainsForDraft(")
        val method = source.substring(methodStart)

        assertTrue(method.contains("draft.fontHookDomainsResetRequested"))
        assertTrue(method.contains("HookDomainOverrideStore.fromRaw(draft.draftFontHookDomainsRaw)"))
        assertTrue(method.contains("HookDomainOverrideStore(activity.hookConfigStore).read("))
        assertFalse(method.contains("saveCustomIfDifferentFromAutomatic("))
        assertFalse(method.contains("store.restoreRecommended("))
        assertFalse(method.contains("store.setTargetViewportApplyMode("))
        assertTrue(hookPage.contains("fun HookChainEditorPage("))
        assertTrue(hookPage.contains("onHookChainChanged"))
    }

    @Test
    fun fontHookDomainButtonTextUsesMutablePreviewStateFlag() {
        val source = read(
            "src/main/java/com/dpis/module/appconfig/presentation/ComposeAppEditorActivityGateway.kt"
        )
        val methodStart = source.indexOf(
            "override fun hookChainText("
        )
        val methodEnd = source.indexOf(
            "override fun systemHooksEnabled(",
            methodStart
        )
        val method = source.substring(methodStart, methodEnd)

        assertTrue(method.contains("FontHookDomainPresentation.forOverride("))
        assertTrue(source.contains("FontHookDomainPresentation"))
        assertTrue(method.contains("EditorDraft"))
        assertTrue(source.contains("item?.previewFromGlobalPrefill"))
    }

    @Test
    fun composeTemplateEditorBridgesSelectionDraftAndCloseLifecycle() {
        val activity = read("src/main/java/com/dpis/module/MainActivity.kt")
        val coordinator = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspacePresentationCoordinator.kt")
        val workspace = read(
            "src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceContent.kt")
        val editorSurface = read(
            "src/main/java/com/dpis/module/templates/presentation/TemplateEditorContent.kt")
        val editorSheet = read(
            "src/main/java/com/dpis/module/ui/presentation/editor/EditorBottomSheet.kt")
        val shellHost = read("src/main/java/com/dpis/module/ui/presentation/MainComposeShellHost.kt")

        val templateSource = read("src/main/java/com/dpis/module/templates/TemplateWorkspacePresentationSource.kt")
        val templateCoordinator = read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceCoordinator.kt")
        val workspaceSession = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt"
        )
        assertTrue(workspaceSession.contains("override fun templateWorkspace()"))
        assertFalse(activity.contains("onComposeTemplateEditorOpened"))
        assertTrue(templateSource.contains("fun openEditor("))
        assertTrue(templateSource.contains("fun updateEditor("))
        assertTrue(templateSource.contains("fun closeEditor()"))
        assertTrue(templateCoordinator.contains("fun presentationSource("))
        assertTrue(templateCoordinator.contains("override fun closeEditor()"))
        assertTrue(coordinator.contains("onEditorOpened ="))
        assertTrue(coordinator.contains("onEditorChanged = content.templateWorkspace()::updateEditor"))
        assertTrue(coordinator.contains("onEditorClosed = content.templateWorkspace()::closeEditor"))
        assertTrue(workspace.contains("onEditorOpened: (quickTemplate: Boolean, templateId: String?)"))
        assertTrue(workspace.contains("onEditorChanged: (TemplateEditorForm) -> Unit"))
        assertTrue(workspace.contains("onEditorClosed: () -> Unit"))
        assertTrue(workspace.contains("onEditorOpened(kind == EDITOR_QUICK, templateId)"))
        assertTrue(workspace.contains("onEditorChanged(editorDraft.form)"))
        assertTrue(workspace.contains("onEditorClosed()"))
        assertTrue(workspace.contains(
                "val createdNewTemplate = editorDraft.form.quickTemplate && editorDraft.form.newTemplate"))
        assertTrue(workspace.contains("if (createdNewTemplate)"))
        assertFalse(workspace.contains("editorSheetVisible"))
        assertFalse(workspace.contains("editorSheetClosing"))
        assertTrue(workspace.contains("fun finishEditorClose()"))
        assertTrue(shellHost.contains("RenderTemplateEditorOverlay(state.workspaceMode, isCompactUi)"))
        assertTrue(coordinator.contains("TemplateEditorOverlayHost"))
        assertFalse(editorSurface.contains("sheetVisible"))
        assertFalse(editorSurface.contains("onSheetHidden"))
        assertFalse(editorSurface.contains("AppConfigEditorOverlay("))
        val editorHost = read(
            "src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorOverlay.kt")
        assertTrue(editorHost.contains("EditorSheetScaffoldFrame("))
        val workspaceCloseStart = workspace.indexOf("fun closeEditor()")
        val workspaceCloseEnd = workspace.indexOf("fun saveEditor()", workspaceCloseStart)
        val workspaceClose = workspace.substring(workspaceCloseStart, workspaceCloseEnd)
        assertFalse(workspaceClose.contains("onEditorDestinationChanged"))
        assertTrue(workspace.contains("closeEditor()"))
        assertTrue(workspace.contains("@Preview(showBackground = true"))
    }

    @Test
    fun composeTemplateRestorePublishesDetailWithoutLegacyEditorFallback() {
        val workspace = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt"
        )
        val methodStart = workspace.indexOf(
            "fun restoreWorkspaceEditorForCurrentConfiguration()"
        )
        val methodEnd = workspace.indexOf("fun refreshApps()", methodStart)
        val method = workspace.substring(methodStart, methodEnd)
        assertTrue(method.contains("restoreForConfiguration("))
        assertFalse(method.contains("showGlobalPrefillSheet"))
        assertFalse(method.contains("showQuickTemplateSheet"))
        assertFalse(method.contains("closeActiveTemplateSheetForMigration"))
    }

    private fun read(relativePath: String): String {
        return SourceSmokeTestPaths.read(relativePath)
    }

    private fun stringEntry(source: String, name: String): String {
        val marker = "name=\"$name\""
        val start = source.indexOf(marker)
        if (start < 0) {
            return ""
        }
        val end = source.indexOf("</string>", start)
        if (end < start) {
            return source.substring(start)
        }
        return source.substring(start, end)
    }

    private fun compact(source: String): String {
        return source.replace(Regex("\\s+"), " ").trim()
    }

    @Test
    fun hyperOsRestartPreparesNativeProxyBeforeProcessAction() {
        val source = read(
            "src/main/java/com/dpis/module/runtime/presentation/RuntimeLaunchSession.kt"
        )
        val policy = read(
            "src/main/java/com/dpis/module/hyperos/HyperOsNativeProxyApplyPolicy.kt",
        )
        val facade = read(
            "src/main/java/com/dpis/module/runtime/hyperos/HyperOsNativeProxyFacade.kt",
        )
        val activity = read("src/main/java/com/dpis/module/MainActivity.kt")
        assertTrue(
            source.contains("executeDialogProcessActionAfterHyperOsProxyReady")
        )
        assertFalse(activity.contains("ViewportTargetSpec.relativeScale(viewportValue * 10)"))
        assertTrue(
            source.contains("hyperOsNativeProxy.runAfterOptionalRestartPrepare(")
        )
        assertTrue(facade.contains("prepareForRestart(item, store)"))
        assertTrue(facade.contains("if (success) onReady()"))
        assertTrue(policy.contains("store.isTargetDpisEnabled(packageName)"))
        assertTrue(policy.contains("hasActiveStoredConfig(store, packageName)"))
        assertFalse(policy.contains("store.getTargetTypefaceId(packageName)"))
        assertFalse(policy.contains("typefaceId != null && !typefaceId.isBlank()"))
        assertFalse(policy.contains("item.fontScalePercent"))
        assertFalse(policy.contains("FontApplyMode.isEnabled"))
        assertTrue(facade.contains("executeMount(current.packageName, apply = true, onFinished)"))
        assertTrue(
            source.contains("processActionHandler.execute(item, mappedAction)")
        )
    }
}
