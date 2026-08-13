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

    public interface BridgeSink {
        void log(String message);
    }

    private FeedbackDiagnosticRuntimeBridgeEvents() {
    }

    public static void emitSessionDiscovery(
            String packageName,
            String processName,
            BridgeSink bridgeSink
    ) {
        try {
            String discovery = FeedbackDiagnosticRuntimeTransport.activeSessionDiscoveryDetail();
            if (discovery.isBlank()) {
                return;
            }
            String message = SESSION_PREFIX + " process-entry: package=" + packageName
                    + ", process=" + processName
                    + ", " + discovery;
            DpisLog.i(message);
            if (bridgeSink != null) {
                bridgeSink.log(BRIDGE_PREFIX + message);
            }
        } catch (Throwable ignored) {
            // Diagnostic discovery must not affect target app startup.
        }
    }
}
