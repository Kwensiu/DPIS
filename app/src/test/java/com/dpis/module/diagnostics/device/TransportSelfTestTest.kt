package com.dpis.module.diagnostics.device

import com.dpis.module.root.RootAppProcessLauncher
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportSelfTestTest {
    @After
    fun tearDown() {
        RuntimeTransport.cancel { RootAppProcessLauncher.ShellResult(0, "") }
        TransportSelfTest.resetForTest()
    }

    @Test
    fun uiTransportSelfTestSucceedsWhenItsProbeCanBeReadBack() {
        val shell = EchoShell()
        RuntimeTransport.start("com.example.app", shell::run)

        val status = TransportSelfTest.runUiTransportSelfTest("com.example.app", shell::run)

        assertTrue(status.prepared)
        assertTrue(status.uiWriteReadOk)
        assertTrue(status.transportEventCount > 0)
        assertTrue(status.message.contains("ok"))
    }

    @Test
    fun uiTransportSelfTestFailsWhenItsProbeCannotBeReadBack() {
        val shell = EmptyReadShell()
        RuntimeTransport.start("com.example.app", shell::run)

        val status = TransportSelfTest.runUiTransportSelfTest("com.example.app", shell::run)

        assertTrue(status.prepared)
        assertFalse(status.uiWriteReadOk)
    }

    private class EchoShell {
        private var lastAppendedLine = ""

        fun run(command: String): RootAppProcessLauncher.ShellResult {
            if (command.startsWith("printf %s")) {
                lastAppendedLine = command
                return RootAppProcessLauncher.ShellResult(0, "")
            }
            if (command.startsWith("cat ")) {
                return RootAppProcessLauncher.ShellResult(0, extractJson(lastAppendedLine) + "\n")
            }
            return RootAppProcessLauncher.ShellResult(0, "")
        }

        private fun extractJson(command: String): String {
            val firstQuote = command.indexOf('\'')
            val secondQuote = command.indexOf('\'', firstQuote + 1)
            if (firstQuote < 0 || secondQuote <= firstQuote) return ""
            return command.substring(firstQuote + 1, secondQuote)
        }
    }

    private class EmptyReadShell {
        fun run(command: String) = RootAppProcessLauncher.ShellResult(0, "")
    }
}
