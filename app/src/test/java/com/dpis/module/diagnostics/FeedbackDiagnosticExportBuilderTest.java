package com.dpis.module.diagnostics;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.*;

import com.dpis.module.viewport.DpiConfig;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;



import com.dpis.module.root.RootAccessProbe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Test;

public final class FeedbackDiagnosticExportBuilderTest {
    private static final long SESSION_START_MILLIS = millis("2023-11-15 06:13:20.000");
    private static final long SESSION_END_MILLIS = millis("2023-11-15 06:13:30.000");

    @Test
    public void zipContainsDiagnosticAndSeparateLogEntries() throws IOException {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                () -> List.of(
                        new DpisLogEntry(
                                millis("2023-11-15 06:13:20.100"),
                                "11-14 22:13:20",
                                "I",
                                "DPIS",
                                "io.github.kwensiu.dpis",
                                "io.github.kwensiu.dpis",
                                "DPIS",
                                "dpis app line",
                                false
                        ),
                        new DpisLogEntry(
                                millis("2023-11-15 06:15:00.000"),
                                "11-14 22:15:00",
                                "I",
                                "DPIS",
                                "io.github.kwensiu.dpis",
                                "io.github.kwensiu.dpis",
                                "DPIS",
                                "stale line",
                                false
                        )
                ),
                () -> new LogReadResult(
                        0,
                        "test-source",
                        "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                                + "DPIS DPIS_DIAG_HOTPATH route=font stage=begin routeName=text_appearance "
                                + "package=com.example.app detail=view=android.widget.TextView,percent=120\n"
                                + "[ 2023-11-15T06:13:20.150     1000:  1234:  5678 I/LSPosedFramework ] "
                                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                                + "target app matched: package=com.example.app\n"
                                + "[ 2023-11-15T06:16:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                                + "stale target app matched: package=com.example.app",
                        ""
                )
        );

        byte[] zipBytes = builder.buildZip(result());
        Map<String, String> entries = unzip(zipBytes);

        assertEquals(5, entries.size());
        assertTrue(entries.containsKey("diagnostic.txt"));
        assertTrue(entries.containsKey("timeline.tsv"));
        assertTrue(entries.containsKey("module-effects.tsv"));
        assertTrue(entries.containsKey("dpis-log.txt"));
        assertTrue(entries.containsKey("lsposed-log.txt"));
        String diagnostic = entries.get("diagnostic.txt");
        assertTrue(diagnostic.contains("[manifest]"));
        assertTrue(diagnostic.contains("[app-config]"));
        assertTrue(diagnostic.contains("[diagnostic-plan]"));
        assertTrue(diagnostic.contains("[runtime-summary]"));
        assertTrue(diagnostic.contains("[performance-summary]"));
        assertTrue(diagnostic.contains("[runtime-density]"));
        assertTrue(diagnostic.contains("[runtime-anomalies]"));
        assertTrue(diagnostic.contains("[runtime-timeline]"));
        assertTrue(diagnostic.contains("[runtime-self-test]"));
        assertTrue(diagnostic.contains("[perfetto]"));
        assertTrue(diagnostic.contains("[raw-log]"));
        assertTrue(diagnostic.contains("package: com.example.app"));
        assertTrue(diagnostic.contains("versionName: 1.2.3"));
        assertTrue(diagnostic.contains("runtime transport and LSPosed window parsing are experimental"));
        assertTrue(diagnostic.contains("11-14 22:13:20.000 session started"));
        assertTrue(diagnostic.contains("source=runtime-hotpath"));
        assertTrue(diagnostic.contains("routeName=text_appearance"));
        assertTrue(diagnostic.contains("runtimeEvents: "));
        assertTrue(diagnostic.contains("entries: 0"));
        assertTrue(diagnostic.contains("font=1"));
        assertTrue(diagnostic.contains("begin=1"));
        assertTrue(diagnostic.contains("peakSecond: "));
        assertTrue(diagnostic.contains(" events)"));
        assertTrue(diagnostic.contains("[runtime-anomalies]\nnone observed"));
        assertTrue(diagnostic.contains("lsposedHotpathProbe: found"));
        assertTrue(diagnostic.contains("source=lsposed-log"));
        assertTrue(diagnostic.contains("see dpis-log.txt"));
        assertTrue(diagnostic.contains("see lsposed-log.txt"));
        assertFalse(section(diagnostic, "[runtime-timeline]", "[raw-log]")
                .contains("see lsposed-log.txt for raw evidence"));
        assertFalse(diagnostic.contains("output:\n[ 2023-11-15T06:13:20.100"));
        assertTrue(entries.get("dpis-log.txt").contains("dpis app line"));
        assertTrue(entries.get("dpis-log.txt").contains("scope: diagnostic-window"));
        assertFalse(entries.get("dpis-log.txt").contains("stale line"));
        assertTrue(entries.get("lsposed-log.txt").contains("droppedOutsideWindow: 1"));
        assertTrue(entries.get("lsposed-log.txt").contains("DPIS DPIS_DIAG_HOTPATH route=font stage=begin"));
        assertTrue(entries.get("lsposed-log.txt").contains("target app matched"));
        assertFalse(entries.get("lsposed-log.txt").contains("stale target app matched"));
        assertTrue(entries.get("timeline.tsv").contains(
                "time\tsource\tcategory\tmodule\troute\tstage\tprocess\tpackage\tmessage"));
        assertTrue(entries.get("timeline.tsv").contains(
                "06:13:20.100\truntime-hotpath\truntime\tfont\ttext_appearance\tbegin"));
        assertTrue(entries.get("module-effects.tsv").startsWith(
                "source\tprocess\tpid\tmodule\troute\tcalls\tapplied\tskipped"));
    }

    @Test
    public void performanceSummaryPrefersTargetProcessLsposedAggregate() {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(0, "test-source", "", "")
        );

        String diagnostic = builder.buildDiagnosticText(result(List.of(
                "11-15 06:13:20.100 source=runtime-transport "
                        + "category=performance route=runtime stage=aggregate "
                        + "package=com.example.app message=process=com.example.app,pid=123;"
                        + "route=paint_fallback,calls=20,applied=3,skipped=17,"
                        + "measuredCalls=3,p50Us=4,p95Us=20,p99Us=20,maxUs=30"
        )));

        assertTrue(diagnostic.contains("source: target-process-lsposed-aggregate"));
        assertTrue(diagnostic.contains("processes: 1"));
        assertTrue(diagnostic.contains("route: paint_fallback,calls=20"));
        assertFalse(diagnostic.contains("source: ui-process-fallback"));
    }

    @Test
    public void performanceSummaryFallsBackToTargetMutationLogs() {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(0, "test-source", "", "")
        );

        String diagnostic = builder.buildDiagnosticText(result(List.of(
                "11-15 06:13:20.100 source=lsposed-log category=runtime route=font "
                        + "stage=mutation_applied level=I package=com.example.app "
                        + "process=com.example.app message=DPIS DPIS_FONT Paint.setTextSize "
                        + "fallback applied: package=com.example.app, hookId=paint_set_text_size"
        )));

        assertTrue(diagnostic.contains("source: target-process-log-fallback"));
        assertTrue(diagnostic.contains("aggregate transport missing"));
        assertTrue(diagnostic.contains("process: com.example.app,pid=unknown"));
        assertTrue(diagnostic.contains("route: paint_set_text_size,calls=1,applied=1"));
        assertFalse(diagnostic.contains("entries: 0"));
    }

    @Test
    public void timelineEntryOrdersRuntimeEvents() throws IOException {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(0, "test-source", "", "")
        );

        Map<String, String> entries = unzip(builder.buildZip(result(List.of(
                "11-15 06:13:20.300 source=runtime-hotpath category=runtime "
                        + "route=font stage=end routeName=paint_text_size "
                        + "package=com.example.app process=com.example.app message=durationMs=2",
                "11-15 06:13:20.100 source=runtime-hotpath category=runtime "
                        + "route=font stage=begin routeName=paint_text_size "
                        + "package=com.example.app process=com.example.app message=view=TextView"
        ))));

        String timeline = entries.get("timeline.tsv");

        assertTrue(timeline.indexOf("06:13:20.100") < timeline.indexOf("06:13:20.300"));
        assertTrue(timeline.contains(
                "06:13:20.100\truntime-hotpath\truntime\tfont\tpaint_text_size\tbegin"));
        assertTrue(timeline.contains("view=TextView"));
    }

    @Test
    public void timelineIncludesSlowMutationBreakdownEvidence() throws IOException {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(0, "test-source", "", "")
        );

        Map<String, String> entries = unzip(builder.buildZip(result(List.of(
                "11-15 06:13:20.100 source=runtime-hotpath category=font "
                        + "route=textview_sp_rewrite stage=slow_mutation_breakdown "
                        + "package=com.example.app process=com.example.app "
                        + "message=frameworkUs=5200,bookkeepingUs=40,totalUs=5240"
        ))));

        String timeline = entries.get("timeline.tsv");

        assertTrue(timeline.contains(
                "06:13:20.100\truntime-hotpath\tfont\tfont\ttextview_sp_rewrite\t"
                        + "slow_mutation_breakdown"));
        assertTrue(timeline.contains("frameworkUs=5200,bookkeepingUs=40,totalUs=5240"));
    }

    @Test
    public void timelineTsvExcludesUnstructuredCoordinatorNotes() throws IOException {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(0, "test-source", "", "")
        );

        Map<String, String> entries = unzip(builder.buildZip(result(List.of(
                "11-15 06:13:20.050 session requested",
                "11-15 06:13:20.100 source=runtime-transport category=runtime "
                        + "route=self_test stage=self_test package=com.example.app "
                        + "message=ui-self-test"
        ))));

        String timeline = entries.get("timeline.tsv");

        assertFalse(timeline.contains("\tunknown\tunknown\tunknown\tunknown\tunknown\tunknown\tunknown\t"));
        assertFalse(timeline.contains("session requested"));
        assertTrue(timeline.contains("ui-self-test"));
    }

    @Test
    public void moduleEffectsEntryUsesTargetProcessLsposedAggregate() throws IOException {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(0, "test-source", "", "")
        );

        Map<String, String> entries = unzip(builder.buildZip(result(List.of(
                "11-15 06:13:20.100 source=runtime-transport "
                        + "category=performance route=runtime stage=aggregate "
                        + "package=com.example.app message=process=com.example.app,pid=123;"
                        + "route=paint_fallback,calls=20,applied=3,skipped=17,"
                        + "measuredCalls=3,p50Us=4,p95Us=20,p99Us=20,maxUs=30"
        ))));

        String moduleEffects = entries.get("module-effects.tsv");

        assertTrue(moduleEffects.contains(
                "target-process-lsposed-aggregate\tcom.example.app\t123\tfont\tpaint_fallback"
                        + "\t20\t3\t17\t0\t3\t4\t20\t20\t30\t"));
        assertFalse(moduleEffects.contains("ui-process-fallback"));
    }

    @Test
    public void moduleEffectsEntryFallsBackToMutationLogs() throws IOException {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(0, "test-source", "", "")
        );

        Map<String, String> entries = unzip(builder.buildZip(result(List.of(
                "11-15 06:13:20.100 source=lsposed-log category=runtime route=font "
                        + "stage=mutation_applied level=I package=com.example.app "
                        + "process=com.example.app message=DPIS DPIS_FONT Paint.setTextSize "
                        + "fallback applied: package=com.example.app, hookId=paint_set_text_size"
        ))));

        String moduleEffects = entries.get("module-effects.tsv");

        assertTrue(moduleEffects.contains(
                "target-process-log-fallback\tcom.example.app\tunknown\tfont"
                        + "\tpaint_set_text_size\t1\t1\t0\t0\t0\t0\t0\t0\t0"
                        + "\taggregate transport missing; latency percentiles unavailable"));
    }

    @Test
    public void moduleEffectsEntryReportsSelectedViewportWhenUnobserved() throws IOException {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(0, "test-source", "", "")
        );

        Map<String, String> entries = unzip(builder.buildZip(result(List.of(
                "11-15 06:13:20.100 source=lsposed-log category=runtime route=app_process "
                        + "stage=route_callback_entered level=I package=com.example.app "
                        + "process=com.example.app message=DPIS package ready: "
                        + "process=com.example.app, package=com.example.app"
        ))));

        String moduleEffects = entries.get("module-effects.tsv");

        assertTrue(moduleEffects.contains(
                "diagnostic-plan\tunknown\tunknown\tviewport\tviewport_auto"
                        + "\t0\t0\t0\t0\t0\t0\t0\t0\t0"
                        + "\tselected but no viewport route effect observed"));
    }

    @Test
    public void timelineEntryClassifiesAppProcessAndSelfTestModules() throws IOException {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(0, "test-source", "", "")
        );

        Map<String, String> entries = unzip(builder.buildZip(result(List.of(
                "11-15 06:13:20.100 source=runtime-transport category=runtime "
                        + "route=self_test stage=self_test package=com.example.app "
                        + "message=ui-self-test",
                "11-15 06:13:20.200 source=lsposed-log category=runtime "
                        + "route=app_process stage=route_callback_entered "
                        + "package=com.example.app process=com.example.app "
                        + "message=DPIS package ready"
        ))));

        String timeline = entries.get("timeline.tsv");

        assertTrue(timeline.contains(
                "06:13:20.100\truntime-transport\truntime\tdiagnostic\tself_test"));
        assertTrue(timeline.contains(
                "06:13:20.200\tlsposed-log\truntime\tapp_process\tapp_process"));
    }

    @Test
    public void diagnosticExportsAvailablePerfettoTrace() throws IOException {
        FeedbackDiagnosticCoordinator.Result base = result();
        FeedbackDiagnosticCoordinator.Result withTrace =
                new FeedbackDiagnosticCoordinator.Result(
                        base.request,
                        base.startedAtMillis,
                        base.finishedAtMillis,
                        base.durationMs,
                        base.targetLaunchStarted,
                        base.rootAccess,
                        base.systemHooksEnabled,
                        base.summary,
                        base.timelineEvents,
                        base.performanceSnapshot,
                        true,
                        3L,
                        false,
                        "trace exported with diagnostic package",
                        "abc".getBytes(StandardCharsets.UTF_8)
                );

        Map<String, String> entries = unzip(
                new FeedbackDiagnosticExportBuilder(List::of, () ->
                        new LogReadResult(0, "test-source", "", ""))
                        .buildZip(withTrace)
        );

        assertEquals("abc", entries.get(FeedbackDiagnosticExportBuilder.PERFETTO_TRACE_ENTRY_NAME));
        String diagnostic = unzip(
                new FeedbackDiagnosticExportBuilder(List::of, () ->
                        new LogReadResult(0, "test-source", "", ""))
                        .buildZip(withTrace)
        ).get("diagnostic.txt");
        assertTrue(diagnostic.contains("exported: true"));
        assertTrue(diagnostic.contains("sizeBytes: 3"));
    }

    @Test
    public void dpisLogKeepsWindowEntriesWhenPresent() throws IOException {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                () -> List.of(
                        new DpisLogEntry(
                                millis("2023-11-15 06:13:25.000"),
                                "11-14 22:13:25",
                                "I",
                                "DPIS",
                                "io.github.kwensiu.dpis",
                                "io.github.kwensiu.dpis",
                                "DPIS",
                                "window line",
                                false
                        ),
                        new DpisLogEntry(
                                millis("2023-11-15 06:15:00.000"),
                                "11-14 22:15:00",
                                "I",
                                "DPIS",
                                "io.github.kwensiu.dpis",
                                "io.github.kwensiu.dpis",
                                "DPIS",
                                "stale line",
                                false
                        )
                ),
                () -> new LogReadResult(0, "test-source", "", "")
        );

        Map<String, String> entries = unzip(builder.buildZip(result()));
        String dpisLog = entries.get("dpis-log.txt");

        assertTrue(dpisLog.contains("scope: diagnostic-window"));
        assertTrue(dpisLog.contains("window line"));
        assertFalse(dpisLog.contains("stale line"));
    }

    @Test
    public void dpisLogFallsBackToRecentEntriesWhenWindowIsEmpty() throws IOException {
        List<DpisLogEntry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < 105; i++) {
            entries.add(new DpisLogEntry(
                    millis("2023-11-15 06:14:00.000") + i,
                    "11-14 22:13:" + String.format(java.util.Locale.US, "%02d", i % 60),
                    "I",
                    "DPIS",
                    "io.github.kwensiu.dpis",
                    "io.github.kwensiu.dpis",
                    "DPIS",
                    "entry-" + String.format(java.util.Locale.US, "%03d", i),
                    false
            ));
        }
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                () -> entries,
                () -> new LogReadResult(0, "test-source", "", "")
        );

        Map<String, String> zipEntries = unzip(builder.buildZip(result()));
        String dpisLog = zipEntries.get("dpis-log.txt");

        assertTrue(dpisLog.contains("scope: recent-fallback"));
        assertTrue(dpisLog.contains("reason: no DPIS app log entries matched the diagnostic window"));
        assertTrue(dpisLog.contains("limit: 100"));
        assertTrue(dpisLog.contains("maxDistanceMs: 300000"));
        assertTrue(dpisLog.contains("entries: 100"));
        assertFalse(dpisLog.contains("entry-000"));
        assertFalse(dpisLog.contains("entry-004"));
        assertTrue(dpisLog.contains("entry-005"));
        assertTrue(dpisLog.contains("entry-104"));
    }

    @Test
    public void dpisLogFallbackIgnoresEntriesFarOutsideSession() throws IOException {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                () -> List.of(
                        new DpisLogEntry(
                                millis("2023-11-15 05:00:00.000"),
                                "11-14 21:00:00",
                                "I",
                                "DPIS",
                                "io.github.kwensiu.dpis",
                                "io.github.kwensiu.dpis",
                                "DPIS",
                                "far-before",
                                false
                        ),
                        new DpisLogEntry(
                                millis("2023-11-15 08:00:00.000"),
                                "11-15 00:00:00",
                                "I",
                                "DPIS",
                                "io.github.kwensiu.dpis",
                                "io.github.kwensiu.dpis",
                                "DPIS",
                                "far-after",
                                false
                        )
                ),
                () -> new LogReadResult(0, "test-source", "", "")
        );

        Map<String, String> zipEntries = unzip(builder.buildZip(result()));
        String dpisLog = zipEntries.get("dpis-log.txt");

        assertTrue(dpisLog.contains("scope: recent-fallback"));
        assertTrue(dpisLog.contains("entries: 0"));
        assertTrue(dpisLog.contains("No DPIS app log entries available."));
        assertFalse(dpisLog.contains("far-before"));
        assertFalse(dpisLog.contains("far-after"));
    }

    @Test
    public void emptyTimelineIncludesRawEvidenceNote() {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(0, "test-source", "", "")
        );

        String text = builder.buildDiagnosticText(result(List.of()));

        assertTrue(text.contains("[runtime-summary]\ntimelineEvents: 0\nruntimeEvents: 0"));
        assertTrue(text.contains("[runtime-density]\nno runtime density available"));
        assertTrue(text.contains("[runtime-anomalies]\nnone observed"));
        assertTrue(section(text, "[runtime-timeline]", "[raw-log]")
                .contains("no runtime events captured; see lsposed-log.txt for raw evidence"));
    }

    @Test
    public void wechatDpiConfigAddsAppSpecificDiagnosticPlan() {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(0, "test-source", "", "")
        );

        String text = builder.buildDiagnosticText(result(List.of(), 600));

        assertTrue(section(text, "[app-config]", "[diagnostic-plan]")
                .contains("appSpecific: wechatDpi=600"));
        assertTrue(section(text, "[diagnostic-plan]", "[runtime-summary]")
                .contains("wechatDpiRoute: selected (targetDpi=600)"));
    }

    @Test
    public void runtimeAnalysisKeepsFullTimelineAndFlagsWarnings() {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(0, "test-source", "", "")
        );
        String warning = "11-14 22:13:21.100 source=runtime-hotpath "
                + "category=warning route=font stage=repeated_write level=W "
                + "package=com.example.app process=com.example.app "
                + "message=same runtime hot path repeated within 300ms";
        String longEvent = "11-14 22:13:21.200 source=runtime-hotpath "
                + "category=runtime route=font stage=end routeName=paint_text_size_fallback "
                + "level=I package=com.example.app process=com.example.app "
                + "message=durationMs=17";

        String text = builder.buildDiagnosticText(result(List.of(warning, longEvent)));

        assertTrue(text.indexOf("[runtime-summary]") < text.indexOf("[runtime-timeline]"));
        assertTrue(text.contains("stageCounts: repeated_write=1"));
        assertTrue(text.contains("sample: " + warning));
        assertTrue(text.contains("maxDurationMs: 17"));
        assertTrue(section(text, "[runtime-timeline]", "[runtime-self-test]")
                .contains(warning));
        assertTrue(section(text, "[runtime-timeline]", "[runtime-self-test]")
                .contains(longEvent));
    }

    @Test
    public void runtimeSelfTestReportsFoundHotpathProbe() {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(
                        0,
                        "test-source",
                        "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                                + "DPIS DPIS_DIAG_HOTPATH route=font stage=probe "
                                + "routeName=process_entry package=com.example.app "
                                + "detail=process-entry",
                        ""
                )
        );

        String text = builder.buildDiagnosticText(result(List.of()));

        assertTrue(text.contains("[runtime-self-test]"));
        assertTrue(text.contains("lsposedHotpathProbe: found"));
    }

    @Test
    public void runtimeSelfTestReportsFoundForWechatHotpath() {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(
                        0,
                        "test-source",
                        "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                                + "(com.tencent.mm)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                                + "DPIS DPIS_DIAG_HOTPATH route=wechat_dpi stage=mutation_applied "
                                + "routeName=displaymetrics package=com.tencent.mm "
                                + "detail=targetDpi=390",
                        ""
                )
        );

        String text = builder.buildDiagnosticText(wechatResult(List.of(), 390));

        assertTrue(text.contains("[runtime-self-test]"));
        assertTrue(text.contains("lsposedHotpathProbe: found"));
    }

    @Test
    public void fileNameUsesZipExtension() {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(0, "test-source", "", "")
        );

        String fileName = builder.buildFileName(result());

        assertTrue(fileName.startsWith("dpis-diagnostic-com.example.app-"));
        assertTrue(fileName.matches(
                "dpis-diagnostic-com\\.example\\.app-\\d{8}-\\d{6}-\\d{8}-\\d{6}\\.zip"));
        assertTrue(fileName.endsWith(".zip"));
    }

    @Test
    public void emptyLogEntriesUsePlaceholders() throws IOException {
        FeedbackDiagnosticExportBuilder builder = new FeedbackDiagnosticExportBuilder(
                List::of,
                () -> new LogReadResult(0, "test-source", "", "")
        );

        Map<String, String> entries = unzip(builder.buildZip(result()));

        assertTrue(entries.get("dpis-log.txt")
                .contains("No DPIS app log entries available."));
        assertTrue(entries.get("lsposed-log.txt")
                .contains("LSPosed filtered log unavailable or empty in diagnostic window."));
    }

    private static FeedbackDiagnosticCoordinator.Result result() {
        return result(List.of("11-14 22:13:20.000 session started"), null);
    }

    private static FeedbackDiagnosticCoordinator.Result result(List<String> timelineEvents) {
        return result(timelineEvents, null);
    }

    private static FeedbackDiagnosticCoordinator.Result result(
            List<String> timelineEvents,
            Integer wechatDpi
    ) {
        FeedbackDiagnosticCoordinator.Request request = new FeedbackDiagnosticCoordinator.Request(
                "com.example.app",
                "Example",
                "1.2.3",
                true,
                true,
                true,
                false,
                ViewportTargetSpec.absoluteDp(411),
                ViewportApplyMode.AUTO,
                120,
                FontApplyMode.FIELD_REWRITE,
                "font-id",
                "system_server_font",
                wechatDpi
        );
        return new FeedbackDiagnosticCoordinator.Result(
                request,
                SESSION_START_MILLIS,
                SESSION_END_MILLIS,
                10_000L,
                true,
                RootAccessProbe.Result.available("Magisk"),
                true,
                "summary",
                timelineEvents
        );
    }

    private static FeedbackDiagnosticCoordinator.Result wechatResult(
            List<String> timelineEvents,
            Integer wechatDpi
    ) {
        FeedbackDiagnosticCoordinator.Request request = new FeedbackDiagnosticCoordinator.Request(
                "com.tencent.mm",
                "微信",
                "8.0.74",
                true,
                true,
                true,
                false,
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                null,
                null,
                wechatDpi
        );
        return new FeedbackDiagnosticCoordinator.Result(
                request,
                SESSION_START_MILLIS,
                SESSION_END_MILLIS,
                10_000L,
                true,
                RootAccessProbe.Result.available("Magisk"),
                true,
                "summary",
                timelineEvents
        );
    }

    private static String section(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        int end = text.indexOf(endMarker);
        if (start < 0 || end < start) {
            return "";
        }
        return text.substring(start, end);
    }

    private static Map<String, String> unzip(byte[] zipBytes) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                assertNotNull(entry);
                entries.put(
                        entry.getName(),
                        new String(zip.readAllBytes(), StandardCharsets.UTF_8)
                );
            }
        }
        return entries;
    }

    private static long millis(String value) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                    .parse(value)
                    .getTime();
        } catch (ParseException exception) {
            throw new AssertionError(exception);
        }
    }
}
