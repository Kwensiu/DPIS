package com.dpis.module;

import com.dpis.module.hooks.HookDomainOverride;

import android.content.res.Configuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SystemServerDisplayEnvironmentInstallerEnvironmentSelectionTest {
    @Test
    public void keepsPreEnvironmentWhenPostEnvironmentMissing() {
        String selected = SystemServerDisplayEnvironmentInstaller.selectEnvironmentSourceForTest(
                true,
                false
        );
        assertEquals("pre", selected);
    }

    @Test
    public void prefersPostEnvironmentWhenAvailable() {
        String selected = SystemServerDisplayEnvironmentInstaller.selectEnvironmentSourceForTest(
                true,
                true
        );
        assertEquals("post", selected);
    }

    @Test
    public void systemServerUsesViewportOrFontEmulationConfig() {
        PerAppDisplayConfig fontOnlyEmulation = new PerAppDisplayConfig(
                "com.example.target",
                null,
                120,
                FontApplyMode.SYSTEM_EMULATION
        );
        PerAppDisplayConfig fontOnlyRewrite = new PerAppDisplayConfig(
                "com.example.target",
                null,
                120,
                FontApplyMode.FIELD_REWRITE
        );
        PerAppDisplayConfig viewport = new PerAppDisplayConfig(
                "com.example.target",
                360
        );
        PerAppDisplayConfig autoViewport = new PerAppDisplayConfig(
                "com.example.target",
                ViewportTargetSpec.absoluteDp(360),
                ViewportApplyMode.AUTO,
                null,
                FontApplyMode.OFF,
                false,
                HookDomainOverride.automatic()
        );
        PerAppDisplayConfig compatViewport = new PerAppDisplayConfig(
                "com.example.target",
                ViewportTargetSpec.absoluteDp(360),
                ViewportApplyMode.COMPAT,
                null,
                FontApplyMode.OFF,
                false,
                HookDomainOverride.automatic()
        );
        PerAppDisplayConfig relativeViewport = new PerAppDisplayConfig(
                "com.example.target",
                ViewportTargetSpec.relativeScale(150000),
                ViewportApplyMode.AUTO,
                null,
                FontApplyMode.OFF,
                false,
                HookDomainOverride.automatic()
        );
        PerAppDisplayConfig explicitSystemRelativeViewport = new PerAppDisplayConfig(
                "com.example.target",
                ViewportTargetSpec.relativeScale(150000),
                ViewportApplyMode.SYSTEM,
                null,
                FontApplyMode.OFF,
                false,
                HookDomainOverride.automatic()
        );
        PerAppDisplayConfig relativeViewportWithFont = new PerAppDisplayConfig(
                "com.example.target",
                ViewportTargetSpec.relativeScale(150000),
                ViewportApplyMode.AUTO,
                120,
                FontApplyMode.SYSTEM_EMULATION,
                false,
                HookDomainOverride.automatic()
        );

        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerForTest(fontOnlyEmulation));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerForTest(fontOnlyRewrite));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerForTest(viewport));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerForTest(autoViewport));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerForTest(compatViewport));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerForTest(relativeViewport));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerForTest(explicitSystemRelativeViewport));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerForTest(relativeViewportWithFont));
    }

    @Test
    public void recognizesRelativeScaleMarkerResultAsAlreadyApplied() {
        Configuration configuration = new Configuration();
        configuration.screenWidthDp = 1215;
        configuration.screenHeightDp = 2484;
        configuration.smallestScreenWidthDp = 1215;
        configuration.densityDpi = 142;
        ViewportRuntimeMarkerBridge.MarkerRecord record =
                ViewportRuntimeMarkerBridge.createRecord(
                        "com.tencent.mm",
                        ViewportTargetSpec.relativeScale(150000),
                        1215,
                        ViewportSourceSnapshot.systemDisplayInfo(
                                810,
                                1656,
                                810,
                                213,
                                1080,
                                2208),
                        new ViewportOverride.Result(1215, 2484, 1215, 142),
                        "s",
                        1000L);

        assertTrue(SystemServerDisplayEnvironmentInstaller
                .isAlreadyAppliedRelativeScaleMarkerForTest(
                        configuration,
                        ViewportSourceSnapshot.SCOPE_DISPLAY,
                        ViewportRuntimeMarkerBridge.ParseResult.hit(record, 10L)));
    }

    @Test
    public void rejectsRelativeScaleMarkerWhenSourceIsNotResult() {
        Configuration configuration = new Configuration();
        configuration.screenWidthDp = 810;
        configuration.screenHeightDp = 1656;
        configuration.smallestScreenWidthDp = 810;
        configuration.densityDpi = 213;
        ViewportRuntimeMarkerBridge.MarkerRecord record =
                ViewportRuntimeMarkerBridge.createRecord(
                        "com.tencent.mm",
                        ViewportTargetSpec.relativeScale(150000),
                        1215,
                        ViewportSourceSnapshot.systemDisplayInfo(
                                810,
                                1656,
                                810,
                                213,
                                1080,
                                2208),
                        new ViewportOverride.Result(1215, 2484, 1215, 142),
                        "s",
                        1000L);

        assertFalse(SystemServerDisplayEnvironmentInstaller
                .isAlreadyAppliedRelativeScaleMarkerForTest(
                        configuration,
                        ViewportSourceSnapshot.SCOPE_DISPLAY,
                        ViewportRuntimeMarkerBridge.ParseResult.hit(record, 10L)));
    }
}

