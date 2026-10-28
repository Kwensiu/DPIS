package com.dpis.module.config

import com.dpis.module.FakePrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageIdentityMigrationTest {
    @Test
    fun migratesEachValidFieldToTheArchiveOwnerAndKeepsPerFieldConflicts() {
        val prefs = FakePrefs()
        prefs.edit()
            .putInt("font.com.example.app.scale_percent", 140)
            .putString("font.com.example.app.typeface_id", "legacy-face")
            .putString(
                "package_config.com.example.app.font.typeface_id",
                "archive-face",
            )
            .putString(
                "package_config.com.example.app.viewport.target_type",
                "relative_scale",
            )
            .putInt("package_config.com.example.app.viewport.scale_milli_percent", 150000)
            .commit()
        val store = DpisConfigStore(prefs, 999)

        assertTrue(store.migrateLegacyPackageConfigToAggregated())
        assertTrue(store.migratePackageConfigToUserScoped())
        val migrated = store.readPackageConfig(PackageIdentity("com.example.app", 999))

        assertEquals(140, migrated.fontScalePercent())
        assertEquals("archive-face", migrated.typefaceId())
        assertEquals(
            140,
            prefs.getInt(
                PackageIdentity("com.example.app", 999).packageConfigKey("font.scale_percent"),
                0,
            ),
        )
        assertEquals(
            "archive-face",
            prefs.getString(
                PackageIdentity("com.example.app", 999).packageConfigKey("font.typeface_id"),
                null,
            ),
        )
        assertEquals(
            "relative_scale",
            prefs.getString(
                PackageIdentity("com.example.app", 999)
                    .packageConfigKey("viewport.target_type"),
                null,
            ),
        )
        assertEquals(
            150000,
            prefs.getInt(
                PackageIdentity("com.example.app", 999)
                    .packageConfigKey("viewport.scale_milli_percent"),
                0,
            ),
        )
        assertFalse(
            prefs.contains(
                PackageIdentity("com.example.app", 0).packageConfigKey("font.scale_percent"),
            ),
        )
        assertEquals(999, prefs.getInt(PACKAGE_IDENTITY_MIGRATION_OWNER_USER_ID, -1))
    }

    @Test
    fun invalidAndUnknownPackageFieldsStayLocalAndAreExcludedFromRuntimeAndBackup() {
        val prefs = FakePrefs()
        prefs.edit()
            .putInt("viewport.com.example.app.scale_milli_percent", 999999)
            .putInt("package_config.com.example.app.font.scale_percent", 9999)
            .putString("font.com.example.app.unknown_route", "custom")
            .putInt("viewport.invalid!.app.width_dp", 411)
            .commit()
        val store = DpisConfigStore(prefs, 0)

        assertTrue(store.migrateLegacyPackageConfigToAggregated())
        assertTrue(store.migratePackageConfigToUserScoped())

        assertTrue(prefs.contains("viewport.com.example.app.scale_milli_percent"))
        assertTrue(prefs.contains("package_config.com.example.app.font.scale_percent"))
        assertTrue(prefs.contains("font.com.example.app.unknown_route"))
        assertTrue(prefs.contains("viewport.invalid!.app.width_dp"))
        assertNull(store.getTargetViewportScaleMilliPercent("com.example.app"))
        assertFalse(
            store.snapshotRuntimeDelivery()
                .containsKey("viewport.com.example.app.scale_milli_percent")
        )
        assertFalse(
            store.snapshotRuntimeDelivery().containsKey("font.com.example.app.unknown_route")
        )
        assertFalse(
            store.snapshotRuntimeDelivery()
                .containsKey("package_config.com.example.app.font.scale_percent")
        )
        assertFalse(store.snapshotBackup().keys.any { it.startsWith("package_identity.") })
        assertFalse(store.snapshotBackup().keys.any { it.startsWith("package_identity_residual.") })
    }

    @Test
    fun failedArchiveCommitLeavesSourceAndMarkerUntouchedAndRetryIsIdempotent() {
        val prefs = FakePrefs()
        prefs.edit()
            .putInt("font.com.example.app.scale_percent", 140)
            .commit()
        val store = DpisConfigStore(prefs, 999)
        assertTrue(store.migrateLegacyPackageConfigToAggregated())
        prefs.setCommitResult(false)

        assertFalse(store.migratePackageConfigToUserScoped())

        val scopedKey = PackageIdentity("com.example.app", 999)
            .packageConfigKey("font.scale_percent")
        assertFalse(prefs.contains(scopedKey))
        assertFalse(prefs.contains(PACKAGE_IDENTITY_MIGRATION_OWNER_USER_ID))
        assertTrue(prefs.contains("package_config.com.example.app.font.scale_percent"))

        prefs.setCommitResult(true)
        assertTrue(store.migratePackageConfigToUserScoped())
        assertEquals(140, prefs.getInt(scopedKey, 0))
        assertEquals(999, prefs.getInt(PACKAGE_IDENTITY_MIGRATION_OWNER_USER_ID, -1))
        assertTrue(store.migratePackageConfigToUserScoped())
        assertEquals(140, prefs.getInt(scopedKey, 0))
    }

    @Test
    fun globalFontDebugSettingIsNotInterpretedAsPackageConfigDuringMigration() {
        val prefs = FakePrefs()
        prefs.edit()
            .putInt("font.debug.selected_mode", 2)
            .putInt("font.com.example.app.scale_percent", 140)
            .commit()
        val store = DpisConfigStore(prefs, 0)

        assertTrue(store.migrateLegacyPackageConfigToAggregated())
        assertTrue(store.migratePackageConfigToUserScoped())

        assertEquals(2, prefs.getInt("font.debug.selected_mode", 0))
        assertFalse(prefs.contains("package_config.debug.selected.font.mode"))
        assertFalse(
            prefs.contains(
                PackageIdentity("debug.selected", 0).packageConfigKey("font.mode"),
            ),
        )
        assertEquals(
            140,
            prefs.getInt(
                PackageIdentity("com.example.app", 0).packageConfigKey("font.scale_percent"),
                0,
            ),
        )
    }

    @Test
    fun sameOwnerRerunMigratesFieldsAddedAfterTheCompletionMarker() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs, 999)
        assertTrue(store.migrateLegacyPackageConfigToAggregated())
        assertTrue(store.migratePackageConfigToUserScoped())

        prefs.edit()
            .putInt("font.com.example.app.scale_percent", 140)
            .commit()
        assertTrue(store.migrateLegacyPackageConfigToAggregated())

        assertTrue(store.migratePackageConfigToUserScoped())

        val scopedKey = PackageIdentity("com.example.app", 999)
            .packageConfigKey("font.scale_percent")
        assertEquals(140, prefs.getInt(scopedKey, 0))
        assertTrue(store.migratePackageConfigToUserScoped())
        assertEquals(140, prefs.getInt(scopedKey, 0))
    }

    @Test
    fun runtimeProjectionKeepsOnlyEffectiveLegacyFieldWhenAggregateIsAbsent() {
        val prefs = FakePrefs()
        prefs.edit()
            .putInt("font.com.example.app.scale_percent", 140)
            .commit()
        val store = DpisConfigStore(prefs)

        assertTrue(
            store.snapshotRuntimeDelivery().containsKey("font.com.example.app.scale_percent")
        )

        assertTrue(store.setTargetFontScalePercent("com.example.app", 160))
        val snapshot = store.snapshotRuntimeDelivery()

        assertFalse(snapshot.containsKey("font.com.example.app.scale_percent"))
        assertEquals(160, snapshot["package_config.com.example.app.font.scale_percent"])
    }

    @Test
    fun migrationMarkerCannotBeReusedForAnotherArchiveOwner() {
        val prefs = FakePrefs()
        prefs.edit()
            .putInt("font.com.example.app.scale_percent", 140)
            .commit()
        val firstOwner = DpisConfigStore(prefs, 0)
        assertTrue(firstOwner.migrateLegacyPackageConfigToAggregated())
        assertTrue(firstOwner.migratePackageConfigToUserScoped())

        val otherOwner = DpisConfigStore(prefs, 999)

        assertFalse(otherOwner.migratePackageConfigToUserScoped())
        assertFalse(
            prefs.contains(
                PackageIdentity("com.example.app", 999).packageConfigKey("font.scale_percent"),
            ),
        )
    }
}
