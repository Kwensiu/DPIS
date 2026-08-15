package com.dpis.module.fonts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PaintProvenanceTrackerTest {
    @Test
    public void recordsBaseAndLastAppliedForPaintLikeObject() {
        Object paint = new Object();

        float adjusted = PaintProvenanceTracker.resolveScaled(paint, 18f, 2.0f);
        PaintProvenanceTracker.recordApplied(paint, adjusted, 2.0f);

        PaintProvenanceTracker.Entry entry = PaintProvenanceTracker.snapshotForTest(paint);
        assertNotNull(entry);
        assertEquals(18f, entry.basePx, 0.0001f);
        assertEquals(36f, entry.lastAppliedPx, 0.0001f);
        assertEquals(2.0f, entry.factorAtApply, 0.0001f);
        assertTrue(PaintProvenanceTracker.isKnownApplied(paint, 36f, 2.0f));
    }

    @Test
    public void unknownPaintWriteScalesWithoutTextViewProvenance() {
        Object paint = new Object();

        float adjusted = PaintProvenanceTracker.resolveScaled(paint, 18f, 2.0f);

        PaintProvenanceTracker.Entry entry = PaintProvenanceTracker.snapshotForTest(paint);
        assertNotNull(entry);
        assertEquals(36f, adjusted, 0.0001f);
        assertEquals(18f, entry.basePx, 0.0001f);
        assertFalse(PaintProvenanceTracker.isKnownApplied(paint, 36f, 2.0f));
    }

    @Test
    public void invalidatesKnownAppliedWhenFactorChanges() {
        Object paint = new Object();

        float adjusted = PaintProvenanceTracker.resolveScaled(paint, 18f, 2.0f);
        PaintProvenanceTracker.recordApplied(paint, adjusted, 2.0f);

        assertFalse(PaintProvenanceTracker.isKnownApplied(paint, 36f, 1.5f));
    }

    @Test
    public void invalidatesKnownAppliedWhenCurrentSizeDriftedExternally() {
        Object paint = new Object();

        float adjusted = PaintProvenanceTracker.resolveScaled(paint, 18f, 2.0f);
        PaintProvenanceTracker.recordApplied(paint, adjusted, 2.0f);
        PaintProvenanceTracker.invalidateIfDrifted(paint, 20f);

        PaintProvenanceTracker.Entry entry = PaintProvenanceTracker.snapshotForTest(paint);
        assertNotNull(entry);
        assertNull(entry.lastAppliedPx);
        assertNull(entry.factorAtApply);
        assertFalse(PaintProvenanceTracker.isKnownApplied(paint, 36f, 2.0f));
    }

    @Test
    public void doesNotTrustBaseTimesFactorCoincidenceAsAppliedProof() {
        Object paint = new Object();

        PaintProvenanceTracker.resolveScaled(paint, 18f, 2.0f);

        assertFalse(PaintProvenanceTracker.isKnownApplied(paint, 36f, 2.0f));
    }

    @Test
    public void keepsCurrentPaintTargetWhenUpstreamRepeatsBaseSize() {
        Object paint = new Object();

        float adjusted = PaintProvenanceTracker.resolveScaled(paint, 18f, 0.93f);
        PaintProvenanceTracker.recordApplied(paint, adjusted, 0.93f);

        PaintProvenanceTracker.Resolution resolution = PaintProvenanceTracker.resolveFallback(
                paint, 18f, 16.74f, 0.93f, false);

        assertEquals(PaintProvenanceTracker.Action.KEEP, resolution.action());
    }

    @Test
    public void scaledValueAfterDriftIsTreatedAsAlreadyScaledSafetyNet() {
        Object paint = new Object();

        float adjusted = PaintProvenanceTracker.resolveScaled(paint, 18f, 2.0f);
        PaintProvenanceTracker.recordApplied(paint, adjusted, 2.0f);
        PaintProvenanceTracker.invalidateIfDrifted(paint, 20f);

        float resolved = PaintProvenanceTracker.resolveScaled(paint, 36f, 2.0f);

        PaintProvenanceTracker.Entry entry = PaintProvenanceTracker.snapshotForTest(paint);
        assertNotNull(entry);
        assertEquals(36f, resolved, 0.0001f);
        assertEquals(18f, entry.basePx, 0.0001f);
        assertNull(entry.lastAppliedPx);
        assertFalse(PaintProvenanceTracker.isKnownApplied(paint, 36f, 2.0f));
    }

    @Test
    public void reusedPaintKeepsMultipleBaseSizesFromAmplifyingEachOther() {
        Object paint = new Object();

        float firstAdjusted = PaintProvenanceTracker.resolveScaled(paint, 37f, 1.5f);
        PaintProvenanceTracker.recordApplied(paint, firstAdjusted, 1.5f);
        float secondAdjusted = PaintProvenanceTracker.resolveScaled(paint, 36f, 1.5f);
        PaintProvenanceTracker.recordApplied(paint, secondAdjusted, 1.5f);

        float firstAppliedAgain = PaintProvenanceTracker.resolveScaled(paint, 55.5f, 1.5f);

        assertEquals(55.5f, firstAdjusted, 0.0001f);
        assertEquals(54f, secondAdjusted, 0.0001f);
        assertEquals(55.5f, firstAppliedAgain, 0.0001f);
    }

    @Test
    public void doesNotTreatHigherOrderScaledSizeAsSafetyNet() {
        Object paint = new Object();

        float adjusted = PaintProvenanceTracker.resolveScaled(paint, 37f, 1.5f);
        PaintProvenanceTracker.recordApplied(paint, adjusted, 1.5f);

        float resolved = PaintProvenanceTracker.resolveScaled(paint, 83.25f, 1.5f);

        assertEquals(124.875f, resolved, 0.0001f);
    }

    @Test
    public void rebasesWhenIncomingSizeChanges() {
        Object paint = new Object();

        PaintProvenanceTracker.resolveScaled(paint, 18f, 2.0f);
        float adjusted = PaintProvenanceTracker.resolveScaled(paint, 22f, 2.0f);

        PaintProvenanceTracker.Entry entry = PaintProvenanceTracker.snapshotForTest(paint);
        assertNotNull(entry);
        assertEquals(44f, adjusted, 0.0001f);
        assertEquals(22f, entry.basePx, 0.0001f);
    }

}
