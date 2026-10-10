package com.dpis.module.diagnostics.device

import com.dpis.module.diagnostics.device.RuntimeTransport.ShellRunner

object TransportSelfTest {
    @Volatile
    private var lastStatus = Status.notStarted()

    @JvmStatic
    fun runUiTransportSelfTest(packageName: String?, shellRunner: ShellRunner?): Status {
        val transport = RuntimeTransport.statusForTest()
        if (!transport.available) {
            return Status.unavailable(transport.message?.orEmpty()).also { lastStatus = it }
        }

        val token = "ui-self-test-${System.currentTimeMillis()}"
        if (!RuntimeTransport.writeSelfTestEvent(packageName, token, shellRunner)) {
            return Status.failed("ui transport write failed", 0).also { lastStatus = it }
        }

        val snapshot = RuntimeTransport.peekSnapshot(shellRunner)
        val events = snapshot.events.orEmpty()
        val ok = snapshot.available && events.any { it?.contains(token) == true }
        return (if (ok) Status.ok(events.size) else Status.failed(
            snapshot.note?.orEmpty(),
            events.size,
        )).also { lastStatus = it }
    }

    @JvmStatic
    fun lastStatus(): Status = lastStatus

    @JvmStatic
    fun resetForTest() {
        lastStatus = Status.notStarted()
    }

    @JvmStatic
    fun hasHotPathProbe(timelineEvents: List<String>?): Boolean =
        timelineEvents?.any { it.contains("source=runtime-hotpath") } == true

    data class Status private constructor(
        val prepared: Boolean,
        val uiWriteReadOk: Boolean,
        val transportEventCount: Int,
        val message: String,
    ) {
        companion object {
            fun notStarted() = Status(false, false, 0, "runtime self-test not started")

            fun unavailable(message: String?) = Status(false, false, 0, message.orEmpty())

            fun ok(count: Int) = Status(true, true, count.coerceAtLeast(0), "ui transport write/read ok")

            fun failed(message: String?, count: Int): Status {
                val reason = if (message.isNullOrBlank()) {
                    "ui transport write/read failed"
                } else {
                    message
                }
                return Status(true, false, count.coerceAtLeast(0), reason)
            }
        }
    }
}
