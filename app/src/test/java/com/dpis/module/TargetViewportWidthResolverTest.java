package com.dpis.module;

import com.dpis.module.viewport.VirtualDisplayState;

import com.dpis.module.viewport.TargetViewportWidthResolver;
import com.dpis.module.viewport.ViewportRuntimeRecord;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportOverride;
import com.dpis.module.viewport.ViewportSourceSnapshot;
import com.dpis.module.viewport.ViewportTargetResolution;
import com.dpis.module.viewport.ViewportTargetSpec;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TargetViewportWidthResolverTest {
    @After
    public void tearDown() {
        VirtualDisplayState.set(null);
    }

    @Test
    public void returnsNullWhenEmulationModeAndSystemHookOff() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setSystemServerHooksEnabled(false);
        store.setTargetViewportWidthDp("com.example.target", 360);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.SYSTEM_EMULATION);

        Integer value = TargetViewportWidthResolver.resolve(store, "com.example.target");

        assertNull(value);
    }

    @Test
    public void keepsWidthWhenReplaceModeAndSystemHookOff() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setSystemServerHooksEnabled(false);
        store.setTargetViewportWidthDp("com.example.target", 360);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.FIELD_REWRITE);

        Integer value = TargetViewportWidthResolver.resolve(store, "com.example.target");

        assertEquals(Integer.valueOf(360), value);
    }

    @Test
    public void explicitRuntimeClearDoesNotShadowReplaceModeStoreWidth() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetViewportWidthDp("com.example.target", 360);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.FIELD_REWRITE);

        Integer value = TargetViewportWidthResolver.resolveForTest(
                store, "com.example.target", 0);

        assertEquals(Integer.valueOf(360), value);
    }

    @Test
    public void explicitRuntimeClearDisablesSystemEmulationStoreWidth() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetViewportWidthDp("com.example.target", 360);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.SYSTEM_EMULATION);

        Integer value = TargetViewportWidthResolver.resolveForTest(
                store, "com.example.target", 0);

        assertNull(value);
    }

    @Test
    public void absoluteDpUsesRuntimeRecordBeforeReDerivingTargetConfiguration() {
        String packageName = "com.example.viewport";
        ViewportTargetSpec targetSpec = ViewportTargetSpec.absoluteDp(500);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.systemDisplayInfo(
                500,
                1022,
                500,
                432,
                1080,
                2208);
        ViewportSourceSnapshot systemSource = ViewportSourceSnapshot.systemDisplayInfo(
                360,
                736,
                360,
                480,
                1080,
                2208);
        VirtualDisplayState.publish(
                packageName,
                targetSpec,
                systemSource,
                new ViewportOverride.Result(500, 1022, 500, 346),
                null,
                ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER);
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec(packageName, targetSpec);
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);

        ViewportTargetResolution resolution =
                TargetViewportWidthResolver.resolve(store, packageName, source);

        assertNotNull(resolution.record);
        assertEquals(500, resolution.effectiveSmallestWidthDp);
        assertEquals(346, resolution.record.viewportResult.densityDpi);
    }

    @Test
    public void resourcesReadRelativeScaleDoesNotDeriveTargetWithoutDisplayRecord() {
        String packageName = "com.example.viewport";
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(150000));
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_READ,
                configuration(360, 640, 360, 480),
                null);

        ViewportTargetResolution resolution =
                TargetViewportWidthResolver.resolve(store, packageName, source);

        assertEquals("relative-scale-no-display-baseline", resolution.reason);
        assertEquals(0, resolution.effectiveSmallestWidthDp);
        assertNull(resolution.record);
    }

    @Test
    public void resourcesReadRelativeScaleDoesNotCompoundAlreadyScaledTarget() {
        String packageName = "com.example.viewport";
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(83333));
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_READ,
                configuration(480, 900, 480, 480),
                null);

        ViewportTargetResolution resolution =
                TargetViewportWidthResolver.resolve(store, packageName, source);

        assertEquals("relative-scale-no-display-baseline", resolution.reason);
        assertEquals(0, resolution.effectiveSmallestWidthDp);
        assertNull(resolution.record);
    }

    @Test
    public void appProcessRelativeScaleBorrowKeepsDisplayRecordForStableDensity() {
        String packageName = "com.example.viewport";
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(150000);
        android.content.res.Configuration displayConfig = configuration(360, 792, 360, 480);
        VirtualDisplayState.publish(
                packageName,
                targetSpec,
                ViewportSourceSnapshot.fromConfiguration(
                        ViewportSourceSnapshot.ORIGIN_RESOURCES_MANAGER,
                        displayConfig,
                        null),
                new ViewportOverride.Result(540, 1188, 540, 320),
                null,
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec(packageName, targetSpec);
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_READ,
                configuration(360, 640, 360, 320),
                null);

        ViewportTargetResolution resolution =
                TargetViewportWidthResolver.resolve(store, packageName, source);

        assertEquals(ViewportTargetResolution.REASON_APP_PROCESS_BORROW_TARGET,
                resolution.reason);
        assertEquals(540, resolution.effectiveSmallestWidthDp);
        assertNotNull(resolution.record);
        assertEquals(320, resolution.record.viewportResult.densityDpi);
    }

    @Test
    public void appProcessRelativeScaleTreatsMixedTargetSmallestWidthAsBorrowedRecord() {
        String packageName = "com.example.viewport";
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(150000);
        VirtualDisplayState.publish(
                packageName,
                targetSpec,
                ViewportSourceSnapshot.fromConfiguration(
                        ViewportSourceSnapshot.ORIGIN_RESOURCES_MANAGER,
                        configuration(360, 792, 360, 480),
                        null),
                new ViewportOverride.Result(540, 1188, 540, 320),
                null,
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec(packageName, targetSpec);
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);
        ViewportSourceSnapshot mixedWindowSource = ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_READ,
                configuration(360, 640, 540, 480),
                null);

        ViewportTargetResolution resolution =
                TargetViewportWidthResolver.resolve(store, packageName, mixedWindowSource);

        assertEquals(ViewportTargetResolution.REASON_APP_PROCESS_BORROW_TARGET,
                resolution.reason);
        assertEquals(540, resolution.effectiveSmallestWidthDp);
        assertNotNull(resolution.record);
        assertEquals(320, resolution.record.viewportResult.densityDpi);
    }

    private static android.content.res.Configuration configuration(
            int widthDp, int heightDp, int smallestWidthDp, int densityDpi) {
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.screenWidthDp = widthDp;
        config.screenHeightDp = heightDp;
        config.smallestScreenWidthDp = smallestWidthDp;
        config.densityDpi = densityDpi;
        return config;
    }
}

