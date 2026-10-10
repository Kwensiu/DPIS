package com.dpis.module.diagnostics.device

import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundAppReaderTest {
    @Test
    fun resumedActivityIdentifiesTheForegroundPackage() {
        val output = "mResumedActivity: ActivityRecord{ u0 com.example.target/.MainActivity t42}"

        assertEquals("com.example.target", ForegroundAppReader.parsePackage(output))
    }

    @Test
    fun focusedWindowIdentifiesTheForegroundPackage() {
        val output = "mCurrentFocus=Window{ u0 com.dpis.module/com.dpis.module.MainActivity}"

        assertEquals("com.dpis.module", ForegroundAppReader.parsePackage(output))
    }

    @Test
    fun blankOutputHasNoForegroundPackage() {
        assertEquals("", ForegroundAppReader.parsePackage(""))
    }
}
