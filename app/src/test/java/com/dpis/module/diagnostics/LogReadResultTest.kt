package com.dpis.module.diagnostics

import com.dpis.module.diagnostics.device.LsposedLogReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogReadResultTest {
    @Test
    fun failureReasonPrefersErrorThenOutputThenUnknown() {
        assertEquals(
            "permission denied",
            LogReadResult(1, "LSPosed", "stdout detail", "permission denied")
                .failureReason("unknown"),
        )
        assertEquals(
            "stdout detail",
            LogReadResult(1, "LSPosed", "stdout detail", "").failureReason("unknown"),
        )
        assertEquals(
            "unknown",
            LogReadResult(1, "LSPosed", "", "").failureReason("unknown"),
        )
    }

    @Test
    fun rootAccessErrorsAreDetectedForLsposedEmptyState() {
        val denied = LogReadResult(-1, "LSPosed", "", "su: permission denied")

        assertTrue(denied.needsRootAccess())
        assertTrue(LogReadResult(1, "LSPosed", "", "root access timed out").needsRootAccess())
        assertFalse(LogReadResult(1, "LSPosed", "", "missing file").needsRootAccess())
    }

    @Test
    fun lsposedAvailabilitySeparatesPermissionFilesAndValidEntries() {
        assertEquals(
            LsposedLogReader.Availability.NO_PERMISSION,
            LsposedLogReader.availability(LogReadResult(-1, "LSPosed", "", "permission denied")),
        )
        assertEquals(
            LsposedLogReader.Availability.NO_LOGS,
            LsposedLogReader.availability(LogReadResult(0, "LSPosed", "", "", false, false)),
        )
        assertEquals(
            LsposedLogReader.Availability.NO_LOGS,
            LsposedLogReader.availability(
                LogReadResult(
                    2,
                    "LSPosed",
                    "",
                    "missing file",
                    false,
                    false
                )
            ),
        )
        assertEquals(
            LsposedLogReader.Availability.NO_VALID_LOGS,
            LsposedLogReader.availability(LogReadResult(0, "LSPosed", "", "", true, false)),
        )
        assertEquals(
            LsposedLogReader.Availability.AVAILABLE,
            LsposedLogReader.availability(LogReadResult(0, "LSPosed", "entry", "", true, true)),
        )
    }
}
