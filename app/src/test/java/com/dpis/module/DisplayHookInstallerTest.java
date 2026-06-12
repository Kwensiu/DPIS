package com.dpis.module;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
    public void legacyCanInitializeDisplayTarget() {
        DisplayHookInstaller.setTargetPackageNameForLegacy("com.max.xiaoheihe");
        DisplayHookInstaller.setTargetStoreForLegacy(new DpiConfigStore(new FakePrefs()));
        setCurrentPackageResolver("com.max.xiaoheihe");

        assertEquals(true, DisplayHookInstaller.shouldApplyOverrideForPackage("com.max.xiaoheihe"));

        DisplayHookInstaller.setTargetPackageNameForLegacy(null);
        DisplayHookInstaller.setTargetStoreForLegacy(null);
        clearCurrentPackageResolver();
    }

    @Test
    public void skipsGlobalDisplayStateWithoutPackageScopedRecord() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        store.setTargetViewportSpec("com.tencent.mm", ViewportTargetSpec.relativeScale(1500));
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(900, 1800, 900,
                240, 1080, 2160));

        assertNull(DisplayHookInstaller.resolvePackageScopedOverrideForTest(
                "com.tencent.mm", store));

        VirtualDisplayState.set(null);
    }

    @Test
    public void usesPackageScopedDisplayRecordForCurrentTarget() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(1500);
        store.setTargetViewportSpec("com.tencent.mm", targetSpec);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.systemDisplayInfo(
                360, 736, 360, 480, 1080, 2208);
        VirtualDisplayOverride.Result virtualDisplay =
                new VirtualDisplayOverride.Result(540, 1104, 540, 320, 1080, 2208);

        VirtualDisplayState.publish(
                "com.tencent.mm",
                targetSpec,
                source,
                new ViewportOverride.Result(540, 1104, 540, 320),
                virtualDisplay,
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);

        assertEquals(320, DisplayHookInstaller.resolvePackageScopedOverrideForTest(
                "com.tencent.mm", store).densityDpi);

        VirtualDisplayState.set(null);
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
