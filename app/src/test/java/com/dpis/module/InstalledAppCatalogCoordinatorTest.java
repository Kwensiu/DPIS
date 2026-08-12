package com.dpis.module.applist;

import android.content.pm.ApplicationInfo;

import com.dpis.module.DpisConfigStore;
import com.dpis.module.FakePrefs;


import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
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
    public void userVisibleConfiguredPackagesIncludesKnownScopeOnlyPackages() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetFontScalePercent("com.example.saved", 125);

        Set<String> configured = InstalledAppCatalogCoordinator
                .userVisibleConfiguredPackages(
                        store,
                        Set.of("com.example.injected"),
                        true);

        assertTrue(configured.contains("com.example.saved"));
        assertTrue(configured.contains("com.example.injected"));
        assertFalse(configured.contains("com.example.plain"));
    }

    @Test
    public void userVisibleConfiguredPackagesExcludesPersistedSystemFrameworkAliases() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetFontScalePercent("system", 125);
        store.setTargetFontScalePercent("android", 125);
        store.setTargetFontScalePercent("com.example.saved", 125);

        Set<String> configured = InstalledAppCatalogCoordinator
                .userVisibleConfiguredPackages(store);

        assertTrue(configured.contains("com.example.saved"));
        assertFalse(configured.contains("system"));
        assertFalse(configured.contains("android"));
    }

    @Test
    public void userVisibleConfiguredPackagesExcludesSystemFrameworkScopeAliases() {
        Set<String> configured = InstalledAppCatalogCoordinator
                .userVisibleConfiguredPackages(
                        null,
                        Set.of("system", "android", "com.example.injected"),
                        true);

        assertTrue(configured.contains("com.example.injected"));
        assertFalse(configured.contains("system"));
        assertFalse(configured.contains("android"));
    }

    @Test
    public void systemFrameworkScopeAliasesAreNotConfiguredByScopeOnlyState() {
        assertFalse(InstalledAppCatalogCoordinator.isUserVisibleConfiguredPackage(
                null,
                "system",
                true,
                true));
        assertFalse(InstalledAppCatalogCoordinator.isUserVisibleConfiguredPackage(
                null,
                "android",
                true,
                true));
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

    @Test
    public void launcherFallbackIsUsedWhenPackageManagerReturnsOnlySelf() {
        ApplicationInfo self = new ApplicationInfo();
        self.packageName = "io.github.kwensiu.dpis";

        assertTrue(InstalledAppCatalogCoordinator.shouldUseLauncherVisibilityFallback(
                Collections.singletonList(self), self.packageName));
        assertTrue(InstalledAppCatalogCoordinator.shouldUseLauncherVisibilityFallback(
                Collections.emptyList(), self.packageName));
    }

    @Test
    public void launcherFallbackIsNotUsedWhenPackageManagerReturnsAnotherApp() {
        ApplicationInfo self = new ApplicationInfo();
        self.packageName = "io.github.kwensiu.dpis";
        ApplicationInfo other = new ApplicationInfo();
        other.packageName = "com.example.launcher";

        assertFalse(InstalledAppCatalogCoordinator.shouldUseLauncherVisibilityFallback(
                Arrays.asList(self, other), self.packageName));
    }
}
