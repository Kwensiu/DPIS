package com.dpis.module.diagnostics;

import com.dpis.module.diagnostics.DiagnosticLogGate;

import com.dpis.module.*;

import com.dpis.module.appconfig.AppConfigDialogBinder;

import com.dpis.module.runtime.font.WebViewFontHookInstaller;

import com.dpis.module.runtime.font.PaintTextSizeFallbackHookInstaller;

import com.dpis.module.runtime.font.ForceTextSizeHookInstaller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public final class FeedbackDiagnosticSourceSmokeTest {
    @Test
    public void appConfigSheetExposesFeedbackDiagnosticEntry() throws IOException {
        String binder = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        String actions = read("src/main/java/com/dpis/module/appconfig/AppConfigSheetActionBinder.java");
        String layout = read("src/main/res/layout/dialog_app_config.xml");
        String dimens = read("src/main/res/values/dimens.xml");

        assertTrue(layout.contains("dialog_feedback_diagnostic_button"));
        assertTrue(layout.contains("@string/feedback_diagnostic_action"));
        assertTrue(layout.contains("@drawable/ic_bug_report_24"));
        assertTrue(layout.contains(
                "android:layout_marginTop=\"@dimen/dialog_feedback_diagnostic_button_margin_top\""));
        assertTrue(binder.contains("startFeedbackDiagnostic("));
        assertTrue(binder.contains("feedbackDiagnosticButton"));
        assertTrue(actions.contains("host.startFeedbackDiagnostic(item, state);"));
        assertTrue(dimens.contains(
                "<dimen name=\"dialog_feedback_diagnostic_button_size\">32dp</dimen>"));
        assertTrue(dimens.contains(
                "<dimen name=\"dialog_feedback_diagnostic_button_margin_top\">12dp</dimen>"));
    }

    @Test
    public void feedbackDiagnosticUsesCoordinatorInsteadOfMainActivityStateMachine()
            throws IOException {
        String main = read("src/main/java/com/dpis/module/MainActivity.java");
        String coordinator = read(
                "src/main/java/com/dpis/module/diagnostics/FeedbackDiagnosticCoordinator.java");
        String summary = read(
                "src/main/java/com/dpis/module/diagnostics/FeedbackDiagnosticSummaryBuilder.java");
        String structuredExporter = read(
                "src/main/java/com/dpis/module/diagnostics/"
                        + "FeedbackDiagnosticStructuredEvidenceExporter.java");
        String logGate = read(
                "src/main/java/com/dpis/module/diagnostics/DiagnosticLogGate.java");

        assertTrue(main.contains("new FeedbackDiagnosticCoordinator(createFeedbackDiagnosticHost())"));
        assertTrue(main.contains("private FeedbackDiagnosticCoordinator.Host createFeedbackDiagnosticHost()"));
        assertTrue(main.contains("DiagnosticLogGate.ensureEnabled("));
        assertTrue(main.contains("showFeedbackDiagnosticConfirmation(item, state)"));
        assertTrue(main.contains("ComposeConfirmDialog.showWithLabels("));
        assertTrue(main.contains("resolvePackageVersionName(item.packageName)"));
        assertTrue(main.contains("feedbackDiagnosticCoordinator.start("));
        assertTrue(main.contains("FeedbackDiagnosticCoordinator.Request.fromPersisted("));
        assertTrue(main.contains("maybeShowPendingFeedbackDiagnosticResult();"));
        assertTrue(main.contains("showFeedbackDiagnosticPackagingDialog();"));
        assertTrue(main.contains("feedbackDiagnosticExportBuilder.buildPackage(result)"));
        assertTrue(main.contains("showFeedbackDiagnosticResultSheet(finalBuilt);"));
        assertFalse(main.contains("postDelayed(() -> finish("));
        assertFalse(main.contains("summaryBuilder.build("));
        assertTrue(logGate.contains("ComposeConfirmDialog.showWithLabels("));
        assertFalse(logGate.contains("MaterialAlertDialogBuilder"));

        assertTrue(coordinator.contains("handler.postDelayed("));
        assertTrue(coordinator.contains("RootAccessProbe.probe()"));
        assertTrue(coordinator.contains("FeedbackDiagnosticForegroundAppReader.readForegroundPackage()"));
        assertTrue(coordinator.contains("host.restartTargetAppForDiagnostic(request.packageName)"));
        assertTrue(coordinator.contains("handler.post(() -> startAfterRootLaunch(request, launched));"));
        assertTrue(coordinator.contains("runningStartedAtMillis = host.currentTimeMillis();"));
        assertTrue(coordinator.contains("FeedbackDiagnosticRuntimeTransport.start("));
        assertTrue(coordinator.contains("FeedbackDiagnosticRuntimeSelfTest.runUiTransportSelfTest("));
        assertTrue(coordinator.contains("FeedbackDiagnosticRuntimeEvents.start("));
        assertTrue(coordinator.contains("FeedbackDiagnosticRuntimeEvents.stopSnapshot()"));
        assertTrue(coordinator.contains("FeedbackDiagnosticRuntimeTransport.stopSnapshot("));
        assertTrue(coordinator.contains("host.onFeedbackDiagnosticRootRequired();"));
        assertTrue(coordinator.contains("handler.post(() -> host.onFeedbackDiagnosticFinished(result));"));
        assertTrue(coordinator.contains("summaryInput(request)"));
        assertTrue(coordinator.contains("static Request fromPersisted("));
        assertFalse(coordinator.contains("DEFAULT_DURATION_MS"));
        assertFalse(coordinator.contains("finish(true), DEFAULT_DURATION_MS"));
        assertFalse(coordinator.contains("bringDpisToFront"));
        assertTrue(summary.contains("source: feedback-diagnostic-summary"));
        assertTrue(summary.contains("versionName: "));
        assertTrue(summary.contains("previewFromGlobalPrefill: "));
        assertTrue(summary.contains("Diagnostic package includes diagnostic.txt"));
        assertTrue(summary.contains("timeline.tsv"));
        assertTrue(summary.contains("module-effects.tsv"));
        assertTrue(summary.contains("runtime transport"));
        assertTrue(summary.contains("public static final class Input"));
        assertFalse(summary.contains("FeedbackDiagnosticCoordinator.Request"));
        assertFalse(summary.contains("Runtime event capture is TODO"));
        assertTrue(structuredExporter.contains("buildTimelineTsv("));
        assertTrue(structuredExporter.contains("buildModuleEffectsTsv("));
        assertTrue(structuredExporter.contains("target-process-log-fallback"));
        assertTrue(structuredExporter.contains("latency percentiles unavailable"));
    }

    @Test
    public void feedbackDiagnosticMirrorsRuntimeDpisLogEvents()
            throws IOException {
        String dpisLog = read("src/main/java/com/dpis/module/DpisLog.java");
        String collector = read(
                "src/main/java/com/dpis/module/diagnostics/FeedbackDiagnosticRuntimeEvents.java");
        String hotPath = read(
                "src/main/java/com/dpis/module/diagnostics/FeedbackDiagnosticRuntimeHotPathEvents.java");

        assertTrue(dpisLog.contains("FeedbackDiagnosticRuntimeEvents.recordDpisLog(\"I\", msg);"));
        assertTrue(dpisLog.contains("FeedbackDiagnosticRuntimeEvents.recordDpisLog("));
        assertTrue(dpisLog.contains("FeedbackDiagnosticRuntimeTransport.record("));
        assertTrue(hotPath.contains("FeedbackDiagnosticRuntimeEvents.recordStructured("));
        assertTrue(hotPath.contains("FeedbackDiagnosticRuntimeTransport.record("));
        assertTrue(hotPath.contains("static void begin("));
        assertTrue(hotPath.contains("static void applied("));
        assertTrue(hotPath.contains("static void end("));
        assertTrue(collector.contains("private static volatile Session activeSession;"));
        assertTrue(collector.contains("\"unexpected_route_hit\""));
        assertTrue(collector.contains("\"repeated_write\""));
    }

    @Test
    public void feedbackDiagnosticFinishesOnlyAfterReturningToDpis()
            throws IOException {
        String coordinator = read(
                "src/main/java/com/dpis/module/diagnostics/FeedbackDiagnosticCoordinator.java");

        assertTrue(coordinator.contains("recordTimelineEvent(\"foreground returned to DPIS\");"));
        assertTrue(coordinator.contains("onDpisResumed();"));
        assertTrue(coordinator.contains("executor.execute(this::finishInBackground);"));
        assertTrue(coordinator.contains("private void finishInBackground()"));
        assertTrue(coordinator.contains("recordTimelineEvent(\"foreground changed to \" + packageName);"));
        assertFalse(coordinator.contains("private void finish()"));
        assertFalse(coordinator.contains("finish(true);"));
        assertFalse(coordinator.contains("finish(false);"));
    }

    @Test
    public void feedbackDiagnosticLaunchesTargetThroughRootRestartOnly()
            throws IOException {
        String main = read("src/main/java/com/dpis/module/MainActivity.java");
        String launcher = read(
                "src/main/java/com/dpis/module/diagnostics/FeedbackDiagnosticAppLauncher.java");
        String rootLauncher = read("src/main/java/com/dpis/module/root/RootAppProcessLauncher.java");

        assertTrue(main.contains("new FeedbackDiagnosticAppLauncher(this)"));
        assertTrue(main.contains("restartTargetAppForDiagnostic(String packageName)"));
        assertTrue(main.contains(".restartForDiagnostic(packageName)"));
        assertFalse(main.contains("public boolean launchTargetApp(String packageName)"));

        assertTrue(launcher.contains("new RootAppProcessLauncher(context)"));
        assertTrue(launcher.contains("rootLauncher.restart(packageName).code() == 0"));
        assertTrue(rootLauncher.contains("am force-stop \" + packageName"));
        assertTrue(rootLauncher.contains("am start --user current"));
        assertTrue(rootLauncher.contains("-a android.intent.action.MAIN"));
        assertTrue(rootLauncher.contains("-c android.intent.category.LAUNCHER"));
        assertTrue(rootLauncher.contains("flattenToShortString()"));
        assertTrue(rootLauncher.contains("shellQuote("));
        assertTrue(rootLauncher.contains("isSafePackageName(packageName)"));
        assertFalse(launcher.contains("startActivity("));
    }

    @Test
    public void feedbackDiagnosticForegroundObserverUsesRootTopAppSnapshot()
            throws IOException {
        String reader = read(
                "src/main/java/com/dpis/module/diagnostics/FeedbackDiagnosticForegroundAppReader.java");

        assertTrue(reader.contains("new ProcessBuilder(\"su\", \"-c\", COMMAND)"));
        assertTrue(reader.contains("dumpsys activity activities"));
        assertTrue(reader.contains("dumpsys window"));
        assertTrue(reader.contains("parsePackage("));
    }

    @Test
    public void feedbackDiagnosticResultSupportsShareAndSaveZip() throws IOException {
        String main = read("src/main/java/com/dpis/module/MainActivity.java");
        String exportBuilder = read(
                "src/main/java/com/dpis/module/diagnostics/FeedbackDiagnosticExportBuilder.java");
        String moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java");
        String resultSheet = read(
                "src/main/java/com/dpis/module/diagnostics/FeedbackDiagnosticResultSheet.kt");
        String forceTextSize = read("src/main/java/com/dpis/module/runtime/font/ForceTextSizeHookInstaller.java");
        String paintFallback = read(
                "src/main/java/com/dpis/module/runtime/font/PaintTextSizeFallbackHookInstaller.java");
        String webViewFont = read("src/main/java/com/dpis/module/runtime/font/WebViewFontHookInstaller.java");
        String modernWechat = read(
                "src/modern/java/com/dpis/module/WechatDpiModernHookInstaller.java");
        String modernAppSpecific = read(
                "src/modern/java/com/dpis/module/ModernAppSpecificRouteInstaller.java");
        String legacyWechat = read(
                "src/legacy/java/com/dpis/module/WechatDpiLegacyHookInstaller.java");
        String legacyAppSpecific = read(
                "src/legacy/java/com/dpis/module/LegacyAppSpecificRouteInstaller.java");

        assertTrue(main.contains("REQUEST_SAVE_FEEDBACK_DIAGNOSTIC"));
        assertTrue(main.contains("Intent.ACTION_CREATE_DOCUMENT"));
        assertTrue(main.contains("FeedbackDiagnosticExportBuilder.MIME_TYPE"));
        assertTrue(main.contains("getContentResolver().openOutputStream(uri)"));
        assertFalse(main.contains("getContentResolver().openOutputStream(uri, \"wt\")"));
        assertTrue(main.contains("feedbackDiagnosticExportBuilder.buildPackage(result)"));
        assertTrue(main.contains("FileProvider.getUriForFile"));
        assertTrue(main.contains("Intent.ACTION_SEND"));
        assertTrue(main.contains("putExtra(Intent.EXTRA_STREAM, uri)"));
        assertFalse(main.contains("putExtra(Intent.EXTRA_TEXT, result.summary)"));
        assertTrue(exportBuilder.contains("DIAGNOSTIC_ENTRY_NAME = \"diagnostic.txt\""));
        assertTrue(exportBuilder.contains("DPIS_LOG_ENTRY_NAME = \"dpis-log.txt\""));
        assertTrue(exportBuilder.contains("LSPOSED_LOG_ENTRY_NAME = \"lsposed-log.txt\""));
        assertTrue(exportBuilder.contains("FeedbackDiagnosticLsposedTimelineParser.parse("));
        assertTrue(exportBuilder.contains("FeedbackDiagnosticSessionWindow.around("));
        assertTrue(exportBuilder.contains("filterDpisEntries("));
        assertTrue(exportBuilder.contains("windowRawLog("));
        assertTrue(exportBuilder.contains("[manifest]"));
        assertTrue(exportBuilder.contains("[app-config]"));
        assertTrue(exportBuilder.contains("[diagnostic-plan]"));
        assertTrue(exportBuilder.contains("[runtime-summary]"));
        assertTrue(exportBuilder.contains("[runtime-density]"));
        assertTrue(exportBuilder.contains("[runtime-anomalies]"));
        assertTrue(exportBuilder.contains("[runtime-timeline]"));
        assertTrue(exportBuilder.contains("[runtime-self-test]"));
        assertTrue(exportBuilder.contains("[raw-log]"));
        assertTrue(exportBuilder.contains("versionName: "));
        assertTrue(exportBuilder.contains("static final class DiagnosticPackage"));
        assertTrue(exportBuilder.contains("static final class EntrySummary"));
        assertTrue(exportBuilder.contains("DiagnosticPackage buildPackage("));
        assertTrue(forceTextSize.contains("FeedbackDiagnosticRuntimeHotPathEvents.begin("));
        assertTrue(moduleMain.contains("FeedbackDiagnosticRuntimeHotPathEvents.probe("));
        assertTrue(moduleMain.contains("\"process_entry\""));
        assertTrue(forceTextSize.contains("\"text_appearance\""));
        assertTrue(forceTextSize.contains("\"textview_sp_rewrite\""));
        assertTrue(forceTextSize.contains("\"textview_absolute_rewrite\""));
        assertTrue(forceTextSize.contains("\"textview_current_px_fallback\""));
        assertTrue(forceTextSize.contains("\"textview_span_rewrite\""));
        assertTrue(forceTextSize.contains("\"paint_text_size_fallback\""));
        assertTrue(forceTextSize.contains("\"textpaint_text_size_fallback\""));
        assertTrue(paintFallback.contains("\"paint_fallback\""));
        assertTrue(webViewFont.contains("\"webview_text_zoom\""));
        assertTrue(webViewFont.contains("\"x5_webview_text_zoom\""));
        assertTrue(exportBuilder.contains("wechatDpiRoute: selected"));
        assertTrue(modernWechat.contains("\"wechat_dpi\""));
        assertTrue(modernWechat.contains("\"displaymetrics\""));
        assertTrue(modernWechat.contains("\"bottom_tab_icon\""));
        assertTrue(modernWechat.contains("modern WeChat DPI route plan: "));
        assertTrue(modernWechat.contains("retiredTargets="));
        assertTrue(modernWechat.contains("retiredActive=false"));
        assertTrue(modernWechat.contains("firstCallbackMethod="));
        assertTrue(modernWechat.contains("appliedMethod="));
        assertTrue(modernAppSpecific.contains("\"wechat_dpi\""));
        assertTrue(modernAppSpecific.contains("\"application_attach\""));
        assertTrue(modernAppSpecific.contains("application-attach retry result"));
        assertTrue(modernAppSpecific.contains("retryInstallAttempted=true, installed="));
        assertFalse(modernAppSpecific.contains("\"module_loaded_class\""));
        assertTrue(legacyWechat.contains("\"wechat_dpi\""));
        assertTrue(legacyAppSpecific.contains("\"legacy_load_package\""));
        assertTrue(resultSheet.contains("BottomSheetDialog(activity)"));
        assertTrue(resultSheet.contains("FeedbackDiagnosticResultContent("));
        assertTrue(resultSheet.contains("R.string.feedback_diagnostic_result_entry_meta"));
        assertFalse(resultSheet.contains("bindStatusChips(statusChips, result);"));
        assertTrue(resultSheet.contains("host.shareFeedbackDiagnostic(diagnosticPackage)"));
        assertTrue(resultSheet.contains("host.saveFeedbackDiagnostic(diagnosticPackage)"));
        assertTrue(resultSheet.contains("R.string.feedback_diagnostic_result_privacy_hint"));
        assertFalse(resultSheet.contains("result.summary"));
        assertTrue(resultSheet.contains("R.string.feedback_diagnostic_save_action"));
        assertTrue(resultSheet.contains("R.string.feedback_diagnostic_share_action"));
        assertTrue(resultSheet.contains("FeedbackDiagnosticPackagingDialog"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
