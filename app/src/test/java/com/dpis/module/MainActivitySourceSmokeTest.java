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

        assertTrue(source.contains("help_fab"));
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

        int restoreSnapshotLine = source.indexOf("allApps.addAll(retainedState.appsSnapshot);");
        assertTrue(restoreSnapshotLine > 0);
        String beforeRestoreSnapshot = source.substring(0, restoreSnapshotLine);

        assertTrue(beforeRestoreSnapshot.contains("if (retainedState != null) {"));
        assertTrue(!beforeRestoreSnapshot.contains("else if (retainedState != null)"));
    }

    @Test
    public void loadInstalledApps_usesIconCacheEntryPoint() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("AppIconMemoryCache"));
        assertTrue(source.contains("loadAppIcon(packageManager, applicationInfo)"));
        assertTrue(!source.contains("Drawable icon = applicationInfo.loadIcon(packageManager);"));
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

        assertTrue(source.contains("maybeShowStartupDisclaimerDialog()"));
        assertTrue(source.contains("if (!maybeShowStartupDisclaimerDialog()) {"));
        assertTrue(source.contains("showStartupDisclaimerDialog(store, this::maybeCheckForUpdatesOnStartup);"));
        assertTrue(source.contains("new MaterialAlertDialogBuilder(this)"));
        assertTrue(source.contains("R.layout.dialog_startup_disclaimer"));
        assertTrue(source.contains("setStartupDisclaimerAccepted(true)"));
        assertTrue(source.contains("dialog.setCancelable(false)"));
        assertTrue(source.contains("setCanceledOnTouchOutside(false)"));
    }

    @Test
    public void startupUpdateCheckShowsPromptOnlyOncePerRemoteVersion() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("maybeCheckForUpdatesOnStartup();"));
        assertTrue(source.contains("UPDATE_STARTUP_CHECK_INTERVAL_MS"));
        assertTrue(source.contains("UPDATE_STARTUP_CHECK_FAILURE_RETRY_INTERVAL_MS"));
        assertTrue(source.contains("KEY_LAST_UPDATE_CHECK_FAILED"));
        assertTrue(source.contains("wasLastUpdateCheckFailed()"));
        assertTrue(source.contains("setLastUpdateCheckFailed(!requestSucceeded);"));
        assertTrue(source.contains("KEY_LAST_PROMPTED_UPDATE_VERSION_CODE"));
        assertTrue(source.contains("setLastPromptedUpdateVersionCode(manifest.versionCode);"));
        assertTrue(source.contains("dialogHandle.cancelButton.setOnClickListener"));
        assertTrue(source.contains("UpdateAvailableDialog.create("));
        assertTrue(source.contains("startStartupUpdateDownload("));
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
        int applyFilterEnd = source.indexOf("private void toggleScope(", applyFilterStart);
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

        assertTrue(source.contains("INSTALLED_APP_CATALOG_TTL_MS"));
        assertTrue(source.contains("getInstalledAppCatalog("));
        assertTrue(source.contains("forceInstalledAppCatalogReloadRequested"));
        assertTrue(source.contains("cacheFresh"));
    }

    @Test
    public void firstScreen_loadUsesPlaceholderAndAsyncIconWarmup() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("FIRST_SCREEN_ICON_WARMUP_LIMIT"));
        assertTrue(source.contains("maybeScheduleFirstScreenIconWarmup("));
        assertTrue(source.contains("private void onIconLoadRequested(String packageName)"));
        assertTrue(source.contains("pendingOnDemandIconLoads"));
        assertTrue(source.contains("resolveDisplayIcon(item)"));
        assertTrue(source.contains("scheduleIconRefresh();"));
        assertTrue(!source.contains("getDefaultActivityIcon()"));
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

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
