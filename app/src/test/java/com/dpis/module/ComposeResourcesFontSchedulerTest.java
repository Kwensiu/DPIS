package com.dpis.module;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ComposeResourcesFontSchedulerTest {
    @After
    public void tearDown() {
        ComposeResourcesFontScheduler.clearForTest();
    }

    @Test
    public void resourcesHandledComposeSuppressesResourcesFontToObservedBaseScale() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);
        ComposeResourcesFontEvidence.Summary evidence = ComposeResourcesFontEvidence.summarize(
                plan,
                1.5f,
                3.0f,
                4.5f,
                1.5f,
                true);
        FontScaleOverride.Result target = new FontScaleOverride.Result(
                1.5f, 1.5f, 150, false);

        ComposeResourcesFontScheduler.observe("com.example.compose", evidence,
                1.5f, 1.5f, 1_000L);
        FontScaleOverride.Result result = ComposeResourcesFontScheduler.maybeSuppressResourcesFont(
                "com.example.compose", target, 1_100L);

        assertEquals(1.5f, result.original, 0.0001f);
        assertEquals(1.0f, result.effective, 0.0001f);
    }

    @Test
    public void nonComposeObservationClearsSuppression() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);
        ComposeResourcesFontEvidence.Summary handled = ComposeResourcesFontEvidence.summarize(
                plan, 1.5f, 3.0f, 4.5f, 1.5f, true);
        ComposeResourcesFontEvidence.Summary nonCompose = ComposeResourcesFontEvidence.summarize(
                plan, 1.5f, 3.0f, 4.5f, 1.5f, false);
        FontScaleOverride.Result target = new FontScaleOverride.Result(
                1.5f, 1.5f, 150, false);

        ComposeResourcesFontScheduler.observe("com.example.compose", handled,
                1.5f, 1.5f, 1_000L);
        ComposeResourcesFontScheduler.observe("com.example.compose", nonCompose,
                1.5f, 1.5f, 1_100L);
        FontScaleOverride.Result result = ComposeResourcesFontScheduler.maybeSuppressResourcesFont(
                "com.example.compose", target, 1_200L);

        assertEquals(1.5f, result.effective, 0.0001f);
    }

    @Test
    public void suppressionExpires() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);
        ComposeResourcesFontEvidence.Summary evidence = ComposeResourcesFontEvidence.summarize(
                plan, 1.5f, 3.0f, 4.5f, 1.5f, true);
        FontScaleOverride.Result target = new FontScaleOverride.Result(
                1.5f, 1.5f, 150, false);

        ComposeResourcesFontScheduler.observe("com.example.compose", evidence,
                1.5f, 1.5f, 1_000L);
        FontScaleOverride.Result result = ComposeResourcesFontScheduler.maybeSuppressResourcesFont(
                "com.example.compose",
                target,
                1_001L + ComposeResourcesFontScheduler.SUPPRESSION_TTL_MS);

        assertEquals(1.5f, result.effective, 0.0001f);
    }

    @Test
    public void metricsFontScaleUsesSuppressedBaseScale() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);
        ComposeResourcesFontEvidence.Summary evidence = ComposeResourcesFontEvidence.summarize(
                plan, 1.5f, 3.0f, 4.5f, 1.5f, true);

        ComposeResourcesFontScheduler.observe("com.example.compose", evidence,
                1.5f, 1.5f, 1_000L);
        float result = ComposeResourcesFontScheduler.maybeSuppressMetricsFontScale(
                "com.example.compose",
                1.5f,
                1_100L);

        assertEquals(1.0f, result, 0.0001f);
    }
}
