package com.dpis.module.backup

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigBackupCodecTest {
    @Test
    fun roundTrip_preservesSupportedPortableValues() {
        val entries = linkedMapOf<String, Any?>(
            "package_config.com.example.viewport.width_dp" to 360,
            "package_config.com.example.font.typeface_id" to "font_example",
            "default_config.font.scale_percent" to 125,
            "template.compact.name" to "Compact",
            "template.compact.selected_packages" to linkedSetOf("com.example", "com.other"),
            "ui.interface_scale_percent" to 110,
        )

        assertEquals(entries, ConfigBackupCodec.decode(ConfigBackupCodec.encode(entries)))
    }

    @Test
    fun encode_rejectsUnsupportedValueForPortableKey() {
        val error = assertThrows(JSONException::class.java) {
            ConfigBackupCodec.encode(mapOf("ui.interface_scale_percent" to Double.NaN))
        }

        assertTrue(error.message.orEmpty().contains("Unsupported backup value type"))
    }

    @Test
    fun encode_excludesUnknownKeysButKeepsKnownPortableKeys() {
        val decoded = ConfigBackupCodec.decode(
            ConfigBackupCodec.encode(
                mapOf(
                    "ui.interface_scale_percent" to 110,
                    "ui.temporary_preview" to true,
                ),
            ),
        )

        assertEquals(110, decoded["ui.interface_scale_percent"])
        assertFalse(decoded.containsKey("ui.temporary_preview"))
    }

    @Test
    fun backupPolicy_rejectsUnknownFieldsInsideKnownDomains() {
        assertTrue(BackupKeyPolicy.isImportable("package_config.com.example.viewport.width_dp"))
        assertTrue(BackupKeyPolicy.isImportable("wechat.com.example.dpi"))
        assertTrue(BackupKeyPolicy.isImportable("template.compact.config.font.typeface_id"))
        assertFalse(BackupKeyPolicy.isImportable("ui.temporary_preview"))
        assertFalse(BackupKeyPolicy.isImportable("global.debug_override"))
        assertFalse(BackupKeyPolicy.isImportable("template.compact.temporary_state"))
    }
}
