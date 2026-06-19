package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LogReadResultTest {
    @Test
    public void failureReasonPrefersErrorThenOutputThenUnknown() {
        assertEquals(
                "permission denied",
                new LogReadResult(1, "LSPosed", "stdout detail", "permission denied")
                        .failureReason("unknown")
        );
        assertEquals(
                "stdout detail",
                new LogReadResult(1, "LSPosed", "stdout detail", "")
                        .failureReason("unknown")
        );
        assertEquals(
                "unknown",
                new LogReadResult(1, "LSPosed", "", "")
                        .failureReason("unknown")
        );
    }

    @Test
    public void rootAccessErrorsAreDetectedForLsposedEmptyState() {
        LogReadResult denied = new LogReadResult(
                -1,
                "LSPosed",
                "",
                "su: permission denied"
        );

        assertTrue(denied.needsRootAccess());
        assertTrue(new LogReadResult(1, "LSPosed", "", "root access timed out")
                .needsRootAccess());
        assertFalse(new LogReadResult(1, "LSPosed", "", "missing file")
                .needsRootAccess());
    }
}
