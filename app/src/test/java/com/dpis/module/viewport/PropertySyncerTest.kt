package com.dpis.module.viewport

import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportPropertySyncer
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class PropertySyncerTest {
    @Test
    fun viewportSyncPublishesCompatConfigWithoutEnablingSystemEmulation() {
        val command = ViewportPropertySyncer.buildCompatConfigCommandForTest(
            "com.max.xiaoheihe", 300, ViewportApplyMode.COMPAT,
        )
        assertEquals(expectedCommand("0", "absolute_dp", "0", "300", "compat"), command)
    }

    @Test
    fun viewportSystemEmulationPublishesBothRuntimeAndCompatConfig() {
        val command = ViewportPropertySyncer.buildCompatConfigCommandForTest(
            "com.max.xiaoheihe", 300, ViewportApplyMode.SYSTEM,
        )
        assertEquals(expectedCommand("300", "absolute_dp", "0", "300", "system"), command)
    }

    @Test
    fun viewportRelativeScalePublishesScaleWithoutWidthOnlyValues() {
        val command = ViewportPropertySyncer.buildCompatConfigCommandForTest(
            "com.max.xiaoheihe", ViewportTargetSpec.relativeScale(125000), ViewportApplyMode.SYSTEM,
        )
        assertEquals(expectedCommand("0", "relative_scale", "125000", "0", "system"), command)
    }

    @Test
    fun viewportOffOrInvalidWidthClearsRuntimeAndCompatConfig() {
        assertEquals(
            expectedCommand("0", "off", "0", "0", "off"),
            ViewportPropertySyncer.buildCompatConfigCommandForTest(
                "com.max.xiaoheihe", 300, ViewportApplyMode.OFF,
            ),
        )
        assertEquals(
            expectedCommand("0", "off", "0", "0", "off"),
            ViewportPropertySyncer.buildCompatConfigCommandForTest(
                "com.max.xiaoheihe", 0, ViewportApplyMode.COMPAT,
            ),
        )
    }

    @Test
    fun viewportBoundaryWidthsArePreserved() {
        assertEquals(
            expectedCommand("0", "absolute_dp", "0", "1", "compat"),
            ViewportPropertySyncer.buildCompatConfigCommandForTest(
                "com.max.xiaoheihe", 1, ViewportApplyMode.COMPAT,
            ),
        )
        assertEquals(
            expectedCommand("0", "absolute_dp", "0", "9999", "compat"),
            ViewportPropertySyncer.buildCompatConfigCommandForTest(
                "com.max.xiaoheihe", 9999, ViewportApplyMode.COMPAT,
            ),
        )
    }

    private fun expectedCommand(
        viewport: String, targetType: String, scalePermille: String, compatConfig: String, mode: String,
    ): String =
        "setprop 'debug.dpis.vp.eab4efd3' '$viewport'; " +
            "setprop 'persist.debug.dpis.vp.eab4efd3' '$viewport'; " +
            "setprop 'debug.dpis.vptype.eab4efd3' '$targetType'; " +
            "setprop 'persist.debug.dpis.vptype.eab4efd3' '$targetType'; " +
            "setprop 'debug.dpis.vpscale.eab4efd3' '$scalePermille'; " +
            "setprop 'persist.debug.dpis.vpscale.eab4efd3' '$scalePermille'; " +
            "setprop 'debug.dpis.vpcfg.eab4efd3' '$compatConfig'; " +
            "setprop 'persist.debug.dpis.vpcfg.eab4efd3' '$compatConfig'; " +
            "setprop 'debug.dpis.vpmode.eab4efd3' '$mode'; " +
            "setprop 'persist.debug.dpis.vpmode.eab4efd3' '$mode'"
}
