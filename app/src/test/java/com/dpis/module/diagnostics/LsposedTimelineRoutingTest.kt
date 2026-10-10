package com.dpis.module.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class LsposedTimelineRoutingTest {
    @Test
    fun parsesTargetEventsInsideDiagnosticWindow() {
        val events = parse(
            log(
                "target app matched: package=com.example.app",
                timestamp = "2023-11-15T06:13:19.900"
            ) +
                    "\n" + log(
                "target app matched: package=com.example.app",
                timestamp = "2023-11-15T06:13:20.100"
            ) +
                    "\n" + log(
                "target app matched: package=com.other.app",
                process = "com.other.app",
                timestamp = "2023-11-15T06:13:25.000"
            ),
        )

        assertEquals(2, events.size)
        assertTrue(events[0].contains("source=lsposed-log"))
        assertTrue(events[0].contains("stage=config_resolved"))
        assertTrue(events[0].contains("package=com.example.app"))
    }

    @Test
    fun disabledConfigMarksUnexpectedViewportHit() = assertEvent(
        "DPIS_VIEWPORT app-process state seeded: package=com.example.app",
        app(viewportEnabled = false),
        "stage=unexpected_route_hit",
    )

    @Test
    fun hookInstalledSummaryIsClassifiedAsConfigSummary() = assertEvent(
        "hooks installed (safe mode): package=com.example.app viewportEnabled=false " +
                "fontMode=FIELD_REWRITE resolvedViewportMode=off",
        app(viewportEnabled = false),
        "route=config",
        "stage=config_resolved",
    )

    @Test
    fun hotReloadFrameworkWarningIsClassifiedAsSkipped() {
        val event = parse(
            log(
                "Auto hot reload failed for io.github.kwensiu.dpis in com.example.app/1234: status=3, message=null",
                level = "W",
            ),
            app(),
        ).single()
        assertTrue(event.contains("route=hot_reload"))
        assertTrue(event.contains("stage=skipped"))
        assertTrue(event.contains("level=W"))
        assertTrue(event.contains("status=3"))
    }

    @Test
    fun hookReadyIsNotClassifiedAsMutationApplied() = assertEvent(
        "DPIS_FONT Flutter settings hook ready for com.example.app",
        app(),
        "route=font",
        "stage=hook_ready",
    )

    @Test
    fun overrideIsClassifiedAsMutationApplied() = assertEvent(
        "DPIS_FONT Flutter settings textScaleFactor override: package=com.example.app",
        app(),
        "route=font",
        "stage=mutation_applied",
    )

    @Test
    fun featureOffSkipIsClassifiedAsSkipped() = assertEvent(
        "Resources write hooks skipped: package=com.example.app",
        app(viewportEnabled = false),
        "stage=skipped",
    )

    @Test
    fun repeatedOverrideEmitsRepeatedWriteWarning() {
        val events = parse(
            log("DPIS_FONT Flutter settings textScaleFactor override: package=com.example.app") +
                    "\n" + log(
                "DPIS_FONT Flutter settings textScaleFactor override: package=com.example.app",
                timestamp = "2023-11-15T06:13:20.200"
            ),
        )
        assertTrue(events.any { it.contains("stage=repeated_write") })
    }

    @Test
    fun appHookPlanWithSuppressedNoneIsConfigResolvedNotSkipped() = assertEvent(
        "DPIS_FONT app hook plan: package=com.example.app fontMode=field_rewrite " +
                "suppressed=none debugDisableTextViewAbsoluteRewrite=false",
        app(),
        "route=config",
        "stage=config_resolved",
    )

    @Test
    fun packageLoadedEnterIsRouteCallback() = assertEvent(
        "module loaded onPackageLoaded enter: package=com.example.app",
        app(),
        "route=app_process",
        "stage=route_callback_entered",
    )

    @Test
    fun skipHookMessagesRemainSkipped() = assertEvent(
        "DPIS_FONT skip abstract WebSettings#setTextZoom hook: package=com.example.app",
        app(),
        "stage=skipped",
    )

    @Test
    fun diagnosticHotPathLogBecomesRuntimeHotpathSource() = assertEvent(
        "DPIS DPIS_DIAG_HOTPATH route=font stage=begin routeName=text_appearance " +
                "package=com.example.app detail=view=android.widget.TextView,percent=120",
        app(),
        "source=runtime-hotpath",
        "route=font",
        "stage=begin",
        "routeName=text_appearance",
    )

    @Test
    fun diagnosticHotPathProcessEntryProbeIsRecognized() = assertEvent(
        "DPIS DPIS_DIAG_HOTPATH route=font stage=probe routeName=process_entry " +
                "package=com.example.app detail=process-entry",
        app(),
        "source=runtime-hotpath",
        "stage=probe",
        "routeName=process_entry",
    )

    @Test
    fun diagnosticPerformanceAggregateBecomesTargetProcessEvidence() = assertEvent(
        "DPIS DPIS_DIAG_PERF process=com.example.app,pid=123;route=paint_fallback," +
                "calls=20,applied=3,skipped=17,measuredCalls=3,p50Us=4,p95Us=20,p99Us=20,maxUs=30",
        app(),
        "source=runtime-hotpath",
        "category=performance",
        "stage=aggregate",
        "process=com.example.app",
    )

    @Test
    fun diagnosticSessionDiscoveryBecomesTransportEvidence() = assertEvent(
        "DPIS DPIS_DIAG_SESSION process-entry: package=com.example.app, process=com.example.app, " +
                "source=remote-session, markerVisible=true, propertyVisible=true",
        app(),
        "source=runtime-hotpath",
        "category=transport",
        "route=app_process",
        "stage=session_discovered",
        "markerVisible=true",
    )

    @Test
    fun diagnosticHotPathKeepsExpandedFontRouteName() = assertEvent(
        "DPIS DPIS_DIAG_HOTPATH route=font stage=applied routeName=textview_sp_rewrite " +
                "package=com.example.app detail=view=android.widget.TextView,in=20.0,out=10.0",
        app(),
        "source=runtime-hotpath",
        "stage=applied",
        "routeName=textview_sp_rewrite",
    )

    @Test
    fun wechatDpiHookReadyIsClassifiedAsWechatRoute() = assertEvent(
        "modern WeChat DPI hook ready: j65.f#e, installed=1, locator=static-route",
        wechat(600),
        "route=wechat_dpi",
        "stage=hook_ready",
    )

    @Test
    fun wechatDpiRoutePlanIsClassifiedAsWechatConfigResolved() = assertEvent(
        "modern WeChat DPI route plan: versionCode=3120, locator=static-route, class=j65.f, " +
                "metricsTargets=d,e, bottomTab=true, retiredTargets=g,k,l, retiredActive=false",
        wechat(600),
        "route=wechat_dpi",
        "stage=config_resolved",
    )

    @Test
    fun wechatDpiCallbackIsUnexpectedWhenRouteNotConfigured() = assertEvent(
        "modern WeChat DPI callback hit: method=j65.f#e, firstCallbackMethod=e, configuredDpi=0",
        wechat(null),
        "route=wechat_dpi",
        "stage=unexpected_route_hit",
    )

    @Test
    fun wechatDpiAppliedIsExpectedWhenRouteConfigured() = assertEvent(
        "modern WeChat DPI applied: method=j65.f#e, appliedMethod=e, targetDpi=600, densityDpi 480 -> 600",
        wechat(600),
        "route=wechat_dpi",
        "stage=mutation_applied",
    )

    @Test
    fun viewportOverrideWithFontFieldsStaysOnViewportRoute() = assertEvent(
        "DPIS_VIEWPORT ResourcesRead(getConfiguration) override: package=com.example.app " +
                "densityDpi 533 -> 533, fontScale 1.0 -> 1.05",
        app(),
        "route=viewport",
        "stage=mutation_applied",
    )

    @Test
    fun systemServerMutationKeepsSystemServerOwnerRoute() = assertEvent(
        "system_server display-manager-info apply: package=com.example.app",
        app(),
        process = "system",
        expected = arrayOf("route=system_server", "stage=mutation_applied"),
    )

    @Test
    fun systemServerSkipIsCapturedWithoutUnexpectedRouteWarning() = assertEvent(
        "system_server display-manager-info skip: reason=env-null, package=com.example.app",
        app(),
        process = "system",
        expected = arrayOf("route=system_server", "stage=skipped"),
    )

    private fun assertEvent(
        message: String,
        input: LsposedTimelineParser.Input,
        vararg expected: String,
    ) = assertEvent(message, input, input.packageName, expected)

    private fun assertEvent(
        message: String,
        input: LsposedTimelineParser.Input,
        process: String = "com.example.app",
        expected: Array<out String>,
    ) {
        val events = parse(log(message, process = process.ifBlank { input.packageName }), input)
        assertEquals(1, events.size)
        expected.forEach {
            assertTrue(
                "missing $it in ${events.single()}",
                events.single().contains(it)
            )
        }
    }

    private fun parse(raw: String, input: LsposedTimelineParser.Input = app()): List<String> =
        LsposedTimelineParser.parse(raw, START, END, input).filterNotNull()

    private fun app(
        viewportEnabled: Boolean = true,
        wechatDpiEnabled: Boolean = false,
    ) = LsposedTimelineParser.Input(
        "com.example.app",
        true,
        viewportEnabled,
        true,
        false,
        wechatDpiEnabled
    )

    private fun wechat(dpi: Int?) =
        LsposedTimelineParser.Input("com.tencent.mm", true, false, false, false, dpi != null)

    private fun log(
        message: String,
        process: String = "com.example.app",
        timestamp: String = "2023-11-15T06:13:20.100",
        level: String = "I",
    ) = "[ $timestamp     1000:  1234:  5678 $level/LSPosedFramework ] " +
            "($process)[io.github.kwensiu.dpis,DPIS,id,0,1] $message"

    companion object {
        private val START = millis("2023-11-15 06:13:19.000")
        private val END = millis("2023-11-15 06:13:29.000")

        private fun millis(value: String): Long =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).parse(value)!!.time
    }
}
