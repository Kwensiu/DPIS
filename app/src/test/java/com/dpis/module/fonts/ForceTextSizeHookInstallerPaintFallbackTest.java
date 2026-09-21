package com.dpis.module;

import static org.junit.Assert.assertEquals;

import com.dpis.module.fonts.PaintProvenanceTracker;
import com.dpis.module.runtime.font.ForceTextSizeHookInstaller;

import org.junit.Test;

public class ForceTextSizeHookInstallerPaintFallbackTest {
    @Test
    public void paintFallbackDecisionWritesUnknownPaintWhenIndependent() {
        Object paint = new Object();

        ForceTextSizeHookInstaller.PaintFallbackDecision decision =
                ForceTextSizeHookInstaller.resolvePaintFallbackDecisionForTest(
                        paint, 18f, 18f, 2.0f, false);

        assertEquals(ForceTextSizeHookInstaller.PaintFallbackAction.WRITE, decision.action);
        assertEquals(36f, decision.adjustedPx, 0.0001f);
    }

    @Test
    public void paintFallbackDecisionOnlyObservesWhenStrongerDomainOwnsWrite() {
        Object paint = new Object();

        ForceTextSizeHookInstaller.PaintFallbackDecision decision =
                ForceTextSizeHookInstaller.resolvePaintFallbackDecisionForTest(
                        paint, 18f, 18f, 2.0f, true);

        assertEquals(ForceTextSizeHookInstaller.PaintFallbackAction.OBSERVE, decision.action);
        assertEquals(18f, decision.adjustedPx, 0.0001f);
    }

    @Test
    public void paintFallbackDecisionKeepsKnownAppliedPaint() {
        Object paint = new Object();

        ForceTextSizeHookInstaller.PaintFallbackDecision first =
                ForceTextSizeHookInstaller.resolvePaintFallbackDecisionForTest(
                        paint, 18f, 18f, 2.0f, false);
        PaintProvenanceTracker.recordApplied(paint, first.adjustedPx, 2.0f);

        ForceTextSizeHookInstaller.PaintFallbackDecision second =
                ForceTextSizeHookInstaller.resolvePaintFallbackDecisionForTest(
                        paint, 36f, 36f, 2.0f, false);

        assertEquals(ForceTextSizeHookInstaller.PaintFallbackAction.KEEP, second.action);
        assertEquals(36f, second.adjustedPx, 0.0001f);
    }
}
