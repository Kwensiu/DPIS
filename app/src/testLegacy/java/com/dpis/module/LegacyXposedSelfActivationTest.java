package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LegacyXposedSelfActivationTest {
    @Test
    public void otherPackageDoesNotMarkCurrentAppProcessActivated() {
        DpisApplication.clearXposedSelfLoadedForTest();

        LegacyXposedSelfActivation.markIfSelfPackageForTest(
                "com.example.target",
                getClass().getClassLoader(),
                "test");

        assertFalse(DpisApplication.isXposedSelfLoaded());
    }

    @Test
    public void selfPackageRouteDelegatesToSharedMarkerBeforeConstructorHook() throws Exception {
        String source = SourceSmokeTestPaths.read(
                "src/legacy/java/com/dpis/module/LegacyXposedSelfActivation.java");

        int markIndex = source.indexOf("XposedSelfActivation.markIfSelfPackage(");
        int constructorIndex = source.indexOf("installConstructorMarker(classLoader, source)");

        assertTrue(markIndex > 0);
        assertTrue(constructorIndex > markIndex);
        assertTrue(source.contains("XposedBridge.hookAllConstructors"));
    }
}
