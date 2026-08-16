package com.dpis.module.fonts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FontMutationSchedulerTest {
    @Test
    public void keepsCurrentTargetWhenIncomingValueIsTheUnscaledBase() {
        FontMutationScheduler.Decision decision = FontMutationScheduler.decide(
                18f, 16.74f, 16.74f, 0.93f, false);

        assertEquals(FontMutationScheduler.Action.KEEP_CURRENT, decision.action());
    }

    @Test
    public void appliesNewTargetAfterExternalDrift() {
        FontMutationScheduler.Decision decision = FontMutationScheduler.decide(
                18f, 18f, 16.74f, 0.93f, false);

        assertEquals(FontMutationScheduler.Action.APPLY, decision.action());
        assertEquals(16.74f, decision.targetPx(), 0.0001f);
    }

    @Test
    public void observesWhenStrongerDomainOwnsTheMutation() {
        FontMutationScheduler.Decision decision = FontMutationScheduler.decide(
                18f, 16.74f, 16.74f, 0.93f, true);

        assertEquals(FontMutationScheduler.Action.OBSERVE, decision.action());
    }
}
