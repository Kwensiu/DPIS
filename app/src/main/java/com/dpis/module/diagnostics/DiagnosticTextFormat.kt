package com.dpis.module.diagnostics

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object DiagnosticTextFormat {
    const val UNKNOWN = "unknown"

    fun valueOrUnknown(value: String?): String = valueOrDefault(value, UNKNOWN)

    fun valueOrDefault(value: String?, fallback: String): String {
        val normalized = value?.trim().orEmpty()
        return normalized.ifEmpty { fallback }
    }

    fun displayTime(millis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(millis))
    }

    fun fileTime(millis: Long): String {
        return SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(millis))
    }

    fun safeFilePart(value: String?): String {
        return valueOrDefault(value, "unknown").replace(Regex("[^A-Za-z0-9_.-]"), "_")
    }
}
