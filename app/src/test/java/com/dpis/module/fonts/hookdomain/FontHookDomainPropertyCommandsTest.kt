package com.dpis.module.fonts.hookdomain

import org.junit.Assert.assertEquals
import org.junit.Test

class FontHookDomainPropertyCommandsTest {
    @Test
    fun publishWritesRuntimeAndPersistentMask() {
        assertEquals(
            "setprop 'debug.dpis.hookdomains.0.0b666619' 'v2:a44631ddc484:25'; " +
                    "setprop 'persist.debug.dpis.hookdomains.0.0b666619' 'v2:a44631ddc484:25'",
            FontHookDomainPropertyCommands.buildPublish(
                TEST_PACKAGE_NAME,
                setOf(
                    FontHookDomainRegistry.ID_TEXTVIEW_CURRENT_PX_FALLBACK,
                    FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK,
                ),
                0,
            ),
        )
    }

    @Test
    fun emptyCustomPathPublishesDistinctNonAutomaticValue() {
        assertEquals(
            "setprop 'debug.dpis.hookdomains.0.0b666619' 'v2:a44631ddc484:1'; " +
                    "setprop 'persist.debug.dpis.hookdomains.0.0b666619' 'v2:a44631ddc484:1'",
            FontHookDomainPropertyCommands.buildPublish(TEST_PACKAGE_NAME, emptySet(), 0),
        )
    }

    @Test
    fun clearWritesAutomaticMarker() {
        assertEquals(
            "setprop 'debug.dpis.hookdomains.0.0b666619' '0'; " +
                    "setprop 'persist.debug.dpis.hookdomains.0.0b666619' '0'",
            FontHookDomainPropertyCommands.buildClear(TEST_PACKAGE_NAME, 0),
        )
    }

    private companion object {
        const val TEST_PACKAGE_NAME = "com.example.dpis.test"
    }
}
