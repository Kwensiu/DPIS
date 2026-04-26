package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HyperOsFlutterFontHookConfigTest {
    @Test
    public void experimentalHookDefaultsOffAndPersists() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        assertFalse(store.isHyperOsFlutterFontHookEnabled());

        assertTrue(store.setHyperOsFlutterFontHookEnabled(true));
        assertTrue(store.isHyperOsFlutterFontHookEnabled());
    }

    @Test
    public void bridgePropertyNameUsesStablePackageHash() {
        assertEquals("debug.dpis.font.a55b5fe1",
                HyperOsFlutterFontBridge.propertyNameForPackage("com.miui.gallery"));
    }

    @Test
    public void bridgeForcePropertyNameUsesStablePackageHash() {
        assertEquals("debug.dpis.forcefont.a55b5fe1",
                HyperOsFlutterFontBridge.forcePropertyNameForPackage("com.miui.gallery"));
    }

    @Test
    public void rustProcessEnvironmentIncludesFontTarget() {
        String envs = HyperOsRustProcessHookInstaller.appendEnvironmentForTest(
                "", "com.miui.gallery", 300,
                "/data/app/MIUIGallery/lib/arm64/libapp_gallery.so");

        assertEquals("DPIS_PACKAGE=com.miui.gallery --envs=DPIS_FONT_SCALE_PERCENT=300"
                        + " --envs=DPIS_RUST_BINARY=/data/app/MIUIGallery/lib/arm64/libapp_gallery.so"
                        + " --cold-boot-speed",
                envs);
    }

    @Test
    public void bridgeDoesNotClearRustTargetWhenUiHookSwitchIsOff() throws Exception {
        java.lang.reflect.Method method = HyperOsFlutterFontBridge.class.getDeclaredMethod(
                "shouldClearOnPublishTargetSkipForTest", String.class, PerAppDisplayConfig.class);
        method.setAccessible(true);
        PerAppDisplayConfig config = new PerAppDisplayConfig("com.miui.gallery", null,
                300, FontApplyMode.SYSTEM_EMULATION, false);

        boolean shouldClear = (boolean) method.invoke(null, "com.miui.gallery", config);

        assertFalse(shouldClear);
    }
}
