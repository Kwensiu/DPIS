package com.dpis.module;

import android.content.res.Configuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SystemServerDisplayDiagnosticsTest {
    @Test
    public void formatsProbeLog() {
        String message = SystemServerDisplayDiagnostics.buildProbeLog(
                "config-dispatch",
                "com.max.xiaoheihe",
                "config{widthDp=360,heightDp=736,smallestWidthDp=360,densityDpi=480}",
                "config{widthDp=200,heightDp=409,smallestWidthDp=200,densityDpi=864}",
                "config{widthDp=360,heightDp=736,smallestWidthDp=360,densityDpi=480}");

        assertEquals(
                "system_server probe: entry=config-dispatch, package=com.max.xiaoheihe, original=config{widthDp=360,heightDp=736,smallestWidthDp=360,densityDpi=480}, target=config{widthDp=200,heightDp=409,smallestWidthDp=200,densityDpi=864}, actual=config{widthDp=360,heightDp=736,smallestWidthDp=360,densityDpi=480}",
                message);
    }

    @Test
    public void describesConfigurationOnly() {
        Configuration configuration = new Configuration();
        configuration.screenWidthDp = 360;
        configuration.screenHeightDp = 736;
        configuration.smallestScreenWidthDp = 360;
        configuration.densityDpi = 480;

        String summary = SystemServerDisplayDiagnostics.describeState(configuration, null);

        assertEquals(
                "config{widthDp=360,heightDp=736,smallestWidthDp=360,densityDpi=480}",
                summary);
    }

    @Test
    public void findsPackageNameFromNestedObject() {
        String packageName = SystemServerDisplayEnvironmentInstaller.findPackageNameForTest(
                null,
                new PackageHolder(new WindowRecord("com.max.xiaoheihe")));

        assertEquals("com.max.xiaoheihe", packageName);
    }

    @Test
    public void findsPackageNameFromIntentStyleMethod() {
        String packageName = SystemServerDisplayEnvironmentInstaller.findPackageNameForTest(
                null,
                new IntentStyleCarrier("com.max.xiaoheihe"));

        assertEquals("com.max.xiaoheihe", packageName);
    }

    @Test
    public void findsPackageNameFromComponentStyleMethod() {
        String packageName = SystemServerDisplayEnvironmentInstaller.findPackageNameForTest(
                null,
                new IntentCarrier(new ComponentCarrier("com.max.xiaoheihe")));

        assertEquals("com.max.xiaoheihe", packageName);
    }

    @Test
    public void describesUnavailableActualSummaryInProbeLog() {
        String message = SystemServerDisplayDiagnostics.buildProbeLog(
                "session-relayout",
                "com.max.xiaoheihe",
                null,
                "config{widthDp=200,heightDp=409,smallestWidthDp=200,densityDpi=864}",
                null);

        assertEquals(
                "system_server probe: entry=session-relayout, package=com.max.xiaoheihe, original=unavailable, target=config{widthDp=200,heightDp=409,smallestWidthDp=200,densityDpi=864}, actual=unavailable",
                message);
    }

    @Test
    public void formatsDisabledByGateLog() {
        String message = SystemServerDisplayDiagnostics.buildGateDisabledLog(false, true);

        assertEquals(
                "system_server hooks disabled: totalEnabled=false, safeMode=true",
                message);
    }

    @Test
    public void formatsPackageReadyStateLog() {
        String message = SystemServerDisplayDiagnostics.buildPackageReadyStateLog(
                "android",
                "com.android.providers.settings",
                true,
                false);

        assertEquals(
                "system_server package ready: process=android, package=com.android.providers.settings, moduleLoadedObserved=true, installAttempted=false, marker="
                        + SystemServerDisplayDiagnostics.BUILD_MARKER,
                message);
    }

    @Test
    public void formatsPackageResolveMissLog() {
        String message = SystemServerDisplayDiagnostics.buildPackageResolveMissLog(
                "activity-start",
                "com.android.server.wm.ActivityStarter",
                "0:com.android.server.wm.ActivityStarter$Request",
                "0:Request{intent=Intent { cmp=com.max.xiaoheihe/.SplashActivity }}",
                "com.max.xiaoheihe");

        assertEquals(
                "system_server package unresolved: entry=activity-start, this=com.android.server.wm.ActivityStarter, argClasses=0:com.android.server.wm.ActivityStarter$Request, argPreview=0:Request{intent=Intent { cmp=com.max.xiaoheihe/.SplashActivity }}, textPackages=com.max.xiaoheihe",
                message);
    }

    @Test
    public void formatsInterceptEnterLog() {
        String message = SystemServerDisplayDiagnostics.buildInterceptEnterLog(
                "activity-start",
                "com.android.server.wm.ActivityStarter",
                "0:com.android.server.wm.ActivityStarter$Request",
                "0:Request{callingUid=1000}");

        assertEquals(
                "system_server intercept enter: entry=activity-start, this=com.android.server.wm.ActivityStarter, argClasses=0:com.android.server.wm.ActivityStarter$Request, argPreview=0:Request{callingUid=1000}",
                message);
    }

    @Test
    public void formatsConfigMissLog() {
        String message = SystemServerDisplayDiagnostics.buildConfigMissLog(
                "activity-start",
                "com.android.launcher",
                "com.max.xiaoheihe");

        assertEquals(
                "system_server config miss: entry=activity-start, package=com.android.launcher, targetCandidates=com.max.xiaoheihe",
                message);
    }

    @Test
    public void resolvesConfiguredPackageFromCandidateText() {
        String packageName = SystemServerDisplayEnvironmentInstaller.resolveConfiguredPackageForTest(
                new PackageCarrier("android.graphics"),
                "com.max.xiaoheihe"::equals,
                new WindowTextCarrier("Window{u0 com.max.xiaoheihe/com.max.xiaoheihe.SplashActivity}"));

        assertEquals("com.max.xiaoheihe", packageName);
    }

    @Test
    public void formatsConfigFallbackLog() {
        String message = SystemServerDisplayDiagnostics.buildConfigFallbackLog(
                "display-policy-layout",
                "android.graphics",
                "com.max.xiaoheihe",
                "android.graphics|com.max.xiaoheihe");

        assertEquals(
                "system_server config fallback: entry=display-policy-layout, fromPackage=android.graphics, selectedPackage=com.max.xiaoheihe, targetCandidates=android.graphics|com.max.xiaoheihe",
                message);
    }

    private static final class PackageHolder {
        final Object record;

        PackageHolder(Object record) {
            this.record = record;
        }
    }

    private static final class WindowRecord {
        final String packageName;

        WindowRecord(String packageName) {
            this.packageName = packageName;
        }
    }

    private static final class IntentStyleCarrier {
        private final String targetPackage;

        IntentStyleCarrier(String targetPackage) {
            this.targetPackage = targetPackage;
        }

        public String getPackage() {
            return targetPackage;
        }
    }

    private static final class IntentCarrier {
        private final Object component;

        private IntentCarrier(Object component) {
            this.component = component;
        }

        public Object getComponent() {
            return component;
        }
    }

    private static final class ComponentCarrier {
        private final String packageName;

        private ComponentCarrier(String packageName) {
            this.packageName = packageName;
        }

        public String getPackageName() {
            return packageName;
        }
    }

    private static final class PackageCarrier {
        private final String packageName;

        private PackageCarrier(String packageName) {
            this.packageName = packageName;
        }

        public String getPackageName() {
            return packageName;
        }
    }

    private static final class WindowTextCarrier {
        private final String text;

        private WindowTextCarrier(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
