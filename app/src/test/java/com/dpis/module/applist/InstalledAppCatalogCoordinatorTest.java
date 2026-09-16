package com.dpis.module.applist;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import com.dpis.module.config.DpisConfigStore;
import com.dpis.module.FakePrefs;


import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InstalledAppCatalogCoordinatorTest {
    @Test
    public void inScopePackageIsUserVisibleConfiguredEvenWithoutSavedValues() {
        assertTrue(InstalledAppCatalogPolicy.isUserVisibleConfiguredPackage(
                null,
                "com.example.injected",
                true,
                true));
    }

    @Test
    public void unknownScopeDoesNotMakePackageUserVisibleConfigured() {
        assertFalse(InstalledAppCatalogPolicy.isUserVisibleConfiguredPackage(
                null,
                "com.example.legacy",
                false,
                true));
    }

    @Test
    public void plainPackageWithoutSavedValuesIsNotUserVisibleConfigured() {
        assertFalse(InstalledAppCatalogPolicy.isUserVisibleConfiguredPackage(
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

        Set<String> configured = InstalledAppCatalogPolicy
                .userVisibleConfiguredPackages(store);

        assertTrue(configured.contains("com.example.saved"));
        assertFalse(configured.contains("com.example.plain"));
    }

    @Test
    public void userVisibleConfiguredPackagesIncludesKnownScopeOnlyPackages() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetFontScalePercent("com.example.saved", 125);

        Set<String> configured = InstalledAppCatalogPolicy
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

        Set<String> configured = InstalledAppCatalogPolicy
                .userVisibleConfiguredPackages(store);

        assertTrue(configured.contains("com.example.saved"));
        assertFalse(configured.contains("system"));
        assertFalse(configured.contains("android"));
    }

    @Test
    public void userVisibleConfiguredPackagesExcludesSystemFrameworkScopeAliases() {
        Set<String> configured = InstalledAppCatalogPolicy
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
        assertFalse(InstalledAppCatalogPolicy.isUserVisibleConfiguredPackage(
                null,
                "system",
                true,
                true));
        assertFalse(InstalledAppCatalogPolicy.isUserVisibleConfiguredPackage(
                null,
                "android",
                true,
                true));
    }

    @Test
    public void unconfiguredItemUsesTheSameDefaultStatusWithoutStoreReads() {
        AppListItem item = InstalledAppCatalogPolicy.createUnconfiguredAppListItem(
                "Plain", "com.example.plain", false, true, false, false, true);

        assertFalse(item.configured);
        assertTrue(item.dpisEnabled);
        assertFalse(item.viewportTargetSpec.isEnabled());
        assertFalse(item.hasAppSpecificConfig());
    }

    @Test
    public void launcherFallbackIsUsedWhenPackageManagerReturnsOnlySelf() {
        assertTrue(InstalledAppCatalogPolicy.shouldUseLauncherVisibilityFallback(
                Collections.singletonList("io.github.kwensiu.dpis"),
                "io.github.kwensiu.dpis"));
        assertTrue(InstalledAppCatalogPolicy.shouldUseLauncherVisibilityFallback(
                Collections.emptyList(), "io.github.kwensiu.dpis"));
    }

    @Test
    public void launcherFallbackIsNotUsedWhenPackageManagerReturnsAnotherApp() {
        assertFalse(InstalledAppCatalogPolicy.shouldUseLauncherVisibilityFallback(
                Arrays.asList("io.github.kwensiu.dpis", "com.example.launcher"),
                "io.github.kwensiu.dpis"));
    }

    @Test
    public void unresolvedCatalogLabelPrefersNonLocalizedLabel() {
        ApplicationInfo info = new ApplicationInfo();
        info.nonLocalizedLabel = "Camera";
        assertEquals(
                "Camera",
                InstalledAppCatalogPolicy.unresolvedCatalogLabel(info, "com.android.camera"));
    }

    @Test
    public void unresolvedCatalogLabelFallsBackToPackageName() {
        assertEquals(
                "com.example.app",
                InstalledAppCatalogPolicy.unresolvedCatalogLabel(null, "com.example.app"));
        ApplicationInfo info = new ApplicationInfo();
        assertEquals(
                "com.example.app",
                InstalledAppCatalogPolicy.unresolvedCatalogLabel(info, "com.example.app"));
    }

    @Test
    public void catalogItemCopiesPackageInfoTimestamps() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = "com.example.app";
        packageInfo.firstInstallTime = 11L;
        packageInfo.lastUpdateTime = 22L;
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = "com.example.app";
        packageInfo.applicationInfo = applicationInfo;

        InstalledAppCatalogItem item = InstalledAppCatalogPolicy.createCatalogItem(
                packageInfo, "io.github.kwensiu.dpis", "com.example.app", false);

        assertEquals("com.example.app", item.packageName);
        assertEquals(11L, item.firstInstallTime);
        assertEquals(22L, item.lastUpdateTime);
        assertFalse(item.labelResolved);
    }

    @Test
    public void catalogItemSkipsSelfPackage() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = "io.github.kwensiu.dpis";
        assertNull(InstalledAppCatalogPolicy.createCatalogItem(
                packageInfo, "io.github.kwensiu.dpis", "DPIS", false));
    }

    @Test
    public void installedCatalogChangeActionsMatchPackageLifecycle() {
        assertTrue(InstalledAppCatalogPolicy.isInstalledCatalogChangeAction(
                Intent.ACTION_PACKAGE_ADDED));
        assertTrue(InstalledAppCatalogPolicy.isInstalledCatalogChangeAction(
                Intent.ACTION_PACKAGE_REMOVED));
        assertTrue(InstalledAppCatalogPolicy.isInstalledCatalogChangeAction(
                Intent.ACTION_PACKAGE_CHANGED));
        assertTrue(InstalledAppCatalogPolicy.isInstalledCatalogChangeAction(
                Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE));
        assertFalse(InstalledAppCatalogPolicy.isInstalledCatalogChangeAction(
                Intent.ACTION_BOOT_COMPLETED));
        assertFalse(InstalledAppCatalogPolicy.isInstalledCatalogChangeAction(
                Intent.ACTION_LOCALE_CHANGED));
        assertTrue(InstalledAppCatalogPolicy.shouldInvalidateInstalledCatalog(
                Intent.ACTION_LOCALE_CHANGED));
        assertTrue(InstalledAppCatalogPolicy.shouldInvalidateInstalledCatalog(
                Intent.ACTION_PACKAGE_CHANGED));
        assertFalse(InstalledAppCatalogPolicy.shouldInvalidateInstalledCatalog(
                Intent.ACTION_BOOT_COMPLETED));
    }
}
