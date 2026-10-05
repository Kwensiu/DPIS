package com.dpis.module

import android.graphics.Point
import android.util.DisplayMetrics
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.Coordinator
import com.dpis.module.diagnostics.RuntimeEvents
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.runtime.appprocess.DisplayHookInstaller
import com.dpis.module.runtime.probe.RuntimeClock
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge
import com.dpis.module.viewport.ViewportRuntimeRecord
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.VirtualDisplayOverride
import com.dpis.module.viewport.VirtualDisplayPlan
import com.dpis.module.viewport.VirtualDisplayState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field
import java.lang.reflect.Method

class VirtualDisplayOverrideTest {
    @Before
    fun setUp() {
        setTargetPackageName("com.max.xiaoheihe")
        setCurrentPackageResolver()
    }

    @After
    fun tearDown() {
        RuntimeEvents.cancel()
        RuntimeHotPathEvents.resetForTest()
        DisplayHookInstaller.resetHotPathSamplerForTest()
        VirtualDisplayState.set(null)
        setTargetPackageName(null)
        DisplayHookInstaller.setTargetStoreForLegacy(null)
        clearCurrentPackageResolver()
    }

    @Test
    fun keepsWindowPixelSizeAtPhysicalBounds() {
        val result = requireNotNull(
            VirtualDisplayOverride.derive(360, 736, 360, 480, 1080, 2208, 300),
        )

        assertEquals(300, result.widthDp.toLong())
        assertEquals(613, result.heightDp.toLong())
        assertEquals(300, result.smallestWidthDp.toLong())
        assertEquals(576, result.densityDpi.toLong())
        assertEquals(1080, result.widthPx.toLong())
        assertEquals(2208, result.heightPx.toLong())
    }

    @Test
    fun appliesDisplayMetricsFromPackageScopedRecord() {
        publishTargetRecord()
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 2208
        metrics.densityDpi = 480

        DisplayHookInstaller.applyDisplayMetrics(metrics, "test")

        assertEquals(1080, metrics.widthPixels.toLong())
        assertEquals(2208, metrics.heightPixels.toLong())
        assertEquals(576, metrics.densityDpi.toLong())
    }

    @Test
    fun relativeDisplayMetricsRewritePhysicalDensityOnce() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec("com.max.xiaoheihe", ViewportTargetSpec.relativeScale(120000))
        store.setSystemServerHooksEnabled(true)
        DisplayHookInstaller.setTargetStoreForLegacy(store)
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 2208
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f

        DisplayHookInstaller.applyDisplayMetrics(metrics, "getRealMetrics")

        assertEquals(1080, metrics.widthPixels.toLong())
        assertEquals(2208, metrics.heightPixels.toLong())
        assertEquals(400, metrics.densityDpi.toLong())
    }

    @Test
    fun displayMetricsOverrideRecordsViewportHotpathEvidence() {
        publishTargetRecord()
        RuntimeEvents.start("com.max.xiaoheihe", request())
        DisplayHookInstaller.resetHotPathSamplerForTest()
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 2208
        metrics.densityDpi = 480

        DisplayHookInstaller.applyDisplayMetrics(metrics, "diagnostic-test")

        val events = RuntimeEvents.stopSnapshot()
        assertTrue(
            events.any { event ->
                event.contains("route=viewport") &&
                        event.contains("stage=applied") &&
                        event.contains("display_metrics_override")
            },
        )
    }

    @Test
    fun displayMetricsProbeEvidenceIncludesCountedSamples() {
        RuntimeEvents.start("com.max.xiaoheihe", request())
        DisplayHookInstaller.resetHotPathSamplerForTest()
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 2208
        metrics.densityDpi = 480

        repeat(50) {
            DisplayHookInstaller.applyDisplayMetrics(metrics, "diagnostic-count-test")
        }

        val events = RuntimeEvents.stopSnapshot()
        assertTrue(
            events.any { event ->
                event.contains("route=viewport") &&
                        event.contains("stage=probe") &&
                        event.contains("display_metrics_override") &&
                        event.contains("hitCount=50") &&
                        event.contains("suppressedCount=48")
            },
        )
    }

    @Test
    fun realMetricsRestoreDisplayPixelsAndAlignScaledDensity() {
        publishRelativeDisplay()
        val metrics = displayMetrics(1080, 1600, 400, 3.0f)

        DisplayHookInstaller.applyDisplayMetrics(metrics, "getRealMetrics")

        assertEquals(1080, metrics.widthPixels.toLong())
        assertEquals(2376, metrics.heightPixels.toLong())
        assertEquals(400, metrics.densityDpi.toLong())
        assertEquals(2.5f, metrics.density, 0.001f)
        assertEquals(2.5f, metrics.scaledDensity, 0.001f)
    }

    @Test
    fun appMetricsKeepWindowPixelsAndAlignScaledDensity() {
        publishRelativeDisplay()
        val metrics = displayMetrics(1080, 1600, 400, 3.0f)

        DisplayHookInstaller.applyDisplayMetrics(metrics, "getMetrics")

        assertEquals(1080, metrics.widthPixels.toLong())
        assertEquals(1600, metrics.heightPixels.toLong())
        assertEquals(400, metrics.densityDpi.toLong())
        assertEquals(2.5f, metrics.density, 0.001f)
        assertEquals(2.5f, metrics.scaledDensity, 0.001f)
    }

    @Test
    fun windowMetricsKeepOwnPixelsAndUseDisplayDensity() {
        publishTargetRecord()
        val metrics = displayMetrics(1080, 1600, 480, 3.0f)

        DisplayHookInstaller.applyDisplayMetrics(metrics, "getMetrics")

        assertEquals(1080, metrics.widthPixels.toLong())
        assertEquals(1600, metrics.heightPixels.toLong())
        assertEquals(576, metrics.densityDpi.toLong())
    }

    @Test
    fun windowPointKeepsOwnSize() {
        publishTargetRecord()
        val point = Point()
        point.x = 900
        point.y = 1400

        DisplayHookInstaller.applyPoint(point, "getSize")

        assertEquals(900, point.x.toLong())
        assertEquals(1400, point.y.toLong())
    }

    @Test
    fun appliesPointFromPackageScopedRecord() {
        publishTargetRecord()
        val point = Point()
        point.x = 1
        point.y = 2

        DisplayHookInstaller.applyPoint(point, "test")

        assertEquals(1080, point.x.toLong())
        assertEquals(2208, point.y.toLong())
    }

    @Test
    fun keepsDensityStableAcrossOrientationForSameShortSideTarget() {
        val portrait = requireNotNull(
            VirtualDisplayOverride.derive(412, 915, 412, 420, 1080, 2400, 360),
        )
        val landscape = requireNotNull(
            VirtualDisplayOverride.derive(915, 412, 412, 420, 2400, 1080, 360),
        )

        assertEquals(481, portrait.densityDpi.toLong())
        assertEquals(481, landscape.densityDpi.toLong())
        assertEquals(360, portrait.widthDp.toLong())
        assertEquals(800, landscape.widthDp.toLong())
        assertEquals(360, landscape.heightDp.toLong())
    }

    @Test
    fun targetMatchingSmallestWidthKeepsDisplayEnvironmentIdentity() {
        val result = requireNotNull(
            VirtualDisplayOverride.derive(393, 800, 360, 480, 1080, 2208, 360),
        )

        assertEquals(393, result.widthDp.toLong())
        assertEquals(800, result.heightDp.toLong())
        assertEquals(360, result.smallestWidthDp.toLong())
        assertEquals(480, result.densityDpi.toLong())
        assertEquals(1080, result.widthPx.toLong())
        assertEquals(2208, result.heightPx.toLong())
    }

    @Test
    fun absolutePhysicalPixelPlanIgnoresDriftedSourceDensity() {
        val result = requireNotNull(
            VirtualDisplayPlan.deriveAbsoluteResultFromPhysicalPixels(
                360,
                736,
                360,
                1080,
                2208,
                500,
            ),
        )

        assertEquals(500, result.widthDp.toLong())
        assertEquals(1022, result.heightDp.toLong())
        assertEquals(500, result.smallestWidthDp.toLong())
        assertEquals(346, result.densityDpi.toLong())
        assertEquals(1080, result.widthPx.toLong())
        assertEquals(2208, result.heightPx.toLong())
    }

    private fun displayMetrics(
        width: Int,
        height: Int,
        densityDpi: Int,
        density: Float,
    ): DisplayMetrics {
        val metrics = DisplayMetrics()
        metrics.widthPixels = width
        metrics.heightPixels = height
        metrics.densityDpi = densityDpi
        metrics.density = density
        metrics.scaledDensity = density
        return metrics
    }

    private fun setTargetPackageName(packageName: String?) {
        try {
            val field: Field =
                DisplayHookInstaller::class.java.getDeclaredField("targetPackageName")
            field.isAccessible = true
            field.set(null, packageName)
        } catch (exception: ReflectiveOperationException) {
            throw AssertionError(exception)
        }
    }

    private fun setCurrentPackageResolver() {
        try {
            val method: Method = VirtualDisplayOverrideTest::class.java.getDeclaredMethod(
                "testCurrentPackageName",
            )
            method.isAccessible = true
            val resolverField = DisplayHookInstaller::class.java.getDeclaredField(
                "currentPackageNameMethod",
            )
            resolverField.isAccessible = true
            resolverField.set(null, method)
            val unavailableField = DisplayHookInstaller::class.java.getDeclaredField(
                "currentPackageNameUnavailable",
            )
            unavailableField.isAccessible = true
            unavailableField.setBoolean(null, false)
        } catch (exception: ReflectiveOperationException) {
            throw AssertionError(exception)
        }
    }

    private fun clearCurrentPackageResolver() {
        try {
            val resolverField = DisplayHookInstaller::class.java.getDeclaredField(
                "currentPackageNameMethod",
            )
            resolverField.isAccessible = true
            resolverField.set(null, null)
            val unavailableField = DisplayHookInstaller::class.java.getDeclaredField(
                "currentPackageNameUnavailable",
            )
            unavailableField.isAccessible = true
            unavailableField.setBoolean(null, false)
        } catch (exception: ReflectiveOperationException) {
            throw AssertionError(exception)
        }
    }

    private fun publishRelativeDisplay() {
        val store = DpisConfigStore(FakePrefs())
        val targetSpec = ViewportTargetSpec.relativeScale(120000)
        store.setTargetViewportSpec("com.max.xiaoheihe", targetSpec)
        store.setSystemServerHooksEnabled(true)
        DisplayHookInstaller.setTargetStoreForLegacy(store)
        VirtualDisplayState.set(VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376))
        ViewportRuntimeMarkerBridge.publish(
            "com.max.xiaoheihe",
            ViewportRuntimeMarkerBridge.createRecord(
                "com.max.xiaoheihe",
                targetSpec,
                432,
                ViewportSourceSnapshot.systemDisplayInfo(360, 792, 360, 480, 1080, 2376),
                ViewportOverride.Result(432, 950, 432, 400),
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
                RuntimeClock.crossProcessMarkerMillis(),
            ),
        )
    }

    private fun publishTargetRecord() {
        val store = DpisConfigStore(FakePrefs())
        val targetSpec = ViewportTargetSpec.absoluteDp(300)
        store.setTargetViewportSpec("com.max.xiaoheihe", targetSpec)
        DisplayHookInstaller.setTargetStoreForLegacy(store)
        VirtualDisplayState.publish(
            "com.max.xiaoheihe",
            targetSpec,
            ViewportSourceSnapshot.systemDisplayInfo(360, 736, 360, 480, 1080, 2208),
            ViewportOverride.Result(300, 613, 300, 576),
            VirtualDisplayOverride.Result(300, 613, 300, 576, 1080, 2208),
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
        )
    }

    private fun request(): Coordinator.Request {
        return Coordinator.Request(
            "com.max.xiaoheihe",
            "Xiaoheihe",
            "1.2.3",
            true,
            true,
            true,
            false,
            ViewportTargetSpec.absoluteDp(300),
            ViewportApplyMode.COMPAT,
            100,
            FontApplyMode.OFF,
            null,
            null,
            null,
        )
    }

    companion object {
        @JvmStatic
        private fun testCurrentPackageName(): String = "com.max.xiaoheihe"
    }
}
