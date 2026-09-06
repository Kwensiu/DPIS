package com.dpis.module.fonts

import android.content.Context
import android.os.Bundle
import com.dpis.module.runtime.SecureProcessLauncher
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object FontDebugLogcatBridge {
    private const val MAX_LINES = 300
    private const val TOP_LIMIT = 20
    private val MARKERS = arrayOf<String?>(
        "DPIS_FONT TextPaint.setTextSize override",
        "DPIS_FONT Paint.setTextSize override",
        "DPIS_FONT ForceTextSize override",
        "DPIS_FONT TextView span override",
        "DPIS_FONT SystemServer config fontScale"
    )

    @JvmStatic
    fun importRecent(context: Context?): Boolean {
        if (context == null) {
            return false
        }
        val counts: MutableMap<String?, Int?> = LinkedHashMap<String?, Int?>()
        var process: Process? = null
        try {
            process = SecureProcessLauncher.startMerged(
                "logcat", "-d", "-t", MAX_LINES.toString(), "-s", "DPIS:I", "*:S"
            )
            BufferedReader(
                InputStreamReader(
                    process.inputStream, StandardCharsets.UTF_8
                )
            ).use { reader ->
                var line: String?
                while ((reader.readLine().also { line = it }) != null) {
                    val key = resolveKey(line)
                    if (key != null) {
                        counts.put(key, counts.getOrDefault(key, 0)!! + 1)
                    }
                }
            }
            process.waitFor()
        } catch (ignored: IOException) {
            return false
        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt()
            return false
        } finally {
            if (process != null) {
                process.destroy()
            }
        }
        if (counts.isEmpty()) {
            return true
        }
        val formatted = formatCounts(counts)
        val extras = Bundle()
        extras.putString(FontDebugStatsStore.EXTRA_CHAIN_5S, formatted)
        extras.putString(FontDebugStatsStore.EXTRA_CHAIN_30S, formatted)
        extras.putString(FontDebugStatsStore.EXTRA_CHAIN_ALL, formatted)
        extras.putInt(
            FontDebugStatsStore.EXTRA_EVENT_TOTAL,
            counts.values.stream().mapToInt { obj: Int? -> obj!!.toInt() }.sum()
        )
        extras.putLong(FontDebugStatsStore.EXTRA_UPDATED_AT, System.currentTimeMillis())
        FontDebugStatsUpdateWriter.applyExtras(context, extras)
        return true
    }

    private fun resolveKey(line: String?): String? {
        if (line == null) {
            return null
        }
        for (marker in MARKERS) {
            val index = line.indexOf(marker!!)
            if (index >= 0) {
                return marker.replace("DPIS_FONT ", "").replace(" override", "")
            }
        }
        return null
    }

    private fun formatCounts(counts: MutableMap<String?, Int?>): String {
        val builder = StringBuilder()
        var emitted = 0
        for (entry in counts.entries) {
            if (emitted > 0) {
                builder.append('\n')
            }
            builder.append(entry.value).append(' ').append(entry.key)
            emitted++
            if (emitted >= TOP_LIMIT) {
                break
            }
        }
        return builder.toString()
    }
}
