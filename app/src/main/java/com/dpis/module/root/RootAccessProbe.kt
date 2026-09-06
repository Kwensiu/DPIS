package com.dpis.module.root

import com.dpis.module.runtime.SecureProcessLauncher
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import kotlin.concurrent.Volatile

object RootAccessProbe {
    private val PROBE_COMMAND = ("id"
            + "; echo DPIS_KSU=\$KSU"
            + "; echo DPIS_KSU_VER=\$KSU_VER"
            + "; echo DPIS_KSU_VER_CODE=\$KSU_VER_CODE"
            + "; echo DPIS_KSU_KERNEL_VER_CODE=\$KSU_KERNEL_VER_CODE"
            + "; echo DPIS_MAGISK_VER=\$MAGISK_VER"
            + "; echo DPIS_MAGISK_VER_CODE=\$MAGISK_VER_CODE")
    private const val PROBE_TIMEOUT_MS = 3000L

    @Volatile
    private var cachedResult: Result = Result.Companion.unknown()
    private val probeInFlight = AtomicBoolean(false)

    @JvmStatic
    fun cachedResult(): Result {
        return cachedResult
    }

    @JvmStatic
    fun warmUpAsync() {
        refreshAsync(null)
    }

    /** Rechecks authorization without requiring an activity or process restart.  */
    @JvmStatic
    fun refreshAsync(callback: Consumer<Result?>?) {
        if (!probeInFlight.compareAndSet(false, true)) {
            return
        }
        Thread(Runnable {
            var result: Result?
            try {
                result = probe()
            } finally {
                probeInFlight.set(false)
            }
            if (callback != null) {
                callback.accept(result)
            }
        }, "dpis-root-access-probe").start()
    }

    @JvmStatic
    fun probe(): Result {
        var process: Process? = null
        try {
            val startedProcess = SecureProcessLauncher.start("su", "-c", PROBE_COMMAND)
                ?: return cache(Result.Companion.unavailable())
            process = startedProcess
            val finished = startedProcess.waitFor(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            if (!finished) {
                startedProcess.destroyForcibly()
                return cache(Result.Companion.unavailable())
            }
            val output = readOutput(startedProcess)
            val code = startedProcess.exitValue()
            if (code != 0 || !output.contains("uid=0")) {
                return cache(Result.Companion.unavailable())
            }
            return cache(Result.Companion.available(resolveProvider(output, readSuVersion())))
        } catch (ignored: IOException) {
            return cache(Result.Companion.unavailable())
        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt()
            return cachedResult
        } finally {
            if (process != null) {
                process.destroy()
            }
        }
    }

    private fun cache(result: Result?): Result {
        cachedResult = if (result != null) result else Result.Companion.unknown()
        return cachedResult
    }

    /** Records successful use of a root shell without replacing a known provider name.  */
    @JvmStatic
    fun recordSuccessfulRootCommand() {
        val current = cachedResult
        if (current.status != Status.AVAILABLE) {
            cachedResult = Result.Companion.available("su")
        }
    }

    @Throws(IOException::class)
    private fun readOutput(process: Process): String {
        val builder = StringBuilder()
        BufferedReader(
            InputStreamReader(process.inputStream)
        ).use { out ->
            BufferedReader(
                InputStreamReader(process.errorStream)
            ).use { err ->
                appendLines(builder, out)
                appendLines(builder, err)
            }
        }
        return builder.toString()
    }

    @Throws(IOException::class)
    private fun appendLines(
        builder: StringBuilder,
        reader: BufferedReader
    ) {
        var line: String?
        while ((reader.readLine().also { line = it }) != null) {
            if (builder.length > 0) {
                builder.append('\n')
            }
            builder.append(line)
        }
    }

    @JvmStatic
    fun resolveProvider(output: String?, suVersion: String?): String {
        val normalizedVersion = if (suVersion != null)
            suVersion.lowercase()
        else
            ""
        if (normalizedVersion.contains("magisk")) {
            return "Magisk"
        }
        if (normalizedVersion.contains("apatch")) {
            return "APatch"
        }
        if (normalizedVersion.contains("kernelsu") || normalizedVersion.contains("ksu")) {
            return "KernelSU"
        }
        val normalized = if (output != null) output else ""
        if (hasProbeValue(normalized, "DPIS_KSU=", "true")
            || hasNonEmptyProbeValue(normalized, "DPIS_KSU_VER=")
            || hasNonEmptyProbeValue(normalized, "DPIS_KSU_VER_CODE=")
            || hasNonEmptyProbeValue(
                normalized,
                "DPIS_KSU_KERNEL_VER_CODE="
            )
        ) {
            return "KernelSU"
        }
        if (hasNonEmptyProbeValue(normalized, "DPIS_MAGISK_VER=")
            || hasNonEmptyProbeValue(normalized, "DPIS_MAGISK_VER_CODE=")
        ) {
            return "Magisk"
        }
        return "su"
    }

    private fun readSuVersion(): String {
        var process: Process? = null
        try {
            val startedProcess = SecureProcessLauncher.start("su", "-v") ?: return ""
            process = startedProcess
            if (!startedProcess.waitFor(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                startedProcess.destroyForcibly()
                return ""
            }
            if (startedProcess.exitValue() != 0) {
                return ""
            }
            return readOutput(startedProcess)
        } catch (ignored: IOException) {
            return ""
        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt()
            return ""
        } finally {
            if (process != null) {
                process.destroy()
            }
        }
    }

    private fun hasProbeValue(
        output: String,
        prefix: String,
        expectedValue: String?
    ): Boolean {
        val expected = if (expectedValue != null)
            expectedValue.trim { it <= ' ' }.lowercase()
        else
            ""
        for (line in output.split("\\R".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (!line.startsWith(prefix)) {
                continue
            }
            val value = line.substring(prefix.length)
                .trim { it <= ' ' }
                .lowercase()
            return value == expected
        }
        return false
    }

    private fun hasNonEmptyProbeValue(output: String, prefix: String): Boolean {
        for (line in output.split("\\R".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (!line.startsWith(prefix)) {
                continue
            }
            val value = line.substring(prefix.length).trim { it <= ' ' }
            return !value.isEmpty()
        }
        return false
    }

    private fun normalizeProvider(provider: String?): String {
        val normalized = if (provider != null) provider.trim { it <= ' ' } else ""
        return if (normalized.isEmpty()) "su" else normalized
    }

    enum class Status {
        UNKNOWN,
        AVAILABLE,
        UNAVAILABLE
    }

    class Result private constructor(status: Status?, provider: String?) {
        @JvmField
        val status: Status
        @JvmField
        val provider: String

        init {
            this.status = if (status != null) status else Status.UNKNOWN
            this.provider = if (this.status == Status.AVAILABLE)
                normalizeProvider(provider)
            else
                ""
        }

        companion object {
            @JvmStatic
            fun unknown(): Result {
                return Result(Status.UNKNOWN, null)
            }

            @JvmStatic
            fun unavailable(): Result {
                return Result(Status.UNAVAILABLE, null)
            }

            @JvmStatic
            fun available(provider: String?): Result {
                return Result(Status.AVAILABLE, provider)
            }
        }
    }
}
