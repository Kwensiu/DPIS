package com.dpis.module.diagnostics;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Emits low-volume diagnostic bridge events from injected runtime processes.
 *
 * <p>The target app domain may be unable to append to the runtime transport
 * file, so bridge logs are the durable path for proving session discovery and
 * process-local aggregate publication. Keep this class as the owner of the
 * diagnostic log prefixes so runtime entrypoints only call semantic helpers.</p>
 */
public final class FeedbackDiagnosticRuntimeBridgeEvents {
    private static final String BRIDGE_PREFIX = "DPIS ";
    private static final String SESSION_PREFIX = "DPIS_DIAG_SESSION";
    private static final String HOT_PATH_PREFIX = "DPIS_DIAG_HOTPATH";
    private static final String PERFORMANCE_PREFIX = "DPIS_DIAG_PERF";
    private static final int MAX_PENDING_HOT_PATH_EVENTS = 4096;
    private static final LinkedBlockingQueue<String> PENDING_HOT_PATH_EVENTS =
            new LinkedBlockingQueue<>(MAX_PENDING_HOT_PATH_EVENTS);
    private static final Object DISPATCHER_LOCK = new Object();
    private static final AtomicLong DROPPED_HOT_PATH_EVENTS = new AtomicLong();
    private static int dispatchingHotPathEvents;
    private static volatile BridgeSink bridgeSink;
    private static volatile Thread dispatcherThread;

    public interface BridgeSink {
        void log(String message);
    }

    private FeedbackDiagnosticRuntimeBridgeEvents() {
    }

    public static void setBridgeSink(BridgeSink sink) {
        bridgeSink = sink;
    }

    public static void emitSessionDiscovery(
            String packageName,
            String processName
    ) {
        try {
            String discovery = FeedbackDiagnosticRuntimeTransport.activeSessionDiscoveryDetail();
            if (discovery.isBlank()) {
                return;
            }
            String message = SESSION_PREFIX + " process-entry: package=" + packageName
                    + ", process=" + processName
                    + ", " + discovery;
            emitBridgeMessage(message);
        } catch (Throwable ignored) {
            // Diagnostic discovery must not affect target app startup.
        }
    }

    public static void emitHotPath(
            String categoryRoute,
            String stage,
            String routeName,
            String packageName,
            String detail
    ) {
        if (!FeedbackDiagnosticRuntimeTransport.isCaptureActive()) {
            return;
        }
        enqueueHotPathMessage(HOT_PATH_PREFIX
                + " route=" + valueOrDefault(categoryRoute, "font")
                + " stage=" + valueOrDefault(stage, "event")
                + " routeName=" + valueOrDefault(routeName, "unknown")
                + " package=" + valueOrDefault(packageName, "unknown")
                + " detail=" + valueOrDefault(detail, ""));
    }

    public static void emitPerformance(String message) {
        if (!FeedbackDiagnosticRuntimeTransport.isCaptureActive()) {
            return;
        }
        long dropped = DROPPED_HOT_PATH_EVENTS.getAndSet(0L);
        String suffix = dropped > 0L
                ? ",bridgeDroppedHotPath=" + dropped
                : "";
        emitBridgeMessage(PERFORMANCE_PREFIX + " " + valueOrDefault(message, "") + suffix);
    }

    private static void emitBridgeMessage(String message) {
        BridgeSink sink = bridgeSink;
        if (sink == null) {
            return;
        }
        try {
            sink.log(BRIDGE_PREFIX + message);
        } catch (Throwable ignored) {
            // Diagnostics must never affect hooked app behavior.
        }
    }

    private static void enqueueHotPathMessage(String message) {
        if (!PENDING_HOT_PATH_EVENTS.offer(message)) {
            DROPPED_HOT_PATH_EVENTS.incrementAndGet();
            return;
        }
        ensureDispatcher();
    }

    private static void ensureDispatcher() {
        if (dispatcherThread != null) {
            return;
        }
        synchronized (DISPATCHER_LOCK) {
            if (dispatcherThread != null) {
                return;
            }
            Thread thread = new Thread(() -> {
                while (true) {
                    try {
                        String message = PENDING_HOT_PATH_EVENTS.take();
                        synchronized (DISPATCHER_LOCK) {
                            dispatchingHotPathEvents++;
                        }
                        try {
                            emitBridgeMessage(message);
                        } finally {
                            synchronized (DISPATCHER_LOCK) {
                                dispatchingHotPathEvents--;
                                DISPATCHER_LOCK.notifyAll();
                            }
                        }
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (Throwable ignored) {
                        // A bridge failure must never affect the target process.
                    }
                }
            }, "DPIS-diagnostic-bridge");
            thread.setDaemon(true);
            dispatcherThread = thread;
            thread.start();
        }
    }

    static void flushForTest() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2L);
        synchronized (DISPATCHER_LOCK) {
            while ((!PENDING_HOT_PATH_EVENTS.isEmpty() || dispatchingHotPathEvents > 0)
                    && System.nanoTime() < deadline) {
                try {
                    DISPATCHER_LOCK.wait(10L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static String valueOrDefault(String value, String fallback) {
        String normalized = value != null ? value.trim() : "";
        return normalized.isEmpty() ? fallback : normalized;
    }
}
