package com.dpis.module.diagnostics

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackDiagnosticSourceSmokeTest {
    @Test
    fun appConfigSheetExposesFeedbackDiagnosticEntry() {
        val binder =
            read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.kt")
        val actions =
            read("src/main/java/com/dpis/module/appconfig/AppConfigSheetActionBinder.java")
        val layout = read("src/main/res/layout/dialog_app_config.xml")
        val dimens = read("src/main/res/values/dimens.xml")

        assertTrue(layout.contains("dialog_feedback_diagnostic_button"))
        assertTrue(layout.contains("@string/feedback_diagnostic_action"))
        assertTrue(layout.contains("@drawable/ic_bug_report_24"))
        assertTrue(
            layout.contains(
                "android:layout_marginTop=\"@dimen/dialog_feedback_diagnostic_button_margin_top\""
            )
        )
        assertTrue(binder.contains("startFeedbackDiagnostic("))
        assertTrue(binder.contains("feedbackDiagnosticButton"))
        assertTrue(actions.contains("host.startFeedbackDiagnostic(item, state);"))
        assertTrue(
            dimens.contains(
                "<dimen name=\"dialog_feedback_diagnostic_button_size\">32dp</dimen>"
            )
        )
        assertTrue(
            dimens.contains(
                "<dimen name=\"dialog_feedback_diagnostic_button_margin_top\">12dp</dimen>"
            )
        )
    }

    @Test
    fun feedbackDiagnosticUsesCoordinatorInsteadOfMainActivityStateMachine() {
        val main = read("src/main/java/com/dpis/module/MainActivity.java")
        val pageController = read(
            "src/main/java/com/dpis/module/diagnostics/presentation/PageController.kt"
        )
        val packageActions = read(
            "src/main/java/com/dpis/module/diagnostics/PackageActions.kt"
        )
        val preparation = read(
            "src/main/java/com/dpis/module/diagnostics/presentation/"
                    + "FeedbackDiagnosticPreparationContent.kt"
        )
        assertTrue(preparation.contains("FeedbackIconButton("))
        assertTrue(preparation.contains("onClick = presentation::explainLsposedAvailability"))
        assertFalse(preparation.contains(
                "onClick = rememberClickAction(presentation::explainLsposedAvailability)"))
        assertTrue(preparation.contains("onCheckedChange = presentation::setDurationEnabled"))
        assertFalse(preparation.contains(
                "onCheckedChange = rememberClickValueAction(presentation::setDurationEnabled)"))
        val theme = read(
            "src/main/java/com/dpis/module/settings/presentation/ThemeSettingsContent.kt"
        )
        val edgeFade = read(
            "src/main/java/com/dpis/module/ui/presentation/editor/HorizontalScrollEdgeFade.kt"
        )
        val shell = read("src/main/java/com/dpis/module/ui/presentation/MainComposeShellHost.kt")
        val segmentedPolicy = read(
            "src/main/java/com/dpis/module/ui/presentation/workspace/SegmentedListItemPolicy.kt"
        )
        val hookChain = read(
            "src/main/java/com/dpis/module/fonts/presentation/HookChainEditorPage.kt"
        )
        val coordinator = read(
            "src/main/java/com/dpis/module/diagnostics/Coordinator.java"
        )
        val summary = read(
            "src/main/java/com/dpis/module/diagnostics/SummaryBuilder.java"
        )
        val structuredExporter = read(
            "src/main/java/com/dpis/module/diagnostics/"
                    + "StructuredEvidenceExporter.java"
        )
        val logGate = read(
            "src/main/java/com/dpis/module/diagnostics/presentation/LogGate.kt"
        )
        val confirm = read(
            "src/main/java/com/dpis/module/diagnostics/presentation/FeedbackDiagnosticConfirm.kt"
        )
        val sessionOwner = read(
            "src/main/java/com/dpis/module/diagnostics/presentation/FeedbackDiagnosticActivitySession.kt"
        )
        val duration = read(
            "src/main/java/com/dpis/module/diagnostics/FeedbackDiagnosticDuration.kt"
        )
        val diagnosticShell = read("src/main/java/com/dpis/module/diagnostics/presentation/FeedbackDiagnosticShell.kt")

        assertTrue(main.contains("new FeedbackDiagnosticActivitySession("))
        assertTrue(main.contains("new FeedbackDiagnosticShell(this)"))
        assertTrue(main.contains("feedbackDiagnostic.startFromViewEditor("))
        assertTrue(main.contains("feedbackDiagnostic.showPreparation("))
        assertTrue(main.contains("feedbackDiagnostic.restorePage()"))
        assertTrue(main.contains("feedbackDiagnostic.attachHost()"))
        assertTrue(main.contains("feedbackDiagnostic.onDestroy(isChangingConfigurations())"))
        assertFalse(main.contains("private Session.Host createFeedbackDiagnosticHost()"))
        assertFalse(main.contains("createDiagnosticPageControllerHost()"))
        assertTrue(confirm.contains("LogGate.isEnabled("))
        assertTrue(confirm.contains("host.showEnableLogsConfirm("))
        assertTrue(logGate.contains("ConfirmDialog.showWithLabels("))
        assertTrue(confirm.contains("whenLogsEnabled"))
        assertTrue(confirm.contains("showStart(item.label)"))
        assertTrue(sessionOwner.contains("persistComposeEditor("))
        assertTrue(sessionOwner.contains("EditorDialogStateFactory.create("))
        assertTrue(diagnosticShell.contains("persistComposeEditor("))
        assertTrue(confirm.contains("ConfirmDialog.showWithLabels("))
        assertTrue(diagnosticShell.contains("activity.resolvePackageVersionName(packageName)"))
        assertTrue(confirm.contains("session.get().start("))
        assertTrue(pageController.contains("selectedDurationSeconds()"))
        assertTrue(pageController.contains("isDurationEnabled()"))
        assertTrue(pageController.contains("AppLocaleManager.wrap(context.applicationContext)"))
        assertTrue(sessionOwner.contains("session.attachHost(host)"))
        assertTrue(sessionOwner.contains("session.detachHost()"))
        assertTrue(confirm.contains("ComposeMessageDialog.show("))
        assertTrue(pageController.contains("current.lsposedStatus()"))
        assertTrue(sessionOwner.contains("pageController.presentation()?.markStartFailed()"))
        assertTrue(duration.contains("fun format(durationMs: Long)"))
        assertTrue(sessionOwner.contains("feedback_diagnostic_auto_finished"))
        assertTrue(pageController.contains("target.updateEnvironment("))
        assertTrue(pageController.contains("refreshEnvironment(created, refreshLsposed = true)"))
        assertTrue(pageController.contains("current.lsposedAvailabilityCode()"))
        assertTrue(pageController.contains("refreshLsposedAvailability"))
        assertTrue(pageController.contains("discardDiagnostic()"))
        assertTrue(sessionOwner.contains("Coordinator.Request.fromPersisted("))
        assertTrue(sessionOwner.contains("session.diagnosticPackage()"))
        assertTrue(sessionOwner.contains("confirm.onPageBack("))
        assertTrue(sessionOwner.contains("session.cancel()"))
        assertTrue(confirm.contains("feedback_diagnostic_exit_confirm_message"))
        assertTrue(preparation.contains("SecondaryPageScaffold("))
        assertTrue(preparation.contains("feedback_diagnostic_target_section"))
        assertTrue(preparation.contains("rememberInstalledAppIcon(state.packageName, state.appIcon)"))
        assertTrue(preparation.contains("Text(\"v\${state.versionName}\")"))
        assertTrue(preparation.contains("feedback_diagnostic_status_section"))
        assertTrue(preparation.contains("DiagnosticResultPlaceholder()"))
        assertTrue(preparation.contains("DiagnosticOutputDetails(state, presentation)"))
        assertTrue(preparation.contains("feedback_diagnostic_discard_and_restart_action"))
        assertTrue(preparation.contains("navigationBarsPadding()"))
        assertTrue(preparation.contains("RoundedCornerShape(18.dp)"))
        assertTrue(preparation.contains("color = MaterialTheme.colorScheme.surfaceContainer"))
        assertTrue(preparation.contains("surfaceBright"))
        assertTrue(preparation.contains("ButtonDefaults.buttonColors("))
        assertTrue(preparation.contains("disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh"))
        assertTrue(preparation.contains("DiagnosticOutputFileCard(entry)"))
        assertTrue(preparation.contains("DiagnosticOutputFileBackdrop()"))
        assertTrue(preparation.contains("basicMarquee(animationMode = MarqueeAnimationMode.Immediately)"))
        assertTrue(preparation.contains("presentation.copyPackagePath(state.packagePath)"))
        assertTrue(preparation.contains("feedback_diagnostic_output_pending"))
        assertTrue(preparation.contains("SegmentedListItem("))
        assertTrue(preparation.contains("Switch("))
        assertTrue(preparation.contains("setDurationEnabled("))
        assertTrue(preparation.contains("AnimatedConditionalItem(visible = state.durationEnabled)"))
        assertTrue(preparation.contains("DurationChipSelector("))
        assertTrue(edgeFade.contains("horizontalScroll(scrollState)"))
        assertTrue(preparation.contains("HorizontalScrollWithEdgeFade("))
        assertTrue(preparation.contains("R.drawable.ic_view_kanban_24"))
        assertTrue(preparation.contains("R.drawable.ic_healing_24"))
        assertTrue(preparation.contains("R.drawable.ic_hourglass_check_24"))
        assertTrue(theme.contains("HorizontalScrollWithEdgeFade("))
        assertTrue(edgeFade.contains("scrollState.value > 0"))
        assertTrue(edgeFade.contains("scrollState.value < scrollState.maxValue"))
        assertTrue(preparation.contains("CustomDurationDialog("))
        assertTrue(preparation.contains("MAX_DIAGNOSTIC_DURATION_SECONDS = 86_400"))
        assertTrue(preparation.contains("feedback_diagnostic_duration_custom"))
        assertTrue(preparation.contains("DiagnosticLogOutputRow(state.logStatus)"))
        assertTrue(preparation.contains("DiagnosticRootPermissionRow(state, presentation)"))
        assertTrue(preparation.contains("presentation::refreshRootPermission"))
        assertTrue(preparation.contains("DiagnosticLsposedRow(state, presentation)"))
        assertTrue(preparation.contains("presentation::refreshLsposedAvailability"))
        assertTrue(preparation.contains("presentation::explainLsposedAvailability"))
        assertTrue(preparation.contains("LSPOSED_NO_PERMISSION"))
        assertTrue(preparation.contains("LSPOSED_AVAILABLE"))
        assertTrue(pageController.contains("LsposedLogReader.availability(result)"))
        assertTrue(preparation.contains("feedback_diagnostic_recording_title"))
        assertTrue(shell.contains("showDiagnosticPreparation("))
        assertTrue(shell.contains("FeedbackDiagnosticPreparationContent(currentPreparation)"))
        assertTrue(shell.contains("BackHandler(enabled = preparation != null)"))
        assertTrue(segmentedPolicy.contains("count.coerceAtLeast(1)"))
        assertTrue(segmentedPolicy.contains("ListItemDefaults.segmentedShapes(safeIndex, safeCount)"))
        assertTrue(segmentedPolicy.contains("safeCount == 1"))
        assertTrue(segmentedPolicy.contains("RoundedCornerShape(16.dp)"))
        assertTrue(segmentedPolicy.contains("} else {\n        shapes"))
        assertTrue(
            read("src/main/java/com/dpis/module/diagnostics/presentation/FeedbackDiagnosticActivitySession.kt")
                .contains("FeedbackDiagnosticPreparationPresentation.OutputEntry")
        )
        assertTrue(
            read("src/main/java/com/dpis/module/diagnostics/presentation/FeedbackDiagnosticActivitySession.kt")
                .contains("feedback_diagnostic_result_entry_meta")
        )
        assertTrue(packageActions.contains("feedbackDiagnosticSharedCachePath("))
        assertTrue(packageActions.contains("copyFeedbackDiagnosticPath("))
        assertFalse(main.contains("private void saveFeedbackDiagnosticZip("))
        assertFalse(main.contains("private void shareFeedbackDiagnostic("))
        assertFalse(main.contains("private void copyFeedbackDiagnosticPath("))
        assertFalse(main.contains("private void writeSharedFeedbackDiagnosticZip("))
        assertTrue(
            read("src/main/java/com/dpis/module/diagnostics/presentation/FeedbackDiagnosticActivitySession.kt")
                .contains("Formatter.formatFileSize")
        )
        assertTrue(hookChain.contains("val shapes = dpisSegmentedShapes(index, total)"))
        assertFalse(hookChain.contains("SingleItemShape"))
        assertFalse(main.contains("postDelayed(() -> finish("))
        assertFalse(main.contains("summaryBuilder.build("))
        assertTrue(logGate.contains("ConfirmDialog.showWithLabels("))
        assertFalse(logGate.contains("MaterialAlertDialogBuilder"))

        assertTrue(coordinator.contains("handler.postDelayed("))
        assertTrue(coordinator.contains("scheduleFinishAfterDelay(long delayMs)"))
        assertTrue(coordinator.contains("requestFinish(\"diagnostic timer elapsed\")"))
        assertTrue(coordinator.contains("onFeedbackDiagnosticAutoFinished()"))
        assertTrue(coordinator.contains("handler.removeCallbacksAndMessages(null)"))
        assertTrue(coordinator.contains("private final Object timelineLock = new Object();"))
        assertTrue(coordinator.contains("synchronized (timelineLock)"))
        assertTrue(coordinator.contains("RootAccessProbe.probe()"))
        assertTrue(coordinator.contains("ForegroundAppReader.readForegroundPackage()"))
        assertTrue(coordinator.contains("host.restartTargetAppForDiagnostic(request.packageName)"))
        assertTrue(coordinator.contains("handler.post(() -> startAfterRootLaunch(request, launched));"))
        assertTrue(coordinator.contains("runningStartedAtMillis = host.currentTimeMillis();"))
        assertTrue(coordinator.contains("RuntimeTransport.start("))
        assertTrue(coordinator.contains("RuntimeSelfTest.runUiTransportSelfTest("))
        assertTrue(coordinator.contains("RuntimeEvents.start("))
        assertTrue(coordinator.contains("RuntimeEvents.stopSnapshot()"))
        assertTrue(coordinator.contains("RuntimeTransport.stopSnapshot("))
        assertTrue(coordinator.contains("host.onFeedbackDiagnosticRootRequired();"))
        assertTrue(coordinator.contains("handler.post(() -> host.onFeedbackDiagnosticFinished(result));"))
        assertTrue(coordinator.contains("summaryInput(request)"))
        assertTrue(coordinator.contains("static Request fromPersisted("))
        assertFalse(coordinator.contains("DEFAULT_DURATION_MS"))
        assertFalse(coordinator.contains("finish(true), DEFAULT_DURATION_MS"))
        assertFalse(coordinator.contains("bringDpisToFront"))
        assertTrue(summary.contains("source: feedback-diagnostic-summary"))
        assertTrue(summary.contains("versionName: "))
        assertTrue(summary.contains("previewFromGlobalPrefill: "))
        assertTrue(summary.contains("Diagnostic package includes diagnostic.txt"))
        assertTrue(summary.contains("timeline.tsv"))
        assertTrue(summary.contains("module-effects.tsv"))
        assertTrue(summary.contains("runtime transport"))
        assertTrue(summary.contains("public static final class Input"))
        assertFalse(summary.contains("Coordinator.Request"))
        assertFalse(summary.contains("Runtime event capture is TODO"))
        assertTrue(structuredExporter.contains("buildTimelineTsv("))
        assertTrue(structuredExporter.contains("buildModuleEffectsTsv("))
        assertTrue(structuredExporter.contains("target-process-lsposed-aggregate"))
        assertTrue(structuredExporter.contains("target-process-log-fallback"))
        assertTrue(structuredExporter.contains("latency percentiles unavailable"))
    }

    @Test
    fun feedbackDiagnosticMirrorsRuntimeDpisLogEvents() {
        val dpisLog = read("src/main/java/com/dpis/module/DpisLog.java")
        val collector = read(
            "src/main/java/com/dpis/module/diagnostics/RuntimeEvents.java"
        )
        val hotPath = read(
            "src/main/java/com/dpis/module/diagnostics/RuntimeHotPathEvents.java"
        )

        assertTrue(dpisLog.contains("private static void write("))
        assertTrue(dpisLog.contains("RuntimeEvents.recordDpisLog("))
        assertTrue(dpisLog.contains("RuntimeTransport.record("))
        assertTrue(hotPath.contains("RuntimeEvents.recordStructured("))
        assertTrue(hotPath.contains("RuntimeTransport.record("))
        assertTrue(hotPath.contains("static void begin("))
        assertTrue(hotPath.contains("static void applied("))
        assertTrue(hotPath.contains("static void end("))
        assertTrue(collector.contains("private static volatile Session activeSession;"))
        assertTrue(collector.contains("\"unexpected_route_hit\""))
        assertTrue(collector.contains("\"repeated_write\""))
    }

    @Test
    fun feedbackDiagnosticFinishesOnlyAfterReturningToDpis() {
        val coordinator = read(
            "src/main/java/com/dpis/module/diagnostics/Coordinator.java"
        )

        assertTrue(coordinator.contains("recordTimelineEvent(\"foreground returned to DPIS\");"))
        assertTrue(coordinator.contains("onDpisResumed();"))
        assertTrue(coordinator.contains("executor.execute(this::finishInBackground);"))
        assertTrue(coordinator.contains("private void finishInBackground()"))
        assertTrue(coordinator.contains("recordTimelineEvent(\"foreground changed to \" + packageName);"))
        assertFalse(coordinator.contains("private void finish()"))
        assertFalse(coordinator.contains("finish(true);"))
        assertFalse(coordinator.contains("finish(false);"))
    }

    @Test
    fun feedbackDiagnosticLaunchesTargetThroughRootRestartOnly() {
        val sessionOwner = read(
            "src/main/java/com/dpis/module/diagnostics/presentation/FeedbackDiagnosticActivitySession.kt"
        )
        val launcher = read(
            "src/main/java/com/dpis/module/diagnostics/AppLauncher.java"
        )
        val rootLauncher =
            read("src/main/java/com/dpis/module/root/RootAppProcessLauncher.kt")

        assertTrue(sessionOwner.contains("AppLauncher(activity)"))
        assertTrue(sessionOwner.contains("restartTargetAppForDiagnostic("))
        assertTrue(sessionOwner.contains("launcher.restartForDiagnostic(packageName)"))
        assertFalse(sessionOwner.contains("public boolean launchTargetApp(String packageName)"))

        assertTrue(launcher.contains("new RootAppProcessLauncher(context)"))
        assertTrue(launcher.contains("rootLauncher.restart(packageName).code() == 0"))
        assertTrue(rootLauncher.contains("am force-stop \" + packageName"))
        assertTrue(rootLauncher.contains("am start --user current"))
        assertTrue(rootLauncher.contains("-a android.intent.action.MAIN"))
        assertTrue(rootLauncher.contains("-c android.intent.category.LAUNCHER"))
        assertTrue(rootLauncher.contains("flattenToShortString()"))
        assertTrue(rootLauncher.contains("shellQuote("))
        assertTrue(rootLauncher.contains("isSafePackageName(packageName)"))
        assertFalse(launcher.contains("startActivity("))
    }

    @Test
    fun feedbackDiagnosticForegroundObserverUsesRootTopAppSnapshot() {
        val reader = read(
            "src/main/java/com/dpis/module/diagnostics/ForegroundAppReader.kt"
        )

        assertTrue(reader.contains("SecureProcessLauncher.startMerged(\"su\", \"-c\", COMMAND)"))
        assertTrue(reader.contains("dumpsys activity activities"))
        assertTrue(reader.contains("dumpsys window"))
        assertTrue(reader.contains("parsePackage("))
    }

    @Test
    fun feedbackDiagnosticResultSupportsShareAndSaveZip() {
        val main = read("src/main/java/com/dpis/module/MainActivity.java")
        val packageActions = read(
            "src/main/java/com/dpis/module/diagnostics/PackageActions.kt"
        )
        val exportBuilder = read(
            "src/main/java/com/dpis/module/diagnostics/ExportBuilder.java"
        )
        val moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java")
        val resultSheet = read(
            "src/main/java/com/dpis/module/diagnostics/ResultSheet.kt"
        )
        val forceTextSize =
            read("src/main/java/com/dpis/module/runtime/font/ForceTextSizeHookRuntime.kt")
        val paintTextSize =
            read("src/main/java/com/dpis/module/runtime/font/PaintTextSizeHookInstaller.kt")
        val textViewAppearance =
            read("src/main/java/com/dpis/module/runtime/font/TextViewAppearanceHookInstaller.kt")
        val textViewSetText =
            read("src/main/java/com/dpis/module/runtime/font/TextViewSetTextHookInstaller.kt")
        val paintFallback = read(
            "src/main/java/com/dpis/module/runtime/font/PaintTextSizeFallbackHookInstaller.kt"
        )
        val webViewFont =
            read("src/main/java/com/dpis/module/runtime/font/WebViewFontHookInstaller.kt")
        val modernWechat = read(
            "src/modern/java/com/dpis/module/wechat/WechatDpiModernHookInstaller.java"
        )
        val modernAppSpecific = read(
            "src/modern/java/com/dpis/module/ModernAppSpecificRouteInstaller.kt"
        )
        val legacyWechat = read(
            "src/legacy/java/com/dpis/module/WechatDpiLegacyHookInstaller.java"
        )
        val legacyAppSpecific = read(
            "src/legacy/java/com/dpis/module/LegacyAppSpecificRouteInstaller.java"
        )

        assertTrue(
            read("src/main/java/com/dpis/module/diagnostics/presentation/FeedbackDiagnosticActivitySession.kt")
                .contains("const val SAVE_REQUEST = 10024")
        )
        assertTrue(main.contains("feedbackDiagnostic.handleActivityResult("))
        assertTrue(packageActions.contains("Intent.ACTION_CREATE_DOCUMENT"))
        assertTrue(packageActions.contains("ExportBuilder.MIME_TYPE"))
        assertTrue(packageActions.contains("openOutputStream(uri)"))
        assertFalse(packageActions.contains("openOutputStream(uri, \"wt\")"))
        assertTrue(
            read("src/main/java/com/dpis/module/diagnostics/presentation/FeedbackDiagnosticActivitySession.kt")
                .contains("session.diagnosticPackage()")
        )
        assertTrue(packageActions.contains("FileProvider.getUriForFile"))
        assertTrue(packageActions.contains("Intent.ACTION_SEND"))
        assertTrue(packageActions.contains("putExtra(Intent.EXTRA_STREAM, uri)"))
        assertFalse(packageActions.contains("putExtra(Intent.EXTRA_TEXT, result.summary)"))
        assertTrue(exportBuilder.contains("DIAGNOSTIC_ENTRY_NAME = \"diagnostic.txt\""))
        assertTrue(exportBuilder.contains("DPIS_LOG_ENTRY_NAME = \"dpis-log.txt\""))
        assertTrue(exportBuilder.contains("LSPOSED_LOG_ENTRY_NAME = \"lsposed-log.txt\""))
        assertTrue(exportBuilder.contains("LsposedTimelineParser.parse("))
        assertTrue(exportBuilder.contains("SessionWindow.around("))
        assertTrue(exportBuilder.contains("filterDpisEntries("))
        assertTrue(exportBuilder.contains("windowRawLog("))
        assertTrue(exportBuilder.contains("[manifest]"))
        assertTrue(exportBuilder.contains("[app-config]"))
        assertTrue(exportBuilder.contains("[diagnostic-plan]"))
        assertTrue(exportBuilder.contains("[runtime-summary]"))
        assertTrue(exportBuilder.contains("[runtime-density]"))
        assertTrue(exportBuilder.contains("[runtime-anomalies]"))
        assertTrue(exportBuilder.contains("[wechat-dpi-evidence]"))
        assertTrue(exportBuilder.contains("[runtime-timeline]"))
        assertTrue(exportBuilder.contains("[runtime-self-test]"))
        assertTrue(exportBuilder.contains("[raw-log]"))
        assertTrue(exportBuilder.contains("versionName: "))
        assertTrue(exportBuilder.contains("static final class DiagnosticPackage"))
        assertTrue(exportBuilder.contains("static final class EntrySummary"))
        assertTrue(exportBuilder.contains("DiagnosticPackage buildPackage("))
        assertTrue(forceTextSize.contains("RuntimeHotPathEvents.begin("))
        assertTrue(moduleMain.contains("RuntimeHotPathEvents.probe("))
        assertTrue(moduleMain.contains("\"process_entry\""))
        assertTrue(textViewAppearance.contains("\"text_appearance\""))
        assertTrue(forceTextSize.contains("\"textview_sp_rewrite\""))
        assertTrue(forceTextSize.contains("\"textview_absolute_rewrite\""))
        assertTrue(forceTextSize.contains("\"textview_current_px_fallback\""))
        assertTrue(textViewSetText.contains("\"textview_span_rewrite\""))
        assertTrue(paintTextSize.contains("\"paint_text_size_fallback\""))
        assertTrue(paintTextSize.contains("\"textpaint_text_size_fallback\""))
        assertTrue(paintFallback.contains("\"paint_fallback\""))
        assertTrue(webViewFont.contains("\"webview_text_zoom\""))
        assertTrue(webViewFont.contains("\"x5_webview_text_zoom\""))
        assertTrue(exportBuilder.contains("wechatDpiRoute: selected"))
        assertTrue(modernWechat.contains("\"wechat_dpi\""))
        assertTrue(modernWechat.contains("\"displaymetrics\""))
        assertTrue(modernWechat.contains("\"bottom_tab_icon\""))
        assertTrue(modernWechat.contains("reason=init_method_not_found"))
        assertTrue(modernWechat.contains("modern WeChat DPI route plan: "))
        assertTrue(modernWechat.contains("retiredTargets="))
        assertTrue(modernWechat.contains("retiredActive=false"))
        assertTrue(modernWechat.contains("firstCallbackMethod="))
        assertTrue(modernWechat.contains("appliedMethod="))
        assertTrue(modernAppSpecific.contains("WechatDpiRouteCoordinator"))
        assertFalse(modernAppSpecific.contains("\"module_loaded_class\""))
        assertTrue(legacyWechat.contains("\"wechat_dpi\""))
        assertTrue(legacyAppSpecific.contains("\"legacy_load_package\""))
        assertTrue(resultSheet.contains("BottomSheetDialog(activity)"))
        assertTrue(resultSheet.contains("FeedbackDiagnosticResultContent("))
        assertTrue(resultSheet.contains("R.string.feedback_diagnostic_result_entry_meta"))
        assertFalse(resultSheet.contains("bindStatusChips(statusChips, result);"))
        assertTrue(resultSheet.contains("host.shareFeedbackDiagnostic(diagnosticPackage)"))
        assertTrue(resultSheet.contains("host.saveFeedbackDiagnostic(diagnosticPackage)"))
        assertTrue(resultSheet.contains("R.string.feedback_diagnostic_result_privacy_hint"))
        assertFalse(resultSheet.contains("result.summary"))
        assertTrue(resultSheet.contains("R.string.feedback_diagnostic_save_action"))
        assertTrue(resultSheet.contains("R.string.feedback_diagnostic_share_action"))
        assertTrue(resultSheet.contains("PackagingDialog"))
    }

    companion object {
        private fun read(relativePath: String?): String {
            return SourceSmokeTestPaths.read(relativePath).replace("\r\n", "\n")
        }
    }
}
