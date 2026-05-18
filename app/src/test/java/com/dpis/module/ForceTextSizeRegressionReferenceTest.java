package com.dpis.module;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public void textSizeScalingReference_rebasesWhenCurrentClearlyChanges() {
        Map<Object, Float> base = new HashMap<>();
        Object key = new Object();

        FontFieldRewriteMath.resolveScaledTextSize(18f, 2.0f, base, key);
        float rebased = FontFieldRewriteMath.resolveScaledTextSize(22f, 2.0f, base, key);

        assertEquals(44f, rebased, 0.0001f);
    }

    @Test
    public void textViewCurrentPxFallbackIsNotPartOfDefaultFieldRewritePlan() {
        Map<Object, Float> base = new HashMap<>();
        Object key = new Object();

        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, true);

        assertFalse(plan.textViewCurrentPxFallbackEnabled);
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
        String source = read("src/main/java/com/dpis/module/ForceTextSizeHookInstaller.java");

        assertTrue(source.contains("installTextViewAttachHook("));
        assertTrue(source.contains("getDeclaredMethod(\"onAttachedToWindow\")"));
        assertTrue(source.contains("return View.class.getDeclaredMethod(\"onAttachedToWindow\")"));
        assertTrue(source.contains("DPIS_FONT TextView attach override"));
        assertTrue(source.contains("domainPlan.textViewCurrentPxFallbackEnabled"));
        assertTrue(source.contains("domainPlan.paintFallbackEnabled"));
        assertTrue(source.contains("DPIS_FONT Paint/TextPaint fallback suppressed"));
        assertTrue(source.indexOf("installTextViewAttachHook(")
                < source.indexOf("installPaintTextSizeHooks("));
    }

    @Test
    public void fontHookArbitrationKeepsResourcesWebViewAndSuppressesTextViewFallbacks() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, true);

        assertTrue(plan.resourcesFontEnabled);
        assertTrue(plan.webViewTextZoomEnabled);
        assertFalse(plan.textViewHooksEnabled);
        assertFalse(plan.textViewSpRewriteEnabled);
        assertFalse(plan.textViewAbsoluteRewriteEnabled);
        assertFalse(plan.textViewCurrentPxFallbackEnabled);
        assertFalse(plan.paintFallbackEnabled);
    }

    @Test
    public void textViewUnitRewriteSkipsSpAndAbsoluteUnitsWhenResourcesOwnsFontScale() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, true);

        assertFalse(ForceTextSizeHookInstaller.shouldForceTextUnitForTest(
                android.util.TypedValue.COMPLEX_UNIT_SP,
                plan));
        assertFalse(ForceTextSizeHookInstaller.shouldForceTextUnitForTest(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                plan));
    }

    private static String read(String relativePath) throws Exception {
        Path path = Path.of(relativePath);
        if (!Files.exists(path)) {
            path = Path.of("app", relativePath);
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
