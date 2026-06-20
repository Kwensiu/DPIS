package com.dpis.module;

import android.os.Handler;
import android.os.Looper;

final class FeedbackDiagnosticCoordinator {
    static final long DEFAULT_DURATION_MS = 10_000L;

    interface Host {
        boolean launchTargetApp(String packageName);

        void bringDpisToFront();

        RootAccessProbe.Result rootAccess();

        boolean systemHooksEnabled();

        long currentTimeMillis();

        void onFeedbackDiagnosticFinished(Result result);
    }

    static final class Request {
        final String packageName;
        final String label;
        final boolean scopeKnown;
        final boolean inScope;
        final boolean dpisEnabled;
        final boolean previewFromGlobalPrefill;
        final ViewportTargetSpec viewportTargetSpec;
        final String viewportApplyMode;
        final Integer fontScalePercent;
        final String fontApplyMode;
        final String typefaceId;
        final String fontHookDomainsRaw;

        private Request(
                String packageName,
                String label,
                boolean scopeKnown,
                boolean inScope,
                boolean dpisEnabled,
                boolean previewFromGlobalPrefill,
                ViewportTargetSpec viewportTargetSpec,
                String viewportApplyMode,
                Integer fontScalePercent,
                String fontApplyMode,
                String typefaceId,
                String fontHookDomainsRaw
        ) {
            this.packageName = valueOrEmpty(packageName);
            this.label = valueOrEmpty(label);
            this.scopeKnown = scopeKnown;
            this.inScope = inScope;
            this.dpisEnabled = dpisEnabled;
            this.previewFromGlobalPrefill = previewFromGlobalPrefill;
            this.viewportTargetSpec = viewportTargetSpec != null
                    ? viewportTargetSpec
                    : ViewportTargetSpec.off();
            this.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode);
            this.fontScalePercent = fontScalePercent;
            this.fontApplyMode = FontApplyMode.normalize(fontApplyMode);
            this.typefaceId = normalizeNullableString(typefaceId);
            this.fontHookDomainsRaw = normalizeNullableString(fontHookDomainsRaw);
        }

        static Request from(
                AppListItem item,
                AppConfigDialogBinder.AppConfigDialogState state
        ) {
            boolean useState = state != null;
            return new Request(
                    useState && !valueOrEmpty(state.packageName).isBlank()
                            ? state.packageName
                            : item.packageName,
                    item.label,
                    useState ? state.scopeKnown : item.scopeKnown,
                    useState ? state.scopeSelected : item.inScope,
                    useState ? state.dpisEnabled : item.dpisEnabled,
                    useState ? state.previewFromGlobalPrefill : item.previewFromGlobalPrefill,
                    item.viewportTargetSpec,
                    useState ? state.viewportApplyMode : item.viewportMode,
                    item.fontScalePercent,
                    item.fontMode,
                    useState ? state.selectedTypefaceId : item.typefaceId,
                    useState ? state.draftFontHookDomainsRaw : item.previewFontHookDomainsRaw
            );
        }

        boolean isValid() {
            return !packageName.isBlank();
        }
    }

    static final class Result {
        final Request request;
        final long startedAtMillis;
        final long finishedAtMillis;
        final long durationMs;
        final boolean targetLaunchStarted;
        final RootAccessProbe.Result rootAccess;
        final boolean systemHooksEnabled;
        final String summary;

        private Result(
                Request request,
                long startedAtMillis,
                long finishedAtMillis,
                long durationMs,
                boolean targetLaunchStarted,
                RootAccessProbe.Result rootAccess,
                boolean systemHooksEnabled,
                String summary
        ) {
            this.request = request;
            this.startedAtMillis = startedAtMillis;
            this.finishedAtMillis = finishedAtMillis;
            this.durationMs = durationMs;
            this.targetLaunchStarted = targetLaunchStarted;
            this.rootAccess = rootAccess != null ? rootAccess : RootAccessProbe.Result.unknown();
            this.systemHooksEnabled = systemHooksEnabled;
            this.summary = summary != null ? summary : "";
        }
    }

    private final Host host;
    private final Handler handler;
    private final FeedbackDiagnosticSummaryBuilder summaryBuilder;
    private boolean running;

    FeedbackDiagnosticCoordinator(Host host) {
        this(host, new Handler(Looper.getMainLooper()), new FeedbackDiagnosticSummaryBuilder());
    }

    FeedbackDiagnosticCoordinator(
            Host host,
            Handler handler,
            FeedbackDiagnosticSummaryBuilder summaryBuilder
    ) {
        this.host = host;
        this.handler = handler;
        this.summaryBuilder = summaryBuilder;
    }

    boolean start(Request request) {
        if (running || host == null || request == null || !request.isValid()) {
            return false;
        }
        running = true;
        long startedAt = host.currentTimeMillis();
        boolean launched = host.launchTargetApp(request.packageName);
        handler.postDelayed(
                () -> finish(request, startedAt, launched),
                launched ? DEFAULT_DURATION_MS : 0L
        );
        return true;
    }

    boolean isRunning() {
        return running;
    }

    void cancel() {
        running = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void finish(Request request, long startedAt, boolean launched) {
        if (!running) {
            return;
        }
        running = false;
        long finishedAt = host.currentTimeMillis();
        long durationMs = Math.max(0L, finishedAt - startedAt);
        RootAccessProbe.Result rootAccess = host.rootAccess();
        boolean systemHooksEnabled = host.systemHooksEnabled();
        Result result = new Result(
                request,
                startedAt,
                finishedAt,
                durationMs,
                launched,
                rootAccess,
                systemHooksEnabled,
                summaryBuilder.build(
                        request,
                        startedAt,
                        finishedAt,
                        durationMs,
                        launched,
                        rootAccess,
                        systemHooksEnabled
                )
        );
        host.bringDpisToFront();
        host.onFeedbackDiagnosticFinished(result);
    }

    private static String valueOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }

    private static String normalizeNullableString(String value) {
        String normalized = valueOrEmpty(value);
        return normalized.isEmpty() ? null : normalized;
    }
}
