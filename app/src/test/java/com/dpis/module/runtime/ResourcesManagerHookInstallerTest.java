package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.res.Configuration;

import com.dpis.module.diagnostics.Coordinator;
import com.dpis.module.diagnostics.RuntimeEvents;
import com.dpis.module.diagnostics.RuntimeHotPathEvents;
import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.runtime.appprocess.ResourcesManagerHookInstaller;
import com.dpis.module.runtime.appprocess.WebApkRuntimeOwnerBridge;
import com.dpis.module.runtime.font.ResourcesFontScheduler;
import com.dpis.module.viewport.TargetViewportWidthResolver;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportConfigurationScope;
import com.dpis.module.viewport.ViewportOverride;
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge;
import com.dpis.module.viewport.ViewportRuntimeRecord;
import com.dpis.module.viewport.ViewportSourceSnapshot;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;
import com.dpis.module.viewport.VirtualDisplayOverride;
import com.dpis.module.viewport.VirtualDisplayState;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ResourcesManagerHookInstallerTest {
    private static final String PACKAGE_NAME = "com.example.target";

    @Before
    public void setUp() {
        ResourcesManagerHookInstaller.resetHotPathSamplerForTest();
    }

    @After
    public void tearDown() {
        RuntimeEvents.cancel();
        RuntimeHotPathEvents.resetForTest();
        TargetViewportWidthResolver.resetResolveCacheForTest();
        ViewportConfigurationScope.resetReflectionCacheForTest();
        VirtualDisplayState.set(null);
        ResourcesFontScheduler.clearForTest();
    }

    @Test
    public void nullConfigurationRecordsFeedbackDiagnosticSkip() {
        RuntimeEvents.start(PACKAGE_NAME, request());

        ResourcesManagerHookInstaller.applyResourceOverrides(
                null, new DpisConfigStore(new FakePrefs()), PACKAGE_NAME, "ResourcesManager");

        List<String> events = RuntimeEvents.stopSnapshot();
        assertTrue(events.stream().anyMatch(event ->
                event.contains("route=viewport")
                        && event.contains("stage=skipped")
                        && event.contains("resources_manager_config_override")
                        && event.contains("null_configuration")));
    }

    @Test
    public void restoresStableDensityWhenTargetConfigWasReDerivedFromStaleDensity() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(800, 1636, 800,
                216, 1080, 2209));
        Configuration config = new Configuration();
        config.densityDpi = 456;
        config.screenWidthDp = 800;
        config.screenHeightDp = 1636;
        config.smallestScreenWidthDp = 800;
        config.fontScale = 1.0f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, 800);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesManagerHookInstaller.applyResourceOverrides(config, store, PACKAGE_NAME,
                "ResourcesManager");

        assertEquals(216, config.densityDpi);
        assertEquals(216, VirtualDisplayState.get().densityDpi);
    }

    @Test
    public void relativeScaleDoesNotApplyTwiceAfterConfigurationOnlyHookPublishesRecord() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec(PACKAGE_NAME, ViewportTargetSpec.relativeScale(90000));
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT);
        Configuration initial = new Configuration();
        initial.screenWidthDp = 362;
        initial.screenHeightDp = 783;
        initial.smallestScreenWidthDp = 362;
        initial.densityDpi = 478;
        initial.fontScale = 1.0f;

        ResourcesManagerHookInstaller.applyResourceOverrides(
                initial, store, PACKAGE_NAME, "ResourcesManagerActivity");

        assertEquals(326, initial.screenWidthDp);
        assertEquals(705, initial.screenHeightDp);
        assertEquals(326, initial.smallestScreenWidthDp);
        assertEquals(531, initial.densityDpi);

        Configuration alreadyApplied = new Configuration();
        alreadyApplied.screenWidthDp = 326;
        alreadyApplied.screenHeightDp = 705;
        alreadyApplied.smallestScreenWidthDp = 326;
        alreadyApplied.densityDpi = 531;
        alreadyApplied.fontScale = 1.0f;

        ResourcesManagerHookInstaller.applyResourceOverrides(
                alreadyApplied, store, PACKAGE_NAME, "ResourcesRead");

        assertEquals(326, alreadyApplied.screenWidthDp);
        assertEquals(705, alreadyApplied.screenHeightDp);
        assertEquals(326, alreadyApplied.smallestScreenWidthDp);
        assertEquals(531, alreadyApplied.densityDpi);
    }

    @Test
    public void stalePortraitRecordDoesNotRewriteLandscapeConfigurationAsPortrait() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(200000);
        store.setTargetViewportSpec(PACKAGE_NAME, targetSpec);
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT);
        ViewportSourceSnapshot portraitSource = ViewportSourceSnapshot.systemDisplayInfo(
                462, 1001, 462, 374, 1080, 2340);
        VirtualDisplayState.publish(
                PACKAGE_NAME,
                targetSpec,
                portraitSource,
                new ViewportOverride.Result(924, 2002, 924, 187),
                new VirtualDisplayOverride.Result(924, 2002, 924, 187, 1080, 2340),
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);
        Configuration landscape = new Configuration();
        landscape.screenWidthDp = 1001;
        landscape.screenHeightDp = 462;
        landscape.smallestScreenWidthDp = 462;
        landscape.densityDpi = 374;
        landscape.fontScale = 1.0f;

        ResourcesManagerHookInstaller.applyResourceOverrides(
                landscape, store, PACKAGE_NAME, "ResourcesManagerActivity");

        assertEquals(2002, landscape.screenWidthDp);
        assertEquals(924, landscape.screenHeightDp);
        assertEquals(924, landscape.smallestScreenWidthDp);
        assertEquals(187, landscape.densityDpi);
    }

    @Test
    public void chromeActivityScopedConfigurationAppliesCompatViewport() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(200000);
        String packageName = WebApkRuntimeOwnerBridge.CHROME_PACKAGE;
        store.setTargetViewportSpec(packageName, targetSpec);
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);
        Configuration config = new Configuration();
        config.screenWidthDp = 1001;
        config.screenHeightDp = 462;
        config.smallestScreenWidthDp = 462;
        config.densityDpi = 374;
        config.fontScale = 1.15f;

        ResourcesManagerHookInstaller.applyResourceOverrides(
                config, store, packageName, "ResourcesManagerActivity");

        assertEquals(2002, config.screenWidthDp);
        assertEquals(924, config.screenHeightDp);
        assertEquals(924, config.smallestScreenWidthDp);
        assertEquals(187, config.densityDpi);
    }

    @Test
    public void chromeResourcesManagerConfigurationAppliesCompatViewport() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(200000);
        String packageName = WebApkRuntimeOwnerBridge.CHROME_PACKAGE;
        store.setTargetViewportSpec(packageName, targetSpec);
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);
        Configuration config = new Configuration();
        config.screenWidthDp = 1001;
        config.screenHeightDp = 462;
        config.smallestScreenWidthDp = 462;
        config.densityDpi = 374;
        config.fontScale = 1.15f;

        ResourcesManagerHookInstaller.applyResourceOverrides(
                config, store, packageName, "ResourcesManager");

        assertEquals(2002, config.screenWidthDp);
        assertEquals(924, config.screenHeightDp);
        assertEquals(924, config.smallestScreenWidthDp);
        assertEquals(187, config.densityDpi);
    }

    @Test
    public void chromeResourceCreationConfigurationAppliesCompatViewport() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(200000);
        String packageName = WebApkRuntimeOwnerBridge.CHROME_PACKAGE;
        store.setTargetViewportSpec(packageName, targetSpec);
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);
        Configuration config = new Configuration();
        config.screenWidthDp = 1001;
        config.screenHeightDp = 462;
        config.smallestScreenWidthDp = 462;
        config.densityDpi = 374;
        config.fontScale = 1.15f;

        ResourcesManagerHookInstaller.applyResourceOverrides(
                config, store, packageName,
                "ResourcesManagerCreate(createBaseTokenResources)");

        assertEquals(2002, config.screenWidthDp);
        assertEquals(924, config.screenHeightDp);
        assertEquals(924, config.smallestScreenWidthDp);
        assertEquals(187, config.densityDpi);
    }

    @Test
    public void fillsEmptyResourcesKeyOverrideFromGlobalConfiguration() {
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, 800);
        DpisConfigStore store = new DpisConfigStore(prefs);
        Configuration globalConfig = new Configuration();
        globalConfig.screenWidthDp = 360;
        globalConfig.screenHeightDp = 736;
        globalConfig.smallestScreenWidthDp = 360;
        globalConfig.densityDpi = 480;
        globalConfig.fontScale = 1.0f;
        FakeResourcesManager resourcesManager = new FakeResourcesManager(globalConfig);
        FakeResourcesKey key = new FakeResourcesKey();

        ResourcesManagerHookInstaller.maybeApplyKeyOverride(
                resourcesManager, key, store, PACKAGE_NAME, "createResourcesImpl");

        assertEquals(800, key.mOverrideConfiguration.screenWidthDp);
        assertEquals(1636, key.mOverrideConfiguration.screenHeightDp);
        assertEquals(800, key.mOverrideConfiguration.smallestScreenWidthDp);
        assertEquals(216, key.mOverrideConfiguration.densityDpi);
        assertEquals(0.0f, key.mOverrideConfiguration.fontScale, 0.0001f);
    }

    @Test
    public void keepsExistingResourcesKeyOverride() {
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, 800);
        DpisConfigStore store = new DpisConfigStore(prefs);
        Configuration globalConfig = new Configuration();
        globalConfig.screenWidthDp = 360;
        globalConfig.screenHeightDp = 736;
        globalConfig.smallestScreenWidthDp = 360;
        globalConfig.densityDpi = 480;
        FakeResourcesKey key = new FakeResourcesKey();
        key.mOverrideConfiguration.screenWidthDp = 500;
        key.mOverrideConfiguration.screenHeightDp = 1000;
        key.mOverrideConfiguration.smallestScreenWidthDp = 500;
        key.mOverrideConfiguration.densityDpi = 320;

        ResourcesManagerHookInstaller.maybeApplyKeyOverride(
                new FakeResourcesManager(globalConfig), key, store,
                PACKAGE_NAME, "createResourcesImpl");

        assertEquals(500, key.mOverrideConfiguration.screenWidthDp);
        assertEquals(1000, key.mOverrideConfiguration.screenHeightDp);
        assertEquals(500, key.mOverrideConfiguration.smallestScreenWidthDp);
        assertEquals(320, key.mOverrideConfiguration.densityDpi);
    }

    @Test
    public void replacesResourcesKeyOverrideThatMatchesBaseActivityConfiguration() {
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, 800);
        DpisConfigStore store = new DpisConfigStore(prefs);
        Configuration globalConfig = new Configuration();
        globalConfig.screenWidthDp = 360;
        globalConfig.screenHeightDp = 736;
        globalConfig.smallestScreenWidthDp = 360;
        globalConfig.densityDpi = 480;
        globalConfig.fontScale = 1.0f;
        FakeResourcesKey key = new FakeResourcesKey();
        key.mOverrideConfiguration.screenWidthDp = 360;
        key.mOverrideConfiguration.screenHeightDp = 736;
        key.mOverrideConfiguration.smallestScreenWidthDp = 360;
        key.mOverrideConfiguration.densityDpi = 480;

        ResourcesManagerHookInstaller.maybeApplyKeyOverride(
                new FakeResourcesManager(globalConfig), key, store,
                PACKAGE_NAME, "createResourcesImpl");

        assertEquals(800, key.mOverrideConfiguration.screenWidthDp);
        assertEquals(1636, key.mOverrideConfiguration.screenHeightDp);
        assertEquals(800, key.mOverrideConfiguration.smallestScreenWidthDp);
        assertEquals(216, key.mOverrideConfiguration.densityDpi);
    }

    @Test
    public void preservesExistingResourcesKeyFontOnlyOverride() {
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, 800);
        DpisConfigStore store = new DpisConfigStore(prefs);
        Configuration globalConfig = new Configuration();
        globalConfig.screenWidthDp = 360;
        globalConfig.screenHeightDp = 736;
        globalConfig.smallestScreenWidthDp = 360;
        globalConfig.densityDpi = 480;
        globalConfig.fontScale = 1.0f;
        FakeResourcesKey key = new FakeResourcesKey();
        key.mOverrideConfiguration.fontScale = 0.5f;

        ResourcesManagerHookInstaller.maybeApplyKeyOverride(
                new FakeResourcesManager(globalConfig), key, store,
                PACKAGE_NAME, "createResourcesImpl");

        assertEquals(800, key.mOverrideConfiguration.screenWidthDp);
        assertEquals(1636, key.mOverrideConfiguration.screenHeightDp);
        assertEquals(800, key.mOverrideConfiguration.smallestScreenWidthDp);
        assertEquals(216, key.mOverrideConfiguration.densityDpi);
        assertEquals(0.5f, key.mOverrideConfiguration.fontScale, 0.0001f);
    }

    @Test
    public void preservesWindowLikeResourcesKeyOverrideWhenDisplayRecordIsTaller() {
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(150000);
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("viewport." + PACKAGE_NAME + ".scale_milli_percent", 150000)
                .putString("viewport." + PACKAGE_NAME + ".type", ViewportTargetType.RELATIVE_SCALE)
                .putString("viewport." + PACKAGE_NAME + ".mode", ViewportApplyMode.COMPAT)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);
        ViewportRuntimeMarkerBridge.MarkerRecord marker =
                new ViewportRuntimeMarkerBridge.MarkerRecord(
                        PACKAGE_NAME,
                        targetSpec.fingerprint(),
                        "source",
                        540,
                        "result",
                        540,
                        1188,
                        540,
                        320,
                        ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
                        1000L);
        VirtualDisplayState.importMarker(
                PACKAGE_NAME,
                targetSpec,
                ViewportRuntimeMarkerBridge.ParseResult.hit(marker, 0L));
        Configuration windowConfig = new Configuration();
        windowConfig.screenWidthDp = 540;
        windowConfig.screenHeightDp = 960;
        windowConfig.smallestScreenWidthDp = 540;
        windowConfig.densityDpi = 320;
        windowConfig.fontScale = 1.0f;
        FakeResourcesKey key = new FakeResourcesKey();
        key.mOverrideConfiguration.screenWidthDp = 540;
        key.mOverrideConfiguration.screenHeightDp = 960;
        key.mOverrideConfiguration.smallestScreenWidthDp = 540;
        key.mOverrideConfiguration.densityDpi = 320;

        ResourcesManagerHookInstaller.maybeApplyKeyOverride(
                new FakeResourcesManager(windowConfig), key, store,
                PACKAGE_NAME, "createResourcesImpl");

        assertEquals(540, key.mOverrideConfiguration.screenWidthDp);
        assertEquals(960, key.mOverrideConfiguration.screenHeightDp);
        assertEquals(540, key.mOverrideConfiguration.smallestScreenWidthDp);
        assertEquals(320, key.mOverrideConfiguration.densityDpi);
    }

    @Test
    public void debugResourcesManagerKeyDisablePropertyIsPackageScoped() throws Exception {
        String source = readSource("src/main/java/com/dpis/module/runtime/appprocess/ResourcesManagerHookInstaller.kt");

        assertTrue(source.contains(
                "debug.dpis.viewport.disable_resources_manager_key_package"));
        assertTrue(source.contains("DebugPackageOverride.matches("));
    }

    @Test
    public void targetMatchingSmallestWidthDoesNotRewriteWindowConfiguration() {
        Configuration config = new Configuration();
        config.screenWidthDp = 448;
        config.screenHeightDp = 970;
        config.smallestScreenWidthDp = 411;
        config.densityDpi = 420;
        config.fontScale = 1.0f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, 411);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesManagerHookInstaller.applyResourceOverrides(config, store, PACKAGE_NAME,
                "ResourcesManager");

        assertEquals(448, config.screenWidthDp);
        assertEquals(970, config.screenHeightDp);
        assertEquals(411, config.smallestScreenWidthDp);
        assertEquals(420, config.densityDpi);
        assertNull(VirtualDisplayState.get());
    }

    @Test
    public void unknownDensityDoesNotPublishMdpiVirtualDisplayState() {
        Configuration config = new Configuration();
        config.screenWidthDp = 360;
        config.screenHeightDp = 736;
        config.smallestScreenWidthDp = 360;
        config.densityDpi = 0;
        config.fontScale = 1.0f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, 360);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesManagerHookInstaller.applyResourceOverrides(config, store, PACKAGE_NAME,
                "ResourcesManager");

        assertEquals(360, config.screenWidthDp);
        assertEquals(736, config.screenHeightDp);
        assertEquals(360, config.smallestScreenWidthDp);
        assertEquals(0, config.densityDpi);
        assertNull(VirtualDisplayState.get());
    }

    @Test
    public void doesNotUseViewportDpAsPixelsWhenMetricsAreUnavailable() {
        Configuration config = new Configuration();
        config.screenWidthDp = 360;
        config.screenHeightDp = 736;
        config.smallestScreenWidthDp = 360;
        config.densityDpi = 480;
        config.fontScale = 1.0f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, 500);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesManagerHookInstaller.applyResourceOverrides(config, store, PACKAGE_NAME,
                "ResourcesManager");

        assertEquals(500, config.smallestScreenWidthDp);
        assertNull(VirtualDisplayState.get());
    }

    private record FakeResourcesManager(Configuration configuration) {

        @Override
        @SuppressWarnings("unused")
        public Configuration configuration() {
                return configuration;
            }

        public Configuration getConfiguration() {
            return configuration;
        }
        }

    private static final class FakeResourcesKey {
        @SuppressWarnings("unused")
        private final Configuration mOverrideConfiguration = new Configuration();
    }

    private static void putCompatViewport(FakePrefs prefs, int widthDp) {
        prefs.edit()
                .putInt("viewport." + PACKAGE_NAME + ".width_dp", widthDp)
                .putString("viewport." + PACKAGE_NAME + ".mode", ViewportApplyMode.COMPAT)
                .commit();
    }

    private static Coordinator.Request request() {
        return new Coordinator.Request(
                PACKAGE_NAME,
                "Target",
                "1",
                true,
                true,
                true,
                false,
                ViewportTargetSpec.relativeScale(90000),
                ViewportApplyMode.COMPAT,
                null,
                FontApplyMode.OFF,
                null,
                null,
                null);
    }

    private static String readSource(String relativePath) throws Exception {
        return new String(
                java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(relativePath)),
                java.nio.charset.StandardCharsets.UTF_8);
    }
}

