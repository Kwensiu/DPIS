package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeConfigDeliverySourceTest {
    @Test
    fun centralizesRemoteDeliveryResyncAfterRealConfigSaves() {
        val delivery =
            read("src/main/java/com/dpis/module/runtime/delivery/RuntimeConfigDelivery.kt")
        val mainActivity = read("src/main/java/com/dpis/module/MainActivity.kt")
        val runtimeLaunch = read(
            "src/main/java/com/dpis/module/runtime/presentation/RuntimeLaunchSession.kt"
        )
        val templateWorkspace = read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceCoordinator.kt")
        val templateHost = read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceActivityHost.kt")
        val saveEffects = read(
            "src/main/java/com/dpis/module/appconfig/presentation/MainWorkspaceEditorPostSaveEffects.kt",
        )
        val persister = read(
            "src/main/java/com/dpis/module/appconfig/editor/AppConfigEditorPersister.kt",
        )
        val fontLibrary = read("src/main/java/com/dpis/module/fonts/presentation/FontLibrarySession.kt")
        val fontDetail = read("src/main/java/com/dpis/module/fonts/presentation/FontDetailSession.kt")
        val systemHooks = read("src/main/java/com/dpis/module/settings/SystemHooksToggleController.java")
        val systemSettings = read("src/main/java/com/dpis/module/settings/presentation/SystemServerSettingsPageController.kt")

        assertTrue(delivery.contains("fun setLocalSnapshotReloader(reloader: Runnable?)"))
        assertTrue(delivery.contains("fun publishLocalSnapshotAfterSave()"))
        assertTrue(delivery.contains("localSnapshotReloader.run()"))
        assertTrue(
            read("src/main/java/com/dpis/module/DpisApplication.kt").contains(
                "RuntimeConfigDelivery.setLocalSnapshotReloader(Runnable { reloadConfigStore() })",
        ))
        assertTrue(runtimeLaunch.contains("onRuntimeConfigSaved()"))
        assertTrue(runtimeLaunch.contains("fun onRuntimeConfigSaved()"))
        assertTrue(runtimeLaunch.contains("RuntimeConfigDelivery.publishLocalSnapshotAfterSave()"))
        assertTrue(runtimeLaunch.contains("WechatDpiEditor.applyAfterPersist("))
        assertTrue(runtimeLaunch.contains("fun finalizeAppConfigSaveWithRuntimeSync("))
        assertTrue(saveEffects.contains("runtimeLaunchSession.finalizeAppConfigSaveWithRuntimeSync("))
        assertTrue(runtimeLaunch.contains("scheduleRuntimePropertiesForTargetLaunch(packageName)"))
        assertTrue(runtimeLaunch.contains("fun syncRuntimePropertiesForTargetLaunch(packageName: String?)"))
        assertTrue(runtimeLaunch.contains("ViewportPropertySyncer.syncTarget(packageName, store)"))
        assertTrue(runtimeLaunch.contains("FontRuntimePropertySyncer.syncTarget(packageName, store)"))
        assertTrue(runtimeLaunch.contains("ProcessActionHandler("))
        assertTrue(runtimeLaunch.contains("syncRuntimePropertiesForTargetLaunch(packageName)"))
        assertTrue(runtimeLaunch.contains("fun onRuntimeConfigSaved()"))
        assertTrue(persister.contains("saveHandler.saveResolved("))
        assertTrue(saveEffects.contains("syncHyperOsNativeProxyAfterSave(item)"))
        assertTrue(templateWorkspace.contains("if (result.successCount() > 0)"))
        assertTrue(templateWorkspace.contains("host.onTemplateRuntimeConfigSaved()"))
        assertTrue(templateHost.contains("runtimeLaunchSession.onRuntimeConfigSaved()"))
        assertTrue(occurrences(fontLibrary, "RuntimeConfigDelivery.publishLocalSnapshotAfterSave()") >= 3)
        assertTrue(occurrences(fontDetail, "RuntimeConfigDelivery.publishLocalSnapshotAfterSave()") >= 3)
        assertTrue(systemHooks.contains("RuntimeConfigDelivery::publishLocalSnapshotAfterSave"))
        assertTrue(systemSettings.contains("RuntimeConfigDelivery.publishLocalSnapshotAfterSave()"))
    }

    @Test
    fun activeFontLibraryStoreUsesLocalPreferencesOnly() {
        val factory = read("src/main/java/com/dpis/module/runtime/ConfigStoreFactory.kt")
        val activeFontFactory = activeFontLibraryFactoryBlock(factory)

        assertTrue(activeFontFactory.contains("= createLocalFontLibraryStore(context)"))
        assertFalse(activeFontFactory.contains("getRemotePreferences"))
    }

    private fun activeFontLibraryFactoryBlock(source: String): String {
        val start = source.indexOf("fun createLocalUiFontLibraryStore(")
        return source.substring(start, source.indexOf("fun createForXposedHost", start))
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
