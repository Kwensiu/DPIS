package com.dpis.module.config
import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.templates.GlobalPrefillStore
import com.dpis.module.templates.QuickTemplateStore
import com.dpis.module.templates.TemplateConfigValue
import com.dpis.module.templates.TemplateConfigValueAdapters
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    @Test
    fun snapshotBackupExcludesFontLibraryMetadataButKeepsTypefaceSelection() {
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, setOf("com.max.xiaoheihe"))
            .putString("font.com.max.xiaoheihe.typeface_id", "font_abcd1234")
            .putString("package_config.com.max.xiaoheihe.font.typeface_id", "font_abcd1234")
            .putString("font.library.entries", "[{\"id\":\"font_abcd1234\"}]")
            .putBoolean("font.debug.overlay_enabled", true)
            .putString("runtime.log.ring", "debug log")
            .putString("runtime.log.token", "token")
            .commit()
        val store = DpisConfigStore(prefs)

        val snapshot = store.snapshotBackup()

        assertEquals(setOf("com.max.xiaoheihe"), snapshot[DpisConfigStore.KEY_TARGET_PACKAGES])
        assertFalse(snapshot.containsKey("font.com.max.xiaoheihe.typeface_id"))
        assertEquals("font_abcd1234", snapshot["package_config.com.max.xiaoheihe.font.typeface_id"])
        assertFalse(snapshot.containsKey("font.library.entries"))
        assertFalse(snapshot.containsKey("font.debug.overlay_enabled"))
        assertFalse(snapshot.containsKey("runtime.log.ring"))
        assertFalse(snapshot.containsKey("runtime.log.token"))
    }

    @Test
    fun snapshotBackupIncludesPrefillAndTemplateKeysWithoutFontLibraryMetadata() {
        val prefs = FakePrefs()
        assertTrue(
            GlobalPrefillStore(prefs).write(
                TemplateConfigValueAdapters.fromViewportTargetSpec(
                    ViewportTargetSpec.absoluteDp(411),
                    ViewportApplyMode.AUTO,
                    120,
                    FontApplyMode.FIELD_REWRITE,
                    "missing_font_id",
                    "resources_font",
                ),
            ),
        )
        assertTrue(
            QuickTemplateStore(prefs).save(
                QuickTemplateStore.QuickTemplate(
                    "template_a",
                    "Compact",
                    1000L,
                    setOf("com.example.app"),
                    TemplateConfigValueAdapters.fromViewportTargetSpec(
                        ViewportTargetSpec.relativeScale(110000),
                        ViewportApplyMode.COMPAT,
                        115,
                        FontApplyMode.SYSTEM_EMULATION,
                        "missing_template_font_id",
                        "textview_sp",
                    ),
                ),
            ),
        )
        prefs.edit()
            .putString("font.library.entries", "[{\"id\":\"missing_font_id\"}]")
            .putString("font.library.migration_state", "done")
            .putBoolean("font.debug.overlay_enabled", true)
            .putString("runtime.log.ring", "debug log")
            .commit()
        val store = DpisConfigStore(prefs)

        val all = store.snapshotAll()
        val backup = store.snapshotBackup()

        assertEquals("missing_font_id", all["default_config.font.typeface_id"])
        assertEquals("Compact", all["template.template_a.name"])
        assertFalse(backup.containsKey(DpisConfigStore.KEY_TARGET_PACKAGES))
        assertEquals("missing_font_id", backup["default_config.font.typeface_id"])
        assertEquals(411, backup["default_config.viewport.width_dp"])
        assertEquals(ViewportApplyMode.AUTO, backup["default_config.viewport.mode"])
        assertEquals(120, backup["default_config.font.scale_percent"])
        assertEquals("resources_font", backup["default_config.font.hook_domains"])
        assertEquals(setOf("template_a"), backup[QuickTemplateStore.KEY_TEMPLATE_IDS])
        assertEquals("Compact", backup["template.template_a.name"])
        assertEquals(1000L, backup["template.template_a.updated_at"])
        assertEquals(setOf("com.example.app"), backup["template.template_a.selected_packages"])
        assertEquals(
            ViewportTargetType.RELATIVE_SCALE,
            backup["template.template_a.config.viewport.target_type"],
        )
        assertEquals(1100, backup["template.template_a.config.viewport.scale_permille"])
        assertEquals(ViewportApplyMode.COMPAT, backup["template.template_a.config.viewport.mode"])
        assertEquals(115, backup["template.template_a.config.font.scale_percent"])
        assertEquals(
            FontApplyMode.SYSTEM_EMULATION,
            backup["template.template_a.config.font.mode"],
        )
        assertEquals(
            "missing_template_font_id",
            backup["template.template_a.config.font.typeface_id"],
        )
        assertEquals("textview_sp", backup["template.template_a.config.font.hook_domains"])
        assertFalse(backup.containsKey("font.library.entries"))
        assertFalse(backup.containsKey("font.library.migration_state"))
        assertFalse(backup.containsKey("font.debug.overlay_enabled"))
        assertFalse(backup.containsKey("runtime.log.ring"))
    }

    @Test
    fun replaceBackupIgnoresIncomingExcludedStateButPreservesLocalExcludedState() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("font.library.entries", "[{\"id\":\"local_font\"}]")
            .putString("font.library.migration_state", "local_done")
            .putBoolean("font.debug.overlay_enabled", true)
            .putString("runtime.log.ring", "local debug log")
            .putString("runtime.log.token", "local token")
            .commit()
        val store = DpisConfigStore(prefs)
        val values = linkedMapOf<String, Any?>()
        values[DpisConfigStore.KEY_TARGET_PACKAGES] = setOf("com.max.xiaoheihe")
        values["font.com.max.xiaoheihe.typeface_id"] = "font_abcd1234"
        values["font.library.entries"] = "[{\"id\":\"incoming_font\"}]"
        values["font.library.migration_state"] = "incoming_done"
        values["font.debug.overlay_enabled"] = false
        values["runtime.log.ring"] = "incoming debug log"
        values["runtime.log.token"] = "incoming token"

        assertTrue(store.replaceBackup(values))

        assertEquals("font_abcd1234", store.getTargetTypefaceId("com.max.xiaoheihe"))
        assertEquals("[{\"id\":\"local_font\"}]", prefs.getString("font.library.entries", null))
        assertEquals("local_done", prefs.getString("font.library.migration_state", null))
        assertTrue(prefs.getBoolean("font.debug.overlay_enabled", false))
        assertEquals("local debug log", prefs.getString("runtime.log.ring", null))
        assertEquals("local token", prefs.getString("runtime.log.token", null))
    }

    @Test
    fun replaceBackupRemovesStaleBackupManagedKeysWhilePreservingExcludedKeys() {
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, setOf("com.old.app"))
            .putInt("viewport.com.old.app.width_dp", 360)
            .putString("font.com.old.app.typeface_id", "font_old")
            .putString("default_config.font.typeface_id", "font_old_default")
            .putStringSet(QuickTemplateStore.KEY_TEMPLATE_IDS, setOf("old_template"))
            .putString("template.old_template.name", "Old")
            .putString("template.old_template.config.font.typeface_id", "font_old_template")
            .putString("font.library.entries", "[{\"id\":\"font_local\"}]")
            .putString("font.library.migration_state", "local_done")
            .putBoolean("font.debug.overlay_enabled", true)
            .putString("runtime.log.ring", "local debug log")
            .commit()
        val store = DpisConfigStore(prefs)
        val values = linkedMapOf<String, Any?>()
        values[DpisConfigStore.KEY_TARGET_PACKAGES] = setOf("com.new.app")
        values["font.com.new.app.typeface_id"] = "font_new"
        values["default_config.font.typeface_id"] = "font_new_default"
        values["font.library.entries"] = "[{\"id\":\"font_incoming\"}]"

        assertTrue(store.replaceBackup(values))

        assertFalse(store.getConfiguredPackages().contains("com.old.app"))
        assertTrue(store.getConfiguredPackages().contains("com.new.app"))
        assertNull(store.getTargetViewportWidthDp("com.old.app"))
        assertNull(store.getTargetTypefaceId("com.old.app"))
        assertEquals("font_new", store.getTargetTypefaceId("com.new.app"))
        assertEquals("font_new_default", GlobalPrefillStore(prefs).read().typefaceId)
        assertFalse(prefs.contains(QuickTemplateStore.KEY_TEMPLATE_IDS))
        assertFalse(prefs.contains("template.old_template.name"))
        assertFalse(prefs.contains("template.old_template.config.font.typeface_id"))
        assertEquals("[{\"id\":\"font_local\"}]", prefs.getString("font.library.entries", null))
        assertEquals("local_done", prefs.getString("font.library.migration_state", null))
        assertTrue(prefs.getBoolean("font.debug.overlay_enabled", false))
        assertEquals("local debug log", prefs.getString("runtime.log.ring", null))
    }

    @Test
    fun replaceBackupMigratesLegacyPackageConfigAndDeletesLegacyKeys() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        val values = linkedMapOf<String, Any?>()
        values[DpisConfigStore.KEY_TARGET_PACKAGES] = setOf("com.tencent.mm")
        values["viewport.com.tencent.mm.target_type"] = ViewportTargetType.RELATIVE_SCALE
        values["viewport.com.tencent.mm.scale_permille"] = 1250
        values["font.com.tencent.mm.hook_domains"] = " resources_font,textview_sp "
        values["target.com.tencent.mm.dpis_enabled"] = false
        values["wechat.com.tencent.mm.dpi"] = 600

        assertTrue(store.replaceBackup(values))

        assertEquals(
            ViewportTargetSpec.relativeScale(125000),
            store.readPackageConfig("com.tencent.mm").viewportTargetSpec(),
        )
        assertEquals(
            "resources_font,textview_sp",
            store.readPackageConfig("com.tencent.mm").fontHookDomainsRaw(),
        )
        assertFalse(store.isTargetDpisEnabled("com.tencent.mm"))
        assertEquals(600, store.getWechatDpi("com.tencent.mm"))
        assertEquals(
            ViewportTargetType.RELATIVE_SCALE,
            prefs.getString("package_config.com.tencent.mm.viewport.target_type", null),
        )
        assertEquals(
            "resources_font,textview_sp",
            prefs.getString("package_config.com.tencent.mm.font.hook_domains", null),
        )
        assertFalse(prefs.contains("viewport.com.tencent.mm.target_type"))
        assertFalse(prefs.contains("viewport.com.tencent.mm.scale_permille"))
        assertFalse(prefs.contains("font.com.tencent.mm.hook_domains"))
        assertFalse(prefs.contains("target.com.tencent.mm.dpis_enabled"))
        assertFalse(prefs.contains("wechat.com.tencent.mm.dpi"))
    }

    @Test
    fun replaceBackupKeepsAggregatedPackageConfigWhenLegacyBackupConflicts() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        val values = linkedMapOf<String, Any?>()
        values["viewport.com.example.app.target_type"] = ViewportTargetType.ABSOLUTE_DP
        values["viewport.com.example.app.width_dp"] = 411
        values["package_config.com.example.app.viewport.target_type"] = ViewportTargetType.RELATIVE_SCALE
        values["package_config.com.example.app.viewport.scale_permille"] = 900

        assertTrue(store.replaceBackup(values))

        assertEquals(
            ViewportTargetSpec.relativeScale(90000),
            store.readPackageConfig("com.example.app").viewportTargetSpec(),
        )
        assertFalse(prefs.contains("viewport.com.example.app.target_type"))
        assertFalse(prefs.contains("viewport.com.example.app.width_dp"))
        assertEquals(
            ViewportTargetType.RELATIVE_SCALE,
            prefs.getString("package_config.com.example.app.viewport.target_type", null),
        )
        assertEquals(900, prefs.getInt("package_config.com.example.app.viewport.scale_permille", 0))
    }

    @Test
    fun replaceBackupRestoresPrefillAndTemplateKeysWithMissingTypefaceIds() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        val values = linkedMapOf<String, Any?>()
        values[DpisConfigStore.KEY_TARGET_PACKAGES] = setOf("com.max.xiaoheihe")
        values["font.com.max.xiaoheihe.typeface_id"] = "font_abcd1234"
        values["default_config.font.typeface_id"] = "missing_font_id"
        values["default_config.viewport.width_dp"] = 411
        values["default_config.viewport.target_type"] = ViewportTargetType.ABSOLUTE_DP
        values["default_config.viewport.mode"] = ViewportApplyMode.AUTO
        values["default_config.font.scale_percent"] = 120
        values["default_config.font.mode"] = FontApplyMode.FIELD_REWRITE
        values["default_config.font.hook_domains"] = "resources_font"
        values[QuickTemplateStore.KEY_TEMPLATE_IDS] = setOf("template_a")
        values["template.template_a.name"] = "Compact"
        values["template.template_a.updated_at"] = 1000L
        values["template.template_a.selected_packages"] = setOf("com.example.app")
        values["template.template_a.config.viewport.target_type"] = ViewportTargetType.RELATIVE_SCALE
        values["template.template_a.config.viewport.scale_permille"] = 1100
        values["template.template_a.config.viewport.mode"] = ViewportApplyMode.COMPAT
        values["template.template_a.config.font.scale_percent"] = 115
        values["template.template_a.config.font.mode"] = FontApplyMode.SYSTEM_EMULATION
        values["template.template_a.config.font.typeface_id"] = "missing_template_font_id"
        values["template.template_a.config.font.hook_domains"] = "textview_sp"
        values["font.library.entries"] = "[{\"id\":\"missing_font_id\"},{\"id\":\"missing_template_font_id\"}]"
        values["font.library.migration_state"] = "done"

        assertTrue(store.replaceBackup(values))

        assertEquals("font_abcd1234", store.getTargetTypefaceId("com.max.xiaoheihe"))
        val prefill = GlobalPrefillStore(prefs).read()
        assertEquals(
            ViewportTargetSpec.absoluteDp(411),
            TemplateConfigValueAdapters.toViewportTargetSpec(prefill),
        )
        assertEquals(ViewportApplyMode.AUTO, prefill.viewportApplyMode)
        assertEquals(120, prefill.fontScalePercent)
        assertEquals(FontApplyMode.FIELD_REWRITE, prefill.fontApplyMode)
        assertEquals("missing_font_id", prefill.typefaceId)
        assertEquals("resources_font", prefill.fontHookDomainsRaw)

        val template = QuickTemplateStore(prefs).read("template_a")
        assertNotNull(template)
        assertEquals("Compact", template!!.name)
        assertEquals(1000L, template.updatedAt)
        assertEquals(setOf("com.example.app"), template.selectedPackages)
        assertEquals(
            ViewportTargetSpec.relativeScale(110000),
            TemplateConfigValueAdapters.toViewportTargetSpec(template.configValue),
        )
        assertEquals(ViewportApplyMode.COMPAT, template.configValue.viewportApplyMode)
        assertEquals(115, template.configValue.fontScalePercent)
        assertEquals(FontApplyMode.SYSTEM_EMULATION, template.configValue.fontApplyMode)
        assertEquals("missing_template_font_id", template.configValue.typefaceId)
        assertEquals("textview_sp", template.configValue.fontHookDomainsRaw)
        assertFalse(prefs.contains("font.library.entries"))
        assertFalse(prefs.contains("font.library.migration_state"))
    }

    @Test
    fun replaceAllOverwritesCurrentStoreValues() {
        val prefs = FakePrefs()
        prefs.edit()
            .putBoolean(DpisConfigStore.KEY_GLOBAL_LOG_ENABLED, false)
            .putInt("font.com.old.scale_percent", 120)
            .commit()
        val store = DpisConfigStore(prefs)
        val values = linkedMapOf<String?, Any?>()
        values[DpisConfigStore.KEY_GLOBAL_LOG_ENABLED] = true
        values["viewport.com.max.xiaoheihe.width_dp"] = 360
        values["font.com.max.xiaoheihe.scale_percent"] = 120
        values[DpisConfigStore.KEY_TARGET_PACKAGES] = setOf("com.max.xiaoheihe")

        assertTrue(store.replaceAll(values))

        assertTrue(store.isGlobalLogEnabled())
        assertNull(store.getTargetFontScalePercent("com.old"))
        assertEquals(360, store.getTargetViewportWidthDp("com.max.xiaoheihe"))
        assertEquals(120, store.getTargetFontScalePercent("com.max.xiaoheihe"))
        assertTrue(store.getConfiguredPackages().contains("com.max.xiaoheihe"))
    }
}
