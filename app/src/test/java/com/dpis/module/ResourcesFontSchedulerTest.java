package com.dpis.module;

import com.dpis.module.fonts.hookdomain.FontHookArbitration;

import com.dpis.module.runtime.font.FontScaleOverride;

import com.dpis.module.runtime.font.ResourcesFontScheduler;

import com.dpis.module.runtime.font.ComposeResourcesFontEvidence;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ResourcesFontSchedulerTest {
    @After
    public void tearDown() {
        ResourcesFontScheduler.clearForTest();
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
        ResourcesFontScheduler.observe("com.example.compose", "root-a", resources, evidence,
                1.5f, 1.5f, 1_000L);
        FontScaleOverride.Result result = ResourcesFontScheduler.maybeSuppressResourcesFont(
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
        ResourcesFontScheduler.observe("com.example.compose", "root-a", resources, handled,
                1.5f, 1.5f, 1_000L);
        ResourcesFontScheduler.observe("com.example.compose", "root-a", resources, nonCompose,
                1.5f, 1.5f, 1_100L);
        FontScaleOverride.Result result = ResourcesFontScheduler.maybeSuppressResourcesFont(
                resources, "com.example.compose", target, 1_200L);

        assertEquals(1.5f, result.effective, 0.0001f);
    }

    @Test
    public void suppressionPersistsWithoutTimeExpiry() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);
        ComposeResourcesFontEvidence.Summary evidence = ComposeResourcesFontEvidence.summarize(
                plan, 1.5f, 3.0f, 4.5f, 1.5f, true);
        FontScaleOverride.Result target = new FontScaleOverride.Result(
                1.5f, 1.5f, 150, false);

        Object resources = new Object();
        ResourcesFontScheduler.observe("com.example.compose", "root-a", resources, evidence,
                1.5f, 1.5f, 1_000L);
        FontScaleOverride.Result result = ResourcesFontScheduler.maybeSuppressResourcesFont(
                resources,
                "com.example.compose",
                target,
                60_000L);

        assertEquals(1.0f, result.effective, 0.0001f);
    }

    @Test
    public void resourcesReadConflictEventSuppressesResourcesFont() {
        FontScaleOverride.Result target = new FontScaleOverride.Result(
                1.4f, 1.4f, 140, false);
        Object resources = new Object();

        ResourcesFontScheduler.observeResourcesFontScale(
                resources, "com.example.compose", 1.0f, 1.4f);
        FontScaleOverride.Result first = ResourcesFontScheduler.maybeSuppressResourcesFont(
                resources, "com.example.compose", target, 1_100L);
        ResourcesFontScheduler.observeResourcesFontScale(
                resources, "com.example.compose", 1.4f, 1.4f);
        FontScaleOverride.Result second = ResourcesFontScheduler.maybeSuppressResourcesFont(
                resources, "com.example.compose", target, 1_200L);

        assertEquals(1.4f, first.effective, 0.0001f);
        assertEquals(1.4f, second.effective, 0.0001f);
    }

    @Test
    public void targetFactorChangeStartsANewResourcesReadEventBoundary() {
        Object resources = new Object();

        ResourcesFontScheduler.observeResourcesFontScale(
                resources, "com.example.compose", 1.0f, 1.4f);
        ResourcesFontScheduler.observeResourcesFontScale(
                resources, "com.example.compose", 1.4f, 1.4f);
        float oldTarget = ResourcesFontScheduler.maybeSuppressMetricsFontScale(
                resources, "com.example.compose", 1.4f, 1.4f);
        float newTarget = ResourcesFontScheduler.maybeSuppressMetricsFontScale(
                resources, "com.example.compose", 1.5f, 1.5f);

        assertEquals(1.4f, oldTarget, 0.0001f);
        assertEquals(1.5f, newTarget, 0.0001f);
    }

    @Test
    public void conflictEventPropagatesAcrossResourceScopesWithinPackage() {
        Object resourcesA = new Object();
        Object resourcesB = new Object();
        FontScaleOverride.Result target = new FontScaleOverride.Result(
                1.4f, 1.4f, 140, false);

        ResourcesFontScheduler.observeResourcesFontScale(
                resourcesA, "com.example.compose", 1.0f, 1.4f);
        ResourcesFontScheduler.observeResourcesFontScale(
                resourcesA, "com.example.compose", 1.4f, 1.4f);
        FontScaleOverride.Result result = ResourcesFontScheduler.maybeSuppressResourcesFont(
                resourcesB, "com.example.compose", target, 1_200L);

        assertEquals(1.4f, result.effective, 0.0001f);
    }

    @Test
    public void metricsFontScaleUsesSuppressedBaseScale() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);
        ComposeResourcesFontEvidence.Summary evidence = ComposeResourcesFontEvidence.summarize(
                plan, 1.5f, 3.0f, 4.5f, 1.5f, true);

        Object resources = new Object();
        ResourcesFontScheduler.observe("com.example.compose", "root-a", resources, evidence,
                1.5f, 1.5f, 1_000L);
        float result = ResourcesFontScheduler.maybeSuppressMetricsFontScale(
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

        ResourcesFontScheduler.observe("com.example.compose", evidence,
                1.5f, 1.5f, 1_000L);
        FontScaleOverride.Result result = ResourcesFontScheduler.maybeSuppressResourcesFont(
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
        ResourcesFontScheduler.observe("com.example.compose", "root-a", resourcesA, evidence,
                1.5f, 1.5f, 1_000L);
        ResourcesFontScheduler.observe("com.example.compose", "root-b", resourcesB, evidence,
                1.8f, 1.5f, 1_100L);
        FontScaleOverride.Result oldResult = ResourcesFontScheduler.maybeSuppressResourcesFont(
                resourcesA, "com.example.compose", target, 1_200L);
        FontScaleOverride.Result newResult = ResourcesFontScheduler.maybeSuppressResourcesFont(
                resourcesB, "com.example.compose", target, 1_200L);

        assertEquals(1.0f, oldResult.effective, 0.0001f);
        assertEquals(1.2f, newResult.effective, 0.0001f);
    }

    @Test
    public void composeObservationDoesNotOverrideTargetSuppression() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);
        ComposeResourcesFontEvidence.Summary evidence = ComposeResourcesFontEvidence.summarize(
                plan, 1.4f, 3.0f, 4.2f, 1.4f, true);
        Object resources = new Object();
        FontScaleOverride.Result target = new FontScaleOverride.Result(
                1.4f, 1.4f, 140, false);

        ResourcesFontScheduler.observeResourcesFontScale(
                resources, "com.example.compose", 1.0f, 1.4f);
        ResourcesFontScheduler.observeResourcesFontScale(
                resources, "com.example.compose", 1.4f, 1.4f);
        ResourcesFontScheduler.observe(
                "com.example.compose", "root-a", resources, evidence, 1.4f, 1.4f, 1_300L);
        FontScaleOverride.Result result = ResourcesFontScheduler.maybeSuppressResourcesFont(
                resources, "com.example.compose", target, 1_400L);

        assertEquals(1.4f, result.effective, 0.0001f);
    }

    @Test
    public void nonComposeObservationDoesNotClearTargetSuppression() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);
        ComposeResourcesFontEvidence.Summary nonCompose = ComposeResourcesFontEvidence.summarize(
                plan, 1.4f, 3.0f, 4.2f, 1.4f, false);
        Object resources = new Object();
        FontScaleOverride.Result target = new FontScaleOverride.Result(
                1.4f, 1.4f, 140, false);

        ResourcesFontScheduler.observeResourcesFontScale(
                resources, "com.example.compose", 1.0f, 1.4f);
        ResourcesFontScheduler.observeResourcesFontScale(
                resources, "com.example.compose", 1.4f, 1.4f);
        ResourcesFontScheduler.observe(
                "com.example.compose", "root-a", resources, nonCompose, 1.4f, 1.4f, 1_300L);
        FontScaleOverride.Result result = ResourcesFontScheduler.maybeSuppressResourcesFont(
                resources, "com.example.compose", target, 1_400L);

        assertEquals(1.4f, result.effective, 0.0001f);
    }
}
