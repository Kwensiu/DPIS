package com.dpis.module;

import static org.junit.Assert.assertEquals;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

import com.dpis.module.config.ConfigSnapshot;
import com.dpis.module.config.PackageConfigSnapshot;
import com.dpis.module.config.PerAppDisplayConfigSource;
import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.hooks.HookDomainOverride;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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

    private static PerAppDisplayConfigSource sourceFor(String packageName, String mode) {
        return sourceFor(packageName, mode, 0);
    }

    private static PerAppDisplayConfigSource sourceFor(String packageName, String mode, int ownerUserId) {
        Set<String> configuredPackages = new LinkedHashSet<>();
        configuredPackages.add(packageName);
        Map<String, PackageConfigSnapshot> packages = new LinkedHashMap<>();
        boolean userZero = ownerUserId == 0;
        packages.put(packageName, new PackageConfigSnapshot(
                packageName,
                true,
                userZero ? ViewportTargetSpec.relativeScale(200000) : ViewportTargetSpec.off(),
                userZero ? mode : ViewportApplyMode.OFF,
                userZero ? null : 130,
                userZero ? FontApplyMode.OFF : FontApplyMode.SYSTEM_EMULATION,
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
                true,
                ownerUserId);
        return new PerAppDisplayConfigSource(snapshot);
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

    private static ActivityInfo activityInfo(String packageName) {
        return activityInfo(packageName, 10088);
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

    private static ActivityInfo activityInfo(String packageName, int uid) {
        ActivityInfo info = new ActivityInfo();
        info.packageName = packageName;
        info.applicationInfo = new android.content.pm.ApplicationInfo();
        info.applicationInfo.packageName = packageName;
        info.applicationInfo.uid = uid;
        return info;
    }

    private static ActivityInfo activityInfoWithoutUid(String packageName) {
        ActivityInfo info = new ActivityInfo();
        info.packageName = packageName;
        return info;
    }

    @Test
    public void missingActivityUidFailsClosedInsteadOfUsingUserZero() {
        String packageName = "com.android.chrome";
        Configuration configuration = configuration(462, 1001, 462, 374, 1.15f);

        LegacySystemServerHookInstaller.applyLaunchActivityItemArgs(
                sourceFor(packageName, ViewportApplyMode.SYSTEM),
                new Object[]{activityInfoWithoutUid(packageName), configuration});

        assertEquals(462, configuration.screenWidthDp);
        assertEquals(374, configuration.densityDpi);
    }

    @Test
    public void user999ActivityUidSelectsUser999SnapshotForFontOnlyConfig() {
        String packageName = "com.android.chrome";
        Configuration configuration = configuration(462, 1001, 462, 374, 1.15f);

        LegacySystemServerHookInstaller.applyLaunchActivityItemArgs(
                sourceFor(packageName, ViewportApplyMode.SYSTEM, 999),
                new Object[]{activityInfo(packageName, 99910088), configuration});

        assertEquals(462, configuration.screenWidthDp);
        assertEquals(374, configuration.densityDpi);
        assertEquals(1.3f, configuration.fontScale, 0.0001f);
    }

    @Test
    public void user999LaunchActivityItemObjectUsesUser999SnapshotForFontOnlyConfig() {
        String packageName = "com.android.chrome";
        LaunchActivityItemStub item = new LaunchActivityItemStub(
                activityInfo(packageName, 99910088),
                configuration(462, 1001, 462, 374, 1.15f),
                configuration(462, 1001, 462, 374, 1.15f));

        LegacySystemServerHookInstaller.applyLaunchActivityItemObject(
                sourceFor(packageName, ViewportApplyMode.SYSTEM, 999),
                item);

        assertEquals(462, item.mCurConfig.screenWidthDp);
        assertEquals(374, item.mCurConfig.densityDpi);
        assertEquals(1.3f, item.mCurConfig.fontScale, 0.0001f);
        assertEquals(1.3f, item.mOverrideConfig.fontScale, 0.0001f);
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

    private record LaunchActivityItemStub(ActivityInfo mInfo, Configuration mCurConfig,
                                          Configuration mOverrideConfig) {
    }
}
