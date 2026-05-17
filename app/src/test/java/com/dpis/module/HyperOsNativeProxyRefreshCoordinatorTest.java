package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;

import org.junit.Test;

public class HyperOsNativeProxyRefreshCoordinatorTest {
    @Test
    public void coordinatorDocumentsDormantAutomaticRefreshState() throws Exception {
        String source = read("src/main/java/com/dpis/module/HyperOsNativeProxyRefreshCoordinator.java");

        assertTrue(source.contains("Dormant helper"));
        assertTrue(source.contains("automatic startup/package-update proxy refresh is intentionally disabled"));
        assertTrue(source.contains("do not wire it"));
    }

    @Test
    public void refreshesOnlyEnabledConfiguredFontTargetsWhenHookEnabled() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        assertTrue(store.setFlutterFontHookEnabled(true));
        assertTrue(store.setHyperOsFlutterFontHookEnabled(true));
        assertTrue(store.setTargetFontScalePercent("com.miui.gallery", 180));
        assertTrue(store.setTargetFontApplyMode("com.miui.gallery", FontApplyMode.SYSTEM_EMULATION));
        assertTrue(store.setTargetFontScalePercent("com.miui.weather2", 200));
        assertTrue(store.setTargetFontApplyMode("com.miui.weather2", FontApplyMode.FIELD_REWRITE));
        assertTrue(store.setTargetViewportWidthDp("com.example.viewport", 500));
        assertTrue(store.setTargetFontScalePercent("com.example.disabled", 160));
        assertTrue(store.setTargetFontApplyMode("com.example.disabled", FontApplyMode.FIELD_REWRITE));
        assertTrue(store.setTargetDpisEnabled("com.example.disabled", false));

        LinkedHashSet<String> packages = HyperOsNativeProxyRefreshCoordinator
                .collectRefreshPackagesForTest(store);

        assertTrue(packages.contains("com.miui.gallery"));
        assertTrue(packages.contains("com.miui.weather2"));
        assertFalse(packages.contains("com.example.viewport"));
        assertFalse(packages.contains("com.example.disabled"));
    }

    @Test
    public void refreshSkipsWhenHookDisabled() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        assertTrue(store.setFlutterFontHookEnabled(true));
        assertTrue(store.setTargetFontScalePercent("com.miui.gallery", 180));
        assertTrue(store.setTargetFontApplyMode("com.miui.gallery", FontApplyMode.SYSTEM_EMULATION));

        assertTrue(HyperOsNativeProxyRefreshCoordinator
                .collectRefreshPackagesForTest(store).isEmpty());
    }

    @Test
    public void refreshSkipsWhenFlutterMasterSwitchDisabled() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        assertTrue(store.setFlutterFontHookEnabled(false));
        assertTrue(store.setHyperOsFlutterFontHookEnabled(true));
        assertTrue(store.setTargetFontScalePercent("com.miui.gallery", 180));
        assertTrue(store.setTargetFontApplyMode("com.miui.gallery", FontApplyMode.SYSTEM_EMULATION));

        assertTrue(HyperOsNativeProxyRefreshCoordinator
                .collectRefreshPackagesForTest(store).isEmpty());
    }

    private static String read(String relativePath) throws Exception {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
