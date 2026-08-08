package com.dpis.module.applist;

import com.dpis.module.DpisConfigStore;
import com.dpis.module.FakePrefs;


import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InstalledAppCatalogCoordinatorTest {
    @Test
    public void inScopePackageIsUserVisibleConfiguredEvenWithoutSavedValues() {
        assertTrue(InstalledAppCatalogCoordinator.isUserVisibleConfiguredPackage(
                null,
                "com.example.injected",
                true,
                true));
    }

    @Test
    public void unknownScopeDoesNotMakePackageUserVisibleConfigured() {
        assertFalse(InstalledAppCatalogCoordinator.isUserVisibleConfiguredPackage(
                null,
                "com.example.legacy",
                false,
                true));
    }

    @Test
    public void plainPackageWithoutSavedValuesIsNotUserVisibleConfigured() {
        assertFalse(InstalledAppCatalogCoordinator.isUserVisibleConfiguredPackage(
                null,
                "com.example.plain",
                true,
                false));
    }

    @Test
    public void userVisibleConfiguredPackagesValidatesOnlyPersistedCandidates() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetFontScalePercent("com.example.saved", 125);

        Set<String> configured = InstalledAppCatalogCoordinator
                .userVisibleConfiguredPackages(store);

        assertTrue(configured.contains("com.example.saved"));
        assertFalse(configured.contains("com.example.plain"));
    }

    @Test
    public void unconfiguredItemUsesTheSameDefaultStatusWithoutStoreReads() {
        AppListItem item = InstalledAppCatalogCoordinator.createUnconfiguredAppListItem(
                "Plain", "com.example.plain", false, true, false, false, true);

        assertFalse(item.configured);
        assertTrue(item.dpisEnabled);
        assertFalse(item.viewportTargetSpec.isEnabled());
        assertFalse(item.hasAppSpecificConfig());
    }
}
