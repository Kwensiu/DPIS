package com.dpis.module;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class DisplayHookInstallerTest {
    @Test
    public void enablesDisplayOverrideForConfiguredPackage() {
        assertEquals(true, DisplayHookInstaller.shouldApplyOverrideForPackage(
                "com.max.xiaoheihe", "com.max.xiaoheihe"));
    }

    @Test
    public void skipsDisplayOverrideForMissingPackage() {
        assertEquals(false, DisplayHookInstaller.shouldApplyOverrideForPackage(null, "com.max.xiaoheihe"));
    }

    @Test
    public void skipsDisplayOverrideForDifferentCurrentPackage() {
        assertEquals(false, DisplayHookInstaller.shouldApplyOverrideForPackage(
                "com.max.xiaoheihe", "com.example.other"));
    }

    @Test
    public void skipsDisplayOverrideForBlankTargetPackage() {
        assertEquals(false, DisplayHookInstaller.shouldApplyOverrideForPackage(
                "  ", "com.max.xiaoheihe"));
    }

    @Test
    public void skipsWhenCurrentPackageCannotBeResolved() {
        assertEquals(false, DisplayHookInstaller.shouldApplyOverrideForPackage("com.max.xiaoheihe", null));
    }

    @Test
    public void compat100CanInitializeDisplayTarget() {
        DisplayHookInstaller.setTargetPackageNameForCompat100("com.max.xiaoheihe");
        setCurrentPackageResolver("com.max.xiaoheihe");

        assertEquals(true, DisplayHookInstaller.shouldApplyOverrideForPackage("com.max.xiaoheihe"));

        DisplayHookInstaller.setTargetPackageNameForCompat100(null);
        clearCurrentPackageResolver();
    }

    private static String testCurrentPackageName() {
        return "com.max.xiaoheihe";
    }

    private static void setCurrentPackageResolver(String ignored) {
        try {
            Method method = DisplayHookInstallerTest.class.getDeclaredMethod("testCurrentPackageName");
            method.setAccessible(true);
            Field resolverField = DisplayHookInstaller.class.getDeclaredField("currentPackageNameMethod");
            resolverField.setAccessible(true);
            resolverField.set(null, method);
            Field unavailableField =
                    DisplayHookInstaller.class.getDeclaredField("currentPackageNameUnavailable");
            unavailableField.setAccessible(true);
            unavailableField.setBoolean(null, false);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void clearCurrentPackageResolver() {
        try {
            Field resolverField = DisplayHookInstaller.class.getDeclaredField("currentPackageNameMethod");
            resolverField.setAccessible(true);
            resolverField.set(null, null);
            Field unavailableField =
                    DisplayHookInstaller.class.getDeclaredField("currentPackageNameUnavailable");
            unavailableField.setAccessible(true);
            unavailableField.setBoolean(null, false);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
