package com.dpis.module.diagnostics

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertTrue
import org.junit.Test

/** Locks the log gate to the same active preference store as the Settings page. */
class LogGateSourceSmokeTest {
    @Test
    fun diagnosticGateUsesActiveUiConfigStore() {
        val factory = read("src/main/java/com/dpis/module/ConfigStoreFactory.java")
        val gate = read("src/main/java/com/dpis/module/diagnostics/LogGate.java")

        assertTrue(factory.contains("DpisApplication.getActiveHookConfigStore(context)"))
        assertTrue(
            factory.contains(
                "activeStore != null ? activeStore : createLocalModuleConfigStore(context)",
            ),
        )
        assertTrue(gate.contains("ConfigStoreFactory.createDiagnosticLogGateConfigStore(activity)"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
