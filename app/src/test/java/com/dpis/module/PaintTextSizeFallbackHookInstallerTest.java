package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PaintTextSizeFallbackHookInstallerTest {
    @Test
    public void returnsFactorForFieldRewriteMode() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        store.setTargetFontScalePercent("com.max.xiaoheihe", 150);
        store.setTargetFontApplyMode("com.max.xiaoheihe", FontApplyMode.FIELD_REWRITE);

        float factor = PaintTextSizeFallbackHookInstaller.resolveFieldRewriteFactor(
                store, "com.max.xiaoheihe");

        assertEquals(1.5f, factor, 0.0001f);
    }

    @Test
    public void returnsIdentityFactorForSystemEmulationMode() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        store.setTargetFontScalePercent("com.max.xiaoheihe", 150);
        store.setTargetFontApplyMode("com.max.xiaoheihe", FontApplyMode.SYSTEM_EMULATION);

        float factor = PaintTextSizeFallbackHookInstaller.resolveFieldRewriteFactor(
                store, "com.max.xiaoheihe");

        assertEquals(1.0f, factor, 0.0001f);
    }
}
