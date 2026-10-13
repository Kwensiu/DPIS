package com.dpis.module.runtime.systemserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.res.Configuration;

import com.dpis.module.config.PerAppDisplayConfig;
import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.hooks.HookDomainOverride;
import com.dpis.module.viewport.PerAppDisplayEnvironment;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportOverride;
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge;
import com.dpis.module.viewport.ViewportSourceSnapshot;
import com.dpis.module.viewport.ViewportTargetSpec;

import org.junit.Test;

public class ViewportSelectionTest {
    private static PerAppDisplayEnvironment launch(String packageName,
                                                   Configuration configuration,
                                                   PerAppDisplayConfig config) {
        return HookInstaller
                .resolveLaunchViewportEnvironment(
                        packageName, configuration, 1080, 2376, config);
    }

    @Test
    public void keepsPreEnvironmentWhenPostEnvironmentMissing() {
        PerAppDisplayEnvironment pre = new PerAppDisplayEnvironment(320, 704, 320, 540, 1080, 2376);

        assertSame(pre, HookCallback.chooseEffectiveEnvironment(pre, null));
    }

    private static PerAppDisplayConfig launchConfig(ViewportTargetSpec spec) {
        return new PerAppDisplayConfig(
                "com.example.launch",
                spec,
                ViewportApplyMode.AUTO,
                null,
                FontApplyMode.OFF,
                false,
                HookDomainOverride.automatic());
    }

    @Test
    public void prefersPostEnvironmentWhenAvailable() {
        PerAppDisplayEnvironment pre = new PerAppDisplayEnvironment(320, 704, 320, 540, 1080, 2376);
        PerAppDisplayEnvironment post = new PerAppDisplayEnvironment(360, 736, 360, 480, 1080, 2376);

        assertSame(post, HookCallback.chooseEffectiveEnvironment(pre, post));
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

        assertTrue(RelativeViewport
                .isAlreadyApplied(
                        configuration,
                        ViewportSourceSnapshot.SCOPE_DISPLAY,
                        ViewportRuntimeMarkerBridge.ParseResult.hit(record, 10L)));
    }

    private static Configuration panelConfiguration(int densityDpi) {
        Configuration configuration = new Configuration();
        configuration.densityDpi = densityDpi;
        configuration.screenWidthDp = Math.round(1080 * 160f / densityDpi);
        configuration.screenHeightDp = Math.round(2376 * 160f / densityDpi);
        configuration.smallestScreenWidthDp = Math.min(
                configuration.screenWidthDp, configuration.screenHeightDp);
        return configuration;
    }

    private static Configuration configurationFrom(PerAppDisplayEnvironment environment) {
        Configuration configuration = new Configuration();
        configuration.screenWidthDp = environment.widthDp;
        configuration.screenHeightDp = environment.heightDp;
        configuration.smallestScreenWidthDp = environment.smallestWidthDp;
        configuration.densityDpi = environment.densityDpi;
        return configuration;
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

        assertFalse(RelativeViewport
                .isAlreadyApplied(
                        configuration,
                        ViewportSourceSnapshot.SCOPE_DISPLAY,
                        ViewportRuntimeMarkerBridge.ParseResult.hit(record, 10L)));
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

        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry(null, fontOnlyEmulation));
        assertNull(ConfigSelection.selectConfigForSystemServerEntry(null, fontOnlyRewrite));
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry(null, viewport));
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry(null, autoViewport));
        assertNull(ConfigSelection.selectConfigForSystemServerEntry(null, compatViewport));
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry(null, relativeViewport));
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry(null, explicitSystemRelativeViewport));
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry(null, relativeViewportWithFont));
    }

    @Test
    public void secondLaunchPassKeepsAbsoluteWidthWhenPixelsDoNotDivideEvenly() {
        ViewportRuntimeMarkerBridge.clearForTest();
        PerAppDisplayConfig config = launchConfig(ViewportTargetSpec.absoluteDp(320));
        Configuration first = panelConfiguration(420);
        PerAppDisplayEnvironment once = launch(
                "com.example.absolute", first, config);
        PerAppDisplayEnvironment twice = launch(
                "com.example.absolute", configurationFrom(once), config);

        assertEquals(320, once.smallestWidthDp);
        assertEquals(once.smallestWidthDp, twice.smallestWidthDp);
        assertEquals(once.widthDp, twice.widthDp);
        assertEquals(once.heightDp, twice.heightDp);
        assertEquals(once.densityDpi, twice.densityDpi);
    }

    @Test
    public void secondLaunchPassDoesNotCompoundRelativeScale() {
        ViewportRuntimeMarkerBridge.clearForTest();
        PerAppDisplayConfig config = launchConfig(ViewportTargetSpec.relativeScale(120000));
        Configuration first = panelConfiguration(420);
        PerAppDisplayEnvironment once = launch(
                "com.example.relative", first, config);
        PerAppDisplayEnvironment twice = launch(
                "com.example.relative", configurationFrom(once), config);

        assertTrue(once.smallestWidthDp > first.smallestScreenWidthDp);
        assertEquals(once.smallestWidthDp, twice.smallestWidthDp);
        assertEquals(once.widthDp, twice.widthDp);
        assertEquals(once.heightDp, twice.heightDp);
        assertEquals(once.densityDpi, twice.densityDpi);
    }
}

