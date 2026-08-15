package com.dpis.module.diagnostics;

import com.dpis.module.DpisConfigStore;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.*;

import com.dpis.module.appconfig.AppConfigDialogBinder;
import com.dpis.module.appconfig.AppConfigInputValidation;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import com.dpis.module.applist.AppListItem;

import android.os.Handler;
import android.os.Looper;

import com.dpis.module.root.RootAccessProbe;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DiagnosticCoordinator {
    private static final long FOREGROUND_CHECK_INTERVAL_MS = 1_000L;

    public interface Host {
        boolean restartTargetAppForDiagnostic(String packageName);

        String dpisPackageName();

        RootAccessProbe.Result rootAccess();

        boolean systemHooksEnabled();

        long currentTimeMillis();

        void onFeedbackDiagnosticStarted();

        void onFeedbackDiagnosticUnavailable();

        void onFeedbackDiagnosticRootRequired();

        void onFeedbackDiagnosticFinished(Result result);

        default void onFeedbackDiagnosticAutoFinished() {
        }
    }

    public static final class Request {
        public final String packageName;
        public final String label;
        public final String versionName;
        public final boolean scopeKnown;
        public final boolean inScope;
        public final boolean dpisEnabled;
        public final boolean previewFromGlobalPrefill;
        public final ViewportTargetSpec viewportTargetSpec;
        public final String viewportApplyMode;
        public final Integer fontScalePercent;
        public final String fontApplyMode;
        public final String typefaceId;
        public final String fontHookDomainsRaw;
        public final Integer wechatDpi;

        public Request(
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

        public static Request from(
                AppListItem item,
                AppConfigDialogBinder.AppConfigDialogState state
        ) {
            return from(item, state, "");
        }

        public static Request from(
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
                    useState ? state.draftFontHookDomainsRaw : item.effectiveFontHookDomainsRaw(),
                    item.wechatDpi
            );
        }

        public static Request fromPersisted(
                AppListItem item,
                AppConfigDialogBinder.AppConfigDialogState state,
                String versionName,
                DpisConfigStore store
        ) {
            String statePackageName = state != null ? valueOrEmpty(state.packageName) : "";
            String packageName = !statePackageName.isBlank()
                    ? statePackageName
                    : (item != null ? item.packageName : "");
            if (store == null || packageName.isBlank() || item == null) {
                return from(item, state, versionName);
            }
            ViewportTargetSpec persistedViewportSpec = store.getTargetViewportSpec(packageName);
            String persistedViewportMode = store.getTargetViewportApplyMode(packageName);
            Integer persistedFontScale = store.getTargetFontScalePercent(packageName);
            String persistedFontMode = store.getTargetFontApplyMode(packageName);
            String persistedTypefaceId = store.getTargetTypefaceId(packageName);
            String persistedHookDomains = store.getTargetFontHookDomainsRaw(packageName);
            Integer persistedWechatDpi = store.getWechatDpi(packageName);
            // Diagnostic plan text should describe the just-persisted package config.
            // The app-list row is only a pre-save snapshot and can lag behind the editor.
            return new Request(
                    packageName,
                    item.label,
                    versionName,
                    state != null ? state.scopeKnown : item.scopeKnown,
                    state != null ? state.scopeSelected : item.inScope,
                    store.isTargetDpisEnabled(packageName),
                    false,
                    persistedViewportSpec,
                    persistedViewportMode,
                    persistedFontScale,
                    persistedFontMode,
                    persistedTypefaceId,
                    persistedHookDomains,
                    persistedWechatDpi
            );
        }

        public boolean isValid() {
            return !packageName.isBlank();
        }
    }

    public static final class Result {
        public final Request request;
        public final long startedAtMillis;
        public final long finishedAtMillis;
        public final long durationMs;
        public final boolean targetLaunchStarted;
        public final RootAccessProbe.Result rootAccess;
        public final boolean systemHooksEnabled;
        public final String summary;
        public final List<String> timelineEvents;
        public final PerformanceSnapshot performanceSnapshot;
        public final boolean perfettoAvailable;
        public final long perfettoSizeBytes;
        public final boolean perfettoTruncated;
        public final String perfettoNote;
        public final byte[] perfettoTraceBytes;

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
            this(
                    request,
                    startedAtMillis,
                    finishedAtMillis,
                    durationMs,
                    targetLaunchStarted,
                    rootAccess,
                    systemHooksEnabled,
                    summary,
                    timelineEvents,
                    PerformanceSnapshot.EMPTY
            );
        }

        Result(
                Request request,
                long startedAtMillis,
                long finishedAtMillis,
                long durationMs,
                boolean targetLaunchStarted,
                RootAccessProbe.Result rootAccess,
                boolean systemHooksEnabled,
                String summary,
                List<String> timelineEvents,
                PerformanceSnapshot performanceSnapshot
        ) {
            this(
                    request,
                    startedAtMillis,
                    finishedAtMillis,
                    durationMs,
                    targetLaunchStarted,
                    rootAccess,
                    systemHooksEnabled,
                    summary,
                    timelineEvents,
                    performanceSnapshot,
                    false,
                    0L,
                    false,
                    "",
                    new byte[0]
            );
        }

        Result(
                Request request,
                long startedAtMillis,
                long finishedAtMillis,
                long durationMs,
                boolean targetLaunchStarted,
                RootAccessProbe.Result rootAccess,
                boolean systemHooksEnabled,
                String summary,
                List<String> timelineEvents,
                PerformanceSnapshot performanceSnapshot,
                boolean perfettoAvailable,
                long perfettoSizeBytes,
                boolean perfettoTruncated,
                String perfettoNote
        ) {
            this(
                    request,
                    startedAtMillis,
                    finishedAtMillis,
                    durationMs,
                    targetLaunchStarted,
                    rootAccess,
                    systemHooksEnabled,
                    summary,
                    timelineEvents,
                    performanceSnapshot,
                    perfettoAvailable,
                    perfettoSizeBytes,
                    perfettoTruncated,
                    perfettoNote,
                    new byte[0]
            );
        }

        Result(
                Request request,
                long startedAtMillis,
                long finishedAtMillis,
                long durationMs,
                boolean targetLaunchStarted,
                RootAccessProbe.Result rootAccess,
                boolean systemHooksEnabled,
                String summary,
                List<String> timelineEvents,
                PerformanceSnapshot performanceSnapshot,
                boolean perfettoAvailable,
                long perfettoSizeBytes,
                boolean perfettoTruncated,
                String perfettoNote,
                byte[] perfettoTraceBytes
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
            this.performanceSnapshot = performanceSnapshot != null
                    ? performanceSnapshot
                    : PerformanceSnapshot.EMPTY;
            this.perfettoAvailable = perfettoAvailable;
            this.perfettoSizeBytes = Math.max(0L, perfettoSizeBytes);
            this.perfettoTruncated = perfettoTruncated;
            this.perfettoNote = perfettoNote != null ? perfettoNote : "";
            this.perfettoTraceBytes = perfettoTraceBytes != null
                    ? perfettoTraceBytes.clone()
                    : new byte[0];
        }
    }

    private final Host host;
    private final Handler handler;
    private final SummaryBuilder summaryBuilder;
    private final ExecutorService executor;
    private volatile boolean running;
    private volatile Request runningRequest;
    private long runningStartedAtMillis;
    private boolean runningTargetLaunchStarted;
    private boolean finishing;
    private String lastObservedForegroundPackage;
    private final Object timelineLock = new Object();
    private final List<String> runningTimelineEvents = new ArrayList<>();
    private volatile PerfettoTrace runningPerfettoTrace;

    public DiagnosticCoordinator(Host host) {
        this(
                host,
                new Handler(Looper.getMainLooper()),
                Executors.newSingleThreadExecutor(),
                new SummaryBuilder()
        );
    }

    DiagnosticCoordinator(
            Host host,
            Handler handler,
            ExecutorService executor,
            SummaryBuilder summaryBuilder
    ) {
        this.host = host;
        this.handler = handler;
        this.executor = executor;
        this.summaryBuilder = summaryBuilder;
    }

    public boolean start(Request request) {
        if (running || host == null || request == null || !request.isValid()) {
            return false;
        }
        running = true;
        runningRequest = request;
        synchronized (timelineLock) {
            runningTimelineEvents.clear();
        }
        recordTimelineEvent("session requested");
        executor.execute(() -> {
            RootAccessProbe.Result rootAccess = ensureRootAccess();
            if (rootAccess.status != RootAccessProbe.Status.AVAILABLE) {
                handler.post(() -> failForMissingRoot(request));
                return;
            }
            if (!isActiveRequest(request)) {
                return;
            }
            recordTimelineEvent("root available: " + rootProvider(rootAccess));
            RuntimeTransport.Status transportStatus =
                    RuntimeTransport.start(request.packageName, null);
            if (!isActiveRequest(request)) {
                RuntimeTransport.cancel(null);
                return;
            }
            recordTimelineEvent(transportStatus.available
                    ? "runtime transport prepared"
                    : transportStatus.message);
            PerfettoTrace.StartResult perfettoStart =
                    PerfettoTrace.start(null);
            runningPerfettoTrace = perfettoStart.trace;
            if (!isActiveRequest(request)) {
                runningPerfettoTrace = null;
                if (perfettoStart.trace != null) {
                    perfettoStart.trace.discard();
                }
                RuntimeTransport.cancel(null);
                return;
            }
            recordTimelineEvent(perfettoStart.available
                    ? "perfetto trace prepared"
                    : "perfetto unavailable: " + perfettoStart.note);
            RuntimeSelfTest.Status selfTest =
                    RuntimeSelfTest.runUiTransportSelfTest(
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

    public boolean isRunning() {
        return running;
    }

    public void cancel() {
        RuntimeEvents.cancel();
        RuntimeTransport.cancel(null);
        PerfettoTrace trace = runningPerfettoTrace;
        runningPerfettoTrace = null;
        if (trace != null) {
            executor.execute(trace::discard);
        }
        clearRunningState();
        handler.removeCallbacksAndMessages(null);
    }

    public void shutdown() {
        cancel();
        executor.shutdownNow();
    }

    public void onDpisResumed() {
        if (running && runningTargetLaunchStarted && !finishing) {
            requestFinish("foreground returned to DPIS");
        }
    }

    /** Schedules a bounded diagnostic stop without requiring the DPIS UI to stay foreground. */
    public boolean scheduleFinishAfterDelay(long delayMs) {
        if (!running || !runningTargetLaunchStarted || finishing || delayMs <= 0L) {
            return false;
        }
        handler.postDelayed(() -> {
            if (running && runningTargetLaunchStarted && !finishing) {
                requestFinish("diagnostic timer elapsed");
            }
        }, delayMs);
        return true;
    }

    private void requestFinish(String reason) {
        if (!running || finishing) {
            return;
        }
        finishing = true;
        handler.removeCallbacksAndMessages(null);
        recordTimelineEvent(reason);
        if ("diagnostic timer elapsed".equals(reason)) {
            host.onFeedbackDiagnosticAutoFinished();
        }
        executor.execute(this::finishInBackground);
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
            RuntimeEvents.cancel();
            RuntimeTransport.cancel(null);
            PerfettoTrace trace = runningPerfettoTrace;
            runningPerfettoTrace = null;
            if (trace != null) {
                executor.execute(trace::discard);
            }
            clearRunningState();
            host.onFeedbackDiagnosticUnavailable();
            return;
        }
        runningStartedAtMillis = host.currentTimeMillis();
        RuntimeEvents.start(request.packageName, request);
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
        return RootAccessProbe.probe();
    }

    private void scheduleForegroundCheck() {
        handler.postDelayed(this::checkForegroundPackage, FOREGROUND_CHECK_INTERVAL_MS);
    }

    private void checkForegroundPackage() {
        if (!running) {
            return;
        }
        executor.execute(() -> {
            String packageName = ForegroundAppReader.readForegroundPackage();
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
            onDpisResumed();
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

    private void finishInBackground() {
        if (!running || runningRequest == null) {
            return;
        }
        Request request = runningRequest;
        long startedAt = runningStartedAtMillis;
        boolean launched = runningTargetLaunchStarted;
        recordTimelineEvent("session finished");
        PerformanceSnapshot performanceSnapshot =
                RuntimeEvents.stopPerformanceSnapshot();
        List<String> runtimeEvents = RuntimeEvents.stopSnapshot();
        RuntimeTransport.Snapshot transportSnapshot =
                RuntimeTransport.stopSnapshot(null);
        PerfettoTrace trace = runningPerfettoTrace;
        runningPerfettoTrace = null;
        PerfettoTrace.StopResult perfettoStop = trace != null
                ? trace.stop()
                : PerfettoTrace.StopResult.unavailable(
                        "Perfetto trace was not started");
        if (trace != null && perfettoStop.available) {
            perfettoStop = trace.consumeStoppedTrace(perfettoStop);
        }
        List<String> timelineEvents;
        synchronized (timelineLock) {
            timelineEvents = new ArrayList<>(runningTimelineEvents);
        }
        timelineEvents.addAll(runtimeEvents);
        timelineEvents.addAll(transportSnapshot.events);
        if (!transportSnapshot.available || transportSnapshot.events.isEmpty()) {
            String note = !transportSnapshot.note.isBlank()
                    ? transportSnapshot.note
                    : "runtime transport empty";
            timelineEvents.add(formatTime(host.currentTimeMillis())
                    + " source=runtime-transport stage=transport_note message=" + note);
        }
        if (!perfettoStop.available) {
            timelineEvents.add(formatTime(host.currentTimeMillis())
                    + " source=perfetto stage=stop_failed message="
                    + valueOrEmpty(perfettoStop.note));
        }
        timelineEvents.sort(String::compareTo);
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
                        summaryInput(request),
                        startedAt,
                        finishedAt,
                        durationMs,
                        launched,
                        rootAccess,
                        systemHooksEnabled
                ),
                timelineEvents,
                performanceSnapshot,
                perfettoStop.available,
                perfettoStop.sizeBytes,
                perfettoStop.truncated,
                perfettoStop.note,
                perfettoStop.traceBytes
        );
        clearRunningState();
        handler.post(() -> host.onFeedbackDiagnosticFinished(result));
    }


    private static SummaryBuilder.Input summaryInput(Request request) {
        if (request == null) {
            return null;
        }
        return new SummaryBuilder.Input(
                request.packageName,
                request.label,
                request.versionName,
                request.scopeKnown,
                request.inScope,
                request.dpisEnabled,
                request.previewFromGlobalPrefill,
                viewportSummary(request),
                request.viewportApplyMode,
                request.fontScalePercent,
                request.fontApplyMode,
                request.typefaceId,
                request.fontHookDomainsRaw
        );
    }

    private static String viewportSummary(Request request) {
        if (request == null || request.viewportTargetSpec == null) {
            return "off";
        }
        if (request.viewportTargetSpec.isRelativeScale()) {
            return "scale=" + AppConfigInputValidation.formatScaleMilliPercent(
                    request.viewportTargetSpec.scaleMilliPercent());
        }
        if (request.viewportTargetSpec.isAbsoluteDp()) {
            return "widthDp=" + request.viewportTargetSpec.absoluteWidthDp();
        }
        return "off";
    }

    private void clearRunningState() {
        running = false;
        runningRequest = null;
        runningStartedAtMillis = 0L;
        runningTargetLaunchStarted = false;
        finishing = false;
        lastObservedForegroundPackage = null;
        synchronized (timelineLock) {
            runningTimelineEvents.clear();
        }
        runningPerfettoTrace = null;
    }

    private boolean isActiveRequest(Request request) {
        return running && runningRequest == request;
    }

    private static boolean samePackage(String first, String second) {
        return valueOrEmpty(first).equals(valueOrEmpty(second));
    }

    private void recordTimelineEvent(String event) {
        String normalized = valueOrEmpty(event);
        if (!normalized.isEmpty()) {
            synchronized (timelineLock) {
                runningTimelineEvents.add(
                        formatTime(host.currentTimeMillis()) + " " + normalized
                );
            }
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
