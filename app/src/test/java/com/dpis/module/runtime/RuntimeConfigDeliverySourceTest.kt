package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dpis.module.process.presentation.ProcessActionHandler
import com.dpis.module.runtime.ConfigStoreFactory
import com.dpis.module.config.DpisConfigStore

class RuntimeConfigDeliverySourceTest {
    @Test
    fun centralizesRemoteDeliveryResyncAfterRealConfigSaves() {
        val delivery = read("src/main/java/com/dpis/module/runtime/RuntimeConfigDelivery.java")
        val mainActivity = read("src/main/java/com/dpis/module/MainActivity.java")
        val templateWorkspace = read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceCoordinator.kt")
        val templateHost = read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceActivityHost.kt")
        val appConfigHost = read(
            "src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogActivityHost.kt"
        )
        val sheetActions = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetActionBinder.kt")
        val fontLibrary = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.kt")
        val fontDetail = read("src/main/java/com/dpis/module/fonts/FontDetailActivity.kt")
        val systemHooks = read("src/main/java/com/dpis/module/settings/SystemHooksToggleController.java")
        val systemSettings = read("src/main/java/com/dpis/module/settings/presentation/SystemServerSettingsPageController.kt")

        assertTrue(delivery.contains("public static void setLocalSnapshotReloader(Runnable reloader)"))
        assertTrue(delivery.contains("public static void publishLocalSnapshotAfterSave()"))
        assertTrue(delivery.contains("localSnapshotReloader.run();"))
        assertTrue(
            read("src/main/java/com/dpis/module/DpisApplication.kt").contains(
                "RuntimeConfigDelivery.setLocalSnapshotReloader(Runnable { reloadConfigStore() })",
        ))
        assertTrue(mainActivity.contains("public void onRuntimeConfigSaved()"))
        assertTrue(mainActivity.contains("RuntimeConfigDelivery.publishLocalSnapshotAfterSave();"))
        assertTrue(mainActivity.contains("private AppConfigSaveHandler.Result finalizeAppConfigSaveWithWechatDpi("))
        assertTrue(mainActivity.contains("AppConfigSaveHandler.Result finalizeAppConfigSaveWithRuntimeSync("))
        assertTrue(appConfigHost.contains("activity.finalizeAppConfigSaveWithRuntimeSync("))
        assertTrue(mainActivity.contains("scheduleRuntimePropertiesForTargetLaunch(packageName);"))
        assertTrue(mainActivity.contains("void syncRuntimePropertiesForTargetLaunch(String packageName)"))
        assertTrue(mainActivity.contains("ViewportPropertySyncer.syncTarget(packageName, store);"))
        assertTrue(mainActivity.contains("FontRuntimePropertySyncer.syncTarget(packageName, store);"))
        assertTrue(mainActivity.contains("new ProcessActionHandler("))
        assertTrue(mainActivity.contains("this::syncRuntimePropertiesForTargetLaunch"))
        assertTrue(appConfigHost.contains("override fun onRuntimeConfigSaved()"))
        assertTrue(appConfigHost.contains("activity.onRuntimeConfigSaved()"))
        assertTrue(sheetActions.contains("val result = host.saveAppConfig("))
        assertTrue(templateWorkspace.contains("if (result.successCount() > 0)"))
        assertTrue(templateWorkspace.contains("host.onTemplateRuntimeConfigSaved()"))
        assertTrue(templateHost.contains("activity.onRuntimeConfigSaved()"))
        assertTrue(occurrences(fontLibrary, "RuntimeConfigDelivery.publishLocalSnapshotAfterSave()") >= 3)
        assertTrue(occurrences(fontDetail, "RuntimeConfigDelivery.publishLocalSnapshotAfterSave()") >= 3)
        assertTrue(systemHooks.contains("RuntimeConfigDelivery::publishLocalSnapshotAfterSave"))
        assertTrue(systemSettings.contains("RuntimeConfigDelivery.publishLocalSnapshotAfterSave()"))
    }

    @Test
    fun activeFontLibraryStoreUsesLocalPreferencesOnly() {
        val factory = read("src/main/java/com/dpis/module/runtime/ConfigStoreFactory.java")
        val activeFontFactory = activeFontLibraryFactoryBlock(factory)

        assertTrue(activeFontFactory.contains("return createLocalFontLibraryStore(context);"))
        assertFalse(activeFontFactory.contains("getRemotePreferences"))
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
