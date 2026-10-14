package com.dpis.module

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.runtime.webapk.WebApkRuntimeOwnerBridge
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebApkRuntimeOwnerBridgeTest {
    @Before
    fun setUp() {
        WebApkRuntimeOwnerBridge.resetForTest()
    }

    @Test
    fun extractsOwnerOnlyFromChromeWebApkActivityText() {
        assertEquals(
            "org.chromium.webapk.ac19cf34f94565db5_v2",
            WebApkRuntimeOwnerBridge.currentWebApkOwnerForTest(
                "ActivityRecord{u0 com.android.chrome/" +
                        "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity " +
                        "dat=webapp://webapk-org.chromium.webapk.ac19cf34f94565db5_v2}",
            ),
        )
    }

    @Test
    fun ignoresNormalChromeActivityText() {
        assertNull(
            WebApkRuntimeOwnerBridge.currentWebApkOwnerForTest(
                "ActivityRecord{u0 com.android.chrome/" +
                        "org.chromium.chrome.browser.ChromeTabbedActivity}",
            ),
        )
    }

    @Test
    fun extractsOwnerFromActivityRecordIntentText() {
        assertEquals(
            "org.chromium.webapk.ac19cf34f94565db5_v2",
            WebApkRuntimeOwnerBridge.currentWebApkOwnerForTest(
                "ActivityClientRecord{intent=Intent { " +
                        "dat=webapp://webapk-org.chromium.webapk.ac19cf34f94565db5_v2 " +
                        "cmp=com.android.chrome/" +
                        "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity }}",
            ),
        )
    }

    @Test
    fun cachesOwnerFromWebApkLifecycleIntent() {
        assertEquals(
            "org.chromium.webapk.ac19cf34f94565db5_v2",
            WebApkRuntimeOwnerBridge.observeActivityForTest(
                "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity",
                "Intent { dat=webapp://webapk-org.chromium.webapk.ac19cf34f94565db5_v2 }",
                null,
                "onResume",
            ),
        )
    }

    @Test
    fun clearsOwnerWhenNormalChromeActivityResumes() {
        WebApkRuntimeOwnerBridge.observeActivityForTest(
            "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity",
            "Intent { dat=webapp://webapk-org.chromium.webapk.ac19cf34f94565db5_v2 }",
            null,
            "onResume",
        )

        assertNull(
            WebApkRuntimeOwnerBridge.observeActivityForTest(
                "org.chromium.chrome.browser.ChromeTabbedActivity",
                "Intent { cmp=com.android.chrome/.ChromeTabbedActivity }",
                null,
                "onResume",
            ),
        )
    }

    @Test
    fun hotReloadResetClearsCachedOwner() {
        WebApkRuntimeOwnerBridge.observeActivityForTest(
            "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity",
            "Intent { dat=webapp://webapk-org.chromium.webapk.ac19cf34f94565db5_v2 }",
            null,
            "onResume",
        )

        WebApkRuntimeOwnerBridge.resetForHotReload()

        assertNull(
            WebApkRuntimeOwnerBridge.observeActivityForTest(
                "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity",
                "Intent { cmp=com.android.chrome/.SameTaskWebApkActivity }",
                null,
                "onResume",
            ),
        )
    }

    @Test
    fun emptyOwnerConfigDoesNotOverrideChromeCarrier() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetDpisEnabled(OWNER, true)

        assertFalse(WebApkRuntimeOwnerBridge.hasActiveOwnerConfigForTest(store, OWNER))
    }

    @Test
    fun activeOwnerConfigCanOverrideChromeCarrier() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetDpisEnabled(OWNER, true)
        store.setTargetViewportSpec(OWNER, ViewportTargetSpec.relativeScale(150000))

        assertTrue(WebApkRuntimeOwnerBridge.hasActiveOwnerConfigForTest(store, OWNER))
    }

    @Test
    fun ownerResolutionDoesNotUsePendingPropertyBridge() {
        val source = SourceSmokeTestPaths.read(SOURCE_PATH)

        assertFalse(source.contains("debug.dpis.webapk.pending_owner"))
        assertFalse(source.contains("consumePendingOwnerForChrome"))
    }

    @Test
    fun ownerLifecycleTriggersActivityResourceSync() {
        val source = SourceSmokeTestPaths.read(SOURCE_PATH)

        // Activity/resource hook calls are an external wiring contract not executable in JVM tests.
        assertTrue(source.contains("syncActivityResources(activity, sourceTag)"))
        assertTrue(source.contains("ResourcesManagerHookInstaller.applyResourceOverrides("))
        assertTrue(source.contains("ResourcesImplHookInstaller.applyDensityOverride("))
        assertTrue(source.contains("AutoViewportRuntimeRoute.ANY_ENABLED_TARGET"))
        assertTrue(source.contains("DPIS_WEBAPK activity resources synced"))
    }

    private companion object {
        const val OWNER = "org.chromium.webapk.ac19cf34f94565db5_v2"
        const val SOURCE_PATH =
            "src/main/java/com/dpis/module/runtime/webapk/WebApkRuntimeOwnerBridge.kt"
    }
}
