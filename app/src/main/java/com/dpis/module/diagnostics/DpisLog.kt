package com.dpis.module.diagnostics

import android.util.Log
import com.dpis.module.BuildConfig
import com.dpis.module.diagnostics.device.RuntimeTransport

object DpisLog {
    const val TAG = "DPIS"

    @Volatile
    private var loggingEnabled = true

    @Volatile
    private var appLogSink: AppLogSink? = null

    fun interface AppLogSink {
        fun record(level: String?, message: String?)
    }

    /** Keeps normal logging at INFO while an active diagnostic can collect extra detail. */
    @JvmStatic
    fun d(msg: String?) {
        if (!isDebugLoggingEnabled()) return
        write(Log.DEBUG, "D", msg, null)
    }

    @JvmStatic
    fun i(msg: String?) {
        if (!shouldLog()) return
        write(Log.INFO, "I", msg, null)
    }

    @JvmStatic
    fun w(msg: String?) {
        if (!shouldLog()) return
        write(Log.WARN, "W", msg, null)
    }

    @JvmStatic
    fun e(msg: String?, throwable: Throwable?) {
        if (!shouldLog()) return
        write(Log.ERROR, "E", msg, throwable)
    }

    /**
     * Temporary long-idle recovery history path. It stays independent from the global log switch
     * so next-day WeChat recovery evidence remains exportable; remove or narrow it once that
     * regression is understood.
     */
    @JvmStatic
    fun routeHistory(msg: String?) {
        write(Log.INFO, "I", msg, null)
    }

    private fun write(priority: Int, level: String, msg: String?, throwable: Throwable?) {
        try {
            if (throwable == null) {
                Log.println(priority, TAG, msg.orEmpty())
            } else {
                Log.e(TAG, msg, throwable)
            }
        } catch (_: RuntimeException) {
            // Local unit tests may execute without Android logging available.
        }
        val throwableMessage = throwable?.let { "${it.javaClass.name}: ${it.message}" }
        val recordedMessage = if (throwableMessage.isNullOrEmpty()) msg else "$msg | $throwableMessage"
        recordAppLog(level, recordedMessage)
        RuntimeEvents.recordDpisLog(level, recordedMessage)
        RuntimeTransport.record("runtime", "dpis_log", "", recordedMessage)
    }

    @JvmStatic
    fun isLoggingEnabled(): Boolean = loggingEnabled || isDiagnosticCaptureActive()

    @JvmStatic
    fun isDebugLoggingEnabled(): Boolean = BuildConfig.DEBUG || isDiagnosticCaptureActive()

    private fun shouldLog(): Boolean = BuildConfig.DEBUG || isLoggingEnabled()

    private fun isDiagnosticCaptureActive(): Boolean = try {
        RuntimeTransport.isCaptureActive
    } catch (_: RuntimeException) {
        false
    } catch (_: LinkageError) {
        false
    }

    @JvmStatic
    fun setLoggingEnabled(enabled: Boolean) {
        loggingEnabled = enabled
    }

    @JvmStatic
    fun setAppLogSink(sink: AppLogSink?) {
        appLogSink = sink
    }

    private fun recordAppLog(level: String, message: String?) {
        try {
            appLogSink?.record(level, message)
        } catch (_: RuntimeException) {
            // Logging must never affect runtime behavior.
        }
    }
}
