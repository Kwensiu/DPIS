package com.dpis.module

import com.dpis.module.fonts.FontFieldRewriteMath
import com.dpis.module.fonts.TextViewFontProvenanceTracker
import com.dpis.module.fonts.hookdomain.FontHookArbitration
import com.dpis.module.runtime.font.ForceTextSizeHookInstaller
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForceTextSizeRegressionReferenceTest {
    @Test
    fun markdownSpanReference_scalesAbsoluteAndRelative() {
        assertEquals(40, FontFieldRewriteMath.scaleAbsoluteSize(20, 2.0f))
        assertEquals(2.4f, FontFieldRewriteMath.scaleRelativeSize(1.2f, 2.0f), 0.0001f)
    }

    @Test
    fun textSizeScalingReference_doesNotDoubleScaleSameViewState() {
        val base = HashMap<Any, Float?>()
        val key = Any()

        val first = FontFieldRewriteMath.resolveScaledTextSize(18f, 2.0f, base, key)
        val second = FontFieldRewriteMath.resolveScaledTextSize(36f, 2.0f, base, key)

        assertEquals(36f, first, 0.0001f)
        assertEquals(36f, second, 0.0001f)
    }

    @Test
    fun textSizeProvenance_recognizesPreviouslyAppliedTargetSize() {
        assertTrue(
            FontFieldRewriteMath.isKnownScaledTextSize(
                36f,
                2.0f,
                36f,
            ),
        )
        assertFalse(
            FontFieldRewriteMath.shouldRecordTextBase(
                36f,
                2.0f,
                null,
                36f,
            ),
        )
    }

    @Test
    fun textSizeProvenance_doesNotTreatScaledBaseAsProof() {
        assertFalse(FontFieldRewriteMath.isKnownScaledTextSize(36f, 2.0f, null))
        assertTrue(
            FontFieldRewriteMath.shouldRecordTextBase(
                36f,
                2.0f,
                18f,
                null,
            ),
        )
    }

    @Test
    fun textSizeProvenance_recordsNewUnscaledBase() {
        assertTrue(
            FontFieldRewriteMath.shouldRecordTextBase(
                20f,
                2.0f,
                18f,
                36f,
            ),
        )
        assertFalse(FontFieldRewriteMath.isKnownScaledTextSize(20f, 2.0f, 36f))
    }

    @Test
    fun textSizeProvenance_usesRelativeToleranceForAppliedTarget() {
        assertTrue(FontFieldRewriteMath.approximatelyEqual(80f, 80.6f))
        assertFalse(FontFieldRewriteMath.approximatelyEqual(10f, 10.6f))
        assertTrue(FontFieldRewriteMath.isKnownScaledTextSize(80.6f, 2.0f, 80f))
    }

    @Test
    fun spIsCoveredWhenScaledDensityAlreadyCarriesTheTarget() {
        val density = 2.5625f
        val factor = 1.3f
        assertTrue(
            FontFieldRewriteMath.isSpAlreadyCoveredByScaledDensity(
                density,
                density * factor,
                factor,
            ),
        )
    }

    @Test
    fun systemScaleIsNotTreatedAsTheTargetFontFactor() {
        val density = 2.5625f
        assertFalse(
            FontFieldRewriteMath.isSpAlreadyCoveredByScaledDensity(
                density,
                density * 1.25f,
                1.3f,
            ),
        )
    }

    @Test
    fun spCoverageUsesFactorMatchNotPixelTolerance() {
        val density = 2.5f
        val factor = 1.3f
        assertTrue(
            FontFieldRewriteMath.isSpAlreadyCoveredByScaledDensity(
                density,
                density * (factor + 0.0005f),
                factor,
            ),
        )
        assertFalse(
            FontFieldRewriteMath.isSpAlreadyCoveredByScaledDensity(
                density,
                density * (factor + 0.002f),
                factor,
            ),
        )
        assertFalse(FontFieldRewriteMath.isSpAlreadyCoveredByScaledDensity(2.5625f, 3.33125f, 1.0f))
        assertFalse(FontFieldRewriteMath.isSpAlreadyCoveredByScaledDensity(0f, 3.33125f, 1.3f))
        assertFalse(FontFieldRewriteMath.isSpAlreadyCoveredByScaledDensity(2.5625f, 0f, 1.3f))
    }

    @Test
    fun textSizeScalingReference_rebasesWhenCurrentClearlyChanges() {
        val base = HashMap<Any, Float?>()
        val key = Any()

        FontFieldRewriteMath.resolveScaledTextSize(18f, 2.0f, base, key)
        val rebased = FontFieldRewriteMath.resolveScaledTextSize(22f, 2.0f, base, key)

        assertEquals(44f, rebased, 0.0001f)
    }

    @Test
    fun textViewCurrentPxFallbackIsPartOfDefaultFieldRewritePlan() {
        val base = HashMap<Any, Float?>()
        val plan = FontHookArbitration.resolveDomainPlan(true, true)

        assertTrue(plan.textViewCurrentPxFallbackEnabled)
        assertTrue(base.isEmpty())
    }

    @Test
    fun textSizeScalingReference_showsWhyCurrentPxFallbackMustBeOptIn() {
        val base = HashMap<Any, Float?>()
        val key = Any()

        val resolved = FontFieldRewriteMath.resolveScaledTextSize(42f, 2.0f, base, key)

        assertEquals(84f, resolved, 0.0001f)
        assertEquals(42f, base[key]!!, 0.0001f)
    }

    @Test
    fun commentHintReference_identifiesCommentLikeAndNonCommentLike() {
        assertTrue(FontFieldRewriteMath.containsCommentHint("com.max.xiaoheihe.comment.CommentTextView"))
        assertTrue(FontFieldRewriteMath.containsCommentHint("com.max.xiaoheihe.reply.ReplyItem"))
        assertTrue(FontFieldRewriteMath.containsCommentHint("com.max.xiaoheihe.bbs.HbLineHeightView"))
        assertFalse(FontFieldRewriteMath.containsCommentHint("com.max.xiaoheihe.feed.NormalTitleView"))
    }

    @Test
    fun replacementHookKeepsCurrentPxFallbackBehindDomainPlan() {
        val source =
            read("src/main/java/com/dpis/module/runtime/font/ForceTextSizeHookRuntime.kt") +
                    read("src/main/java/com/dpis/module/runtime/font/TextViewAttachHookInstaller.kt") +
                    read("src/main/java/com/dpis/module/runtime/font/PaintTextSizeHookInstaller.kt") +
                    read("src/main/java/com/dpis/module/runtime/font/TextViewTextSizeHookInstaller.kt")

        assertTrue(source.contains("installTextViewAttachHook("))
        assertTrue(source.contains("getDeclaredMethod(\"onAttachedToWindow\")"))
        assertTrue(source.contains("View::class.java.getDeclaredMethod(\"onAttachedToWindow\")"))
        assertTrue(source.contains("DPIS_FONT TextView attach override"))
        assertTrue(source.contains("TextSizePolicy.shouldInstallCurrentPxTextViewFallbacks(domainPlan)"))
        assertTrue(source.contains("domainPlan.paintFallbackEnabled"))
        assertTrue(source.contains("DPIS_FONT Paint/TextPaint fallback suppressed"))
        assertTrue(source.contains("isSpAlreadyCoveredByScaledDensity("))
        assertTrue(source.contains("reason=scaled_density_at_target"))
        assertTrue(source.contains("recordResourcesHandledTextSize(thisObject, originalPx, factor)"))
        assertTrue(source.contains("TextViewFontProvenanceTracker.recordResourcesHandled"))
        assertTrue(source.contains("TextViewFontProvenanceTracker.Source.TEXTVIEW_CURRENT_PX_FALLBACK"))
        assertTrue(source.contains("hasStrongerProvenanceForCurrentPxFallback"))
        assertTrue(source.contains("FontMutationScheduler.decide("))
        assertTrue(source.contains("chain.proceed(arrayOf<Any>(decision.targetPx()))"))
        assertTrue(source.contains("summarizePaintFallbackStack("))
        assertFalse(source.contains("paint.setTextSize(adjusted)"))
        assertFalse(source.contains("textPaint.setTextSize(adjusted)"))
        assertFalse(source.contains("isPxTextHandledByResources"))
        assertFalse(source.contains("isCurrentPxHandledByResources"))
        assertTrue(
            source.indexOf("installTextViewAttachHook(") <
                    source.indexOf("installPaintTextSizeHooks("),
        )
    }

    @Test
    fun paintFallbackUsesArgumentReplacementInsteadOfPostWrite() {
        val source =
            read("src/main/java/com/dpis/module/runtime/font/PaintTextSizeHookInstaller.kt")

        assertTrue(source.contains("chain.proceed(arrayOf<Any>(decision.targetPx()))"))
        assertTrue(source.contains("installPaintTextSizeHook("))
        assertTrue(source.contains("FontMutationScheduler.Action.PASS_THROUGH"))
        assertTrue(source.contains("reason=transaction_target"))
        assertTrue(source.contains("RuntimeHotPathEvents.kept("))
        assertFalse(source.contains("paint.setTextSize(adjusted)"))
        assertFalse(source.contains("textPaint.setTextSize(adjusted)"))
        val passThrough = source.indexOf("FontMutationScheduler.Action.PASS_THROUGH")
        val applyBegin = source.indexOf("RuntimeHotPathEvents.begin(", passThrough)
        assertTrue(passThrough >= 0)
        assertTrue(applyBegin > passThrough)
        val passThroughBlock = source.substring(passThrough, applyBegin)
        assertTrue(passThroughBlock.contains("RuntimeHotPathEvents.kept("))
        assertFalse(passThroughBlock.contains("RuntimeHotPathEvents.applied("))
    }

    @Test
    fun strongTextViewProvenanceStillSuppressesCurrentPxFallback() {
        val textView = Any()

        TextViewFontProvenanceTracker.recordApplied(
            textView,
            18f,
            36f,
            2.0f,
            TextViewFontProvenanceTracker.Source.TEXTVIEW_SP_REWRITE,
            TextViewFontProvenanceTracker.UnitKind.SP,
        )

        assertTrue(
            TextViewFontProvenanceTracker.hasStrongerProvenanceForCurrentPxFallback(
                textView,
                2.0f,
            ),
        )
    }

    @Test
    fun fontHookArbitrationKeepsTextViewAndPaintFallbacks() {
        val plan = FontHookArbitration.resolveDomainPlan(true, true)

        assertFalse(plan.resourcesFontEnabled)
        assertTrue(plan.webViewTextZoomEnabled)
        assertTrue(plan.textViewHooksEnabled)
        assertTrue(plan.textViewSpRewriteEnabled)
        assertTrue(plan.textViewAbsoluteRewriteEnabled)
        assertTrue(plan.textViewCurrentPxFallbackEnabled)
        assertTrue(plan.paintFallbackEnabled)
    }

    @Test
    fun textViewUnitRewriteAllowsDefaultSpAndAbsoluteRewrites() {
        val plan = FontHookArbitration.resolveDomainPlan(true, true)

        assertTrue(
            ForceTextSizeHookInstaller.shouldForceTextUnitForTest(
                android.util.TypedValue.COMPLEX_UNIT_SP,
                plan,
            ),
        )
        assertTrue(
            ForceTextSizeHookInstaller.shouldForceTextUnitForTest(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                plan,
            ),
        )
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
