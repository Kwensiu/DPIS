package com.dpis.module

import com.dpis.module.config.ConfigSnapshotLoader
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.hooks.HookDomainOverrideStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HookDomainOverrideStoreTest {
    @Test
    fun missingKeyUsesAutomaticPath() {
        val store = HookDomainOverrideStore(DpisConfigStore(FakePrefs()))
        val override = store.read("com.example.app")
        assertFalse(override.customPathEnabled)
        assertTrue(override.enabledKnownDomains.isEmpty())
        assertTrue(override.unknownDomains.isEmpty())
    }

    @Test
    fun emptySelectionRemainsAnExplicitOptOutAcrossSnapshots() {
        val config = DpisConfigStore(FakePrefs())
        val store = HookDomainOverrideStore(config)
        assertTrue(store.save("com.example.app", emptySet(), emptySet()))
        assertEquals("", config.getPackageFontHookDomainsRaw("com.example.app"))
        val snapshot =
            requireNotNull(ConfigSnapshotLoader.fromStore(config).getPackage("com.example.app"))
        assertTrue(snapshot.hookDomainOverride.customPathEnabled)
        assertTrue(snapshot.hookDomainOverride.enabledKnownDomains.isEmpty())
    }

    @Test
    fun readAndSavePreserveUnknownDomainsButKeepKnownOrder() {
        val config = DpisConfigStore(FakePrefs())
        val store = HookDomainOverrideStore(config)
        assertTrue(
            config.setPackageFontHookDomainsRaw(
                "com.example.app",
                "unknown_one,webview_text_zoom,removed_domain,resources_font"
            )
        )
        val override = store.read("com.example.app")
        assertEquals(
            linkedSetOf("resources_font", "webview_text_zoom"),
            override.enabledKnownDomains
        )
        assertEquals(linkedSetOf("unknown_one", "removed_domain"), override.unknownDomains)
        assertTrue(
            store.save(
                "com.example.app",
                linkedSetOf("hyperos_native_flutter"),
                override.unknownDomains
            )
        )
        assertEquals(
            "hyperos_native_flutter,unknown_one,removed_domain",
            config.getPackageFontHookDomainsRaw("com.example.app")
        )
    }

    @Test
    fun systemOnlyDomainsAreExcludedFromCompatCustomPath() {
        val config = DpisConfigStore(FakePrefs())
        val store = HookDomainOverrideStore(config)
        assertTrue(
            store.save(
                "com.example.app",
                linkedSetOf(
                    "activity_thread_font",
                    "system_server_font",
                    FontHookDomainRegistry.ID_RESOURCES_FONT,
                    "webview_text_zoom"
                ),
                emptySet()
            )
        )
        assertEquals(
            "resources_font,webview_text_zoom",
            config.getPackageFontHookDomainsRaw("com.example.app")
        )
    }

    @Test
    fun restoreRecommendedClearsCustomDomains() {
        val config = DpisConfigStore(FakePrefs())
        val store = HookDomainOverrideStore(config)
        assertTrue(store.save("com.example.app", setOf("resources_font"), setOf("removed_domain")))
        assertTrue(store.restoreRecommended("com.example.app"))
        assertNull(config.getPackageFontHookDomainsRaw("com.example.app"))
        assertFalse(store.read("com.example.app").customPathEnabled)
    }

    @Test
    fun automaticEquivalentSelectionClearsCustomState() {
        val override = HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
            HookDomainOverride(
                true,
                linkedSetOf("resources_font", "webview_text_zoom"),
                emptySet()
            ),
            linkedSetOf("resources_font", "webview_text_zoom"),
        )
        assertFalse(override.customPathEnabled)
    }

    @Test
    fun automaticEquivalentSelectionKeepsUnknownDomainsCustom() {
        val override = HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
            HookDomainOverride(
                true,
                linkedSetOf("resources_font", "webview_text_zoom"),
                setOf("removed_domain")
            ),
            linkedSetOf("resources_font", "webview_text_zoom"),
        )
        assertTrue(override.customPathEnabled)
        assertEquals(setOf("removed_domain"), override.unknownDomains)
    }

    @Test
    fun previewSelectionDoesNotWriteStoreState() {
        val config = DpisConfigStore(FakePrefs())
        val store = HookDomainOverrideStore(config)
        val raw = HookDomainOverrideStore.rawValueForSelection(
            linkedSetOf("hyperos_native_flutter", "resources_font"),
            linkedSetOf("resources_font"),
            setOf("removed_domain"),
        )
        assertEquals("resources_font,hyperos_native_flutter,removed_domain", raw)
        assertFalse(config.getConfiguredPackages().contains("com.example.app"))
        assertNull(config.getPackageFontHookDomainsRaw("com.example.app"))
        assertTrue(store.read("com.example.app").enabledKnownDomains.isEmpty())
    }

    @Test
    fun snapshotReplacementPreservesEmptyCustomPath() {
        val source = DpisConfigStore(FakePrefs())
        assertTrue(HookDomainOverrideStore(source).save("com.example.app", emptySet(), emptySet()))
        val target = DpisConfigStore(FakePrefs())
        assertTrue(target.replaceAll(source.snapshotAll().mapKeys { it.key as String? }
            .toMutableMap()))
        val override = HookDomainOverrideStore(target).read("com.example.app")
        assertTrue(override.customPathEnabled)
        assertTrue(override.enabledKnownDomains.isEmpty())
    }

    @Test
    fun restoringRecommendedKeepsExplicitPackageConfiguration() {
        val config = DpisConfigStore(FakePrefs())
        val store = HookDomainOverrideStore(config)
        assertTrue(store.save("com.example.app", setOf("resources_font"), emptySet()))
        assertTrue(config.setTargetDpisEnabled("com.example.app", false))
        assertTrue(config.setTargetDpisEnabled("com.example.app", true))
        assertTrue(config.setTargetDpisEnabled("com.example.app", false))
        assertTrue(store.restoreRecommended("com.example.app"))
        assertTrue(config.getConfiguredPackages().contains("com.example.app"))
    }
}
