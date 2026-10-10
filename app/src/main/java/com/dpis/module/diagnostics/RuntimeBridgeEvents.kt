package com.dpis.module.diagnostics

import com.dpis.module.diagnostics.device.RuntimeTransport
import java.util.ArrayDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Emits low-volume diagnostic bridge events from injected runtime processes.
 *
 * The target app domain may be unable to append to the runtime transport
 * file, so bridge logs are the durable path for proving session discovery and
 * process-local aggregate publication. This object owns the diagnostic log
 * prefixes so runtime entrypoints only call semantic helpers.
 */
object RuntimeBridgeEvents {
    private const val BRIDGE_PREFIX = "DPIS "
    private const val SESSION_PREFIX = "DPIS_DIAG_SESSION"
    private const val HOT_PATH_PREFIX = "DPIS_DIAG_HOTPATH"
    private const val PERFORMANCE_PREFIX = "DPIS_DIAG_PERF"
    private const val MAX_PENDING_HOT_PATH_EVENTS = 4096
    private const val MAX_RETAINED_APPLIED_EVENTS = 16

    private sealed interface PendingHotPathEvent {
        data class Message(val value: String) : PendingHotPathEvent
        class Flush(val completed: CountDownLatch) : PendingHotPathEvent
    }

    private val pendingHotPathEvents =
        LinkedBlockingQueue<PendingHotPathEvent>(MAX_PENDING_HOT_PATH_EVENTS)
    private val retainedAppliedEvents = ArrayDeque<String>()
    private val dispatcherLock = Object()
    private val droppedHotPathEvents = AtomicLong()

    @Volatile
    private var bridgeSink: BridgeSink? = null

    @Volatile
    private var dispatcherThread: Thread? = null

    private var dispatchingHotPathEvents = 0

    fun interface BridgeSink {
        fun log(message: String)
    }

    @JvmStatic
    fun setBridgeSink(sink: BridgeSink?) {
        bridgeSink = sink
        if (sink == null) {
            clearRetainedApplied()
            return
        }
        flushRetainedApplied()
    }

    @JvmStatic
    fun emitSessionDiscovery(packageName: String?, processName: String?) {
        try {
            val discovery = RuntimeTransport.activeSessionDiscoveryDetail()
            if (discovery.isBlank()) {
                return
            }
            emitBridgeMessage(
                "$SESSION_PREFIX process-entry: package=$packageName, process=$processName, $discovery",
            )
        } catch (_: Throwable) {
            // Diagnostic discovery must not affect target app startup.
        } finally {
            flushRetainedApplied()
        }
    }

    @JvmStatic
    fun emitHotPath(
        categoryRoute: String?,
        stage: String?,
        routeName: String?,
        packageName: String?,
        detail: String?,
    ) {
        val message = HOT_PATH_PREFIX +
                " route=" + valueOrDefault(categoryRoute, "font") +
                " stage=" + valueOrDefault(stage, "event") +
                " routeName=" + valueOrDefault(routeName, "unknown") +
                " package=" + valueOrDefault(packageName, "unknown") +
                " detail=" + valueOrDefault(detail, "")
        if (!canDeliverHotPath()) {
            // Display metrics can mutate before the bridge sink or capture
            // session exists. Keep that one applied record so the density
            // before/after still reaches the diagnostic pack.
            if (stage == "applied") {
                retainApplied("$message, delivery=retained-until-bridge")
            }
            return
        }
        flushRetainedApplied()
        enqueueHotPathMessage(message)
    }

    @JvmStatic
    fun emitPerformance(message: String?) {
        if (!RuntimeTransport.isCaptureActive) {
            return
        }
        val dropped = droppedHotPathEvents.getAndSet(0L)
        val suffix = if (dropped > 0L) ",bridgeDroppedHotPath=$dropped" else ""
        emitBridgeMessage(PERFORMANCE_PREFIX + " " + valueOrDefault(message, "") + suffix)
    }

    private fun emitBridgeMessage(message: String) {
        val sink = bridgeSink ?: return
        try {
            sink.log(BRIDGE_PREFIX + message)
        } catch (_: Throwable) {
            // Diagnostics must never affect hooked app behavior.
        }
    }

    private fun enqueueHotPathMessage(message: String) {
        if (!pendingHotPathEvents.offer(PendingHotPathEvent.Message(message))) {
            droppedHotPathEvents.incrementAndGet()
            return
        }
        ensureDispatcher()
    }

    private fun canDeliverHotPath(): Boolean {
        return bridgeSink != null && RuntimeTransport.isCaptureActive
    }

    private fun retainApplied(message: String) {
        synchronized(retainedAppliedEvents) {
            retainedAppliedEvents.remove(message)
            retainedAppliedEvents.addLast(message)
            while (retainedAppliedEvents.size > MAX_RETAINED_APPLIED_EVENTS) {
                retainedAppliedEvents.removeFirst()
            }
        }
    }

    private fun clearRetainedApplied() {
        synchronized(retainedAppliedEvents) {
            retainedAppliedEvents.clear()
        }
    }

    private fun flushRetainedApplied() {
        if (!canDeliverHotPath()) {
            return
        }
        val pending = synchronized(retainedAppliedEvents) {
            if (retainedAppliedEvents.isEmpty()) {
                return
            }
            val copy = ArrayList(retainedAppliedEvents)
            retainedAppliedEvents.clear()
            copy
        }
        for (message in pending) {
            if (!pendingHotPathEvents.offer(PendingHotPathEvent.Message(message))) {
                retainApplied(message)
                droppedHotPathEvents.incrementAndGet()
                return
            }
        }
        ensureDispatcher()
    }

    private fun ensureDispatcher() {
        if (dispatcherThread != null) {
            return
        }
        synchronized(dispatcherLock) {
            if (dispatcherThread != null) {
                return
            }
            val thread = Thread({
                while (true) {
                    try {
                        val message = when (val event = pendingHotPathEvents.take()) {
                            is PendingHotPathEvent.Flush -> {
                                event.completed.countDown()
                                continue
                            }

                            is PendingHotPathEvent.Message -> event.value
                        }
                        synchronized(dispatcherLock) {
                            dispatchingHotPathEvents++
                        }
                        try {
                            emitBridgeMessage(message)
                        } finally {
                            synchronized(dispatcherLock) {
                                dispatchingHotPathEvents--
                                dispatcherLock.notifyAll()
                            }
                        }
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return@Thread
                    } catch (_: Throwable) {
                        // A bridge failure must never affect the target process.
                    }
                }
            }, "DPIS-diagnostic-bridge")
            thread.isDaemon = true
            dispatcherThread = thread
            thread.start()
        }
    }

    @JvmStatic
    internal fun flushForTest() {
        val completed = CountDownLatch(1)
        if (!pendingHotPathEvents.offer(PendingHotPathEvent.Flush(completed))) {
            return
        }
        ensureDispatcher()
        try {
            completed.await(2L, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun valueOrDefault(value: String?, fallback: String): String {
        val normalized = value?.trim().orEmpty()
        return if (normalized.isEmpty()) fallback else normalized
    }
}
