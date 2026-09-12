package com.dpis.module.backup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

class ConfigBackupRestorePolicyTest {
    @Test
    fun unknownKeysAreRejected() {
        assertTrue(ConfigBackupRestorePolicy.hasUnknownKeys(mapOf("unknown.preference" to "x")))
        assertFalse(
            ConfigBackupRestorePolicy.hasUnknownKeys(
                mapOf("package_config.com.example.viewport.width_dp" to 360),
            ),
        )
    }

    @Test
    fun legacyResolutionKeysMoveOutOfPackageConfig() {
        val entries = linkedMapOf<String, Any?>(
            "package_config.com.example.resolution.width_px" to 1080,
            "package_config.com.example.viewport.width_dp" to 360,
        )
        ConfigBackupRestorePolicy.normalizeIncoming(entries)
        assertEquals(1080, entries["resolution.com.example.width_px"])
        assertEquals(360, entries["package_config.com.example.viewport.width_dp"])
        assertFalse(entries.containsKey("package_config.com.example.resolution.width_px"))
        assertFalse(entries.containsKey("target_packages"))
    }

    @Test
    fun targetPackagesKeepOnlyValidNamesWhenNoPackageConfigExists() {
        val entries = linkedMapOf<String, Any?>(
            "target_packages" to setOf("com.example.app", "bad", "com.ok.two"),
        )
        ConfigBackupRestorePolicy.normalizeIncoming(entries)
        assertEquals(linkedSetOf("com.example.app", "com.ok.two"), entries["target_packages"])
    }

    @Test
    fun configEntriesDropTemplateKeys() {
        val incoming = mapOf(
            "target_packages" to setOf("com.example.app"),
            "template.ids" to setOf("t1"),
        )
        val config = ConfigBackupRestorePolicy.configEntries(incoming)
        assertTrue(config.containsKey("target_packages"))
        assertFalse(config.containsKey("template.ids"))
    }

    @Test(expected = IOException::class)
    fun readLimitedRejectsOversizedPayloads() {
        val oversized = ByteArray(ConfigBackupRestorePolicy.MAX_BACKUP_BYTES + 1)
        ConfigBackupRestorePolicy.readLimited(ByteArrayInputStream(oversized))
    }

    @Test
    fun readLimitedKeepsSmallPayloads() {
        val body = "backup-body"
        assertEquals(
            body,
            ConfigBackupRestorePolicy.readLimited(ByteArrayInputStream(body.toByteArray())),
        )
    }
}
