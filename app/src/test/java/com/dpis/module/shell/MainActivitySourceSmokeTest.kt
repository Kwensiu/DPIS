package com.dpis.module

import com.dpis.module.ui.DialogWindowSizer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

import java.io.IOException
import org.junit.Test

class MainActivitySourceSmokeTest {

    @Test
    fun homeStatusReflectsUpdateCheckProgressWhilePromptOwnsUpdateActions() {
        val activity = read("src/main/java/com/dpis/module/MainActivity.java")
        val homeState = read("src/main/java/com/dpis/module/home/HomeUpdateUiState.java")
        val composeHome = read(
                "src/main/java/com/dpis/module/home/presentation/HomeWorkspaceContent.kt"
        )

        assertTrue(activity.contains("applyHomeUpdateState(HomeUpdateUiState.CHECKING)"))
        assertTrue(activity.contains("applyHomeUpdateState(HomeUpdateUiState.available(manifest))"))
        assertTrue(activity.contains("showUpdateAvailableDialog("))
        assertTrue(activity.contains("applyHomeUpdateState(HomeUpdateUiState.UP_TO_DATE)"))
        assertTrue(activity.contains("applyHomeUpdateState(HomeUpdateUiState.FAILED)"))
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

        assertTrue(source.contains("fallbackFocusView.setFocusable(true)"));
        assertTrue(source.contains("fallbackFocusView.setFocusableInTouchMode(true)"));
        assertTrue(source.contains("fallbackFocusView.requestFocus()"));
        assertTrue(source.contains("hideSoftInputFromWindow("))
    }

    @Test
    fun landDetailSaveRequestsScopeAfterSuccessfulSave() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val binder = read("src/main/java/com/dpis/module/appconfig/LandAppDetailPaneBinder.java")

        assertTrue(binder.contains("void saveDraft("))
        assertTrue(binder.contains("AppConfigDialogBinder.AppConfigDialogState state,"))
        assertTrue(source.contains("requestLandDetailScopeAfterSuccessfulSave(item, state)"));
        assertTrue(source.contains("!state.scopeKnown"))
        assertTrue(source.contains("state.scopeSelected"))
        assertTrue(source.contains("state.scopeRequestPending"))
        assertTrue(source.contains("systemScopeCoordinator.requestScope("))
        assertTrue(source.contains("showToast(R.string.save_scope_request_notice)"));
    }

    @Test
    fun composeSavePromotesApprovedScopeIntoCurrentEditorDraft() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val coordinator = read(
                "src/main/java/com/dpis/module/appconfig/editor/ComposeEditorScopeRequestCoordinator.kt"
        )
        val gateway = read(
                "src/main/java/com/dpis/module/appconfig/editor/ComposeAppEditorActivityGateway.kt"
        )

        assertTrue(source.contains("new ComposeEditorScopeRequestCoordinator("))
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

        assertTrue(source.contains("STATE_WORKSPACE_MODE"))
        assertTrue(source.contains("MainUiState.WorkspaceMode.fromName("))
        assertFalse(source.contains("bindWorkspaceSwitch()"))
        assertFalse(source.contains("workspaceSwitch.setOnItemSelectedListener"))
        assertFalse(source.contains("private boolean updatingWorkspaceSelection"));
        assertTrue(source.contains("MainUiAction.workspaceModeChanged("))
        assertTrue(source.contains("applyWorkspaceMode(state.workspaceMode)"));
        assertTrue(
            source.contains(
                "boolean appWorkspace = mode == MainUiState.WorkspaceMode.APP"
            )
        )
        assertFalse(source.contains("private void updateWatchFilterTabsScrollOffset(int dy)"))
        assertTrue(source.contains(
                "setVisible(templateWorkspaceContainer, templateWorkspace)"
        ))
        assertTrue(source.contains("setVisible(toolsWorkspaceContainer, toolsWorkspace)"));
        assertTrue(source.contains("setVisible(settingsWorkspaceContainer, settingsWorkspace)"));
        assertFalse(source.contains("setSearchFocusFabVisible("))
        assertTrue(
            source.contains(
                "templateWorkspaceBinder = new TemplateWorkspaceBinder("
            )
        )
        assertTrue(source.contains("createTemplateWorkspaceActions()"))
        assertTrue(source.contains("new TemplateWorkspaceBinder.GlobalPrefillActions()"))
        assertTrue(source.contains("new TemplateWorkspaceBinder.QuickTemplateActions()"))
        assertFalse(source.contains("GlobalPrefillActionsAdapter"))
        assertFalse(source.contains("QuickTemplateActionsAdapter"))
        assertTrue(source.contains("bindTemplateWorkspace()"));
        assertTrue(
            compact(source).contains(
                "templateWorkspaceBinder.bind( templateWorkspaceContainer, requireUiState().currentQuery() )"
            )
        )
        assertTrue(source.contains("STATE_TEMPLATE_QUERY"))
        assertTrue(source.contains("QuickTemplateSortDialog.show"))
        assertFalse(source.contains("searchFilterButton.setEnabled(appWorkspace)"));
        assertFalse(source.contains("applySearchClearButtonPosition(appWorkspace)"));
        assertFalse(source.contains("workspaceModeForButtonId(int checkedId)"))
        assertTrue(source.contains("new MainComposeShellHost("))
    }

    @Test
    fun restoreSnapshot_isNotBlockedBySavedStateBranch() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")

        val restoreSnapshotLine = source.indexOf(
            "initialAppsSnapshot = new ArrayList<>(retainedState.appsSnapshot)"
        )
        assertTrue(restoreSnapshotLine > 0)
        val beforeRestoreSnapshot = source.substring(0, restoreSnapshotLine)

        assertTrue(
            beforeRestoreSnapshot.contains("if (retainedState != null) {")
        )
        assertFalse(
            beforeRestoreSnapshot.contains("else if (retainedState != null)")
        )
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
        assertTrue(source.contains("private View landDetailPane"));
        assertTrue(source.contains("private View landDetailDivider"));
        assertTrue(source.contains("private FrameLayout templateDetailContent"));
        assertTrue(source.contains("TemplateDetailSelection"))
        assertTrue(source.contains("applyLandscapeDetailVisibility(appWorkspace, templateWorkspace)"));
        assertTrue(source.contains("appWorkspace || templateWorkspace"))
        assertTrue(source.contains("restoreTemplateDetailPane()"));
        assertTrue(source.contains("showGlobalPrefillEditor()"))
        assertTrue(source.contains("showQuickTemplateEditor(String templateId)"))
        assertTrue(source.contains("TemplateDetailSelection.quickTemplate(templateId)"))
        assertTrue(source.contains("showQuickTemplateEditor(null)"));
        assertFalse(source.contains("GlobalPrefillEditorBinder"))
        assertFalse(source.contains("QuickTemplateEditorBinder"))
        assertFalse(source.contains("GlobalPrefillSheetDialog"))
        assertFalse(source.contains("QuickTemplateEditSheetDialog"))
        assertTrue(source.contains("templateDetailSelection = TemplateDetailSelection.none()"));
        assertTrue(source.contains("TemplateDetailPaneController"))
        assertTrue(source.contains("TemplateDetailKind.QUICK_TEMPLATE_TARGETS"))
        assertTrue(source.contains("TemplateDetailSelection.quickTemplateTargets(templateId)"))
        assertTrue(source.contains("templateDetailPaneController.dispose()"));
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

        assertTrue(source.contains("restoreAppEditorForCurrentWorkspace()"));
        assertTrue(source.contains("private void restoreAppEditorForCurrentWorkspace()"))
        assertTrue(source.contains("requireUiState().workspaceMode != MainUiState.WorkspaceMode.APP"))
        assertTrue(source.contains("showEditBottomSheet(appItem)"));
        assertTrue(source.contains("showEditDetailPane(appItem)"));
        assertTrue(source.contains("private BottomSheetDialog activeAppEditorDialog"));
        assertTrue(source.contains("activeAppEditorDialog != null && activeAppEditorDialog.isShowing()"))
        assertTrue(source.contains("if (activeAppEditorDialog != null && activeAppEditorDialog.isShowing())"))
        assertTrue(source.contains("activeAppEditorDialog = dialog"));
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
        assertFalse(source.contains("bindLandscapeWorkspaceRailItemHeight()"));
        assertFalse(source.contains("workspaceSwitch instanceof NavigationRailView"))
        assertFalse(source.contains("availableHeight / railView.getMenu().size()"))
        assertTrue(dimensions.contains("main_land_workspace_rail_item_min_height\">64dp"))
        assertTrue(roundDimensions.contains("main_land_workspace_rail_item_min_height\">56dp"))
        assertFalse(source.contains("NavigationRailMenuView"))
    }

    @Test
    fun templateEditorDraftMigratesBetweenSheetAndPane() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val draft = read("src/main/java/com/dpis/module/templates/TemplateEditorDraft.java")
        val workspace = read(
                "src/main/java/com/dpis/module/templates/TemplateWorkspacePresentation.kt")

        assertTrue(source.contains("retainedGlobalPrefillDraft"))
        assertTrue(source.contains("retainedQuickTemplateDraft"))
        assertTrue(source.contains("retainedState.globalPrefillDraft"))
        assertTrue(source.contains("retainedState.quickTemplateDraft"))
        assertTrue(source.contains("retainedGlobalPrefillDraft"))
        assertTrue(source.contains("retainedQuickTemplateDraft"))
        assertTrue(source.contains("TemplateEditorDraft globalPrefillDraft"))
        assertTrue(source.contains("TemplateEditorDraft quickTemplateDraft"))
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

        assertTrue(
            source.contains("installedAppCatalogCoordinator.loadInstalledApps(")
        )
        assertTrue(coordinatorSource.contains("item.hyperOsNativeProxyCandidate,"))
        assertTrue(coordinatorSource.contains("ApplicationInfoFlags.of(0L)"))
        assertTrue(coordinatorSource.contains("getInstalledApplications(0)"))
        assertFalse(coordinatorSource.contains("GET_META_DATA"))
        assertTrue(source.contains("HyperOsNativeAppDetector.isNativeProxyCandidate("))
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

        assertTrue(source.contains("XIAOMI_GET_INSTALLED_APPS_PERMISSION"))
        assertTrue(
            source.contains("com.android.permission.GET_INSTALLED_APPS")
        )
        assertTrue(source.contains("requestPermissions("))
        assertTrue(source.contains("REQUEST_XIAOMI_GET_INSTALLED_APPS"))
        assertTrue(source.contains("onRequestPermissionsResult("))
        assertTrue(source.contains("installedAppsPermissionRequestCompleted"))
        assertTrue(
            source.contains("isXiaomiInstalledAppsPermissionDeclared()")
        )
        assertTrue(source.contains("getPermissionInfo("))
        assertTrue(
            source.contains(
                "dispatchMainUiAction(MainUiAction.requestAppsLoad(true))"
            )
        )
        val requestLoadStart = source.indexOf(
            "private void requestAppsLoad(boolean forceInstalledAppCatalogReload)"
        )
        val requestLoadEnd = source.indexOf(
            "private boolean ensureInstalledAppsPermissionBeforeLoad()",
            requestLoadStart
        )
        assertTrue(requestLoadStart >= 0)
        assertTrue(requestLoadEnd > requestLoadStart)
        val requestLoadBody = source.substring(
            requestLoadStart,
            requestLoadEnd
        )
        assertTrue(
            compact(requestLoadBody).indexOf(
                "ensureInstalledAppsPermissionBeforeLoad()"
            ) <
                compact(requestLoadBody).indexOf(
                    "dispatchMainUiAction("
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
        assertTrue(source.contains("maybeShowStartupDisclaimerDialog()"))
        assertTrue(
            source.contains("if (!maybeShowStartupDisclaimerDialog()) {")
        )
        assertTrue(
            source.contains(
                "updatePromptDialogCoordinator().maybeShowStartupDisclaimerDialog("
            )
        )
        assertTrue(
            source.contains(
                "new UpdatePromptDialogCoordinator.StartupDisclaimerAcceptance()"
            )
        )
        assertTrue(source.contains("new StartupDisclaimerStore(this)"))
        assertTrue(source.contains("return store.isAccepted()"));
        assertTrue(source.contains("return store.setAccepted(true)"));
        assertTrue(
            source.contains(
                "void applyLargeDialogWidth(androidx.appcompat.app.AlertDialog dialog)"
            )
        )
        assertTrue(
            source.contains(
                "DialogWindowSizer.applyLargeWidth(dialog, MainActivity.this)"
            )
        )
        val disclaimerBlock = source.substring(
            source.indexOf("private boolean maybeShowStartupDisclaimerDialog()"),
            source.indexOf("private boolean maybeShowModuleRuntimeReloadAdvice()")
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
        val binder = read(
            "src/main/java/com/dpis/module/home/HomeWorkspaceBinder.java"
        )
        val compose = read(
            "src/main/java/com/dpis/module/home/presentation/HomeWorkspaceContent.kt"
        )

        assertTrue(source.contains("startupUpdateCheckCoordinator.checkForUpdatesNow()"));
        assertTrue(binder.contains("state.updateState.subtitle(context)"))
        assertTrue(binder.contains("state.actions::checkForUpdates"))
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
        assertTrue(refreshBody.contains("requestAppsLoad(true)"));
    }

    @Test
    fun appLoad_reusesInstalledAppCatalogBetweenRefreshes() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val viewModelSource = read(
            "src/main/java/com/dpis/module/MainViewModel.kt"
        )
        val coordinatorSource = read(
            "src/main/java/com/dpis/module/applist/InstalledAppCatalogCoordinator.kt"
        )

        assertTrue(source.contains("INSTALLED_APP_CATALOG_TTL_MS"))
        assertTrue(source.contains("new InstalledAppCatalogCoordinator("))
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
        assertTrue(
            source.contains(
                "skipNextImmediateServiceReload = !initialAppsSnapshot.isEmpty()"
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

        assertTrue(source.contains("new GlobalPrefillStore("))
        assertTrue(source.contains("AppConfigPrefillPreview.applyIfEligible("))
        assertTrue(
            source.contains("dialogView, sheetItem, systemHooksEnabled")
        )
        assertTrue(
            source.contains(
                "private void showEditBottomSheet(AppListItem item)"
            )
        )
        assertTrue(
            source.contains("new AppConfigDialogBinder(")
        )
        assertTrue(source.contains("createAppConfigDialogHost()"))
        assertTrue(source.contains("binder.bind("))
        assertTrue(
            source.contains("new AppConfigDialogCoordinator(this).show(")
                    || compact(source).contains(
                            "new AppConfigDialogCoordinator(this).show("
                    )
        )
        assertTrue(
            source.contains("private void showEditDetailPane(")
        )
        assertTrue(source.contains("R.layout.view_land_app_detail"))
        assertTrue(source.contains("new LandAppDetailPaneBinder("))
        assertTrue(
            source.contains(
                "saveAppConfigDraft("
            )
        )
        assertTrue(source.contains("editorItem"))
        assertTrue(source.contains("state,"))
        assertTrue(
            source.contains("showLandDetailTypefaceSelector(")
        )
        assertTrue(
            source.contains("showLandDetailHookDomains(editorItem, state, onChanged)")
        )
        assertTrue(
            compact(source).contains(
                "toggleLandDetailScope( editorItem, currentlyInScope, onTurnedInScope, onTurnedOutScope )"
            )
        )
        assertTrue(
            source.contains(
                "public boolean setDpisEnabled(String packageName, boolean enabled)"
            )
        )
        assertFalse(source.contains("resetLandDetailConfig(editorItem)"));
        assertTrue(source.contains("appConfigSaveHandler.saveResolved("))
        assertTrue(source.contains("updateEditingDraft(state)"));
        assertTrue(source.contains("void onDraftStateChanged("))
        assertTrue(source.contains("if (draft == null && mainViewModel != null)"))
        assertTrue(
            read("src/main/java/com/dpis/module/appconfig/LandAppDetailPaneBinder.java")
                .contains("AppConfigDialogBinder.AppConfigDialogState.fromItem(item)")
        )
        assertTrue(
            source.contains("executeDialogProcessAction(processItem, action)")
        )
        assertTrue(compact(source).contains("landDetailContent.addView( dialogView"))
        assertTrue(source.contains("ViewGroup.LayoutParams.MATCH_PARENT"))
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
            "private void showEditBottomSheet(AppListItem item)",
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
            "src/main/java/com/dpis/module/appconfig/LandAppDetailPaneBinder.java"
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
        val clearanceStart = binder.indexOf("private void updateScrollContentClearance(")
        val clearanceEnd = binder.indexOf(
                "private static boolean updateSaveButtonState(",
                clearanceStart
        )
        val clearanceBlock = binder.substring(clearanceStart, clearanceEnd)
        assertTrue(clearanceBlock.contains("R.id.land_detail_scroll_content"))
        assertTrue(clearanceBlock.contains("content.setPaddingRelative("))
        assertFalse(clearanceBlock.contains("MarginLayoutParams"))
        assertFalse(clearanceBlock.contains("bottomMargin"))
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
        assertTrue(binder.contains("getPackageManager()"))
        assertTrue(binder.contains("getPackageInfo(item.packageName, 0)"))
        assertTrue(binder.contains("void saveDraft("))
        assertTrue(binder.contains("AppListItem item,"))
        assertTrue(binder.contains("actions.saveDraft("))
        assertTrue(
            binder.contains("AppConfigDialogBinder.bindViewportModeToggle(")
        )
        assertTrue(
            binder.contains("AppConfigDialogBinder.bindFontModeToggle(")
        )
        assertTrue(binder.contains("actions.toggleScope("))
        assertTrue(binder.contains("actions.startFeedbackDiagnostic(item, state)"));
        assertTrue(binder.contains("state.scopeSelected,"))
        assertTrue(compact(binder).contains("actions.setDpisEnabled(item.packageName, nextEnabled)"))
        assertTrue(binder.contains("resetDraft("))
        assertTrue(binder.contains("root,"))
        assertTrue(binder.contains("item,"))
        assertTrue(binder.contains("FormInputFocusBinder.bindDismissOnOutsideTouch"))
        assertTrue(binder.contains("FormInputFocusBinder.clearFocusAndHideIme"))
        assertTrue(binder.contains("WechatDpiSheetBinder.inputViewForFocus(root)"))
        assertTrue(binder.contains("state.clearViewportInputs()"));
        assertTrue(binder.contains("WechatDpiSheetBinder.clearDraft(root)"));
        assertTrue(
            binder.contains(
                "AppConfigDialogBinder.AppConfigDialogState.fromItem(item)"
            )
        )
        assertTrue(
            compact(binder).contains(
                "root.setTag(R.id.land_detail_hook_chain_row, state)"
            )
        )
        assertTrue(
            compact(binder).contains(
                "root.getTag(R.id.land_detail_hook_chain_row)"
            )
        )
        assertTrue(
            compact(binder).contains(
                "root.setTag( R.id.land_detail_save_button, signature != null ? signature : \"\" )"
            )
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
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val methodStart = source.indexOf("private void applyLandDetailContentInsets")
        val methodEnd = source.indexOf("private void showToast", methodStart)

        assertTrue(methodStart >= 0)
        assertTrue(methodEnd > methodStart)
        val methodBody = source.substring(methodStart, methodEnd)
        assertTrue(methodBody.contains("detailView.findViewById(R.id.land_detail_scroll)"))
        assertTrue(methodBody.contains(
                "WindowInsetsBinder.applySafeDrawingPadding(scrollView, false, true, false, true)"
        ))
        assertFalse(methodBody.contains("ViewCompat.requestApplyInsets(scrollView)"));
        assertTrue(source.contains("applyLandDetailContentInsets(dialogView)"));
        assertTrue(source.contains("ViewCompat.requestApplyInsets(scrollView)"));
    }

    @Test
    fun appConfigAndProcessActions_delegateToDedicatedHandlers() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")

        assertTrue(source.contains(
                "new ProcessActionHandler(this, this::syncRuntimePropertiesForTargetLaunch)"))
        assertTrue(source.contains("new AppConfigSaveHandler()"))
        assertTrue(
            source.contains("processActionHandler.execute(item, mappedAction)")
        )
        assertTrue(source.contains("appConfigSaveHandler.save("))
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

        assertTrue(source.contains("loadInstalledApps(forceInstalledAppCatalogReload)"))
        assertTrue(coordinatorSource.contains("val catalog = loadInstalledAppCatalog("))
        assertTrue(coordinatorSource.contains("applicationInfo.loadIcon(packageManager)"))
        assertFalse(coordinatorSource.contains("icon = loadApplicationIcon(packageManager, applicationInfo)"));
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
        assertTrue(source.contains("systemScopeCoordinator.toggleScope("))
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
            "src/main/java/com/dpis/module/DpisApplication.java"
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

        assertTrue(
            source.contains("public void showFontHookDomains(")
        )
        assertTrue(
            source.contains("AppConfigDialogBinder.AppConfigDialogState state,")
        )
        assertTrue(
            compact(source).contains(
                "MainActivity.this.showFontHookDomains( item, state, onStateChanged )"
            )
        )
        assertTrue(
            source.contains("public String getFontHookDomainsButtonText(")
        )
        assertTrue(
            source.contains("AppConfigDialogBinder.AppConfigDialogState state")
        )
        assertTrue(
            source.contains("MainActivity.this.getFontHookDomainsButtonText(")
        )
        assertTrue(source.contains("resolveFontHookDomainsForDraft(item, state)"))
        assertTrue(source.contains("new HookDomainOverrideStore(getHookConfigStore()).read("))
        assertTrue(source.contains("FontHookDomainDialog.show("))
        assertTrue(source.contains("isFontHookDomainEditingEnabled()"))
        assertTrue(source.contains("AppConfigDialogBinder.resolveFontMode(findFontModeToggle(root))"))
        assertTrue(source.contains("this,"))
        val saveSource = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.kt")
        assertFalse(
            saveSource.contains("FontRuntimePropertySyncer.publishTargetAsync(")
        )
        assertTrue(source.contains("scheduleRuntimePropertiesForTargetLaunch(packageName)"));
        assertTrue(source.contains("FontRuntimePropertySyncer.syncTarget(packageName, store)"));
        assertTrue(source.contains("FontHookDomainRegistry.recommendedTemplateKnownDomains()"))
        assertFalse(source.contains("AppProcessHookInstaller.resolveDebugFontOverrideForPackage("))
        assertTrue(source.contains("FontHookDomainPresentation.forOverride("))
        assertTrue(source.contains(".buttonText(this)"));
        assertFalse(source.contains("item.fontScalePercent != null && item.fontScalePercent > 0"))
        assertFalse(source.contains("publishFontRuntimeTarget("))
    }

    @Test
    fun fontHookDomainEditorUsesDraftStateOnly() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val methodStart = source.indexOf(
            "private void showFontHookDomains("
        )
        val methodEnd = source.indexOf(
            "String getFontHookDomainsButtonText",
            methodStart
        )
        val method = source.substring(methodStart, methodEnd)

        assertTrue(
            compact(method).contains(
                "HookDomainOverride currentOverride = resolveFontHookDomainsForDraft(item, state)"
            )
        )
        assertTrue(
            compact(method).contains(
                "state.draftFontHookDomainsRaw = HookDomainOverrideStore.rawValueForSelection("
            )
        )
        assertTrue(method.contains("state.draftFontHookDomainsRaw = null"));
        assertTrue(
            compact(method).contains(
                "state.viewportApplyMode = ViewportApplyMode.normalize(mode)"
            )
        )
        assertTrue(
            compact(method).contains(
                "state != null ? state.viewportApplyMode : store.getTargetViewportApplyMode(item.packageName)"
            )
        )
        val compactMethod = compact(method)
        assertFalse(compactMethod.contains("saveCustomIfDifferentFromAutomatic("))
        assertFalse(compactMethod.contains("restoreRecommended(packageName)"))
        assertFalse(compactMethod.contains("store.setTargetViewportApplyMode("))
        assertFalse(compactMethod.contains("requestAppsLoad()"));
    }

    @Test
    fun fontHookDomainButtonTextUsesMutablePreviewStateFlag() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val methodStart = source.indexOf(
            "String getFontHookDomainsButtonText("
        )
        val methodEnd = source.indexOf(
            "Set<String> recommendedTemplateFontHookDomains",
            methodStart
        )
        val method = source.substring(methodStart, methodEnd)

        assertTrue(method.contains("FontHookDomainPresentation.forOverride("))
        assertTrue(source.contains("FontHookDomainPresentation"))
        assertTrue(method.contains("AppConfigDialogBinder.AppConfigDialogState state"))
        assertFalse(method.contains("item.previewFromGlobalPrefill"))
    }

    @Test
    fun composeTemplateEditorBridgesSelectionDraftAndCloseLifecycle() {
        val activity = read("src/main/java/com/dpis/module/MainActivity.java")
        val coordinator = read(
            "src/main/java/com/dpis/module/MainWorkspacePresentationCoordinator.kt")
        val workspace = read(
            "src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceContent.kt")
        val editorSurface = read(
            "src/main/java/com/dpis/module/templates/presentation/TemplateEditorContent.kt")
        val editorSheet = read(
            "src/main/java/com/dpis/module/ui/presentation/DpisEditorBottomSheet.kt")

        assertTrue(activity.contains("onComposeTemplateEditorOpened(quickTemplate, templateId)"))
        assertTrue(activity.contains("boolean dirty = form.isDirty()"));
        assertTrue(activity.contains("retainedGlobalPrefillDraft = dirty ? form.globalDraft() : null"));
        assertTrue(activity.contains("retainedQuickTemplateDraft = dirty ? form.quickDraft() : null"));
        assertTrue(activity.contains("private void onComposeTemplateEditorClosed()"))
        assertTrue(activity.contains("clearTemplateDetailSelection()"));
        val closeStart = activity.indexOf("private void onComposeTemplateEditorClosed()")
        val closeEnd = activity.indexOf("private void showQuickTemplateEditor", closeStart)
        val closeMethod = activity.substring(closeStart, closeEnd)
        assertTrue(closeMethod.indexOf("clearTemplateDetailSelection()")
                < closeMethod.indexOf("bindTemplateWorkspace()"));
        assertTrue(coordinator.contains("onEditorOpened ="))
        assertTrue(coordinator.contains("onEditorChanged = content::updateTemplateEditor"))
        assertTrue(coordinator.contains("onEditorClosed = content::closeTemplateEditor"))
        assertTrue(workspace.contains("onEditorOpened: (quickTemplate: Boolean, templateId: String?)"))
        assertTrue(workspace.contains("onEditorChanged: (TemplateEditorForm) -> Unit"))
        assertTrue(workspace.contains("onEditorClosed: () -> Unit"))
        assertTrue(workspace.contains("onEditorOpened(kind == EDITOR_QUICK, templateId)"))
        assertTrue(workspace.contains("onEditorChanged(editorDraft.form)"))
        assertTrue(workspace.contains("onEditorClosed()"))
        assertTrue(workspace.contains(
                "val createdNewTemplate = editorDraft.form.quickTemplate && editorDraft.form.newTemplate"))
        assertTrue(workspace.contains("if (createdNewTemplate)"))
        assertTrue(workspace.contains("var editorSheetVisible"))
        assertTrue(workspace.contains("var editorSheetClosing"))
        assertTrue(workspace.contains("fun finishEditorClose()"))
        assertTrue(workspace.contains("sheetVisible = editorSheetVisible"))
        assertTrue(workspace.contains(
                "onSheetHidden = { if (editorSheetClosing) finishEditorClose() }"))
        assertTrue(editorSurface.contains("sheetVisible: Boolean = true"))
        assertTrue(editorSurface.contains("onSheetHidden: () -> Unit = {}"))
        assertTrue(editorSurface.contains("visible = sheetVisible"))
        assertTrue(editorSurface.contains("onHidden = onSheetHidden"))
        assertTrue(editorSheet.contains("if (visible)"))
        assertTrue(editorSheet.contains("sheetState.hide()"))
        assertTrue(editorSheet.contains("onHidden()"))
        val workspaceCloseStart = workspace.indexOf("fun closeEditor()")
        val workspaceCloseEnd = workspace.indexOf("fun saveEditor()", workspaceCloseStart)
        val workspaceClose = workspace.substring(workspaceCloseStart, workspaceCloseEnd)
        assertFalse(workspaceClose.contains("onEditorDestinationChanged"))
        assertTrue(workspace.contains("closeEditor()"))
        assertTrue(workspace.contains("@Preview(showBackground = true"))
    }

    @Test
    fun composeTemplateRestorePublishesDetailWithoutLegacyEditorFallback() {
        val activity = read("src/main/java/com/dpis/module/MainActivity.java")
        val methodStart = activity.indexOf(
                "private void restoreTemplateEditorForCurrentConfiguration()")
        val methodEnd = activity.indexOf(
                "private void clearTemplateDetailSelection()", methodStart)
        val method = activity.substring(methodStart, methodEnd)

        val composeBranch = method.indexOf("if (composeShellHost != null)")
        assertTrue(composeBranch >= 0)
        assertTrue(method.contains("bindTemplateWorkspace()"));
        assertFalse(method.contains("showGlobalPrefillSheet"))
        assertFalse(method.contains("showQuickTemplateSheet"))
        assertFalse(method.contains("closeActiveTemplateSheetForMigration"))
    }

    private fun read(relativePath: String): String {
        return SourceSmokeTestPaths.read(relativePath)
    }

    private fun stringEntry(source: String, name: String): String {
        val marker = "name=\"" + name + "\""
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
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val methodStart = source.indexOf(
            "private boolean shouldPrepareHyperOsNativeProxyForRestart"
        )
        val methodEnd = source.indexOf(
            "private static boolean hasActiveStoredConfig",
            methodStart
        )
        assertTrue(methodStart >= 0)
        assertTrue(methodEnd > methodStart)
        val methodBody = source.substring(methodStart, methodEnd)

        assertTrue(
            source.contains("executeDialogProcessActionAfterHyperOsProxyReady")
        )
        assertTrue(source.contains("AppConfigInputValidation.parseViewportTargetSpec("))
        assertFalse(source.contains("ViewportTargetSpec.relativeScale(viewportValue * 10)"))
        assertTrue(
            source.contains("shouldPrepareHyperOsNativeProxyForRestart(item)")
        )
        assertTrue(
            methodBody.contains("DpisConfigStore store = getHookConfigStore()")
        )
        assertTrue(
            methodBody.contains("store.isTargetDpisEnabled(item.packageName)")
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
                "executeHyperOsNativeProxyMount(item, true, success ->"
            )
        )
        assertTrue(source.contains("if (success)"))
        assertTrue(
            source.contains("processActionHandler.execute(item, mappedAction)")
        )
    }
}
