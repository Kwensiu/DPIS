package com.dpis.module.fonts;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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

    @Test
    public void transactionTargetPassesThroughBeforeStrongerOwner() {
        FontMutationScheduler.Decision decision = FontMutationScheduler.decide(
                28.2f, 28.2f, 26.508f, 0.94f, true, 28.2f, false);

        assertEquals(FontMutationScheduler.Action.PASS_THROUGH, decision.action());
        assertEquals(0f, decision.targetPx(), 0.0001f);
    }

    @Test
    public void alreadyAppliedTargetKeepsCurrentValue() {
        FontMutationScheduler.Decision decision = FontMutationScheduler.decide(
                28.2f, 28.2f, 26.508f, 0.94f, false, null, true);

        assertEquals(FontMutationScheduler.Action.KEEP_CURRENT, decision.action());
    }

    @Test
    public void independentIncomingStillAppliesScaledTarget() {
        FontMutationScheduler.Decision decision = FontMutationScheduler.decide(
                30f, 30f, 28.2f, 0.94f, false, null, false);

        assertEquals(FontMutationScheduler.Action.APPLY, decision.action());
        assertEquals(28.2f, decision.targetPx(), 0.0001f);
    }

    @Test
    public void differentObjectMatchingAnotherViewsTargetStillApplies() {
        FontMutationScheduler.Decision decision = FontMutationScheduler.decide(
                28.2f, 28.2f, 26.508f, 0.94f, false, null, false);

        assertEquals(FontMutationScheduler.Action.APPLY, decision.action());
        assertEquals(26.508f, decision.targetPx(), 0.0001f);
    }

    @Test
    public void javaCallersUseActionAndTargetPxMethods() {
        FontMutationScheduler.Decision decision = FontMutationScheduler.decide(
                18f, 18f, 16.74f, 0.93f, false);
        assertEquals(FontMutationScheduler.Action.APPLY, decision.action());
        assertEquals(16.74f, decision.targetPx(), 0.0001f);

        long decideCount = java.util.Arrays.stream(FontMutationScheduler.class.getDeclaredMethods())
                .filter(method -> "decide".equals(method.getName()))
                .count();
        assertEquals(2L, decideCount);
    }
}
