package com.dpis.module;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
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
