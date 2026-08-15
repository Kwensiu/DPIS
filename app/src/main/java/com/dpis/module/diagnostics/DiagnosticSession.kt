package com.dpis.module.diagnostics

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.dpis.module.root.RootAccessProbe
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Retained owner for one feedback diagnostic page session.
 *
 * It deliberately does not retain an Activity or Compose presentation. A replacement Activity
 * attaches after a configuration change, while collection and package construction keep running.
 */
class DiagnosticSession(context: Context) {
    interface Host {
        fun restartTargetAppForDiagnostic(packageName: String): Boolean
        fun systemHooksEnabled(): Boolean
        fun onRecordingStarted()
        fun onStartUnavailable(rootRequired: Boolean)
        fun onPackagingStarted()
        fun onPackageReady(diagnosticPackage: DiagnosticExportBuilder.DiagnosticPackage)
        fun onPackagingFailed()
        fun onAutoFinished()
    }

    enum class Phase {
        PREPARING,
        RECORDING,
        PACKAGING,
        READY,
        PACKAGING_FAILED,
    }

    private val applicationContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val exportBuilder = DiagnosticExportBuilder(applicationContext)
    private val exportExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    @Volatile
    private var host: Host? = null

    @Volatile
    private var currentPhase = Phase.PREPARING

    @Volatile
    private var completedPackage: DiagnosticExportBuilder.DiagnosticPackage? = null

    private var durationEnabled = false
    private var durationSeconds = 30

    @Volatile
    // Invalidates a package build that outlives the page/session which started it.
    private var sessionGeneration = 0L

    private val coordinator: DiagnosticCoordinator by lazy(LazyThreadSafetyMode.NONE) {
        DiagnosticCoordinator(object : DiagnosticCoordinator.Host {
        override fun restartTargetAppForDiagnostic(packageName: String): Boolean {
            return host?.restartTargetAppForDiagnostic(packageName) ?: false
        }

        override fun dpisPackageName(): String = applicationContext.packageName

        override fun rootAccess(): RootAccessProbe.Result = RootAccessProbe.cachedResult()

        override fun systemHooksEnabled(): Boolean = host?.systemHooksEnabled() ?: false

        override fun currentTimeMillis(): Long = System.currentTimeMillis()

        override fun onFeedbackDiagnosticStarted() {
            currentPhase = Phase.RECORDING
            if (durationEnabled) {
                this@DiagnosticSession.coordinator.scheduleFinishAfterDelay(durationSeconds * 1_000L)
            }
            notifyHost { it.onRecordingStarted() }
        }

        override fun onFeedbackDiagnosticUnavailable() {
            currentPhase = Phase.PREPARING
            notifyHost { it.onStartUnavailable(false) }
        }

        override fun onFeedbackDiagnosticRootRequired() {
            currentPhase = Phase.PREPARING
            notifyHost { it.onStartUnavailable(true) }
        }

        override fun onFeedbackDiagnosticFinished(result: DiagnosticCoordinator.Result) {
            currentPhase = Phase.PACKAGING
            notifyHost { it.onPackagingStarted() }
            val generation = sessionGeneration
            exportExecutor.execute { buildPackage(result, generation) }
        }

        override fun onFeedbackDiagnosticAutoFinished() {
            notifyHost { it.onAutoFinished() }
        }
        })
    }

    fun attachHost(host: Host) {
        this.host = host
        replayCurrentState(host)
    }

    fun detachHost() {
        host = null
    }

    fun start(
        request: DiagnosticCoordinator.Request,
        durationEnabled: Boolean,
        durationSeconds: Int,
    ): Boolean {
        if (currentPhase != Phase.PREPARING) return false
        sessionGeneration++
        this.durationEnabled = durationEnabled
        this.durationSeconds = durationSeconds.coerceAtLeast(1)
        return coordinator.start(request)
    }

    fun isRunning(): Boolean = coordinator.isRunning()

    fun hasPageState(): Boolean = currentPhase != Phase.PREPARING || isRunning() || completedPackage != null

    fun diagnosticPackage(): DiagnosticExportBuilder.DiagnosticPackage? = completedPackage

    fun cancel() {
        sessionGeneration++
        coordinator.cancel()
        completedPackage = null
        currentPhase = Phase.PREPARING
    }

    fun shutdown() {
        detachHost()
        coordinator.shutdown()
        exportExecutor.shutdownNow()
    }

    private fun buildPackage(result: DiagnosticCoordinator.Result, generation: Long) {
        val built = try {
            exportBuilder.buildPackage(result)
        } catch (_: IOException) {
            null
        } catch (_: RuntimeException) {
            null
        }
        if (generation != sessionGeneration) return
        if (built == null) {
            currentPhase = Phase.PACKAGING_FAILED
            notifyHost { it.onPackagingFailed() }
            return
        }
        completedPackage = built
        currentPhase = Phase.READY
        notifyHost { it.onPackageReady(built) }
    }

    private fun replayCurrentState(host: Host) {
        when (currentPhase) {
            Phase.RECORDING -> host.onRecordingStarted()
            Phase.PACKAGING -> host.onPackagingStarted()
            Phase.READY -> completedPackage?.let(host::onPackageReady)
            Phase.PACKAGING_FAILED -> host.onPackagingFailed()
            Phase.PREPARING -> Unit
        }
    }

    private fun notifyHost(callback: (Host) -> Unit) {
        mainHandler.post {
            host?.let(callback)
        }
    }
}
