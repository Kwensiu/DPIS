package com.dpis.module;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class FeedbackDiagnosticSummaryBuilder {
    private static final String UNKNOWN = "unknown";

    String build(
            FeedbackDiagnosticCoordinator.Request request,
            long startedAtMillis,
            long finishedAtMillis,
            long durationMs,
            boolean targetLaunchStarted,
            RootAccessProbe.Result rootAccess,
            boolean systemHooksEnabled
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("# DPIS").append('\n');
        builder.append("source: feedback-diagnostic-summary").append('\n');
        builder.append("package: ").append(request.packageName).append('\n');
        builder.append("label: ").append(valueOrUnknown(request.label)).append('\n');
        builder.append("versionName: ").append(valueOrUnknown(request.versionName)).append('\n');
        builder.append("startedAt: ").append(formatTime(startedAtMillis)).append('\n');
        builder.append("finishedAt: ").append(formatTime(finishedAtMillis)).append('\n');
        builder.append("durationMs: ").append(durationMs).append('\n');
        builder.append("targetLaunchStarted: ").append(targetLaunchStarted).append('\n');
        builder.append("rootStatus: ").append(rootStatus(rootAccess)).append('\n');
        builder.append("rootProvider: ").append(rootProvider(rootAccess)).append('\n');
        builder.append("systemHooksEnabled: ").append(systemHooksEnabled).append('\n');
        builder.append("scopeKnown: ").append(request.scopeKnown).append('\n');
        builder.append("inScope: ").append(request.inScope).append('\n');
        builder.append("dpisEnabled: ").append(request.dpisEnabled).append('\n');
        builder.append("previewFromGlobalPrefill: ")
                .append(request.previewFromGlobalPrefill)
                .append('\n');
        builder.append("viewport: ").append(formatViewport(request)).append('\n');
        builder.append("font: ").append(formatFont(request)).append('\n');
        builder.append("notes: ")
                .append(targetLaunchStarted
                        ? "Diagnostic package includes diagnostic.txt, dpis-log.txt, "
                                + "and lsposed-log.txt. Runtime evidence is collected from "
                                + "DPIS app events, runtime transport, and the LSPosed log window "
                                + "when available."
                        : "Target app launch failed or was unavailable.")
                .append('\n');
        return builder.toString();
    }

    private static String formatViewport(FeedbackDiagnosticCoordinator.Request request) {
        ViewportTargetSpec spec = request.viewportTargetSpec;
        String target;
        if (spec.isRelativeScale()) {
            target = "scale=" + spec.scalePermille() / 10 + "%";
        } else if (spec.isAbsoluteDp()) {
            target = "widthDp=" + spec.absoluteWidthDp();
        } else {
            target = "off";
        }
        return target + ", mode=" + request.viewportApplyMode;
    }

    private static String formatFont(FeedbackDiagnosticCoordinator.Request request) {
        String scale = request.fontScalePercent != null
                ? request.fontScalePercent + "%"
                : "off";
        String typeface = request.typefaceId != null ? request.typefaceId : "default";
        String hookDomains = request.fontHookDomainsRaw != null ? "custom" : "default";
        return "scale=" + scale
                + ", mode=" + request.fontApplyMode
                + ", typeface=" + typeface
                + ", hookDomains=" + hookDomains;
    }

    private static String rootStatus(RootAccessProbe.Result rootAccess) {
        RootAccessProbe.Result result = rootAccess != null
                ? rootAccess
                : RootAccessProbe.Result.unknown();
        return result.status.name().toLowerCase(Locale.ROOT);
    }

    private static String rootProvider(RootAccessProbe.Result rootAccess) {
        RootAccessProbe.Result result = rootAccess != null
                ? rootAccess
                : RootAccessProbe.Result.unknown();
        return result.provider != null && !result.provider.isBlank()
                ? result.provider
                : UNKNOWN;
    }

    private static String formatTime(long millis) {
        return new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)
                .format(new Date(millis));
    }

    private static String valueOrUnknown(String value) {
        String normalized = value != null ? value.trim() : "";
        return normalized.isEmpty() ? UNKNOWN : normalized;
    }
}
