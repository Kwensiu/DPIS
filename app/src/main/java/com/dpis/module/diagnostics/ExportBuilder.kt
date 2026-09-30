package com.dpis.module.diagnostics

import android.content.Context
import com.dpis.module.diagnostics.device.LsposedLogReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportBuilder {
    fun interface DpisLogReader {
        fun read(): List<DpisLogEntry?>?
    }

    fun interface RawLogReader {
        fun read(): LogReadResult?
    }

    class EntrySummary {
        @JvmField
        val name: String

        @JvmField
        val byteCount: Int

        @JvmField
        val lineCount: Int

        @JvmField
        val hasLineCount: Boolean

        constructor(name: String?, content: String?) : this(
            name,
            content?.toByteArray(StandardCharsets.UTF_8)?.size ?: 0,
            countLines(content),
            true,
        )

        constructor(name: String?, byteCount: Int, lineCount: Int) : this(
            name,
            byteCount,
            lineCount,
            true,
        )

        private constructor(
            name: String?,
            byteCount: Int,
            lineCount: Int,
            hasLineCount: Boolean,
        ) {
            this.name = name.orEmpty()
            this.byteCount = maxOf(0, byteCount)
            this.lineCount = maxOf(0, lineCount)
            this.hasLineCount = hasLineCount
        }

        companion object {
            @JvmStatic
            fun binary(name: String?, byteCount: Int): EntrySummary {
                return EntrySummary(name, byteCount, 0, false)
            }

            private fun countLines(content: String?): Int {
                if (content.isNullOrEmpty()) {
                    return 0
                }
                var count = 1
                for (element in content) {
                    if (element == '\n') {
                        count++
                    }
                }
                return count
            }
        }
    }

    class DiagnosticPackage(
        @JvmField val result: Coordinator.Result?,
        fileName: String?,
        zipBytes: ByteArray?,
        entries: List<EntrySummary>?,
    ) {
        @JvmField
        val fileName: String = fileName.orEmpty()

        @JvmField
        val zipBytes: ByteArray = zipBytes?.clone() ?: ByteArray(0)

        @JvmField
        val entries: List<EntrySummary> = if (entries != null) ArrayList(entries) else emptyList()
    }

    private val dpisLogReader: DpisLogReader
    private val lsposedLogReader: RawLogReader

    constructor(context: Context) : this(
        DpisLogReader { DpisAppLogStore(context).readRecentEntries() },
        RawLogReader { LsposedLogReader.readLsposedDpisCurrent() },
    )

    constructor(
        dpisLogReader: DpisLogReader?,
        lsposedLogReader: RawLogReader?,
    ) {
        this.dpisLogReader = dpisLogReader ?: DpisLogReader { emptyList() }
        this.lsposedLogReader = lsposedLogReader ?: RawLogReader {
            LsposedLogReader.readLsposedDpisCurrent()
        }
    }

    @Throws(IOException::class)
    fun buildZip(result: Coordinator.Result?): ByteArray = buildPackage(result).zipBytes

    @Throws(IOException::class)
    fun buildPackage(result: Coordinator.Result?): DiagnosticPackage {
        val lsposedLog = readLsposedLog()
        val window = DiagnosticLogExcerpt.windowFor(result)
        val runtimeEvents = DiagnosticLogExcerpt.sortedRuntimeEvents(result, lsposedLog, window)
        val diagnostic = buildDiagnosticText(result, runtimeEvents)
        val timeline = StructuredEvidenceExporter.buildTimelineTsv(runtimeEvents)
        val moduleEffects = StructuredEvidenceExporter.buildModuleEffectsTsv(
            result,
            runtimeEvents,
            if (result != null) result.performanceSnapshot else null,
        )
        val dpisLog = DiagnosticLogExcerpt.buildDpisLogText(dpisLogReader.read(), window)
        val lsposed = DiagnosticLogExcerpt.buildLsposedLogText(
            lsposedLog,
            window,
            result?.request,
        )
        val perfettoTrace = if (result != null) result.perfettoTraceBytes else ByteArray(0)
        val output = ByteArrayOutputStream()
        ZipOutputStream(output, StandardCharsets.UTF_8).use { zip ->
            writeZipEntry(zip, DIAGNOSTIC_ENTRY_NAME, diagnostic)
            writeZipEntry(zip, TIMELINE_ENTRY_NAME, timeline)
            writeZipEntry(zip, MODULE_EFFECTS_ENTRY_NAME, moduleEffects)
            writeZipEntry(zip, DPIS_LOG_ENTRY_NAME, dpisLog)
            writeZipEntry(zip, LSPOSED_LOG_ENTRY_NAME, lsposed)
            if (perfettoTrace.isNotEmpty()) {
                writeZipEntry(zip, PERFETTO_TRACE_ENTRY_NAME, perfettoTrace)
            }
        }
        val entries = ArrayList(
            listOf(
                EntrySummary(DIAGNOSTIC_ENTRY_NAME, diagnostic),
                EntrySummary(TIMELINE_ENTRY_NAME, timeline),
                EntrySummary(MODULE_EFFECTS_ENTRY_NAME, moduleEffects),
                EntrySummary(DPIS_LOG_ENTRY_NAME, dpisLog),
                EntrySummary(LSPOSED_LOG_ENTRY_NAME, lsposed),
            ),
        )
        if (perfettoTrace.isNotEmpty()) {
            entries.add(EntrySummary.binary(PERFETTO_TRACE_ENTRY_NAME, perfettoTrace.size))
        }
        return DiagnosticPackage(
            result,
            buildFileName(result),
            output.toByteArray(),
            entries,
        )
    }

    @Throws(IOException::class)
    fun writeZip(output: OutputStream, result: Coordinator.Result?) {
        output.write(buildPackage(result).zipBytes)
    }

    fun buildDiagnosticText(result: Coordinator.Result?): String {
        val window = DiagnosticLogExcerpt.windowFor(result)
        return buildDiagnosticText(
            result,
            DiagnosticLogExcerpt.sortedRuntimeEvents(result, readLsposedLog(), window),
        )
    }

    fun buildFileName(result: Coordinator.Result?): String {
        val packageName = if (result?.request != null) {
            DiagnosticTextFormat.safeFilePart(result.request.packageName)
        } else {
            "unknown"
        }
        val startedAt = result?.startedAtMillis ?: System.currentTimeMillis()
        val finishedAt = if (result != null && result.finishedAtMillis > 0L) {
            result.finishedAtMillis
        } else {
            startedAt
        }
        return "dpis-diagnostic-" +
                packageName +
                "-" +
                DiagnosticTextFormat.fileTime(startedAt) +
                "-" +
                DiagnosticTextFormat.fileTime(finishedAt) +
                ".zip"
    }

    private fun buildDiagnosticText(
        result: Coordinator.Result?,
        runtimeEvents: List<String>,
    ): String = DiagnosticReportText.build(result, runtimeEvents)

    private fun readLsposedLog(): LogReadResult {
        return DiagnosticLogExcerpt.readLsposedLog(lsposedLogReader.read())
    }

    companion object {
        const val DIAGNOSTIC_ENTRY_NAME = "diagnostic.txt"
        const val TIMELINE_ENTRY_NAME = "timeline.tsv"
        const val MODULE_EFFECTS_ENTRY_NAME = "module-effects.tsv"
        const val DPIS_LOG_ENTRY_NAME = "dpis-log.txt"
        const val LSPOSED_LOG_ENTRY_NAME = "lsposed-log.txt"
        const val PERFETTO_TRACE_ENTRY_NAME = "perfetto-trace.pftrace"
        const val MIME_TYPE = "application/zip"

        @Throws(IOException::class)
        private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String?) {
            writeZipEntry(zip, name, (content ?: "").toByteArray(StandardCharsets.UTF_8))
        }

        @Throws(IOException::class)
        private fun writeZipEntry(zip: ZipOutputStream, name: String, content: ByteArray?) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(content ?: ByteArray(0))
            zip.closeEntry()
        }
    }
}
