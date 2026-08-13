package com.dpis.module.diagnostics;

import com.dpis.module.DpisLog;

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
    private static volatile BridgeSink bridgeSink;

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
        emitBridgeMessage(HOT_PATH_PREFIX
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
        emitBridgeMessage(PERFORMANCE_PREFIX + " " + valueOrDefault(message, ""));
    }

    private static void emitBridgeMessage(String message) {
        DpisLog.i(message);
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

    private static String valueOrDefault(String value, String fallback) {
        String normalized = value != null ? value.trim() : "";
        return normalized.isEmpty() ? fallback : normalized;
    }
}
