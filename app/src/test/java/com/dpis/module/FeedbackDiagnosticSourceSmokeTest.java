package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public final class FeedbackDiagnosticSourceSmokeTest {
    @Test
    public void appConfigSheetExposesFeedbackDiagnosticEntry() throws IOException {
        String binder = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        String actions = read("src/main/java/com/dpis/module/AppConfigSheetActionBinder.java");
        String layout = read("src/main/res/layout/dialog_app_config.xml");

        assertTrue(layout.contains("dialog_feedback_diagnostic_button"));
        assertTrue(layout.contains("@string/feedback_diagnostic_action"));
        assertTrue(layout.contains("@drawable/ic_bug_report_24"));
        assertTrue(binder.contains("startFeedbackDiagnostic("));
        assertTrue(binder.contains("feedbackDiagnosticButton"));
        assertTrue(actions.contains("host.startFeedbackDiagnostic(item, state);"));
    }

    @Test
    public void feedbackDiagnosticUsesCoordinatorInsteadOfMainActivityStateMachine()
            throws IOException {
        String main = read("src/main/java/com/dpis/module/MainActivity.java");
        String coordinator = read(
                "src/main/java/com/dpis/module/FeedbackDiagnosticCoordinator.java");
        String summary = read(
                "src/main/java/com/dpis/module/FeedbackDiagnosticSummaryBuilder.java");

        assertTrue(main.contains("new FeedbackDiagnosticCoordinator(createFeedbackDiagnosticHost())"));
        assertTrue(main.contains("private FeedbackDiagnosticCoordinator.Host createFeedbackDiagnosticHost()"));
        assertTrue(main.contains("feedbackDiagnosticCoordinator.start("));
        assertTrue(main.contains("maybeShowPendingFeedbackDiagnosticResult();"));
        assertFalse(main.contains("postDelayed(() -> finish("));
        assertFalse(main.contains("summaryBuilder.build("));

        assertTrue(coordinator.contains("static final long DEFAULT_DURATION_MS = 10_000L;"));
        assertTrue(coordinator.contains("handler.postDelayed("));
        assertTrue(coordinator.contains("host.launchTargetApp(request.packageName)"));
        assertTrue(coordinator.contains("host.bringDpisToFront();"));
        assertTrue(coordinator.contains("host.onFeedbackDiagnosticFinished(result);"));
        assertTrue(summary.contains("source: feedback-diagnostic-summary"));
        assertTrue(summary.contains("previewFromGlobalPrefill: "));
    }

    @Test
    public void feedbackDiagnosticResultSupportsShareAndSaveSummary() throws IOException {
        String main = read("src/main/java/com/dpis/module/MainActivity.java");
        String resultSheet = read(
                "src/main/java/com/dpis/module/FeedbackDiagnosticResultSheet.java");
        String layout = read("src/main/res/layout/dialog_feedback_diagnostic_result.xml");

        assertTrue(main.contains("REQUEST_SAVE_FEEDBACK_DIAGNOSTIC"));
        assertTrue(main.contains("Intent.ACTION_CREATE_DOCUMENT"));
        assertTrue(main.contains("setType(\"text/plain\")"));
        assertTrue(main.contains("Intent.ACTION_SEND"));
        assertTrue(main.contains("putExtra(Intent.EXTRA_TEXT, result.summary)"));
        assertTrue(resultSheet.contains("new BottomSheetDialog(activity)"));
        assertTrue(resultSheet.contains("host.shareFeedbackDiagnostic(result);"));
        assertTrue(resultSheet.contains("host.saveFeedbackDiagnostic(result);"));
        assertTrue(layout.contains("feedback_diagnostic_result_summary"));
        assertTrue(layout.contains("feedback_diagnostic_save_button"));
        assertTrue(layout.contains("feedback_diagnostic_share_button"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
