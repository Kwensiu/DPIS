package com.dpis.module.diagnostics

import android.os.Handler
import android.os.Looper
import com.dpis.module.appconfig.AppConfigInput
import com.dpis.module.appconfig.editor.EditorDraft
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.device.ForegroundAppReader
import com.dpis.module.diagnostics.device.PerfettoTrace
import com.dpis.module.diagnostics.device.RuntimeTransport
import com.dpis.module.diagnostics.device.TransportSelfTest
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.root.RootAccessProbe
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Coordinates one feedback diagnostic recording and its asynchronous packaging inputs. */
class Coordinator private constructor(
    val host: Host,
    private val handler: Handler,
    private val executor: ExecutorService,
    private val summaryBuilder: SummaryBuilder,
    @Suppress("UNUSED_PARAMETER") marker: Unit
) {
    constructor(host: Host) : this(
        host,
        Handler(Looper.getMainLooper()),
        Executors.newSingleThreadExecutor(),
        SummaryBuilder(),
        Unit
    )

    internal constructor(
        host: Host,
        handler: Handler,
        executor: ExecutorService,
        summaryBuilder: SummaryBuilder
    ) : this(host, handler, executor, summaryBuilder, Unit)

    interface Host {
        fun restartTargetAppForDiagnostic(packageName: String?): Boolean
        fun dpisPackageName(): String?
        fun rootAccess(): RootAccessProbe.Result
        fun systemHooksEnabled(): Boolean
        fun currentTimeMillis(): Long
        fun onFeedbackDiagnosticStarted()
        fun onFeedbackDiagnosticUnavailable()
        fun onFeedbackDiagnosticRootRequired()
        fun onFeedbackDiagnosticFinished(result: Result?)
        fun onFeedbackDiagnosticAutoFinished() {}
        fun diagnosticCacheDirectory(): String? = null
    }

    class Request(
        packageName: String?,
        label: String?,
        versionName: String?,
        @JvmField val scopeKnown: Boolean,
        @JvmField val inScope: Boolean,
        @JvmField val dpisEnabled: Boolean,
        @JvmField val previewFromGlobalPrefill: Boolean,
        viewportTargetSpec: ViewportTargetSpec?,
        viewportApplyMode: String?,
        fontScalePercent: Int?,
        fontApplyMode: String?,
        typefaceId: String?,
        fontHookDomainsRaw: String?,
        wechatDpi: Int?
    ) {
        @JvmField val packageName = valueOrEmpty(packageName)
        @JvmField val label = valueOrEmpty(label)
        @JvmField val versionName = valueOrEmpty(versionName)
        @JvmField val viewportTargetSpec = viewportTargetSpec ?: ViewportTargetSpec.off()
        @JvmField val viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode)
        @JvmField val fontApplyMode = FontApplyMode.normalize(fontApplyMode)
        @JvmField val fontScalePercent = fontScalePercent
        @JvmField val typefaceId = normalizeNullableString(typefaceId)
        @JvmField val fontHookDomainsRaw = normalizeNullableString(fontHookDomainsRaw)
        @JvmField val wechatDpi = wechatDpi

        fun isValid(): Boolean = packageName.isNotBlank()

        companion object {
            @JvmStatic
            fun from(item: AppListItem?, draft: EditorDraft?): Request = from(item, draft, "")

            @JvmStatic
            fun from(item: AppListItem?, draft: EditorDraft?, versionName: String?): Request {
                val actualItem = requireNotNull(item)
                val useDraft = draft != null
                return Request(
                    if (useDraft && !valueOrEmpty(draft!!.packageName).isBlank()) {
                        draft.packageName
                    } else actualItem.packageName,
                    actualItem.label,
                    versionName,
                    actualItem.scopeKnown,
                    if (useDraft) draft!!.scopeSelected else actualItem.inScope,
                    if (useDraft) draft!!.dpisEnabled else actualItem.dpisEnabled,
                    actualItem.previewFromGlobalPrefill,
                    actualItem.viewportTargetSpec,
                    if (useDraft) draft!!.viewportApplyMode else actualItem.viewportMode,
                    actualItem.fontScalePercent,
                    actualItem.fontMode,
                    if (useDraft) draft!!.selectedTypefaceId else actualItem.typefaceId,
                    if (useDraft) draft!!.draftFontHookDomainsRaw else actualItem.effectiveFontHookDomainsRaw(),
                    actualItem.wechatDpi
                )
            }

            @JvmStatic
            fun fromPersisted(
                item: AppListItem?,
                draft: EditorDraft?,
                versionName: String?,
                store: DpisConfigStore?
            ): Request {
                val draftPackageName = valueOrEmpty(draft?.packageName)
                val packageName = if (draftPackageName.isNotBlank()) draftPackageName
                else item?.packageName ?: ""
                if (store == null || packageName.isBlank() || item == null) {
                    return from(item, draft, versionName)
                }
                return Request(
                    packageName,
                    item.label,
                    versionName,
                    item.scopeKnown,
                    draft?.scopeSelected ?: item.inScope,
                    store.isTargetDpisEnabled(packageName),
                    false,
                    store.getTargetViewportSpec(packageName),
                    store.getTargetViewportApplyMode(packageName),
                    store.getTargetFontScalePercent(packageName),
                    store.getTargetFontApplyMode(packageName),
                    store.getTargetTypefaceId(packageName),
                    store.getTargetFontHookDomainsRaw(packageName),
                    store.getWechatDpi(packageName)
                )
            }
        }
    }

    class Result internal constructor(
        @JvmField val request: Request,
        @JvmField val startedAtMillis: Long,
        @JvmField val finishedAtMillis: Long,
        @JvmField val durationMs: Long,
        @JvmField val targetLaunchStarted: Boolean,
        rootAccess: RootAccessProbe.Result?,
        @JvmField val systemHooksEnabled: Boolean,
        summary: String?,
        timelineEvents: List<String>?,
        performanceSnapshot: PerformanceSnapshot?,
        @JvmField val perfettoAvailable: Boolean,
        perfettoSizeBytes: Long,
        @JvmField val perfettoTruncated: Boolean,
        perfettoNote: String?,
        perfettoTraceBytes: ByteArray?,
        @JvmField val perfettoTraceFilePath: String? = null,
    ) {
        @JvmField val rootAccess = rootAccess ?: RootAccessProbe.Result.unknown()
        @JvmField val summary = summary ?: ""
        @JvmField val timelineEvents = ArrayList(timelineEvents ?: emptyList())
        @JvmField val performanceSnapshot = performanceSnapshot ?: PerformanceSnapshot.EMPTY
        @JvmField val perfettoSizeBytes = perfettoSizeBytes.coerceAtLeast(0L)
        @JvmField val perfettoNote = perfettoNote ?: ""
        @JvmField val perfettoTraceBytes = perfettoTraceBytes?.clone() ?: ByteArray(0)

        constructor(
            request: Request, startedAtMillis: Long, finishedAtMillis: Long, durationMs: Long,
            targetLaunchStarted: Boolean, rootAccess: RootAccessProbe.Result?,
            systemHooksEnabled: Boolean, summary: String?, timelineEvents: List<String>?
        ) : this(request, startedAtMillis, finishedAtMillis, durationMs, targetLaunchStarted,
            rootAccess, systemHooksEnabled, summary, timelineEvents, PerformanceSnapshot.EMPTY,
            false, 0L, false, "", ByteArray(0), null
        )

        constructor(
            request: Request, startedAtMillis: Long, finishedAtMillis: Long, durationMs: Long,
            targetLaunchStarted: Boolean, rootAccess: RootAccessProbe.Result?,
            systemHooksEnabled: Boolean, summary: String?, timelineEvents: List<String>?,
            performanceSnapshot: PerformanceSnapshot?
        ) : this(request, startedAtMillis, finishedAtMillis, durationMs, targetLaunchStarted,
            rootAccess, systemHooksEnabled, summary, timelineEvents, performanceSnapshot,
            false, 0L, false, "", ByteArray(0), null
        )

        constructor(
            request: Request, startedAtMillis: Long, finishedAtMillis: Long, durationMs: Long,
            targetLaunchStarted: Boolean, rootAccess: RootAccessProbe.Result?,
            systemHooksEnabled: Boolean, summary: String?, timelineEvents: List<String>?,
            performanceSnapshot: PerformanceSnapshot?, perfettoAvailable: Boolean,
            perfettoSizeBytes: Long, perfettoTruncated: Boolean, perfettoNote: String?
        ) : this(request, startedAtMillis, finishedAtMillis, durationMs, targetLaunchStarted,
            rootAccess, systemHooksEnabled, summary, timelineEvents, performanceSnapshot,
            perfettoAvailable,
            perfettoSizeBytes,
            perfettoTruncated,
            perfettoNote,
            ByteArray(0),
            null
        )
    }

    @Volatile private var running = false
    @Volatile private var runningRequest: Request? = null
    private var runningStartedAtMillis = 0L
    private var runningTargetLaunchStarted = false
    private var finishing = false
    private var lastObservedForegroundPackage: String? = null
    private val timelineLock = Any()
    private val runningTimelineEvents = ArrayList<String>()
    @Volatile private var runningPerfettoTrace: PerfettoTrace? = null

    private val activeHandler: Handler get() = handler
    private val activeExecutor: ExecutorService get() = executor
    private val activeSummaryBuilder: SummaryBuilder get() = summaryBuilder

    fun start(request: Request?): Boolean {
        return start(request, PerfettoTrace.MAX_CAPTURE_DURATION_MS)
    }

    /**
     * Starts a diagnostic session, optionally skipping Perfetto when duration is non-positive.
     */
    fun start(request: Request?, perfettoDurationMs: Long): Boolean {
        if (running || request == null || !request.isValid()) return false
        running = true
        runningRequest = request
        synchronized(timelineLock) { runningTimelineEvents.clear() }
        recordTimelineEvent("session requested")
        activeExecutor.execute {
            val rootAccess = ensureRootAccess()
            if (rootAccess.status != RootAccessProbe.Status.AVAILABLE) {
                activeHandler.post { failForMissingRoot(request) }
                return@execute
            }
            if (!isActiveRequest(request)) return@execute
            recordTimelineEvent("root available: ${rootProvider(rootAccess)}")
            val transportStatus = RuntimeTransport.start(request.packageName, null)
            if (!isActiveRequest(request)) {
                RuntimeTransport.cancel(null)
                return@execute
            }
            recordTimelineEvent(if (transportStatus.available) "runtime transport prepared" else transportStatus.message)
            if (perfettoDurationMs <= 0L) {
                recordTimelineEvent("perfetto not requested")
            } else {
                val perfettoStart = PerfettoTrace.start(null, perfettoDurationMs)
                runningPerfettoTrace = perfettoStart.trace
                if (!isActiveRequest(request)) {
                    runningPerfettoTrace = null
                    perfettoStart.trace?.discard()
                    RuntimeTransport.cancel(null)
                    return@execute
                }
                recordTimelineEvent(if (perfettoStart.available) "perfetto trace prepared" else "perfetto unavailable: ${perfettoStart.note}")
            }
            val selfTest = TransportSelfTest.runUiTransportSelfTest(request.packageName, null)
            recordTimelineEvent(if (selfTest.uiWriteReadOk) "runtime transport self-test ok" else "runtime transport self-test failed: ${selfTest.message}")
            recordTimelineEvent("root force-stop/start requested")
            val launched = host.restartTargetAppForDiagnostic(request.packageName)
            recordTimelineEvent(if (launched) "root force-stop/start succeeded" else "root force-stop/start failed")
            activeHandler.post { startAfterRootLaunch(request, launched) }
        }
        return true
    }

    fun isRunning(): Boolean = running

    fun cancel() {
        RuntimeEvents.cancel()
        RuntimeTransport.cancel(null)
        val trace = runningPerfettoTrace
        runningPerfettoTrace = null
        trace?.let { activeExecutor.execute(it::discard) }
        clearRunningState()
        activeHandler.removeCallbacksAndMessages(null)
    }

    fun shutdown() {
        cancel()
        activeExecutor.shutdownNow()
    }

    fun onDpisResumed() {
        if (DiagnosticFinishPolicy.canFinishAfterDpisResume(
                running,
                runningTargetLaunchStarted,
                finishing,
            )
        ) {
            requestFinish("foreground returned to DPIS")
        }
    }

    fun scheduleFinishAfterDelay(delayMs: Long): Boolean {
        if (!running || !runningTargetLaunchStarted || finishing || delayMs <= 0L) return false
        activeHandler.postDelayed({
            if (running && runningTargetLaunchStarted && !finishing) requestFinish("diagnostic timer elapsed")
        }, delayMs)
        return true
    }

    private fun requestFinish(reason: String) {
        if (!running || finishing) return
        finishing = true
        activeHandler.removeCallbacksAndMessages(null)
        recordTimelineEvent(reason)
        if (reason == "diagnostic timer elapsed") host.onFeedbackDiagnosticAutoFinished()
        activeExecutor.execute(::finishInBackground)
    }

    private fun failForMissingRoot(request: Request) {
        if (!running || runningRequest !== request) return
        clearRunningState()
        host.onFeedbackDiagnosticRootRequired()
    }

    private fun startAfterRootLaunch(request: Request, launched: Boolean) {
        if (!running || runningRequest !== request) return
        if (!launched) {
            RuntimeEvents.cancel()
            RuntimeTransport.cancel(null)
            val trace = runningPerfettoTrace
            runningPerfettoTrace = null
            trace?.let { activeExecutor.execute(it::discard) }
            clearRunningState()
            host.onFeedbackDiagnosticUnavailable()
            return
        }
        runningStartedAtMillis = host.currentTimeMillis()
        RuntimeEvents.start(request.packageName, request)
        recordTimelineEvent("session started")
        recordTimelineEvent("app config resolved")
        DpisLog.i("feedback diagnostic session started: package=${request.packageName}, versionName=${valueOrEmpty(request.versionName)}")
        runningTargetLaunchStarted = true
        lastObservedForegroundPackage = request.packageName
        host.onFeedbackDiagnosticStarted()
        scheduleForegroundCheck()
    }

    private fun ensureRootAccess(): RootAccessProbe.Result = RootAccessProbe.probe()

    private fun scheduleForegroundCheck() {
        activeHandler.postDelayed(::checkForegroundPackage, FOREGROUND_CHECK_INTERVAL_MS)
    }

    private fun checkForegroundPackage() {
        if (!running) return
        activeExecutor.execute {
            val packageName = ForegroundAppReader.readForegroundPackage()
            activeHandler.post { handleForegroundPackage(packageName) }
        }
    }

    private fun handleForegroundPackage(packageName: String?) {
        if (!running) return
        val request = runningRequest ?: return
        if (samePackage(packageName, request.packageName)) {
            scheduleForegroundCheck()
            return
        }
        if (samePackage(packageName, host.dpisPackageName())) {
            recordTimelineEvent("foreground returned to DPIS")
            onDpisResumed()
            return
        }
        if (!packageName.isNullOrBlank() && !samePackage(packageName, lastObservedForegroundPackage)) {
            recordTimelineEvent("foreground changed to $packageName")
            lastObservedForegroundPackage = packageName
        }
        scheduleForegroundCheck()
    }

    private fun finishInBackground() {
        val request = runningRequest ?: return
        val startedAt = runningStartedAtMillis
        val launched = runningTargetLaunchStarted
        recordTimelineEvent("session finished")
        val performanceSnapshot = RuntimeEvents.stopPerformanceSnapshot()
        val runtimeEvents = RuntimeEvents.stopSnapshot()
        val transportSnapshot = RuntimeTransport.stopSnapshot(null)
        val trace = runningPerfettoTrace
        runningPerfettoTrace = null
        var perfettoStop = trace?.stop() ?: PerfettoTrace.StopResult.unavailable("Perfetto trace was not started")
        if (trace != null && perfettoStop.available) {
            val destination = host.diagnosticCacheDirectory()?.let { directory ->
                File(directory).apply { mkdirs() }
                    .resolve("dpis-perfetto-${startedAt}-${request.packageName.hashCode()}.pftrace")
            }
            perfettoStop = trace.consumeStoppedTrace(perfettoStop, destination)
        }
        val timelineEvents = synchronized(timelineLock) { ArrayList(runningTimelineEvents) }
        timelineEvents.addAll(runtimeEvents)
        timelineEvents.addAll(transportSnapshot.events)
        if (!transportSnapshot.available || transportSnapshot.events.isEmpty()) {
            val note = transportSnapshot.note.takeIf { it.isNotBlank() } ?: "runtime transport empty"
            timelineEvents.add("${formatTime(host.currentTimeMillis())} source=runtime-transport stage=transport_note message=$note")
        }
        if (!perfettoStop.available) {
            timelineEvents.add("${formatTime(host.currentTimeMillis())} source=perfetto stage=stop_failed message=${valueOrEmpty(perfettoStop.note)}")
        }
        timelineEvents.sort()
        val finishedAt = host.currentTimeMillis()
        val durationMs = (finishedAt - startedAt).coerceAtLeast(0L)
        val rootAccess = host.rootAccess()
        val systemHooksEnabled = host.systemHooksEnabled()
        DpisLog.i("feedback diagnostic session finished: package=${request.packageName}, durationMs=$durationMs")
        val result = Result(
            request, startedAt, finishedAt, durationMs, launched, rootAccess, systemHooksEnabled,
            activeSummaryBuilder.build(summaryInput(request), startedAt, finishedAt, durationMs,
                launched, rootAccess, systemHooksEnabled), timelineEvents, performanceSnapshot,
            perfettoStop.available, perfettoStop.sizeBytes, perfettoStop.truncated,
            perfettoStop.note, perfettoStop.traceBytes, perfettoStop.traceFilePath
        )
        clearRunningState()
        activeHandler.post { host.onFeedbackDiagnosticFinished(result) }
    }

    private fun clearRunningState() {
        running = false
        runningRequest = null
        runningStartedAtMillis = 0L
        runningTargetLaunchStarted = false
        finishing = false
        lastObservedForegroundPackage = null
        synchronized(timelineLock) { runningTimelineEvents.clear() }
        runningPerfettoTrace = null
    }

    private fun isActiveRequest(request: Request): Boolean = running && runningRequest === request

    private fun recordTimelineEvent(event: String?) {
        val normalized = valueOrEmpty(event)
        if (normalized.isNotEmpty()) synchronized(timelineLock) {
            runningTimelineEvents.add("${formatTime(host.currentTimeMillis())} $normalized")
        }
    }

    companion object {
        private const val FOREGROUND_CHECK_INTERVAL_MS = 1_000L

        @JvmStatic
        private fun summaryInput(request: Request?): SummaryBuilder.Input? = request?.let {
            SummaryBuilder.Input(it.packageName, it.label, it.versionName, it.scopeKnown, it.inScope,
                it.dpisEnabled, it.previewFromGlobalPrefill, viewportSummary(it), it.viewportApplyMode,
                it.fontScalePercent, it.fontApplyMode, it.typefaceId, it.fontHookDomainsRaw)
        }

        @JvmStatic
        private fun viewportSummary(request: Request?): String {
            val spec = request?.viewportTargetSpec ?: return "off"
            return when {
                spec.isRelativeScale -> "scale=${AppConfigInput.formatScaleMilliPercent(spec.scaleMilliPercent())}"
                spec.isAbsoluteDp -> "widthDp=${spec.absoluteWidthDp()}"
                else -> "off"
            }
        }

        private fun samePackage(first: String?, second: String?): Boolean = valueOrEmpty(first) == valueOrEmpty(second)

        private fun rootProvider(rootAccess: RootAccessProbe.Result?): String =
            rootAccess?.provider?.takeUnless { it.isBlank() } ?: "unknown"

        private fun formatTime(millis: Long): String =
            SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date(millis))

        private fun valueOrEmpty(value: String?): String = value?.trim() ?: ""

        private fun normalizeNullableString(value: String?): String? = valueOrEmpty(value).takeIf { it.isNotEmpty() }
    }
}
