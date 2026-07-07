package com.dpis.module;

import com.dpis.module.applist.InstalledAppCatalogCoordinator;


import org.junit.Test;

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
}
