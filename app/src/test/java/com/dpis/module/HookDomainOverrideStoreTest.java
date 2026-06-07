package com.dpis.module;

import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HookDomainOverrideStoreTest {
    @Test
    public void missingKeyUsesAutomaticPath() {
        HookDomainOverrideStore store = new HookDomainOverrideStore(
                new DpiConfigStore(new FakePrefs()));

        HookDomainOverride override = store.read("com.example.app");

        assertFalse(override.customPathEnabled);
        assertTrue(override.enabledKnownDomains.isEmpty());
        assertTrue(override.unknownDomains.isEmpty());
    }

    @Test
    public void emptyKeyStoresCustomZeroDomainPath() {
        DpiConfigStore configStore = new DpiConfigStore(new FakePrefs());
        HookDomainOverrideStore store = new HookDomainOverrideStore(configStore);

        assertTrue(store.save("com.example.app", Set.of(), Set.of()));

        HookDomainOverride override = store.read("com.example.app");
        assertTrue(override.customPathEnabled);
        assertTrue(override.enabledKnownDomains.isEmpty());
        assertTrue(override.unknownDomains.isEmpty());
        assertEquals("", configStore.getPackageFontHookDomainsRaw("com.example.app"));
        assertTrue(configStore.getConfiguredPackages().contains("com.example.app"));
    }

    @Test
    public void emptyCustomPathSurvivesSnapshotAsExplicitOptOut() {
        DpiConfigStore configStore = new DpiConfigStore(new FakePrefs());
        HookDomainOverrideStore store = new HookDomainOverrideStore(configStore);

        assertTrue(store.save("com.example.app", Set.of(), Set.of()));

        PackageConfigSnapshot snapshot =
                ConfigSnapshotLoader.fromStore(configStore).getPackage("com.example.app");
        assertTrue(snapshot.hookDomainOverride.customPathEnabled);
        assertTrue(snapshot.hookDomainOverride.enabledKnownDomains.isEmpty());
        assertTrue(snapshot.hookDomainOverride.unknownDomains.isEmpty());
    }

    @Test
    public void unknownIdsArePreservedOnReadAndSave() {
        DpiConfigStore configStore = new DpiConfigStore(new FakePrefs());
        HookDomainOverrideStore store = new HookDomainOverrideStore(configStore);

        assertTrue(configStore.setPackageFontHookDomainsRaw(
                "com.example.app",
                "unknown_one,webview_text_zoom,removed_domain,resources_font"));
        HookDomainOverride override = store.read("com.example.app");
        assertEquals(orderedSet("resources_font", "webview_text_zoom"),
                override.enabledKnownDomains);
        assertEquals(orderedSet("unknown_one", "removed_domain"), override.unknownDomains);

        assertTrue(store.save("com.example.app",
                orderedSet("hyperos_native_flutter"),
                override.unknownDomains));

        assertEquals("hyperos_native_flutter,unknown_one,removed_domain",
                configStore.getPackageFontHookDomainsRaw("com.example.app"));
    }

    @Test
    public void saveIgnoresSystemOnlyDomainsForCompatCustomPath() {
        DpiConfigStore configStore = new DpiConfigStore(new FakePrefs());
        HookDomainOverrideStore store = new HookDomainOverrideStore(configStore);

        assertTrue(store.save("com.example.app",
                orderedSet("activity_thread_font", "system_server_font",
                        "resources_font", "webview_text_zoom"),
                Set.of()));

        assertEquals("resources_font,webview_text_zoom",
                configStore.getPackageFontHookDomainsRaw("com.example.app"));
    }

    @Test
    public void readDropsSystemOnlyDomainsFromStaleCustomPath() {
        DpiConfigStore configStore = new DpiConfigStore(new FakePrefs());
        HookDomainOverrideStore store = new HookDomainOverrideStore(configStore);
        assertTrue(configStore.setPackageFontHookDomainsRaw(
                "com.example.app",
                "resources_font,system_server_font,activity_thread_font,webview_text_zoom"));

        HookDomainOverride override = store.read("com.example.app");

        assertTrue(override.customPathEnabled);
        assertEquals(orderedSet("resources_font", "webview_text_zoom"),
                override.enabledKnownDomains);
    }

    @Test
    public void restoreClearsCustomKeyAndUnknownIds() {
        DpiConfigStore configStore = new DpiConfigStore(new FakePrefs());
        HookDomainOverrideStore store = new HookDomainOverrideStore(configStore);
        assertTrue(store.save("com.example.app",
                orderedSet("resources_font"),
                orderedSet("removed_domain")));

        assertTrue(store.restoreRecommended("com.example.app"));

        assertNull(configStore.getPackageFontHookDomainsRaw("com.example.app"));
        assertFalse(store.read("com.example.app").customPathEnabled);
    }

    @Test
    public void restoreKeepsPackageWhenExplicitDpisEnabledKeyRemains() {
        DpiConfigStore configStore = new DpiConfigStore(new FakePrefs());
        HookDomainOverrideStore store = new HookDomainOverrideStore(configStore);
        assertTrue(store.save("com.example.app", orderedSet("resources_font"), Set.of()));
        assertTrue(configStore.setTargetDpisEnabled("com.example.app", false));
        assertTrue(configStore.setTargetDpisEnabled("com.example.app", true));
        assertTrue(configStore.setTargetDpisEnabled("com.example.app", false));

        assertTrue(store.restoreRecommended("com.example.app"));

        assertNull(configStore.getPackageFontHookDomainsRaw("com.example.app"));
        assertTrue(configStore.getConfiguredPackages().contains("com.example.app"));
    }

    @Test
    public void savingAutomaticKnownPathClearsKeyWhenNoUnknownIdsExist() {
        DpiConfigStore configStore = new DpiConfigStore(new FakePrefs());
        HookDomainOverrideStore store = new HookDomainOverrideStore(configStore);
        assertTrue(store.save("com.example.app",
                orderedSet("resources_font"),
                Set.of()));

        assertTrue(store.saveCustomIfDifferentFromAutomatic(
                "com.example.app",
                orderedSet("resources_font", "webview_text_zoom"),
                orderedSet("resources_font", "webview_text_zoom"),
                Set.of()));

        assertNull(configStore.getPackageFontHookDomainsRaw("com.example.app"));
    }

    @Test
    public void snapshotAndReplaceAllPreserveEmptyCustomPathValue() {
        FakePrefs primary = new FakePrefs();
        DpiConfigStore source = new DpiConfigStore(primary);
        HookDomainOverrideStore sourceOverrides = new HookDomainOverrideStore(source);
        assertTrue(sourceOverrides.save("com.example.app", Set.of(), Set.of()));

        DpiConfigStore target = new DpiConfigStore(new FakePrefs());
        assertTrue(target.replaceAll(source.snapshotAll()));

        HookDomainOverride targetOverride =
                new HookDomainOverrideStore(target).read("com.example.app");
        assertTrue(targetOverride.customPathEnabled);
        assertTrue(targetOverride.enabledKnownDomains.isEmpty());
    }

    @Test
    public void clearingLastViewportAndFontValuesKeepsPackageWhenCustomPathRemains() {
        DpiConfigStore configStore = new DpiConfigStore(new FakePrefs());
        HookDomainOverrideStore store = new HookDomainOverrideStore(configStore);
        assertTrue(configStore.setTargetViewportWidthDp("com.example.app", 411));
        assertTrue(configStore.setTargetFontScalePercent("com.example.app", 130));
        assertTrue(store.save("com.example.app",
                orderedSet("resources_font"),
                orderedSet("removed_domain")));

        assertTrue(configStore.clearTargetViewportWidthDp("com.example.app"));
        assertTrue(configStore.clearTargetFontScalePercent("com.example.app"));
        assertTrue(configStore.setTargetFontApplyMode("com.example.app", FontApplyMode.OFF));
        assertTrue(configStore.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.OFF));

        assertTrue(configStore.getConfiguredPackages().contains("com.example.app"));
        PackageConfigSnapshot snapshot =
                ConfigSnapshotLoader.fromStore(configStore).getPackage("com.example.app");
        assertTrue(snapshot.hookDomainOverride.customPathEnabled);
        assertEquals(orderedSet("resources_font"), snapshot.hookDomainOverride.enabledKnownDomains);
        assertEquals(orderedSet("removed_domain"), snapshot.hookDomainOverride.unknownDomains);
    }

    @Test
    public void rawValueForSelectionReturnsNullWhenSelectionMatchesAutomaticAndNoUnknowns() {
        assertNull(HookDomainOverrideStore.rawValueForSelection(
                orderedSet("resources_font", "webview_text_zoom"),
                orderedSet("resources_font", "webview_text_zoom"),
                Set.of()));
    }

    @Test
    public void rawValueForSelectionKeepsPreviewDomainsWithoutWritingStoreState() {
        DpiConfigStore configStore = new DpiConfigStore(new FakePrefs());
        HookDomainOverrideStore store = new HookDomainOverrideStore(configStore);

        String raw = HookDomainOverrideStore.rawValueForSelection(
                orderedSet("hyperos_native_flutter", "resources_font"),
                orderedSet("resources_font"),
                orderedSet("removed_domain"));

        assertEquals("resources_font,hyperos_native_flutter,removed_domain", raw);
        assertFalse(configStore.getConfiguredPackages().contains("com.example.app"));
        assertNull(configStore.getPackageFontHookDomainsRaw("com.example.app"));
        assertTrue(store.read("com.example.app").enabledKnownDomains.isEmpty());
    }

    private static LinkedHashSet<String> orderedSet(String... values) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String value : values) {
            set.add(value);
        }
        return set;
    }
}
