package com.dpis.module.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class LsposedTimelineWindowTest {
    @Test
    fun sameTimestampHotPathEventsUseSemanticStageOrder() {
        val raw = listOf("end", "applied", "begin").map { stage ->
            log("DPIS DPIS_DIAG_HOTPATH route=font stage=$stage routeName=text_appearance package=com.example.app detail=view=TextView")
        }.joinToString("\n")

        val events = parse(raw)

        assertEquals(3, events.size)
        assertTrue(events[0].contains("stage=begin"))
        assertTrue(events[1].contains("stage=applied"))
        assertTrue(events[2].contains("stage=end"))
    }

    @Test
    fun wechatDpiHistoryWithinLookbackSurvivesDiagnosticSessionWindow() {
        val raw = log(
            "DPIS_WECHAT_DPI_HISTORY package=com.tencent.mm stage=reapplied " +
                    "source=ResourcesImpl.updateConfiguration,targetDpi=368,observedDpi=480,resultDpi=368",
            process = "com.tencent.mm",
            timestamp = "2023-11-14T09:00:00.000",
        )

        val events = parse(
            raw,
            LsposedTimelineParser.Input("com.tencent.mm", true, false, false, false, true)
        )

        assertEquals(1, events.size)
        assertTrue(events.single().contains("source=lsposed-history"))
        assertTrue(events.single().contains("stage=reapplied"))
        assertTrue(events.single().contains("targetDpi=368"))
    }

    @Test
    fun windowRawLogReportsFilteredAndUnparsedEntries() {
        val raw = listOf(
            log("inside package=com.example.app"),
            log("outside package=com.example.app", timestamp = "2023-11-15T06:20:00.000"),
            "Auto hot reload failed for io.github.kwensiu.dpis",
            log("unrelated", process = "system", module = "com.other.module"),
        ).joinToString("\n")

        val result = LsposedTimelineParser.windowRawLog(
            LogReadResult(0, "LSPosed", raw, ""),
            SessionWindow.around(START, END),
            app(),
        )

        assertEquals(3, result.totalParsed())
        assertEquals(2, result.droppedOutsideWindow())
        assertEquals(0, result.droppedUnparsed())
        assertEquals(1, result.droppedNonDpis())
        assertTrue(result.output().contains("inside"))
    }

    @Test
    fun windowRawLogHandlesEmptyResultAndMissingWindow() {
        assertEquals("", LsposedTimelineParser.windowRawLog(null, null, null).output())
        val result = LogReadResult(0, "LSPosed", "raw output", "")
        val windowless = LsposedTimelineParser.windowRawLog(result, null, null)
        assertEquals("raw output", windowless.output())
        assertEquals(0, windowless.totalParsed())
    }

    @Test
    fun resolvesTimestampVariantsAndSortsStageRanks() {
        assertEquals(-1L, LsposedTimelineParser.resolveTimestampMillis(null, START))
        assertEquals(-1L, LsposedTimelineParser.resolveTimestampMillis(" ", START))
        assertEquals(
            START,
            LsposedTimelineParser.resolveTimestampMillis("11-15 06:13:19.000", START)
        )
        assertEquals(START, LsposedTimelineParser.resolveTimestampMillis("11-15 06:13:19", START))
        assertEquals(-1L, LsposedTimelineParser.resolveTimestampMillis("not-a-time", START))

        val events = arrayListOf<String?>(
            "2023-11-15 06:13:20.000 stage=end",
            "2023-11-15 06:13:20.000 stage=probe",
            "short",
            null,
            "2023-11-15 06:13:20.000 stage=repeated_write",
        )
        LsposedTimelineParser.sortTimelineEvents(events)

        val probe = events.indexOf("2023-11-15 06:13:20.000 stage=probe")
        val end = events.indexOf("2023-11-15 06:13:20.000 stage=end")
        val repeated = events.indexOf("2023-11-15 06:13:20.000 stage=repeated_write")
        assertTrue(probe >= 0 && probe < end)
        assertTrue(end < repeated)
    }

    private fun parse(raw: String, input: LsposedTimelineParser.Input = app()): List<String> =
        LsposedTimelineParser.parse(raw, START, END, input).filterNotNull()

    private fun app() =
        LsposedTimelineParser.Input("com.example.app", true, true, true, false, false)

    private fun log(
        message: String,
        process: String = "com.example.app",
        timestamp: String = "2023-11-15T06:13:20.100",
        module: String = "io.github.kwensiu.dpis",
    ) = "[ $timestamp     1000:  1234:  5678 I/LSPosedFramework ] " +
            "($process)[$module,DPIS,id,0,1] $message"

    companion object {
        private val START = millis("2023-11-15 06:13:19.000")
        private val END = millis("2023-11-15 06:13:29.000")

        private fun millis(value: String): Long =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).parse(value)!!.time
    }
}
