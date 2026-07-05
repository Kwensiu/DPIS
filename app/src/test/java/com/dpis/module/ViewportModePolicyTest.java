package com.dpis.module;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportModePolicy;
import com.dpis.module.viewport.ViewportTargetResolution;
import com.dpis.module.viewport.ViewportTargetSpec;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ViewportModePolicyTest {
    @Test
    public void systemModeTurnsOffInAppProcessWhenSystemHookOff() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setSystemServerHooksEnabled(false);
        store.setTargetViewportWidthDp("com.example.target", 360);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.SYSTEM_EMULATION);

        String mode = ViewportModePolicy.resolve(store, "com.example.target");

        assertEquals(ViewportApplyMode.OFF, mode);
        assertFalse(ViewportModePolicy.shouldApplyConfigurationOverride(store, "com.example.target"));
    }

    @Test
    public void systemHookOnKeepsSystemModeOutOfAppProcessConfigurationOverride() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setSystemServerHooksEnabled(true);
        store.setTargetViewportWidthDp("com.example.target", 360);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.SYSTEM_EMULATION);

        String mode = ViewportModePolicy.resolve(store, "com.example.target");

        assertEquals(ViewportApplyMode.SYSTEM_EMULATION, mode);
        assertFalse(ViewportModePolicy.shouldApplyConfigurationOverride(store, "com.example.target"));
    }

    @Test
    public void autoUsesSystemModeOutOfAppProcessConfigurationOverrideWhenSystemHookOn() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setSystemServerHooksEnabled(true);
        store.setTargetViewportSpec("com.example.target", ViewportTargetSpec.absoluteDp(360));
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.AUTO);

        String mode = ViewportModePolicy.resolve(store, "com.example.target");

        assertEquals(ViewportApplyMode.SYSTEM, mode);
        assertFalse(ViewportModePolicy.shouldApplyConfigurationOverride(store, "com.example.target"));
    }

    @Test
    public void relativeScaleDoesNotForceConfigurationOverrideInSystemRoute() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setSystemServerHooksEnabled(true);
        store.setTargetViewportSpec("com.example.target", ViewportTargetSpec.relativeScale(150000));
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.AUTO);

        String mode = ViewportModePolicy.resolve(store, "com.example.target");

        assertEquals(ViewportApplyMode.SYSTEM, mode);
        assertFalse(ViewportModePolicy.shouldApplyConfigurationOverride(store, "com.example.target"));
    }

    @Test
    public void autoAppliesGuardedConfigurationFallbackWhenSystemRouteStillNeedsViewportUpdate() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        ViewportTargetSpec spec = ViewportTargetSpec.relativeScale(150000);
        store.setSystemServerHooksEnabled(true);
        store.setTargetViewportSpec("com.example.target", spec);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.AUTO);
        ViewportTargetResolution resolution =
                ViewportTargetResolution.resolved(spec, 540, null, "system-marker");

        assertTrue(ViewportModePolicy.shouldApplyConfigurationOverride(
                store, "com.example.target", resolution, true));
    }

    @Test
    public void explicitSystemDoesNotApplyGuardedConfigurationFallbackInAppProcess() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        ViewportTargetSpec spec = ViewportTargetSpec.absoluteDp(300);
        store.setSystemServerHooksEnabled(true);
        store.setTargetViewportSpec("com.example.target", spec);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.SYSTEM);
        ViewportTargetResolution resolution =
                ViewportTargetResolution.resolved(spec, 300, null, "system-marker");

        assertFalse(ViewportModePolicy.shouldApplyConfigurationOverride(
                store, "com.example.target", resolution, true));
    }

    @Test
    public void autoDoesNotApplyGuardedConfigurationFallbackWhenConfigurationAlreadyMatches() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        ViewportTargetSpec spec = ViewportTargetSpec.relativeScale(150000);
        store.setSystemServerHooksEnabled(true);
        store.setTargetViewportSpec("com.example.target", spec);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.AUTO);
        ViewportTargetResolution resolution =
                ViewportTargetResolution.resolved(spec, 540, null, "derived");

        assertFalse(ViewportModePolicy.shouldApplyConfigurationOverride(
                store, "com.example.target", resolution, false));
    }

    @Test
    public void autoFallsBackToCompatWithoutConfigurationOverrideWhenSystemHookOff() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setSystemServerHooksEnabled(false);
        store.setTargetViewportSpec("com.example.target", ViewportTargetSpec.absoluteDp(360));
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.AUTO);

        String mode = ViewportModePolicy.resolve(store, "com.example.target");

        assertEquals(ViewportApplyMode.COMPAT, mode);
        assertTrue(ViewportModePolicy.shouldApplyConfigurationOverride(store, "com.example.target"));
    }

    @Test
    public void webApkOwnerAutoKeepsAppProcessConfigurationOverrideWhenSystemHookOn() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        String packageName = "org.chromium.webapk.ac19cf34f94565db5_v2";
        store.setSystemServerHooksEnabled(true);
        store.setTargetDpisEnabled(packageName, true);
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(150000));
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.AUTO);

        assertEquals(ViewportApplyMode.SYSTEM, ViewportModePolicy.resolve(store, packageName));
        assertTrue(ViewportModePolicy.shouldApplyConfigurationOverride(store, packageName));
    }

    @Test
    public void fieldRewriteAppliesConfigurationOverrideInAppProcess() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setSystemServerHooksEnabled(false);
        store.setTargetViewportWidthDp("com.example.target", 360);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.FIELD_REWRITE);

        String mode = ViewportModePolicy.resolve(store, "com.example.target");

        assertEquals(ViewportApplyMode.FIELD_REWRITE, mode);
        assertTrue(ViewportModePolicy.shouldApplyConfigurationOverride(store, "com.example.target"));
    }
}
