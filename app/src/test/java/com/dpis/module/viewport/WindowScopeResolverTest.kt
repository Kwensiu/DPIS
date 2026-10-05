package com.dpis.module.viewport

import com.dpis.module.viewport.window.WindowMode
import com.dpis.module.viewport.window.WindowScopeDecision
import com.dpis.module.viewport.window.WindowScopeDetector
import com.dpis.module.viewport.window.WindowScopeEvidence
import com.dpis.module.viewport.window.WindowScopeInput
import com.dpis.module.viewport.window.WindowScopeKind
import com.dpis.module.viewport.window.WindowScopeResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowScopeResolverTest {
    @Test
    fun additionalDetectorCanBeRegisteredWithoutChangingResolverApi() {
        try {
            WindowScopeResolver.register(object : WindowScopeDetector {
                override fun detect(input: WindowScopeInput): WindowScopeDecision =
                    WindowScopeDecision(WindowScopeKind.WINDOW, WindowScopeEvidence.PLATFORM_BOUNDS)
            })

            val decision = WindowScopeResolver.resolve(null)

            assertEquals(WindowScopeKind.WINDOW, decision.kind)
            assertEquals(WindowScopeEvidence.PLATFORM_BOUNDS, decision.evidence)
        } finally {
            WindowScopeResolver.resetForTest()
        }
    }

    @Test
    fun colorOsTaskEvidenceIsPreferredWhenAvailable() {
        val decision = WindowScopeResolver.resolve(null, ColorOsTask())

        assertEquals(WindowScopeKind.WINDOW, decision.kind)
        assertEquals(WindowScopeEvidence.COLOROS_TASK_UTILS, decision.evidence)
        assertEquals(WindowMode.FREEFORM, decision.mode)
        assertTrue(ViewportConfigurationScope.isWindowScoped(null, ColorOsTask()))
    }

    @Test
    fun missingConfigurationAndTaskRemainUnknown() {
        val decision = WindowScopeResolver.resolve(null)

        assertEquals(WindowScopeKind.UNKNOWN, decision.kind)
        assertEquals(null, decision.evidence)
    }

    private class ColorOsTask {
        @Suppress("unused")
        fun getWindowingMode(): Int = 5
    }
}
