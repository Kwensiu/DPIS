package com.dpis.module.runtime.systemserver.diagnostics

import com.dpis.module.runtime.systemserver.HookDiagnostics
import org.junit.Assert.assertEquals
import org.junit.Test

class HookLogDispatchTest {
    @Test
    fun dispatchAndSelectionMessagesAreSentToConfiguredBridge() {
        val entry = "bridge-test-${System.nanoTime()}"
        val messages = mutableListOf<String>()
        val selectedLog =
            HookDiagnostics.buildHookMethodSelectedLog(entry, "ActivityStarter.execute()")
        HookLog.setBridgeLogger(messages::add)

        try {
            HookLog.logHookDispatch(entry, "execute")
            HookLog.logHookConstructorDispatch(entry, "LaunchActivityItem(Intent)")
            HookLog.logHookSelection(selectedLog)

            assertEquals(
                listOf(
                    "system_server hook dispatch: entry=$entry, method=execute",
                    "system_server hook dispatch: entry=$entry, constructor=LaunchActivityItem(Intent)",
                    selectedLog,
                ),
                messages,
            )
        } finally {
            HookLog.setBridgeLogger(null)
        }
    }
}
