package com.dpis.module.viewport

import org.junit.Assert.assertEquals
import org.junit.Test

class PropertySyncerTest {
    @Test
    fun viewportPropertiesArePublishedToTheSelectedAndroidUserNamespace() {
        val primaryUser = ViewportPropertySyncer.buildCompatConfigCommandForTest(
            "com.max.xiaoheihe",
            ViewportTargetSpec.relativeScale(80000),
            ViewportApplyMode.SYSTEM,
            0,
        )
        val multiAppUser = ViewportPropertySyncer.buildCompatConfigCommandForTest(
            "com.max.xiaoheihe",
            ViewportTargetSpec.relativeScale(160000),
            ViewportApplyMode.SYSTEM,
            999,
        )

        assertEquals(expectedCommand("0", "relative_scale", "80000", "0", "system", 0), primaryUser)
        assertEquals(
            expectedCommand("0", "relative_scale", "160000", "0", "system", 999),
            multiAppUser
        )
    }

    @Test
    fun viewportReadUsesOnlyTheRequestedAndroidUserNamespace() {
        val properties = mapOf(
            ViewportPropertyBridge.propertyNameForPackage("com.example.browser", 0) to "360",
            ViewportPropertyBridge.targetTypePropertyNameForPackage(
                "com.example.browser",
                0
            ) to "absolute_dp",
            ViewportPropertyBridge.compatModePropertyNameForPackage(
                "com.example.browser",
                0
            ) to "system",
            ViewportPropertyBridge.propertyNameForPackage("com.example.browser", 999) to "720",
            ViewportPropertyBridge.targetTypePropertyNameForPackage(
                "com.example.browser",
                999
            ) to "absolute_dp",
            ViewportPropertyBridge.compatModePropertyNameForPackage(
                "com.example.browser",
                999
            ) to "system",
        )
        val reader: (String) -> String? = properties::get

        assertEquals(
            ViewportTargetSpec.absoluteDp(360),
            ViewportPropertyBridge.readTargetSpecForTest("com.example.browser", 0, reader),
        )
        assertEquals(
            ViewportTargetSpec.absoluteDp(720),
            ViewportPropertyBridge.readTargetSpecForTest("com.example.browser", 999, reader),
        )
        assertEquals(
            ViewportTargetSpec.off(),
            ViewportPropertyBridge.readTargetSpecForTest("com.example.browser", 10, reader),
        )
    }

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
            "com.max.xiaoheihe",
            ViewportTargetSpec.relativeScale(125000),
            ViewportApplyMode.SYSTEM,
            0,
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
        viewport: String,
        targetType: String,
        scalePermille: String,
        compatConfig: String,
        mode: String,
        userId: Int = 0,
    ): String {
        val propertyHash = "$userId.eab4efd3"
        return "setprop 'debug.dpis.vp.$propertyHash' '$viewport'; " +
                "setprop 'persist.debug.dpis.vp.$propertyHash' '$viewport'; " +
                "setprop 'debug.dpis.vptype.$propertyHash' '$targetType'; " +
                "setprop 'persist.debug.dpis.vptype.$propertyHash' '$targetType'; " +
                "setprop 'debug.dpis.vpscale.$propertyHash' '$scalePermille'; " +
                "setprop 'persist.debug.dpis.vpscale.$propertyHash' '$scalePermille'; " +
                "setprop 'debug.dpis.vpcfg.$propertyHash' '$compatConfig'; " +
                "setprop 'persist.debug.dpis.vpcfg.$propertyHash' '$compatConfig'; " +
                "setprop 'debug.dpis.vpmode.$propertyHash' '$mode'; " +
                "setprop 'persist.debug.dpis.vpmode.$propertyHash' '$mode'"
    }
}
