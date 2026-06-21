package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

public final class FeedbackDiagnosticLsposedTimelineParserTest {
    private static final long WINDOW_START_MILLIS = millis("2023-11-15 06:13:19.000");
    private static final long WINDOW_END_MILLIS = millis("2023-11-15 06:13:29.000");

    @Test
    public void parsesTargetEventsInsideDiagnosticWindow() {
        String raw = ""
                + "[ 2023-11-15T06:13:19.900     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "target app matched: package=com.example.app\n"
                + "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "target app matched: package=com.example.app\n"
                + "[ 2023-11-15T06:13:25.000     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.other.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "target app matched: package=com.other.app";

        List<String> events = FeedbackDiagnosticLsposedTimelineParser.parse(
                raw,
                WINDOW_START_MILLIS,
                WINDOW_END_MILLIS,
                request(true, true, true)
        );

        assertEquals(2, events.size());
        assertTrue(events.get(0).contains("source=lsposed-log"));
        assertTrue(events.get(0).contains("stage=config_resolved"));
        assertTrue(events.get(0).contains("package=com.example.app"));
    }

    @Test
    public void disabledConfigMarksUnexpectedViewportHit() {
        String raw = "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "DPIS_VIEWPORT app-process state seeded: package=com.example.app";

        List<String> events = FeedbackDiagnosticLsposedTimelineParser.parse(
                raw,
                WINDOW_START_MILLIS,
                WINDOW_END_MILLIS,
                request(true, true, false)
        );

        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("stage=unexpected_route_hit"));
    }

    @Test
    public void hookInstalledSummaryIsClassifiedAsConfigSummary() {
        String raw = "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "hooks installed (safe mode): package=com.example.app "
                + "viewportEnabled=false fontMode=FIELD_REWRITE resolvedViewportMode=off";

        List<String> events = FeedbackDiagnosticLsposedTimelineParser.parse(
                raw,
                WINDOW_START_MILLIS,
                WINDOW_END_MILLIS,
                request(true, true, false)
        );

        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=config"));
        assertTrue(events.get(0).contains("stage=config_resolved"));
        assertFalse(events.get(0).contains("unexpected_route_hit"));
    }

    @Test
    public void hookReadyIsNotClassifiedAsMutationApplied() {
        String raw = "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "DPIS_FONT Flutter settings hook ready for com.example.app";

        List<String> events = FeedbackDiagnosticLsposedTimelineParser.parse(
                raw,
                WINDOW_START_MILLIS,
                WINDOW_END_MILLIS,
                request(true, true, true)
        );

        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=font"));
        assertTrue(events.get(0).contains("stage=hook_ready"));
        assertFalse(events.get(0).contains("mutation_applied"));
    }

    @Test
    public void overrideIsClassifiedAsMutationApplied() {
        String raw = "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "DPIS_FONT Flutter settings textScaleFactor override: package=com.example.app";

        List<String> events = FeedbackDiagnosticLsposedTimelineParser.parse(
                raw,
                WINDOW_START_MILLIS,
                WINDOW_END_MILLIS,
                request(true, true, true)
        );

        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=font"));
        assertTrue(events.get(0).contains("stage=mutation_applied"));
    }

    @Test
    public void featureOffSkipIsClassifiedAsSkipped() {
        String raw = "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "Resources write hooks skipped: package=com.example.app";

        List<String> events = FeedbackDiagnosticLsposedTimelineParser.parse(
                raw,
                WINDOW_START_MILLIS,
                WINDOW_END_MILLIS,
                request(true, true, false)
        );

        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("stage=skipped"));
        assertFalse(events.get(0).contains("unexpected_route_hit"));
    }

    @Test
    public void repeatedOverrideEmitsRepeatedWriteWarning() {
        String raw = ""
                + "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "DPIS_FONT Flutter settings textScaleFactor override: package=com.example.app\n"
                + "[ 2023-11-15T06:13:20.200     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "DPIS_FONT Flutter settings textScaleFactor override: package=com.example.app";

        List<String> events = FeedbackDiagnosticLsposedTimelineParser.parse(
                raw,
                WINDOW_START_MILLIS,
                WINDOW_END_MILLIS,
                request(true, true, true)
        );

        assertTrue(events.stream().anyMatch(event -> event.contains("stage=repeated_write")));
    }

    @Test
    public void appHookPlanWithSuppressedNoneIsConfigResolvedNotSkipped() {
        String raw = "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "DPIS_FONT app hook plan: package=com.example.app "
                + "fontMode=field_rewrite suppressed=none "
                + "debugDisableTextViewAbsoluteRewrite=false";

        List<String> events = FeedbackDiagnosticLsposedTimelineParser.parse(
                raw,
                WINDOW_START_MILLIS,
                WINDOW_END_MILLIS,
                request(true, true, true)
        );

        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=config"));
        assertTrue(events.get(0).contains("stage=config_resolved"));
        assertFalse(events.get(0).contains("stage=skipped"));
    }

    @Test
    public void packageLoadedEnterIsRouteCallback() {
        String raw = "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "module loaded onPackageLoaded enter: package=com.example.app";

        List<String> events = FeedbackDiagnosticLsposedTimelineParser.parse(
                raw,
                WINDOW_START_MILLIS,
                WINDOW_END_MILLIS,
                request(true, true, true)
        );

        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=app_process"));
        assertTrue(events.get(0).contains("stage=route_callback_entered"));
    }

    @Test
    public void skipHookMessagesRemainSkipped() {
        String raw = "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "DPIS_FONT skip abstract WebSettings#setTextZoom hook: package=com.example.app";

        List<String> events = FeedbackDiagnosticLsposedTimelineParser.parse(
                raw,
                WINDOW_START_MILLIS,
                WINDOW_END_MILLIS,
                request(true, true, true)
        );

        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("stage=skipped"));
    }

    @Test
    public void diagnosticHotPathLogBecomesRuntimeHotpathSource() {
        String raw = "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "DPIS DPIS_DIAG_HOTPATH route=font stage=begin routeName=text_appearance "
                + "package=com.example.app detail=view=android.widget.TextView,percent=120";

        List<String> events = FeedbackDiagnosticLsposedTimelineParser.parse(
                raw,
                WINDOW_START_MILLIS,
                WINDOW_END_MILLIS,
                request(true, true, true)
        );

        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("source=runtime-hotpath"));
        assertTrue(events.get(0).contains("route=font"));
        assertTrue(events.get(0).contains("stage=begin"));
        assertTrue(events.get(0).contains("routeName=text_appearance"));
    }

    @Test
    public void diagnosticHotPathProcessEntryProbeIsRecognized() {
        String raw = "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "DPIS DPIS_DIAG_HOTPATH route=font stage=probe "
                + "routeName=process_entry package=com.example.app detail=process-entry";

        List<String> events = FeedbackDiagnosticLsposedTimelineParser.parse(
                raw,
                WINDOW_START_MILLIS,
                WINDOW_END_MILLIS,
                request(true, true, true)
        );

        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("source=runtime-hotpath"));
        assertTrue(events.get(0).contains("stage=probe"));
        assertTrue(events.get(0).contains("routeName=process_entry"));
    }

    @Test
    public void diagnosticHotPathKeepsExpandedFontRouteName() {
        String raw = "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "DPIS DPIS_DIAG_HOTPATH route=font stage=applied "
                + "routeName=textview_sp_rewrite package=com.example.app "
                + "detail=view=android.widget.TextView,in=20.0,out=10.0";

        List<String> events = FeedbackDiagnosticLsposedTimelineParser.parse(
                raw,
                WINDOW_START_MILLIS,
                WINDOW_END_MILLIS,
                request(true, true, true)
        );

        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("source=runtime-hotpath"));
        assertTrue(events.get(0).contains("stage=applied"));
        assertTrue(events.get(0).contains("routeName=textview_sp_rewrite"));
    }

    @Test
    public void viewportOverrideWithFontFieldsStaysOnViewportRoute() {
        String raw = "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "DPIS_VIEWPORT ResourcesRead(getConfiguration) override: package=com.example.app "
                + "densityDpi 533 -> 533, fontScale 1.0 -> 1.05";

        List<String> events = FeedbackDiagnosticLsposedTimelineParser.parse(
                raw,
                WINDOW_START_MILLIS,
                WINDOW_END_MILLIS,
                request(true, true, true)
        );

        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=viewport"));
        assertTrue(events.get(0).contains("stage=mutation_applied"));
    }

    @Test
    public void sameTimestampHotPathEventsUseSemanticStageOrder() {
        String raw = ""
                + "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "DPIS DPIS_DIAG_HOTPATH route=font stage=end routeName=text_appearance "
                + "package=com.example.app detail=view=android.widget.TextView,durationMs=1\n"
                + "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "DPIS DPIS_DIAG_HOTPATH route=font stage=applied routeName=text_appearance "
                + "package=com.example.app detail=view=android.widget.TextView\n"
                + "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] "
                + "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                + "DPIS DPIS_DIAG_HOTPATH route=font stage=begin routeName=text_appearance "
                + "package=com.example.app detail=view=android.widget.TextView";

        List<String> events = FeedbackDiagnosticLsposedTimelineParser.parse(
                raw,
                WINDOW_START_MILLIS,
                WINDOW_END_MILLIS,
                request(true, true, true)
        );

        assertEquals(3, events.size());
        assertTrue(events.get(0).contains("stage=begin"));
        assertTrue(events.get(1).contains("stage=applied"));
        assertTrue(events.get(2).contains("stage=end"));
    }

    private static FeedbackDiagnosticCoordinator.Request request(
            boolean inScope,
            boolean dpisEnabled,
            boolean viewportEnabled
    ) {
        return new FeedbackDiagnosticCoordinator.Request(
                "com.example.app",
                "Example",
                "1.2.3",
                true,
                inScope,
                dpisEnabled,
                false,
                viewportEnabled ? ViewportTargetSpec.absoluteDp(411) : ViewportTargetSpec.off(),
                viewportEnabled ? ViewportApplyMode.AUTO : ViewportApplyMode.OFF,
                120,
                FontApplyMode.FIELD_REWRITE,
                null,
                null
        );
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
