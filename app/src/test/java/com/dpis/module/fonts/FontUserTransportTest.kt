package com.dpis.module.fonts

import com.dpis.module.config.ConfigSnapshotLoader
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.config.RuntimePropertyConfigPreferences
import com.dpis.module.fonts.hookdomain.FontHookDomainPropertyBridge
import com.dpis.module.fonts.hookdomain.FontHookDomainPropertyCommands
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.runtime.font.FontRuntimePropertySyncer
import org.junit.Assert.*
import org.junit.Test

/** User-owned font settings stay isolated across publish, read, disable, and recovery. */
class FontUserTransportTest {
    private val packageName = "com.android.chrome"
    private val properties = mutableMapOf<String, String>()

    @Test
    fun samePackageReadsEachUsersPublishedFontAndTypeface() {
        publish(0, 200, FontApplyMode.SYSTEM_EMULATION, false, "face_zero")
        publish(999, 60, FontApplyMode.SYSTEM_EMULATION, false, "face_clone")

        assertEquals(200, store(0).getTargetFontScalePercent(packageName))
        assertEquals(60, store(999).getTargetFontScalePercent(packageName))
        assertEquals("face_zero", store(0).getTargetTypefaceId(packageName))
        assertEquals("face_clone", store(999).getTargetTypefaceId(packageName))
        assertEquals(FontApplyMode.SYSTEM_EMULATION, store(999).getTargetFontApplyMode(packageName))
        val snapshot = ConfigSnapshotLoader.fromStore(store(999))
        assertEquals(60, snapshot.getPackage(packageName, 999)?.targetFontScalePercent)
        assertNull(snapshot.getPackage(packageName, 0))
    }

    @Test
    fun unownedOldPropertiesAndOtherUsersValuesNeverSeedMissingConfig() {
        publish(0, 200, FontApplyMode.FIELD_REWRITE, true, "face_zero")
        properties["debug.dpis.compatfont.0f493ae6"] = "300"
        properties["persist.debug.dpis.forcefont.0f493ae6"] = "300"
        properties["persist.debug.dpis.typeface.0f493ae6"] = "old_face"
        properties["persist.debug.dpis.hookdomains.0f493ae6"] = "1"

        val absentUser = store(999)
        assertNull(absentUser.getTargetFontScalePercent(packageName))
        assertNull(absentUser.getTargetTypefaceId(packageName))
        assertNull(absentUser.getPackageFontHookDomainsRaw(packageName))
        assertTrue(store(-1).snapshotAll().isEmpty())
    }

    @Test
    fun persistentRecoveryUsesOnlyRequestedUserAndExplicitClearWins() {
        publish(0, 200, FontApplyMode.SYSTEM_EMULATION, false, "face_zero")
        publish(999, 60, FontApplyMode.SYSTEM_EMULATION, false, "face_clone")
        properties.keys.filter { it.startsWith("debug.") }.toList().forEach(properties::remove)
        assertEquals(60, store(999).getTargetFontScalePercent(packageName))

        properties[FontPropertyKeys.name(packageName, 999, "compatfont")] = "0"
        properties[FontPropertyKeys.name(packageName, 999, "typeface")] = "0"
        assertNull(store(999).getTargetFontScalePercent(packageName))
        assertNull(store(999).getTargetTypefaceId(packageName))
        assertEquals(200, store(0).getTargetFontScalePercent(packageName))
    }

    @Test
    fun clearingOneUsersFontScalePreservesTypefaceAndOtherUsersConfig() {
        publish(0, 200, FontApplyMode.SYSTEM_EMULATION, true, "face_zero")
        publish(999, 60, FontApplyMode.SYSTEM_EMULATION, true, "face_clone")
        apply(FontRuntimePropertySyncer.buildClearFontScaleCommandForTest(packageName, 999))

        assertNull(store(999).getTargetFontScalePercent(packageName))
        assertEquals("face_clone", store(999).getTargetTypefaceId(packageName))
        assertEquals(200, store(0).getTargetFontScalePercent(packageName))
        assertEquals("face_zero", store(0).getTargetTypefaceId(packageName))
        apply(FontRuntimePropertySyncer.buildClearTargetCommandForTest(packageName, 999))
        assertNull(store(999).getTargetTypefaceId(packageName))
        assertEquals("face_zero", store(0).getTargetTypefaceId(packageName))
    }

    @Test
    fun compatAndNativeOverridesKeepTheirModeAndUserAcrossRecovery() {
        publish(0, 200, FontApplyMode.FIELD_REWRITE, false, null)
        publish(999, 60, FontApplyMode.SYSTEM_EMULATION, true, null)
        assertEquals(FontApplyMode.FIELD_REWRITE, store(0).getTargetFontApplyMode(packageName))
        assertEquals(200, store(0).getTargetFontScalePercent(packageName))
        assertEquals(FontApplyMode.SYSTEM_EMULATION, store(999).getTargetFontApplyMode(packageName))
        assertEquals(60, store(999).getTargetFontScalePercent(packageName))
        assertEquals("60", properties[FontPropertyKeys.name(packageName, 999, "forcefont")])
        publish(999, 60, FontApplyMode.OFF, true, null)
        assertNull(store(999).getTargetFontScalePercent(packageName))
        assertEquals(200, store(0).getTargetFontScalePercent(packageName))
    }

    @Test
    fun hookDomainOptOutAndCustomSelectionStayUserScoped() {
        val selected = setOf(FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM)
        apply(FontHookDomainPropertyCommands.buildPublish(packageName, selected, 0))
        apply(FontHookDomainPropertyCommands.buildPublish(packageName, emptySet(), 999))
        fun read(userId: Int) = FontHookDomainPropertyBridge.readOverride(packageName, userId) {
            properties[it].orEmpty()
        }
        assertEquals(selected, read(0).enabledKnownDomains)
        assertTrue(read(999).customPathEnabled)
        assertTrue(read(999).enabledKnownDomains.isEmpty())
        apply(FontHookDomainPropertyCommands.buildClear(packageName, 999))
        assertFalse(read(999).customPathEnabled)
        assertEquals(selected, read(0).enabledKnownDomains)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidUserCannotGenerateAFontTransportKey() {
        FontPropertyKeys.name(packageName, -1, "compatfont")
    }

    private fun publish(userId: Int, percent: Int, mode: String, native: Boolean, face: String?) {
        apply(
            FontRuntimePropertySyncer.buildTargetCommandForTest(
                packageName,
                percent,
                mode,
                native,
                userId
            )
        )
        apply(FontRuntimePropertySyncer.buildTypefaceCommandForTest(packageName, face, userId))
    }

    private fun store(userId: Int): DpisConfigStore = DpisConfigStore(
        RuntimePropertyConfigPreferences(packageName, userId = userId, propertyReader = {
            properties[it].orEmpty()
        }),
        userId.coerceAtLeast(0),
    )

    private fun apply(command: String) {
        // Parse the actual wire command; no shell, Root, or Android property stub is involved.
        Regex("setprop '([^']+)' '([^']*)'").findAll(command).forEach { match ->
            properties[match.groupValues[1]] = match.groupValues[2]
        }
    }
}
