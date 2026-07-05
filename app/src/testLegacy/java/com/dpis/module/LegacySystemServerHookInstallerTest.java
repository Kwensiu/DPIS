package com.dpis.module;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import com.dpis.module.hooks.HookDomainOverride;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class LegacySystemServerHookInstallerTest {
    @Test
    public void systemModeLaunchActivityItemAppliesViewportConfiguration() {
        String packageName = "com.android.chrome";
        Configuration configuration = configuration(462, 1001, 462, 374, 1.15f);

        LegacySystemServerHookInstaller.applyLaunchActivityItemArgs(
                sourceFor(packageName, ViewportApplyMode.SYSTEM),
                new Object[]{activityInfo(packageName), configuration});

        assertEquals(924, configuration.screenWidthDp);
        assertEquals(2002, configuration.screenHeightDp);
        assertEquals(924, configuration.smallestScreenWidthDp);
        assertEquals(187, configuration.densityDpi);
    }

    @Test
    public void compatModeLaunchActivityItemDoesNotApplyViewportConfiguration() {
        String packageName = "com.android.chrome";
        Configuration configuration = configuration(462, 1001, 462, 374, 1.15f);

        LegacySystemServerHookInstaller.applyLaunchActivityItemArgs(
                sourceFor(packageName, ViewportApplyMode.COMPAT),
                new Object[]{activityInfo(packageName), configuration});

        assertEquals(462, configuration.screenWidthDp);
        assertEquals(1001, configuration.screenHeightDp);
        assertEquals(462, configuration.smallestScreenWidthDp);
        assertEquals(374, configuration.densityDpi);
    }

    @Test
    public void systemModeLaunchActivityItemObjectAppliesViewportConfiguration() {
        String packageName = "com.android.chrome";
        LaunchActivityItemStub item = new LaunchActivityItemStub(
                activityInfo(packageName),
                configuration(462, 1001, 462, 374, 1.15f),
                configuration(462, 1001, 462, 374, 1.15f));

        LegacySystemServerHookInstaller.applyLaunchActivityItemObject(
                sourceFor(packageName, ViewportApplyMode.SYSTEM),
                item);

        assertEquals(924, item.mCurConfig.screenWidthDp);
        assertEquals(2002, item.mCurConfig.screenHeightDp);
        assertEquals(924, item.mCurConfig.smallestScreenWidthDp);
        assertEquals(187, item.mCurConfig.densityDpi);
        assertEquals(924, item.mOverrideConfig.screenWidthDp);
        assertEquals(2002, item.mOverrideConfig.screenHeightDp);
        assertEquals(924, item.mOverrideConfig.smallestScreenWidthDp);
        assertEquals(187, item.mOverrideConfig.densityDpi);
    }

    @Test
    public void compatModeLaunchActivityItemObjectDoesNotApplyViewportConfiguration() {
        String packageName = "com.android.chrome";
        LaunchActivityItemStub item = new LaunchActivityItemStub(
                activityInfo(packageName),
                configuration(462, 1001, 462, 374, 1.15f),
                configuration(462, 1001, 462, 374, 1.15f));

        LegacySystemServerHookInstaller.applyLaunchActivityItemObject(
                sourceFor(packageName, ViewportApplyMode.COMPAT),
                item);

        assertEquals(462, item.mCurConfig.screenWidthDp);
        assertEquals(1001, item.mCurConfig.screenHeightDp);
        assertEquals(462, item.mCurConfig.smallestScreenWidthDp);
        assertEquals(374, item.mCurConfig.densityDpi);
        assertEquals(462, item.mOverrideConfig.screenWidthDp);
        assertEquals(1001, item.mOverrideConfig.screenHeightDp);
        assertEquals(462, item.mOverrideConfig.smallestScreenWidthDp);
        assertEquals(374, item.mOverrideConfig.densityDpi);
    }

    private static PerAppDisplayConfigSource sourceFor(String packageName, String mode) {
        Set<String> configuredPackages = new LinkedHashSet<>();
        configuredPackages.add(packageName);
        Map<String, PackageConfigSnapshot> packages = new LinkedHashMap<>();
        packages.put(packageName, new PackageConfigSnapshot(
                packageName,
                true,
                ViewportTargetSpec.relativeScale(200000),
                mode,
                null,
                FontApplyMode.OFF,
                null,
                false,
                false,
                false,
                HookDomainOverride.automatic()));
        ConfigSnapshot snapshot = new ConfigSnapshot(
                configuredPackages,
                packages,
                true,
                true,
                true,
                true,
                true,
                true);
        return new PerAppDisplayConfigSource(snapshot);
    }

    private static ActivityInfo activityInfo(String packageName) {
        ActivityInfo info = new ActivityInfo();
        info.packageName = packageName;
        return info;
    }

    private static Configuration configuration(int widthDp,
                                               int heightDp,
                                               int smallestWidthDp,
                                               int densityDpi,
                                               float fontScale) {
        Configuration configuration = new Configuration();
        configuration.screenWidthDp = widthDp;
        configuration.screenHeightDp = heightDp;
        configuration.smallestScreenWidthDp = smallestWidthDp;
        configuration.densityDpi = densityDpi;
        configuration.fontScale = fontScale;
        return configuration;
    }

    private static final class LaunchActivityItemStub {
        final ActivityInfo mInfo;
        final Configuration mCurConfig;
        final Configuration mOverrideConfig;

        LaunchActivityItemStub(ActivityInfo info,
                               Configuration current,
                               Configuration override) {
            this.mInfo = info;
            this.mCurConfig = current;
            this.mOverrideConfig = override;
        }
    }
}
