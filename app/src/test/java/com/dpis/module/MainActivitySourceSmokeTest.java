package com.dpis.module;

import com.dpis.module.appconfig.LandAppDetailPaneBinder;

import com.dpis.module.fonts.HyperOsNativeProxyRefreshCoordinator;

import com.dpis.module.settings.SystemScopeCoordinator;

import com.dpis.module.applist.InstalledAppCatalogCoordinator;


import com.dpis.module.fonts.FontApplyMode;



import com.dpis.module.appconfig.AppConfigDialogBinder;
import com.dpis.module.appconfig.AppConfigInputValidation;
import com.dpis.module.appconfig.AppConfigPrefillPreview;
import com.dpis.module.appconfig.AppConfigSaveHandler;

import com.dpis.module.runtime.appprocess.AppProcessHookInstaller;

import com.dpis.module.applist.AppStatusFormatter;

import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry;

import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation;

import com.dpis.module.fonts.hookdomain.FontHookDomainDialog;

import com.dpis.module.runtime.font.FontRuntimePropertySyncer;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import com.dpis.module.applist.AppListFilter;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.AppListPage;
import com.dpis.module.hooks.HookDomainOverride;
import com.dpis.module.hooks.HookDomainOverrideStore;

import com.dpis.module.quirks.WechatDpiSheetBinder;
import com.dpis.module.templates.QuickTemplateTargetsBinder;

import com.dpis.module.templates.QuickTemplateSortDialog;

import com.dpis.module.templates.GlobalPrefillStore;

import com.dpis.module.applist.AppListFilterState;

import com.dpis.module.home.HomeUpdateUiState;
import com.dpis.module.home.HomeWorkspaceBinder;

import com.dpis.module.ui.DialogWindowSizer;

import com.dpis.module.updates.UpdateStateStore;

import com.dpis.module.updates.UpdateManifestFetcher;

import com.dpis.module.updates.UpdateDownloadCoordinator;

import com.dpis.module.updates.UpdateCoordinator;

import com.dpis.module.updates.StartupUpdatePackageHandler;

import com.dpis.module.updates.StartupUpdateManifest;

import com.dpis.module.updates.StartupUpdateCheckOnce;

import com.dpis.module.updates.StartupUpdateCheckCoordinator;

import com.dpis.module.updates.ReleaseNotesController;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

public class MainActivitySourceSmokeTest {

    @Test
    public void mainActivityRetainsHelpFabWiring() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String layout = read("src/main/res/layout/activity_status.xml");

        assertTrue(source.contains("searchFocusFab = findViewById(R.id.search_focus_fab);"));
        assertTrue(source.contains("FormInputFocusBinder.isInsideAny("));
        assertTrue(source.contains("clearSearchFocus();"));
        assertTrue(source.contains("return true;"));
        assertTrue(source.contains("searchFocusFab.setOnClickListener"));
        assertTrue(layout.contains("@+id/search_focus_fab"));
    }

    @Test
    public void formInputFocusCanMoveFocusToFallbackView() throws IOException {
        String source = read("src/main/java/com/dpis/module/ui/FormInputFocusBinder.java");

        assertTrue(source.contains("fallbackFocusView.setFocusable(true);"));
        assertTrue(source.contains("fallbackFocusView.setFocusableInTouchMode(true);"));
        assertTrue(source.contains("fallbackFocusView.requestFocus();"));
        assertTrue(source.contains("hideSoftInputFromWindow("));
    }

    @Test
    public void landDetailSaveRequestsScopeAfterSuccessfulSave() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String binder = read("src/main/java/com/dpis/module/appconfig/LandAppDetailPaneBinder.java");

        assertTrue(binder.contains("void saveDraft("));
        assertTrue(binder.contains("AppConfigDialogBinder.AppConfigDialogState state,"));
        assertTrue(source.contains("requestLandDetailScopeAfterSuccessfulSave(item, state);"));
        assertTrue(source.contains("!state.scopeKnown"));
        assertTrue(source.contains("state.scopeSelected"));
        assertTrue(source.contains("state.scopeRequestPending"));
        assertTrue(source.contains("systemScopeCoordinator.requestScope("));
        assertTrue(source.contains("showToast(R.string.save_scope_request_notice);"));
    }

    @Test
    public void mainActivityWiresPagerMediatorAndFilterEntry()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("R.id.workspace_switch"));
        assertTrue(source.contains("R.id.workspace_app_button"));
        assertTrue(source.contains("R.id.workspace_template_button"));
        assertTrue(source.contains("R.id.search_focus_fab"));
        assertTrue(source.contains("searchFilterButton.setOnClickListener"));
        assertTrue(source.contains("focusSearchInputAndShowKeyboard()"));
        assertTrue(!source.contains("RichTextDialog.show("));
        assertTrue(source.contains("searchFocusFab.setOnClickListener"));
        assertTrue(source.contains("bindFabTouchFeedback(searchFocusFab);"));
        assertTrue(
            source.contains(
                "private void bindFabTouchFeedback(FloatingActionButton fab)"
            )
        );
        assertTrue(
            source.contains("TouchFeedbackBinder.bindPressScaleAndHaptic(fab);")
        );
        assertTrue(source.contains("focusSearchInputAndShowKeyboard()"));
        assertTrue(source.contains("hideSearchFocusFab()"));
        assertTrue(source.contains("showSearchFocusFab()"));
        assertTrue(source.contains("R.dimen.floating_actions_hide_offset_y"));
        assertTrue(source.contains(".translationY(getResources().getDimensionPixelSize("));
        assertFalse(source.contains(".translationY(searchTargetTranslationY)"));
        assertFalse(source.contains("searchFocusFab.getHeight() +"));
        assertTrue(source.contains("showFilterDialog()"));
        assertTrue(source.contains("new AppListFilterState("));
        assertTrue(source.contains("AppFilterComposeSheet.show(this"));
        assertTrue(!source.contains("R.id.filter_apply_button"));
        assertTrue(!source.contains("R.id.filter_reset_button"));
    }

    @Test
    public void searchFabPolicySurvivesListRefreshAndExcludesLandscapeDetail()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("private Boolean searchFabPolicyVisible;"));
        assertTrue(source.contains("private boolean shouldShowFloatingAppSearch("));
        assertTrue(source.contains(
                "if (searchFabPolicyVisible != null && searchFabPolicyVisible == visible)"));
        assertTrue(source.contains("searchFabPolicyVisible = visible;"));
        assertTrue(source.contains("!isLandscapeDetailMode()"));
    }

    @Test
    public void workspaceSwitchHidesAppControlsInTemplateWorkspace()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("STATE_WORKSPACE_MODE"));
        assertTrue(source.contains("MainUiState.WorkspaceMode.fromName("));
        assertTrue(source.contains("bindWorkspaceSwitch()"));
        assertTrue(
            source.contains("workspaceSwitch.setOnItemSelectedListener")
        );
        assertTrue(
            source.contains("private boolean updatingWorkspaceSelection;")
        );
        assertTrue(
            source.contains("private void selectWorkspaceItem(int itemId)")
        );
        assertTrue(source.contains("MainUiAction.workspaceModeChanged("));
        assertTrue(source.contains("applyWorkspaceMode(state.workspaceMode);"));
        assertTrue(
            source.contains(
                "boolean appWorkspace = mode == MainUiState.WorkspaceMode.APP;"
            )
        );
        assertFalse(source.contains("private void updateWatchFilterTabsScrollOffset(int dy)"));
        assertTrue(source.contains(
                "setVisible(templateWorkspaceContainer, templateWorkspace);"
        ));
        assertTrue(source.contains("setVisible(toolsWorkspaceContainer, toolsWorkspace);"));
        assertTrue(source.contains("setVisible(settingsWorkspaceContainer, settingsWorkspace);"));
        assertTrue(
            source.contains(
                "boolean floatingActionsVisible"
            )
        );
        assertTrue(compact(source).contains(
                "boolean floatingActionsVisible = appWorkspace && !isLandscapeDetailMode()"));
        assertTrue(source.contains("&& WatchUiMode.shouldUseFloatingAppSearch(this);"));
        assertTrue(
            source.contains(
                "setSearchFocusFabVisible(floatingActionsVisible);"
            )
        );
        assertTrue(source.contains("private void setSearchFocusFabVisible(boolean visible)"));
        assertTrue(source.contains("searchFocusFab.setTranslationY(0f);"));
        assertTrue(source.contains("searchFabHidden = false;"));
        assertTrue(
            !source.contains("setVisible(helpFab, floatingActionsVisible);")
        );
        assertTrue(
            source.contains(
                "templateWorkspaceBinder = new TemplateWorkspaceBinder("
            )
        );
        assertTrue(source.contains("createTemplateWorkspaceActions()"));
        assertTrue(source.contains("bindTemplateWorkspace();"));
        assertTrue(
            compact(source).contains(
                "templateWorkspaceBinder.bind( templateWorkspaceContainer, requireUiState().currentQuery() );"
            )
        );
        assertTrue(source.contains("R.string.template_search_hint"));
        assertTrue(source.contains("STATE_TEMPLATE_QUERY"));
        assertTrue(source.contains("QuickTemplateSortDialog.show"));
        assertTrue(
            source.contains("searchFilterButton.setEnabled(appWorkspace);")
        );
        assertTrue(
            compact(source).contains(
                "searchFilterButton.setVisibility( appWorkspace ? View.VISIBLE : View.GONE );"
            )
        );
        assertTrue(source.contains("applySearchClearButtonPosition(appWorkspace);"));
        assertTrue(source.contains("private void applySearchClearButtonPosition(boolean filterButtonVisible)"));
        assertTrue(source.contains("R.dimen.main_search_action_pair_padding"));
        assertTrue(source.contains("R.dimen.main_search_action_icon_padding_end"));
        assertTrue(source.contains("workspaceModeForButtonId(int checkedId)"));
        assertTrue(
            source.contains("checkedId == R.id.workspace_template_button")
        );
    }

    @Test
    public void restoreSnapshot_isNotBlockedBySavedStateBranch()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        int restoreSnapshotLine = source.indexOf(
            "initialAppsSnapshot = new ArrayList<>(retainedState.appsSnapshot);"
        );
        assertTrue(restoreSnapshotLine > 0);
        String beforeRestoreSnapshot = source.substring(0, restoreSnapshotLine);

        assertTrue(
            beforeRestoreSnapshot.contains("if (retainedState != null) {")
        );
        assertTrue(
            !beforeRestoreSnapshot.contains("else if (retainedState != null)")
        );
    }

    @Test
    public void composeTemplateWorkspaceKeepsTargetSelectionFallbackOnly()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String layout = read("src/main/res/layout-land/activity_status.xml");
        String targetsDetail = read("src/main/res/layout/view_land_quick_template_targets_detail.xml");

        assertTrue(layout.contains("android:id=\"@+id/land_detail_content\""));
        assertTrue(layout.contains("android:id=\"@+id/template_detail_content\""));
        assertTrue(layout.contains("android:id=\"@+id/template_detail_empty\""));
        assertTrue(layout.contains("android:id=\"@+id/land_detail_divider\""));
        assertTrue(source.contains("private View landDetailPane;"));
        assertTrue(source.contains("private View landDetailDivider;"));
        assertTrue(source.contains("private FrameLayout templateDetailContent;"));
        assertTrue(source.contains("TemplateDetailSelection"));
        assertTrue(source.contains("applyLandscapeDetailVisibility(appWorkspace, templateWorkspace);"));
        assertTrue(source.contains("appWorkspace || templateWorkspace"));
        assertTrue(source.contains("restoreTemplateDetailPane();"));
        assertTrue(source.contains("showGlobalPrefillEditor()"));
        assertTrue(source.contains("showQuickTemplateEditor(String templateId)"));
        assertTrue(source.contains("TemplateDetailSelection.quickTemplate(templateId)"));
        assertTrue(source.contains("showQuickTemplateEditor(null);"));
        assertFalse(source.contains("GlobalPrefillEditorBinder"));
        assertFalse(source.contains("QuickTemplateEditorBinder"));
        assertFalse(source.contains("GlobalPrefillSheetDialog"));
        assertFalse(source.contains("QuickTemplateEditSheetDialog"));
        assertTrue(source.contains("templateDetailSelection = TemplateDetailSelection.none();"));
        assertTrue(source.contains("R.layout.view_land_quick_template_targets_detail"));
        assertTrue(source.contains("TemplateDetailKind.QUICK_TEMPLATE_TARGETS"));
        assertTrue(source.contains("TemplateDetailSelection.quickTemplateTargets(templateId)"));
        assertTrue(source.contains("activeQuickTemplateTargetsBinder.dispose();"));
        assertFalse(source.contains("? R.layout.dialog_global_prefill_sheet"));
        assertFalse(source.contains(": R.layout.dialog_quick_template_edit_sheet"));
        assertTrue(targetsDetail.contains("android:id=\"@+id/quick_template_targets_detail_root\""));
        assertTrue(targetsDetail.contains("android:id=\"@+id/quick_template_targets_list\""));
        assertTrue(targetsDetail.contains("android:id=\"@+id/quick_template_targets_save_button\""));
        assertTrue(targetsDetail.contains("@dimen/land_template_detail_subtitle_spacing_top"));
        assertFalse(targetsDetail.contains("@dimen/land_app_identity_secondary_spacing_top"));
        assertFalse(targetsDetail.contains("quick_template_targets_back_button"));
        assertFalse(targetsDetail.contains("@layout/activity_quick_template_targets"));
        assertTrue(targetsDetail.contains("android:id=\"@+id/quick_template_targets_detail_root\""));
    }

    @Test
    public void appEditorRestoreIsScopedToAppWorkspace() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("restoreAppEditorForCurrentWorkspace();"));
        assertTrue(source.contains("private void restoreAppEditorForCurrentWorkspace()"));
        assertTrue(source.contains("requireUiState().workspaceMode != MainUiState.WorkspaceMode.APP"));
        assertTrue(source.contains("showEditBottomSheet(appItem);"));
        assertTrue(source.contains("showEditDetailPane(appItem);"));
        assertTrue(source.contains("private BottomSheetDialog activeAppEditorDialog;"));
        assertTrue(source.contains("activeAppEditorDialog != null && activeAppEditorDialog.isShowing()"));
        assertTrue(source.contains("if (activeAppEditorDialog != null && activeAppEditorDialog.isShowing())"));
        assertTrue(source.contains("activeAppEditorDialog = dialog;"));
    }

    @Test
    public void landscapeWorkspaceRailUsesCompactMaterialItemHeightAndScrollsWhenNeeded()
            throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String landLayout = read("src/main/res/layout-land/activity_status.xml");
        String dimensions = read("src/main/res/values/dimens.xml");
        String roundDimensions = read("src/main/res/values-round/dimens.xml");

        assertTrue(landLayout.contains("com.google.android.material.navigationrail.NavigationRailView"));
        assertTrue(landLayout.contains("android:id=\"@+id/workspace_switch_scroll\""));
        assertTrue(landLayout.contains("android:fillViewport=\"true\""));
        assertTrue(landLayout.contains("app:labelVisibilityMode=\"selected\""));
        assertTrue(source.contains("bindLandscapeWorkspaceRailItemHeight();"));
        assertTrue(source.contains("workspaceSwitch instanceof NavigationRailView"));
        assertTrue(source.contains("applyCompactLandscapeWorkspaceRailItemHeight();"));
        assertTrue(source.contains("R.dimen.main_land_workspace_rail_item_min_height"));
        assertTrue(source.contains("railView.setItemMinimumHeight(itemHeight);"));
        assertTrue(!source.contains("availableHeight / railView.getMenu().size()"));
        assertTrue(dimensions.contains("main_land_workspace_rail_item_min_height\">64dp"));
        assertTrue(roundDimensions.contains("main_land_workspace_rail_item_min_height\">56dp"));
        assertTrue(!source.contains("NavigationRailMenuView"));
    }

    @Test
    public void templateEditorDraftMigratesBetweenSheetAndPane() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String draft = read("src/main/java/com/dpis/module/templates/TemplateEditorDraft.java");
        String workspace = read(
                "src/main/java/com/dpis/module/templates/TemplateWorkspacePresentation.kt");

        assertTrue(source.contains("retainedGlobalPrefillDraft"));
        assertTrue(source.contains("retainedQuickTemplateDraft"));
        assertTrue(source.contains("retainedState.globalPrefillDraft"));
        assertTrue(source.contains("retainedState.quickTemplateDraft"));
        assertTrue(source.contains("retainedGlobalPrefillDraft"));
        assertTrue(source.contains("retainedQuickTemplateDraft"));
        assertTrue(source.contains("TemplateEditorDraft globalPrefillDraft"));
        assertTrue(source.contains("TemplateEditorDraft quickTemplateDraft"));
        assertTrue(draft.contains("viewportScaleInput"));
        assertTrue(draft.contains("viewportAbsoluteInput"));
        assertTrue(workspace.contains("globalPrefillDraft: TemplateEditorDraft?"));
        assertTrue(workspace.contains("quickTemplateDraft: TemplateEditorDraft?"));
        assertFalse(source.contains("GlobalPrefillEditorBinder"));
        assertFalse(source.contains("QuickTemplateEditorBinder"));
    }

    @Test
    public void loadInstalledApps_publishesRowsBeforeIcons() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String coordinatorSource = read(
            "src/main/java/com/dpis/module/applist/InstalledAppCatalogCoordinator.java"
        );
        String iconSource = read("src/main/java/com/dpis/module/ui/compose/InstalledAppIcon.kt");
        String workspaceSource = read("src/main/java/com/dpis/module/ui/compose/AppWorkspaceContent.kt");

        assertTrue(
            source.contains("installedAppCatalogCoordinator.loadInstalledApps(")
        );
        assertTrue(coordinatorSource.contains("item.hyperOsNativeProxyCandidate, true, null"));
        assertTrue(coordinatorSource.contains("ApplicationInfoFlags.of(0L)"));
        assertTrue(coordinatorSource.contains("getInstalledApplications(0)"));
        assertFalse(coordinatorSource.contains("GET_META_DATA"));
        assertTrue(source.contains("HyperOsNativeAppDetector.isNativeProxyCandidate("));
        assertTrue(iconSource.contains("produceState<Drawable?>"));
        assertTrue(iconSource.contains("InstalledAppIconCache.load"));
        assertTrue(workspaceSource.contains("rememberInstalledAppIcon(item.packageName, item.icon)"));
        assertFalse(workspaceSource.contains("preloadIcons("));
        assertTrue(!coordinatorSource.contains("getDefaultActivityIcon()"));
    }

    @Test
    public void appLoad_requestsXiaomiInstalledAppsPermissionBeforeQueryingPackages()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("XIAOMI_GET_INSTALLED_APPS_PERMISSION"));
        assertTrue(
            source.contains("com.android.permission.GET_INSTALLED_APPS")
        );
        assertTrue(source.contains("requestPermissions("));
        assertTrue(source.contains("REQUEST_XIAOMI_GET_INSTALLED_APPS"));
        assertTrue(source.contains("onRequestPermissionsResult("));
        assertTrue(source.contains("installedAppsPermissionRequestCompleted"));
        assertTrue(
            source.contains("isXiaomiInstalledAppsPermissionDeclared()")
        );
        assertTrue(source.contains("getPermissionInfo("));
        assertTrue(
            source.contains(
                "dispatchMainUiAction(MainUiAction.requestAppsLoad(true));"
            )
        );
        int requestLoadStart = source.indexOf(
            "private void requestAppsLoad(boolean forceInstalledAppCatalogReload)"
        );
        int requestLoadEnd = source.indexOf(
            "private boolean ensureInstalledAppsPermissionBeforeLoad()",
            requestLoadStart
        );
        assertTrue(requestLoadStart >= 0);
        assertTrue(requestLoadEnd > requestLoadStart);
        String requestLoadBody = source.substring(
            requestLoadStart,
            requestLoadEnd
        );
        assertTrue(
            compact(requestLoadBody).indexOf(
                "ensureInstalledAppsPermissionBeforeLoad()"
            ) <
                compact(requestLoadBody).indexOf(
                    "dispatchMainUiAction("
                )
        );
    }

    @Test
    public void savesAndRestoresPageScrollStatesForRotation()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("STATE_PAGE_SCROLL_STATES"));
        assertTrue(source.contains("putSparseParcelableArray("));
    }

    @Test
    public void startupDisclaimerUsesMaterialDialogAndPersistsConsent()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String runtimeLayout = read(
            "src/main/java/com/dpis/module/ui/compose/LocalToolDialogs.kt"
        );
        String strings = read("src/main/res/values/strings.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(source.contains("maybeShowModuleRuntimeReloadAdvice()"));
        assertTrue(
            source.contains(
                "ModuleRuntimeReloadAdvisor.shouldShowReloadAdvice(this)"
            )
        );
        assertTrue(
            source.contains(
                "ModuleRuntimeReloadAdvisor.markReloadAdviceAcknowledged(this)"
            )
        );
        assertTrue(
            source.contains("ModuleRuntimeReloadComposeDialog.show(this")
        );
        assertTrue(runtimeLayout.contains("DialogWindowSizer.applyStandardWidth(dialog, activity)"));
        assertTrue(!source.contains("ModuleRuntimeReloader.softReloadAsync("));
        assertTrue(!source.contains("module_runtime_reload_now_button"));
        assertTrue(!source.contains("module_runtime_reload_later_button"));
        assertTrue(runtimeLayout.contains("R.drawable.ic_error_outline_24"));
        assertTrue(runtimeLayout.contains("module_runtime_reload_title"));
        assertTrue(runtimeLayout.contains("module_runtime_reload_message"));
        assertTrue(runtimeLayout.contains("module_runtime_reload_ack_button"));
        assertTrue(runtimeLayout.contains("R.dimen.dialog_status_icon_padding"));
        assertTrue(
            runtimeLayout.contains("R.dimen.dialog_surface_padding_horizontal")
        );
        assertTrue(runtimeLayout.contains("R.dimen.dialog_body_spacing"));
        assertTrue(runtimeLayout.contains("R.dimen.dialog_action_spacing_top"));
        String runtimeMessage = stringEntry(
            strings,
            "module_runtime_reload_message"
        );
        String zhRuntimeTitle = stringEntry(
            zhStrings,
            "module_runtime_reload_title"
        );
        String zhRuntimeMessage = stringEntry(
            zhStrings,
            "module_runtime_reload_message"
        );
        assertTrue(zhRuntimeTitle.contains("建议重启设备"));
        assertTrue(
            zhRuntimeMessage.contains("部分修改可能需要重启设备后才能完全生效")
        );
        assertTrue(!runtimeMessage.contains("HyperOS"));
        assertTrue(!runtimeMessage.contains("Rust"));
        assertTrue(!zhRuntimeMessage.contains("HyperOS"));
        assertTrue(!zhRuntimeMessage.contains("Rust"));
        assertTrue(source.contains("maybeShowStartupDisclaimerDialog()"));
        assertTrue(
            source.contains("if (!maybeShowStartupDisclaimerDialog()) {")
        );
        assertTrue(
            source.contains(
                "updatePromptDialogCoordinator().maybeShowStartupDisclaimerDialog("
            )
        );
        assertTrue(
            source.contains(
                "new UpdatePromptDialogCoordinator.StartupDisclaimerAcceptance()"
            )
        );
        assertTrue(source.contains("new StartupDisclaimerStore(this)"));
        assertTrue(source.contains("return store.isAccepted();"));
        assertTrue(source.contains("return store.setAccepted(true);"));
        assertTrue(
            source.contains(
                "void applyLargeDialogWidth(androidx.appcompat.app.AlertDialog dialog)"
            )
        );
        assertTrue(
            source.contains(
                "DialogWindowSizer.applyLargeWidth(dialog, MainActivity.this);"
            )
        );
        String disclaimerBlock = source.substring(
            source.indexOf("private boolean maybeShowStartupDisclaimerDialog()"),
            source.indexOf("private boolean maybeShowModuleRuntimeReloadAdvice()")
        );
        assertTrue(!disclaimerBlock.contains("DpisConfigStore"));
    }

    @Test
    public void dialogWindowSizerUsesResponsivePresetConstraints()
        throws IOException {
        String source = read(
            "src/main/java/com/dpis/module/ui/DialogWindowSizer.java"
        );
        String dimens = read("src/main/res/values/dimens.xml");
        String integers = read("src/main/res/values/integers.xml");

        assertTrue(
            source.contains(
                "applyCompactWidth(AlertDialog dialog, Context context)"
            )
        );
        assertTrue(
            source.contains(
                "applyStandardWidth(AlertDialog dialog, Context context)"
            )
        );
        assertTrue(
            source.contains(
                "applyLargeWidth(AlertDialog dialog, Context context)"
            )
        );
        assertTrue(source.contains("dialog_window_margin_horizontal"));
        assertTrue(source.contains("resolvePreset(context, preset)"));
        assertTrue(
            source.contains("R.integer.dialog_window_large_min_width_dp")
        );
        assertTrue(source.contains("? Preset.STANDARD"));
        assertTrue(source.contains("calculateWindowWidth(screenWidth"));
        assertTrue(source.contains("screenWidth - horizontalMargin * 2"));
        assertTrue(
            source.contains(
                "COMPACT(R.dimen.dialog_window_compact_max_width, 0.88f)"
            )
        );
        assertTrue(
            source.contains(
                "STANDARD(R.dimen.dialog_window_standard_max_width, 0.90f)"
            )
        );
        assertTrue(
            source.contains(
                "LARGE(R.dimen.dialog_window_large_max_width, 0.92f)"
            )
        );
        assertTrue(dimens.contains("dialog_window_margin_horizontal\">16dp"));
        assertTrue(dimens.contains("dialog_window_compact_max_width\">360dp"));
        assertTrue(dimens.contains("dialog_window_standard_max_width\">420dp"));
        assertTrue(dimens.contains("dialog_window_large_max_width\">560dp"));
        assertTrue(integers.contains("dialog_window_large_min_width_dp\">600"));
    }

    @Test
    public void dialogWindowSizerTreatsHorizontalMarginAsPerSideInset() {
        assertTrue(
            DialogWindowSizer.calculateWindowWidth(360, 16, 420, 0.90f) == 324
        );
        assertTrue(
            DialogWindowSizer.calculateWindowWidth(1000, 16, 560, 0.92f) == 560
        );
        assertTrue(
            DialogWindowSizer.calculateWindowWidth(24, 16, 420, 0.90f) == 0
        );
    }

    @Test
    public void startupUpdateCheckPublishesHomeUpdateCardState()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String coordinatorSource = read(
            "src/main/java/com/dpis/module/updates/StartupUpdateCheckCoordinator.java"
        );
        String homeBinderSource = read(
            "src/main/java/com/dpis/module/home/HomeWorkspaceBinder.java"
        );
        String homeActivationSource = read(
            "src/main/java/com/dpis/module/home/HomeActivationStateResolver.java"
        );
        String homeUpdateStateSource = read(
            "src/main/java/com/dpis/module/home/HomeUpdateUiState.java"
        );
        String homeLayout = read("src/main/res/layout/home_workspace.xml");
        String downloadCoordinatorSource = read(
            "src/main/java/com/dpis/module/updates/UpdateDownloadCoordinator.java"
        );
        String manifestFetcherSource = read(
            "src/main/java/com/dpis/module/updates/UpdateManifestFetcher.java"
        );
        String storeSource = read(
            "src/main/java/com/dpis/module/updates/UpdateStateStore.java"
        );

        assertTrue(source.contains("maybeCheckForUpdatesOnStartup();"));
        assertTrue(source.contains("HomeActivationStateResolver.isActivatedForHome("));
        assertTrue(homeBinderSource.contains("final boolean xposedModuleActivated;"));
        assertTrue(homeBinderSource.contains("return !state.xposedModuleActivated;"));
        assertTrue(homeActivationSource.contains("isModernLibXposedServiceApi(int apiVersion)"));
        assertTrue(source.contains("DpisApplication.isXposedSelfLoaded()"));
        assertTrue(source.contains("new UpdateCoordinator("));
        assertTrue(source.contains("new StartupUpdateCheckCoordinator("));
        assertTrue(source.contains("StartupUpdateCheckOnce.consume()"));
        assertTrue(
            source.contains(
                "startupUpdateCheckCoordinator.maybeCheckForUpdatesOnStartup();"
            )
        );
        assertTrue(
            source.contains(
                "private volatile boolean startupUpdateDownloadInProgress;"
            )
        );
        assertTrue(
            source.contains(
                "private volatile boolean startupUpdateDownloadCancelRequested;"
            )
        );
        assertTrue(
            coordinatorSource.contains(
                "updateCoordinator.evaluateStartupCheck("
            )
        );
        assertTrue(
            coordinatorSource.contains(
                "updateCoordinator.markStartupCheckStarted(state)"
            )
        );
        assertTrue(coordinatorSource.contains("void maybeCheckForUpdatesOnStartup()"));
        assertTrue(coordinatorSource.contains("checkForUpdates(true);"));
        assertTrue(coordinatorSource.contains("void checkForUpdatesNow()"));
        assertTrue(
            coordinatorSource.contains(
                "UpdateCoordinator.isRemoteVersionNewer("
            )
        );
        assertTrue(coordinatorSource.contains("onStartupUpdateAvailable"));
        assertTrue(coordinatorSource.contains("onStartupUpdateUpToDate"));
        assertTrue(coordinatorSource.contains("onStartupUpdateCheckFailed"));
        assertTrue(!coordinatorSource.contains("launchStartupUpdateDialog"));
        assertTrue(!source.contains("private void launchStartupUpdateDialog("));
        assertTrue(source.contains("HomeUpdateUiState.CHECKING"));
        assertTrue(source.contains("HomeUpdateUiState.available(manifest)"));
        assertTrue(source.contains("HomeUpdateUiState.UP_TO_DATE"));
        assertTrue(source.contains("HomeUpdateUiState.FAILED"));
        assertTrue(homeUpdateStateSource.contains("INSTALL_READY"));
        assertTrue(homeUpdateStateSource.contains("asInstallReady(File apkFile)"));
        assertTrue(homeUpdateStateSource.contains("boolean showsUpdateActionCard()"));
        assertTrue(source.contains("showHomeUpdateReleaseNotesDialog()"));
        assertTrue(source.contains("startHomeUpdateDownload()"));
        assertTrue(source.contains("installHomeDownloadedUpdate()"));
        assertTrue(source.contains("startupUpdateCheckCoordinator.checkForUpdatesNow();"));
        assertTrue(homeBinderSource.contains("bindUpdateActions("));
        assertTrue(homeBinderSource.contains("home_update_action_card"));
        assertTrue(!homeBinderSource.contains("bringToFront();"));
        assertTrue(homeLayout.contains("com.dpis.module.home.HomePrimaryStatusClusterLayout"));
        assertTrue(!homeBinderSource.contains("bindPrimaryStatusShape("));
        assertTrue(!homeBinderSource.contains("setBottomLeftCornerSize("));
        assertTrue(homeLayout.contains("android:id=\"@+id/home_primary_status_cluster\""));
        assertTrue(homeLayout.contains("android:clipChildren=\"false\""));
        assertTrue(
            homeLayout.contains(
                "app:cardCornerRadius=\"@dimen/home_workspace_primary_status_corner_radius\""
            )
        );
        assertTrue(homeLayout.contains("android:id=\"@+id/home_update_action_card\""));
        assertTrue(homeLayout.contains("app:cardElevation=\"0dp\""));
        assertTrue(!homeLayout.contains("android:translationZ="));
        assertTrue(!homeLayout.contains("home_primary_status_foreground_elevation"));
        assertTrue(homeLayout.contains("android:layout_height=\"@dimen/home_update_action_card_height\""));
        assertTrue(homeLayout.contains("android:layout_marginTop=\"@dimen/home_update_action_card_hidden_offset_top\""));
        assertTrue(homeLayout.contains("app:cardBackgroundColor=\"@color/home_update_action_card_container\""));
        assertTrue(homeLayout.contains("app:strokeColor=\"?attr/colorOutlineVariant\""));
        assertTrue(homeLayout.contains("android:gravity=\"bottom|center_vertical\""));
        assertTrue(!homeLayout.contains("android:background=\"@drawable/bg_home_update_drawer\""));
        assertTrue(!homeLayout.contains("home_update_drawer"));
        assertTrue(!homeLayout.contains("home_update_card_"));
        assertTrue(!homeLayout.contains("home_update_foreground_card_elevation"));
        assertTrue(!homeLayout.contains("android:background=\"?attr/colorOutlineVariant\""));
        assertTrue(homeLayout.contains("android:layout_height=\"@dimen/home_update_action_card_button_height\""));
        assertTrue(homeLayout.contains("android:paddingStart=\"@dimen/home_update_action_card_padding_start\""));
        assertTrue(homeLayout.contains("android:paddingEnd=\"@dimen/home_update_action_card_padding_end\""));
        assertTrue(homeLayout.contains("android:id=\"@+id/home_update_action_release_notes_button\""));
        assertTrue(homeLayout.contains("android:id=\"@+id/home_update_action_install_frame\""));
        assertTrue(homeLayout.contains("android:background=\"@drawable/bg_home_update_action_install_button\""));
        assertTrue(homeLayout.contains("android:id=\"@+id/home_update_action_install_progress_fill\""));
        assertTrue(homeLayout.contains("android:background=\"@drawable/bg_home_update_action_install_progress\""));
        assertTrue(homeLayout.contains("android:id=\"@+id/home_update_action_install_button\""));
        assertTrue(!homeLayout.contains("android:id=\"@+id/home_update_download_progress\""));
        assertTrue(homeLayout.contains("style=\"@style/Widget.Dpis.HomeUpdateActionCard.NotesButton\""));
        assertTrue(homeLayout.contains("style=\"@style/Widget.Dpis.HomeUpdateActionCard.InstallButton\""));
        assertTrue(homeLayout.contains("android:layout_width=\"wrap_content\""));
        assertTrue(homeLayout.contains("android:minWidth=\"0dp\""));
        assertTrue(homeLayout.contains("android:text=\"@string/home_update_action_release_notes\""));
        assertTrue(homeBinderSource.contains("PrimaryStatusTone.DISABLED"));
        assertTrue(homeBinderSource.contains("PrimaryStatusTone.ENABLED"));
        assertTrue(homeBinderSource.contains("PrimaryStatusTone.UPDATE_AVAILABLE"));
        assertTrue(!homeBinderSource.contains("home_status_checking"));
        assertTrue(homeBinderSource.contains("state.updateState.showsUpdateActionCard()"));
        assertTrue(homeBinderSource.contains("installButton.setOnClickListener(downloading"));
        assertTrue(homeBinderSource.contains("state.actions.installDownloadedUpdate();"));
        assertTrue(homeBinderSource.contains("R.string.home_update_action_downloading"));
        assertTrue(homeBinderSource.contains("R.string.home_update_action_install_ready"));
        assertTrue(!homeBinderSource.contains("installButton.setClickable(false)"));
        assertTrue(!homeBinderSource.contains("R.id.home_update_download_progress)"));
        assertTrue(!homeLayout.contains("ic_download_24"));
        assertTrue(
            coordinatorSource.contains(
                "updateCoordinator.markStartupCheckFinished("
            )
        );
        assertTrue(coordinatorSource.contains("manifestFetcher.fetch("));
        assertTrue(storeSource.contains("KEY_LAST_UPDATE_CHECK_FAILED"));
        assertTrue(
            storeSource.contains("KEY_LAST_PROMPTED_UPDATE_VERSION_CODE")
        );
        assertTrue(
            manifestFetcherSource.contains(
                "static StartupUpdateManifest fetch("
            )
        );
        assertTrue(source.contains("markPromptedVersion("));
        assertTrue(
            downloadCoordinatorSource.contains(
                "updateCoordinator.requestDownloadStart("
            )
        );
        assertTrue(downloadCoordinatorSource.contains("interface HomeDownloadListener"));
        assertTrue(downloadCoordinatorSource.contains("void startHomeDownload("));
        assertTrue(
            downloadCoordinatorSource.contains(
                "updateCoordinator.requestDownloadCancel("
            )
        );
        assertTrue(
            downloadCoordinatorSource.contains(
                "updateCoordinator.markDownloadFinished("
            )
        );
        assertTrue(
            downloadCoordinatorSource.contains("downloadExecutor.download(")
        );
        assertTrue(source.contains("new StartupUpdatePackageHandler(this)"));
        assertTrue(
            !downloadCoordinatorSource.contains("verifyDownloadedApk(")
        );
        assertTrue(
            !downloadCoordinatorSource.contains("UntrustedUpdateException")
        );
        assertTrue(
            !downloadCoordinatorSource.contains("about_update_download_untrusted")
        );
        assertTrue(downloadCoordinatorSource.contains("void onSucceeded(File targetFile)"));
        assertTrue(source.contains("current.asInstallReady(targetFile)"));
        assertTrue(
            source.contains(
                "startupUpdatePackageHandler.launchPackageInstaller(targetFile);"
            )
        );
        assertTrue(source.contains("new ReleaseNotesController("));
        assertTrue(source.contains("ReleaseNotesMarkdownRenderer.render("));
        assertTrue(!source.contains("ReleaseNotesMarkdownLite.format("));
        assertTrue(
            !source.contains("private void verifyDownloadedApk(File apkFile)")
        );
        assertTrue(!source.contains("updatePromptDialogCoordinator().showUpdateAvailableDialog("));
        assertTrue(source.contains("current.releaseNotes"));
        assertTrue(source.contains("startStartupUpdateDownload("));
        assertTrue(
            source.contains(
                "startupUpdateDownloadInProgress = state.downloadInProgress;"
            )
        );
        assertTrue(
            source.contains(
                "startupUpdateDownloadCancelRequested = state.downloadCancelRequested;"
            )
        );
        assertTrue(
            !source.contains(
                "startActivity(AboutActivity.createStartupUpdateIntent("
            )
        );
    }

    @Test
    public void startupDisclaimerLayoutKeepsScrollableContent()
        throws IOException {
        String layout = read(
            "src/main/res/layout/dialog_startup_disclaimer.xml"
        );
        String roundLayout = read(
            "src/main/res/layout-round/dialog_startup_disclaimer.xml"
        );
        String dimensions = read("src/main/res/values/dimens.xml");

        assertTrue(
            layout.contains("com.dpis.module.ui.MaxHeightNestedScrollView")
        );
        assertTrue(layout.contains("app:maxHeightFraction=\"0.45\""));
        assertTrue(layout.contains("startup_disclaimer_message"));
        assertTrue(layout.contains("startup_disclaimer_checkbox"));
        assertTrue(
            layout.indexOf("</com.dpis.module.ui.MaxHeightNestedScrollView>") <
                layout.indexOf(
                    "android:id=\"@+id/startup_disclaimer_checkbox\""
                )
        );
        assertTrue(layout.contains("startup_disclaimer_accept_button"));
        assertTrue(!layout.contains("startup_disclaimer_exit_button"));
        assertTrue(layout.contains("@dimen/dialog_surface_padding_horizontal"));
        assertTrue(layout.contains("@dimen/dialog_body_spacing"));
        assertTrue(layout.contains("@dimen/dialog_text_line_spacing"));
        assertTrue(layout.contains("@dimen/dialog_action_spacing_top"));
        assertTrue(!layout.contains("@dimen/dialog_action_spacing_between"));
        assertTrue(roundLayout.contains("@style/TextAppearance.Material3.TitleSmall"));
        assertTrue(roundLayout.contains("android:maxLines=\"2\""));
        assertTrue(roundLayout.contains("app:maxHeightFraction=\"0.25\""));
        assertTrue(roundLayout.contains("@style/TextAppearance.Material3.BodySmall"));
        assertTrue(roundLayout.contains("startup_disclaimer_checkbox"));
        assertTrue(roundLayout.contains("startup_disclaimer_accept_button"));
        assertTrue(dimensions.contains("dialog_round_surface_padding_horizontal"));
        assertTrue(dimensions.contains("dialog_round_surface_padding_vertical"));
        assertTrue(dimensions.contains("dialog_round_body_spacing"));
        assertTrue(dimensions.contains("dialog_round_text_line_spacing"));
        assertTrue(dimensions.contains("dialog_round_action_spacing_top"));
    }

    @Test
    public void appConfigLayoutUsesScrollableContainerAndAdaptiveModeRows()
        throws IOException {
        String layout = read("src/main/res/layout/dialog_app_config.xml");

        assertTrue(layout.contains("androidx.core.widget.NestedScrollView"));
        assertTrue(layout.contains("android:fillViewport=\"true\""));
        assertTrue(
            layout.contains(
                "android:minHeight=\"@dimen/dialog_mode_toggle_row_min_height\""
            )
        );
        assertTrue(
            !layout.contains(
                "android:layout_height=\"@dimen/dialog_mode_toggle_row_height\""
            )
        );
    }

    public void applyFilter_submitsPerPageListsWithoutRedundantStatusRefresh()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        int applyFilterStart = source.indexOf("private void applyFilter() {");
        int applyFilterEnd = source.indexOf(
            "private void showFilterDialog()",
            applyFilterStart
        );
        assertTrue(applyFilterStart >= 0);
        assertTrue(applyFilterEnd > applyFilterStart);

        String applyFilterBody = source.substring(
            applyFilterStart,
            applyFilterEnd
        );
        assertTrue(applyFilterBody.contains("pagerAdapter.submitPage("));
        assertTrue(applyFilterBody.contains("landListController.bind("));
        assertTrue(
            applyFilterBody.contains("state.visibleItems(landCurrentPage)")
        );
        assertTrue(
            applyFilterBody.contains(
                "landScrollStates.get(landCurrentPage.position())"
            )
        );
        assertFalse(
            applyFilterBody.contains("restoredPageScrollStates.remove")
        );
        assertTrue(
            !applyFilterBody.contains("pagerAdapter.refreshVisibleStatuses();")
        );
    }

    public void landscapeList_keepsScrollStateSeparateFromPagerAdapter()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(
            source.contains(
                "private final SparseArray<Parcelable> landScrollStates"
            )
        );
        assertTrue(source.contains("= new SparseArray<>();"));
        assertTrue(
            source.contains(
                "restoreLandscapeScrollStates(restoredPageScrollStates);"
            )
        );
        assertTrue(
            source.contains("private void captureCurrentLandscapeScrollState()")
        );
        assertTrue(
            source.contains(
                "landScrollStates.put(landCurrentPage.position(), landState);"
            )
        );
        assertTrue(
            source.contains("return pagerAdapter.capturePageScrollStates();")
        );
        assertTrue(
            source.contains("for (int i = 0; i < landScrollStates.size(); i++)")
        );
    }

    @Test
    public void pageRefresh_forcesInstalledAppCatalogReload()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        int refreshStart = source.indexOf(
            "private void onPageRefreshRequested(AppListPage page) {"
        );
        int refreshEnd = source.indexOf(
            "private void requestAppsLoad()",
            refreshStart
        );
        assertTrue(refreshStart >= 0);
        assertTrue(refreshEnd > refreshStart);

        String refreshBody = source.substring(refreshStart, refreshEnd);
        assertTrue(refreshBody.contains("requestAppsLoad(true);"));
    }

    @Test
    public void appLoad_reusesInstalledAppCatalogBetweenRefreshes()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String viewModelSource = read(
            "src/main/java/com/dpis/module/MainViewModel.java"
        );
        String coordinatorSource = read(
            "src/main/java/com/dpis/module/applist/InstalledAppCatalogCoordinator.java"
        );

        assertTrue(source.contains("INSTALLED_APP_CATALOG_TTL_MS"));
        assertTrue(source.contains("new InstalledAppCatalogCoordinator("));
        assertTrue(coordinatorSource.contains("getInstalledAppCatalog("));
        assertTrue(
            viewModelSource.contains("forceInstalledAppCatalogReloadRequested")
        );
        assertTrue(coordinatorSource.contains("cacheFresh"));
    }

    @Test
    public void retainedAppListSkipsImmediateServiceReloadOnRotation()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(
            source.contains("private boolean skipNextImmediateServiceReload;")
        );
        assertTrue(
            source.contains(
                "skipNextImmediateServiceReload = !initialAppsSnapshot.isEmpty();"
            )
        );
        assertTrue(
            source.contains(
                "DpisApplication.addServiceStateListener(this, true);"
            )
        );
        assertTrue(source.contains("if (skipNextImmediateServiceReload)"));
    }

    @Test
    public void appConfigSheet_halfExpandedStateUsesDownwardOffset()
        throws IOException {
        String coordinatorSource = read(
            "src/main/java/com/dpis/module/appconfig/AppConfigDialogCoordinator.java"
        );

        assertTrue(
            coordinatorSource.contains(
                "R.dimen.dialog_app_config_half_expanded_down_offset"
            )
        );
        assertTrue(
            coordinatorSource.contains(
                "anchorBottom - sheetPos[1] - halfExpandedDownOffsetPx"
            )
        );
    }

    @Test
    public void showEditDialog_usesSheetCoordinatorInPortraitAndDetailPaneInLandscape()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("new GlobalPrefillStore("));
        assertTrue(source.contains("AppConfigPrefillPreview.applyIfEligible("));
        assertTrue(
            source.contains("dialogView, sheetItem, systemHooksEnabled")
        );
        assertTrue(
            source.contains(
                "private void showEditBottomSheet(AppListItem item)"
            )
        );
        assertTrue(
            source.contains("new AppConfigDialogBinder(")
        );
        assertTrue(source.contains("createAppConfigDialogHost()"));
        assertTrue(source.contains("binder.bind("));
        assertTrue(
            source.contains("new AppConfigDialogCoordinator(this).show(")
                    || compact(source).contains(
                            "new AppConfigDialogCoordinator(this).show("
                    )
        );
        assertTrue(
            source.contains("private void showEditDetailPane(")
        );
        assertTrue(source.contains("R.layout.view_land_app_detail"));
        assertTrue(source.contains("new LandAppDetailPaneBinder("));
        assertTrue(
            source.contains(
                "saveAppConfigDraft("
            )
        );
        assertTrue(source.contains("editorItem"));
        assertTrue(source.contains("state,"));
        assertTrue(
            source.contains("showLandDetailTypefaceSelector(")
        );
        assertTrue(
            source.contains("showLandDetailHookDomains(editorItem, state, onChanged);")
        );
        assertTrue(
            compact(source).contains(
                "toggleLandDetailScope( editorItem, currentlyInScope, onTurnedInScope, onTurnedOutScope );"
            )
        );
        assertTrue(
            source.contains(
                "public boolean setDpisEnabled(String packageName, boolean enabled)"
            )
        );
        assertFalse(source.contains("resetLandDetailConfig(editorItem);"));
        assertTrue(source.contains("appConfigSaveHandler.saveResolved("));
        assertTrue(source.contains("updateEditingDraft(state);"));
        assertTrue(source.contains("void onDraftStateChanged("));
        assertTrue(source.contains("if (draft == null && mainViewModel != null)"));
        assertTrue(
            read("src/main/java/com/dpis/module/appconfig/LandAppDetailPaneBinder.java")
                .contains("AppConfigDialogBinder.AppConfigDialogState.fromItem(item)")
        );
        assertTrue(
            source.contains("executeDialogProcessAction(processItem, action);")
        );
        assertTrue(compact(source).contains("landDetailContent.addView( dialogView"));
        assertTrue(source.contains("ViewGroup.LayoutParams.MATCH_PARENT"));
        assertFalse(source.contains("createLandDetailContentLayoutParams()"));
        assertFalse(
            source.contains(
                "landDetailContent.post(() -> applyLandDetailContentLayout(dialogView))"
            )
        );
        assertTrue(!source.contains("private void bindDialogValidation("));
        assertTrue(!source.contains("private void bindDialogActions("));
        assertTrue(!source.contains("private void refreshDialogState("));
    }

    @Test
    public void showEditDialog_doesNotRefreshListRowsBeforeOpeningDetail()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        int methodStart = source.indexOf(
            "private void showEditDialog(AppListItem item) {"
        );
        int methodEnd = source.indexOf(
            "private void showEditBottomSheet(AppListItem item)",
            methodStart
        );
        assertTrue(methodStart >= 0);
        assertTrue(methodEnd > methodStart);

        String methodBody = source.substring(methodStart, methodEnd);
        assertFalse(methodBody.contains("refreshVisibleStatuses"));
    }

    public void appListAdapter_usesStableIdsAndPositionBasedClickBinding()
        throws IOException {
        String source = read(
            "src/main/java/com/dpis/module/applist/AppListPagerAdapter.java"
        );

        assertTrue(source.contains("setHasStableIds(true);"));
        assertTrue(source.contains("public long getItemId(int position)"));
        assertTrue(source.contains("holder.getBindingAdapterPosition();"));
        assertTrue(source.contains("position == RecyclerView.NO_POSITION"));
        assertTrue(
            source.contains(
                "onAppClickListener.onAppClicked(getItem(position));"
            )
        );
    }

    @Test
    public void landscapeStatusLayout_usesFlatDetailPane() throws IOException {
        String layout = read("src/main/res/layout-land/activity_status.xml");

        assertTrue(layout.contains("@+id/land_root_row"));
        assertFalse(layout.contains("@+id/app_pager"));
        assertTrue(layout.contains("@+id/land_detail_pane"));
        assertTrue(layout.contains("@+id/land_detail_content"));
        assertFalse(layout.contains("android:paddingTop=\"@dimen/main_land_detail_top_padding\""));
        assertTrue(layout.contains("<FrameLayout"));
        assertFalse(
            layout.contains(
                "<com.google.android.material.card.MaterialCardView\n" +
                    "                android:id=\"@+id/land_detail_pane\""
            )
        );
    }

    @Test
    public void landscapeAppDetailUsesDedicatedOverviewRows()
        throws IOException {
        String layout = read("src/main/res/layout/view_land_app_detail.xml");
        String binder = read(
            "src/main/java/com/dpis/module/appconfig/LandAppDetailPaneBinder.java"
        );
        String dimens = read("src/main/res/values/dimens.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertFalse(layout.contains("@string/land_detail_display_font_section_title"));
        assertFalse(layout.contains("@string/land_detail_font_hook_section_title"));
        assertTrue(layout.contains("@drawable/bg_land_detail_connected_row_top"));
        assertTrue(layout.contains("@drawable/bg_land_detail_connected_row_bottom"));
        assertTrue(layout.contains("@drawable/ripple_land_detail_connected_row_top"));
        assertTrue(layout.contains("@drawable/ripple_land_detail_connected_row_bottom"));
        assertTrue(layout.contains("@dimen/land_app_detail_connected_row_gap"));
        assertTrue(layout.contains("@string/dialog_advanced_section_title"));
        assertFalse(layout.contains("@string/land_detail_app_control_section_title"));
        assertTrue(layout.contains("<com.google.android.material.card.MaterialCardView"));
        assertTrue(layout.contains("android:layout_height=\"match_parent\""));
        assertTrue(layout.contains("android:id=\"@+id/land_detail_scroll\""));
        int landScrollStart = layout.indexOf("android:id=\"@+id/land_detail_scroll\"");
        int landScrollEnd = layout.indexOf(">", landScrollStart);
        String landScrollBlock = layout.substring(landScrollStart, landScrollEnd);
        assertTrue(landScrollBlock.contains(
            "android:paddingTop=\"@dimen/main_land_detail_top_padding\""
        ));
        assertTrue(layout.contains("android:id=\"@+id/land_detail_scroll_content\""));
        assertFalse(layout.contains("android:layout_height=\"@dimen/main_content_divider_height\""));
        assertFalse(layout.contains("@dimen/land_app_detail_input_group_padding_horizontal"));
        assertTrue(layout.contains("@dimen/land_app_detail_section_gap"));
        assertTrue(layout.contains("@dimen/land_app_detail_editor_row_spacing"));
        assertTrue(layout.contains("@dimen/land_app_detail_card_inner_spacing"));
        assertFalse(layout.contains("@dimen/dialog_app_config_process_row_spacing_top"));
        assertFalse(layout.contains("@dimen/dialog_app_config_save_row_spacing_top"));
        assertFalse(layout.contains("@drawable/bg_land_detail_process_capsule"));
        int advancedCardStart = layout.indexOf("<!-- Advanced Actions Card -->");
        int advancedCardEnd = layout.indexOf("</com.google.android.material.card.MaterialCardView>",
                advancedCardStart);
        String advancedCardBlock = layout.substring(advancedCardStart, advancedCardEnd);
        assertTrue(advancedCardBlock.contains(
                "app:cardBackgroundColor=\"?attr/colorSurfaceContainer\""));
        assertFalse(advancedCardBlock.contains("app:strokeColor=\"?attr/colorOutlineVariant\""));
        assertFalse(advancedCardBlock.contains(
                "app:strokeWidth=\"@dimen/land_app_detail_card_stroke_width\""));
        assertTrue(advancedCardBlock.contains("app:strokeWidth=\"0dp\""));
        assertTrue(layout.contains(
                "android:layout_marginBottom=\"@dimen/land_app_detail_dock_margin_bottom\""));
        int landStatusStart = layout.indexOf("android:id=\"@+id/land_detail_status\"");
        int landStatusEnd = layout.indexOf("/>", landStatusStart);
        String landStatusBlock = layout.substring(landStatusStart, landStatusEnd);
        assertTrue(layout.contains("@style/Widget.Dpis.AppIdentityTitle"));
        assertTrue(layout.contains("@style/Widget.Dpis.LandAppIdentitySecondaryText"));
        assertTrue(layout.contains("@style/Widget.Dpis.LandAppIdentityStatusText"));
        assertTrue(layout.contains("@dimen/land_app_identity_secondary_spacing_top"));
        assertTrue(layout.contains("@dimen/land_app_identity_status_spacing_top"));
        assertFalse(layout.contains("@dimen/land_template_detail_subtitle_spacing_top"));
        assertTrue(dimens.contains(
                "<dimen name=\"land_app_identity_secondary_spacing_top\">0dp</dimen>"
        ));
        assertTrue(dimens.contains(
                "<dimen name=\"land_app_identity_status_spacing_top\">0dp</dimen>"
        ));
        assertTrue(landStatusBlock.contains("android:layout_width=\"0dp\""));
        assertTrue(landStatusBlock.contains("android:layout_weight=\"1\""));
        assertTrue(layout.contains("android:id=\"@+id/land_detail_unsaved_badge\""));
        int unsavedBadgeStart = layout.indexOf("android:id=\"@+id/land_detail_unsaved_badge\"");
        int unsavedBadgeEnd = layout.indexOf("/>", unsavedBadgeStart);
        String unsavedBadgeBlock = layout.substring(unsavedBadgeStart, unsavedBadgeEnd);
        assertTrue(unsavedBadgeBlock.contains("android:layout_width=\"wrap_content\""));
        assertFalse(unsavedBadgeBlock.contains("android:layout_weight=\"1\""));
        assertTrue(layout.contains("android:id=\"@+id/land_detail_action_dock\""));
        int actionDockIdStart = layout.indexOf("android:id=\"@+id/land_detail_action_dock\"");
        int actionDockStart = layout.lastIndexOf("<FrameLayout", actionDockIdStart);
        int actionDockTagEnd = layout.indexOf(">", actionDockStart);
        String actionDockTag = layout.substring(actionDockStart, actionDockTagEnd);
        assertTrue(actionDockTag.contains("<FrameLayout"));
        assertFalse(actionDockTag.contains("cardBackgroundColor"));
        int actionSurfaceStart = layout.indexOf(
                "android:id=\"@+id/land_detail_action_surface\""
        );
        int actionSurfaceTagEnd = layout.indexOf(">", actionSurfaceStart);
        String actionSurfaceTag = layout.substring(actionSurfaceStart, actionSurfaceTagEnd);
        assertTrue(actionSurfaceTag.contains(
                "app:cardBackgroundColor=\"?attr/colorSurfaceContainerHigh\""));
        assertTrue(actionSurfaceTag.contains(
                "app:cardCornerRadius=\"@dimen/land_app_detail_dock_corner_radius\""));
        assertTrue(layout.contains(
                "android:id=\"@+id/land_detail_process_action_group\""));
        assertTrue(layout.contains(
                "app:cardBackgroundColor=\"?attr/colorSurfaceContainer\""));
        int clearanceStart = binder.indexOf("private void updateScrollContentClearance(");
        int clearanceEnd = binder.indexOf(
                "private static boolean updateSaveButtonState(",
                clearanceStart
        );
        String clearanceBlock = binder.substring(clearanceStart, clearanceEnd);
        assertTrue(clearanceBlock.contains("R.id.land_detail_scroll_content"));
        assertTrue(clearanceBlock.contains("content.setPaddingRelative("));
        assertFalse(clearanceBlock.contains("MarginLayoutParams"));
        assertFalse(clearanceBlock.contains("bottomMargin"));
        assertTrue(layout.contains("android:id=\"@+id/land_detail_save_button\""));
        assertTrue(layout.contains("android:id=\"@+id/land_detail_scope_row\""));
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_viewport_input\"")
        );
        assertTrue(layout.contains("android:inputType=\"numberDecimal\""));
        assertTrue(
            layout.contains(
                "android:id=\"@+id/land_detail_viewport_mode_toggle_button\""
            )
        );
        assertTrue(
            layout.contains(
                "android:id=\"@+id/land_detail_viewport_mode_scale_label\""
            )
        );
        assertTrue(
            layout.contains(
                "android:id=\"@+id/land_detail_viewport_mode_width_label\""
            )
        );
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_font_scale_input\"")
        );
        assertTrue(
            layout.indexOf("android:id=\"@+id/land_detail_font_scale_editor\"")
                < layout.indexOf("android:id=\"@+id/dialog_wechat_dpi_row\"")
        );
        assertTrue(
            layout.indexOf("android:id=\"@+id/dialog_wechat_dpi_row\"")
                < layout.indexOf("android:id=\"@+id/land_detail_typeface_row\"")
        );
        assertTrue(
            layout.contains(
                "android:id=\"@+id/land_detail_font_mode_toggle_button\""
            )
        );
        assertTrue(
            layout.contains(
                "android:id=\"@+id/land_detail_font_mode_system_label\""
            )
        );
        assertTrue(
            layout.contains(
                "android:id=\"@+id/land_detail_font_mode_compat_label\""
            )
        );
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_typeface_row\"")
        );
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_hook_chain_row\"")
        );
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_scope_row\"")
        );
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_dpis_toggle_row\"")
        );
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_reset_row\"")
        );
        assertTrue(
            layout.contains(
                "android:id=\"@+id/land_detail_feedback_diagnostic_row\""
            )
        );
        assertTrue(
            layout.contains(
                "android:text=\"@string/feedback_diagnostic_record_action\""
            )
        );
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_start_button\"")
        );
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_restart_button\"")
        );
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_stop_button\"")
        );
        assertTrue(
            layout.contains("android:id=\"@+id/land_detail_save_button\"")
        );
        assertFalse(layout.contains("dialog_viewport_input_layout"));
        assertTrue(binder.contains("interface Actions"));
        assertTrue(binder.contains("AppStatusFormatter.formatCompact("));
        assertTrue(binder.contains("new AppStatusFormatter.StatusInput("));
        assertTrue(binder.contains("void saveDraft("));
        assertTrue(binder.contains("AppListItem item,"));
        assertTrue(binder.contains("actions.saveDraft("));
        assertTrue(
            binder.contains("AppConfigDialogBinder.bindViewportModeToggle(")
        );
        assertTrue(
            binder.contains("AppConfigDialogBinder.bindFontModeToggle(")
        );
        assertTrue(binder.contains("actions.toggleScope("));
        assertTrue(binder.contains("actions.startFeedbackDiagnostic(item, state);"));
        assertTrue(binder.contains("state.scopeSelected,"));
        assertTrue(compact(binder).contains("actions.setDpisEnabled(item.packageName, nextEnabled)"));
        assertTrue(binder.contains("resetDraft("));
        assertTrue(binder.contains("root,"));
        assertTrue(binder.contains("item,"));
        assertTrue(binder.contains("FormInputFocusBinder.bindDismissOnOutsideTouch"));
        assertTrue(binder.contains("FormInputFocusBinder.clearFocusAndHideIme"));
        assertTrue(binder.contains("WechatDpiSheetBinder.inputViewForFocus(root)"));
        assertTrue(binder.contains("state.clearViewportInputs();"));
        assertTrue(binder.contains("WechatDpiSheetBinder.clearDraft(root);"));
        assertTrue(
            binder.contains(
                "AppConfigDialogBinder.AppConfigDialogState.fromItem(item)"
            )
        );
        assertTrue(
            compact(binder).contains(
                "root.setTag(R.id.land_detail_hook_chain_row, state);"
            )
        );
        assertTrue(
            compact(binder).contains(
                "root.getTag(R.id.land_detail_hook_chain_row)"
            )
        );
        assertTrue(
            compact(binder).contains(
                "root.setTag( R.id.land_detail_save_button, signature != null ? signature : \"\" );"
            )
        );
        assertTrue(binder.contains("actions.showTypefaceSelector(item"));
        assertTrue(binder.contains("actions.showHookDomains(item, state"));
        assertFalse(binder.contains("currentFontConfigItem("));
        assertFalse(binder.contains("withFontConfig("));
        assertTrue(dimens.contains("land_app_detail_card_padding"));
        assertTrue(dimens.contains("land_app_detail_section_gap"));
        assertTrue(dimens.contains("land_app_detail_card_gap"));
        assertTrue(dimens.contains("land_app_detail_card_inner_spacing"));
        assertTrue(dimens.contains("land_app_detail_list_item_padding_horizontal"));
        assertTrue(dimens.contains("land_app_detail_editor_row_spacing"));
        assertTrue(dimens.contains("land_app_detail_editor_input_min_width"));
        assertTrue(dimens.contains("land_app_detail_connected_row_inner_radius"));
    }

    @Test
    public void landscapeDetailInsetsAreAppliedToScrollableContent()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        int methodStart = source.indexOf("private void applyLandDetailContentInsets");
        int methodEnd = source.indexOf("private void focusSearchInputAndShowKeyboard", methodStart);

        assertTrue(methodStart >= 0);
        assertTrue(methodEnd > methodStart);
        String methodBody = source.substring(methodStart, methodEnd);
        assertTrue(methodBody.contains("detailView.findViewById(R.id.land_detail_scroll)"));
        assertTrue(methodBody.contains(
                "WindowInsetsBinder.applySafeDrawingPadding(scrollView, false, true, false, true);"
        ));
        assertFalse(methodBody.contains("ViewCompat.requestApplyInsets(scrollView);"));
        assertTrue(source.contains("applyLandDetailContentInsets(dialogView);"));
        assertTrue(source.contains("ViewCompat.requestApplyInsets(scrollView);"));
    }

    @Test
    public void appConfigAndProcessActions_delegateToDedicatedHandlers()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains(
                "new ProcessActionHandler(this, this::syncRuntimePropertiesForTargetLaunch)"));
        assertTrue(source.contains("new AppConfigSaveHandler()"));
        assertTrue(
            source.contains("processActionHandler.execute(item, mappedAction);")
        );
        assertTrue(source.contains("appConfigSaveHandler.save("));
        assertTrue(
            source.contains(
                "FontRuntimePropertySyncer.clearTargetAsync(packageName)"
            )
        );
        assertTrue(
            !source.contains("private void runProcessAction(String packageName")
        );
        assertTrue(
            !source.contains("private int[] saveAppConfig(AppListItem item")
        );
    }

    @Test
    public void installedCatalog_defersIconsUntilRowsAreVisible()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String coordinatorSource = read(
            "src/main/java/com/dpis/module/applist/InstalledAppCatalogCoordinator.java"
        );

        assertTrue(source.contains("loadInstalledApps(forceInstalledAppCatalogReload)"));
        assertTrue(coordinatorSource.contains("List<InstalledAppCatalogItem> catalog = loadInstalledAppCatalog("));
        assertTrue(coordinatorSource.contains("return applicationInfo.loadIcon(packageManager);"));
        assertFalse(coordinatorSource.contains("icon = loadApplicationIcon(packageManager, applicationInfo);"));
        assertFalse(coordinatorSource.contains("maybeScheduleFirstScreenIconWarmup("));
        assertFalse(coordinatorSource.contains("ExecutorService"));
        assertFalse(coordinatorSource.contains("onIconsLoaded("));
        assertTrue(!coordinatorSource.contains("getDefaultActivityIcon()"));
    }

    @Test
    public void systemScopeAndHookStatus_delegateToCoordinator()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(
            source.contains(
                "new SystemScopeCoordinator(createSystemScopeHost())"
            )
        );
        assertTrue(source.contains("systemScopeCoordinator.toggleScope("));
        assertTrue(
            source.contains(
                "systemScopeCoordinator.resolveSystemHookEffectiveEnabled("
            )
        );
        assertFalse(source.contains("openLsposedModuleSettings()"));
        assertFalse(
            source.contains(
                "de.robv.android.xposed.intent.action.MODULE_SETTINGS"
            )
        );
        assertTrue(
            !source.contains("private void toggleScope(String packageName")
        );
    }

    @Test
    public void touchFeedbackBinderProvidesSharedHapticAndScaleBehavior()
        throws IOException {
        String source = read(
            "src/main/java/com/dpis/module/ui/TouchFeedbackBinder.java"
        );

        assertTrue(source.contains("public final class TouchFeedbackBinder"));
        assertTrue(source.contains("bindPressScaleAndHaptic(View view)"));
        assertTrue(
            source.contains(
                "performHapticFeedback(resolvePressHapticConstant())"
            )
        );
        assertTrue(source.contains("HapticFeedbackConstants.CONFIRM"));
        assertTrue(source.contains("HapticFeedbackConstants.VIRTUAL_KEY"));
    }

    @Test
    public void applicationSyncsHyperOsNativeFontTargetsOnStartup()
        throws IOException {
        String source = read(
            "src/main/java/com/dpis/module/DpisApplication.java"
        );

        assertTrue(
            source.contains(
                "RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore)"
            )
        );
        assertTrue(
            source.contains(
                "RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(refreshedStore)"
            )
        );
        assertTrue(
            !source.contains(
                "HyperOsNativeProxyRefreshCoordinator.refreshConfiguredTargetsAsync(this, configStore)"
            )
        );
        assertTrue(
            !source.contains(
                "HyperOsNativeProxyRefreshCoordinator.refreshConfiguredTargetsAsync(this, runtimeDeliveryStore)"
            )
        );
    }

    @Test
    public void appReceivesPackageReplacementWithoutAutoMountingHyperOsNativeProxy()
        throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");
        String receiver = read(
            "src/main/java/com/dpis/module/runtime/DpisPackageLifecycleReceiver.java"
        );

        assertTrue(manifest.contains(".runtime.DpisPackageLifecycleReceiver"));
        assertTrue(
            manifest.contains("android.intent.action.MY_PACKAGE_REPLACED")
        );
        assertTrue(receiver.contains("Intent.ACTION_MY_PACKAGE_REPLACED"));
        assertTrue(
            receiver.contains(
                "HyperOsNativeProxyAssetExporter.exportBundledNativeProxyLibrary(context, DpisLog::e)"
            )
        );
        assertTrue(
            !receiver.contains(
                "HyperOsNativeProxyRefreshCoordinator.refreshConfiguredTargetsAsync(context, store)"
            )
        );
        assertTrue(
            receiver.contains(
                "RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(store)"
            )
        );
    }

    @Test
    public void appConfigHostWiresFontHookDomainEditor() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(
            source.contains("public void showFontHookDomains(")
        );
        assertTrue(
            source.contains("AppConfigDialogBinder.AppConfigDialogState state,")
        );
        assertTrue(
            compact(source).contains(
                "MainActivity.this.showFontHookDomains( item, state, onStateChanged );"
            )
        );
        assertTrue(
            source.contains("public String getFontHookDomainsButtonText(")
        );
        assertTrue(
            source.contains("AppConfigDialogBinder.AppConfigDialogState state")
        );
        assertTrue(
            source.contains("MainActivity.this.getFontHookDomainsButtonText(")
        );
        assertTrue(source.contains("resolveFontHookDomainsForDraft(item, state)"));
        assertTrue(source.contains("new HookDomainOverrideStore(getHookConfigStore()).read("));
        assertTrue(source.contains("FontHookDomainDialog.show("));
        assertTrue(source.contains("isFontHookDomainEditingEnabled()"));
        assertTrue(source.contains("AppConfigDialogBinder.resolveFontMode(findFontModeToggle(root))"));
        assertTrue(source.contains("this,"));
        String saveSource = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.java");
        assertTrue(
            !saveSource.contains("FontRuntimePropertySyncer.publishTargetAsync(")
        );
        assertTrue(source.contains("scheduleRuntimePropertiesForTargetLaunch(packageName);"));
        assertTrue(source.contains("FontRuntimePropertySyncer.syncTarget(packageName, store);"));
        assertTrue(source.contains("FontHookDomainRegistry.recommendedTemplateKnownDomains()"));
        assertFalse(source.contains("AppProcessHookInstaller.resolveDebugFontOverrideForPackage("));
        assertTrue(source.contains("FontHookDomainPresentation.forOverride("));
        assertTrue(source.contains(".buttonText(this);"));
        assertFalse(source.contains("item.fontScalePercent != null && item.fontScalePercent > 0"));
        assertFalse(source.contains("publishFontRuntimeTarget("));
    }

    @Test
    public void fontHookDomainEditorUsesDraftStateOnly()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        int methodStart = source.indexOf(
            "private void showFontHookDomains("
        );
        int methodEnd = source.indexOf(
            "private String getFontHookDomainsButtonText",
            methodStart
        );
        String method = source.substring(methodStart, methodEnd);

        assertTrue(
            compact(method).contains(
                "HookDomainOverride currentOverride = resolveFontHookDomainsForDraft(item, state);"
            )
        );
        assertTrue(
            compact(method).contains(
                "state.draftFontHookDomainsRaw = HookDomainOverrideStore.rawValueForSelection("
            )
        );
        assertTrue(method.contains("state.draftFontHookDomainsRaw = null;"));
        assertTrue(
            compact(method).contains(
                "state.viewportApplyMode = ViewportApplyMode.normalize(mode);"
            )
        );
        assertTrue(
            compact(method).contains(
                "state != null ? state.viewportApplyMode : store.getTargetViewportApplyMode(item.packageName)"
            )
        );
        String compactMethod = compact(method);
        assertFalse(compactMethod.contains("saveCustomIfDifferentFromAutomatic("));
        assertFalse(compactMethod.contains("restoreRecommended(packageName)"));
        assertFalse(compactMethod.contains("store.setTargetViewportApplyMode("));
        assertFalse(compactMethod.contains("requestAppsLoad();"));
    }

    @Test
    public void fontHookDomainButtonTextUsesMutablePreviewStateFlag()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        int methodStart = source.indexOf(
            "private String getFontHookDomainsButtonText("
        );
        int methodEnd = source.indexOf(
            "private Set<String> recommendedTemplateFontHookDomains",
            methodStart
        );
        String method = source.substring(methodStart, methodEnd);

        assertTrue(method.contains("FontHookDomainPresentation.forOverride("));
        assertTrue(source.contains("FontHookDomainPresentation"));
        assertTrue(method.contains("AppConfigDialogBinder.AppConfigDialogState state"));
        assertFalse(method.contains("item.previewFromGlobalPrefill"));
    }

    @Test
    public void composeTemplateEditorBridgesSelectionDraftAndCloseLifecycle()
        throws IOException {
        String activity = read("src/main/java/com/dpis/module/MainActivity.java");
        String coordinator = read(
            "src/main/java/com/dpis/module/MainWorkspacePresentationCoordinator.kt");
        String workspace = read(
            "src/main/java/com/dpis/module/ui/compose/TemplateWorkspaceContent.kt");

        assertTrue(activity.contains("onComposeTemplateEditorOpened(quickTemplate, templateId)"));
        assertTrue(activity.contains("boolean dirty = form.isDirty();"));
        assertTrue(activity.contains("retainedGlobalPrefillDraft = dirty ? form.globalDraft() : null;"));
        assertTrue(activity.contains("retainedQuickTemplateDraft = dirty ? form.quickDraft() : null;"));
        assertTrue(activity.contains("private void onComposeTemplateEditorClosed()"));
        assertTrue(activity.contains("clearTemplateDetailSelection();"));
        int closeStart = activity.indexOf("private void onComposeTemplateEditorClosed()");
        int closeEnd = activity.indexOf("private void showQuickTemplateEditor", closeStart);
        String closeMethod = activity.substring(closeStart, closeEnd);
        assertTrue(closeMethod.indexOf("clearTemplateDetailSelection();")
                < closeMethod.indexOf("bindTemplateWorkspace();"));
        assertTrue(coordinator.contains("onEditorOpened ="));
        assertTrue(coordinator.contains("onEditorChanged = content::updateTemplateEditor"));
        assertTrue(coordinator.contains("onEditorClosed = content::closeTemplateEditor"));
        assertTrue(workspace.contains("onEditorOpened: (quickTemplate: Boolean, templateId: String?)"));
        assertTrue(workspace.contains("onEditorChanged: (TemplateEditorForm) -> Unit"));
        assertTrue(workspace.contains("onEditorClosed: () -> Unit"));
        assertTrue(workspace.contains("onEditorOpened(kind == EDITOR_QUICK, templateId)"));
        assertTrue(workspace.contains("onEditorChanged(editorDraft.form)"));
        assertTrue(workspace.contains("onEditorClosed()"));
        int workspaceCloseStart = workspace.indexOf("fun closeEditor()");
        int workspaceCloseEnd = workspace.indexOf("fun saveEditor()", workspaceCloseStart);
        String workspaceClose = workspace.substring(workspaceCloseStart, workspaceCloseEnd);
        assertFalse(workspaceClose.contains("onEditorDestinationChanged"));
        assertTrue(workspace.contains("closeEditor()"));
        assertTrue(workspace.contains("@Preview(showBackground = true"));
    }

    @Test
    public void composeTemplateRestorePublishesDetailWithoutLegacyEditorFallback()
        throws IOException {
        String activity = read("src/main/java/com/dpis/module/MainActivity.java");
        int methodStart = activity.indexOf(
                "private void restoreTemplateEditorForCurrentConfiguration()");
        int methodEnd = activity.indexOf(
                "private void clearTemplateDetailSelection()", methodStart);
        String method = activity.substring(methodStart, methodEnd);

        int composeBranch = method.indexOf("if (composeShellHost != null)");
        assertTrue(composeBranch >= 0);
        assertTrue(method.contains("bindTemplateWorkspace();"));
        assertFalse(method.contains("showGlobalPrefillSheet"));
        assertFalse(method.contains("showQuickTemplateSheet"));
        assertFalse(method.contains("closeActiveTemplateSheetForMigration"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }

    private static String stringEntry(String source, String name) {
        String marker = "name=\"" + name + "\"";
        int start = source.indexOf(marker);
        if (start < 0) {
            return "";
        }
        int end = source.indexOf("</string>", start);
        if (end < start) {
            return source.substring(start);
        }
        return source.substring(start, end);
    }

    private static String compact(String source) {
        return source.replaceAll("\\s+", " ").trim();
    }

    @Test
    public void hyperOsRestartPreparesNativeProxyBeforeProcessAction()
        throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        int methodStart = source.indexOf(
            "private boolean shouldPrepareHyperOsNativeProxyForRestart"
        );
        int methodEnd = source.indexOf(
            "private static boolean hasActiveStoredConfig",
            methodStart
        );
        assertTrue(methodStart >= 0);
        assertTrue(methodEnd > methodStart);
        String methodBody = source.substring(methodStart, methodEnd);

        assertTrue(
            source.contains("executeDialogProcessActionAfterHyperOsProxyReady")
        );
        assertTrue(source.contains("AppConfigInputValidation.parseViewportTargetSpec("));
        assertFalse(source.contains("ViewportTargetSpec.relativeScale(viewportValue * 10)"));
        assertTrue(
            source.contains("shouldPrepareHyperOsNativeProxyForRestart(item)")
        );
        assertTrue(
            methodBody.contains("DpisConfigStore store = getHookConfigStore();")
        );
        assertTrue(
            methodBody.contains("store.isTargetDpisEnabled(item.packageName)")
        );
        assertTrue(
            methodBody.contains(
                "hasActiveStoredConfig(store, item.packageName)"
            )
        );
        assertFalse(
            methodBody.contains("store.getTargetTypefaceId(packageName)")
        );
        assertFalse(
            methodBody.contains("typefaceId != null && !typefaceId.isBlank()")
        );
        assertFalse(
            methodBody.contains(
                "item.fontScalePercent != null\n                && item.fontScalePercent > 0"
            )
        );
        assertFalse(methodBody.contains("FontApplyMode.isEnabled"));
        assertTrue(
            source.contains(
                "executeHyperOsNativeProxyMount(item, true, success ->"
            )
        );
        assertTrue(source.contains("if (success)"));
        assertTrue(
            source.contains("processActionHandler.execute(item, mappedAction);")
        );
    }
}
