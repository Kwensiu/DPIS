package com.dpis.module;

import com.dpis.module.fonts.FontDebugStatsReporter;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Behavior tests for the {@code FontDebugStatsReporter} hot-path entry gates.
 *
 * <p>These tests pin the core optimization for issue #54 item 4: when
 * diagnostics logging is off, the per-event String allocation and state
 * mutation must be skipped entirely. They run on the plain JVM; the android
 * {@code Context} path is exercised with {@code null} (which makes
 * {@code resolveContext} fall through to the reflection branch and return
 * {@code null} under a non-android runtime, so no snapshot is dispatched).
 */
public class FontDebugStatsReporterTest {

    private boolean savedLoggingEnabled;

    @Before
    public void setUp() {
        savedLoggingEnabled = DpisLog.isLoggingEnabled();
        DpisLog.setLoggingEnabled(false);
        FontDebugStatsReporter.resetForTest();
    }

    @After
    public void tearDown() {
        DpisLog.setLoggingEnabled(savedLoggingEnabled);
        FontDebugStatsReporter.resetForTest();
    }

    @Test
    public void loggingOff_recordUnit_doesNotCountEventsOrAllocateChain() {
        // The "text-size-unit-" + unit concatenation must be gated behind the
        // logging check, so with logging off no event is recorded.
        FontDebugStatsReporter.recordUnit(0, "android.widget.TextView", null);

        assertEquals(0, FontDebugStatsReporter.debugTotalEvents());
    }

    @Test
    public void loggingOff_record_doesNotCountEvents() {
        FontDebugStatsReporter.record("text-size-float", "android.widget.TextView", null);

        assertEquals(0, FontDebugStatsReporter.debugTotalEvents());
    }

    @Test
    public void loggingOff_nullAndEmptyInputsAreCheap() {
        // Even with malformed inputs the gate must return before any state work.
        FontDebugStatsReporter.record(null, null, null);
        FontDebugStatsReporter.record("", "", null);
        FontDebugStatsReporter.recordUnit(2, null, null);

        assertEquals(0, FontDebugStatsReporter.debugTotalEvents());
    }

    @Test
    public void loggingOn_recordUnit_countsAsUnitChain() {
        DpisLog.setLoggingEnabled(true);

        // null context on a non-android runtime keeps resolveContext() == null,
        // so no snapshot is dispatched; the event is still counted.
        FontDebugStatsReporter.recordUnit(0, "android.widget.TextView", null);

        assertEquals(1, FontDebugStatsReporter.debugTotalEvents());
    }

    @Test
    public void loggingOn_record_countsEvent() {
        DpisLog.setLoggingEnabled(true);

        FontDebugStatsReporter.record("text-size-float", "android.widget.TextView", null);

        assertEquals(1, FontDebugStatsReporter.debugTotalEvents());
    }

    @Test
    public void loggingOn_emptyChainIsSkippedEvenWhenLoggingEnabled() {
        DpisLog.setLoggingEnabled(true);

        FontDebugStatsReporter.record("", "android.widget.TextView", null);
        FontDebugStatsReporter.record(null, "android.widget.TextView", null);

        assertEquals(0, FontDebugStatsReporter.debugTotalEvents());
    }

    @Test
    public void loggingOn_recordUnitWithNullViewClassCountsAsUnknown() {
        // Defensive: a null view class must not throw; it normalizes to
        // "unknown" inside recordInternal and still counts the event.
        DpisLog.setLoggingEnabled(true);

        FontDebugStatsReporter.recordUnit(1, null, null);

        assertEquals(1, FontDebugStatsReporter.debugTotalEvents());
    }
}
