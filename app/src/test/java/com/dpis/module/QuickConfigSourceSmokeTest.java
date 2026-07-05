package com.dpis.module;

import com.dpis.module.diagnostics.FeedbackDiagnosticCoordinator;

import com.dpis.module.appconfig.AppConfigDialogBinder;

import com.dpis.module.runtime.font.FontRuntimePropertySyncer;

import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.ForegroundPackageResolver;

import com.dpis.module.quirks.WechatDpiSheetBinder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class QuickConfigSourceSmokeTest {
    @Test
    public void manifestDeclaresQuickSettingsTileAndUsageStatsPermission() throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");

        assertTrue(manifest.contains("android.permission.PACKAGE_USAGE_STATS"));
        assertTrue(manifest.contains("android:name=\".QuickConfigActivity\""));
        assertTrue(manifest.contains("@style/Theme.Dpis.QuickConfig"));
        assertTrue(manifest.contains("android:name=\".QuickConfigTileService\""));
        assertTrue(manifest.contains("android:icon=\"@drawable/ic_quick_config_24\""));
        assertTrue(manifest.contains("android.permission.BIND_QUICK_SETTINGS_TILE"));
        assertTrue(manifest.contains("android.service.quicksettings.action.QS_TILE"));
    }

    @Test
    public void quickConfigUsesAppConfigSheetForForegroundPackage() throws IOException {
        String activity = read("src/main/java/com/dpis/module/QuickConfigActivity.java");
        String resolver = read("src/main/java/com/dpis/module/applist/ForegroundPackageResolver.java");
        String tile = read("src/main/java/com/dpis/module/QuickConfigTileService.java");
        String styles = read("src/main/res/values/styles.xml");
        String panelBackground = read("src/main/res/drawable/bg_quick_config_panel.xml");

        assertTrue(activity.contains("EXTRA_PACKAGE_NAME"));
        assertTrue(activity.contains("new FrameLayout(this)"));
        assertTrue(activity.contains("R.layout.dialog_app_config"));
        assertTrue(activity.contains("panel.setBackgroundResource(R.drawable.bg_quick_config_panel);"));
        assertTrue(activity.contains("new AppConfigDialogBinder(this, appConfigDialogHost)"));
        assertTrue(activity.contains("InstalledAppCatalogCoordinator.createAppListItem("));
        assertTrue(styles.contains("Theme.Dpis.QuickConfig"));
        assertTrue(styles.contains("android:windowIsTranslucent"));
        assertTrue(panelBackground.contains("quick_config_panel_corner_radius"));
        assertTrue(resolver.contains("UsageStatsManager"));
        assertTrue(resolver.contains("UsageEvents.Event.MOVE_TO_FOREGROUND"));
        assertTrue(resolver.contains("SYSTEM_UI_PACKAGE"));
        assertTrue(tile.contains("QuickConfigActivity.createIntent("));
        assertTrue(tile.contains("startActivityAndCollapse"));
    }

    @Test
    public void quickConfigRoutesSheetActionsToExistingRuntimeSemantics() throws IOException {
        String activity = read("src/main/java/com/dpis/module/QuickConfigActivity.java");

        assertTrue(activity.contains("appConfigSaveHandler.save("));
        assertTrue(activity.contains("return finalizeSave(result, dialogView, item.packageName, dpisEnabled);"));
        assertTrue(activity.contains("WechatDpiSheetBinder.save(dialogView, packageName, dpisEnabled, store)"));
        assertTrue(activity.contains("systemScopeCoordinator.requestScope("));
        assertTrue(activity.contains("executeHyperOsNativeProxyMount(item, true, onFinished);"));
        assertTrue(activity.contains("executeDialogProcessAction(item, action);"));
        assertTrue(activity.contains("FontRuntimePropertySyncer.clearTargetAsync(packageName);"));
        assertFalse(activity.contains("quick_config_open_main_for_advanced"));
        assertFalse(activity.contains("showMainAppToast"));
        assertFalse(activity.contains("publishAfterSave(item.packageName);\n                    finish();"));
    }

    @Test
    public void quickConfigKeepsFeedbackDiagnosticSemanticsAvailable() throws IOException {
        String activity = read("src/main/java/com/dpis/module/QuickConfigActivity.java");

        assertTrue(activity.contains("new FeedbackDiagnosticCoordinator(createFeedbackDiagnosticHost())"));
        assertTrue(activity.contains("QuickConfigActivity.this.startFeedbackDiagnostic(item, state);"));
        assertTrue(activity.contains("FeedbackDiagnosticCoordinator.Request.fromPersisted("));
        assertTrue(activity.contains("showFeedbackDiagnosticPackagingDialog();"));
        assertTrue(activity.contains("showFeedbackDiagnosticResultSheet(finalBuilt);"));
        assertTrue(activity.contains("appConfigDialogHost.saveAppConfig("));
        assertFalse(activity.contains("dialog_feedback_diagnostic_button);\n        if (feedbackDiagnosticButton != null)"));
    }

    @Test
    public void quickConfigTileUsesDedicatedDisplaySettingsIcon() throws IOException {
        String tileIcon = read("src/main/res/drawable/ic_quick_config_24.xml");

        assertTrue(tileIcon.contains("android:viewportWidth=\"960\""));
        assertTrue(tileIcon.contains("M300,550v20"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
