package com.dpis.module.runtime.systemserver

import android.content.res.Configuration
import com.dpis.module.config.PerAppDisplayConfig
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.runtime.systemserver.diagnostics.HookLog
import com.dpis.module.runtime.systemserver.diagnostics.HookLogGate
import com.dpis.module.runtime.systemserver.diagnostics.HotPathInspector
import com.dpis.module.runtime.systemserver.mutation.FontScale
import com.dpis.module.runtime.systemserver.mutation.MutationField
import com.dpis.module.runtime.systemserver.mutation.RelayoutWindow
import com.dpis.module.runtime.systemserver.selection.ConfigSelection
import com.dpis.module.runtime.systemserver.viewport.RelativeViewport
import com.dpis.module.runtime.webapk.WebApkCarrierResolver
import com.dpis.module.viewport.PerAppDisplayEnvironment
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge
import com.dpis.module.viewport.ViewportRuntimeRecord
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MutationPolicyTest {
    @Test
    fun mutationPhasesAndFieldsFollowEntryPolicy() {
        assertTrue(MutationPolicy.shouldApplyPreProceedMutations("config-dispatch"))
        assertTrue(MutationPolicy.shouldApplyPreProceedMutations("activity-start"))
        assertFalse(MutationPolicy.shouldApplyPreProceedMutations("display-policy-layout"))
        assertFalse(MutationPolicy.shouldApplyPostProceedMutations("config-dispatch"))
        assertTrue(MutationPolicy.shouldApplyPostProceedMutations("activity-start"))
        assertTrue(MutationPolicy.shouldApplyPostProceedMutations("display-content-config"))
        val viewportEntries = listOf(
            "launch-activity-item", "config-dispatch", "activity-start",
            "display-content-config", "display-policy-layout", "relayout-dispatch",
        )
        viewportEntries.forEach {
            assertTrue(
                MutationPolicy.shouldApplyMutationField(
                    it,
                    MutationField.VIEWPORT
                )
            )
        }
        assertTrue(
            MutationPolicy.shouldApplyMutationField(
                "launch-activity-item",
                MutationField.FONT_SCALE
            )
        )
        (viewportEntries - "launch-activity-item").forEach {
            assertFalse(MutationPolicy.shouldApplyMutationField(it, MutationField.FONT_SCALE))
        }
    }

    @Test
    fun hotEntryLoggingAndInspectionAreBoundedByRiskAndConfiguredPackage() {
        assertFalse(HookLogGate.shouldLogInterceptEnter("display-policy-layout"))
        assertFalse(HookLogGate.shouldLogInterceptEnter("relayout-dispatch"))
        assertTrue(HookLogGate.shouldLogInterceptEnter("activity-start"))
        assertTrue(HookLogGate.shouldLogInterceptEnter("config-dispatch"))
        assertEquals(1200L, HookLogGate.resolveLogMinIntervalMs("display-policy-layout"))
        assertEquals(1200L, HookLogGate.resolveLogMinIntervalMs("relayout-dispatch"))
        assertEquals(800L, HookLogGate.resolveLogMinIntervalMs("activity-start"))
        assertEquals(800L, HookLogGate.resolveLogMinIntervalMs("config-dispatch"))
        assertEquals(400L, HookLogGate.resolveLogMinIntervalMs("unknown-entry"))

        val configured = setOf("com.max.xiaoheihe")
        assertFalse(
            RelayoutWindow.shouldInspectHotEntryForTest(
                "display-policy-layout",
                FakeWindow("Window{u0 com.android.launcher/Launcher}"),
                configured
            )
        )
        assertTrue(
            RelayoutWindow.shouldInspectHotEntryForTest(
                "display-policy-layout",
                FakeWindow("Window{u0 com.max.xiaoheihe/MainActivity}"),
                configured
            )
        )
        assertTrue(
            RelayoutWindow.shouldInspectHotEntryForTest(
                "activity-start", FakeWindow("Window{u0 com.android.launcher/Launcher}"), configured
            )
        )
    }

    @Test
    fun hotEntryInspectionRecognizesWebApkOwnerAndLaterRelayoutArguments() {
        val owner = "org.chromium.webapk.a5e359e2ce8b830bb_v2"
        assertTrue(
            RelayoutWindow.shouldInspectHotEntryForTest(
                "display-policy-layout",
                FakeWindow(
                    "Window{u0 com.android.chrome/SameTaskWebApkActivity " +
                            "${WebApkCarrierResolver.WEBAPK_PACKAGE_EXTRA}=$owner}"
                ), setOf(owner)
            )
        )
        assertTrue(
            HotPathInspector.shouldInspectHotEntry(
                "relayout-dispatch", "WindowManagerService",
                listOf(
                    "session", "client", "attrs", "requestedWidth", "requestedHeight",
                    "Window{u0 com.android.chrome/Main}"
                ), setOf("com.android.chrome")
            )
        )
    }

    @Test
    fun relayoutInspectionUsesResolvedOwnerAndMutationRequiresDirectTarget() {
        assertFalse(
            RelayoutWindow.shouldInspectResolvedRelayoutTargetForTest(
                FakeWindow("WindowStateWithoutPackageText"), setOf("com.android.chrome")
            )
        )
        assertTrue(
            RelayoutWindow.shouldInspectResolvedRelayoutTargetForTest(
                FakeWindow("WindowStateWithoutPackageText", packageName = "com.android.chrome"),
                setOf("com.android.chrome")
            )
        )
        assertTrue(
            RelayoutWindow.canApplyRelayoutMutationForTest(
                FakeWindow(
                    "WindowStateWithoutPackageText", "com.android.chrome", 1,
                    "com.android.chrome", null
                ), "com.android.chrome"
            )
        )
        assertFalse(
            RelayoutWindow.canApplyRelayoutMutationForTest(
                FakeWindow(
                    "WindowStateWithoutPackageText", "com.other.app", 1,
                    "com.other.app", null
                ), "com.android.chrome"
            )
        )
        assertFalse(
            RelayoutWindow.canApplyRelayoutMutationForTest(
                FakeWindow(
                    "Window{u0 com.android.systemui/NotificationShade for com.android.chrome}",
                    "com.android.systemui", 2040, "com.android.chrome", null
                ), "com.android.chrome"
            )
        )
        assertTrue(
            RelayoutWindow.canApplyRelayoutMutationForTest(
                FakeWindow(
                    "Window{u0 com.android.systemui/NotificationShade}",
                    "com.android.systemui", 2040, "com.android.systemui", null
                ), "com.android.systemui"
            )
        )
        assertTrue(
            RelayoutWindow.canApplyRelayoutMutationForTest(
                FakeWindow("WindowStateWithoutPackageText", null, 1, null, "com.android.chrome"),
                "com.android.chrome"
            )
        )
        assertFalse(
            RelayoutWindow.canApplyRelayoutMutationForTest(
                FakeWindow("Window{u0 com.android.chrome/Main}"), "com.android.chrome"
            )
        )
    }

    @Test
    fun safeModeLimitsRiskyTargetsWhileNormalModeInstallsAll() {
        val targets = listOf(
            "display-policy-layout", "display-content-config", "relayout-dispatch",
            "activity-start", "config-dispatch", "launch-activity-item", "display-manager-info",
            "hyperos-rust-process"
        )
        assertFalse(MutationPolicy.shouldInstallTarget("display-policy-layout", true))
        assertFalse(MutationPolicy.shouldInstallTarget("display-content-config", true))
        targets.filterNot { it in setOf("display-policy-layout", "display-content-config") }
            .forEach { assertTrue(MutationPolicy.shouldInstallTarget(it, true)) }
        targets.forEach { assertTrue(MutationPolicy.shouldInstallTarget(it, false)) }
    }

    @Test
    fun relativeScaleMaintenanceRequiresCompleteCurrentMarkerOrFreshDisplayBaseline() {
        val spec = ViewportTargetSpec.relativeScale(150000)
        val complete = markerRecord(spec)
        val incomplete = ViewportRuntimeMarkerBridge.MarkerRecord(
            "pkg", spec.fingerprint(), "source", 540, "result",
            ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER, 1L,
        )
        assertTrue(
            RelativeViewport.hasCompleteMarkerResult(
                ViewportRuntimeMarkerBridge.ParseResult.hit(complete, 0L)
            )
        )
        assertFalse(
            RelativeViewport.hasCompleteMarkerResult(
                ViewportRuntimeMarkerBridge.ParseResult.hit(incomplete, 0L)
            )
        )
        assertTrue(
            RelativeViewport.canDeriveFromSystemServerSource(
                configuration(), ViewportRuntimeMarkerBridge.ParseResult.miss("empty")
            )
        )
        assertFalse(
            RelativeViewport.canDeriveFromSystemServerSource(
                configuration(), ViewportRuntimeMarkerBridge.ParseResult.hit(incomplete, 0L)
            )
        )
        assertFalse(
            RelativeViewport.canDeriveFromSystemServerSource(
                configuration(), ViewportRuntimeMarkerBridge.ParseResult.miss("stale")
            )
        )
        assertTrue(RelativeViewport.isStaleMarker(ViewportRuntimeMarkerBridge.ParseResult.miss("stale")))
    }

    @Test
    fun windowMaintenanceReusesOnlyMatchingMarkerResultAndOrientation() {
        val spec = ViewportTargetSpec.relativeScale(150000)
        val marker = ViewportRuntimeMarkerBridge.ParseResult.hit(markerRecord(spec), 0L)
        assertTrue(
            RelativeViewport.environmentMatchesMarkerResult(
                PerAppDisplayEnvironment(540, 1188, 540, 320, 1080, 2376), marker
            )
        )
        assertFalse(
            RelativeViewport.environmentMatchesMarkerResult(
                PerAppDisplayEnvironment(360, 640, 360, 480, 1080, 2376), marker
            )
        )
        val portrait = configuration()
        val landscape = Configuration().apply {
            screenWidthDp = 792
            screenHeightDp = 360
            smallestScreenWidthDp = 360
            densityDpi = 480
        }
        assertTrue(RelativeViewport.canReuseCompleteMarkerResult(portrait, marker))
        assertFalse(RelativeViewport.canReuseCompleteMarkerResult(landscape, marker))
    }

    @Test
    fun systemServerConfigSelectionRequiresAnApplicableViewportOrFontField() {
        val viewport = config(
            "com.example.viewport", ViewportTargetSpec.absoluteDp(600),
            ViewportApplyMode.SYSTEM, null, FontApplyMode.OFF
        )
        val font = config(
            "com.example.font", ViewportTargetSpec.off(),
            ViewportApplyMode.OFF, 150, FontApplyMode.SYSTEM_EMULATION
        )
        val neither = config(
            "com.example.neither", ViewportTargetSpec.off(),
            ViewportApplyMode.OFF, null, FontApplyMode.OFF
        )
        val both = config(
            "com.example.both", ViewportTargetSpec.absoluteDp(600),
            ViewportApplyMode.SYSTEM, 150, FontApplyMode.SYSTEM_EMULATION
        )
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry(null, viewport))
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry(null, font))
        assertNull(ConfigSelection.selectConfigForSystemServerEntry(null, neither))
        assertNotNull(
            ConfigSelection.selectConfigForSystemServerEntry(
                "launch-activity-item",
                font
            )
        )
        assertNull(ConfigSelection.selectConfigForSystemServerEntry("config-dispatch", font))
        assertNull(ConfigSelection.selectConfigForSystemServerEntry("display-manager-info", font))
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry("config-dispatch", viewport))
        assertNotNull(
            ConfigSelection.selectConfigForSystemServerEntry(
                "display-manager-info",
                viewport
            )
        )
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry("config-dispatch", both))
    }

    @Test
    fun logDeduplicationAndFontFallbackRespectTheirPolicies() {
        assertTrue(HookLog.shouldEmitLog("a", "b", 1000L, 900L, 0L))
        assertFalse(HookLog.shouldEmitLog("same", "same", 1000L, 0L, 1200L))
        assertFalse(HookLog.shouldEmitLog("old", "new", 1500L, 1000L, 1200L))
        assertTrue(HookLog.shouldEmitLog("old", "new", 2300L, 1000L, 1200L))
        assertTrue(
            FontScale.isDisabledForTest(
                "com.ss.android.ugc.aweme",
                "com.ss.android.ugc.aweme"
            )
        )
        assertTrue(FontScale.isDisabledForTest("com.ss.android.ugc.aweme", "*"))
        assertFalse(FontScale.isDisabledForTest("com.ss.android.ugc.aweme", "tv.danmaku.bili"))
        assertFalse(FontScale.isDisabledForTest("com.ss.android.ugc.aweme", ""))
        val system = config(
            "com.ss.android.ugc.aweme", ViewportTargetSpec.off(),
            ViewportApplyMode.OFF, 80, FontApplyMode.SYSTEM_EMULATION
        )
        val compat = config(
            "com.ss.android.ugc.aweme", ViewportTargetSpec.off(),
            ViewportApplyMode.OFF, 80, FontApplyMode.FIELD_REWRITE
        )
        assertTrue(FontScale.shouldYieldForTest(system, "com.ss.android.ugc.aweme"))
        assertFalse(FontScale.shouldYieldForTest(compat, "com.ss.android.ugc.aweme"))
        assertFalse(FontScale.shouldYieldForTest(system, "tv.danmaku.bili"))
    }

    @Test
    fun fontMutationIgnoresCompatHookDomainOverrides() {
        val automatic = config(
            "tv.danmaku.bili", ViewportTargetSpec.off(),
            ViewportApplyMode.OFF, 150, FontApplyMode.SYSTEM_EMULATION
        )
        val custom = PerAppDisplayConfig(
            "tv.danmaku.bili", ViewportTargetSpec.off(), ViewportApplyMode.OFF, 150,
            FontApplyMode.SYSTEM_EMULATION, false,
            HookDomainOverride(
                true,
                setOf("resources_font", "activity_thread_font", "webview_text_zoom"),
                emptySet()
            ),
        )
        assertTrue(ConfigSelection.hasSystemServerFontOverride(automatic))
        assertTrue(ConfigSelection.hasSystemServerFontOverride(custom))
    }

    private fun markerRecord(spec: ViewportTargetSpec) = ViewportRuntimeMarkerBridge.MarkerRecord(
        "pkg", spec.fingerprint(),
        ViewportRuntimeMarkerBridge.configurationSignature(
            360, 792, 360, 480,
            ViewportSourceSnapshot.SCOPE_DISPLAY
        ),
        540,
        ViewportRuntimeMarkerBridge.configurationSignature(
            540, 1188, 540, 320,
            ViewportSourceSnapshot.SCOPE_DISPLAY
        ),
        540, 1188, 540, 320, ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER, 1L,
    )

    private fun configuration() = Configuration().apply {
        screenWidthDp = 360
        screenHeightDp = 792
        smallestScreenWidthDp = 360
        densityDpi = 480
    }

    private fun config(
        packageName: String,
        spec: ViewportTargetSpec,
        viewportMode: String,
        fontScale: Int?,
        fontMode: String,
    ) = PerAppDisplayConfig(
        packageName, spec, viewportMode, fontScale, fontMode, false,
        HookDomainOverride.automatic(),
    )

    private class FakeWindow(
        private val text: String,
        packageName: String? = null,
        windowType: Int = 0,
        activityPackageName: String? = null,
        tokenPackageName: String? = null,
    ) {
        @Suppress("unused")
        private val mAttrs = packageName?.let { FakeLayoutParams(it, windowType) }

        @Suppress("unused")
        private val mActivityRecord = activityPackageName?.let(::FakePackageOwner)

        @Suppress("unused")
        private val mToken = tokenPackageName?.let(::FakePackageOwner)
        override fun toString(): String = text
    }

    private data class FakeLayoutParams(val packageName: String, val type: Int)
    private data class FakePackageOwner(val packageName: String)
}
