package com.dpis.module;

import com.dpis.module.fonts.hookdomain.FontHookArbitration;

import com.dpis.module.runtime.font.ComposeResourcesFontEvidence;

import com.dpis.module.fonts.ComposeFontRuntimeClassifier;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ComposeResourcesFontEvidenceTest {
    @Test
    public void knownComposeClassNamesClassifyAsCompose() {
        assertTrue(ComposeFontRuntimeClassifier.isKnownComposeViewClassName(
                "androidx.compose.ui.platform.ComposeView"));
        assertTrue(ComposeFontRuntimeClassifier.isKnownComposeViewClassName(
                "androidx.compose.ui.platform.AndroidComposeView"));
    }

    @Test
    public void nonComposeClassNamesDoNotClassifyAsCompose() {
        assertFalse(ComposeFontRuntimeClassifier.isKnownComposeViewClassName(
                "android.widget.TextView"));
        assertFalse(ComposeFontRuntimeClassifier.isKnownComposeViewClassName(
                "android.view.ViewGroup"));
        assertFalse(ComposeFontRuntimeClassifier.isKnownComposeViewClassName(null));
    }

    @Test
    public void currentRootWithoutComposeIsFalseEvenWhenAnotherRootHasCompose() {
        FakeNode staleRoot = node("android.widget.FrameLayout",
                node("androidx.compose.ui.platform.ComposeView"));
        FakeNode currentRoot = node("android.widget.FrameLayout",
                node("android.widget.TextView"));

        assertTrue(ComposeFontRuntimeClassifier.isComposeHeavy(staleRoot));
        assertFalse(ComposeFontRuntimeClassifier.isComposeHeavy(currentRoot));
    }

    @Test
    public void composeClassifierStopsAtTraversalDepthLimit() {
        FakeNode compose = node("androidx.compose.ui.platform.ComposeView");
        FakeNode current = compose;
        for (int i = 0; i < 40; i++) {
            current = node("android.widget.FrameLayout", current);
        }

        assertFalse(ComposeFontRuntimeClassifier.isComposeHeavy(current));
    }

    @Test
    public void composeClassifierStopsAtTraversalNodeLimit() {
        FakeNode[] children = new FakeNode[520];
        Arrays.fill(children, node("android.widget.TextView"));
        children[519] = node("androidx.compose.ui.platform.ComposeView");

        assertFalse(ComposeFontRuntimeClassifier.isComposeHeavy(
                node("android.widget.FrameLayout", children)));
    }

    @Test
    public void resourcesHandledComposeRequiresAllEvidence() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);

        assertTrue(ComposeResourcesFontEvidence.isResourcesHandledCompose(
                plan,
                1.5f,
                2.0f,
                3.0f,
                1.5f,
                true));
    }

    @Test
    public void summaryExposesEachSchedulingEvidencePart() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);

        ComposeResourcesFontEvidence.Summary summary = ComposeResourcesFontEvidence.summarize(
                plan,
                1.5f,
                2.0f,
                3.0f,
                1.5f,
                true);

        assertTrue(summary.resourcesFontDomainEnabled);
        assertTrue(summary.fontScaleMatches);
        assertTrue(summary.scaledDensityRatioMatches);
        assertTrue(summary.composeHeavyCurrentRoot);
        assertTrue(summary.resourcesHandled);
        assertEquals(1.5f, summary.scaledDensityRatio, 0.0001f);
    }

    @Test
    public void partialSummaryDoesNotBecomeResourcesHandled() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);

        ComposeResourcesFontEvidence.Summary summary = ComposeResourcesFontEvidence.summarize(
                plan,
                1.5f,
                2.0f,
                3.0f,
                1.5f,
                false);

        assertTrue(summary.resourcesFontDomainEnabled);
        assertTrue(summary.fontScaleMatches);
        assertTrue(summary.scaledDensityRatioMatches);
        assertFalse(summary.composeHeavyCurrentRoot);
        assertFalse(summary.resourcesHandled);
    }

    @Test
    public void resourcesHandledComposeIsFalseWhenResourcesFontDomainDisabled() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(false, false);

        assertFalse(ComposeResourcesFontEvidence.isResourcesHandledCompose(
                plan,
                1.5f,
                2.0f,
                3.0f,
                1.5f,
                true));
    }

    @Test
    public void resourcesHandledComposeIsFalseWhenFontScaleDoesNotMatch() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);

        assertFalse(ComposeResourcesFontEvidence.isResourcesHandledCompose(
                plan,
                1.4f,
                2.0f,
                3.0f,
                1.5f,
                true));
    }

    @Test
    public void resourcesHandledComposeIsFalseWhenScaledDensityRatioDoesNotMatch() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);

        assertFalse(ComposeResourcesFontEvidence.isResourcesHandledCompose(
                plan,
                1.5f,
                2.0f,
                2.5f,
                1.5f,
                true));
    }

    @Test
    public void resourcesHandledComposeIsFalseWhenCurrentRootIsNotComposeHeavy() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);

        assertFalse(ComposeResourcesFontEvidence.isResourcesHandledCompose(
                plan,
                1.5f,
                2.0f,
                3.0f,
                1.5f,
                false));
    }

    private static FakeNode node(String className, FakeNode... children) {
        return new FakeNode(className, Arrays.asList(children));
    }

    private static final class FakeNode implements ComposeFontRuntimeClassifier.ViewTreeNode {
        private final String className;
        private final List<FakeNode> children;

        FakeNode(String className, List<FakeNode> children) {
            this.className = className;
            this.children = children;
        }

        @Override
        public String className() {
            return className;
        }

        @Override
        public List<FakeNode> children() {
            return children == null ? Collections.emptyList() : children;
        }
    }
}
