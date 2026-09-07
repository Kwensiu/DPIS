package com.dpis.module.diagnostics

import android.app.Application
import android.content.Context
import android.os.Build
import com.dpis.module.DpisLog
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.max

class DpisAppLogStore {
    private val appContext: Context?
    private val logFile: File
    private val maxStoredLines: Int
    private val maxStoredBytes: Long
    private val lock = Any()

    constructor(context: Context) {
        appContext = context.getApplicationContext()
        logFile = File(File(appContext.getFilesDir(), LOG_DIRECTORY_NAME), LOG_FILE_NAME)
        maxStoredLines = DEFAULT_MAX_STORED_LINES
        maxStoredBytes = DEFAULT_MAX_STORED_BYTES
    }

    internal constructor(logFile: File, maxStoredLines: Int, maxStoredBytes: Long) {
        appContext = null
        this.logFile = logFile
        this.maxStoredLines = max(1, maxStoredLines)
        this.maxStoredBytes = max(128L, maxStoredBytes)
    }

    fun record(level: String?, message: String?) {
        if (message == null || message.isBlank()) {
            return
        }
        val timestampMillis = System.currentTimeMillis()
        synchronized(lock) {
            try {
                ensureParentDirectory()
                Files.write(
                    logFile.toPath(),
                    (toJsonLine(timestampMillis, level, message) + "\n")
                        .toByteArray(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                )
                trimToCapacity()
            } catch (ignored: IOException) {
                // Logging must never affect runtime behavior.
            }
        }
    }

    fun readRecentEntries(): MutableList<DpisLogEntry?> {
        synchronized(lock) {
            return readEntriesLocked()
        }
    }

    fun readRecentEntries(maxEntries: Int): MutableList<DpisLogEntry?>? {
        synchronized(lock) {
            val entries = readEntriesLocked()
            if (maxEntries <= 0 || entries.size <= maxEntries) {
                return entries
            }
            return ArrayList<DpisLogEntry?>(
                entries.subList(
                    entries.size - maxEntries,
                    entries.size
                )
            )
        }
    }

    private fun readEntriesLocked(): MutableList<DpisLogEntry?> {
        val entries: MutableList<DpisLogEntry?> = ArrayList<DpisLogEntry?>()
        if (!logFile.isFile()) {
            return entries
        }
        val lines: MutableList<String>
        try {
            lines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8)
        } catch (exception: IOException) {
            return entries
        }
        for (line in lines) {
            val entry = parseJsonLine(line)
            if (entry != null) {
                entries.add(entry)
            }
        }
        return entries
    }

    private fun parseJsonLine(line: String?): DpisLogEntry? {
        if (line == null || line.isBlank()) {
            return null
        }
        val timestampMillis: Long = readLongField(line, "timestampMillis", 0L)
        val message: String = readStringField(line, "message")
        if (message.isBlank()) {
            return null
        }
        return DpisLogEntry(
            timestampMillis,
            formatTime(timestampMillis),
            readStringField(line, "level"),
            readStringField(line, "source"),
            readStringField(line, "process"),
            readStringField(line, "package"),
            readStringField(line, "tag"),
            message,
            false
        )
    }

    private fun toJsonLine(timestampMillis: Long, level: String?, message: String?): String {
        return ("{"
                + "\"timestampMillis\":" + timestampMillis
                + ",\"displayTime\":\"" + jsonEscape(formatTime(timestampMillis)) + "\""
                + ",\"level\":\"" + jsonEscape(sanitize(level)) + "\""
                + ",\"source\":\"" + DPIS_LOG_SOURCE + "\""
                + ",\"package\":\"" + DPIS_MODULE_PACKAGE + "\""
                + ",\"process\":\"" + jsonEscape(currentProcessName()) + "\""
                + ",\"tag\":\"" + DpisLog.TAG + "\""
                + ",\"message\":\"" + jsonEscape(sanitize(message)) + "\""
                + "}")
    }

    @Throws(IOException::class)
    private fun ensureParentDirectory() {
        val parent = logFile.getParentFile()
        if (parent != null) {
            Files.createDirectories(parent.toPath())
        }
    }

    @Throws(IOException::class)
    private fun trimToCapacity() {
        if (!logFile.isFile()) {
            return
        }
        val lines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8)
        val retained = retainNewestWithinCapacity(lines)
        if (retained.size != lines.size || logFile.length() > maxStoredBytes) {
            Files.write(
                logFile.toPath(), retained, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            )
        }
    }

    private fun retainNewestWithinCapacity(lines: MutableList<String>): MutableList<String?> {
        val retained: MutableList<String?> = ArrayList<String?>()
        var retainedBytes = 0L
        for (i in lines.indices.reversed()) {
            val line = lines.get(i)
            val lineBytes = line.toByteArray(StandardCharsets.UTF_8).size + 1L
            if (!retained.isEmpty()
                && (retained.size >= maxStoredLines
                        || retainedBytes + lineBytes > maxStoredBytes)
            ) {
                break
            }
            retained.add(0, line)
            retainedBytes += lineBytes
        }
        return retained
    }

    private fun currentProcessName(): String? {
        if (appContext == null) {
            return DPIS_MODULE_PACKAGE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = Application.getProcessName()
            if (processName != null && !processName.isBlank()) {
                return processName
            }
        }
        return appContext.getPackageName()
    }

    companion object {
        private const val DEFAULT_MAX_STORED_LINES = 5000
        private val DEFAULT_MAX_STORED_BYTES = 1024L * 1024L
        private const val LOG_DIRECTORY_NAME = "dpis_logs"
        private const val LOG_FILE_NAME = "app_log.jsonl"
        private const val DPIS_LOG_SOURCE = "DPIS"
        private const val DPIS_MODULE_PACKAGE = "io.github.kwensiu.dpis"
        private val JSON_STRING_FIELD_PATTERN: Pattern = Pattern.compile(
            "\"%s\"\\s*+:\\s*+\"((?:\\\\.|[^\"\\\\])*+)\""
        )
        private val JSON_LONG_FIELD_PATTERN: Pattern = Pattern.compile(
            "\"%s\"\\s*+:\\s*+(-?+\\d++)"
        )

        private fun sanitize(value: String?): String {
            return if (value == null)
                ""
            else
                value.replace('\n', ' ').replace('\r', ' ').trim { it <= ' ' }
        }

        private fun formatTime(timestampMillis: Long): String {
            if (timestampMillis <= 0L) {
                return ""
            }
            return SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date(timestampMillis))
        }

        private fun readStringField(line: String, fieldName: String): String {
            val matcher = Pattern.compile(
                String.format(
                    Locale.US,
                    JSON_STRING_FIELD_PATTERN.pattern(),
                    Pattern.quote(fieldName)
                )
            ).matcher(line)
            if (!matcher.find()) {
                return ""
            }
            return jsonUnescape(matcher.group(1) ?: return "")
        }

        private fun readLongField(line: String, fieldName: String, fallback: Long): Long {
            val matcher = Pattern.compile(
                String.format(
                    Locale.US,
                    JSON_LONG_FIELD_PATTERN.pattern(),
                    Pattern.quote(fieldName)
                )
            ).matcher(line)
            if (!matcher.find()) {
                return fallback
            }
            try {
                return (matcher.group(1) ?: return fallback).toLong()
            } catch (exception: NumberFormatException) {
                return fallback
            }
        }

        private fun jsonEscape(value: String?): String {
            val builder = StringBuilder()
            val safeValue = if (value != null) value else ""
            for (i in 0..<safeValue.length) {
                val c = safeValue.get(i)
                if (c == '"' || c == '\\') {
                    builder.append('\\').append(c)
                } else if (c == '\t') {
                    builder.append("\\t")
                } else {
                    builder.append(c)
                }
            }
            return builder.toString()
        }

        // This only reverses escapes emitted by jsonEscape; sanitize() removes line breaks before write.
        private fun jsonUnescape(value: String): String {
            val builder = StringBuilder()
            var escaped = false
            for (i in 0..<value.length) {
                val c = value.get(i)
                if (escaped) {
                    builder.append(if (c == 't') '\t' else c)
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else {
                    builder.append(c)
                }
            }
            if (escaped) {
                builder.append('\\')
            }
            return builder.toString()
        }
    }
}
