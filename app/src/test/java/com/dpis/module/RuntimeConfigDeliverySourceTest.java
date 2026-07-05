package com.dpis.module;

import com.dpis.module.viewport.ViewportPropertySyncer;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class RuntimeConfigDeliverySourceTest {
    @Test
    public void centralizesRemoteDeliveryResyncAfterRealConfigSaves() throws IOException {
        String delivery = read("src/main/java/com/dpis/module/RuntimeConfigDelivery.java");
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");
        String appConfigHost = hostBlock(mainActivity);
        String sheetActions = read("src/main/java/com/dpis/module/AppConfigSheetActionBinder.java");
        String fontLibrary = read("src/main/java/com/dpis/module/FontLibraryActivity.java");
        String systemHooks = read("src/main/java/com/dpis/module/SystemHooksToggleController.java");
        String systemSettings = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");

        assertTrue(delivery.contains("static void publishLocalSnapshotAfterSave()"));
        assertTrue(delivery.contains("DpisApplication.reloadConfigStore();"));
        assertTrue(mainActivity.contains("private void onRuntimeConfigSaved()"));
        assertTrue(mainActivity.contains("RuntimeConfigDelivery.publishLocalSnapshotAfterSave();"));
        assertTrue(mainActivity.contains("private AppConfigSaveHandler.Result finalizeAppConfigSaveWithWechatDpi("));
        assertTrue(mainActivity.contains("private AppConfigSaveHandler.Result finalizeAppConfigSaveWithRuntimeSync("));
        assertTrue(mainActivity.contains("return finalizeAppConfigSaveWithRuntimeSync("));
        assertTrue(mainActivity.contains("scheduleRuntimePropertiesForTargetLaunch(packageName);"));
        assertTrue(mainActivity.contains("private void syncRuntimePropertiesForTargetLaunch(String packageName)"));
        assertTrue(mainActivity.contains("ViewportPropertySyncer.syncTarget(packageName, store);"));
        assertTrue(mainActivity.contains("FontRuntimePropertySyncer.syncTarget(packageName, store);"));
        assertTrue(mainActivity.contains("new ProcessActionHandler(this, this::syncRuntimePropertiesForTargetLaunch)"));
        assertTrue(appConfigHost.contains("public void onRuntimeConfigSaved()"));
        assertTrue(appConfigHost.contains("MainActivity.this.onRuntimeConfigSaved();"));
        assertTrue(sheetActions.contains("AppConfigSaveHandler.Result result = host.saveAppConfig("));
        assertTrue(mainActivity.contains("if (result.successCount() > 0)"));
        assertTrue(mainActivity.contains("onRuntimeConfigSaved();"));
        assertTrue(occurrences(fontLibrary, "RuntimeConfigDelivery.publishLocalSnapshotAfterSave();") >= 4);
        assertTrue(systemHooks.contains("RuntimeConfigDelivery::publishLocalSnapshotAfterSave"));
        assertTrue(systemSettings.contains("RuntimeConfigDelivery.publishLocalSnapshotAfterSave();"));
    }

    @Test
    public void activeFontLibraryStoreUsesLocalPreferencesOnly() throws IOException {
        String factory = read("src/main/java/com/dpis/module/ConfigStoreFactory.java");
        String activeFontFactory = activeFontLibraryFactoryBlock(factory);

        assertTrue(activeFontFactory.contains("return createLocalFontLibraryStore(context);"));
        assertTrue(!activeFontFactory.contains("getRemotePreferences"));
    }

    private static String hostBlock(String source) {
        int start = source.indexOf("public AppConfigSaveHandler.Result saveAppConfig(");
        int end = source.indexOf("public void onDraftStateChanged", start);
        return source.substring(start, end);
    }

    private static String activeFontLibraryFactoryBlock(String source) {
        int start = source.indexOf("static FontLibraryStore createLocalUiFontLibraryStore(");
        int end = source.indexOf("static DpisConfigStore createForXposedHost", start);
        return source.substring(start, end);
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
