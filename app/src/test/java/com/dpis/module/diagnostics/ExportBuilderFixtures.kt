package com.dpis.module.diagnostics

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.root.RootAccessProbe
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipInputStream

internal object ExportBuilderFixtures {
    val sessionStart = millis("2023-11-15 06:13:20.000")
    val sessionEnd = millis("2023-11-15 06:13:30.000")

    fun builder(
        appLogs: List<DpisLogEntry?> = emptyList(),
        rawLog: String = "",
    ) = ExportBuilder(
        ExportBuilder.DpisLogReader { appLogs },
        ExportBuilder.RawLogReader { LogReadResult(0, "test-source", rawLog, "") },
    )

    fun appLog(message: String, timestampMillis: Long): DpisLogEntry = DpisLogEntry(
        timestampMillis,
        "11-14 22:13:20",
        "I",
        "DPIS",
        "io.github.kwensiu.dpis",
        "io.github.kwensiu.dpis",
        "DPIS",
        message,
        false,
    )

    fun result(
        timelineEvents: List<String> = listOf("11-14 22:13:20.000 session started"),
        packageName: String = "com.example.app",
        wechatDpi: Int? = null,
    ): Coordinator.Result {
        val request = Coordinator.Request(
            packageName,
            if (packageName == "com.tencent.mm") "微信" else "Example",
            if (packageName == "com.tencent.mm") "8.0.74" else "1.2.3",
            true,
            true,
            true,
            false,
            if (packageName == "com.tencent.mm") ViewportTargetSpec.off() else ViewportTargetSpec.absoluteDp(
                411
            ),
            if (packageName == "com.tencent.mm") ViewportApplyMode.OFF else ViewportApplyMode.AUTO,
            if (packageName == "com.tencent.mm") null else 120,
            if (packageName == "com.tencent.mm") FontApplyMode.OFF else FontApplyMode.FIELD_REWRITE,
            if (packageName == "com.tencent.mm") null else "font-id",
            if (packageName == "com.tencent.mm") null else "system_server_font",
            wechatDpi,
        )
        return Coordinator.Result(
            request,
            sessionStart,
            sessionEnd,
            10_000L,
            true,
            RootAccessProbe.Result.available("Magisk"),
            true,
            "summary",
            timelineEvents,
        )
    }

    fun unzip(zipBytes: ByteArray): Map<String, String> {
        val entries = LinkedHashMap<String, String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = String(zip.readBytes(), StandardCharsets.UTF_8)
            }
        }
        return entries
    }

    fun section(text: String, startMarker: String, endMarker: String): String {
        val start = text.indexOf(startMarker)
        val end = text.indexOf(endMarker)
        return if (start >= 0 && end >= start) text.substring(start, end) else ""
    }

    private fun millis(value: String): Long =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).parse(value)!!.time
}
