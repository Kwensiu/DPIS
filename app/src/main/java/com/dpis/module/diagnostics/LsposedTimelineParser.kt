package com.dpis.module.diagnostics

import com.dpis.module.diagnostics.SessionWindow.Companion.around
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale
import kotlin.math.max

internal object LsposedTimelineParser {
    private const val REPEAT_WARNING_WINDOW_MS = 300L
    private val WECHAT_DPI_HISTORY_LOOKBACK_MS = 48L * 60L * 60L * 1000L
    private const val WECHAT_DPI_HISTORY_PREFIX = "DPIS_WECHAT_DPI_HISTORY "

    @JvmStatic
    fun parse(
        raw: String?,
        startedAtMillis: Long,
        finishedAtMillis: Long,
        input: Input?
    ): MutableList<String?> {
        return parse(
            raw,
            around(startedAtMillis, finishedAtMillis),
            input
        )
    }

    @JvmStatic
    fun parse(
        raw: String?,
        window: SessionWindow?,
        input: Input?
    ): MutableList<String?> {
        if (raw == null || raw.isBlank() || window == null || window.endMillis() <= 0L) {
            return mutableListOf<String?>()
        }
        val events: MutableList<String?> = ArrayList<String?>()
        val lastMutationByKey: MutableMap<String?, Long?> = HashMap<String?, Long?>()
        val context = classifierContext(input)
        for (entry in DpisLogParser.parseLsposedDpis(raw)) {
            entry ?: continue
            val timestampMillis = resolveTimestampMillis(entry.timestamp, window.startMillis())
            if (!window.contains(timestampMillis) && !isWechatDpiHistoryInLookback(
                    timestampMillis, entry, window, input
                )
            ) {
                continue
            }
            if (!matchesTarget(entry, input)) {
                continue
            }
            val historyEvent = formatWechatDpiHistoryEvent(timestampMillis, entry, input)
            if (historyEvent != null) {
                events.add(historyEvent)
                continue
            }
            val hotPathEvent = formatHotPathEvent(timestampMillis, entry, input)
            if (hotPathEvent != null) {
                events.add(hotPathEvent)
                continue
            }
            val performanceEvent = formatPerformanceEvent(timestampMillis, entry, input)
            if (performanceEvent != null) {
                events.add(performanceEvent)
                continue
            }
            val sessionEvent = formatSessionEvent(timestampMillis, entry, input)
            if (sessionEvent != null) {
                events.add(sessionEvent)
                continue
            }
            val event =
                classify(entry, context)
            if (event != null) {
                events.add(formatEvent(timestampMillis, entry, input, event))
                val repeated = repeatedWriteEvent(
                    timestampMillis,
                    entry,
                    input,
                    event,
                    lastMutationByKey
                )
                if (repeated != null) {
                    events.add(repeated)
                }
            }
        }
        sortTimelineEvents(events)
        return events
    }

    @JvmStatic
    fun sortTimelineEvents(events: MutableList<String?>) {
        Collections.sort<String?>(events, TIMELINE_EVENT_COMPARATOR)
    }

    private val TIMELINE_EVENT_COMPARATOR: Comparator<String?> = Comparator { left, right ->
        compareValuesBy(left, right, ::timePrefix, ::stageRank, { it ?: "" })
    }

    private fun timePrefix(event: String?): String {
        val normalized = if (event != null) event else ""
        return if (normalized.length >= 18) normalized.substring(0, 18) else normalized
    }

    private fun stageRank(event: String?): Int {
        val stage = fieldValue(if (event != null) event else "", "stage", "")
        return when (stage) {
            "probe" -> 0
            "session_discovered", "hook_ready", "config_resolved", "route_callback_entered" -> 1
            "begin" -> 2
            "mutation_candidate" -> 3
            "applied", "mutation_applied" -> 4
            "skipped" -> 5
            "end" -> 6
            "repeated_write" -> 7
            "unexpected_route_hit" -> 8
            else -> 9
        }
    }

    private fun formatHotPathEvent(
        timestampMillis: Long,
        entry: DpisLogEntry?,
        input: Input?
    ): String? {
        val message = if (entry != null) entry.message else ""
        val hotPathMessage = hotPathMessage(message)
        if (hotPathMessage == null) {
            return null
        }
        val route = fieldValue(hotPathMessage, "route", "font")
        val stage = fieldValue(hotPathMessage, "stage", "event")
        val routeName = fieldValue(hotPathMessage, "routeName", "unknown")
        val packageName = fieldValue(
            hotPathMessage,
            "package",
            if (input != null) input.packageName else "unknown"
        )
        val detail = detailValue(hotPathMessage)
        return (formatTime(timestampMillis)
                + " source=runtime-hotpath"
                + " category=runtime"
                + " route=" + route
                + " stage=" + stage
                + " routeName=" + routeName
                + " level=" + valueOrDefault(entry!!.level, "I")
                + " package=" + valueOrDefault(packageName, "unknown")
                + " process=" + valueOrDefault(entry.process, "unknown")
                + " message=" + sanitize(detail))
    }

    private fun formatPerformanceEvent(
        timestampMillis: Long,
        entry: DpisLogEntry?,
        input: Input?
    ): String? {
        if (entry == null) {
            return null
        }
        val message = entry.message
        val performanceMessage = performanceMessage(message)
        if (performanceMessage == null) {
            return null
        }
        return (formatTime(timestampMillis)
                + " source=runtime-hotpath"
                + " category=performance"
                + " route=runtime"
                + " stage=aggregate"
                + " level=" + valueOrDefault(entry.level, "I")
                + " package=" + valueOrDefault(
            if (input != null) input.packageName else "",
            "unknown"
        )
                + " process=" + valueOrDefault(entry.process, "unknown")
                + " message=" + sanitize(performanceMessage))
    }

    private fun formatSessionEvent(
        timestampMillis: Long,
        entry: DpisLogEntry?,
        input: Input?
    ): String? {
        if (entry == null) {
            return null
        }
        val message = entry.message
        val sessionMessage = sessionMessage(message)
        if (sessionMessage == null) {
            return null
        }
        val packageName = fieldValue(
            sessionMessage,
            "package",
            if (input != null) input.packageName else "unknown"
        )
        val processName = fieldValue(sessionMessage, "process", entry.process)
        val detail = detailValue(sessionMessage)
        return (formatTime(timestampMillis)
                + " source=runtime-hotpath"
                + " category=transport"
                + " route=app_process"
                + " stage=session_discovered"
                + " level=" + valueOrDefault(entry.level, "I")
                + " package=" + valueOrDefault(packageName, "unknown")
                + " process=" + valueOrDefault(processName, "unknown")
                + " message=" + sanitize(detail))
    }

    @JvmStatic
    fun windowRawLog(
        result: LogReadResult?,
        window: SessionWindow?,
        input: Input?
    ): WindowedRawLog {
        if (result == null || result.output.isBlank()) {
            return WindowedRawLog("", 0, 0, 0, 0)
        }
        if (window == null) {
            return WindowedRawLog(result.output, 0, 0, 0, 0)
        }
        val retained: MutableList<String?> = ArrayList<String?>()
        var total = 0
        var droppedOutsideWindow = 0
        var droppedUnparsed = 0
        val entries = DpisLogParser.parseLsposedDpis(result.output)
        // Non-DPIS lines (other modules/apps, framework noise) are expected to
        // be filtered out and are counted separately from genuine parse misses.
        val droppedNonDpis = max(0, nonBlankLineCount(result.output) - entries.size)
        for (entry in entries) {
            entry ?: continue
            total++
            val timestampMillis = resolveTimestampMillis(entry.timestamp, window.startMillis())
            if (timestampMillis <= 0L) {
                droppedUnparsed++
                continue
            }
            if (!window.contains(timestampMillis)
                && !isWechatDpiHistoryInLookback(timestampMillis, entry, window, input)
            ) {
                droppedOutsideWindow++
                continue
            }
            if (!matchesTarget(entry, input)) {
                continue
            }
            retained.add(formatRawEntry(entry))
        }
        return WindowedRawLog(
            retained.joinToString("\n"),
            total,
            droppedOutsideWindow,
            droppedUnparsed,
            droppedNonDpis
        )
    }

    private fun matchesTarget(
        entry: DpisLogEntry?,
        input: Input?
    ): Boolean {
        val packageName = if (input != null) input.packageName else ""
        if (packageName == null || packageName.isBlank()) {
            return true
        }
        val message = if (entry != null) entry.message else ""
        val process = if (entry != null) entry.process else ""
        return message.contains(packageName) || process == packageName
    }

    private fun isWechatDpiHistoryInLookback(
        timestampMillis: Long,
        entry: DpisLogEntry,
        window: SessionWindow?,
        input: Input?
    ): Boolean {
        if (timestampMillis <= 0L || window == null || input == null || !input.wechatDpiEnabled) {
            return false
        }
        val message = entry.message
        return message.contains(WECHAT_DPI_HISTORY_PREFIX)
                && matchesTarget(entry, input)
                && timestampMillis >= window.endMillis() - WECHAT_DPI_HISTORY_LOOKBACK_MS && timestampMillis <= window.endMillis()
    }

    private fun formatWechatDpiHistoryEvent(
        timestampMillis: Long,
        entry: DpisLogEntry?,
        input: Input?
    ): String? {
        if (entry == null || input == null) {
            return null
        }
        val message = entry.message
        val prefix = message.indexOf(WECHAT_DPI_HISTORY_PREFIX)
        if (prefix < 0) {
            return null
        }
        val history =
            message.substring(prefix + WECHAT_DPI_HISTORY_PREFIX.length).trim { it <= ' ' }
        val stage = fieldValue(history, "stage", "event")
        val packageName = fieldValue(history, "package", input.packageName)
        return (formatTime(timestampMillis)
                + " source=lsposed-history"
                + " category=runtime"
                + " route=wechat_dpi"
                + " stage=" + stage
                + " routeName=resource_recovery"
                + " level=" + valueOrDefault(entry.level, "I")
                + " package=" + valueOrDefault(packageName, "unknown")
                + " process=" + valueOrDefault(entry.process, "unknown")
                + " message=" + sanitize(history))
    }

    private fun formatEvent(
        timestampMillis: Long,
        entry: DpisLogEntry,
        input: Input?,
        event: TimelineClassifier.Event
    ): String {
        return (formatTime(timestampMillis)
                + " source=lsposed-log"
                + " category=" + event.category()
                + " route=" + event.route()
                + " stage=" + event.stage()
                + " level=" + event.level()
                + " package=" + valueOrDefault(
            if (input != null) input.packageName else "",
            "unknown"
        )
                + " process=" + valueOrDefault(entry.process, "unknown")
                + " message=" + sanitize(event.message()))
    }

    private fun classify(
        entry: DpisLogEntry?,
        context: TimelineClassifier.Context?
    ): TimelineClassifier.Event? {
        return TimelineClassifier.classify(
            if (entry != null) entry.level else "",
            if (entry != null) entry.message else "",
            context
        )
    }

    private fun repeatedWriteEvent(
        timestampMillis: Long,
        entry: DpisLogEntry,
        input: Input?,
        event: TimelineClassifier.Event,
        lastMutationByKey: MutableMap<String?, Long?>
    ): String? {
        if ("mutation_applied" != event.stage() && "unexpected_route_hit" != event.stage()) {
            return null
        }
        val key = event.route() + "|" + event.stage() + "|" + event.message()
        val previous = lastMutationByKey.put(key, timestampMillis)
        if (previous == null || timestampMillis - previous > REPEAT_WARNING_WINDOW_MS) {
            return null
        }
        return (formatTime(timestampMillis)
                + " source=lsposed-log"
                + " category=warning"
                + " route=" + event.route()
                + " stage=repeated_write"
                + " level=W"
                + " package=" + valueOrDefault(
            if (input != null) input.packageName else "",
            "unknown"
        )
                + " process=" + valueOrDefault(entry.process, "unknown")
                + " message=same route event repeated within "
                + REPEAT_WARNING_WINDOW_MS
                + "ms: "
                + sanitize(event.message()))
    }

    private fun classifierContext(
        input: Input?
    ): TimelineClassifier.Context {
        return TimelineClassifier.Context(
            input != null && input.appEnabled,
            input != null && input.viewportEnabled,
            input != null && input.fontScaleEnabled,
            input != null && input.typefaceEnabled,
            input != null && input.wechatDpiEnabled
        )
    }

    private fun formatRawEntry(entry: DpisLogEntry): String {
        val builder = StringBuilder()
        builder.append('[')
            .append(entry.timestamp)
            .append("] ")
            .append(valueOrDefault(entry.level, "I"))
            .append('/')
            .append(valueOrDefault(entry.tag, "DPIS"))
        if (!entry.process.isBlank()) {
            builder.append(" (").append(entry.process).append(')')
        }
        if (!entry.modulePackage.isBlank()) {
            builder.append(" [").append(entry.modulePackage).append(']')
        }
        if (!entry.message.isBlank()) {
            builder.append(' ').append(entry.message)
        }
        return builder.toString()
    }

    @JvmStatic
    fun resolveTimestampMillis(timestamp: String?, anchorMillis: Long): Long {
        val normalized = if (timestamp != null) timestamp.trim { it <= ' ' } else ""
        if (normalized.isEmpty()) {
            return -1L
        }
        val anchor = Calendar.getInstance(Locale.US)
        anchor.timeInMillis = anchorMillis
        val year = anchor.get(Calendar.YEAR)
        var millis = parse(year.toString() + "-" + normalized, "yyyy-MM-dd HH:mm:ss.SSS")
        if (millis != null) {
            return millis
        }
        millis = parse(year.toString() + "-" + normalized, "yyyy-MM-dd HH:mm:ss")
        return if (millis != null) millis else -1L
    }

    private fun parse(value: String, pattern: String?): Long? {
        try {
            val format = SimpleDateFormat(pattern, Locale.US)
            format.isLenient = false
            val date = format.parse(value)
            return if (date != null) date.time else null
        } catch (exception: ParseException) {
            return null
        }
    }

    private fun formatTime(millis: Long): String {
        return SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
            .format(Date(millis))
    }

    private fun valueOrDefault(value: String?, fallback: String?): String? {
        val normalized = if (value != null) value.trim { it <= ' ' } else ""
        return if (normalized.isEmpty()) fallback else normalized
    }

    private fun sanitize(value: String?): String {
        return if (value == null) "" else value.replace('\n', ' ').replace('\r', ' ')
            .trim { it <= ' ' }
    }

    private fun nonBlankLineCount(raw: String): Int {
        var count = 0
        for (line in raw.split("\\R".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (!line.isBlank()) {
                count++
            }
        }
        return count
    }

    private fun fieldValue(
        message: String,
        fieldName: String?,
        fallback: String?
    ): String? {
        val prefix = fieldName + "="
        var start = message.indexOf(prefix)
        if (start < 0) {
            return fallback
        }
        start += prefix.length
        var end = message.indexOf(' ', start)
        val comma = message.indexOf(',', start)
        if (end < 0 || (comma >= 0 && comma < end)) {
            end = comma
        }
        val value = if (end >= 0) message.substring(start, end) else message.substring(start)
        return if (value.isBlank()) fallback else value
    }

    private fun detailValue(message: String): String {
        val marker = " detail="
        val start = message.indexOf(marker)
        if (start < 0) {
            return message
        }
        return message.substring(start + marker.length)
    }

    private fun hotPathMessage(message: String?): String? {
        if (message == null) {
            return null
        }
        var normalized = message.trim { it <= ' ' }
        if (normalized.startsWith("DPIS ")) {
            normalized = normalized.substring("DPIS ".length).trim { it <= ' ' }
        }
        return if (normalized.startsWith("DPIS_DIAG_HOTPATH ")) normalized else null
    }

    private fun performanceMessage(message: String?): String? {
        if (message == null) {
            return null
        }
        var normalized = message.trim { it <= ' ' }
        if (normalized.startsWith("DPIS ")) {
            normalized = normalized.substring("DPIS ".length).trim { it <= ' ' }
        }
        return if (normalized.startsWith("DPIS_DIAG_PERF "))
            normalized.substring("DPIS_DIAG_PERF ".length).trim { it <= ' ' }
        else
            null
    }

    private fun sessionMessage(message: String?): String? {
        if (message == null) {
            return null
        }
        var normalized = message.trim { it <= ' ' }
        if (normalized.startsWith("DPIS ")) {
            normalized = normalized.substring("DPIS ".length).trim { it <= ' ' }
        }
        return if (normalized.startsWith("DPIS_DIAG_SESSION "))
            normalized.substring("DPIS_DIAG_SESSION ".length).trim { it <= ' ' }
        else
            null
    }

    class Input(
        packageName: String?,
        val appEnabled: Boolean,
        val viewportEnabled: Boolean,
        val fontScaleEnabled: Boolean,
        val typefaceEnabled: Boolean,
        val wechatDpiEnabled: Boolean
    ) {
        val packageName: String

        init {
            this.packageName = if (packageName != null) packageName else ""
        }
    }

    class WindowedRawLog internal constructor(
        output: String?,
        private val totalParsed: Int,
        private val droppedOutsideWindow: Int,
        private val droppedUnparsed: Int,
        private val droppedNonDpis: Int
    ) {
        private val output: String

        init {
            this.output = if (output != null) output else ""
        }

        fun output(): String {
            return output
        }

        fun totalParsed(): Int {
            return totalParsed
        }

        fun droppedOutsideWindow(): Int {
            return droppedOutsideWindow
        }

        fun droppedUnparsed(): Int {
            return droppedUnparsed
        }

        fun droppedNonDpis(): Int {
            return droppedNonDpis
        }
    }
}
