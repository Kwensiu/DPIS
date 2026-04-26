package com.dpis.module;

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

        assertTrue(source.contains("maybeShowModuleRuntimeReloadAdvice()"));
        assertTrue(source.contains("ModuleRuntimeReloadAdvisor.shouldShowReloadAdvice(this)"));
        assertTrue(source.contains("ModuleRuntimeReloadAdvisor.markReloadAdviceAcknowledged(this)"));
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
        assertTrue(source.contains("startupUpdateCheckCoordinator.maybeCheckForUpdatesOnStartup();"));
        assertTrue(source.contains("private volatile boolean startupUpdateDownloadInProgress;"));
        assertTrue(source.contains("private volatile boolean startupUpdateDownloadCancelRequested;"));
        assertTrue(coordinatorSource.contains("if (state.startupCheckInProgress) {"));
        assertTrue(coordinatorSource.contains("updateCoordinator.markStartupCheckStarted(state)"));
        assertTrue(!coordinatorSource.contains("updateCoordinator.evaluateStartupCheck("));
        assertTrue(coordinatorSource.contains("updateCoordinator.evaluatePromptDecision("));
        assertTrue(coordinatorSource.contains("updateCoordinator.markStartupCheckFinished("));
        assertTrue(coordinatorSource.contains("UpdateManifestFetcher.fetch("));
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
        assertTrue(!source.contains("private void verifyDownloadedApk(File apkFile)"));
        assertTrue(source.contains("startupUpdateDialogCoordinator().showUpdateAvailableDialog("));
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
        assertTrue(source.contains("HyperOsNativeFontPropertySyncer.clearFontTargetAsync(packageName)"));
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

        assertTrue(source.contains("HyperOsNativeFontPropertySyncer.syncConfiguredFontTargetsAsync(configStore)"));
        assertTrue(source.contains("HyperOsNativeFontPropertySyncer.syncConfiguredFontTargetsAsync(remoteStore)"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
