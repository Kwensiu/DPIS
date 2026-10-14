package com.dpis.module.runtime.systemserver

import android.content.res.Configuration
import com.dpis.module.config.PerAppDisplayConfig
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.runtime.systemserver.hook.HookCallback
import com.dpis.module.runtime.systemserver.selection.ConfigSelection
import com.dpis.module.runtime.systemserver.viewport.RelativeViewport
import com.dpis.module.viewport.PerAppDisplayEnvironment
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min
import kotlin.math.roundToInt

class ViewportSelectionTest {
    private fun launch(
        packageName: String,
        configuration: Configuration,
        config: PerAppDisplayConfig,
    ): PerAppDisplayEnvironment = HookInstaller.resolveLaunchViewportEnvironment(
        packageName, configuration, 1080, 2376, config,
    )!!

    @Test
    fun keepsPreEnvironmentWhenPostEnvironmentMissing() {
        val pre = PerAppDisplayEnvironment(320, 704, 320, 540, 1080, 2376)

        assertSame(pre, HookCallback.chooseEffectiveEnvironment(pre, null))
    }

    private fun launchConfig(spec: ViewportTargetSpec): PerAppDisplayConfig = PerAppDisplayConfig(
        "com.example.launch",
        spec,
        ViewportApplyMode.AUTO,
        null,
        FontApplyMode.OFF,
        false,
        HookDomainOverride.automatic(),
    )

    @Test
    fun prefersPostEnvironmentWhenAvailable() {
        val pre = PerAppDisplayEnvironment(320, 704, 320, 540, 1080, 2376)
        val post = PerAppDisplayEnvironment(360, 736, 360, 480, 1080, 2376)

        assertSame(post, HookCallback.chooseEffectiveEnvironment(pre, post))
    }

    @Test
    fun recognizesRelativeScaleMarkerResultAsAlreadyApplied() {
        val configuration = Configuration().apply {
            screenWidthDp = 1215
            screenHeightDp = 2484
            smallestScreenWidthDp = 1215
            densityDpi = 142
        }
        val record = ViewportRuntimeMarkerBridge.createRecord(
            "com.tencent.mm",
            ViewportTargetSpec.relativeScale(150000),
            1215,
            ViewportSourceSnapshot.systemDisplayInfo(810, 1656, 810, 213, 1080, 2208),
            ViewportOverride.Result(1215, 2484, 1215, 142),
            "s",
            1000L,
        )

        assertTrue(
            RelativeViewport.isAlreadyApplied(
                configuration,
                ViewportSourceSnapshot.SCOPE_DISPLAY,
                ViewportRuntimeMarkerBridge.ParseResult.hit(record, 10L),
            ),
        )
    }

    private fun panelConfiguration(densityDpi: Int) = Configuration().apply {
        this.densityDpi = densityDpi
        screenWidthDp = (1080 * 160f / densityDpi).roundToInt()
        screenHeightDp = (2376 * 160f / densityDpi).roundToInt()
        smallestScreenWidthDp = min(screenWidthDp, screenHeightDp)
    }

    private fun configurationFrom(environment: PerAppDisplayEnvironment) = Configuration().apply {
        screenWidthDp = environment.widthDp
        screenHeightDp = environment.heightDp
        smallestScreenWidthDp = environment.smallestWidthDp
        densityDpi = environment.densityDpi
    }

    @Test
    fun rejectsRelativeScaleMarkerWhenSourceIsNotResult() {
        val configuration = Configuration().apply {
            screenWidthDp = 810
            screenHeightDp = 1656
            smallestScreenWidthDp = 810
            densityDpi = 213
        }
        val record = ViewportRuntimeMarkerBridge.createRecord(
            "com.tencent.mm",
            ViewportTargetSpec.relativeScale(150000),
            1215,
            ViewportSourceSnapshot.systemDisplayInfo(810, 1656, 810, 213, 1080, 2208),
            ViewportOverride.Result(1215, 2484, 1215, 142),
            "s",
            1000L,
        )

        assertFalse(
            RelativeViewport.isAlreadyApplied(
                configuration,
                ViewportSourceSnapshot.SCOPE_DISPLAY,
                ViewportRuntimeMarkerBridge.ParseResult.hit(record, 10L),
            ),
        )
    }

    @Test
    fun systemServerUsesViewportOrFontEmulationConfig() {
        val fontOnlyEmulation = PerAppDisplayConfig(
            "com.example.target", null, 120, FontApplyMode.SYSTEM_EMULATION,
        )
        val fontOnlyRewrite = PerAppDisplayConfig(
            "com.example.target", null, 120, FontApplyMode.FIELD_REWRITE,
        )
        val viewport = PerAppDisplayConfig("com.example.target", 360)
        val autoViewport = config(ViewportTargetSpec.absoluteDp(360), ViewportApplyMode.AUTO)
        val compatViewport = config(ViewportTargetSpec.absoluteDp(360), ViewportApplyMode.COMPAT)
        val relativeViewport =
            config(ViewportTargetSpec.relativeScale(150000), ViewportApplyMode.AUTO)
        val explicitSystemRelativeViewport = config(
            ViewportTargetSpec.relativeScale(150000), ViewportApplyMode.SYSTEM,
        )
        val relativeViewportWithFont = PerAppDisplayConfig(
            "com.example.target",
            ViewportTargetSpec.relativeScale(150000),
            ViewportApplyMode.AUTO,
            120,
            FontApplyMode.SYSTEM_EMULATION,
            false,
            HookDomainOverride.automatic(),
        )

        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry(null, fontOnlyEmulation))
        assertNull(ConfigSelection.selectConfigForSystemServerEntry(null, fontOnlyRewrite))
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry(null, viewport))
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry(null, autoViewport))
        assertNull(ConfigSelection.selectConfigForSystemServerEntry(null, compatViewport))
        assertNotNull(ConfigSelection.selectConfigForSystemServerEntry(null, relativeViewport))
        assertNotNull(
            ConfigSelection.selectConfigForSystemServerEntry(null, explicitSystemRelativeViewport),
        )
        assertNotNull(
            ConfigSelection.selectConfigForSystemServerEntry(
                null,
                relativeViewportWithFont
            )
        )
    }

    private fun config(spec: ViewportTargetSpec, mode: String) = PerAppDisplayConfig(
        "com.example.target", spec, mode, null, FontApplyMode.OFF, false,
        HookDomainOverride.automatic(),
    )

    @Test
    fun secondLaunchPassKeepsAbsoluteWidthWhenPixelsDoNotDivideEvenly() {
        ViewportRuntimeMarkerBridge.clearForTest()
        val config = launchConfig(ViewportTargetSpec.absoluteDp(320))
        val first = panelConfiguration(420)
        val once = launch("com.example.absolute", first, config)
        val twice = launch("com.example.absolute", configurationFrom(once), config)

        assertEquals(320, once.smallestWidthDp)
        assertEquals(once.smallestWidthDp, twice.smallestWidthDp)
        assertEquals(once.widthDp, twice.widthDp)
        assertEquals(once.heightDp, twice.heightDp)
        assertEquals(once.densityDpi, twice.densityDpi)
    }

    @Test
    fun secondLaunchPassDoesNotCompoundRelativeScale() {
        ViewportRuntimeMarkerBridge.clearForTest()
        val config = launchConfig(ViewportTargetSpec.relativeScale(120000))
        val first = panelConfiguration(420)
        val once = launch("com.example.relative", first, config)
        val twice = launch("com.example.relative", configurationFrom(once), config)

        assertTrue(once.smallestWidthDp > first.smallestScreenWidthDp)
        assertEquals(once.smallestWidthDp, twice.smallestWidthDp)
        assertEquals(once.widthDp, twice.widthDp)
        assertEquals(once.heightDp, twice.heightDp)
        assertEquals(once.densityDpi, twice.densityDpi)
    }
}
