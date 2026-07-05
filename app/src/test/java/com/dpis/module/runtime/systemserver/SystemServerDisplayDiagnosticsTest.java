package com.dpis.module.runtime.systemserver;

import com.dpis.module.runtime.appprocess.WebApkCarrierResolver;

import com.dpis.module.*;

import android.content.res.Configuration;
import android.os.Binder;
import android.os.IBinder;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

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
        configuration.fontScale = 1.25f;

        String summary = SystemServerDisplayDiagnostics.describeState(configuration, null);

        assertEquals(
                "config{widthDp=360,heightDp=736,smallestWidthDp=360,densityDpi=480,fontScale=1.25}",
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
                "com.max.xiaoheihe",
                "configuredPackages=3, configNull=true");

        assertEquals(
                "system_server config miss: entry=activity-start, package=com.android.launcher, targetCandidates=com.max.xiaoheihe, sourceState=configuredPackages=3, configNull=true",
                message);
    }

    @Test
    public void formatsConfigMissLogWithoutSourceState() {
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
    public void windowMaintenanceDoesNotFallbackToAppPackageFromSystemUiWindowText() {
        String displayPolicyPackage = SystemServerDisplayEnvironmentInstaller
                .resolveConfiguredPackageForEntryForTest(
                        "display-policy-layout",
                        new PackageCarrier("com.android.systemui"),
                        "com.android.chrome"::equals,
                        new WindowTextCarrier("Window{u0 com.android.systemui/NotificationShade "
                                + "for com.android.chrome}"));
        String relayoutPackage = SystemServerDisplayEnvironmentInstaller
                .resolveConfiguredPackageForEntryForTest(
                        "relayout-dispatch",
                        new PackageCarrier("com.android.systemui"),
                        "com.android.chrome"::equals,
                        new WindowTextCarrier("Window{u0 com.android.systemui/NotificationShade "
                                + "for com.android.chrome}"));

        assertEquals("com.android.systemui", displayPolicyPackage);
        assertEquals("com.android.systemui", relayoutPackage);
    }

    @Test
    public void configDispatchStillFallsBackToConfiguredPackageFromText() {
        String packageName = SystemServerDisplayEnvironmentInstaller
                .resolveConfiguredPackageForEntryForTest(
                        "config-dispatch",
                        new PackageCarrier("android.graphics"),
                        "com.max.xiaoheihe"::equals,
                        new WindowTextCarrier(
                                "Window{u0 com.max.xiaoheihe/com.max.xiaoheihe.SplashActivity}"));

        assertEquals("com.max.xiaoheihe", packageName);
    }

    @Test
    public void resolvesRelayoutPackageThroughWindowManagerWindowMap() {
        FakeWindowClient client = new FakeWindowClient();
        FakeWindowState windowState = new FakeWindowState(client, "com.android.chrome");
        FakeWindowManagerService windowManagerService = new FakeWindowManagerService();
        windowManagerService.mWindowMap.put(client.asBinder(), windowState);

        String packageName = SystemServerDisplayEnvironmentInstaller
                .resolveRelayoutWindowPackageForTest(
                        windowManagerService,
                        java.util.List.of(client),
                        "com.android.chrome"::equals);

        assertEquals("com.android.chrome", packageName);
    }

    @Test
    public void relayoutPackageResolverPrefersIWindowClientOverSessionBinder() {
        FakeWindowSession session = new FakeWindowSession();
        FakeWindowClient client = new FakeWindowClient();
        FakeWindowState windowState = new FakeWindowState(client, "com.android.chrome");
        FakeWindowManagerService windowManagerService = new FakeWindowManagerService();
        windowManagerService.mWindowMap.put(client.asBinder(), windowState);

        String packageName = SystemServerDisplayEnvironmentInstaller
                .resolveRelayoutWindowPackageForTest(
                        windowManagerService,
                        java.util.List.of(session, client),
                        "com.android.chrome"::equals);

        assertEquals("com.android.chrome", packageName);
    }

    @Test
    public void resolvesConfiguredWebApkOwnerFromChromeCarrierText() {
        String packageName = SystemServerDisplayEnvironmentInstaller.resolveConfiguredPackageForTest(
                new PackageCarrier("com.android.chrome"),
                "org.chromium.webapk.a5e359e2ce8b830bb_v2"::equals,
                new WindowTextCarrier("Intent { cmp=com.android.chrome/"
                        + "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity "
                        + "(has extras) extras={"
                        + WebApkCarrierResolver.WEBAPK_PACKAGE_EXTRA
                        + "=org.chromium.webapk.a5e359e2ce8b830bb_v2} }"));

        assertEquals("org.chromium.webapk.a5e359e2ce8b830bb_v2", packageName);
    }

    @Test
    public void launchActivityItemPrefersConfiguredWebApkOwnerFromChromeCarrierText() {
        String packageName = SystemServerDisplayEnvironmentInstaller
                .resolveLaunchActivityItemPackageForTest(
                        "com.android.chrome",
                        "org.chromium.webapk.a5e359e2ce8b830bb_v2"::equals,
                        new WindowTextCarrier("Intent { cmp=com.android.chrome/"
                                + "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity "
                                + WebApkCarrierResolver.WEBAPK_PACKAGE_EXTRA
                                + "=org.chromium.webapk.a5e359e2ce8b830bb_v2 }"));

        assertEquals("org.chromium.webapk.a5e359e2ce8b830bb_v2", packageName);
    }

    @Test
    public void launchActivityItemKeepsChromeWhenWebApkOwnerIsUnconfigured() {
        String packageName = SystemServerDisplayEnvironmentInstaller
                .resolveLaunchActivityItemPackageForTest(
                        "com.android.chrome",
                        "com.android.chrome"::equals,
                        new WindowTextCarrier("Intent { cmp=com.android.chrome/"
                                + "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity "
                                + WebApkCarrierResolver.WEBAPK_PACKAGE_EXTRA
                                + "=org.chromium.webapk.a5e359e2ce8b830bb_v2 }"));

        assertEquals("com.android.chrome", packageName);
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

    private static final class FakeWindowManagerService {
        final Map<Object, Object> mWindowMap = new HashMap<>();
    }

    private static final class FakeWindowClient {
        private final IBinder binder = new Binder();

        public IBinder asBinder() {
            return binder;
        }
    }

    private static final class FakeWindowSession {
        private final IBinder binder = new Binder();

        public IBinder asBinder() {
            return binder;
        }
    }

    private static final class FakeWindowState {
        final Object mClient;
        final String packageName;

        private FakeWindowState(Object client, String packageName) {
            this.mClient = client;
            this.packageName = packageName;
        }
    }
}
