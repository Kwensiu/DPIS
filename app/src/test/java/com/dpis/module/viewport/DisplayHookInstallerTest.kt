package com.dpis.module

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.runtime.appprocess.DisplayHookInstaller
import com.dpis.module.runtime.probe.RuntimeClock
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge
import com.dpis.module.viewport.ViewportRuntimeRecord
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.VirtualDisplayOverride
import com.dpis.module.viewport.VirtualDisplayState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DisplayHookInstallerTest {
    @Test
    fun enablesDisplayOverrideForConfiguredPackage() {
        assertEquals(
            true,
            DisplayHookInstaller.shouldApplyOverrideForPackage(
                "com.max.xiaoheihe",
                "com.max.xiaoheihe"
            ),
        )
    }

    @Test
    fun skipsDisplayOverrideForMissingPackage() {
        assertEquals(
            false,
            DisplayHookInstaller.shouldApplyOverrideForPackage(null, "com.max.xiaoheihe")
        )
    }

    @Test
    fun skipsDisplayOverrideForDifferentCurrentPackage() {
        assertEquals(
            false,
            DisplayHookInstaller.shouldApplyOverrideForPackage(
                "com.max.xiaoheihe",
                "com.example.other"
            ),
        )
    }

    @Test
    fun skipsDisplayOverrideForBlankTargetPackage() {
        assertEquals(
            false,
            DisplayHookInstaller.shouldApplyOverrideForPackage("  ", "com.max.xiaoheihe")
        )
    }

    @Test
    fun skipsWhenCurrentPackageCannotBeResolved() {
        assertEquals(
            false,
            DisplayHookInstaller.shouldApplyOverrideForPackage("com.max.xiaoheihe", null)
        )
    }

    @Test
    fun legacyCanInitializeDisplayTarget() {
        DisplayHookInstaller.setTargetPackageNameForLegacy("com.max.xiaoheihe")
        DisplayHookInstaller.setTargetStoreForLegacy(DpisConfigStore(FakePrefs()))
        setCurrentPackageResolver()

        assertEquals(true, DisplayHookInstaller.shouldApplyOverrideForPackage("com.max.xiaoheihe"))

        DisplayHookInstaller.setTargetPackageNameForLegacy(null)
        DisplayHookInstaller.setTargetStoreForLegacy(null)
        clearCurrentPackageResolver()
    }

    @Test
    fun skipsGlobalDisplayStateWithoutPackageScopedRecord() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec("com.tencent.mm", ViewportTargetSpec.relativeScale(150000))
        VirtualDisplayState.set(VirtualDisplayOverride.Result(900, 1800, 900, 240, 1080, 2160))

        assertNull(
            DisplayHookInstaller.resolvePackageScopedOverrideForTest(
                "com.tencent.mm",
                store
            )
        )

        VirtualDisplayState.set(null)
    }

    @Test
    fun usesPackageScopedDisplayRecordForCurrentTarget() {
        val store = DpisConfigStore(FakePrefs())
        val targetSpec = ViewportTargetSpec.relativeScale(150000)
        store.setTargetViewportSpec("com.tencent.mm", targetSpec)
        val source = ViewportSourceSnapshot.systemDisplayInfo(360, 736, 360, 480, 1080, 2208)
        val virtualDisplay = VirtualDisplayOverride.Result(540, 1104, 540, 320, 1080, 2208)

        VirtualDisplayState.publish(
            "com.tencent.mm",
            targetSpec,
            source,
            ViewportOverride.Result(540, 1104, 540, 320),
            virtualDisplay,
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
        )

        assertEquals(
            320,
            DisplayHookInstaller.resolvePackageScopedOverrideForTest(
                "com.tencent.mm",
                store
            )!!.densityDpi,
        )

        VirtualDisplayState.set(null)
    }

    @Test
    fun completeMarkerWinsOverStaleDisplayRecord() {
        val packageName = "com.example.display-marker-boundary"
        val store = DpisConfigStore(FakePrefs())
        val targetSpec = ViewportTargetSpec.relativeScale(120000)
        store.setTargetViewportSpec(packageName, targetSpec)
        VirtualDisplayState.set(VirtualDisplayOverride.Result(518, 1139, 518, 334, 1080, 2376))
        ViewportRuntimeMarkerBridge.publish(
            packageName,
            ViewportRuntimeMarkerBridge.createRecord(
                packageName,
                targetSpec,
                432,
                ViewportSourceSnapshot.systemDisplayInfo(360, 792, 360, 480, 1080, 2376),
                ViewportOverride.Result(432, 950, 432, 400),
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
                RuntimeClock.crossProcessMarkerMillis(),
            ),
        )

        val result = DisplayHookInstaller.resolvePackageScopedOverrideForTest(packageName, store)

        assertNotNull(result)
        assertEquals(432, result!!.smallestWidthDp)
        assertEquals(400, result.densityDpi)
        assertEquals(1080, result.widthPx)
        assertEquals(2376, result.heightPx)
        VirtualDisplayState.set(null)
    }

    @Test
    fun usesPackageScopedDisplayRecordForWebApkOwner() {
        val store = DpisConfigStore(FakePrefs())
        val packageName = "org.chromium.webapk.ac19cf34f94565db5_v2"
        val targetSpec = ViewportTargetSpec.relativeScale(150000)
        store.setTargetDpisEnabled(packageName, true)
        store.setTargetViewportSpec(packageName, targetSpec)
        val source = ViewportSourceSnapshot.systemDisplayInfo(360, 792, 360, 480, 1080, 2376)
        val virtualDisplay = VirtualDisplayOverride.Result(540, 1188, 540, 320, 1080, 2376)

        VirtualDisplayState.publish(
            packageName,
            targetSpec,
            source,
            ViewportOverride.Result(540, 1188, 540, 320),
            virtualDisplay,
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
        )

        assertEquals(
            320,
            DisplayHookInstaller.resolvePackageScopedOverrideForTest(
                packageName,
                store
            )!!.densityDpi,
        )

        VirtualDisplayState.set(null)
    }

    private fun setCurrentPackageResolver() {
        try {
            val method =
                DisplayHookInstallerTest::class.java.getDeclaredMethod("testCurrentPackageName")
            method.isAccessible = true
            val resolverField =
                DisplayHookInstaller::class.java.getDeclaredField("currentPackageNameMethod")
            resolverField.isAccessible = true
            resolverField.set(DisplayHookInstaller, method)
            val unavailableField =
                DisplayHookInstaller::class.java.getDeclaredField("currentPackageNameUnavailable")
            unavailableField.isAccessible = true
            unavailableField.setBoolean(DisplayHookInstaller, false)
        } catch (e: ReflectiveOperationException) {
            throw AssertionError(e)
        }
    }

    private fun clearCurrentPackageResolver() {
        try {
            val resolverField =
                DisplayHookInstaller::class.java.getDeclaredField("currentPackageNameMethod")
            resolverField.isAccessible = true
            resolverField.set(DisplayHookInstaller, null)
            val unavailableField =
                DisplayHookInstaller::class.java.getDeclaredField("currentPackageNameUnavailable")
            unavailableField.isAccessible = true
            unavailableField.setBoolean(DisplayHookInstaller, false)
        } catch (e: ReflectiveOperationException) {
            throw AssertionError(e)
        }
    }

    companion object {
        @JvmStatic
        private fun testCurrentPackageName(): String = "com.max.xiaoheihe"
    }
}
