package com.dpis.module.runtime.appprocess

import android.graphics.Rect
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.runtime.ProcessScopedInstallGate
import com.dpis.module.runtime.hookapi.ModernApiCapabilitiesResolver
import com.dpis.module.runtime.probe.RuntimeDiagnosticLogFingerprint
import com.dpis.module.runtime.probe.RuntimeHotPathEvidenceSampler
import com.dpis.module.viewport.ViewportConsistencyDiagnostics
import com.dpis.module.viewport.VirtualDisplayState
import com.dpis.module.viewport.window.WindowBoundsState
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker
import java.lang.reflect.Method
import kotlin.concurrent.Volatile

object WindowMetricsHookInstaller {
    private const val ROUTE_NAME = "window_metrics_bounds_override"
    private const val HOOK_ID_WINDOW_METRICS_GET_BOUNDS = "window_metrics_get_bounds"

    @Volatile
    private var installedPid = -1

    private val HOTPATH_SAMPLER = RuntimeHotPathEvidenceSampler()

    @JvmStatic
    fun resetForHotReload() {
        installedPid = -1
    }

    @JvmStatic
    @Throws(ReflectiveOperationException::class)
    fun install(xposed: XposedInterface, packageName: String?) {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            return
        }
        synchronized(WindowMetricsHookInstaller::class.java) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                return
            }
            val bootClassLoader = ClassLoader.getSystemClassLoader()
            val windowMetricsClass = Class.forName(
                "android.view.WindowMetrics",
                false,
                bootClassLoader,
            )
            val getBoundsMethod: Method = windowMetricsClass.getDeclaredMethod("getBounds")
            // 102 can replace this hook in place; 101 just ignores the hint.
            ModernApiCapabilitiesResolver.fromXposed(xposed).applyStableHookId<HookBuilder>(
                xposed.hook(getBoundsMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_WINDOW_METRICS_GET_BOUNDS,
            ).intercept(Hooker { chain: XposedInterface.Chain? ->
                val result = chain!!.proceed()
                if (result !is Rect) {
                    return@Hooker result
                }
                val mayInvalidateDisplay = WindowBoundsState.recordAndShouldInvalidateDisplay(
                    packageName,
                    result,
                    chain.thisObject,
                )
                val invalidatedState = if (mayInvalidateDisplay) {
                    VirtualDisplayState.invalidateIfFullscreenBoundsConflict(result)
                } else {
                    null
                }
                ViewportConsistencyDiagnostics.observeWindowBounds(
                    packageName,
                    result,
                    invalidatedState,
                )
                val fingerprint = RuntimeDiagnosticLogFingerprint.field()
                recordProbeAtMostEvery(
                    packageName,
                    "source=WindowMetrics.getBounds, $fingerprint, bounds=${result.width()}x${result.height()}",
                )
                if (!WindowFrameOverride.isEnabled()) {
                    recordSkipAtMostEvery(
                        packageName,
                        "source=WindowMetrics.getBounds, reason=window_frame_override_disabled, "
                                + "bounds=${result.width()}x${result.height()}",
                    )
                    return@Hooker result
                }
                val override = VirtualDisplayState.get()
                if (override == null) {
                    recordSkipAtMostEvery(
                        packageName,
                        "source=WindowMetrics.getBounds, reason=no_virtual_display_state, "
                                + "bounds=${result.width()}x${result.height()}",
                    )
                    return@Hooker result
                }
                val newRect = Rect(
                    result.left,
                    result.top,
                    result.left + override.widthPx,
                    result.top + override.heightPx,
                )
                val detail =
                    "source=WindowMetrics.getBounds, bounds=${result.width()}x${result.height()}" +
                            "->${newRect.width()}x${newRect.height()}"
                val sample = HOTPATH_SAMPLER.sample("applied|$packageName|$detail", detail)
                if (sample.emit) {
                    val sampledDetail = sample.detail
                    DpisLog.i(
                        "WindowMetrics override: bounds=${result.width()}x${result.height()} -> " +
                                "${newRect.width()}x${newRect.height()}, $sampledDetail",
                    )
                    RuntimeHotPathEvents.applied(
                        packageName,
                        "viewport",
                        ROUTE_NAME,
                        sampledDetail,
                    )
                }
                newRect
            })
            installedPid = ProcessScopedInstallGate.currentPid()
            DpisLog.i("WindowMetrics hook ready, ${RuntimeDiagnosticLogFingerprint.field()}")
        }
    }

    @JvmStatic
    fun resetHotPathSamplerForTest() {
        HOTPATH_SAMPLER.resetForTest()
    }

    private fun recordProbeAtMostEvery(packageName: String?, detail: String) {
        val sample = HOTPATH_SAMPLER.sample("probe|" + packageName + "|" + detail, detail)
        if (sample.emit) {
            val sampledDetail = sample.detail
            DpisLog.i(
                "DPIS_VIEWPORT WindowMetrics callback: package=${safeValue(packageName)}, " +
                        "route=$ROUTE_NAME, $sampledDetail",
            )
            RuntimeHotPathEvents.probe(
                packageName,
                "viewport",
                ROUTE_NAME,
                sampledDetail,
            )
        }
    }

    private fun recordSkipAtMostEvery(packageName: String?, detail: String) {
        val sample = HOTPATH_SAMPLER.sample("skip|" + packageName + "|" + detail, detail)
        if (sample.emit) {
            val sampledDetail = sample.detail
            DpisLog.i(
                "DPIS_VIEWPORT WindowMetrics skip: package=${safeValue(packageName)}, " +
                        "route=$ROUTE_NAME, $sampledDetail",
            )
            RuntimeHotPathEvents.skipped(
                packageName,
                "viewport",
                ROUTE_NAME,
                sampledDetail,
            )
        }
    }

    private fun safeValue(value: String?): String {
        return if (value.isNullOrBlank()) "unknown" else value
    }
}
