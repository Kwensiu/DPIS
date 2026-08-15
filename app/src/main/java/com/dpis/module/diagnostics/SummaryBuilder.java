package com.dpis.module.diagnostics;

import com.dpis.module.root.RootAccessProbe;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class SummaryBuilder {
    private static final String UNKNOWN = "unknown";

    public String build(
            Input input,
            long startedAtMillis,
            long finishedAtMillis,
            long durationMs,
            boolean targetLaunchStarted,
            RootAccessProbe.Result rootAccess,
            boolean systemHooksEnabled
    ) {
        Input request = input != null ? input : Input.empty();
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
                        ? "Diagnostic package includes diagnostic.txt, timeline.tsv, "
                                + "module-effects.tsv, dpis-log.txt, and lsposed-log.txt. "
                                + "Runtime evidence is collected from DPIS app events, "
                                + "runtime transport, and the LSPosed log window when available."
                        : "Target app launch failed or was unavailable.")
                .append('\n');
        return builder.toString();
    }

    private static String formatViewport(Input request) {
        return request.viewportSummary + ", mode=" + request.viewportApplyMode;
    }

    private static String formatFont(Input request) {
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

    public static final class Input {
        public final String packageName;
        public final String label;
        public final String versionName;
        public final boolean scopeKnown;
        public final boolean inScope;
        public final boolean dpisEnabled;
        public final boolean previewFromGlobalPrefill;
        public final String viewportSummary;
        public final String viewportApplyMode;
        public final Integer fontScalePercent;
        public final String fontApplyMode;
        public final String typefaceId;
        public final String fontHookDomainsRaw;

        public Input(
                String packageName,
                String label,
                String versionName,
                boolean scopeKnown,
                boolean inScope,
                boolean dpisEnabled,
                boolean previewFromGlobalPrefill,
                String viewportSummary,
                String viewportApplyMode,
                Integer fontScalePercent,
                String fontApplyMode,
                String typefaceId,
                String fontHookDomainsRaw
        ) {
            this.packageName = valueOrUnknown(packageName);
            this.label = label;
            this.versionName = versionName;
            this.scopeKnown = scopeKnown;
            this.inScope = inScope;
            this.dpisEnabled = dpisEnabled;
            this.previewFromGlobalPrefill = previewFromGlobalPrefill;
            this.viewportSummary = valueOrUnknown(viewportSummary);
            this.viewportApplyMode = valueOrUnknown(viewportApplyMode);
            this.fontScalePercent = fontScalePercent;
            this.fontApplyMode = valueOrUnknown(fontApplyMode);
            this.typefaceId = typefaceId;
            this.fontHookDomainsRaw = fontHookDomainsRaw;
        }

        private static Input empty() {
            return new Input(
                    UNKNOWN,
                    UNKNOWN,
                    UNKNOWN,
                    false,
                    false,
                    false,
                    false,
                    "off",
                    UNKNOWN,
                    null,
                    UNKNOWN,
                    null,
                    null
            );
        }
    }
}
