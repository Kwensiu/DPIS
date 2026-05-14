package com.dpis.module;

import android.content.res.Configuration;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ResourcesManagerHookInstallerTest {
    @After
    public void tearDown() {
        VirtualDisplayState.set(null);
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
        prefs.edit().putInt("viewport.com.example.target.width_dp", 800).commit();
        DpiConfigStore store = new DpiConfigStore(prefs);

        ResourcesManagerHookInstaller.applyResourceOverrides(config, store, "com.example.target",
                "ResourcesManager");

        assertEquals(216, config.densityDpi);
        assertEquals(216, VirtualDisplayState.get().densityDpi);
    }

    @Test
    public void fillsEmptyResourcesKeyOverrideFromGlobalConfiguration() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("viewport.com.example.target.width_dp", 800)
                .putString("viewport.com.example.target.mode", ViewportApplyMode.FIELD_REWRITE)
                .commit();
        DpiConfigStore store = new DpiConfigStore(prefs);
        Configuration globalConfig = new Configuration();
        globalConfig.screenWidthDp = 360;
        globalConfig.screenHeightDp = 736;
        globalConfig.smallestScreenWidthDp = 360;
        globalConfig.densityDpi = 480;
        globalConfig.fontScale = 1.0f;
        FakeResourcesManager resourcesManager = new FakeResourcesManager(globalConfig);
        FakeResourcesKey key = new FakeResourcesKey();

        ResourcesManagerHookInstaller.maybeApplyKeyOverride(
                resourcesManager, key, store, "com.example.target", "createResourcesImpl");

        assertEquals(800, key.mOverrideConfiguration.screenWidthDp);
        assertEquals(1636, key.mOverrideConfiguration.screenHeightDp);
        assertEquals(800, key.mOverrideConfiguration.smallestScreenWidthDp);
        assertEquals(216, key.mOverrideConfiguration.densityDpi);
        assertEquals(0.0f, key.mOverrideConfiguration.fontScale, 0.0001f);
    }

    @Test
    public void keepsExistingResourcesKeyOverride() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("viewport.com.example.target.width_dp", 800)
                .putString("viewport.com.example.target.mode", ViewportApplyMode.FIELD_REWRITE)
                .commit();
        DpiConfigStore store = new DpiConfigStore(prefs);
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
                "com.example.target", "createResourcesImpl");

        assertEquals(500, key.mOverrideConfiguration.screenWidthDp);
        assertEquals(1000, key.mOverrideConfiguration.screenHeightDp);
        assertEquals(500, key.mOverrideConfiguration.smallestScreenWidthDp);
        assertEquals(320, key.mOverrideConfiguration.densityDpi);
    }

    @Test
    public void preservesExistingResourcesKeyFontOnlyOverride() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("viewport.com.example.target.width_dp", 800)
                .putString("viewport.com.example.target.mode", ViewportApplyMode.FIELD_REWRITE)
                .commit();
        DpiConfigStore store = new DpiConfigStore(prefs);
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
                "com.example.target", "createResourcesImpl");

        assertEquals(800, key.mOverrideConfiguration.screenWidthDp);
        assertEquals(1636, key.mOverrideConfiguration.screenHeightDp);
        assertEquals(800, key.mOverrideConfiguration.smallestScreenWidthDp);
        assertEquals(216, key.mOverrideConfiguration.densityDpi);
        assertEquals(0.5f, key.mOverrideConfiguration.fontScale, 0.0001f);
    }

    private static final class FakeResourcesManager {
        private final Configuration configuration;

        private FakeResourcesManager(Configuration configuration) {
            this.configuration = configuration;
        }

        @SuppressWarnings("unused")
        private Configuration getConfiguration() {
            return configuration;
        }
    }

    private static final class FakeResourcesKey {
        @SuppressWarnings("unused")
        private final Configuration mOverrideConfiguration = new Configuration();
    }
}
