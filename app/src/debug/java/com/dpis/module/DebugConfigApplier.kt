package com.dpis.module

import android.content.Context
import android.content.Intent
import android.util.Log
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.root.RootAppProcessLauncher
import com.dpis.module.runtime.font.FontRuntimePropertySyncer
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportPropertySyncer

internal object DebugConfigApplier {
    const val ACTION_SET_PACKAGE_CONFIG = "io.github.kwensiu.dpis.DEBUG_SET_PACKAGE_CONFIG"
    const val TAG = "DPIS_DEBUG_CONFIG"

    private const val EXTRA_PACKAGE = "package"
    private const val EXTRA_VIEWPORT_WIDTH_DP = "viewport_width_dp"
    private const val EXTRA_VIEWPORT_MODE = "viewport_mode"
    private const val EXTRA_FONT_SCALE_PERCENT = "font_scale_percent"
    private const val EXTRA_FONT_MODE = "font_mode"
    private const val EXTRA_RESTART = "restart"
    private const val EXTRA_SAFE_MODE = "system_server_safe_mode"
    private const val EXTRA_LOG_ENABLED = "log_enabled"
    private const val EXTRA_FONT_DEBUG_OVERLAY_ENABLED = "font_debug_overlay_enabled"

    // The debug-only Activity and BroadcastReceiver still call this from Java.
    @JvmStatic
    fun apply(context: Context, intent: Intent?, requireActionMatch: Boolean) {
        if (!BuildConfig.DEBUG || intent == null) return
        if (requireActionMatch && ACTION_SET_PACKAGE_CONFIG != intent.action) return
        val packageName = intent.getStringExtra(EXTRA_PACKAGE)
        if (packageName.isNullOrBlank()) {
            Log.w(TAG, "ignored: missing package")
            return
        }
        val store = DpisApplication.getConfigStore()
        val userId = store.packageConfigOwnerUserId()
        var saved = true
        if (intent.hasExtra(EXTRA_VIEWPORT_WIDTH_DP)) {
            val widthDp = intent.getIntExtra(EXTRA_VIEWPORT_WIDTH_DP, 0)
            val normalizedMode =
                ViewportApplyMode.normalize(intent.getStringExtra(EXTRA_VIEWPORT_MODE))
            if (widthDp > 0 && ViewportApplyMode.isEnabled(normalizedMode)) {
                saved = store.setTargetViewportWidthDp(packageName, widthDp) && saved
                saved = store.setTargetViewportApplyMode(packageName, normalizedMode) && saved
                ViewportPropertySyncer.publishTargetAsync(packageName, widthDp, normalizedMode)
            } else {
                saved = store.clearTargetViewportWidthDp(packageName) && saved
                ViewportPropertySyncer.clearTargetAsync(packageName)
            }
        }
        if (intent.hasExtra(EXTRA_FONT_SCALE_PERCENT)) {
            val percent = intent.getIntExtra(EXTRA_FONT_SCALE_PERCENT, 0)
            val normalizedMode = FontApplyMode.normalize(intent.getStringExtra(EXTRA_FONT_MODE))
            if (percent > 0 && FontApplyMode.isEnabled(normalizedMode)) {
                saved = store.setTargetFontScalePercent(packageName, percent) && saved
                saved = store.setTargetFontApplyMode(packageName, normalizedMode) && saved
                FontRuntimePropertySyncer.publishTargetAsync(
                    packageName,
                    percent,
                    normalizedMode,
                    store.isHyperOsFlutterFontHookEnabled,
                    userId,
                )
            } else {
                saved = store.clearTargetFontScalePercent(packageName) && saved
                FontRuntimePropertySyncer.clearTargetAsync(packageName, userId)
            }
        }
        if (intent.hasExtra(EXTRA_SAFE_MODE)) {
            saved = store.setSystemServerSafeModeEnabled(
                intent.getBooleanExtra(
                    EXTRA_SAFE_MODE,
                    true
                )
            ) && saved
        }
        if (intent.hasExtra(EXTRA_LOG_ENABLED)) {
            val enabled = intent.getBooleanExtra(EXTRA_LOG_ENABLED, false)
            saved = store.setGlobalLogEnabled(enabled) && saved
            DpisLog.setLoggingEnabled(enabled)
        }
        if (intent.hasExtra(EXTRA_FONT_DEBUG_OVERLAY_ENABLED)) {
            saved = store.setFontDebugOverlayEnabled(
                intent.getBooleanExtra(EXTRA_FONT_DEBUG_OVERLAY_ENABLED, false),
            ) && saved
        }
        val restart = intent.getBooleanExtra(EXTRA_RESTART, false)
        if (restart) restartTargetAsync(context, packageName)
        Log.i(
            TAG,
            "package=$packageName, saved=$saved" +
                    ", viewportWidthDp=${store.getTargetViewportWidthDp(packageName)}" +
                    ", viewportMode=${store.getTargetViewportApplyMode(packageName)}" +
                    ", fontScalePercent=${store.getTargetFontScalePercent(packageName)}" +
                    ", fontMode=${store.getTargetFontApplyMode(packageName)}" +
                    ", systemServerSafeMode=${store.isSystemServerSafeModeEnabled()}" +
                    ", logEnabled=${store.isGlobalLogEnabled()}" +
                    ", fontDebugOverlayEnabled=${store.isFontDebugOverlayEnabled}" +
                    ", restart=$restart",
        )
    }

    private fun restartTargetAsync(context: Context, packageName: String) {
        val launcher = RootAppProcessLauncher(context.applicationContext)
        Thread({
            try {
                Thread.sleep(400L)
                launcher.restart(packageName)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }, "DPIS-debug-config-restarter").apply { isDaemon = true }.start()
    }
}
