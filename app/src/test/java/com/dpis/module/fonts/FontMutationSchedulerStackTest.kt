package com.dpis.module.fonts

import org.junit.Assert.assertEquals
import org.junit.Test

class FontMutationSchedulerStackTest {
    @Test
    fun withMutationExposesTargetAndRestoresAfterward() {
        FontMutationScheduler.resetForHotReload()

        val nested = FontMutationScheduler.withMutation(28.2f, 0.94f) {
            assertEquals(28.2f, FontMutationScheduler.currentTransactionTarget())
            FontMutationScheduler.decide(
                28.2f,
                28.2f,
                26.508f,
                0.94f,
                true,
                FontMutationScheduler.currentTransactionTarget(),
                false,
            )
        }

        assertEquals(FontMutationScheduler.Action.PASS_THROUGH, nested.action())
        assertEquals(null, FontMutationScheduler.currentTransactionTarget())
    }

    @Test
    fun withMutationRestoresStackWhenBlockThrows() {
        FontMutationScheduler.resetForHotReload()

        try {
            FontMutationScheduler.withMutation(28.2f, 0.94f) {
                error("setter")
            }
        } catch (_: IllegalStateException) {
            // Expected: the stack must still be cleared by finally.
        }

        assertEquals(null, FontMutationScheduler.currentTransactionTarget())
    }
}
