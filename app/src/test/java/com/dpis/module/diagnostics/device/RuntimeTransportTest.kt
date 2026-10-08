package com.dpis.module.diagnostics.device

import com.dpis.module.root.RootAppProcessLauncher
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuntimeTransportTest {
    @Before
    fun setUp() {
        RuntimeTransport.cancel { RootAppProcessLauncher.ShellResult(0, "") }
    }

    @After
    fun tearDown() {
        RuntimeTransport.cancel { RootAppProcessLauncher.ShellResult(0, "") }
    }

    @Test
    fun defaultClosedTransportReportsUnavailable() {
        RuntimeTransport.record(
            "runtime",
            "dpis_log",
            "com.example.app",
            "target app matched: package=com.example.app",
        )

        val status = RuntimeTransport.statusForTest()

        assertFalse(status.available)
        assertTrue(status.message.contains("not started"))
    }

    @Test
    fun startFailureReturnsUnavailableStatus() {
        val status = RuntimeTransport.start("com.example.app") {
            RootAppProcessLauncher.ShellResult(1, "permission denied")
        }

        assertFalse(status.available)
        assertTrue(status.message.contains("permission denied"))
    }

    @Test
    fun enabledTransportCommandsCreateMarkerAndReadEvents() {
        val shell = FakeShell(
            "{\"timestampMillis\":1700000000100," +
                    "\"displayTime\":\"11-14 22:13:20.100\", " +
                    "\"source\":\"runtime-transport\", " +
                    "\"category\":\"runtime\", " +
                    "\"route\":\"font\", " +
                    "\"routeName\":\"textview_sp_rewrite\", " +
                    "\"stage\":\"dpis_log\", " +
                    "\"package\":\"com.example.app\", " +
                    "\"message\":\"target app matched: package=com.example.app\"}\n",
        )

        val status = RuntimeTransport.start("com.example.app", shell::run)
        val snapshot = RuntimeTransport.stopSnapshot(shell::run)

        assertTrue(status.available)
        assertTrue(shell.commands[0].contains("chmod 666"))
        assertTrue(shell.commands[0].contains("active-session"))
        assertTrue(snapshot.available)
        assertTrue(snapshot.events[0].orEmpty().contains("source=runtime-transport"))
        assertTrue(snapshot.events[0].orEmpty().contains("route=font"))
        assertTrue(snapshot.events[0].orEmpty().contains("routeName=textview_sp_rewrite"))
        assertTrue(snapshot.events[0].orEmpty().contains("stage=dpis_log"))
    }

    @Test
    fun activeSessionDiscoveryReportsLocalSession() {
        val shell = FakeShell("")
        RuntimeTransport.start("com.example.app", shell::run)

        assertTrue(RuntimeTransport.activeSessionDiscoveryDetail().contains("source=local-session"))
    }

    private class FakeShell(private val readOutput: String) {
        val commands = ArrayList<String>()

        fun run(command: String): RootAppProcessLauncher.ShellResult {
            commands += command
            return if (command.startsWith("cat ")) {
                RootAppProcessLauncher.ShellResult(0, readOutput)
            } else {
                RootAppProcessLauncher.ShellResult(0, "")
            }
        }
    }
}
