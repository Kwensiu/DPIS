package com.dpis.module;

import com.dpis.module.applist.InstalledAppCatalogCoordinator;


import com.dpis.module.diagnostics.DiagnosticCoordinator;

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
        assertTrue(manifest.contains("android:taskAffinity=\"\""));
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
        String manifestTile = read("src/main/java/com/dpis/module/QuickConfigTileService.java");
        String tile = read("src/main/java/com/dpis/module/quickconfig/QuickConfigTileService.java");
        String styles = read("src/main/res/values/styles.xml");
        String content = read("src/main/java/com/dpis/module/ui/compose/QuickConfigContent.kt");

        assertTrue(activity.contains("EXTRA_PACKAGE_NAME"));
        assertTrue(activity.contains("SupportActivityContent.installQuickConfig(this, presentation)"));
        assertTrue(activity.contains("new AppConfigEditorPresentation.State("));
        assertTrue(content.contains("DpisSheetVisualChrome()"));
        assertFalse(content.contains("extraTopPadding = 12.dp"));
        assertTrue(content.contains("AppHookChainEditorPage(state = state)"));
        assertFalse(content.contains("startFeedbackDiagnostic"));
        assertTrue(content.contains("contentAlignment = Alignment.BottomCenter"));
        assertTrue(content.contains("RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)"));
        assertTrue(activity.contains("InstalledAppCatalogCoordinator.createAppListItem("));
        assertTrue(styles.contains("Theme.Dpis.QuickConfig"));
        assertTrue(styles.contains("android:windowIsTranslucent"));
        assertTrue(resolver.contains("UsageStatsManager"));
        assertTrue(resolver.contains("AppOpsManager.OPSTR_GET_USAGE_STATS"));
        assertTrue(activity.contains("QuickConfigTargetDecision.decide("));
        assertTrue(activity.contains("Settings.ACTION_USAGE_ACCESS_SETTINGS"));
        assertTrue(activity.contains("Uri.parse(\"package:\" + getPackageName())"));
        assertTrue(activity.contains("new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)"));
        assertTrue(resolver.contains("UsageEvents.Event.MOVE_TO_FOREGROUND"));
        assertTrue(resolver.contains("SYSTEM_UI_PACKAGE"));
        assertTrue(manifestTile.contains("extends com.dpis.module.quickconfig.QuickConfigTileService"));
        assertTrue(tile.contains("QuickConfigActivity.createIntent("));
        assertTrue(tile.contains("startActivityAndCollapse"));
    }

    @Test
    public void quickConfigRoutesSheetActionsToExistingRuntimeSemantics() throws IOException {
        String activity = read("src/main/java/com/dpis/module/QuickConfigActivity.java");

        assertTrue(activity.contains("appConfigSaveHandler.saveResolved("));
        assertTrue(activity.contains("WechatDpiSheetBinder.save("));
        assertTrue(activity.contains(
                "draft.wechatDpiInput, item.packageName, draft.dpisEnabled, getHookConfigStore()"));
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

        assertTrue(activity.contains("new DiagnosticCoordinator(createFeedbackDiagnosticHost())"));
        assertTrue(activity.contains("QuickConfigActivity.this.startFeedbackDiagnostic("));
        assertTrue(activity.contains("DiagnosticCoordinator.Request.fromPersisted("));
        assertTrue(activity.contains("ComposeConfirmDialog.showWithLabels("));
        assertTrue(activity.contains("ComposeMessageDialog.show("));
        assertFalse(activity.contains("MaterialAlertDialogBuilder"));
        assertTrue(activity.contains("showPackagingDialog();"));
        assertTrue(activity.contains("showDiagnosticResultSheet(finalBuilt);"));
        assertTrue(activity.contains("saveComposeEditor(item, editingDraft)"));
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
