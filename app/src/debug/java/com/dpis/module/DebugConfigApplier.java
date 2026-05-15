package com.dpis.module;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;

final class DebugConfigApplier {
    static final String ACTION_SET_PACKAGE_CONFIG =
            "io.github.kwensiu.dpis.DEBUG_SET_PACKAGE_CONFIG";
    static final String TAG = "DPIS_DEBUG_CONFIG";

    private static final String EXTRA_PACKAGE = "package";
    private static final String EXTRA_VIEWPORT_WIDTH_DP = "viewport_width_dp";
    private static final String EXTRA_VIEWPORT_MODE = "viewport_mode";
    private static final String EXTRA_FONT_SCALE_PERCENT = "font_scale_percent";
    private static final String EXTRA_FONT_MODE = "font_mode";
    private static final String EXTRA_RESTART = "restart";
    private static final String EXTRA_SAFE_MODE = "system_server_safe_mode";
    private static final String EXTRA_LOG_ENABLED = "log_enabled";
    private static final String EXTRA_FONT_DEBUG_OVERLAY_ENABLED = "font_debug_overlay_enabled";

    private DebugConfigApplier() {
    }

    static void apply(Context context, Intent intent, boolean requireActionMatch) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        if (intent == null) {
            return;
        }
        if (requireActionMatch && !ACTION_SET_PACKAGE_CONFIG.equals(intent.getAction())) {
            return;
        }
        String packageName = intent.getStringExtra(EXTRA_PACKAGE);
        if (packageName == null || packageName.isBlank()) {
            Log.w(TAG, "ignored: missing package");
            return;
        }
        DpiConfigStore store = DpisApplication.getConfigStore();
        boolean saved = true;

        if (intent.hasExtra(EXTRA_VIEWPORT_WIDTH_DP)) {
            int widthDp = intent.getIntExtra(EXTRA_VIEWPORT_WIDTH_DP, 0);
            String mode = intent.getStringExtra(EXTRA_VIEWPORT_MODE);
            String normalizedMode = ViewportApplyMode.normalize(mode);
            if (widthDp > 0 && ViewportApplyMode.isEnabled(normalizedMode)) {
                saved &= store.setTargetViewportWidthDp(packageName, widthDp);
                saved &= store.setTargetViewportApplyMode(packageName, normalizedMode);
                ViewportPropertySyncer.publishTargetAsync(packageName, widthDp, normalizedMode);
            } else {
                saved &= store.clearTargetViewportWidthDp(packageName);
                ViewportPropertySyncer.clearTargetAsync(packageName);
            }
        }

        if (intent.hasExtra(EXTRA_FONT_SCALE_PERCENT)) {
            int fontScalePercent = intent.getIntExtra(EXTRA_FONT_SCALE_PERCENT, 0);
            String mode = intent.getStringExtra(EXTRA_FONT_MODE);
            String normalizedMode = FontApplyMode.normalize(mode);
            if (fontScalePercent > 0 && FontApplyMode.isEnabled(normalizedMode)) {
                saved &= store.setTargetFontScalePercent(packageName, fontScalePercent);
                saved &= store.setTargetFontApplyMode(packageName, normalizedMode);
                FontRuntimePropertySyncer.publishTargetAsync(packageName, fontScalePercent,
                        normalizedMode, store.isHyperOsFlutterFontHookEnabled());
            } else {
                saved &= store.clearTargetFontScalePercent(packageName);
                FontRuntimePropertySyncer.clearTargetAsync(packageName);
            }
        }

        if (intent.hasExtra(EXTRA_SAFE_MODE)) {
            saved &= store.setSystemServerSafeModeEnabled(
                    intent.getBooleanExtra(EXTRA_SAFE_MODE, true));
        }
        if (intent.hasExtra(EXTRA_LOG_ENABLED)) {
            boolean loggingEnabled = intent.getBooleanExtra(EXTRA_LOG_ENABLED, false);
            saved &= store.setGlobalLogEnabled(loggingEnabled);
            DpisLog.setLoggingEnabled(loggingEnabled);
        }
        if (intent.hasExtra(EXTRA_FONT_DEBUG_OVERLAY_ENABLED)) {
            saved &= store.setFontDebugOverlayEnabled(
                    intent.getBooleanExtra(EXTRA_FONT_DEBUG_OVERLAY_ENABLED, false));
        }

        boolean restart = intent.getBooleanExtra(EXTRA_RESTART, false);
        if (restart) {
            restartTargetAsync(packageName);
        }
        Log.i(TAG, "package=" + packageName
                + ", saved=" + saved
                + ", viewportWidthDp=" + store.getTargetViewportWidthDp(packageName)
                + ", viewportMode=" + store.getTargetViewportApplyMode(packageName)
                + ", fontScalePercent=" + store.getTargetFontScalePercent(packageName)
                + ", fontMode=" + store.getTargetFontApplyMode(packageName)
                + ", systemServerSafeMode=" + store.isSystemServerSafeModeEnabled()
                + ", logEnabled=" + store.isGlobalLogEnabled()
                + ", fontDebugOverlayEnabled=" + store.isFontDebugOverlayEnabled()
                + ", restart=" + restart);
    }

    private static void restartTargetAsync(String packageName) {
        Thread thread = new Thread(() -> {
            sleepQuietly(400L);
            runRootCommand("am force-stop " + shellQuote(packageName)
                    + "; monkey -p " + shellQuote(packageName)
                    + " -c android.intent.category.LAUNCHER 1");
        }, "DPIS-debug-config-restarter");
        thread.setDaemon(true);
        thread.start();
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static void runRootCommand(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            process.waitFor();
        } catch (IOException ignored) {
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String shellQuote(String value) {
        if (value == null || value.isEmpty()) {
            return "''";
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
