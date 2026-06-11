package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class LegacyXposedSelfActivationTest {
    @Before
    public void setUp() {
        DpisApplication.clearXposedSelfLoadedForTest();
    }

    @After
    public void tearDown() {
        DpisApplication.clearXposedSelfLoadedForTest();
    }

    @Test
    public void selfPackageMarksCurrentAppProcessActivated() {
        LegacyXposedSelfActivation.markIfSelfPackageForTest(
                BuildConfig.APPLICATION_ID,
                getClass().getClassLoader(),
                "test");

        assertTrue(DpisApplication.isXposedSelfLoaded());
    }

    @Test
    public void otherPackageDoesNotMarkCurrentAppProcessActivated() {
        LegacyXposedSelfActivation.markIfSelfPackageForTest(
                "com.example.target",
                getClass().getClassLoader(),
                "test");

        assertFalse(DpisApplication.isXposedSelfLoaded());
    }
}
