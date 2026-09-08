package com.dpis.module;

import com.dpis.module.fonts.hookdomain.FontHookArbitration;

import com.dpis.module.runtime.font.ForceTextSizeHookInstaller;

import com.dpis.module.fonts.TextViewFontProvenanceTracker;

import com.dpis.module.fonts.FontFieldRewriteMath;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ForceTextSizeRegressionReferenceTest {
    @Test
    public void markdownSpanReference_scalesAbsoluteAndRelative() {
        assertEquals(40, FontFieldRewriteMath.scaleAbsoluteSize(20, 2.0f));
        assertEquals(2.4f, FontFieldRewriteMath.scaleRelativeSize(1.2f, 2.0f), 0.0001f);
    }

    @Test
    public void textSizeScalingReference_doesNotDoubleScaleSameViewState() {
        Map<Object, Float> base = new HashMap<>();
        Object key = new Object();

        float first = FontFieldRewriteMath.resolveScaledTextSize(18f, 2.0f, base, key);
        float second = FontFieldRewriteMath.resolveScaledTextSize(36f, 2.0f, base, key);

        assertEquals(36f, first, 0.0001f);
        assertEquals(36f, second, 0.0001f);
    }

    @Test
    public void textSizeProvenance_recognizesPreviouslyAppliedTargetSize() {
        assertTrue(FontFieldRewriteMath.isKnownScaledTextSize(
                36f, 2.0f, 36f));
        assertFalse(FontFieldRewriteMath.shouldRecordTextBase(
                36f, 2.0f, null, 36f));
    }

    @Test
    public void textSizeProvenance_doesNotTreatScaledBaseAsProof() {
        assertFalse(FontFieldRewriteMath.isKnownScaledTextSize(
                36f, 2.0f, null));
        assertTrue(FontFieldRewriteMath.shouldRecordTextBase(
                36f, 2.0f, 18f, null));
    }

    @Test
    public void textSizeProvenance_recordsNewUnscaledBase() {
        assertTrue(FontFieldRewriteMath.shouldRecordTextBase(
                20f, 2.0f, 18f, 36f));
        assertFalse(FontFieldRewriteMath.isKnownScaledTextSize(
                20f, 2.0f, 36f));
    }

    @Test
    public void textSizeProvenance_usesRelativeToleranceForAppliedTarget() {
        assertTrue(FontFieldRewriteMath.approximatelyEqual(80f, 80.6f));
        assertFalse(FontFieldRewriteMath.approximatelyEqual(10f, 10.6f));
        assertTrue(FontFieldRewriteMath.isKnownScaledTextSize(
                80.6f, 2.0f, 80f));
    }

    @Test
    public void resourcesScaledDensityRecognizesAppliedFontFactor() {
        assertTrue(FontFieldRewriteMath.isResourcesScaledDensityApplied(
                2.1625f, 3.24375f, 1.5f));
        assertFalse(FontFieldRewriteMath.isResourcesScaledDensityApplied(
                2.1625f, 2.1625f, 1.5f));
    }

    @Test
    public void textSizeScalingReference_rebasesWhenCurrentClearlyChanges() {
        Map<Object, Float> base = new HashMap<>();
        Object key = new Object();

        FontFieldRewriteMath.resolveScaledTextSize(18f, 2.0f, base, key);
        float rebased = FontFieldRewriteMath.resolveScaledTextSize(22f, 2.0f, base, key);

        assertEquals(44f, rebased, 0.0001f);
    }

    @Test
    public void textViewCurrentPxFallbackIsPartOfDefaultFieldRewritePlan() {
        Map<Object, Float> base = new HashMap<>();
        Object key = new Object();

        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, true);

        assertTrue(plan.textViewCurrentPxFallbackEnabled);
        assertTrue(base.isEmpty());
    }

    @Test
    public void textSizeScalingReference_showsWhyCurrentPxFallbackMustBeOptIn() {
        Map<Object, Float> base = new HashMap<>();
        Object key = new Object();

        float resolved = FontFieldRewriteMath.resolveScaledTextSize(
                42f, 2.0f, base, key);

        assertEquals(84f, resolved, 0.0001f);
        assertEquals(42f, base.get(key), 0.0001f);
    }

    @Test
    public void commentHintReference_identifiesCommentLikeAndNonCommentLike() {
        assertTrue(FontFieldRewriteMath.containsCommentHint("com.max.xiaoheihe.comment.CommentTextView"));
        assertTrue(FontFieldRewriteMath.containsCommentHint("com.max.xiaoheihe.reply.ReplyItem"));
        assertTrue(FontFieldRewriteMath.containsCommentHint("com.max.xiaoheihe.bbs.HbLineHeightView"));
        assertFalse(FontFieldRewriteMath.containsCommentHint("com.max.xiaoheihe.feed.NormalTitleView"));
    }

    @Test
    public void replacementHookKeepsCurrentPxFallbackBehindDomainPlan() throws Exception {
        String source = read("src/main/java/com/dpis/module/runtime/font/ForceTextSizeHookRuntime.kt")
                + read("src/main/java/com/dpis/module/runtime/font/TextViewAttachHookInstaller.kt")
                + read("src/main/java/com/dpis/module/runtime/font/PaintTextSizeHookInstaller.kt")
                + read("src/main/java/com/dpis/module/runtime/font/TextViewTextSizeHookInstaller.kt");

        assertTrue(source.contains("installTextViewAttachHook("));
        assertTrue(source.contains("getDeclaredMethod(\"onAttachedToWindow\")"));
        assertTrue(source.contains("View::class.java.getDeclaredMethod(\"onAttachedToWindow\")"));
        assertTrue(source.contains("DPIS_FONT TextView attach override"));
        assertTrue(source.contains("TextSizePolicy.shouldInstallCurrentPxTextViewFallbacks(domainPlan)"));
        assertTrue(source.contains("domainPlan.paintFallbackEnabled"));
        assertTrue(source.contains("DPIS_FONT Paint/TextPaint fallback suppressed"));
        assertTrue(source.contains("isSpTextHandledByResources(thisObject, factor, domainPlan)"));
        assertTrue(source.contains("recordResourcesHandledTextSize(thisObject, originalPx, factor)"));
        assertTrue(source.contains("TextViewFontProvenanceTracker.recordResourcesHandled"));
        assertTrue(source.contains("TextViewFontProvenanceTracker.Source.TEXTVIEW_CURRENT_PX_FALLBACK"));
        assertTrue(source.contains("hasStrongerProvenanceForCurrentPxFallback"));
        assertTrue(source.contains("PaintFallbackResolver.resolve("));
        assertTrue(source.contains("chain.proceed(arrayOf<Any>(decision.adjustedPx))"));
        assertTrue(source.contains("summarizePaintFallbackStack("));
        assertTrue(source.contains("detailSuffix()"));
        assertFalse(source.contains("paint.setTextSize(adjusted)"));
        assertFalse(source.contains("textPaint.setTextSize(adjusted)"));
        assertFalse(source.contains("isPxTextHandledByResources"));
        assertFalse(source.contains("isCurrentPxHandledByResources"));
        assertTrue(source.indexOf("installTextViewAttachHook(")
                < source.indexOf("installPaintTextSizeHooks("));
    }

    @Test
    public void paintFallbackUsesArgumentReplacementInsteadOfPostWrite() throws Exception {
        String source = read("src/main/java/com/dpis/module/runtime/font/PaintTextSizeHookInstaller.kt");

        assertTrue(source.contains("chain.proceed(arrayOf<Any>(decision.adjustedPx))"));
        assertTrue(source.contains("installPaintTextSizeHook("));
        assertFalse(source.contains("paint.setTextSize(adjusted)"));
        assertFalse(source.contains("textPaint.setTextSize(adjusted)"));
    }

    @Test
    public void strongTextViewProvenanceStillSuppressesCurrentPxFallback() {
        Object textView = new Object();

        TextViewFontProvenanceTracker.recordApplied(
                textView,
                18f,
                36f,
                2.0f,
                TextViewFontProvenanceTracker.Source.TEXTVIEW_SP_REWRITE,
                TextViewFontProvenanceTracker.UnitKind.SP);

        assertTrue(TextViewFontProvenanceTracker.hasStrongerProvenanceForCurrentPxFallback(
                textView, 2.0f));
    }

    @Test
    public void fontHookArbitrationKeepsTextViewAndPaintFallbacks() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, true);

        assertFalse(plan.resourcesFontEnabled);
        assertTrue(plan.webViewTextZoomEnabled);
        assertTrue(plan.textViewHooksEnabled);
        assertTrue(plan.textViewSpRewriteEnabled);
        assertTrue(plan.textViewAbsoluteRewriteEnabled);
        assertTrue(plan.textViewCurrentPxFallbackEnabled);
        assertTrue(plan.paintFallbackEnabled);
    }

    @Test
    public void textViewUnitRewriteAllowsDefaultSpAndAbsoluteRewrites() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, true);

        assertTrue(ForceTextSizeHookInstaller.shouldForceTextUnitForTest(
                android.util.TypedValue.COMPLEX_UNIT_SP,
                plan));
        assertTrue(ForceTextSizeHookInstaller.shouldForceTextUnitForTest(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                plan));
    }

    private static String read(String relativePath) throws Exception {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
