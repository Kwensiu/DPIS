package com.dpis.module;

import android.os.Handler;
import android.os.Looper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class FeedbackDiagnosticCoordinator {
    private static final long FOREGROUND_CHECK_INTERVAL_MS = 1_000L;

    interface Host {
        boolean restartTargetAppForDiagnostic(String packageName);

        String dpisPackageName();

        RootAccessProbe.Result rootAccess();

        boolean systemHooksEnabled();

        long currentTimeMillis();

        void onFeedbackDiagnosticStarted();

        void onFeedbackDiagnosticUnavailable();

        void onFeedbackDiagnosticRootRequired();

        void onFeedbackDiagnosticFinished(Result result);
    }

    static final class Request {
        final String packageName;
        final String label;
        final String versionName;
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
        final Integer wechatDpi;

        Request(
                String packageName,
                String label,
                String versionName,
                boolean scopeKnown,
                boolean inScope,
                boolean dpisEnabled,
                boolean previewFromGlobalPrefill,
                ViewportTargetSpec viewportTargetSpec,
                String viewportApplyMode,
                Integer fontScalePercent,
                String fontApplyMode,
                String typefaceId,
                String fontHookDomainsRaw,
                Integer wechatDpi
        ) {
            this.packageName = valueOrEmpty(packageName);
            this.label = valueOrEmpty(label);
            this.versionName = valueOrEmpty(versionName);
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
            this.wechatDpi = wechatDpi;
        }

        static Request from(
                AppListItem item,
                AppConfigDialogBinder.AppConfigDialogState state
        ) {
            return from(item, state, "");
        }

        static Request from(
                AppListItem item,
                AppConfigDialogBinder.AppConfigDialogState state,
                String versionName
        ) {
            boolean useState = state != null;
            return new Request(
                    useState && !valueOrEmpty(state.packageName).isBlank()
                            ? state.packageName
                            : item.packageName,
                    item.label,
                    versionName,
                    useState ? state.scopeKnown : item.scopeKnown,
                    useState ? state.scopeSelected : item.inScope,
                    useState ? state.dpisEnabled : item.dpisEnabled,
                    useState ? state.previewFromGlobalPrefill : item.previewFromGlobalPrefill,
                    item.viewportTargetSpec,
                    useState ? state.viewportApplyMode : item.viewportMode,
                    item.fontScalePercent,
                    item.fontMode,
                    useState ? state.selectedTypefaceId : item.typefaceId,
                    useState ? state.draftFontHookDomainsRaw : item.previewFontHookDomainsRaw,
                    item.wechatDpi
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
        final List<String> timelineEvents;

        Result(
                Request request,
                long startedAtMillis,
                long finishedAtMillis,
                long durationMs,
                boolean targetLaunchStarted,
                RootAccessProbe.Result rootAccess,
                boolean systemHooksEnabled,
                String summary,
                List<String> timelineEvents
        ) {
            this.request = request;
            this.startedAtMillis = startedAtMillis;
            this.finishedAtMillis = finishedAtMillis;
            this.durationMs = durationMs;
            this.targetLaunchStarted = targetLaunchStarted;
            this.rootAccess = rootAccess != null ? rootAccess : RootAccessProbe.Result.unknown();
            this.systemHooksEnabled = systemHooksEnabled;
            this.summary = summary != null ? summary : "";
            this.timelineEvents = timelineEvents != null
                    ? new ArrayList<>(timelineEvents)
                    : new ArrayList<>();
        }
    }

    private final Host host;
    private final Handler handler;
    private final FeedbackDiagnosticSummaryBuilder summaryBuilder;
    private final ExecutorService executor;
    private boolean running;
    private Request runningRequest;
    private long runningStartedAtMillis;
    private boolean runningTargetLaunchStarted;
    private String lastObservedForegroundPackage;
    private final List<String> runningTimelineEvents = new ArrayList<>();

    FeedbackDiagnosticCoordinator(Host host) {
        this(
                host,
                new Handler(Looper.getMainLooper()),
                Executors.newSingleThreadExecutor(),
                new FeedbackDiagnosticSummaryBuilder()
        );
    }

    FeedbackDiagnosticCoordinator(
            Host host,
            Handler handler,
            ExecutorService executor,
            FeedbackDiagnosticSummaryBuilder summaryBuilder
    ) {
        this.host = host;
        this.handler = handler;
        this.executor = executor;
        this.summaryBuilder = summaryBuilder;
    }

    boolean start(Request request) {
        if (running || host == null || request == null || !request.isValid()) {
            return false;
        }
        running = true;
        runningRequest = request;
        runningTimelineEvents.clear();
        recordTimelineEvent("session requested");
        executor.execute(() -> {
            RootAccessProbe.Result rootAccess = ensureRootAccess();
            if (rootAccess.status != RootAccessProbe.Status.AVAILABLE) {
                handler.post(() -> failForMissingRoot(request));
                return;
            }
            recordTimelineEvent("root available: " + rootProvider(rootAccess));
            FeedbackDiagnosticRuntimeTransport.Status transportStatus =
                    FeedbackDiagnosticRuntimeTransport.start(request.packageName, null);
            recordTimelineEvent(transportStatus.available
                    ? "runtime transport prepared"
                    : transportStatus.message);
            FeedbackDiagnosticRuntimeSelfTest.Status selfTest =
                    FeedbackDiagnosticRuntimeSelfTest.runUiTransportSelfTest(
                            request.packageName,
                            null
                    );
            recordTimelineEvent(selfTest.uiWriteReadOk
                    ? "runtime transport self-test ok"
                    : "runtime transport self-test failed: " + selfTest.message);
            recordTimelineEvent("root force-stop/start requested");
            boolean launched = host.restartTargetAppForDiagnostic(request.packageName);
            recordTimelineEvent(launched
                    ? "root force-stop/start succeeded"
                    : "root force-stop/start failed");
            handler.post(() -> startAfterRootLaunch(request, launched));
        });
        return true;
    }

    boolean isRunning() {
        return running;
    }

    void cancel() {
        FeedbackDiagnosticRuntimeEvents.cancel();
        FeedbackDiagnosticRuntimeTransport.cancel(null);
        clearRunningState();
        handler.removeCallbacksAndMessages(null);
    }

    void shutdown() {
        cancel();
        executor.shutdownNow();
    }

    void onDpisResumed() {
        if (running && runningTargetLaunchStarted) {
            finish();
        }
    }

    private void failForMissingRoot(Request request) {
        if (!running || runningRequest != request) {
            return;
        }
        clearRunningState();
        host.onFeedbackDiagnosticRootRequired();
    }

    private void startAfterRootLaunch(Request request, boolean launched) {
        if (!running || runningRequest != request) {
            return;
        }
        if (!launched) {
            FeedbackDiagnosticRuntimeEvents.cancel();
            FeedbackDiagnosticRuntimeTransport.cancel(null);
            clearRunningState();
            host.onFeedbackDiagnosticUnavailable();
            return;
        }
        runningStartedAtMillis = host.currentTimeMillis();
        FeedbackDiagnosticRuntimeEvents.start(request.packageName, request);
        recordTimelineEvent("session started");
        recordTimelineEvent("app config resolved");
        DpisLog.i("feedback diagnostic session started: package="
                + request.packageName
                + ", versionName="
                + valueOrEmpty(request.versionName));
        runningTargetLaunchStarted = true;
        lastObservedForegroundPackage = request.packageName;
        host.onFeedbackDiagnosticStarted();
        scheduleForegroundCheck();
    }

    private RootAccessProbe.Result ensureRootAccess() {
        RootAccessProbe.Result rootAccess = host.rootAccess();
        if (rootAccess.status == RootAccessProbe.Status.UNKNOWN) {
            return RootAccessProbe.probe();
        }
        return rootAccess;
    }

    private void scheduleForegroundCheck() {
        handler.postDelayed(this::checkForegroundPackage, FOREGROUND_CHECK_INTERVAL_MS);
    }

    private void checkForegroundPackage() {
        if (!running) {
            return;
        }
        executor.execute(() -> {
            String packageName = FeedbackDiagnosticForegroundAppReader.readForegroundPackage();
            handler.post(() -> handleForegroundPackage(packageName));
        });
    }

    private void handleForegroundPackage(String packageName) {
        if (!running) {
            return;
        }
        if (samePackage(packageName, runningRequest.packageName)) {
            scheduleForegroundCheck();
            return;
        }
        if (samePackage(packageName, host.dpisPackageName())) {
            recordTimelineEvent("foreground returned to DPIS");
            finish();
            return;
        }
        if (packageName != null && !packageName.isBlank()) {
            if (!samePackage(packageName, lastObservedForegroundPackage)) {
                recordTimelineEvent("foreground changed to " + packageName);
                lastObservedForegroundPackage = packageName;
            }
        }
        scheduleForegroundCheck();
    }

    private void finish() {
        if (!running || runningRequest == null) {
            return;
        }
        Request request = runningRequest;
        long startedAt = runningStartedAtMillis;
        boolean launched = runningTargetLaunchStarted;
        recordTimelineEvent("session finished");
        List<String> runtimeEvents = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        FeedbackDiagnosticRuntimeTransport.Snapshot transportSnapshot =
                FeedbackDiagnosticRuntimeTransport.stopSnapshot(null);
        List<String> timelineEvents = new ArrayList<>(runningTimelineEvents);
        timelineEvents.addAll(runtimeEvents);
        timelineEvents.addAll(transportSnapshot.events);
        if (!transportSnapshot.available || transportSnapshot.events.isEmpty()) {
            String note = !transportSnapshot.note.isBlank()
                    ? transportSnapshot.note
                    : "runtime transport empty";
            timelineEvents.add(formatTime(host.currentTimeMillis())
                    + " source=runtime-transport stage=transport_note message=" + note);
        }
        timelineEvents.sort(String::compareTo);
        clearRunningState();
        handler.removeCallbacksAndMessages(null);
        long finishedAt = host.currentTimeMillis();
        long durationMs = Math.max(0L, finishedAt - startedAt);
        RootAccessProbe.Result rootAccess = host.rootAccess();
        boolean systemHooksEnabled = host.systemHooksEnabled();
        DpisLog.i("feedback diagnostic session finished: package="
                + request.packageName
                + ", durationMs="
                + durationMs);
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
                ),
                timelineEvents
        );
        host.onFeedbackDiagnosticFinished(result);
    }

    private void clearRunningState() {
        running = false;
        runningRequest = null;
        runningStartedAtMillis = 0L;
        runningTargetLaunchStarted = false;
        lastObservedForegroundPackage = null;
        runningTimelineEvents.clear();
    }

    private static boolean samePackage(String first, String second) {
        return valueOrEmpty(first).equals(valueOrEmpty(second));
    }

    private void recordTimelineEvent(String event) {
        String normalized = valueOrEmpty(event);
        if (!normalized.isEmpty()) {
            runningTimelineEvents.add(formatTime(host.currentTimeMillis()) + " " + normalized);
        }
    }

    private static String rootProvider(RootAccessProbe.Result rootAccess) {
        if (rootAccess == null || rootAccess.provider == null || rootAccess.provider.isBlank()) {
            return "unknown";
        }
        return rootAccess.provider;
    }

    private static String formatTime(long millis) {
        return new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
                .format(new Date(millis));
    }

    private static String valueOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }

    private static String normalizeNullableString(String value) {
        String normalized = valueOrEmpty(value);
        return normalized.isEmpty() ? null : normalized;
    }
}
