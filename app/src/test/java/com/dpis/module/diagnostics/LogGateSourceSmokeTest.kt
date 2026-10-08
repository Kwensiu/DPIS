package com.dpis.module.diagnostics

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertTrue
import org.junit.Test

/** Locks the log gate to the same active preference store as the Settings page. */
class LogGateSourceSmokeTest {
    @Test
    fun diagnosticGateUsesActiveUiConfigStore() {
        val gate = read("src/main/java/com/dpis/module/diagnostics/presentation/LogGate.kt")

        assertTrue(gate.contains("ConfigStoreFactory.createDiagnosticLogGateConfigStore(activity)"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
