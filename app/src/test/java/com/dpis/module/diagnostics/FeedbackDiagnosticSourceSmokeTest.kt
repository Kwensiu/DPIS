package com.dpis.module.diagnostics

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeps only the lifecycle and framework-bound wiring contracts that the JVM
 * harness cannot invoke directly. Pure diagnostic behavior is tested through
 * the callable policy, parser, transport, and export APIs.
 */
class FeedbackDiagnosticSourceSmokeTest {
    @Test
    fun composeEditorExposesFeedbackDiagnosticEntry() {
        assertTrue(
            read("src/main/java/com/dpis/module/appconfig/presentation/ComposeAppEditorActivityGateway.kt")
                .contains("feedbackDiagnostic?.showPreparation("),
        )
    }

    @Test
    fun feedbackDiagnosticOwnsSessionOutsideActivity() {
        assertTrue(
            read("src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt")
                .contains("FeedbackDiagnosticActivitySession("),
        )
    }

    @Test
    fun feedbackDiagnosticPublishesRuntimeEventsThroughTransport() {
        assertTrue(
            read("src/main/java/com/dpis/module/diagnostics/DpisLog.kt")
                .contains("RuntimeTransport.record("),
        )
    }

    @Test
    fun feedbackDiagnosticRestartsTargetThroughRootLauncher() {
        assertTrue(
            read("src/main/java/com/dpis/module/diagnostics/presentation/FeedbackDiagnosticActivitySession.kt")
                .contains("launcher.restartForDiagnostic(packageName)"),
        )
    }

    @Test
    fun feedbackDiagnosticReadsForegroundThroughRootSnapshot() {
        assertTrue(
            read("src/main/java/com/dpis/module/diagnostics/device/ForegroundAppReader.kt")
                .contains("dumpsys activity activities"),
        )
    }

    @Test
    fun feedbackDiagnosticResultWiresShareActionToPackagedEvidence() {
        assertTrue(
            read("src/main/java/com/dpis/module/diagnostics/presentation/ResultSheet.kt")
                .contains("host.shareFeedbackDiagnostic(diagnosticPackage)"),
        )
    }

    private fun read(relativePath: String): String =
        SourceSmokeTestPaths.read(relativePath).replace("\r\n", "\n")
}
