package com.dpis.module.config

import com.dpis.module.FakePrefs
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.templates.GlobalPrefillStore
import com.dpis.module.templates.QuickTemplateStore
import com.dpis.module.templates.TemplateConfigValue
import com.dpis.module.templates.TemplateConfigValueAdapters
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeConfigSnapshotPublisherTest {
    @Test
    fun oneTimeRemoteSeedPreservesMissingValuesAndLocalWinsConflicts() {
        val localPreferences = FakePrefs()
        val localStore = DpisConfigStore(localPreferences)
        val localPrefill = config(411, "font_local")
        assertTrue(localStore.setWechatDpi("com.tencent.mm", 600))
        assertTrue(localStore.setHyperOsFlutterFontHookEnabled(false))
        assertTrue(localStore.setStartupDisclaimerAccepted(true))
        assertTrue(localStore.setInterfaceScalePercent(73))
        assertTrue(GlobalPrefillStore(localPreferences).write(localPrefill))
        assertTrue(QuickTemplateStore(localPreferences).save(template("local-template")))

        val remotePreferences = FakePrefs()
        val remoteStore = DpisConfigStore(remotePreferences)
        assertTrue(remoteStore.setWechatDpi("com.tencent.mm", 480))
        assertTrue(remoteStore.setHyperOsFlutterFontHookEnabled(true))
        assertTrue(remoteStore.setTargetFontScalePercent("com.example.stale", 200))
        assertTrue(remoteStore.setStartupDisclaimerAccepted(true))
        assertTrue(remoteStore.setInterfaceScalePercent(54))
        assertTrue(GlobalPrefillStore(remotePreferences).write(config(512, "font_remote")))
        assertTrue(QuickTemplateStore(remotePreferences).save(template("remote-template")))

        assertTrue(RuntimeConfigSnapshotPublisher.publish(localStore, remoteStore))

        assertEquals(Integer.valueOf(600), remoteStore.getWechatDpi("com.tencent.mm"))
        assertFalse(remoteStore.isHyperOsFlutterFontHookEnabled)
        assertEquals(
            Integer.valueOf(200),
            localStore.getTargetFontScalePercent("com.example.stale")
        )
        assertEquals(
            Integer.valueOf(200),
            remoteStore.getTargetFontScalePercent("com.example.stale")
        )
        assertFalse(remoteStore.isStartupDisclaimerAccepted)
        assertEquals(
            AppUiScaleManager.DEFAULT_SCALE_PERCENT,
            remoteStore.interfaceScalePercent,
        )
        assertEquals(TemplateConfigValue.EMPTY, GlobalPrefillStore(remotePreferences).read())
        assertNull(QuickTemplateStore(remotePreferences).read("remote-template"))
        assertNull(QuickTemplateStore(remotePreferences).read("local-template"))
        assertFalse(remoteStore.snapshotAll().keys.any { it?.startsWith("runtime.") == true })
        assertFalse(localStore.snapshotBackup().keys.any { it.startsWith("runtime.") })

        assertEquals(Integer.valueOf(600), localStore.getWechatDpi("com.tencent.mm"))
        assertFalse(localStore.isHyperOsFlutterFontHookEnabled)
        assertTrue(localStore.isStartupDisclaimerAccepted)
        assertEquals(73, localStore.interfaceScalePercent)
        assertEquals(localPrefill, GlobalPrefillStore(localPreferences).read())
        assertEquals(
            "local-template", QuickTemplateStore(localPreferences)
                .read("local-template")?.name
        )
    }

    @Test
    fun remoteSeedDoesNotImportInvalidOrUnknownPackageFields() {
        val localStore = DpisConfigStore(FakePrefs())
        val remotePreferences = FakePrefs().apply {
            edit()
                .putInt("font.com.example.app.scale_percent", 9999)
                .putString("font.com.example.app.unknown_route", "custom")
                .commit()
        }
        val remoteStore = DpisConfigStore(remotePreferences)

        assertTrue(RuntimeConfigSnapshotPublisher.publish(localStore, remoteStore))

        assertNull(localStore.getTargetFontScalePercent("com.example.app"))
        assertFalse(localStore.snapshotAll().containsKey("font.com.example.app.scale_percent"))
        assertFalse(localStore.snapshotAll().containsKey("font.com.example.app.unknown_route"))
    }

    @Test
    fun completedRemoteSeedDoesNotRestoreLocallyDeletedConfig() {
        val localStore = DpisConfigStore(FakePrefs())
        val remoteStore = DpisConfigStore(FakePrefs()).apply {
            assertTrue(setWechatDpi("com.tencent.mm", 480))
        }

        assertTrue(RuntimeConfigSnapshotPublisher.publish(localStore, remoteStore))
        assertEquals(Integer.valueOf(480), localStore.getWechatDpi("com.tencent.mm"))

        assertTrue(localStore.clearWechatDpi("com.tencent.mm"))
        assertTrue(RuntimeConfigSnapshotPublisher.publish(localStore, remoteStore))

        assertNull(localStore.getWechatDpi("com.tencent.mm"))
        assertNull(remoteStore.getWechatDpi("com.tencent.mm"))
    }

    @Test
    fun failedRemotePublicationLeavesLocalConfigAvailable() {
        val localStore = DpisConfigStore(FakePrefs()).apply {
            assertTrue(setWechatDpi("com.tencent.mm", 600))
        }
        val remotePreferences = FakePrefs()
        val remoteStore = DpisConfigStore(remotePreferences).apply {
            assertTrue(setWechatDpi("com.tencent.mm", 480))
        }
        remotePreferences.setCommitResult(false)

        assertFalse(RuntimeConfigSnapshotPublisher.publish(localStore, remoteStore))

        assertEquals(Integer.valueOf(600), localStore.getWechatDpi("com.tencent.mm"))
        assertEquals(Integer.valueOf(480), remoteStore.getWechatDpi("com.tencent.mm"))
    }

    @Test
    fun missingRemoteStoreDoesNotInvalidateLocalConfig() {
        val localStore = DpisConfigStore(FakePrefs()).apply {
            assertTrue(setWechatDpi("com.tencent.mm", 600))
        }

        assertFalse(RuntimeConfigSnapshotPublisher.publish(localStore, null))

        assertEquals(Integer.valueOf(600), localStore.getWechatDpi("com.tencent.mm"))
    }

    private fun config(widthDp: Int, typefaceId: String) =
        TemplateConfigValueAdapters.fromViewportTargetSpec(
            ViewportTargetSpec.absoluteDp(widthDp),
            ViewportApplyMode.AUTO,
            120,
            FontApplyMode.SYSTEM_EMULATION,
            typefaceId,
            "resources_font",
        )

    private fun template(id: String) = QuickTemplateStore.QuickTemplate(
        id,
        id,
        1000L,
        linkedSetOf<String?>("com.example.app"),
        config(411, "font_template"),
    )
}
