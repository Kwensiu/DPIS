package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class MainActivitySourceSmokeTest {
    @Test
    public void mainActivityRetainsHelpFabWiring() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String layout = read("src/main/res/layout/activity_status.xml");

        assertTrue(source.contains("helpFab = findViewById(R.id.help_fab);"));
        assertTrue(source.contains("helpFab.setOnClickListener(v -> showHelpTutorialDialog());"));
        assertTrue(source.contains("isTouchInsideView(rawX, rawY, helpFab)"));
        assertTrue(layout.contains("@+id/help_fab"));
    }

    @Test
    public void mainActivityWiresPagerMediatorAndFilterEntry() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("R.id.app_pager"));
        assertTrue(source.contains("R.id.workspace_switch"));
        assertTrue(source.contains("R.id.workspace_app_button"));
        assertTrue(source.contains("R.id.workspace_template_button"));
        assertTrue(source.contains("R.id.search_focus_fab"));
        assertTrue(source.contains("new TabLayoutMediator("));
        assertTrue(source.contains("searchFilterButton.setOnClickListener"));
        assertTrue(source.contains("helpFab.setOnClickListener"));
        assertTrue(source.contains("showHelpTutorialDialog()"));
        assertTrue(source.contains("HelpTutorialDialog.show(this);"));
        assertTrue(!source.contains("RichTextDialog.show("));
        assertTrue(source.contains("searchFocusFab.setOnClickListener"));
        assertTrue(source.contains("bindFabTouchFeedback(searchFocusFab);"));
        assertTrue(source.contains("bindFabTouchFeedback(helpFab);"));
        assertTrue(source.contains("private void bindFabTouchFeedback(FloatingActionButton fab)"));
        assertTrue(source.contains("TouchFeedbackBinder.bindPressScaleAndHaptic(fab);"));
        assertTrue(source.contains("focusSearchInputAndShowKeyboard()"));
        assertTrue(source.contains("onPageListScrolled("));
        assertTrue(source.contains("hideSearchFocusFab()"));
        assertTrue(source.contains("showSearchFocusFab()"));
        assertTrue(source.contains("showFilterDialog()"));
        assertTrue(source.contains("new AppListFilterState("));
        assertTrue(source.contains("setOnCheckedChangeListener"));
        assertTrue(!source.contains("R.id.filter_apply_button"));
        assertTrue(!source.contains("R.id.filter_reset_button"));
    }

    @Test
    public void workspaceSwitchHidesAppControlsInTemplateWorkspace() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("STATE_WORKSPACE_MODE"));
        assertTrue(source.contains("MainWorkspaceMode.fromName("));
        assertTrue(source.contains("bindWorkspaceSwitch()"));
        assertTrue(source.contains("workspaceSwitch.addOnButtonCheckedListener"));
        assertTrue(source.contains("MainUiAction.workspaceModeChanged("));
        assertTrue(source.contains("applyWorkspaceMode(state.workspaceMode);"));
        assertTrue(source.contains("boolean appWorkspace = mode == MainWorkspaceMode.APP;"));
        assertTrue(source.contains("setVisible(filterTabs, appWorkspace);"));
        assertTrue(source.contains("setVisible(appWorkspaceDivider, appWorkspace);"));
        assertTrue(source.contains("setVisible(appPager, appWorkspace);"));
        assertTrue(source.contains("setVisible(templateWorkspaceContainer, !appWorkspace);"));
        assertTrue(source.contains("templateWorkspaceBinder = new TemplateWorkspaceBinder(this);"));
        assertTrue(source.contains("bindTemplateWorkspace();"));
        assertTrue(source.contains("templateWorkspaceBinder.bind(templateWorkspaceContainer);"));
        assertTrue(source.contains("searchFilterButton.setEnabled(appWorkspace);"));
        assertTrue(source.contains("searchFilterButton.setVisibility(appWorkspace ? View.VISIBLE : View.GONE);"));
        assertTrue(source.contains("workspaceModeForButtonId(int checkedId)"));
        assertTrue(source.contains("checkedId == R.id.workspace_template_button"));
    }

    @Test
    public void restoreSnapshot_isNotBlockedBySavedStateBranch() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        int restoreSnapshotLine = source.indexOf(
                "initialAppsSnapshot = new ArrayList<>(retainedState.appsSnapshot);");
        assertTrue(restoreSnapshotLine > 0);
        String beforeRestoreSnapshot = source.substring(0, restoreSnapshotLine);

        assertTrue(beforeRestoreSnapshot.contains("if (retainedState != null) {"));
        assertTrue(!beforeRestoreSnapshot.contains("else if (retainedState != null)"));
    }

    @Test
    public void loadInstalledApps_usesIconCacheEntryPoint() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String coordinatorSource = read("src/main/java/com/dpis/module/InstalledAppCatalogCoordinator.java");

        assertTrue(source.contains("installedAppCatalogCoordinator.loadInstalledApps("));
        assertTrue(coordinatorSource.contains("AppIconMemoryCache"));
        assertTrue(coordinatorSource.contains("loadAppIcon(packageManager, applicationInfo)"));
        assertTrue(!coordinatorSource.contains("getDefaultActivityIcon()"));
    }

    @Test
    public void appLoad_requestsXiaomiInstalledAppsPermissionBeforeQueryingPackages() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("XIAOMI_GET_INSTALLED_APPS_PERMISSION"));
        assertTrue(source.contains("com.android.permission.GET_INSTALLED_APPS"));
        assertTrue(source.contains("requestPermissions("));
        assertTrue(source.contains("REQUEST_XIAOMI_GET_INSTALLED_APPS"));
        assertTrue(source.contains("onRequestPermissionsResult("));
        assertTrue(source.contains("installedAppsPermissionRequestCompleted"));
        assertTrue(source.contains("isXiaomiInstalledAppsPermissionDeclared()"));
        assertTrue(source.contains("getPermissionInfo("));
        assertTrue(source.contains("dispatchMainUiAction(MainUiAction.requestAppsLoad(true));"));
        int requestLoadStart = source.indexOf("private void requestAppsLoad(boolean forceInstalledAppCatalogReload)");
        int requestLoadEnd = source.indexOf("private boolean ensureInstalledAppsPermissionBeforeLoad()",
                requestLoadStart);
        assertTrue(requestLoadStart >= 0);
        assertTrue(requestLoadEnd > requestLoadStart);
        String requestLoadBody = source.substring(requestLoadStart, requestLoadEnd);
        assertTrue(requestLoadBody.indexOf("ensureInstalledAppsPermissionBeforeLoad()") < requestLoadBody
                .indexOf("dispatchMainUiAction(MainUiAction.requestAppsLoad"));
    }

    @Test
    public void savesAndRestoresPageScrollStatesForRotation() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("STATE_PAGE_SCROLL_STATES"));
        assertTrue(source.contains("putSparseParcelableArray("));
        assertTrue(source.contains("capturePageScrollStates()"));
        assertTrue(source.contains("restorePageScrollStates("));
        assertTrue(source.contains("restoredPageScrollStates"));
    }

    @Test
    public void startupDisclaimerUsesMaterialDialogAndPersistsConsent() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String runtimeLayout = read("src/main/res/layout/dialog_module_runtime_reload_advice.xml");
        String strings = read("src/main/res/values/strings.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(source.contains("maybeShowModuleRuntimeReloadAdvice()"));
        assertTrue(source.contains("ModuleRuntimeReloadAdvisor.shouldShowReloadAdvice(this)"));
        assertTrue(source.contains("ModuleRuntimeReloadAdvisor.markReloadAdviceAcknowledged(this)"));
        assertTrue(source.contains("R.layout.dialog_module_runtime_reload_advice"));
        assertTrue(source.contains("new MaterialAlertDialogBuilder(this)"));
        assertTrue(source.contains("module_runtime_reload_ack_button"));
        assertTrue(!source.contains("ModuleRuntimeReloader.softReloadAsync("));
        assertTrue(!source.contains("module_runtime_reload_now_button"));
        assertTrue(!source.contains("module_runtime_reload_later_button"));
        assertTrue(runtimeLayout.contains("@drawable/ic_error_outline_24"));
        assertTrue(runtimeLayout.contains("module_runtime_reload_title"));
        assertTrue(runtimeLayout.contains("module_runtime_reload_message"));
        assertTrue(runtimeLayout.contains("module_runtime_reload_ack_button"));
        assertTrue(runtimeLayout.contains("@dimen/dialog_status_icon_size"));
        assertTrue(runtimeLayout.contains("@dimen/dialog_status_icon_padding"));
        assertTrue(runtimeLayout.contains("@dimen/dialog_surface_padding_horizontal"));
        assertTrue(runtimeLayout.contains("@dimen/dialog_body_spacing"));
        assertTrue(runtimeLayout.contains("@dimen/dialog_text_line_spacing"));
        assertTrue(runtimeLayout.contains("@dimen/dialog_action_spacing_top"));
        String runtimeMessage = stringEntry(strings, "module_runtime_reload_message");
        String zhRuntimeTitle = stringEntry(zhStrings, "module_runtime_reload_title");
        String zhRuntimeMessage = stringEntry(zhStrings, "module_runtime_reload_message");
        assertTrue(zhRuntimeTitle.contains("建议重启设备"));
        assertTrue(zhRuntimeMessage.contains("部分修改可能需要重启设备后才能完全生效"));
        assertTrue(!runtimeMessage.contains("HyperOS"));
        assertTrue(!runtimeMessage.contains("Rust"));
        assertTrue(!zhRuntimeMessage.contains("HyperOS"));
        assertTrue(!zhRuntimeMessage.contains("Rust"));
        assertTrue(source.contains("maybeShowStartupDisclaimerDialog()"));
        assertTrue(source.contains("if (!maybeShowStartupDisclaimerDialog()) {"));
        assertTrue(source.contains("startupUpdateDialogCoordinator().maybeShowStartupDisclaimerDialog("));
    }

    @Test
    public void startupUpdateCheckShowsPromptOnlyOncePerRemoteVersion() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String coordinatorSource = read("src/main/java/com/dpis/module/StartupUpdateCheckCoordinator.java");
        String downloadCoordinatorSource = read("src/main/java/com/dpis/module/UpdateDownloadCoordinator.java");
        String manifestFetcherSource = read("src/main/java/com/dpis/module/UpdateManifestFetcher.java");
        String storeSource = read("src/main/java/com/dpis/module/UpdateStateStore.java");

        assertTrue(source.contains("maybeCheckForUpdatesOnStartup();"));
        assertTrue(source.contains("new UpdateCoordinator("));
        assertTrue(source.contains("new StartupUpdateCheckCoordinator("));
        assertTrue(source.contains("StartupUpdateCheckOnce.consume()"));
        assertTrue(source.contains("startupUpdateCheckCoordinator.maybeCheckForUpdatesOnStartup();"));
        assertTrue(source.contains("private volatile boolean startupUpdateDownloadInProgress;"));
        assertTrue(source.contains("private volatile boolean startupUpdateDownloadCancelRequested;"));
        assertTrue(coordinatorSource.contains("updateCoordinator.evaluateStartupCheck("));
        assertTrue(coordinatorSource.contains("updateCoordinator.markStartupCheckStarted(state)"));
        assertTrue(coordinatorSource.contains("updateCoordinator.evaluatePromptDecision("));
        assertTrue(coordinatorSource.contains("updateCoordinator.markStartupCheckFinished("));
        assertTrue(coordinatorSource.contains("manifestFetcher.fetch("));
        assertTrue(storeSource.contains("KEY_LAST_UPDATE_CHECK_FAILED"));
        assertTrue(storeSource.contains("KEY_LAST_PROMPTED_UPDATE_VERSION_CODE"));
        assertTrue(manifestFetcherSource.contains("static StartupUpdateManifest fetch("));
        assertTrue(source.contains("markPromptedVersion("));
        assertTrue(downloadCoordinatorSource.contains("updateCoordinator.requestDownloadStart("));
        assertTrue(downloadCoordinatorSource.contains("updateCoordinator.requestDownloadCancel("));
        assertTrue(downloadCoordinatorSource.contains("updateCoordinator.markDownloadFinished("));
        assertTrue(downloadCoordinatorSource.contains("downloadExecutor.download("));
        assertTrue(source.contains("new StartupUpdatePackageHandler(this)"));
        assertTrue(downloadCoordinatorSource.contains("packageHandler.verifyDownloadedApk("));
        assertTrue(source.contains("startupUpdatePackageHandler.launchPackageInstaller(targetFile);"));
        assertTrue(source.contains("new ReleaseNotesController("));
        assertTrue(!source.contains("private void verifyDownloadedApk(File apkFile)"));
        assertTrue(source.contains("startupUpdateDialogCoordinator().showUpdateAvailableDialog("));
        assertTrue(source.contains("manifest.releaseNotes"));
        assertTrue(source.contains("startStartupUpdateDownload("));
        assertTrue(source.contains("startupUpdateDownloadInProgress = state.downloadInProgress;"));
        assertTrue(source.contains("startupUpdateDownloadCancelRequested = state.downloadCancelRequested;"));
        assertTrue(!source.contains("startActivity(AboutActivity.createStartupUpdateIntent("));
    }

    @Test
    public void startupDisclaimerLayoutKeepsScrollableContent() throws IOException {
        String layout = read("src/main/res/layout/dialog_startup_disclaimer.xml");

        assertTrue(layout.contains("androidx.core.widget.NestedScrollView"));
        assertTrue(layout.contains("startup_disclaimer_message"));
        assertTrue(layout.contains("startup_disclaimer_checkbox"));
        assertTrue(layout.contains("startup_disclaimer_accept_button"));
        assertTrue(layout.contains("startup_disclaimer_exit_button"));
        assertTrue(layout.contains("@dimen/dialog_surface_padding_horizontal"));
        assertTrue(layout.contains("@dimen/dialog_body_spacing"));
        assertTrue(layout.contains("@dimen/dialog_text_line_spacing"));
        assertTrue(layout.contains("@dimen/dialog_action_spacing_top"));
        assertTrue(layout.contains("@dimen/dialog_action_spacing_between"));
    }

    @Test
    public void appConfigLayoutUsesScrollableContainerAndAdaptiveModeRows() throws IOException {
        String layout = read("src/main/res/layout/dialog_app_config.xml");

        assertTrue(layout.contains("androidx.core.widget.NestedScrollView"));
        assertTrue(layout.contains("android:fillViewport=\"true\""));
        assertTrue(layout.contains("android:minHeight=\"@dimen/dialog_mode_toggle_row_min_height\""));
        assertTrue(!layout.contains("android:layout_height=\"@dimen/dialog_mode_toggle_row_height\""));
    }

    @Test
    public void applyFilter_submitsPerPageListsWithoutRedundantStatusRefresh() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        int applyFilterStart = source.indexOf("private void applyFilter() {");
        int applyFilterEnd = source.indexOf("private void showFilterDialog()", applyFilterStart);
        assertTrue(applyFilterStart >= 0);
        assertTrue(applyFilterEnd > applyFilterStart);

        String applyFilterBody = source.substring(applyFilterStart, applyFilterEnd);
        assertTrue(applyFilterBody.contains("pagerAdapter.submitPage("));
        assertTrue(!applyFilterBody.contains("pagerAdapter.refreshVisibleStatuses();"));
    }

    @Test
    public void pageRefresh_forcesInstalledAppCatalogReload() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        int refreshStart = source.indexOf("private void onPageRefreshRequested(AppListPage page) {");
        int refreshEnd = source.indexOf("private void onPageListScrolled(", refreshStart);
        assertTrue(refreshStart >= 0);
        assertTrue(refreshEnd > refreshStart);

        String refreshBody = source.substring(refreshStart, refreshEnd);
        assertTrue(refreshBody.contains("requestAppsLoad(true);"));
    }

    @Test
    public void appLoad_reusesInstalledAppCatalogBetweenRefreshes() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String viewModelSource = read("src/main/java/com/dpis/module/MainViewModel.java");
        String coordinatorSource = read("src/main/java/com/dpis/module/InstalledAppCatalogCoordinator.java");

        assertTrue(source.contains("INSTALLED_APP_CATALOG_TTL_MS"));
        assertTrue(source.contains("new InstalledAppCatalogCoordinator("));
        assertTrue(coordinatorSource.contains("getInstalledAppCatalog("));
        assertTrue(viewModelSource.contains("forceInstalledAppCatalogReloadRequested"));
        assertTrue(coordinatorSource.contains("cacheFresh"));
    }

    @Test
    public void retainedAppListSkipsImmediateServiceReloadOnRotation() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("private boolean skipNextImmediateServiceReload;"));
        assertTrue(source.contains("skipNextImmediateServiceReload = !initialAppsSnapshot.isEmpty();"));
        assertTrue(source.contains("DpisApplication.addServiceStateListener(this, true);"));
        assertTrue(source.contains("if (skipNextImmediateServiceReload)"));
    }

    @Test
    public void appConfigSheet_halfExpandedStateUsesDownwardOffset() throws IOException {
        String coordinatorSource = read("src/main/java/com/dpis/module/AppConfigDialogCoordinator.java");

        assertTrue(coordinatorSource.contains("R.dimen.dialog_app_config_half_expanded_down_offset"));
        assertTrue(coordinatorSource.contains("anchorBottom - sheetPos[1] - halfExpandedDownOffsetPx"));
    }

    @Test
    public void showEditDialog_delegatesSheetPresentationToCoordinator() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("new GlobalPrefillStore("));
        assertTrue(source.contains("AppConfigPrefillPreview.applyIfEligible("));
        assertTrue(source.contains("dialogView, sheetItem, systemHooksEnabled"));
        assertTrue(source.contains("new AppConfigDialogBinder(this, createAppConfigDialogHost()).bind("));
        assertTrue(source.contains("new AppConfigDialogCoordinator(this).show("));
        assertTrue(!source.contains("private void bindDialogValidation("));
        assertTrue(!source.contains("private void bindDialogActions("));
        assertTrue(!source.contains("private void refreshDialogState("));
    }

    @Test
    public void appConfigAndProcessActions_delegateToDedicatedHandlers() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("new ProcessActionHandler(this)"));
        assertTrue(source.contains("new AppConfigSaveHandler()"));
        assertTrue(source.contains("processActionHandler.execute(item, mappedAction);"));
        assertTrue(source.contains("appConfigSaveHandler.save("));
        assertTrue(source.contains("FontRuntimePropertySyncer.clearTargetAsync(packageName)"));
        assertTrue(!source.contains("private void runProcessAction(String packageName"));
        assertTrue(!source.contains("private int[] saveAppConfig(AppListItem item"));
    }

    @Test
    public void firstScreen_loadUsesPlaceholderAndAsyncIconWarmup() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String coordinatorSource = read("src/main/java/com/dpis/module/InstalledAppCatalogCoordinator.java");

        assertTrue(source.contains("installedAppCatalogCoordinator.onIconLoadRequested(packageName);"));
        assertTrue(coordinatorSource.contains("firstScreenIconWarmupLimit"));
        assertTrue(coordinatorSource.contains("maybeScheduleFirstScreenIconWarmup("));
        assertTrue(coordinatorSource.contains("pendingOnDemandIconLoads"));
        assertTrue(coordinatorSource.contains("resolveDisplayIcon(item)"));
        assertTrue(coordinatorSource.contains("scheduleIconRefresh();"));
        assertTrue(!coordinatorSource.contains("getDefaultActivityIcon()"));
    }

    @Test
    public void systemScopeAndHookStatus_delegateToCoordinator() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("new SystemScopeCoordinator(createSystemScopeHost())"));
        assertTrue(source.contains("systemScopeCoordinator.toggleScope("));
        assertTrue(source.contains("systemScopeCoordinator.resolveSystemHookEffectiveEnabled("));
        assertFalse(source.contains("openLsposedModuleSettings()"));
        assertFalse(source.contains("de.robv.android.xposed.intent.action.MODULE_SETTINGS"));
        assertTrue(!source.contains("private void toggleScope(String packageName"));
    }

    @Test
    public void touchFeedbackBinderProvidesSharedHapticAndScaleBehavior() throws IOException {
        String source = read("src/main/java/com/dpis/module/TouchFeedbackBinder.java");

        assertTrue(source.contains("final class TouchFeedbackBinder"));
        assertTrue(source.contains("bindPressScaleAndHaptic(View view)"));
        assertTrue(source.contains("performHapticFeedback(resolvePressHapticConstant())"));
        assertTrue(source.contains("HapticFeedbackConstants.CONFIRM"));
        assertTrue(source.contains("HapticFeedbackConstants.VIRTUAL_KEY"));
    }

    @Test
    public void applicationSyncsHyperOsNativeFontTargetsOnStartup() throws IOException {
        String source = read("src/main/java/com/dpis/module/DpisApplication.java");

        assertTrue(source.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore)"));
        assertTrue(source.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(remoteStore)"));
        assertTrue(!source
                .contains("HyperOsNativeProxyRefreshCoordinator.refreshConfiguredTargetsAsync(this, configStore)"));
        assertTrue(!source
                .contains("HyperOsNativeProxyRefreshCoordinator.refreshConfiguredTargetsAsync(this, remoteStore)"));
    }

    @Test
    public void appReceivesPackageReplacementWithoutAutoMountingHyperOsNativeProxy() throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");
        String receiver = read("src/main/java/com/dpis/module/DpisPackageLifecycleReceiver.java");

        assertTrue(manifest.contains(".DpisPackageLifecycleReceiver"));
        assertTrue(manifest.contains("android.intent.action.MY_PACKAGE_REPLACED"));
        assertTrue(receiver.contains("Intent.ACTION_MY_PACKAGE_REPLACED"));
        assertTrue(receiver.contains("HyperOsNativeProxyAssetExporter.exportBundledNativeProxyLibrary(context)"));
        assertTrue(!receiver
                .contains("HyperOsNativeProxyRefreshCoordinator.refreshConfiguredTargetsAsync(context, store)"));
        assertTrue(receiver.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(store)"));
    }

    @Test
    public void appConfigHostWiresFontHookDomainEditor() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("public void showFontHookDomains(AppListItem item,"));
        assertTrue(source.contains("AppConfigDialogBinder.AppConfigDialogState state,"));
        assertTrue(source.contains("MainActivity.this.showFontHookDomains(item, state, onStateChanged);"));
        assertTrue(source.contains("public String getFontHookDomainsButtonText(AppListItem item,"));
        assertTrue(source.contains("boolean previewFromGlobalPrefill,"));
        assertTrue(source.contains("MainActivity.this.getFontHookDomainsButtonText("));
        assertTrue(source.contains("new HookDomainOverrideStore(store)"));
        assertTrue(source.contains("FontHookDomainDialog.show(this,"));
        assertTrue(source.contains("overrideStore.saveCustomIfDifferentFromAutomatic("));
        assertTrue(source.contains("new HookDomainOverrideStore(store).restoreRecommended(packageName)"));
        assertTrue(source.contains("publishFontRuntimeTarget(packageName, store)"));
        assertTrue(source.contains("FontRuntimePropertySyncer.publishTargetAsync("));
        assertTrue(source.contains("HookExecutionPlanner.buildPlan("));
        assertTrue(source.contains("AppProcessHookInstaller.resolveDebugFontOverrideForPackage(packageName)"));
        assertTrue(source.contains("dialog_font_hook_domains_title_with_count"));
        assertTrue(source.contains("FontHookDomainRegistry.orderedCustomizableDisplayIdsList().size()"));
        assertTrue(source.contains("FontHookDomainRegistry.orderedCustomizableDisplaySubset("));
        assertTrue(source.contains("FontApplyMode.FIELD_REWRITE"));
        assertTrue(source.contains("ViewportApplyMode.OFF"));
    }

    @Test
    public void previewFontHookDomainEditorUsesSheetStateOnly() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        int methodStart = source.indexOf("private void showFontHookDomains(AppListItem item,");
        int methodEnd = source.indexOf("private static void publishFontRuntimeTarget", methodStart);
        String method = source.substring(methodStart, methodEnd);

        assertTrue(method.contains("boolean previewMode = state != null && state.previewFromGlobalPrefill"));
        assertFalse(method.contains("boolean previewMode = item.previewFromGlobalPrefill"));
        assertTrue(method.contains("HookDomainOverrideStore.fromRaw(state.previewFontHookDomainsRaw)"));
        assertTrue(method.contains("state.previewFontHookDomainsRaw = HookDomainOverrideStore.rawValueForSelection("));
        assertTrue(method.contains("state.previewFontHookDomainsRaw = null;"));
        assertTrue(method.contains("state.viewportApplyMode = ViewportApplyMode.normalize(mode);"));
        assertTrue(method.contains("previewMode ? state.viewportApplyMode : store.getTargetViewportApplyMode(item.packageName)"));
        assertTrue(method.contains("if (previewMode)"));

        int previewBranch = method.indexOf("if (previewMode)");
        int realStoreWrite = method.indexOf("overrideStore.saveCustomIfDifferentFromAutomatic(", previewBranch);
        int realRestore = method.indexOf("new HookDomainOverrideStore(store).restoreRecommended(packageName)", previewBranch);
        int realViewportWrite = method.indexOf("store.setTargetViewportApplyMode(packageName, mode)", previewBranch);
        assertTrue(realStoreWrite > previewBranch);
        assertTrue(realRestore > previewBranch);
        assertTrue(realViewportWrite > previewBranch);
        assertTrue(method.indexOf("return true;", previewBranch) < realStoreWrite);
        assertTrue(method.indexOf("return true;", realStoreWrite) < realRestore);
        assertTrue(method.indexOf("return true;", realRestore) < realViewportWrite);
    }

    @Test
    public void fontHookDomainButtonTextUsesMutablePreviewStateFlag() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        int methodStart = source.indexOf("private String getFontHookDomainsButtonText(AppListItem item,");
        int methodEnd = source.indexOf("private Set<String> resolveAutomaticFontHookDomains", methodStart);
        String method = source.substring(methodStart, methodEnd);

        assertTrue(method.contains("boolean previewFromGlobalPrefill,"));
        assertTrue(method.contains("HookDomainOverride override = previewFromGlobalPrefill"));
        assertFalse(method.contains("item.previewFromGlobalPrefill"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
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

    @Test
    public void hyperOsRestartPreparesNativeProxyBeforeProcessAction() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("executeDialogProcessActionAfterHyperOsProxyReady"));
        assertTrue(source.contains("shouldPrepareHyperOsNativeProxyForRestart(item)"));
        assertTrue(source.contains("DpiConfigStore store = getUiConfigStore();"));
        assertTrue(source.contains("store.isTargetDpisEnabled(item.packageName)"));
        assertTrue(source.contains("hasActiveStoredConfig(store, item.packageName)"));
        assertFalse(source.contains("store.getTargetTypefaceId(packageName)"));
        assertFalse(source.contains("typefaceId != null && !typefaceId.isBlank()"));
        assertFalse(source.contains("item.fontScalePercent != null\n                && item.fontScalePercent > 0"));
        assertFalse(source.contains("FontApplyMode.isEnabled"));
        assertTrue(source.contains("executeHyperOsNativeProxyMount(item, true, success ->"));
        assertTrue(source.contains("if (success)"));
        assertTrue(source.contains("processActionHandler.execute(item, mappedAction);"));
    }

}
