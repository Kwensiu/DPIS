package com.dpis.module.diagnostics.device

import com.dpis.module.root.RootAppProcessLauncher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class PerfettoTraceTest {
    @Test
    fun startsDetachedCliTraceWithoutServiceSideFileWriter() {
        val commands = ArrayList<String>()
        val result = PerfettoTrace.start { command ->
            commands += command
            RootAppProcessLauncher.ShellResult(0, "")
        }

        assertTrue(result.available)
        assertNotNull(result.trace)
        assertTrue(commands.size >= 2)

        val launch = commands[0]
        assertTrue(launch.contains("/system/bin/perfetto --background-wait --txt"))
        assertTrue(launch.contains(" > '"))
        assertFalse(launch.contains("nohup"))
        assertFalse(launch.contains("--size"))
        assertFalse(launch.contains("write_into_file: true"))
        assertFalse(launch.contains("file_write_period_ms"))
        assertFalse(launch.contains("max_file_size_bytes"))
        assertTrue(launch.contains("linux.process_stats"))
        assertTrue(launch.contains("scan_all_processes_on_start: true"))
        assertTrue(launch.contains("android.surfaceflinger.frametimeline"))
        assertTrue(launch.contains("task/task_newtask"))
        assertTrue(launch.contains("task/task_rename"))
    }

    @Test
    fun exportsCompletedTraceAndCleansUpDeviceFiles() {
        val commands = ArrayList<String>()
        val started = PerfettoTrace.start { command ->
            commands += command
            when {
                command.contains("available:size") -> RootAppProcessLauncher.ShellResult(
                    0,
                    "available:size=3"
                )

                command.startsWith("base64 ") -> RootAppProcessLauncher.ShellResult(0, "YWJj")
                else -> RootAppProcessLauncher.ShellResult(0, "")
            }
        }

        val trace = started.trace!!
        val stopped = trace.stop()
        val exported = trace.consumeStoppedTrace(stopped)

        assertTrue(stopped.available)
        assertTrue(exported.available)
        assertEquals("abc", String(exported.traceBytes!!, StandardCharsets.UTF_8))
        assertFalse(commands[2].contains("rm -f"))
        assertTrue(commands.any { it.startsWith("base64 ") })
    }

    @Test
    fun reportsUnavailableWhenPerfettoLaunchOrReadinessFails() {
        val launchFailure = PerfettoTrace.start {
            RootAppProcessLauncher.ShellResult(1, "permission denied")
        }
        val readinessFailure = PerfettoTrace.start(object : PerfettoTrace.ShellRunner {
            private var calls = 0

            override fun run(command: String): RootAppProcessLauncher.ShellResult {
                calls++
                return RootAppProcessLauncher.ShellResult(
                    if (calls == 2) 2 else 0,
                    if (calls == 2) "process exited" else "",
                )
            }
        })

        assertFalse(launchFailure.available)
        assertTrue(launchFailure.note.orEmpty().contains("permission denied"))
        assertFalse(readinessFailure.available)
        assertTrue(readinessFailure.note.orEmpty().contains("did not stay running"))
    }

    @Test
    fun stopRejectsMissingOrInvalidTraceSize() {
        val started = startedWith {
            if (it.contains("available:size")) {
                RootAppProcessLauncher.ShellResult(0, "available:size=not-a-number")
            } else {
                RootAppProcessLauncher.ShellResult(0, "")
            }
        }

        val trace = started.trace!!
        val stopped = trace.stop()

        assertFalse(stopped.available)
        assertTrue(stopped.note.orEmpty().contains("size was invalid"))
        assertFalse(trace.stop().available)
    }

    @Test
    fun truncatedTraceIsDiscardedWithoutExportingBytes() {
        val commands = ArrayList<String>()
        val started = startedWith { command ->
            commands += command
            if (command.contains("available:size")) {
                RootAppProcessLauncher.ShellResult(0, "available:size=16777216,truncated=true")
            } else {
                RootAppProcessLauncher.ShellResult(0, "")
            }
        }

        val trace = started.trace!!
        val exported = trace.consumeStoppedTrace(trace.stop())

        assertTrue(exported.available)
        assertTrue(exported.truncated)
        assertEquals(0, exported.traceBytes?.size ?: 0)
        assertTrue(exported.note.orEmpty().contains("not exported"))
        assertFalse(commands.any { it.startsWith("base64 ") })
    }

    @Test
    fun exportRejectsInvalidOrMismatchedBase64Payloads() {
        val invalid = startedWith { command ->
            when {
                command.contains("available:size") -> RootAppProcessLauncher.ShellResult(
                    0,
                    "available:size=3"
                )

                command.startsWith("base64 ") -> RootAppProcessLauncher.ShellResult(0, "not base64")
                else -> RootAppProcessLauncher.ShellResult(0, "")
            }
        }
        val mismatch = startedWith { command ->
            when {
                command.contains("available:size") -> RootAppProcessLauncher.ShellResult(
                    0,
                    "available:size=4"
                )

                command.startsWith("base64 ") -> RootAppProcessLauncher.ShellResult(0, "YWJj")
                else -> RootAppProcessLauncher.ShellResult(0, "")
            }
        }

        val invalidTrace = invalid.trace!!
        val mismatchTrace = mismatch.trace!!
        assertTrue(
            invalidTrace.consumeStoppedTrace(invalidTrace.stop()).note.orEmpty().contains("invalid")
        )
        assertTrue(
            mismatchTrace.consumeStoppedTrace(mismatchTrace.stop()).note.orEmpty()
                .contains("size mismatch")
        )
    }

    private fun startedWith(runner: PerfettoTrace.ShellRunner): PerfettoTrace.StartResult {
        val started = PerfettoTrace.start(runner)
        assertTrue(started.available)
        return started
    }
}
