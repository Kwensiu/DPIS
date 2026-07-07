package com.dpis.module;

import com.dpis.module.fonts.TextViewFontProvenanceTracker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TextViewFontProvenanceTrackerTest {
    @Test
    public void recordsResourcesHandledTextViewAsStrongProvenance() {
        Object textView = new Object();

        TextViewFontProvenanceTracker.recordResourcesHandled(textView, 36f, 2.0f);

        TextViewFontProvenanceTracker.Entry entry =
                TextViewFontProvenanceTracker.snapshotForTest(textView);
        assertNotNull(entry);
        assertEquals(18f, entry.basePx, 0.0001f);
        assertEquals(36f, entry.appliedPx, 0.0001f);
        assertEquals(2.0f, entry.factorAtApply, 0.0001f);
        assertEquals(TextViewFontProvenanceTracker.Source.RESOURCE_FONT_SCALE, entry.source);
        assertEquals(TextViewFontProvenanceTracker.UnitKind.SP, entry.unitKind);
        assertTrue(TextViewFontProvenanceTracker.hasStrongerProvenanceForCurrentPxFallback(
                textView, 2.0f));
    }

    @Test
    public void recordsSpRewriteAsStrongProvenance() {
        Object textView = new Object();

        TextViewFontProvenanceTracker.recordApplied(
                textView,
                18f,
                36f,
                2.0f,
                TextViewFontProvenanceTracker.Source.TEXTVIEW_SP_REWRITE,
                TextViewFontProvenanceTracker.UnitKind.SP);

        TextViewFontProvenanceTracker.Entry entry =
                TextViewFontProvenanceTracker.snapshotForTest(textView);
        assertNotNull(entry);
        assertEquals(18f, entry.basePx, 0.0001f);
        assertEquals(36f, entry.appliedPx, 0.0001f);
        assertEquals(TextViewFontProvenanceTracker.Source.TEXTVIEW_SP_REWRITE, entry.source);
        assertTrue(TextViewFontProvenanceTracker.hasStrongerProvenanceForCurrentPxFallback(
                textView, 2.0f));
    }

    @Test
    public void recordsAbsoluteRewriteAsStrongProvenance() {
        Object textView = new Object();

        TextViewFontProvenanceTracker.recordApplied(
                textView,
                20f,
                30f,
                1.5f,
                TextViewFontProvenanceTracker.Source.TEXTVIEW_ABSOLUTE_REWRITE,
                TextViewFontProvenanceTracker.UnitKind.ABSOLUTE);

        TextViewFontProvenanceTracker.Entry entry =
                TextViewFontProvenanceTracker.snapshotForTest(textView);
        assertNotNull(entry);
        assertEquals(20f, entry.basePx, 0.0001f);
        assertEquals(30f, entry.appliedPx, 0.0001f);
        assertEquals(TextViewFontProvenanceTracker.Source.TEXTVIEW_ABSOLUTE_REWRITE, entry.source);
        assertTrue(TextViewFontProvenanceTracker.hasStrongerProvenanceForCurrentPxFallback(
                textView, 1.5f));
    }

    @Test
    public void currentPxFallbackEntryDoesNotBlockItself() {
        Object textView = new Object();

        TextViewFontProvenanceTracker.recordApplied(
                textView,
                20f,
                30f,
                1.5f,
                TextViewFontProvenanceTracker.Source.TEXTVIEW_CURRENT_PX_FALLBACK,
                TextViewFontProvenanceTracker.UnitKind.UNKNOWN);

        assertFalse(TextViewFontProvenanceTracker.hasStrongerProvenanceForCurrentPxFallback(
                textView, 1.5f));
    }

    @Test
    public void factorMismatchInvalidatesStrongerProvenanceCheck() {
        Object textView = new Object();

        TextViewFontProvenanceTracker.recordApplied(
                textView,
                18f,
                36f,
                2.0f,
                TextViewFontProvenanceTracker.Source.TEXTVIEW_SP_REWRITE,
                TextViewFontProvenanceTracker.UnitKind.SP);

        assertFalse(TextViewFontProvenanceTracker.hasStrongerProvenanceForCurrentPxFallback(
                textView, 1.5f));
    }

    @Test
    public void nullAndInvalidInputsAreIgnored() {
        TextViewFontProvenanceTracker.recordResourcesHandled(null, 36f, 2.0f);
        TextViewFontProvenanceTracker.recordApplied(null, 18f, 36f, 2.0f,
                TextViewFontProvenanceTracker.Source.TEXTVIEW_SP_REWRITE,
                TextViewFontProvenanceTracker.UnitKind.SP);

        assertNull(TextViewFontProvenanceTracker.snapshotForTest(null));
    }
}
