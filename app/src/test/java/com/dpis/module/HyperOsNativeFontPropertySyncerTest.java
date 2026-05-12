package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

public class HyperOsNativeFontPropertySyncerTest {
    @Test
    public void shellQuoteEscapesSingleQuotes() {
        assertEquals("'debug.dpis.forcefont.a55b5fe1'",
                HyperOsNativeFontPropertySyncer.shellQuoteForTest("debug.dpis.forcefont.a55b5fe1"));
        assertEquals("'a'\\''b'", HyperOsNativeFontPropertySyncer.shellQuoteForTest("a'b"));
        assertEquals("''", HyperOsNativeFontPropertySyncer.shellQuoteForTest(""));
    }
    @Test
    public void buildPublishCommandSetsForceFontPropertyOnly() {
        assertEquals("setprop 'debug.dpis.forcefont.a55b5fe1' '300'",
                HyperOsNativeFontPropertySyncer.buildPublishCommandForTest(
                        "debug.dpis.forcefont.a55b5fe1", 300));
    }

    @Test
    public void compatFontCommandUsesSeparateProperty() {
        assertEquals("setprop 'debug.dpis.compatfont.a55b5fe1' '200'",
                CompatFontPropertySyncer.buildSetCommandForTest(
                        "debug.dpis.compatfont.a55b5fe1", 200));
    }

    @Test
    public void preservesCompatForceFontForActiveFieldRewriteTarget() {
        FakePrefs preferences = new FakePrefs();
        preferences.edit()
                .putStringSet(DpiConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.miui.gallery")))
                .putInt("font.com.miui.gallery.scale_percent", 200)
                .putString("font.com.miui.gallery.mode", FontApplyMode.FIELD_REWRITE)
                .commit();
        DpiConfigStore store = new DpiConfigStore(preferences);

        assertTrue(HyperOsNativeFontPropertySyncer.shouldPreserveCompatForceFontForTest(
                store, "com.miui.gallery"));
    }

    @Test
    public void doesNotPreserveCompatForceFontForSystemEmulationOrDisabledTarget() {
        FakePrefs systemEmulation = new FakePrefs();
        systemEmulation.edit()
                .putStringSet(DpiConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.miui.gallery")))
                .putInt("font.com.miui.gallery.scale_percent", 200)
                .putString("font.com.miui.gallery.mode", FontApplyMode.SYSTEM_EMULATION)
                .commit();
        assertFalse(HyperOsNativeFontPropertySyncer.shouldPreserveCompatForceFontForTest(
                new DpiConfigStore(systemEmulation), "com.miui.gallery"));

        FakePrefs disabled = new FakePrefs();
        disabled.edit()
                .putStringSet(DpiConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.miui.gallery")))
                .putInt("font.com.miui.gallery.scale_percent", 200)
                .putString("font.com.miui.gallery.mode", FontApplyMode.FIELD_REWRITE)
                .putBoolean("target.com.miui.gallery.dpis_enabled", false)
                .commit();
        assertFalse(HyperOsNativeFontPropertySyncer.shouldPreserveCompatForceFontForTest(
                new DpiConfigStore(disabled), "com.miui.gallery"));
    }

    @Test
    public void recoveryPublishesFieldRewriteWithoutNativeHookGate() {
        FakePrefs preferences = new FakePrefs();
        preferences.edit()
                .putBoolean("font.hyperos_flutter_hook_enabled", false)
                .commit();
        DpiConfigStore store = new DpiConfigStore(preferences);

        assertTrue(HyperOsNativeFontPropertySyncer.shouldPublishForceFontOnRecoveryForTest(
                store, FontApplyMode.FIELD_REWRITE));
        assertFalse(HyperOsNativeFontPropertySyncer.shouldPublishForceFontOnRecoveryForTest(
                store, FontApplyMode.OFF));
        assertFalse(HyperOsNativeFontPropertySyncer.shouldPublishForceFontOnRecoveryForTest(
                store, FontApplyMode.SYSTEM_EMULATION));
    }
}
