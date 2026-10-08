package com.dpis.module.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportBuilderEvidenceTest {
    @Test
    fun performanceSummaryPrefersTargetProcessAggregate() {
        val text = ExportBuilderFixtures.builder().buildDiagnosticText(
            ExportBuilderFixtures.result(
                listOf(
                    "11-15 06:13:20.100 source=runtime-transport category=performance " +
                            "route=runtime stage=aggregate package=com.example.app " +
                            "message=process=com.example.app,pid=123;route=paint_fallback,calls=20,applied=3,skipped=17,measuredCalls=3,p50Us=4,p95Us=20,p99Us=20,maxUs=30",
                ),
            ),
        )

        assertTrue(text.contains("source: target-process-lsposed-aggregate"))
        assertTrue(text.contains("route: paint_fallback,calls=20"))
        assertFalse(text.contains("source: ui-process-fallback"))
    }

    @Test
    fun performanceSummaryFallsBackToTargetMutationLogs() {
        val text = ExportBuilderFixtures.builder().buildDiagnosticText(
            ExportBuilderFixtures.result(
                listOf(
                    "11-15 06:13:20.100 source=lsposed-log category=runtime route=font " +
                            "stage=mutation_applied level=I package=com.example.app process=com.example.app " +
                            "message=DPIS DPIS_FONT Paint.setTextSize fallback applied: package=com.example.app, hookId=paint_set_text_size",
                ),
            ),
        )

        assertTrue(text.contains("source: target-process-log-fallback"))
        assertTrue(text.contains("route: paint_set_text_size,calls=1,applied=1"))
    }

    @Test
    fun timelineOrdersEventsAndKeepsSlowMutationBreakdown() {
        val entries = ExportBuilderFixtures.unzip(
            ExportBuilderFixtures.builder().buildZip(
                ExportBuilderFixtures.result(
                    listOf(
                        "11-15 06:13:20.300 source=runtime-hotpath category=runtime route=font stage=end routeName=paint_text_size package=com.example.app process=com.example.app message=durationMs=2",
                        "11-15 06:13:20.100 source=runtime-hotpath category=runtime route=font stage=begin routeName=paint_text_size package=com.example.app process=com.example.app message=view=TextView",
                        "11-15 06:13:20.200 source=runtime-hotpath category=font route=textview_sp_rewrite stage=slow_mutation_breakdown package=com.example.app process=com.example.app message=frameworkUs=5200,bookkeepingUs=40,totalUs=5240",
                    ),
                ),
            ),
        )
        val timeline = entries.getValue("timeline.tsv")

        assertTrue(timeline.indexOf("06:13:20.100") < timeline.indexOf("06:13:20.300"))
        assertTrue(timeline.contains("slow_mutation_breakdown"))
        assertTrue(timeline.contains("frameworkUs=5200"))
    }

    @Test
    fun moduleEffectsUseObservedTargetAggregate() {
        val entries = ExportBuilderFixtures.unzip(
            ExportBuilderFixtures.builder().buildZip(
                ExportBuilderFixtures.result(
                    listOf(
                        "11-15 06:13:20.100 source=runtime-transport category=performance route=runtime stage=aggregate package=com.example.app message=process=com.example.app,pid=123;route=paint_fallback,calls=20,applied=3,skipped=17,measuredCalls=3,p50Us=4,p95Us=20,p99Us=20,maxUs=30",
                    ),
                ),
            ),
        )
        assertTrue(entries.getValue("module-effects.tsv").contains("paint_fallback"))
    }

    @Test
    fun emptyRuntimeAnalysisKeepsRawEvidenceNote() {
        val text = ExportBuilderFixtures.builder().buildDiagnosticText(
            ExportBuilderFixtures.result(emptyList()),
        )

        assertTrue(text.contains("[runtime-summary]\ntimelineEvents: 0\nruntimeEvents: 0"))
        assertTrue(text.contains("[runtime-anomalies]\nnone observed"))
        assertTrue(
            ExportBuilderFixtures.section(text, "[runtime-timeline]", "[raw-log]")
                .contains("no runtime events captured; see lsposed-log.txt for raw evidence"),
        )
    }

    @Test
    fun wechatDpiConfigAddsAppSpecificDiagnosticPlan() {
        val text = ExportBuilderFixtures.builder().buildDiagnosticText(
            ExportBuilderFixtures.result(emptyList(), "com.tencent.mm", 600),
        )

        assertTrue(
            ExportBuilderFixtures.section(text, "[app-config]", "[diagnostic-plan]")
                .contains("appSpecific: wechatDpi=600")
        )
        assertTrue(
            ExportBuilderFixtures.section(text, "[diagnostic-plan]", "[runtime-summary]")
                .contains("wechatDpiRoute: selected (targetDpi=600)")
        )
    }

    @Test
    fun runtimeAnalysisKeepsTimelineAndFlagsRepeatedWrites() {
        val warning =
            "11-14 22:13:21.100 source=runtime-hotpath category=warning route=font stage=repeated_write level=W package=com.example.app process=com.example.app message=same runtime hot path repeated within 300ms"
        val longEvent =
            "11-14 22:13:21.200 source=runtime-hotpath category=runtime route=font stage=end routeName=paint_text_size_fallback level=I package=com.example.app process=com.example.app message=durationMs=17"
        val text = ExportBuilderFixtures.builder().buildDiagnosticText(
            ExportBuilderFixtures.result(listOf(warning, longEvent)),
        )

        assertTrue(text.indexOf("[runtime-summary]") < text.indexOf("[runtime-timeline]"))
        assertTrue(text.contains("stageCounts: repeated_write=1"))
        assertTrue(
            ExportBuilderFixtures.section(text, "[runtime-timeline]", "[runtime-self-test]")
                .contains(warning)
        )
        assertTrue(text.contains("maxDurationMs: 17"))
    }

    @Test
    fun runtimeSelfTestReportsLsposedHotpathProbe() {
        val raw = "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] " +
                "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] " +
                "DPIS DPIS_DIAG_HOTPATH route=font stage=probe routeName=process_entry " +
                "package=com.example.app detail=process-entry"
        val text = ExportBuilderFixtures.builder(rawLog = raw)
            .buildDiagnosticText(ExportBuilderFixtures.result(emptyList()))

        assertTrue(text.contains("[runtime-self-test]"))
        assertTrue(text.contains("lsposedHotpathProbe: found"))
    }

    @Test
    fun runtimeSelfTestReportsWechatHotpathProbe() {
        val raw = "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] " +
                "(com.tencent.mm)[io.github.kwensiu.dpis,DPIS,id,0,1] " +
                "DPIS DPIS_DIAG_HOTPATH route=wechat_dpi stage=mutation_applied " +
                "routeName=displaymetrics package=com.tencent.mm detail=targetDpi=390"
        val text = ExportBuilderFixtures.builder(rawLog = raw).buildDiagnosticText(
            ExportBuilderFixtures.result(emptyList(), "com.tencent.mm", 390),
        )

        assertTrue(text.contains("lsposedHotpathProbe: found"))
    }

    @Test
    fun timelineExcludesUnstructuredCoordinatorNotes() {
        val entries = ExportBuilderFixtures.unzip(
            ExportBuilderFixtures.builder().buildZip(
                ExportBuilderFixtures.result(
                    listOf(
                        "11-15 06:13:20.050 session requested",
                        "11-15 06:13:20.100 source=runtime-transport category=runtime route=self_test stage=self_test package=com.example.app message=ui-self-test",
                    ),
                ),
            ),
        )
        val timeline = entries.getValue("timeline.tsv")

        assertFalse(timeline.contains("session requested"))
        assertTrue(timeline.contains("ui-self-test"))
    }

    @Test
    fun moduleEffectsReportUnobservedSelectedRoutes() {
        val wechat = ExportBuilderFixtures.unzip(
            ExportBuilderFixtures.builder().buildZip(
                ExportBuilderFixtures.result(
                    listOf(
                        "11-15 06:13:20.100 source=runtime-transport category=performance route=runtime stage=aggregate package=com.tencent.mm message=process=com.tencent.mm,pid=21619;route=textview_current_px_fallback,calls=100,applied=10,skipped=0,kept=90,measuredCalls=10,p50Us=4,p95Us=20,p99Us=20,maxUs=30",
                    ),
                    "com.tencent.mm",
                    380,
                ),
            ),
        ).getValue("module-effects.tsv")
        assertTrue(wechat.contains("selected but no WeChat DPI route effect observed"))

        val viewport = ExportBuilderFixtures.unzip(
            ExportBuilderFixtures.builder().buildZip(
                ExportBuilderFixtures.result(
                    listOf("11-15 06:13:20.100 source=lsposed-log category=runtime route=app_process stage=route_callback_entered level=I package=com.example.app process=com.example.app message=DPIS package ready"),
                ),
            ),
        ).getValue("module-effects.tsv")
        assertTrue(viewport.contains("selected but no viewport route effect observed"))
    }

    @Test
    fun wechatDpiEvidenceSeparatesAppliedRouteFromBottomTabFailure() {
        val text = ExportBuilderFixtures.builder().buildDiagnosticText(
            ExportBuilderFixtures.result(
                listOf(
                    "11-15 06:13:20.100 source=runtime-transport category=runtime route=wechat_dpi stage=route_callback_entered routeName=package_ready package=com.tencent.mm message=source=package_ready",
                    "11-15 06:13:20.200 source=runtime-hotpath category=runtime route=wechat_dpi stage=mutation_applied routeName=displaymetrics package=com.tencent.mm message=targetDpi=600",
                    "11-15 06:13:20.300 source=runtime-hotpath category=runtime route=wechat_dpi stage=skipped routeName=bottom_tab_icon package=com.tencent.mm message=reason=init_method_not_found",
                ),
                "com.tencent.mm",
                600,
            ),
        )

        assertTrue(text.contains("[wechat-dpi-evidence]"))
        assertTrue(text.contains("routeEntry: observed"))
        assertTrue(text.contains("displayMetrics: mutation applied"))
        assertTrue(text.contains("bottomTabIcon: skipped (reason=init_method_not_found)"))
    }
}
