package com.dpis.module.diagnostics

import com.dpis.module.runtime.SecureProcessLauncher
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

internal object ForegroundAppReader {
    private const val READ_TIMEOUT_MS = 1500L
    private val COMPONENT_PATTERN: Pattern = Pattern.compile(
        "([a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z0-9_]+)+)/"
    )
    private val COMMAND = ("dumpsys activity activities 2>/dev/null "
            + "| grep -m 1 -E 'mResumedActivity|topResumedActivity|ResumedActivity'; "
            + "dumpsys window 2>/dev/null "
            + "| grep -m 1 -E 'mCurrentFocus|mFocusedApp'")

    @JvmStatic
    fun readForegroundPackage(): String {
        var process: Process? = null
        try {
            val startedProcess = SecureProcessLauncher.startMerged("su", "-c", COMMAND) ?: return ""
            process = startedProcess
            if (!startedProcess.waitFor(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                startedProcess.destroyForcibly()
                return ""
            }
            return parsePackage(readAll(startedProcess))
        } catch (exception: IOException) {
            return ""
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            return ""
        } finally {
            if (process != null) {
                process.destroy()
            }
        }
    }

    @JvmStatic
    fun parsePackage(output: String?): String {
        if (output == null || output.isBlank()) {
            return ""
        }
        val matcher = COMPONENT_PATTERN.matcher(output)
        while (matcher.find()) {
            val packageName = matcher.group(1)
            if (!packageName.startsWith("android.")) {
                return packageName
            }
        }
        return ""
    }

    @Throws(IOException::class)
    private fun readAll(process: Process): String {
        val builder = StringBuilder()
        BufferedReader(
            InputStreamReader(
                process.inputStream, StandardCharsets.UTF_8
            )
        ).use { reader ->
            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                if (builder.length > 0) {
                    builder.append('\n')
                }
                builder.append(line)
            }
        }
        return builder.toString()
    }
}
