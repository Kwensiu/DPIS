package com.dpis.module.diagnostics.device

import com.dpis.module.root.RootAppProcessLauncher.ShellResult
import com.dpis.module.runtime.transport.SecureProcessLauncher
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID
import kotlin.math.max

/**
 * Root-backed Perfetto lifecycle for one feedback diagnostic session.
 * 
 * 
 * The trace is deliberately owned by the diagnostic coordinator rather than
 * by the target app process. Runtime evidence remains target-process-owned;
 * Perfetto provides the system-wide timeline used for later correlation.
 */
internal class PerfettoTrace private constructor(shellRunner: ShellRunner) {
    internal fun interface ShellRunner {
        fun run(command: String): ShellResult
    }

    private val tracePath: String
    private val pidPath: String
    private val errorPath: String
    private val configPath: String
    private val shellRunner: ShellRunner
    private var started = false

    init {
        val id = UUID.randomUUID().toString()
        tracePath = DIRECTORY + "/" + id + ".pftrace"
        pidPath = tracePath + ".pid"
        errorPath = tracePath + ".error"
        configPath = tracePath + ".config"
        this.shellRunner = shellRunner
    }

    fun stop(): StopResult {
        if (!started) {
            return StopResult.unavailable("Perfetto trace was not started")
        }
        started = false
        val result = shellRunner.run(
            ("if [ -s " + quote(pidPath) + " ]; then kill -INT $(cat "
                    + quote(pidPath) + ") 2>/dev/null || true; fi"
                    + "; i=0; while [ \$i -lt 30 ] && [ -s " + quote(pidPath)
                    + " ] && kill -0 $(cat " + quote(pidPath)
                    + ") 2>/dev/null; do i=$((i+1)); sleep 0.1; done"
                    + "; if [ -s " + quote(tracePath) + " ]; then"
                    + " size=$(wc -c < " + quote(tracePath) + ")"
                    + "; if [ \"\$size\" -le " + TRACE_MAX_FILE_BYTES + " ]; then"
                    + " printf 'available:size=%s' \"\$size\";"
                    + " else printf 'available:size=%s,truncated=true' " + TRACE_MAX_FILE_BYTES + "; fi"
                    + "; else printf 'unavailable:error='; cat " + quote(errorPath)
                    + " 2>/dev/null; exit 2; fi"
                    + "; exit $?")
        )
        if (result.code() != 0 || result.output().isBlank()) {
            return StopResult.unavailable(
                "Perfetto trace stop failed: " + compact(result.output())
            )
        }
        val output = result.output().trim { it <= ' ' }
        if (!output.startsWith("available:size=")) {
            return StopResult.unavailable("Perfetto trace unavailable: " + compact(output))
        }
        val sizeText = output.substring("available:size=".length).split(",".toRegex(), limit = 2)
            .toTypedArray()[0]
        try {
            val size = sizeText.trim { it <= ' ' }.toLong()
            val truncated = output.contains("truncated=true")
            return StopResult.ready(
                size, truncated,
                if (truncated)
                    "trace exceeded device-side size limit"
                else
                    "trace ready for diagnostic export"
            )
        } catch (exception: NumberFormatException) {
            return StopResult.unavailable("Perfetto trace size was invalid")
        }
    }

    fun discard() {
        if (!started) {
            return
        }
        started = false
        shellRunner.run(
            ("if [ -s " + quote(pidPath) + " ]; then kill -TERM $(cat "
                    + quote(pidPath) + ") 2>/dev/null || true; fi"
                    + "; rm -f " + quote(tracePath) + " " + quote(pidPath)
                    + " " + quote(errorPath) + " " + quote(configPath))
        )
    }

    /**
     * Transfers the completed trace into the app process, then removes root-owned temporary
     * files. The trace only exists on-device until its diagnostic ZIP is assembled.
     */
    fun consumeStoppedTrace(stoppedTrace: StopResult?, destination: File? = null): StopResult {
        if (stoppedTrace == null || !stoppedTrace.available) {
            return if (stoppedTrace != null)
                stoppedTrace
            else
                StopResult.unavailable("Perfetto trace was not stopped")
        }
        if (stoppedTrace.truncated) {
            discardCompletedTrace()
            return StopResult.ready(
                stoppedTrace.sizeBytes, true, ByteArray(0),
                null,
                "trace exceeded device-side size limit and was not exported"
            )
        }
        val traceBytes = if (destination == null) {
            ByteArrayOutputStream(
                stoppedTrace.sizeBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            )
        } else {
            null
        }
        val traceOutput = destination?.let {
            it.parentFile?.mkdirs()
            FileOutputStream(it)
        }
        var offset = 0L
        var exported = false
        try {
            while (offset < stoppedTrace.sizeBytes) {
                val count = minOf(TRACE_TRANSFER_CHUNK_BYTES, stoppedTrace.sizeBytes - offset)
                val result = shellRunner.run(
                    ("dd if=" + quote(tracePath) + " bs=" + TRACE_TRANSFER_CHUNK_BYTES
                            + " skip=" + (offset / TRACE_TRANSFER_CHUNK_BYTES)
                            + " count=1 2>/dev/null | base64")
                )
                if (result.code() != 0 || result.output().isBlank()) {
                    return StopResult.unavailable(
                        "Perfetto trace export failed: " + compact(result.output())
                    )
                }
                val bytes = Base64.getMimeDecoder().decode(result.output())
                if (bytes.size.toLong() != count) {
                    return StopResult.unavailable("Perfetto trace export size mismatch")
                }
                traceOutput?.write(bytes) ?: traceBytes?.write(bytes)
                offset += count
            }
            traceOutput?.flush()
            val bytes = traceBytes?.toByteArray() ?: ByteArray(0)
            exported = true
            return StopResult.ready(
                stoppedTrace.sizeBytes, false, bytes, destination?.absolutePath,
                "trace exported with diagnostic package"
            )
        } catch (exception: IllegalArgumentException) {
            return StopResult.unavailable("Perfetto trace export was invalid")
        } finally {
            traceOutput?.close()
            if (destination != null && !exported) {
                destination.delete()
            }
            shellRunner.run(removeTraceFilesCommand())
        }
    }

    private fun removeTraceFilesCommand(): String {
        return "rm -f " + quote(tracePath) + " " + quote(pidPath) + " " +
                quote(errorPath) + " " + quote(configPath)
    }

    private fun discardCompletedTrace() {
        shellRunner.run(
            ("rm -f " + quote(tracePath) + " " + quote(pidPath)
                    + " " + quote(errorPath) + " " + quote(configPath))
        )
    }

    internal class StartResult private constructor(
        @JvmField val available: Boolean,
        @JvmField val note: String?,
        @JvmField val trace: PerfettoTrace?
    ) {
        companion object {
            fun ready(trace: PerfettoTrace?): StartResult {
                return StartResult(true, "", trace)
            }

            fun unavailable(note: String?): StartResult {
                return StartResult(false, note, null)
            }
        }
    }

    internal class StopResult private constructor(
        @JvmField val available: Boolean,
        sizeBytes: Long,
        @JvmField val truncated: Boolean,
        traceBytes: ByteArray?,
        traceFilePath: String?,
        note: String?
    ) {
        @JvmField
        val sizeBytes: Long
        @JvmField
        val traceBytes: ByteArray?
        @JvmField
        val traceFilePath: String?

        @JvmField
        val note: String

        init {
            this.sizeBytes = max(0L, sizeBytes)
            this.traceBytes = if (traceBytes != null) traceBytes.clone() else ByteArray(0)
            this.traceFilePath = traceFilePath
            this.note = if (note != null) note else ""
        }

        companion object {
            fun ready(sizeBytes: Long, truncated: Boolean, note: String?): StopResult {
                return ready(sizeBytes, truncated, ByteArray(0), null, note)
            }

            fun ready(
                sizeBytes: Long,
                truncated: Boolean,
                traceBytes: ByteArray?,
                traceFilePath: String?,
                note: String?
            ): StopResult {
                return StopResult(true, sizeBytes, truncated, traceBytes, traceFilePath, note)
            }

            @JvmStatic
            fun unavailable(note: String?): StopResult {
                return StopResult(false, 0L, false, ByteArray(0), null, note)
            }
        }
    }

    companion object {
        private const val DIRECTORY = "/data/local/tmp/dpis-feedback-diagnostic"
        const val MAX_CAPTURE_DURATION_MS = 86_400_000L
        private const val TRACE_BUFFER_KB = 128L * 1024L
        private const val TRACE_MAX_FILE_BYTES = 128L * 1024L * 1024L

        // Keep shell output bounded while preserving Base64 block alignment.
        private const val TRACE_TRANSFER_CHUNK_BYTES = 3L * 1024L * 1024L

        @JvmStatic
        @JvmOverloads
        fun start(
            shellRunner: ShellRunner? = null,
            durationMs: Long = MAX_CAPTURE_DURATION_MS,
        ): StartResult {
            val trace =
                PerfettoTrace(
                    if (shellRunner != null)
                        shellRunner
                    else ShellRunner { command: String? -> runSuCommand(command) })
            val result = trace.shellRunner.run(
                ("mkdir -p " + quote(DIRECTORY)
                        + " && rm -f " + quote(trace.tracePath)
                        + " " + quote(trace.pidPath)
                        + " " + quote(trace.errorPath)
                        + " " + quote(trace.configPath)
                        + " && printf %s " + quote(config(durationMs))
                        + " > " + quote(trace.configPath)
                        // The perfetto client writes -o. write_into_file is owned by
                        // traced and exits immediately when that daemon cannot create
                        // the output under /data/local/tmp.
                        + " && /system/bin/perfetto --background-wait --txt -c "
                        + quote(trace.configPath)
                        + " -o " + quote(trace.tracePath)
                        + " > " + quote(trace.pidPath)
                        + " 2>" + quote(trace.errorPath))
            )
            if (result.code() != 0) {
                return StartResult.unavailable(compact(result.output()))
            }
            val readiness = trace.shellRunner.run(
                ("if [ -s " + quote(trace.pidPath) + " ]"
                        + " && kill -0 $(cat " + quote(trace.pidPath) + ") 2>/dev/null; then"
                        + " exit 0; else cat " + quote(trace.errorPath)
                        + " 2>/dev/null; exit 2; fi")
            )
            if (readiness.code() != 0) {
                trace.discard()
                return StartResult.unavailable(
                    "Perfetto trace did not stay running: " + compact(readiness.output())
                )
            }
            trace.started = true
            return StartResult.ready(trace)
        }

        private fun config(durationMs: Long): String {
            val boundedDurationMs = durationMs.coerceIn(1_000L, MAX_CAPTURE_DURATION_MS)
            return ("buffers { size_kb: " + TRACE_BUFFER_KB + " fill_policy: DISCARD }\n"
                    + "duration_ms: " + boundedDurationMs + "\n"
                    + "data_sources { config { name: \"linux.ftrace\" "
                    + "ftrace_config { "
                    + "ftrace_events: \"sched/sched_switch\" "
                    + "ftrace_events: \"sched/sched_wakeup\" "
                    + "ftrace_events: \"sched/sched_waking\" "
                    + "ftrace_events: \"sched/sched_process_exit\" "
                    + "ftrace_events: \"sched/sched_process_free\" "
                    + "ftrace_events: \"task/task_newtask\" "
                    + "ftrace_events: \"task/task_rename\" "
                    + "atrace_categories: \"gfx\" "
                    + "atrace_categories: \"input\" "
                    + "} } }\n"
                    // Keep the first diagnostic profile focused on scheduler and frame evidence.
                    // If those signals cannot explain a reproduced stall, restore the omitted
                    // view, binder, or process-stats sources based on that concrete evidence.
                    + "data_sources { config { name: \"android.surfaceflinger.frametimeline\" } }\n")
        }

        private fun runSuCommand(command: String?): ShellResult {
            try {
                val process = SecureProcessLauncher.start("su", "-c", command)
                val input = process.inputStream
                val outputBytes = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                var read: Int
                while ((input.read(buffer).also { read = it }) != -1) {
                    outputBytes.write(buffer, 0, read)
                }
                val output = outputBytes.toString(StandardCharsets.UTF_8.name())
                return ShellResult(process.waitFor(), output)
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
                return ShellResult(-1, exception.message)
            } catch (exception: Exception) {
                return ShellResult(-1, exception.message)
            }
        }

        private fun quote(value: String): String {
            return "'" + value.replace("'", "'\\''") + "'"
        }

        private fun compact(value: String?): String {
            return if (value == null || value.isBlank()) "unknown" else value.trim { it <= ' ' }
        }
    }
}
