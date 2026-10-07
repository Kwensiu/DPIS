package com.dpis.module.diagnostics

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackDiagnosticSourceSmokeTest {
    @Test
    fun composeEditorExposesFeedbackDiagnosticEntry() {
        val gateway = read(
            "src/main/java/com/dpis/module/appconfig/presentation/ComposeAppEditorActivityGateway.kt",
        )
        val overlay = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspacePresentationCoordinator.kt",
        )

        assertTrue(gateway.contains("feedbackDiagnostic?.showPreparation("))
        assertTrue(overlay.contains("AppConfigSheetUiTokens.FeedbackActionSize"))
        assertTrue(overlay.contains("R.drawable.ic_bug_report_24") || overlay.contains("feedback_diagnostic"))
    }

    @Test
    fun feedbackDiagnosticOwnsSessionOutsideActivity() {
        val startup = read("src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt")

        // The Activity session boundary is wiring-only and cannot be called from this JVM test.
        assertTrue(startup.contains("FeedbackDiagnosticActivitySession("))
    }

    @Test
    fun feedbackDiagnosticMirrorsRuntimeDpisLogEvents() {
        val dpisLog = read("src/main/java/com/dpis/module/diagnostics/DpisLog.kt")
        val collector = read(
            "src/main/java/com/dpis/module/diagnostics/RuntimeEvents.kt"
        )
        val hotPath = read(
            "src/main/java/com/dpis/module/diagnostics/RuntimeHotPathEvents.kt"
        )

        assertTrue(dpisLog.contains("private fun write("))
        assertTrue(dpisLog.contains("RuntimeEvents.recordDpisLog("))
        assertTrue(dpisLog.contains("RuntimeTransport.record("))
        assertTrue(hotPath.contains("RuntimeEvents.recordStructured("))
        assertTrue(hotPath.contains("RuntimeTransport.record("))
        assertTrue(hotPath.contains("fun begin("))
        assertTrue(hotPath.contains("fun applied("))
        assertTrue(hotPath.contains("fun end("))
        assertTrue(collector.contains("private var activeSession: Session?"))
        assertTrue(collector.contains("\"unexpected_route_hit\""))
        assertTrue(collector.contains("\"repeated_write\""))
    }

    @Test
    fun feedbackDiagnosticLaunchesTargetThroughRootRestartOnly() {
        val sessionOwner = read(
            "src/main/java/com/dpis/module/diagnostics/presentation/FeedbackDiagnosticActivitySession.kt"
        )
        val launcher = read(
            "src/main/java/com/dpis/module/diagnostics/device/TargetAppLauncher.kt"
        )
        val rootLauncher =
            read("src/main/java/com/dpis/module/root/RootAppProcessLauncher.kt")

        assertTrue(sessionOwner.contains("TargetAppLauncher(activity)"))
        assertTrue(sessionOwner.contains("restartTargetAppForDiagnostic("))
        assertTrue(sessionOwner.contains("launcher.restartForDiagnostic(packageName)"))
        assertFalse(sessionOwner.contains("public boolean launchTargetApp(String packageName)"))

        assertTrue(launcher.contains("RootAppProcessLauncher(context)"))
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
            "src/main/java/com/dpis/module/diagnostics/device/ForegroundAppReader.kt"
        )

        assertTrue(reader.contains("SecureProcessLauncher.startMerged(\"su\", \"-c\", COMMAND)"))
        assertTrue(reader.contains("dumpsys activity activities"))
        assertTrue(reader.contains("dumpsys window"))
        assertTrue(reader.contains("parsePackage("))
    }

    @Test
    fun feedbackDiagnosticResultSupportsShareAndSaveZip() {
        val main = read("src/main/java/com/dpis/module/MainActivity.kt")
        val packageActions = read(
            "src/main/java/com/dpis/module/diagnostics/PackageActions.kt"
        )
        val exportBuilder = read(
            "src/main/java/com/dpis/module/diagnostics/ExportBuilder.kt"
        )
        val reportText = read(
            "src/main/java/com/dpis/module/diagnostics/DiagnosticReportText.kt"
        )
        val logExcerpt = read(
            "src/main/java/com/dpis/module/diagnostics/DiagnosticLogExcerpt.kt"
        )
        val moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java")
        val resultSheet = read(
            "src/main/java/com/dpis/module/diagnostics/presentation/ResultSheet.kt"
        )
        val forceTextSize =
            read("src/main/java/com/dpis/module/runtime/font/ForceTextSizeHookRuntime.kt")
        val paintTextSize =
            read("src/main/java/com/dpis/module/runtime/font/PaintTextSizeHookInstaller.kt")
        val textViewAppearance =
            read("src/main/java/com/dpis/module/runtime/font/TextViewAppearanceHookInstaller.kt")
        val textViewSetText =
            read("src/main/java/com/dpis/module/runtime/font/TextViewSetTextHookInstaller.kt")
        val textViewAttach =
            read("src/main/java/com/dpis/module/runtime/font/TextViewAttachHookInstaller.kt")
        val webViewFont =
            read("src/main/java/com/dpis/module/runtime/font/WebViewFontHookInstaller.kt")
        val modernWechat = read(
            "src/modern/java/com/dpis/module/wechat/WechatDpiModernHookInstaller.kt"
        )
        val modernBottomTab = read(
            "src/modern/java/com/dpis/module/wechat/WechatDpiModernBottomTabHookInstaller.kt"
        )
        val modernAppSpecific = read(
            "src/modern/java/com/dpis/module/ModernAppSpecificRouteInstaller.kt"
        )
        val legacyWechat = read(
            "src/legacy/java/com/dpis/module/WechatDpiLegacyHookInstaller.kt"
        )
        val legacyAppSpecific = read(
            "src/legacy/java/com/dpis/module/LegacyAppSpecificRouteInstaller.kt"
        )

        assertTrue(
            read("src/main/java/com/dpis/module/diagnostics/presentation/FeedbackDiagnosticActivitySession.kt")
                .contains("const val SAVE_REQUEST = 10024")
        )
        val startupResult = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )
        assertTrue(startupResult.contains("feedbackDiagnostic?.handleActivityResult("))
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
        assertTrue(logExcerpt.contains("LsposedTimelineParser.parse("))
        assertTrue(logExcerpt.contains("SessionWindow.around("))
        assertTrue(logExcerpt.contains("filterDpisEntries("))
        assertTrue(logExcerpt.contains("windowRawLog("))
        assertTrue(reportText.contains("[manifest]"))
        assertTrue(reportText.contains("[app-config]"))
        assertTrue(reportText.contains("[diagnostic-plan]"))
        assertTrue(reportText.contains("[runtime-summary]"))
        assertTrue(reportText.contains("[runtime-density]"))
        assertTrue(reportText.contains("[runtime-anomalies]"))
        assertTrue(reportText.contains("[wechat-dpi-evidence]"))
        assertTrue(reportText.contains("[runtime-timeline]"))
        assertTrue(reportText.contains("[runtime-self-test]"))
        assertTrue(reportText.contains("[raw-log]"))
        assertTrue(reportText.contains("versionName: "))
        assertTrue(exportBuilder.contains("class DiagnosticPackage"))
        assertTrue(exportBuilder.contains("class EntrySummary"))
        assertTrue(exportBuilder.contains("fun buildPackage("))
        assertTrue(forceTextSize.contains("RuntimeHotPathEvents.begin("))
        assertTrue(moduleMain.contains("RuntimeHotPathEvents.probe("))
        assertTrue(moduleMain.contains("\"process_entry\""))
        assertTrue(textViewAppearance.contains("\"text_appearance\""))
        assertTrue(forceTextSize.contains("\"textview_sp_rewrite\""))
        assertTrue(forceTextSize.contains("\"textview_absolute_rewrite\""))
        assertTrue(forceTextSize.contains("\"textview_current_px_fallback\""))
        assertTrue(forceTextSize.contains("fun currentPxFallbackDetail("))
        assertTrue(textViewAttach.contains("currentPxFallbackDetail("))
        assertTrue(textViewSetText.contains("\"textview_span_rewrite\""))
        assertTrue(paintTextSize.contains("\"paint_text_size_fallback\""))
        assertTrue(paintTextSize.contains("\"textpaint_text_size_fallback\""))
        assertTrue(webViewFont.contains("\"webview_text_zoom\""))
        assertTrue(webViewFont.contains("\"x5_webview_text_zoom\""))
        assertTrue(reportText.contains("wechatDpiRoute: selected"))
        assertTrue(modernWechat.contains("\"wechat_dpi\""))
        assertTrue(modernWechat.contains("\"displaymetrics\""))
        assertTrue(modernBottomTab.contains("\"bottom_tab_icon\""))
        assertTrue(modernBottomTab.contains("init_method_not_found"))
        assertTrue(modernWechat.contains("modern WeChat DPI route plan: "))
        assertTrue(modernWechat.contains("retiredTargets="))
        assertTrue(modernWechat.contains("retiredActive=false"))
        assertTrue(modernBottomTab.contains("mutation_applied"))
        assertTrue(modernAppSpecific.contains("WechatDpiRouteCoordinator"))
        assertFalse(modernAppSpecific.contains("\"module_loaded_class\""))
        assertTrue(legacyWechat.contains("\"wechat_dpi\""))
        assertTrue(legacyAppSpecific.contains("\"legacy_load_package\""))
        assertTrue(resultSheet.contains("ModalSheet(onDismissRequest = dismiss)"))
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
        assertTrue(resultSheet.contains("ComposeOverlay.show(activity)"))
        assertFalse(resultSheet.contains("MaterialAlertDialogBuilder"))
    }

    companion object {
        private fun read(relativePath: String?): String {
            return SourceSmokeTestPaths.read(relativePath).replace("\r\n", "\n")
        }
    }
}
