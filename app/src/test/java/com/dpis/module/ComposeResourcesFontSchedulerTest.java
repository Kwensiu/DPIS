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

        Object resources = new Object();
        ComposeResourcesFontScheduler.observe("com.example.compose", "root-a", resources, evidence,
                1.5f, 1.5f, 1_000L);
        FontScaleOverride.Result result = ComposeResourcesFontScheduler.maybeSuppressResourcesFont(
                resources, "com.example.compose", target, 1_100L);

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

        Object resources = new Object();
        ComposeResourcesFontScheduler.observe("com.example.compose", "root-a", resources, handled,
                1.5f, 1.5f, 1_000L);
        ComposeResourcesFontScheduler.observe("com.example.compose", "root-a", resources, nonCompose,
                1.5f, 1.5f, 1_100L);
        FontScaleOverride.Result result = ComposeResourcesFontScheduler.maybeSuppressResourcesFont(
                resources, "com.example.compose", target, 1_200L);

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

        Object resources = new Object();
        ComposeResourcesFontScheduler.observe("com.example.compose", "root-a", resources, evidence,
                1.5f, 1.5f, 1_000L);
        FontScaleOverride.Result result = ComposeResourcesFontScheduler.maybeSuppressResourcesFont(
                resources,
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

        Object resources = new Object();
        ComposeResourcesFontScheduler.observe("com.example.compose", "root-a", resources, evidence,
                1.5f, 1.5f, 1_000L);
        float result = ComposeResourcesFontScheduler.maybeSuppressMetricsFontScale(
                resources,
                "com.example.compose",
                1.5f,
                1_100L);

        assertEquals(1.0f, result, 0.0001f);
    }

    @Test
    public void packageOnlyObservationDoesNotSuppressResourcesFont() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);
        ComposeResourcesFontEvidence.Summary evidence = ComposeResourcesFontEvidence.summarize(
                plan, 1.5f, 3.0f, 4.5f, 1.5f, true);
        FontScaleOverride.Result target = new FontScaleOverride.Result(
                1.5f, 1.5f, 150, false);

        ComposeResourcesFontScheduler.observe("com.example.compose", evidence,
                1.5f, 1.5f, 1_000L);
        FontScaleOverride.Result result = ComposeResourcesFontScheduler.maybeSuppressResourcesFont(
                "com.example.compose", target, 1_100L);

        assertEquals(1.5f, result.effective, 0.0001f);
    }

    @Test
    public void resourceScopesStayIndependentWithinPackage() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);
        ComposeResourcesFontEvidence.Summary evidence = ComposeResourcesFontEvidence.summarize(
                plan, 1.5f, 3.0f, 4.5f, 1.5f, true);
        FontScaleOverride.Result target = new FontScaleOverride.Result(
                1.5f, 1.5f, 150, false);

        Object resourcesA = new Object();
        Object resourcesB = new Object();
        ComposeResourcesFontScheduler.observe("com.example.compose", "root-a", resourcesA, evidence,
                1.5f, 1.5f, 1_000L);
        ComposeResourcesFontScheduler.observe("com.example.compose", "root-b", resourcesB, evidence,
                1.8f, 1.5f, 1_100L);
        FontScaleOverride.Result oldResult = ComposeResourcesFontScheduler.maybeSuppressResourcesFont(
                resourcesA, "com.example.compose", target, 1_200L);
        FontScaleOverride.Result newResult = ComposeResourcesFontScheduler.maybeSuppressResourcesFont(
                resourcesB, "com.example.compose", target, 1_200L);

        assertEquals(1.0f, oldResult.effective, 0.0001f);
        assertEquals(1.2f, newResult.effective, 0.0001f);
    }
}
