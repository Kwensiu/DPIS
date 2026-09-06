package com.dpis.module.diagnostics

import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.function.Function
import java.util.function.ToLongFunction
import java.util.regex.Pattern

object DpisLogParser {
    private const val DPIS_MODULE_PACKAGE = "io.github.kwensiu.dpis"
    private const val LSPOSED_HOT_RELOAD_PREFIX = "Auto hot reload "
    private val LSPOSED_TIMESTAMP_PATTERN: Pattern = Pattern.compile(
        "^\\[\\s*+\\d{4}-(\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(\\.\\d+)?\\s+[^\\r\\n]*"
    )
    private val GENERIC_LSPOSED_PATTERN: Pattern = Pattern.compile(
        "^\\[\\s*+\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?\\s+[^\\r\\n]*\\s([VDIWEF])/([^\\]\\s]++)\\s*+]\\s*(.*)$"
    )

    @JvmStatic
    fun parseLsposedDpis(raw: String?): MutableList<DpisLogEntry?> {
        val entries: MutableList<DpisLogEntry?> = ArrayList<DpisLogEntry?>()
        if (raw == null || raw.isBlank()) {
            return entries
        }
        val seen: MutableSet<String?> = LinkedHashSet<String?>()
        var lastTimestamp = ""
        try {
            BufferedReader(StringReader(raw)).use { reader ->
                var line: String?
                while ((reader.readLine().also { line = it }) != null) {
                    val parsedLine = parseLine(line!!.replace("\u0000", ""), lastTimestamp)
                    if (parsedLine == null) {
                        continue
                    }
                    if (!parsedLine.timestamp.isEmpty()) {
                        lastTimestamp = parsedLine.timestamp
                    }
                    val entry = entryFromParsedLine(parsedLine)
                    if (entry == null) {
                        continue
                    }
                    val key = (entry.timestamp
                            + "|"
                            + entry.modulePackage
                            + "|"
                            + entry.tag
                            + "|"
                            + entry.process
                            + "|"
                            + entry.message)
                    if (seen.add(key)) {
                        entries.add(entry)
                    }
                }
            }
        } catch (exception: IOException) {
            throw IllegalStateException(exception)
        }
        entries.sortWith(
            Comparator
                .comparingLong<DpisLogEntry?>(ToLongFunction { entry: DpisLogEntry? -> entry!!.timestampMillis })
                .thenComparing<String>(Function { entry: DpisLogEntry? -> entry!!.timestamp })
                .thenComparing<String>(Function { entry: DpisLogEntry? -> entry!!.process })
                .thenComparing<String>(Function { entry: DpisLogEntry? -> entry!!.tag })
                .thenComparing<String>(Function { entry: DpisLogEntry? -> entry!!.message })
        )
        return entries
    }

    private fun entryFromParsedLine(line: ParsedLine): DpisLogEntry? {
        if (!isRelevantToDpis(line)) {
            return null
        }
        return DpisLogEntry(
            sortableTimestampMillis(line.timestamp),
            line.timestamp,
            line.level,
            "LSPosed",
            line.process,
            line.modulePackage,
            line.tag,
            if (line.message.isEmpty()) line.raw else line.message,
            true
        )
    }

    private fun isRelevantToDpis(line: ParsedLine): Boolean {
        return DPIS_MODULE_PACKAGE == line.modulePackage
                || isDpisHotReloadFrameworkWarning(line.message)
    }

    private fun isDpisHotReloadFrameworkWarning(message: String?): Boolean {
        return message != null && message.contains(DPIS_MODULE_PACKAGE)
                && message.contains(LSPOSED_HOT_RELOAD_PREFIX)
    }

    private fun parseLine(line: String?, inheritedTimestamp: String?): ParsedLine? {
        if (line == null) {
            return null
        }
        val trimmed = line.trim { it <= ' ' }
        if (trimmed.isEmpty()) {
            return null
        }
        val timestamp = extractTime(trimmed)
        var parsedLine = parseLsposedMetadata(trimmed)
        if (parsedLine != null) {
            parsedLine.timestamp = (if (timestamp != null) timestamp else inheritedTimestamp)!!
            return parsedLine
        }
        parsedLine = parseGenericLsposedLine(trimmed)
        if (parsedLine != null) {
            parsedLine.timestamp = (if (timestamp != null) timestamp else inheritedTimestamp)!!
            return parsedLine
        }
        val body = stripKnownPrefixes(trimmed)
        return ParsedLine(
            inheritedTimestamp,
            "",
            "",
            extractFallbackTag(body),
            "",
            body,
            trimmed
        )
    }

    private fun parseLsposedMetadata(line: String): ParsedLine? {
        val prefixEnd = line.indexOf("] (")
        if (prefixEnd < 0) {
            return null
        }
        val levelIndex = line.lastIndexOf("/LSPosedFramework", prefixEnd)
        var level = ""
        if (levelIndex > 0) {
            level = line.substring(levelIndex - 1, levelIndex)
        }
        val processStart = prefixEnd + 3
        val processEnd = line.indexOf(")[", processStart)
        if (processEnd < 0) {
            return null
        }
        val metadataStart = processEnd + 1
        val metadataEnd = line.indexOf(']', metadataStart)
        if (metadataEnd < 0 || metadataEnd + 1 >= line.length) {
            return null
        }
        val process = line.substring(processStart, processEnd)
        val metadata: Array<String?> =
            line.substring(metadataStart + 1, metadataEnd).split(",".toRegex(), limit = 3)
                .toTypedArray()
        val modulePackage = if (metadata.size > 0) metadata[0] else ""
        val tag = if (metadata.size > 1) metadata[1] else ""
        val message = line.substring(metadataEnd + 1).trim { it <= ' ' }
        return ParsedLine("", level, process, modulePackage, tag, message, line)
    }

    private fun parseGenericLsposedLine(line: String): ParsedLine? {
        val matcher = GENERIC_LSPOSED_PATTERN.matcher(line)
        if (!matcher.matches()) {
            return null
        }
        return ParsedLine(
            "",
            matcher.group(1),
            "",
            "",
            matcher.group(2),
            matcher.group(3).trim { it <= ' ' },
            line
        )
    }

    private fun stripKnownPrefixes(line: String): String {
        var value = line
        value =
            value.replaceFirst("^\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d+\\s+".toRegex(), "")
        value = value.replaceFirst("^\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\s+".toRegex(), "")
        value = value.replaceFirst(
            "^\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d+\\s+".toRegex(),
            ""
        )
        value =
            value.replaceFirst("^\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\s+".toRegex(), "")
        value = value.replaceFirst("^\\[[^]]+]\\s*".toRegex(), "")
        return value.trim { it <= ' ' }
    }

    private fun extractTime(line: String): String? {
        val matcher = LSPOSED_TIMESTAMP_PATTERN.matcher(line)
        if (matcher.matches()) {
            val fraction = if (matcher.group(2) != null) matcher.group(2) else ""
            return matcher.group(1).replace('T', ' ') + fraction
        }
        if (line.matches("^\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}.*".toRegex())) {
            return line.substring(0, 14)
        }
        if (line.matches("^\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}.*".toRegex())) {
            return line.substring(5, 19)
        }
        return null
    }

    private fun sortableTimestampMillis(timestamp: String?): Long {
        if (timestamp == null || timestamp.isBlank()) {
            return 0L
        }
        var digits = timestamp.replace("[^0-9]".toRegex(), "")
        if (digits.isEmpty()) {
            return 0L
        }
        if (digits.length > 17) {
            digits = digits.substring(0, 17)
        }
        while (digits.length < 17) {
            digits += "0"
        }
        try {
            return digits.toLong()
        } catch (exception: NumberFormatException) {
            return 0L
        }
    }

    private fun extractFallbackTag(body: String): String {
        val colon = body.indexOf(':')
        if (colon > 0) {
            val candidate = body.substring(0, colon).trim { it <= ' ' }
            if (!candidate.isEmpty() && candidate.length <= 48) {
                return candidate
            }
        }
        return if (body.contains("DPIS")) "DPIS" else "LSPosed"
    }

    private class ParsedLine(
        timestamp: String?,
        level: String?,
        process: String?,
        modulePackage: String?,
        tag: String?,
        message: String?,
        raw: String?
    ) {
        var timestamp: String
        val level: String
        val process: String
        val modulePackage: String
        val tag: String
        val message: String
        val raw: String

        init {
            this.timestamp = if (timestamp != null) timestamp else ""
            this.level = if (level != null) level else ""
            this.process = if (process != null) process else ""
            this.modulePackage = if (modulePackage != null) modulePackage else ""
            this.tag = if (tag != null) tag else ""
            this.message = if (message != null) message else ""
            this.raw = if (raw != null) raw else ""
        }
    }
}
