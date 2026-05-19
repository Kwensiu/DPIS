package com.dpis.module;

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
