package com.dpis.module.shell

import com.dpis.module.SourceSmokeTestPaths
import com.dpis.module.ui.DialogWindowSizer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dpis.module.ui.presentation.MainComposeShellHost
import com.dpis.module.ui.presentation.MainWorkspacePresentationCoordinator
import com.dpis.module.appconfig.presentation.ComposeAppEditorActivityGateway
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder
import com.dpis.module.appconfig.landdetail.LandAppDetailPaneBinder
import com.dpis.module.updates.presentation.UpdateAvailableDialog
import com.dpis.module.applist.AppWorkspacePresentation
import com.dpis.module.applist.AppWorkspace
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
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val coordinator = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceCoordinator.kt")
        val session = read(
                "src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceActivitySession.kt")

        assertTrue(source.contains("private TemplateWorkspaceActivitySession workspaceSession"))
        assertTrue(source.contains(".handleActivityResult(requestCode, data)"))
        assertTrue(source.contains(".saveState(outState)"))
        assertTrue(source.contains(".onDestroy()"))
        assertFalse(source.contains("TemplateWorkspaceBinder"))
        assertFalse(source.contains("TemplateDetailPaneController"))
        assertFalse(source.contains("QuickTemplateTargetsBinder"))
        assertFalse(source.contains("QuickTemplateTargetSelectionActivity"))
        assertFalse(source.contains("TemplateEditorForm"))
        assertFalse(source.contains("QuickTemplateStore"))
        assertFalse(source.contains("REQUEST_QUICK_TEMPLATE_TARGETS"))
        assertTrue(coordinator.contains("fun attachLegacyViews("))
        assertTrue(coordinator.contains("fun presentationSource("))
        assertTrue(coordinator.contains("fun handleActivityResult("))
        assertTrue(session.contains("fun attachLegacyViews("))
        assertTrue(session.contains("fun saveState("))
    }

    @Test
    fun homeStatusReflectsUpdateCheckProgressWhilePromptOwnsUpdateActions() {
        val activity = read("src/main/java/com/dpis/module/MainActivity.java")
        val updateSession = read(
                "src/main/java/com/dpis/module/updates/presentation/MainUpdateSession.kt"
        )
        val homeState = read("src/main/java/com/dpis/module/home/HomeUpdateUiState.java")
        val composeHome = read(
                "src/main/java/com/dpis/module/home/presentation/HomeWorkspaceContent.kt"
        )

        assertTrue(activity.contains("new MainUpdateSession(this, this::bindHomeWorkspaceIfVisible)"))
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
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
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
    fun landDetailSaveRequestsScopeAfterSuccessfulSave() {
        val landSession = read(
            "src/main/java/com/dpis/module/appconfig/landdetail/LandAppDetailSession.kt",
        )
        val binder = read("src/main/java/com/dpis/module/appconfig/landdetail/LandAppDetailPaneBinder.kt")

        assertTrue(binder.contains("fun saveDraft("))
        assertTrue(binder.contains("state: AppConfigDialogState?"))
        assertTrue(landSession.contains("requestScopeAfterSuccessfulSave(item, state)"))
        assertTrue(landSession.contains("!state.scopeKnown"))
        assertTrue(landSession.contains("state.scopeSelected"))
        assertTrue(landSession.contains("state.scopeRequestPending"))
        assertTrue(landSession.contains("scopeCoordinator.requestScope("))
        assertTrue(landSession.contains("shell.showToast(R.string.save_scope_request_notice)"))
    }

    @Test
    fun composeSavePromotesApprovedScopeIntoCurrentEditorDraft() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val coordinator = read(
                "src/main/java/com/dpis/module/appconfig/editor/ComposeEditorScopeRequestCoordinator.kt"
        )
        val gateway = read(
                "src/main/java/com/dpis/module/appconfig/presentation/ComposeAppEditorActivityGateway.kt"
        )

        assertTrue(source.contains("new ComposeEditorScopeRequestCoordinator("))
        assertTrue(source.contains("new ComposeAppEditorShell(this)"))
        assertFalse(gateway.contains("import com.dpis.module.MainActivity"))
        assertTrue(gateway.contains("scopeCoordinator.requestAfterSuccessfulSave(item)"))
        assertTrue(coordinator.contains("mainViewModel.markEditingScopeSelected(packageName)"))
    }

    @Test
    fun composeWorkspaceOwnsFilterEntry() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val composeWorkspace = read(
                "src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt")

        assertFalse(source.contains("focusSearchInputAndShowKeyboard()"))
        assertFalse(source.contains("hideSearchFocusFab()"))
        assertFalse(source.contains("showSearchFocusFab()"))
        assertTrue(source.contains("new AppListFilterState("))
        assertTrue(composeWorkspace.contains("AppFilterSheet("))
    }

    @Test
    fun composeShellOwnsWorkspaceSelection() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
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
        assertTrue(
            workspace.contains(
                "val appWorkspace = mode == MainUiState.WorkspaceMode.APP"
            )
        )
        assertFalse(source.contains("private void updateWatchFilterTabsScrollOffset(int dy)"))
        assertTrue(workspace.contains("setVisible(shell.toolsWorkspaceContainer(), toolsWorkspace)"))
        assertTrue(workspace.contains("setVisible(shell.settingsWorkspaceContainer(), settingsWorkspace)"))
        assertFalse(source.contains("setSearchFocusFabVisible("))
        assertTrue(source.contains("ensureWorkspaceSession().attachLegacyViews("))
        assertFalse(source.contains("TemplateWorkspaceBinder"))
        assertFalse(source.contains("GlobalPrefillActionsAdapter"))
        assertFalse(source.contains("QuickTemplateActionsAdapter"))
        assertTrue(workspace.contains("fun bindWorkspaceSession()"))
        assertTrue(workspace.contains("shell.ensureTemplateWorkspace().present("))
        assertTrue(startup.contains("STATE_TEMPLATE_QUERY"))
        assertFalse(source.contains("searchFilterButton.setEnabled(appWorkspace)"))
        assertFalse(source.contains("applySearchClearButtonPosition(appWorkspace)"))
        assertFalse(source.contains("workspaceModeForButtonId(int checkedId)"))
        assertTrue(workspace.contains("MainComposeShellHost("))
    }

    @Test
    fun settingsPresentationUsesOneWorkspaceCapability() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
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
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val appWorkspace = read("src/main/java/com/dpis/module/applist/AppWorkspace.kt")
        val toolsWorkspace = read("src/main/java/com/dpis/module/settings/presentation/ToolsWorkspace.kt")

        val workspace = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt"
        )
        assertTrue(source.contains("private AppWorkspace appWorkspace"))
        assertTrue(source.contains("appWorkspace = new AppWorkspace("))
        assertTrue(workspace.contains("shell.appWorkspace()!!.actions()"))
        assertFalse(source.contains("createComposeAppWorkspaceActions()"))
        assertTrue(appWorkspace.contains("interface Host"))
        assertTrue(appWorkspace.contains("fun actions(): AppWorkspacePresentation.Actions"))
        assertTrue(toolsWorkspace.contains("class ToolsWorkspace("))
        assertTrue(toolsWorkspace.contains("private val binder = ToolsWorkspaceBinder("))
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
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val layout = read("src/main/res/layout-land/activity_status.xml")
        val targetsDetail = read("src/main/res/layout/view_land_quick_template_targets_detail.xml")

        assertTrue(layout.contains("android:id=\"@+id/land_detail_content\""))
        assertTrue(layout.contains("android:id=\"@+id/template_detail_content\""))
        assertTrue(layout.contains("android:id=\"@+id/template_detail_empty\""))
        assertTrue(layout.contains("android:id=\"@+id/land_detail_divider\""))
        assertTrue(source.contains("private View landDetailPane"))
        assertTrue(source.contains("private View landDetailDivider"))
        val templateCoordinator = read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceCoordinator.kt")
        val workspace = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt"
        )
        assertFalse(source.contains("templateDetailContent"))
        assertFalse(source.contains("TemplateDetailPaneController"))
        assertTrue(workspace.contains("applyLandscapeDetailVisibility(appWorkspace, templateWorkspace)"))
        assertTrue(workspace.contains("appWorkspace || templateWorkspace"))
        assertTrue(workspace.contains("restoreForConfiguration("))
        assertTrue(templateCoordinator.contains("fun attachLegacyViews("))
        assertTrue(templateCoordinator.contains("TemplateDetailPaneController("))
        assertTrue(templateCoordinator.contains("startPortraitTargetSelection("))
        assertFalse(source.contains("GlobalPrefillEditorBinder"))
        assertFalse(source.contains("QuickTemplateEditorBinder"))
        assertFalse(source.contains("GlobalPrefillSheetDialog"))
        assertFalse(source.contains("QuickTemplateEditSheetDialog"))
        assertTrue(templateCoordinator.contains("TemplateDetailKind.QUICK_TEMPLATE_TARGETS"))
        assertTrue(templateCoordinator.contains("legacyDetailController?.dispose()"))
        assertFalse(source.contains("? R.layout.dialog_global_prefill_sheet"))
        assertFalse(source.contains(": R.layout.dialog_quick_template_edit_sheet"))
        assertTrue(targetsDetail.contains("android:id=\"@+id/quick_template_targets_detail_root\""))
        assertTrue(targetsDetail.contains("android:id=\"@+id/quick_template_targets_list\""))
        assertTrue(targetsDetail.contains("android:id=\"@+id/quick_template_targets_save_button\""))
        assertTrue(targetsDetail.contains("@dimen/land_template_detail_subtitle_spacing_top"))
        assertFalse(targetsDetail.contains("@dimen/land_app_identity_secondary_spacing_top"))
        assertFalse(targetsDetail.contains("quick_template_targets_back_button"))
        assertFalse(targetsDetail.contains("@layout/activity_quick_template_targets"))
        assertTrue(targetsDetail.contains("android:id=\"@+id/quick_template_targets_detail_root\""))
    }

    @Test
    fun appEditorRestoreIsScopedToAppWorkspace() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val workspace = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt"
        )

        assertTrue(source.contains("restoreAppEditorForCurrentWorkspace()"))
        assertTrue(workspace.contains("fun restoreAppEditorForCurrentWorkspace()"))
        assertTrue(workspace.contains("shell.requireUiState().workspaceMode != MainUiState.WorkspaceMode.APP"))
        assertTrue(workspace.contains("shell.appConfigSheetSession().show(appItem)"))
        assertTrue(workspace.contains("shell.landAppDetailSession().show(appItem)"))
        val sheetSession = read(
            "src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetSession.kt",
        )
        assertTrue(sheetSession.contains("private var dialog: BottomSheetDialog? = null"))
        assertTrue(sheetSession.contains("if (dialog?.isShowing == true)"))
        assertTrue(sheetSession.contains("dialog = shown"))
    }

    @Test
    fun landscapeWorkspaceRailUsesCompactMaterialItemHeightAndScrollsWhenNeeded() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val landLayout = read("src/main/res/layout-land/activity_status.xml")
        val dimensions = read("src/main/res/values/dimens.xml")
        val roundDimensions = read("src/main/res/values-round/dimens.xml")

        assertTrue(landLayout.contains("com.google.android.material.navigationrail.NavigationRailView"))
        assertTrue(landLayout.contains("android:id=\"@+id/workspace_switch_scroll\""))
        assertTrue(landLayout.contains("android:fillViewport=\"true\""))
        assertTrue(landLayout.contains("app:labelVisibilityMode=\"selected\""))
        assertFalse(source.contains("bindLandscapeWorkspaceRailItemHeight()"))
        assertFalse(source.contains("workspaceSwitch instanceof NavigationRailView"))
        assertFalse(source.contains("availableHeight / railView.getMenu().size()"))
        assertTrue(dimensions.contains("main_land_workspace_rail_item_min_height\">64dp"))
        assertTrue(roundDimensions.contains("main_land_workspace_rail_item_min_height\">56dp"))
        assertFalse(source.contains("NavigationRailMenuView"))
    }

    @Test
    fun templateEditorDraftMigratesBetweenSheetAndPane() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
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
        assertTrue(source.contains("ensureWorkspaceSession().restore(savedInstanceState)"))
        assertTrue(source.contains("new TemplateWorkspaceActivitySession("))
        assertTrue(draft.contains("viewportScaleInput"))
        assertTrue(draft.contains("viewportAbsoluteInput"))
        assertTrue(workspace.contains("globalPrefillDraft: TemplateEditorDraft?"))
        assertTrue(workspace.contains("quickTemplateDraft: TemplateEditorDraft?"))
        assertFalse(source.contains("GlobalPrefillEditorBinder"))
        assertFalse(source.contains("QuickTemplateEditorBinder"))
    }

    @Test
    fun loadInstalledApps_publishesRowsBeforeIcons() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
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
        assertTrue(runtimeLaunch.contains("HyperOsNativeAppDetector.isNativeProxyCandidate("))
        assertTrue(iconSource.contains("produceState<Drawable?>"))
        assertTrue(iconSource.contains("InstalledAppIconCache.load"))
        assertTrue(workspaceSource.contains("rememberInstalledAppIcon(item.packageName, item.icon)"))
        assertTrue(workspaceSource.contains("if (icon == null)"))
        assertTrue(workspaceSource.contains("surfaceContainerHighest"))
        assertFalse(workspaceSource.contains("preloadIcons("))
        assertFalse(coordinatorSource.contains("getDefaultActivityIcon()"))
    }

    @Test
    fun appLoad_requestsXiaomiInstalledAppsPermissionBeforeQueryingPackages() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
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
        assertTrue(source.contains("installedAppsLoadSession.onRequestPermissionsResult(requestCode)"))
        assertTrue(loadSession.contains("permissionRequestCompleted"))
        assertTrue(
            loadSession.contains("isXiaomiPermissionDeclared()")
        )
        assertTrue(loadSession.contains("getPermissionInfo("))
        assertTrue(
            loadSession.contains("shell.dispatchRequestAppsLoad(true)")
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
                    "shell.dispatchRequestAppsLoad("
                )
        )
    }

    @Test
    fun savesAndRestoresPageScrollStatesForRotation() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val compose = read(
                "src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt")

        assertTrue(source.contains("appWorkspaceScrollStateStore.snapshot()"))
        assertTrue(source.contains("appWorkspaceScrollStateStore.restore("))
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
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val runtimeLayout = read(
            "src/main/java/com/dpis/module/tools/presentation/LocalToolDialogs.kt"
        )
        val strings = read("src/main/res/values/strings.xml")
        val zhStrings = read("src/main/res/values-zh-rCN/strings.xml")

        assertTrue(source.contains("maybeShowModuleRuntimeReloadAdvice()"))
        assertTrue(source.contains("ModuleRuntimeReloadNoticeCoordinator(this)"))
        assertTrue(source.contains("maybeShow(this::continueStartupDialogsAfterRuntimeReloadAdvice)"))
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
        assertTrue(source.contains("updateSession.maybeShowStartupDisclaimerDialog()"))
        assertTrue(
            source.contains("if (!updateSession.maybeShowStartupDisclaimerDialog()) {")
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
        assertTrue(updateSession.contains("StartupDisclaimerStore(activity)"))
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
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val homeSession = read(
            "src/main/java/com/dpis/module/home/presentation/HomeWorkspaceSession.kt"
        )
        val homeState = read(
            "src/main/java/com/dpis/module/home/HomeWorkspaceState.kt"
        )
        val compose = read(
            "src/main/java/com/dpis/module/home/presentation/HomeWorkspaceContent.kt"
        )

        assertTrue(source.contains("updateSession.checkForUpdatesNow()"))
        assertTrue(homeSession.contains("shell.checkForUpdatesNow()"))
        assertTrue(homeState.contains("interface HomeWorkspaceActions"))
        val primaryStatus = compose
            .substringAfter("private fun HomePrimaryStatus")
            .substringBefore("private fun HomeCountCard")
        assertTrue(primaryStatus.contains("state.updateState.subtitle(context)"))
        assertFalse(primaryStatus.contains("if (!disabled) {\n                    Text("))
        assertTrue(compose.contains("state.actions.checkForUpdates()"))
    }

    @Test
    fun startupDisclaimerLayoutKeepsScrollableContent() {
        val layout = read(
            "src/main/res/layout/dialog_startup_disclaimer.xml"
        )
        val roundLayout = read(
            "src/main/res/layout-round/dialog_startup_disclaimer.xml"
        )
        val dimensions = read("src/main/res/values/dimens.xml")

        assertTrue(
            layout.contains("com.dpis.module.ui.MaxHeightNestedScrollView")
        )
        assertTrue(layout.contains("app:maxHeightFraction=\"0.45\""))
        assertTrue(layout.contains("startup_disclaimer_message"))
        assertTrue(layout.contains("startup_disclaimer_checkbox"))
        assertTrue(
            layout.indexOf("</com.dpis.module.ui.MaxHeightNestedScrollView>") <
                layout.indexOf(
                    "android:id=\"@+id/startup_disclaimer_checkbox\""
                )
        )
        assertTrue(layout.contains("startup_disclaimer_accept_button"))
        assertFalse(layout.contains("startup_disclaimer_exit_button"))
        assertTrue(layout.contains("@dimen/dialog_surface_padding_horizontal"))
        assertTrue(layout.contains("@dimen/dialog_body_spacing"))
        assertTrue(layout.contains("@dimen/dialog_text_line_spacing"))
        assertTrue(layout.contains("@dimen/dialog_action_spacing_top"))
        assertFalse(layout.contains("@dimen/dialog_action_spacing_between"))
        assertTrue(roundLayout.contains("@style/TextAppearance.Material3.TitleSmall"))
        assertTrue(roundLayout.contains("android:maxLines=\"2\""))
        assertTrue(roundLayout.contains("app:maxHeightFraction=\"0.25\""))
        assertTrue(roundLayout.contains("@style/TextAppearance.Material3.BodySmall"))
        assertTrue(roundLayout.contains("startup_disclaimer_checkbox"))
        assertTrue(roundLayout.contains("startup_disclaimer_accept_button"))
        assertTrue(dimensions.contains("dialog_round_surface_padding_horizontal"))
        assertTrue(dimensions.contains("dialog_round_surface_padding_vertical"))
        assertTrue(dimensions.contains("dialog_round_body_spacing"))
        assertTrue(dimensions.contains("dialog_round_text_line_spacing"))
        assertTrue(dimensions.contains("dialog_round_action_spacing_top"))
    }

    @Test
    fun appConfigLayoutUsesScrollableContainerAndAdaptiveModeRows() {
        val layout = read("src/main/res/layout/dialog_app_config.xml")

        assertTrue(layout.contains("androidx.core.widget.NestedScrollView"))
        assertTrue(layout.contains("android:fillViewport=\"true\""))
        assertTrue(
            layout.contains(
                "android:minHeight=\"@dimen/dialog_mode_toggle_row_min_height\""
            )
        )
        assertFalse(
            layout.contains(
                "android:layout_height=\"@dimen/dialog_mode_toggle_row_height\""
            )
        )
    }

    @Test
    fun pageRefresh_forcesInstalledAppCatalogReload() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")

        val refreshStart = source.indexOf(
            "private void onPageRefreshRequested(AppListPage page) {"
        )
        val refreshEnd = source.indexOf(
            "void requestAppsLoad()",
            refreshStart
        )
        assertTrue(refreshStart >= 0)
        assertTrue(refreshEnd > refreshStart)

        val refreshBody = source.substring(refreshStart, refreshEnd)
        assertTrue(refreshBody.contains("installedAppsLoadSession.requestLoad(true)"))
    }

    @Test
    fun appLoad_reusesInstalledAppCatalogBetweenRefreshes() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
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
        val source = read("src/main/java/com/dpis/module/MainActivity.java")

        assertTrue(
            source.contains("private boolean skipNextImmediateServiceReload")
        )
        val startup = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )
        assertTrue(
            startup.contains(
                "skipNextImmediateServiceReload = appsSnapshot.isNotEmpty()"
            )
        )
        assertTrue(
            source.contains(
                "DpisApplication.addServiceStateListener(this, true)"
            )
        )
        assertTrue(source.contains("if (skipNextImmediateServiceReload)"))
    }

    @Test
    fun appConfigSheet_halfExpandedStateUsesDownwardOffset() {
        val coordinatorSource = read(
            "src/main/java/com/dpis/module/appconfig/AppConfigDialogCoordinator.java"
        )

        assertTrue(
            coordinatorSource.contains(
                "R.dimen.dialog_app_config_half_expanded_down_offset"
            )
        )
        assertTrue(
            coordinatorSource.contains(
                "anchorBottom - sheetPos[1] - halfExpandedDownOffsetPx"
            )
        )
    }

    @Test
    fun showEditDialog_usesSheetCoordinatorInPortraitAndDetailPaneInLandscape() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val landSession = read(
            "src/main/java/com/dpis/module/appconfig/landdetail/LandAppDetailSession.kt",
        )

        val sheetSession = read(
            "src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetSession.kt",
        )
        assertTrue(
            sheetSession.contains(
                "AppConfigPrefillPreview.resolveForEditor(activity, item, store)",
            )
                || landSession.contains(
                    "AppConfigPrefillPreview.resolveForEditor(activity, item, store)",
                )
        )
        assertTrue(
            sheetSession.contains("binder.bind(dialogView, sheetItem, systemHooksEnabled)")
                || landSession.contains("dialogView, sheetItem, systemHooksEnabled")
        )
        assertTrue(source.contains("appConfigSheetSession.show(item)"))
        assertTrue(sheetSession.contains("AppConfigDialogBinder(activity, dialogHost)"))
        assertTrue(source.contains("createAppConfigDialogHost()"))
        assertTrue(sheetSession.contains("binder.bind("))
        assertTrue(
            sheetSession.contains("AppConfigDialogCoordinator(activity).show(dialogView)")
        )
        assertTrue(source.contains("landAppDetailSession.show(item)"))
        assertTrue(landSession.contains("R.layout.view_land_app_detail"))
        assertTrue(landSession.contains("LandAppDetailPaneBinder(activity, this)"))
        assertTrue(landSession.contains("fun saveDraft("))
        assertTrue(source.contains("state,"))
        assertTrue(landSession.contains("override fun showTypefaceSelector("))
        assertTrue(
            landSession.contains("dialogHost.showFontHookDomains(item, state, onChanged)")
        )
        assertTrue(landSession.contains("override fun toggleScope("))
        assertTrue(
            source.contains(
                "public boolean setDpisEnabled(String packageName, boolean enabled)"
            )
        )
        assertFalse(source.contains("resetLandDetailConfig(editorItem)"))
        assertTrue(landSession.contains("saveHandler.saveResolved("))
        val dialogHost = read(
            "src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogActivityHost.kt"
        )
        assertTrue(dialogHost.contains("activity.updateEditingDraft(state)"))
        assertTrue(landSession.contains("fun onDraftStateChanged("))
        val startup = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )
        assertTrue(startup.contains("val editingDraft = draft ?: viewModel?.editingDraft"))
        assertTrue(
            read("src/main/java/com/dpis/module/appconfig/landdetail/LandAppDetailPaneBinder.kt")
                .contains("AppConfigDialogState.fromItem(item)")
        )
        assertTrue(landSession.contains("shell.executeProcessAction(item, action)"))
        assertTrue(compact(landSession).contains("landDetailContent.addView( dialogView"))
        assertTrue(landSession.contains("ViewGroup.LayoutParams.MATCH_PARENT"))
        assertFalse(source.contains("createLandDetailContentLayoutParams()"))
        assertFalse(
            source.contains(
                "landDetailContent.post(() -> applyLandDetailContentLayout(dialogView))"
            )
        )
        assertFalse(source.contains("private void bindDialogValidation("))
        assertFalse(source.contains("private void bindDialogActions("))
        assertFalse(source.contains("private void refreshDialogState("))
    }

    @Test
    fun showEditDialog_doesNotRefreshListRowsBeforeOpeningDetail() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val methodStart = source.indexOf(
            "private void showEditDialog(AppListItem item) {"
        )
        val methodEnd = source.indexOf(
            "public HomeWorkspaceState createHomeWorkspaceState()",
            methodStart
        )
        assertTrue(methodStart >= 0)
        assertTrue(methodEnd > methodStart)

        val methodBody = source.substring(methodStart, methodEnd)
        assertFalse(methodBody.contains("refreshVisibleStatuses"))
    }

    @Test
    fun landscapeStatusLayout_usesFlatDetailPane() {
        val layout = read("src/main/res/layout-land/activity_status.xml")

        assertTrue(layout.contains("@+id/land_root_row"))
        assertFalse(layout.contains("@+id/app_pager"))
        assertTrue(layout.contains("@+id/land_detail_pane"))
        assertTrue(layout.contains("@+id/land_detail_content"))
        assertFalse(layout.contains("android:paddingTop=\"@dimen/main_land_detail_top_padding\""))
        assertTrue(layout.contains("<FrameLayout"))
        assertFalse(
            layout.contains(
                "<com.google.android.material.card.MaterialCardView\n" +
                    "                android:id=\"@+id/land_detail_pane\""
            )
        )
    }

    @Test
    fun landscapeAppDetailUsesDedicatedOverviewRows() {
        val layout = read("src/main/res/layout/view_land_app_detail.xml")
        val binder = read(
            "src/main/java/com/dpis/module/appconfig/landdetail/LandAppDetailPaneBinder.kt"
        )
        val dimens = read("src/main/res/values/dimens.xml")
        val strings = read("src/main/res/values/strings.xml")

        assertFalse(layout.contains("@string/land_detail_display_font_section_title"))
        assertFalse(layout.contains("@string/land_detail_font_hook_section_title"))
        assertTrue(layout.contains("@drawable/bg_land_detail_connected_row_top"))
        assertTrue(layout.contains("@drawable/bg_land_detail_connected_row_bottom"))
        assertTrue(layout.contains("@drawable/ripple_land_detail_connected_row_top"))
        assertTrue(layout.contains("@drawable/ripple_land_detail_connected_row_bottom"))
        assertTrue(layout.contains("@dimen/land_app_detail_connected_row_gap"))
        assertTrue(layout.contains("@string/dialog_advanced_section_title"))
        assertFalse(layout.contains("@string/land_detail_app_control_section_title"))
        assertTrue(layout.contains("<com.google.android.material.card.MaterialCardView"))
        assertTrue(layout.contains("android:layout_height=\"match_parent\""))
        assertTrue(layout.contains("android:id=\"@+id/land_detail_scroll\""))
        val landScrollStart = layout.indexOf("android:id=\"@+id/land_detail_scroll\"")
        val landScrollEnd = layout.indexOf(">", landScrollStart)
        val landScrollBlock = layout.substring(landScrollStart, landScrollEnd)
        assertTrue(landScrollBlock.contains(
            "android:paddingTop=\"@dimen/main_land_detail_top_padding\""
        ))
        assertTrue(layout.contains("android:id=\"@+id/land_detail_scroll_content\""))
        assertFalse(layout.contains("android:layout_height=\"@dimen/main_content_divider_height\""))
        assertFalse(layout.contains("@dimen/land_app_detail_input_group_padding_horizontal"))
        assertTrue(layout.contains("@dimen/land_app_detail_section_gap"))
        assertTrue(layout.contains("@dimen/land_app_detail_editor_row_spacing"))
        assertTrue(layout.contains("@dimen/land_app_detail_card_inner_spacing"))
        assertFalse(layout.contains("@dimen/dialog_app_config_process_row_spacing_top"))
        assertFalse(layout.contains("@dimen/dialog_app_config_save_row_spacing_top"))
        assertFalse(layout.contains("@drawable/bg_land_detail_process_capsule"))
        val advancedCardStart = layout.indexOf("<!-- Advanced Actions Card -->")
        val advancedCardEnd = layout.indexOf("</com.google.android.material.card.MaterialCardView>",
                advancedCardStart)
        val advancedCardBlock = layout.substring(advancedCardStart, advancedCardEnd)
        assertTrue(advancedCardBlock.contains(
                "app:cardBackgroundColor=\"?attr/colorSurfaceContainer\""))
        assertFalse(advancedCardBlock.contains("app:strokeColor=\"?attr/colorOutlineVariant\""))
        assertFalse(advancedCardBlock.contains(
                "app:strokeWidth=\"@dimen/land_app_detail_card_stroke_width\""))
        assertTrue(advancedCardBlock.contains("app:strokeWidth=\"0dp\""))
        assertTrue(layout.contains(
                "android:layout_marginBottom=\"@dimen/land_app_detail_dock_margin_bottom\""))
        val landStatusStart = layout.indexOf("android:id=\"@+id/land_detail_status\"")
        val landStatusEnd = layout.indexOf("/>", landStatusStart)
        val landStatusBlock = layout.substring(landStatusStart, landStatusEnd)
        assertTrue(layout.contains("@style/Widget.Dpis.AppIdentityTitle"))
        assertTrue(layout.contains("@style/Widget.Dpis.LandAppIdentitySecondaryText"))
        assertTrue(layout.contains("@style/Widget.Dpis.LandAppIdentityStatusText"))
        assertTrue(layout.contains("@dimen/land_app_identity_secondary_spacing_top"))
        assertTrue(layout.contains("@dimen/land_app_identity_status_spacing_top"))
        assertFalse(layout.contains("@dimen/land_template_detail_subtitle_spacing_top"))
        assertTrue(dimens.contains(
                "<dimen name=\"land_app_identity_secondary_spacing_top\">0dp</dimen>"
        ))
        assertTrue(dimens.contains(
                "<dimen name=\"land_app_identity_status_spacing_top\">0dp</dimen>"
        ))
        assertTrue(landStatusBlock.contains("android:layout_width=\"0dp\""))
        assertTrue(landStatusBlock.contains("android:layout_weight=\"1\""))
        assertTrue(layout.contains("android:id=\"@+id/land_detail_unsaved_badge\""))
        assertTrue(binder.contains("LandAppDetailEditorSession.attach(root, LandAppDetailEditorSession.open(activity, item))"))
        assertTrue(binder.contains("LandAppDetailEditorSession.syncFromViews(root)"))
        assertTrue(binder.contains("LandAppDetailEditorSession.reset(root)"))
        assertTrue(binder.contains("LandAppDetailEditorSession.markSaved(root)"))
        val unsavedBadgeStart = layout.indexOf("android:id=\"@+id/land_detail_unsaved_badge\"")
        val unsavedBadgeEnd = layout.indexOf("/>", unsavedBadgeStart)
        val unsavedBadgeBlock = layout.substring(unsavedBadgeStart, unsavedBadgeEnd)
        assertTrue(unsavedBadgeBlock.contains("android:layout_width=\"wrap_content\""))
        assertFalse(unsavedBadgeBlock.contains("android:layout_weight=\"1\""))
        assertTrue(layout.contains("android:id=\"@+id/land_detail_action_dock\""))
        val actionDockIdStart = layout.indexOf("android:id=\"@+id/land_detail_action_dock\"")
        val actionDockStart = layout.lastIndexOf("<FrameLayout", actionDockIdStart)
        val actionDockTagEnd = layout.indexOf(">", actionDockStart)
        val actionDockTag = layout.substring(actionDockStart, actionDockTagEnd)
        assertTrue(actionDockTag.contains("<FrameLayout"))
        assertFalse(actionDockTag.contains("cardBackgroundColor"))
        val actionSurfaceStart = layout.indexOf(
                "android:id=\"@+id/land_detail_action_surface\""
        )
        val actionSurfaceTagEnd = layout.indexOf(">", actionSurfaceStart)
        val actionSurfaceTag = layout.substring(actionSurfaceStart, actionSurfaceTagEnd)
        assertTrue(actionSurfaceTag.contains(
                "app:cardBackgroundColor=\"?attr/colorSurfaceContainerHigh\""))
        assertTrue(actionSurfaceTag.contains(
                "app:cardCornerRadius=\"@dimen/land_app_detail_dock_corner_radius\""))
        assertTrue(layout.contains(
                "android:id=\"@+id/land_detail_process_action_group\""))
        assertTrue(layout.contains(
                "app:cardBackgroundColor=\"?attr/colorSurfaceContainer\""))
        val adaptiveLayout = read(
            "src/main/java/com/dpis/module/appconfig/landdetail/LandAppDetailAdaptiveLayout.kt",
        )
        assertTrue(binder.contains("LandAppDetailAdaptiveLayout.bindActionDock("))
        assertTrue(adaptiveLayout.contains("fun updateScrollContentClearance("))
        assertTrue(adaptiveLayout.contains("R.id.land_detail_scroll_content"))
        assertTrue(adaptiveLayout.contains("content.setPaddingRelative("))
        assertFalse(adaptiveLayout.contains("MarginLayoutParams"))
        assertTrue(layout.contains("android:id=\"@+id/land_detail_save_button\""))
        assertTrue(layout.contains("android:id=\"@+id/land_detail_scope_row\""))
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_viewport_input\"")
        )
        assertTrue(layout.contains("android:inputType=\"numberDecimal\""))
        assertTrue(
            layout.contains(
                "android:id=\"@+id/land_detail_viewport_mode_toggle_button\""
            )
        )
        assertTrue(
            layout.contains(
                "android:id=\"@+id/land_detail_viewport_mode_scale_label\""
            )
        )
        assertTrue(
            layout.contains(
                "android:id=\"@+id/land_detail_viewport_mode_width_label\""
            )
        )
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_font_scale_input\"")
        )
        assertTrue(
            layout.indexOf("android:id=\"@+id/land_detail_font_scale_editor\"")
                < layout.indexOf("android:id=\"@+id/dialog_wechat_dpi_row\"")
        )
        assertTrue(
            layout.indexOf("android:id=\"@+id/dialog_wechat_dpi_row\"")
                < layout.indexOf("android:id=\"@+id/land_detail_typeface_row\"")
        )
        assertTrue(
            layout.contains(
                "android:id=\"@+id/land_detail_font_mode_toggle_button\""
            )
        )
        assertTrue(
            layout.contains(
                "android:id=\"@+id/land_detail_font_mode_system_label\""
            )
        )
        assertTrue(
            layout.contains(
                "android:id=\"@+id/land_detail_font_mode_compat_label\""
            )
        )
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_typeface_row\"")
        )
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_hook_chain_row\"")
        )
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_scope_row\"")
        )
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_dpis_toggle_row\"")
        )
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_reset_row\"")
        )
        assertTrue(
            layout.contains(
                "android:id=\"@+id/land_detail_feedback_diagnostic_row\""
            )
        )
        assertTrue(
            layout.contains(
                "android:text=\"@string/feedback_diagnostic_record_action\""
            )
        )
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_start_button\"")
        )
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_restart_button\"")
        )
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_stop_button\"")
        )
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_save_button\"")
        )
        assertFalse(layout.contains("dialog_viewport_input_layout"))
        assertTrue(binder.contains("interface Actions"))
        assertTrue(binder.contains("packageManager"))
        assertTrue(binder.contains("getPackageInfo(item.packageName, 0)"))
        assertTrue(binder.contains("fun saveDraft("))
        assertTrue(binder.contains("item: AppListItem?,"))
        assertTrue(binder.contains("actions.saveDraft("))
        assertTrue(
            binder.contains("AppConfigDialogBinder.bindViewportModeToggle(")
        )
        assertTrue(
            binder.contains("AppConfigDialogBinder.bindFontModeToggle(")
        )
        assertTrue(binder.contains("actions.toggleScope("))
        assertTrue(binder.contains("actions.startFeedbackDiagnostic(item, state)"))
        assertTrue(binder.contains("state.scopeSelected,"))
        assertTrue(compact(binder).contains("actions.setDpisEnabled(item.packageName, nextEnabled)"))
        assertTrue(binder.contains("resetDraft("))
        assertTrue(binder.contains("root,"))
        assertTrue(binder.contains("item,"))
        assertTrue(binder.contains("FormInputFocusBinder.bindDismissOnOutsideTouch"))
        assertTrue(binder.contains("FormInputFocusBinder.clearFocusAndHideIme"))
        assertTrue(binder.contains("WechatDpiSheetBinder.inputViewForFocus(root)"))
        assertTrue(binder.contains("state.clearViewportInputs()"))
        assertTrue(binder.contains("WechatDpiSheetBinder.clearDraft(root)"))
        assertTrue(
            binder.contains(
                "AppConfigDialogState.fromItem(item)"
            )
        )
        assertTrue(
            compact(binder).contains(
                "root.setTag(R.id.land_detail_hook_chain_row, state)"
            )
        )
        assertTrue(
            compact(binder).contains(
                "root?.getTag(R.id.land_detail_hook_chain_row)"
            )
        )
        assertTrue(
            read("src/main/java/com/dpis/module/appconfig/landdetail/LandAppDetailDraftSignature.kt")
                .contains("root?.setTag(R.id.land_detail_save_button, signature.orEmpty())")
        )
        assertTrue(binder.contains("actions.showTypefaceSelector(item"))
        assertTrue(binder.contains("actions.showHookDomains(item, state"))
        assertFalse(binder.contains("currentFontConfigItem("))
        assertFalse(binder.contains("withFontConfig("))
        assertTrue(dimens.contains("land_app_detail_card_padding"))
        assertTrue(dimens.contains("land_app_detail_section_gap"))
        assertTrue(dimens.contains("land_app_detail_card_gap"))
        assertTrue(dimens.contains("land_app_detail_card_inner_spacing"))
        assertTrue(dimens.contains("land_app_detail_list_item_padding_horizontal"))
        assertTrue(dimens.contains("land_app_detail_editor_row_spacing"))
        assertTrue(dimens.contains("land_app_detail_editor_input_min_width"))
        assertTrue(dimens.contains("land_app_detail_connected_row_inner_radius"))
    }

    @Test
    fun landscapeDetailInsetsAreAppliedToScrollableContent() {
        val landSession = read(
            "src/main/java/com/dpis/module/appconfig/landdetail/LandAppDetailSession.kt",
        )
        val methodStart = landSession.indexOf("private fun applyContentInsets")
        val methodEnd = landSession.indexOf("private fun readPersistedWechatDpiForDiagnostic", methodStart)

        assertTrue(methodStart >= 0)
        assertTrue(methodEnd > methodStart)
        val methodBody = landSession.substring(methodStart, methodEnd)
        assertTrue(methodBody.contains("detailView.findViewById<View>(R.id.land_detail_scroll)"))
        assertTrue(methodBody.contains(
                "WindowInsetsBinder.applySafeDrawingPadding(scrollView, false, true, false, true)"
        ))
        assertFalse(methodBody.contains("ViewCompat.requestApplyInsets(scrollView)"))
        assertTrue(landSession.contains("applyContentInsets(dialogView)"))
        assertTrue(landSession.contains("ViewCompat.requestApplyInsets(scrollView)"))
    }

    @Test
    fun appConfigAndProcessActions_delegateToDedicatedHandlers() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val runtimeLaunch = read(
            "src/main/java/com/dpis/module/runtime/presentation/RuntimeLaunchSession.kt"
        )
        val runtimeShell = read(
            "src/main/java/com/dpis/module/runtime/presentation/RuntimeLaunchShell.kt"
        )

        assertTrue(source.contains("new RuntimeLaunchSession(new RuntimeLaunchShell(this))"))
        assertTrue(runtimeLaunch.contains("ProcessActionHandler("))
        assertTrue(runtimeLaunch.contains("syncRuntimePropertiesForTargetLaunch(packageName)"))
        assertTrue(runtimeShell.contains("ProcessActionConfirm(activity, activity::composeShell)"))
        assertTrue(source.contains("new AppConfigSaveHandler()"))
        assertTrue(
            runtimeLaunch.contains("processActionHandler.execute(item, mappedAction)")
        )
        assertTrue(
            read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogActivityHost.kt")
                .contains("saveHandler.save(")
        )
        assertTrue(
            source.contains(
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
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
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
        val source = read("src/main/java/com/dpis/module/MainActivity.java")

        assertTrue(
            source.contains(
                "new SystemScopeCoordinator(createSystemScopeHost())"
            )
        )
        val landSession = read(
            "src/main/java/com/dpis/module/appconfig/landdetail/LandAppDetailSession.kt",
        )
        assertTrue(landSession.contains("scopeCoordinator.toggleScope("))
        assertTrue(
            source.contains(
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
            "src/main/java/com/dpis/module/runtime/DpisPackageLifecycleReceiver.java"
        )

        assertTrue(manifest.contains(".runtime.DpisPackageLifecycleReceiver"))
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
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val host = read(
            "src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogActivityHost.kt"
        )

        val landSession = read(
            "src/main/java/com/dpis/module/appconfig/landdetail/LandAppDetailSession.kt",
        )
        assertTrue(source.contains("createAppConfigDialogHost()"))
        assertTrue(source.contains("return appConfigDialogHost"))
        assertTrue(landSession.contains("dialogHost.showFontHookDomains(item, state, onChanged)"))
        assertTrue(source.contains("public String getFontHookDomainsButtonText("))
        assertTrue(source.contains("appConfigDialogHost.fontHookDomainsButtonText(item, state)"))
        assertTrue(host.contains("fun showFontHookDomains("))
        assertTrue(host.contains("resolveFontHookDomainsForDraft(target, state)"))
        assertTrue(host.contains("HookDomainOverrideStore(activity.hookConfigStore).read("))
        assertTrue(host.contains("FontHookDomainDialog.show("))
        assertTrue(host.contains("isFontHookDomainEditingEnabled()"))
        assertTrue(host.contains("AppConfigDialogBinder.resolveFontMode(fontModeToggle(root))"))
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
            "src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogActivityHost.kt"
        )
        val methodStart = source.indexOf(
            "fun showFontHookDomains("
        )
        val methodEnd = source.indexOf(
            "override fun getFontHookDomainsButtonText(",
            methodStart
        )
        val method = source.substring(methodStart, methodEnd)

        assertTrue(
            compact(method).contains(
                "val currentOverride = resolveFontHookDomainsForDraft(target, state)"
            )
        )
        assertTrue(
            compact(method).contains(
                "state.draftFontHookDomainsRaw = HookDomainOverrideStore.rawValueForSelection("
            )
        )
        assertTrue(method.contains("state.draftFontHookDomainsRaw = null"))
        assertTrue(
            compact(method).contains(
                "state.viewportApplyMode = ViewportApplyMode.normalize(mode)"
            )
        )
        assertTrue(
            compact(method).contains(
                "store.getTargetViewportApplyMode(target.packageName)"
            )
        )
        val compactMethod = compact(method)
        assertFalse(compactMethod.contains("saveCustomIfDifferentFromAutomatic("))
        assertFalse(compactMethod.contains("store.restoreRecommended("))
        assertFalse(compactMethod.contains("store.setTargetViewportApplyMode("))
        assertFalse(compactMethod.contains("requestAppsLoad()"))
    }

    @Test
    fun fontHookDomainButtonTextUsesMutablePreviewStateFlag() {
        val source = read(
            "src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogActivityHost.kt"
        )
        val methodStart = source.indexOf(
            "override fun getFontHookDomainsButtonText("
        )
        val methodEnd = source.indexOf(
            "override fun openTypefaceLibrary()",
            methodStart
        )
        val method = source.substring(methodStart, methodEnd)

        assertTrue(method.contains("FontHookDomainPresentation.forOverride("))
        assertTrue(source.contains("FontHookDomainPresentation"))
        assertTrue(method.contains("AppConfigDialogBinder.AppConfigDialogState?"))
        assertFalse(method.contains("item.previewFromGlobalPrefill"))
    }

    @Test
    fun composeTemplateEditorBridgesSelectionDraftAndCloseLifecycle() {
        val activity = read("src/main/java/com/dpis/module/MainActivity.java")
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
        val methodStart = source.indexOf(
            "private fun shouldPrepareHyperOsNativeProxyForRestart"
        )
        val methodEnd = source.indexOf(
            "private fun executeDialogProcessActionAfterHyperOsProxyReady",
            methodStart
        )
        assertTrue(methodStart >= 0)
        assertTrue(methodEnd > methodStart)
        val methodBody = source.substring(methodStart, methodEnd)

        val landSession = read(
            "src/main/java/com/dpis/module/appconfig/landdetail/LandAppDetailSession.kt",
        )
        val activity = read("src/main/java/com/dpis/module/MainActivity.java")
        assertTrue(
            source.contains("executeDialogProcessActionAfterHyperOsProxyReady")
        )
        assertTrue(landSession.contains("AppConfigInputValidation.parseViewportTargetSpec("))
        assertFalse(activity.contains("ViewportTargetSpec.relativeScale(viewportValue * 10)"))
        assertTrue(
            source.contains("shouldPrepareHyperOsNativeProxyForRestart(item)")
        )
        assertTrue(
            methodBody.contains("val store = shell.hookConfigStore()")
        )
        assertTrue(
            methodBody.contains("store.isTargetDpisEnabled(item!!.packageName)")
        )
        assertTrue(
            methodBody.contains(
                "hasActiveStoredConfig(store, item.packageName)"
            )
        )
        assertFalse(
            methodBody.contains("store.getTargetTypefaceId(packageName)")
        )
        assertFalse(
            methodBody.contains("typefaceId != null && !typefaceId.isBlank()")
        )
        assertFalse(
            methodBody.contains(
                "item.fontScalePercent != null\n                && item.fontScalePercent > 0"
            )
        )
        assertFalse(methodBody.contains("FontApplyMode.isEnabled"))
        assertTrue(
            source.contains(
                "executeHyperOsNativeProxyMount(item, true) { success ->"
            )
        )
        assertTrue(source.contains("if (success)"))
        assertTrue(
            source.contains("processActionHandler.execute(item, mappedAction)")
        )
    }
}
