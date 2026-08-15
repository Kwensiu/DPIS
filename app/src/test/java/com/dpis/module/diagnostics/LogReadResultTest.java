package com.dpis.module.diagnostics;

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

    @Test
    public void lsposedAvailabilitySeparatesPermissionFilesAndValidEntries() {
        assertEquals(
                LsposedLogReader.Availability.NO_PERMISSION,
                LsposedLogReader.availability(
                        new LogReadResult(-1, "LSPosed", "", "permission denied")
                )
        );
        assertEquals(
                LsposedLogReader.Availability.NO_LOGS,
                LsposedLogReader.availability(
                        new LogReadResult(0, "LSPosed", "", "", false, false)
                )
        );
        assertEquals(
                LsposedLogReader.Availability.NO_VALID_LOGS,
                LsposedLogReader.availability(
                        new LogReadResult(0, "LSPosed", "", "", true, false)
                )
        );
        assertEquals(
                LsposedLogReader.Availability.AVAILABLE,
                LsposedLogReader.availability(
                        new LogReadResult(0, "LSPosed", "entry", "", true, true)
                )
        );
    }
}
