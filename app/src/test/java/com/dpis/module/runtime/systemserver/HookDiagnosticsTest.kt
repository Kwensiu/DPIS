package com.dpis.module.runtime.systemserver

import android.content.res.Configuration
import android.os.Binder
import android.os.IBinder
import com.dpis.module.runtime.systemserver.selection.ConfiguredPackage
import com.dpis.module.runtime.systemserver.selection.PackageNames
import com.dpis.module.runtime.webapk.WebApkCarrierResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class HookDiagnosticsTest {
    @Test
    fun formatsProbeLogAndUnavailableState() {
        assertEquals(
            "system_server probe: entry=config-dispatch, package=com.max.xiaoheihe, original=config{widthDp=360,heightDp=736,smallestWidthDp=360,densityDpi=480}, target=config{widthDp=200,heightDp=409,smallestWidthDp=200,densityDpi=864}, actual=config{widthDp=360,heightDp=736,smallestWidthDp=360,densityDpi=480}",
            HookDiagnostics.buildProbeLog(
                "config-dispatch", "com.max.xiaoheihe",
                "config{widthDp=360,heightDp=736,smallestWidthDp=360,densityDpi=480}",
                "config{widthDp=200,heightDp=409,smallestWidthDp=200,densityDpi=864}",
                "config{widthDp=360,heightDp=736,smallestWidthDp=360,densityDpi=480}"
            ),
        )
        assertEquals(
            "system_server probe: entry=session-relayout, package=com.max.xiaoheihe, original=unavailable, target=config{widthDp=200,heightDp=409,smallestWidthDp=200,densityDpi=864}, actual=unavailable",
            HookDiagnostics.buildProbeLog(
                "session-relayout", "com.max.xiaoheihe", null,
                "config{widthDp=200,heightDp=409,smallestWidthDp=200,densityDpi=864}", null
            ),
        )
    }

    @Test
    fun describesConfigurationWithoutMutatingFontScale() {
        val configuration = Configuration().apply {
            screenWidthDp = 360
            screenHeightDp = 736
            smallestScreenWidthDp = 360
            densityDpi = 480
            fontScale = 1.25f
        }
        assertEquals(
            "config{widthDp=360,heightDp=736,smallestWidthDp=360,densityDpi=480,fontScale=1.25}",
            HookDiagnostics.describeState(configuration, null),
        )
    }

    @Test
    fun formatsGateAndPackageReadyDiagnostics() {
        assertEquals(
            "system_server hooks disabled: totalEnabled=false, safeMode=true",
            HookDiagnostics.buildGateDisabledLog(false, true)
        )
        assertEquals(
            "system_server package ready: process=android, package=com.android.providers.settings, moduleLoadedObserved=true, installAttempted=false, marker=${HookDiagnostics.BUILD_MARKER}",
            HookDiagnostics.buildPackageReadyStateLog(
                "android",
                "com.android.providers.settings",
                true,
                false
            ),
        )
    }

    @Test
    fun formatsPackageResolutionAndInterceptDiagnostics() {
        assertEquals(
            "system_server package unresolved: entry=activity-start, this=com.android.server.wm.ActivityStarter, argClasses=0:com.android.server.wm.ActivityStarter\$Request, argPreview=0:Request{intent=Intent { cmp=com.max.xiaoheihe/.SplashActivity }}, textPackages=com.max.xiaoheihe",
            HookDiagnostics.buildPackageResolveMissLog(
                "activity-start",
                "com.android.server.wm.ActivityStarter",
                "0:com.android.server.wm.ActivityStarter\$Request",
                "0:Request{intent=Intent { cmp=com.max.xiaoheihe/.SplashActivity }}",
                "com.max.xiaoheihe"
            ),
        )
        assertEquals(
            "system_server intercept enter: entry=activity-start, this=com.android.server.wm.ActivityStarter, argClasses=0:com.android.server.wm.ActivityStarter\$Request, argPreview=0:Request{callingUid=1000}",
            HookDiagnostics.buildInterceptEnterLog(
                "activity-start",
                "com.android.server.wm.ActivityStarter",
                "0:com.android.server.wm.ActivityStarter\$Request", "0:Request{callingUid=1000}"
            ),
        )
    }

    @Test
    fun configMissDistinguishesAbsentSourceState() {
        assertEquals(
            "system_server config miss: entry=activity-start, package=com.android.launcher, targetCandidates=com.max.xiaoheihe, sourceState=configuredPackages=3, configNull=true",
            HookDiagnostics.buildConfigMissLog(
                "activity-start", "com.android.launcher",
                "com.max.xiaoheihe", "configuredPackages=3, configNull=true"
            ),
        )
        assertEquals(
            "system_server config miss: entry=activity-start, package=com.android.launcher, targetCandidates=com.max.xiaoheihe",
            HookDiagnostics.buildConfigMissLog(
                "activity-start",
                "com.android.launcher",
                "com.max.xiaoheihe"
            ),
        )
    }

    @Test
    fun resolvesPackageNamesFromNestedFieldsAndIntentMethods() {
        assertEquals(
            "com.max.xiaoheihe", PackageNames.findPackageNameForTest(
                null,
                PackageHolder(WindowRecord("com.max.xiaoheihe"))
            )
        )
        assertEquals(
            "com.max.xiaoheihe", PackageNames.findPackageNameForTest(
                null,
                IntentStyleCarrier("com.max.xiaoheihe")
            )
        )
        assertEquals(
            "com.max.xiaoheihe", PackageNames.findPackageNameForTest(
                null,
                IntentCarrier(ComponentCarrier("com.max.xiaoheihe"))
            )
        )
    }

    @Test
    fun configuredPackageTextFallbackRespectsEntryOwnership() {
        val systemUiWindow = WindowTextCarrier(
            "Window{u0 com.android.systemui/NotificationShade for com.android.chrome}"
        )
        for (entry in listOf("display-policy-layout", "relayout-dispatch")) {
            assertEquals(
                "com.android.systemui",
                ConfiguredPackage.resolveConfiguredPackageForEntryForTest(
                    entry,
                    PackageCarrier("com.android.systemui"),
                    { it == "com.android.chrome" },
                    systemUiWindow
                )
            )
        }
        val appWindow =
            WindowTextCarrier("Window{u0 com.max.xiaoheihe/com.max.xiaoheihe.SplashActivity}")
        assertEquals(
            "com.max.xiaoheihe", ConfiguredPackage.resolveConfiguredPackageForEntryForTest(
                "config-dispatch",
                PackageCarrier("android.graphics"),
                { it == "com.max.xiaoheihe" },
                appWindow
            )
        )
        assertEquals(
            "com.max.xiaoheihe", ConfiguredPackage.resolveConfiguredPackageForTest(
                PackageCarrier("android.graphics"), { it == "com.max.xiaoheihe" }, appWindow
            )
        )
    }

    @Test
    fun relayoutResolutionUsesWindowMapAndPrefersWindowClient() {
        val client = FakeWindowClient()
        val service = FakeWindowManagerService().apply {
            mWindowMap[client.asBinder()] = FakeWindowState(client, "com.android.chrome")
        }
        assertEquals(
            "com.android.chrome", ConfiguredPackage.resolveRelayoutWindowPackageForTest(
                service, listOf(client), { it == "com.android.chrome" })
        )
        assertEquals(
            "com.android.chrome", ConfiguredPackage.resolveRelayoutWindowPackageForTest(
                service, listOf(FakeWindowSession(), client), { it == "com.android.chrome" })
        )
    }

    @Test
    fun launchResolutionUsesConfiguredWebApkOwnerAndOtherwiseKeepsChrome() {
        val owner = "org.chromium.webapk.a5e359e2ce8b830bb_v2"
        val carrier = WindowTextCarrier(
            "Intent { cmp=com.android.chrome/" +
                    "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity " +
                    "${WebApkCarrierResolver.WEBAPK_PACKAGE_EXTRA}=$owner }"
        )
        assertEquals(
            owner, ConfiguredPackage.resolveConfiguredPackageForTest(
                PackageCarrier("com.android.chrome"), { it == owner }, carrier
            )
        )
        assertEquals(
            owner, ConfiguredPackage.resolveLaunchActivityItemPackageForTest(
                "com.android.chrome", { it == owner }, carrier
            )
        )
        assertEquals(
            "com.android.chrome",
            ConfiguredPackage.resolveLaunchActivityItemPackageForTest(
                "com.android.chrome", { it == "com.android.chrome" }, carrier
            )
        )
    }

    @Test
    fun formatsConfigFallbackDiagnostic() {
        assertEquals(
            "system_server config fallback: entry=display-policy-layout, fromPackage=android.graphics, selectedPackage=com.max.xiaoheihe, targetCandidates=android.graphics|com.max.xiaoheihe",
            HookDiagnostics.buildConfigFallbackLog(
                "display-policy-layout", "android.graphics",
                "com.max.xiaoheihe", "android.graphics|com.max.xiaoheihe"
            ),
        )
    }

    private data class PackageHolder(val record: Any)
    private data class WindowRecord(val packageName: String)
    private data class IntentStyleCarrier(val targetPackage: String) {
        fun getPackage(): String = targetPackage
    }

    private data class IntentCarrier(val component: Any)
    private data class ComponentCarrier(val packageName: String)
    private data class PackageCarrier(val packageName: String)
    private class WindowTextCarrier(private val text: String) {
        override fun toString(): String = text
    }

    private class FakeWindowManagerService {
        val mWindowMap: MutableMap<Any, Any> = HashMap()
    }

    private class FakeWindowClient {
        private val binder: IBinder = Binder()
        fun asBinder(): IBinder = binder
    }

    private class FakeWindowSession {
        private val binder: IBinder = Binder()
        fun asBinder(): IBinder = binder
    }

    private data class FakeWindowState(val mClient: Any, val packageName: String)
}
