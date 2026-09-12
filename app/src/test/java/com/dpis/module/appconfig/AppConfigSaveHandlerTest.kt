package com.dpis.module

import com.dpis.module.appconfig.presentation.AppConfigDialogBinder.AppConfigDialogState
import com.dpis.module.appconfig.AppConfigPrefillPreview
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.templates.TemplateConfigValueAdapters
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigSaveHandlerTest {
    @Test
    fun saveUsesCurrentViewportModeOverPersistedAuto() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(90000))
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.AUTO)

        val resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
            store,
            "com.example.app",
            ViewportApplyMode.SYSTEM,
            false,
            ViewportTargetSpec.relativeScale(90000)
        )

        assertEquals(ViewportApplyMode.SYSTEM, resolvedMode)
    }

    @Test
    fun saveFallsBackToPersistedViewportModeWhenCurrentModeIsMissing() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(90000))
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.COMPAT)

        val resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
            store,
            "com.example.app",
            ViewportApplyMode.OFF,
            false,
            ViewportTargetSpec.relativeScale(90000)
        )

        assertEquals(ViewportApplyMode.COMPAT, resolvedMode)
    }

    @Test
    fun saveFallsBackToListItemViewportModeForFirstEnabledSave() {
        val store = DpisConfigStore(FakePrefs())

        val resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
            store,
            "com.example.app",
            ViewportApplyMode.SYSTEM,
            false,
            ViewportTargetSpec.relativeScale(90000)
        )

        assertEquals(ViewportApplyMode.SYSTEM, resolvedMode)
    }

    @Test
    fun saveDefaultsFirstEnabledViewportModeToAutoWithoutListItemMode() {
        val store = DpisConfigStore(FakePrefs())

        val resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
            store,
            "com.example.app",
            ViewportApplyMode.OFF,
            false,
            ViewportTargetSpec.relativeScale(90000)
        )

        assertEquals(ViewportApplyMode.AUTO, resolvedMode)
    }

    @Test
    fun saveDefaultsInvalidViewportModeToAutoInsteadOfDroppingEnabledTarget() {
        val store = DpisConfigStore(FakePrefs())

        val resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
            store,
            "com.example.app",
            "unknown-mode",
            false,
            ViewportTargetSpec.relativeScale(90000)
        )

        assertEquals(ViewportApplyMode.AUTO, resolvedMode)
    }

    @Test
    fun saveUsesPreviewViewportApplyModeFallbackBeforeRealConfigExists() {
        val store = DpisConfigStore(FakePrefs())

        val resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
            store,
            "com.example.app",
            ViewportApplyMode.COMPAT,
            false,
            ViewportTargetSpec.relativeScale(90000)
        )

        assertEquals(ViewportApplyMode.COMPAT, resolvedMode)
    }

    @Test
    fun savingPreviewOnlyConfigConvertsHiddenPrefillDomainsToRealPackageConfig() {
        val store = DpisConfigStore(FakePrefs())
        val item: AppListItem = app("com.example.app").withGlobalPrefillPreview(
            TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                null,
                "resources_font"
            )
        )

        assertTrue(
            AppConfigSaveHandler.persistPreviewOnlyConfig(
                store, item, "resources_font", false
            )
        )

        assertTrue(store.hasRealPackageConfig(item.packageName))
        assertTrue(store.getConfiguredPackages().contains(item.packageName))
        assertEquals(
            "resources_font",
            store.readPackageTemplateConfigValue(item.packageName).fontHookDomainsRaw
        )
    }

    @Test
    fun savingNonPreviewItemDoesNotCreateHiddenPrefillConfig() {
        val store = DpisConfigStore(FakePrefs())
        val item: AppListItem = app("com.example.app")

        assertTrue(AppConfigSaveHandler.persistPreviewOnlyConfig(store, item, null, false))

        assertFalse(store.hasRealPackageConfig(item.packageName))
    }

    @Test
    fun hookDomainOnlyPreviewSavePersistsRealPackageConfig() {
        val store = DpisConfigStore(FakePrefs())
        val item: AppListItem = app("com.example.app").withGlobalPrefillPreview(
            TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                null,
                "resources_font"
            )
        )

        assertTrue(
            AppConfigSaveHandler.persistPreviewOnlyConfig(
                store, item, item.previewFontHookDomainsRaw, false
            )
        )

        assertEquals("resources_font", store.getPackageFontHookDomainsRaw(item.packageName))
        assertTrue(store.getConfiguredPackages().contains(item.packageName))
    }

    @Test
    fun clearedHookDomainPreviewDoesNotForcePackageConfig() {
        val store = DpisConfigStore(FakePrefs())
        val item: AppListItem = app("com.example.app").withGlobalPrefillPreview(
            TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                null,
                "resources_font"
            )
        )

        assertTrue(AppConfigSaveHandler.persistPreviewOnlyConfig(store, item, null, false))

        assertFalse(store.hasRealPackageConfig(item.packageName))
        assertFalse(store.getConfiguredPackages().contains(item.packageName))
    }

    @Test
    fun resetClearsPreviewOnlyHookDomainsAndViewportApplyMode() {
        val state =
            AppConfigDialogState(
                false,
                true,
                true,
                true,
                "com.example.app",
                "resources_font",
                ViewportApplyMode.COMPAT,
                null,
                ViewportTargetType.RELATIVE_SCALE,
                "",
                "",
                ""
            )

        state.clearHookChainStateForReset()

        assertNull(state.draftFontHookDomainsRaw)
        assertEquals(ViewportApplyMode.OFF, state.viewportApplyMode)
        assertTrue(state.fontHookDomainsResetRequested)
        assertTrue(state.viewportApplyModeResetRequested)
    }

    @Test
    fun resetThenSaveDoesNotPersistHiddenPreviewHookDomains() {
        val store = DpisConfigStore(FakePrefs())
        val item: AppListItem = app("com.example.app").withGlobalPrefillPreview(
            TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.off(),
                ViewportApplyMode.COMPAT,
                null,
                FontApplyMode.OFF,
                null,
                "resources_font"
            )
        )
        val state =
            AppConfigDialogState(
                false,
                true,
                true,
                true,
                item.packageName,
                item.previewFontHookDomainsRaw,
                item.viewportMode,
                null,
                ViewportTargetType.RELATIVE_SCALE,
                "",
                "",
                ""
            )

        state.clearHookChainStateForReset()

        assertTrue(
            AppConfigSaveHandler.persistPreviewOnlyConfig(
                store, item, state.draftFontHookDomainsRaw,
                state.fontHookDomainsResetRequested
            )
        )
        assertFalse(store.hasRealPackageConfig(item.packageName))
        assertFalse(store.getConfiguredPackages().contains(item.packageName))
    }

    @Test
    fun hookDomainResetClearsStoredCustomDomainsOnSave() {
        val store = DpisConfigStore(FakePrefs())
        val item: AppListItem = app("com.example.app")
        assertTrue(store.setPackageFontHookDomainsRaw(item.packageName, "resources_font"))

        assertTrue(
            AppConfigSaveHandler.persistPreviewOnlyConfig(
                store, item, null, true
            )
        )

        assertNull(store.getPackageFontHookDomainsRaw(item.packageName))
    }

    @Test
    fun hookDomainSaveClearsRawWhenDraftMatchesRecommendedDomains() {
        val store = DpisConfigStore(FakePrefs())
        val item: AppListItem = app("com.example.app")
        val recommendedRaw = HookDomainOverrideStore.formatCsv(
            FontHookDomainRegistry.automaticCustomizableDomains(),
            emptySet(),
        )
        assertTrue(store.setPackageFontHookDomainsRaw(item.packageName, "resources_font"))

        assertTrue(
            AppConfigSaveHandler.persistPreviewOnlyConfig(
                store, item, recommendedRaw, false
            )
        )

        assertNull(store.getPackageFontHookDomainsRaw(item.packageName))
    }

    @Test
    fun viewportApplyModeResetOverridesPersistedModeOnSave() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.COMPAT)

        val resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
            store,
            "com.example.app",
            ViewportApplyMode.COMPAT,
            true,
            ViewportTargetSpec.relativeScale(90000)
        )

        assertEquals(ViewportApplyMode.OFF, resolvedMode)
    }

    @Test
    fun savePreservesFontModeWhenFontScaleIsEmpty() {
        val store = DpisConfigStore(FakePrefs())
        val item: AppListItem = app("com.example.app")

        val result = AppConfigSaveHandler().saveResolved(
            item,
            ViewportTargetSpec.off(),
            ViewportTargetType.RELATIVE_SCALE,
            ViewportApplyMode.OFF,
            false,
            null,
            FontApplyMode.FIELD_REWRITE,
            null,
            null,
            false,
            "",
            "",
            true,
            store,
            null
        )

        assertTrue(result.success)
        assertNull(store.getTargetFontScalePercent(item.packageName))
        assertEquals(
            FontApplyMode.FIELD_REWRITE,
            store.getTargetFontApplyMode(item.packageName)
        )
        assertTrue(store.hasRealPackageConfig(item.packageName))
    }

    @Test
    fun savePreservesViewportTargetTypeWhenViewportInputIsEmpty() {
        val store = DpisConfigStore(FakePrefs())
        val item: AppListItem = app("com.example.app")

        val result = AppConfigSaveHandler().saveResolved(
            item,
            ViewportTargetSpec.off(),
            ViewportTargetType.ABSOLUTE_DP,
            ViewportApplyMode.COMPAT,
            false,
            null,
            FontApplyMode.SYSTEM_EMULATION,
            null,
            null,
            false,
            "",
            "",
            true,
            store,
            null
        )

        assertTrue(result.success)
        assertFalse(store.getTargetViewportSpec(item.packageName).isEnabled())
        assertEquals(
            ViewportTargetType.OFF,
            store.getTargetViewportType(item.packageName)
        )
        assertEquals(
            ViewportApplyMode.OFF,
            store.getTargetViewportApplyMode(item.packageName)
        )
        assertFalse(store.hasRealPackageConfig(item.packageName))
    }

    @Test
    fun saveClearsDefaultSystemFontModeWhenFontScaleIsEmpty() {
        val store = DpisConfigStore(FakePrefs())
        val item: AppListItem = app("com.example.app")

        val result = AppConfigSaveHandler().saveResolved(
            item,
            ViewportTargetSpec.off(),
            ViewportTargetType.RELATIVE_SCALE,
            ViewportApplyMode.OFF,
            false,
            null,
            FontApplyMode.SYSTEM_EMULATION,
            null,
            null,
            false,
            "",
            "",
            true,
            store,
            null
        )

        assertTrue(result.success)
        assertEquals(
            FontApplyMode.OFF,
            store.getTargetFontApplyMode(item.packageName)
        )
        assertFalse(store.hasRealPackageConfig(item.packageName))
        assertFalse(store.getConfiguredPackages().contains(item.packageName))
    }

    @Test
    fun savePrunesFullyDefaultPackageConfigAfterReset() {
        val store = DpisConfigStore(FakePrefs())
        val item: AppListItem = app("com.example.app")
        assertTrue(
            store.setTargetViewportTypeDraft(
                item.packageName, ViewportTargetType.RELATIVE_SCALE
            )
        )
        assertTrue(
            store.setTargetFontApplyMode(
                item.packageName, FontApplyMode.SYSTEM_EMULATION
            )
        )

        val result = AppConfigSaveHandler().saveResolved(
            item,
            ViewportTargetSpec.off(),
            ViewportTargetType.RELATIVE_SCALE,
            ViewportApplyMode.OFF,
            true,
            null,
            FontApplyMode.SYSTEM_EMULATION,
            null,
            null,
            true,
            "",
            "",
            true,
            store,
            null
        )

        assertTrue(result.success)
        assertFalse(store.hasRealPackageConfig(item.packageName))
        assertFalse(store.hasUserVisiblePackageConfig(item.packageName))
        assertFalse(store.getConfiguredPackages().contains(item.packageName))
    }

    @Test
    fun resetRemovesStaleAggregatedDefaultViewportType() {
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(
                DpisConfigStore.KEY_TARGET_PACKAGES,
                setOf("com.example.app")
            )
            .putString(
                "package_config.com.example.app.viewport.target_type",
                ViewportTargetType.RELATIVE_SCALE
            )
            .commit()
        val store = DpisConfigStore(prefs)
        val item: AppListItem = app("com.example.app")

        val result = AppConfigSaveHandler().saveResolved(
            item,
            ViewportTargetSpec.off(),
            ViewportTargetType.RELATIVE_SCALE,
            ViewportApplyMode.OFF,
            true,
            null,
            FontApplyMode.SYSTEM_EMULATION,
            null,
            null,
            true,
            "",
            "",
            true,
            store,
            null
        )

        assertTrue(result.success)
        assertFalse(store.hasUserVisiblePackageConfig(item.packageName))
        assertFalse(store.getConfiguredPackages().contains(item.packageName))
        assertFalse(
            prefs.contains(
                "package_config.com.example.app.viewport.target_type"
            )
        )
    }

    @Test
    fun saveAfterDisablingPackageUsesPersistedDisabledState() {
        val store = DpisConfigStore(FakePrefs())
        val item: AppListItem = app("com.example.app")
        assertTrue(store.setTargetDpisEnabled(item.packageName, false))

        val result = AppConfigSaveHandler().saveResolved(
            item,
            ViewportTargetSpec.off(),
            ViewportTargetType.RELATIVE_SCALE,
            ViewportApplyMode.OFF,
            false,
            null,
            FontApplyMode.SYSTEM_EMULATION,
            null,
            null,
            false,
            "",
            "",
            true,
            store,
            null
        )

        assertTrue(result.success)
        assertFalse(store.isTargetDpisEnabled(item.packageName))
        assertTrue(store.hasRealPackageConfig(item.packageName))
        assertFalse(store.hasUserVisiblePackageConfig(item.packageName))
    }

    @Test
    fun savingUnchangedPrefillCreatesRealPackageConfig() {
        val store = DpisConfigStore(FakePrefs())
        val item: AppListItem = app("com.example.app").withGlobalPrefillPreview(
            TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(87500),
                ViewportApplyMode.AUTO,
                125,
                FontApplyMode.FIELD_REWRITE,
                "serif",
                "resources_font"
            )
        )

        val result = AppConfigSaveHandler().saveResolved(
            item,
            ViewportTargetSpec.relativeScale(87500),
            ViewportTargetType.RELATIVE_SCALE,
            ViewportApplyMode.AUTO,
            false,
            125,
            FontApplyMode.FIELD_REWRITE,
            "serif",
            "resources_font",
            false,
            "87",
            "",
            true,
            store,
            null
        )

        assertTrue(result.success)
        assertTrue(store.hasRealPackageConfig(item.packageName))
        assertTrue(store.getConfiguredPackages().contains(item.packageName))
        assertEquals(
            ViewportTargetSpec.relativeScale(87500),
            store.getTargetViewportSpec(item.packageName)
        )
        assertEquals(125, store.getTargetFontScalePercent(item.packageName))
    }

    @Test
    fun resetThenSaveFromPrefillCreatesRealPackageConfig() {
        val store = DpisConfigStore(FakePrefs())
        val item: AppListItem = app("com.example.app").withGlobalPrefillPreview(
            TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(87500),
                ViewportApplyMode.AUTO,
                125,
                FontApplyMode.FIELD_REWRITE,
                "serif",
                "resources_font"
            )
        )

        val result = AppConfigSaveHandler().saveResolved(
            item,
            ViewportTargetSpec.off(),
            ViewportTargetType.RELATIVE_SCALE,
            ViewportApplyMode.OFF,
            true,
            null,
            FontApplyMode.SYSTEM_EMULATION,
            null,
            null,
            true,
            "",
            "",
            true,
            store,
            null
        )

        assertTrue(result.success)
        assertTrue(store.getConfiguredPackages().contains(item.packageName))
        assertFalse(
            AppConfigPrefillPreview.applyIfEligible(
                app("com.example.app"),
                store,
                TemplateConfigValueAdapters.fromViewportTargetSpec(
                    ViewportTargetSpec.relativeScale(87500),
                    ViewportApplyMode.AUTO,
                    125,
                    FontApplyMode.FIELD_REWRITE,
                    "serif",
                    "resources_font"
                )
            )!!.previewFromGlobalPrefill
        )
    }

    @Test
    fun changedGlobalPrefillPreviewSaveCreatesPackageConfig() {
        val store = DpisConfigStore(FakePrefs())
        val item: AppListItem = app("com.example.app").withGlobalPrefillPreview(
            TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(87500),
                ViewportApplyMode.AUTO,
                125,
                FontApplyMode.FIELD_REWRITE,
                "serif",
                "resources_font"
            )
        )

        val result = AppConfigSaveHandler().saveResolved(
            item,
            ViewportTargetSpec.relativeScale(90000),
            ViewportTargetType.RELATIVE_SCALE,
            ViewportApplyMode.AUTO,
            false,
            125,
            FontApplyMode.FIELD_REWRITE,
            "serif",
            "resources_font",
            false,
            "90",
            "",
            true,
            store,
            null
        )

        assertTrue(result.success)
        assertTrue(store.hasRealPackageConfig(item.packageName))
        assertEquals(
            ViewportTargetSpec.relativeScale(90000),
            store.getTargetViewportSpec(item.packageName)
        )
    }

    @Test
    fun saveReportsFailureWhenStoreCommitFails() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        prefs.setCommitResult(false)
        val changed = booleanArrayOf(false)

        val result = AppConfigSaveHandler().saveResolved(
            app("com.example.app"),
            ViewportTargetSpec.relativeScale(90000),
            ViewportTargetType.RELATIVE_SCALE,
            ViewportApplyMode.AUTO,
            false,
            125,
            FontApplyMode.FIELD_REWRITE,
            null,
            null,
            false,
            "90",
            "",
            true,
            store,
            Runnable { changed[0] = true })

        assertFalse(result.success)
        assertEquals(
            R.string.system_settings_save_failed,
            result.messageResId,
        )
        assertFalse(changed[0])
        assertFalse(store.hasRealPackageConfig("com.example.app"))
    }

    companion object {
        private fun app(packageName: String): AppListItem {
            return AppListItem(
                "Example",
                packageName,
                false,
                true,
                null,
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                null,
                true,
                false,
                false,
                null
            )
        }
    }
}
