package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeConfigDeliverySourceTest {
    @Test
    fun centralizesRemoteDeliveryResyncAfterRealConfigSaves() {
        val delivery = read("src/main/java/com/dpis/module/runtime/RuntimeConfigDelivery.java")
        val mainActivity = read("src/main/java/com/dpis/module/MainActivity.java")
        val templateWorkspace = read("src/main/java/com/dpis/module/templates/TemplateWorkspaceCoordinator.kt")
        val templateHost = read("src/main/java/com/dpis/module/templates/TemplateWorkspaceActivityHost.kt")
        val appConfigHost = hostBlock(mainActivity)
        val sheetActions = read("src/main/java/com/dpis/module/appconfig/AppConfigSheetActionBinder.java")
        val fontLibrary = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.java")
        val fontDetail = read("src/main/java/com/dpis/module/fonts/FontDetailActivity.java")
        val systemHooks = read("src/main/java/com/dpis/module/settings/SystemHooksToggleController.java")
        val systemSettings = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.kt")

        assertTrue(delivery.contains("public static void setLocalSnapshotReloader(Runnable reloader)"))
        assertTrue(delivery.contains("public static void publishLocalSnapshotAfterSave()"))
        assertTrue(delivery.contains("localSnapshotReloader.run();"))
        assertTrue(read("src/main/java/com/dpis/module/DpisApplication.java").contains(
            "RuntimeConfigDelivery.setLocalSnapshotReloader(DpisApplication::reloadConfigStore);",
        ))
        assertTrue(mainActivity.contains("public void onRuntimeConfigSaved()"))
        assertTrue(mainActivity.contains("RuntimeConfigDelivery.publishLocalSnapshotAfterSave();"))
        assertTrue(mainActivity.contains("private AppConfigSaveHandler.Result finalizeAppConfigSaveWithWechatDpi("))
        assertTrue(mainActivity.contains("AppConfigSaveHandler.Result finalizeAppConfigSaveWithRuntimeSync("))
        assertTrue(mainActivity.contains("return finalizeAppConfigSaveWithRuntimeSync("))
        assertTrue(mainActivity.contains("scheduleRuntimePropertiesForTargetLaunch(packageName);"))
        assertTrue(mainActivity.contains("private void syncRuntimePropertiesForTargetLaunch(String packageName)"))
        assertTrue(mainActivity.contains("ViewportPropertySyncer.syncTarget(packageName, store);"))
        assertTrue(mainActivity.contains("FontRuntimePropertySyncer.syncTarget(packageName, store);"))
        assertTrue(mainActivity.contains("new ProcessActionHandler(this, this::syncRuntimePropertiesForTargetLaunch)"))
        assertTrue(appConfigHost.contains("public void onRuntimeConfigSaved()"))
        assertTrue(appConfigHost.contains("MainActivity.this.onRuntimeConfigSaved();"))
        assertTrue(sheetActions.contains("AppConfigSaveHandler.Result result = host.saveAppConfig("))
        assertTrue(templateWorkspace.contains("if (result.successCount() > 0)"))
        assertTrue(templateWorkspace.contains("host.onTemplateRuntimeConfigSaved()"))
        assertTrue(templateHost.contains("activity.onRuntimeConfigSaved()"))
        assertTrue(occurrences(fontLibrary, "RuntimeConfigDelivery.publishLocalSnapshotAfterSave();") >= 3)
        assertTrue(occurrences(fontDetail, "RuntimeConfigDelivery.publishLocalSnapshotAfterSave();") >= 3)
        assertTrue(systemHooks.contains("RuntimeConfigDelivery::publishLocalSnapshotAfterSave"))
        assertTrue(systemSettings.contains("RuntimeConfigDelivery.publishLocalSnapshotAfterSave()"))
    }

    @Test
    fun activeFontLibraryStoreUsesLocalPreferencesOnly() {
        val factory = read("src/main/java/com/dpis/module/ConfigStoreFactory.java")
        val activeFontFactory = activeFontLibraryFactoryBlock(factory)

        assertTrue(activeFontFactory.contains("return createLocalFontLibraryStore(context);"))
        assertFalse(activeFontFactory.contains("getRemotePreferences"))
    }

    private fun hostBlock(source: String): String {
        val start = source.indexOf("public AppConfigSaveHandler.Result saveAppConfig(")
        return source.substring(start, source.indexOf("public void onDraftStateChanged", start))
    }

    private fun activeFontLibraryFactoryBlock(source: String): String {
        val start = source.indexOf("static FontLibraryStore createLocalUiFontLibraryStore(")
        return source.substring(start, source.indexOf("static DpisConfigStore createForXposedHost", start))
    }

    private fun occurrences(value: String, needle: String): Int {
        var count = 0
        var index = 0
        while (true) {
            index = value.indexOf(needle, index)
            if (index < 0) return count
            count++
            index += needle.length
        }
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
