package com.dpis.module.runtime.font

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.runtime.font.CompatFontPropertySyncer
import org.junit.Assert.assertEquals
import org.junit.Test

class CompatPropertySyncerTest {
    @Test
    fun fontSyncPublishesCompatModeWithoutEnablingSystemEmulation() {
        val command = CompatFontPropertySyncer.buildCompatConfigCommandForTest(
            "com.max.xiaoheihe", 200, FontApplyMode.FIELD_REWRITE,
        )
        assertEquals(expectedCommand("0", "field_rewrite", "200"), command)
    }

    @Test
    fun fontSystemEmulationPublishesRuntimeValueAndMode() {
        val command = CompatFontPropertySyncer.buildCompatConfigCommandForTest(
            "com.max.xiaoheihe", 200, FontApplyMode.SYSTEM_EMULATION,
        )
        assertEquals(expectedCommand("200", "system_emulation", "0"), command)
    }

    @Test
    fun fontOffOrInvalidPercentClearsRuntimeAndMode() {
        assertEquals(
            expectedCommand("0", "off", "0"),
            CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                "com.max.xiaoheihe", 200, FontApplyMode.OFF,
            ),
        )
        assertEquals(
            expectedCommand("0", "off", "0"),
            CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                "com.max.xiaoheihe", 0, FontApplyMode.FIELD_REWRITE,
            ),
        )
    }

    @Test
    fun fontBoundaryPercentsArePreserved() {
        for (percent in listOf(50, 100, 300)) {
            assertEquals(
                expectedCommand("0", "field_rewrite", percent.toString()),
                CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                    "com.max.xiaoheihe", percent, FontApplyMode.FIELD_REWRITE,
                ),
            )
        }
    }

    private fun expectedCommand(compatFont: String, mode: String, forceFont: String): String =
        "setprop 'debug.dpis.compatfont.eab4efd3' '$compatFont'; " +
            "setprop 'persist.debug.dpis.compatfont.eab4efd3' '$compatFont'; " +
            "setprop 'debug.dpis.fontmode.eab4efd3' '$mode'; " +
            "setprop 'persist.debug.dpis.fontmode.eab4efd3' '$mode'; " +
            "setprop 'debug.dpis.forcefont.eab4efd3' '$forceFont'; " +
            "setprop 'persist.debug.dpis.forcefont.eab4efd3' '$forceFont'"
}
