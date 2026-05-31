package com.dpis.module;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

final class AppProcessViewportStateSeeder {
    private AppProcessViewportStateSeeder() {
    }

    static void seedAbsoluteTarget(String packageName,
                                   ViewportTargetSpec targetSpec,
                                   String requestedMode,
                                   boolean systemHooksEnabled) {
        try {
            Resources resources = Resources.getSystem();
            seedAbsoluteTarget(packageName, targetSpec, requestedMode, systemHooksEnabled,
                    resources.getConfiguration(),
                    resources.getDisplayMetrics());
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_VIEWPORT app-process state seed failed: package=" + packageName,
                    throwable);
        }
    }

    static ViewportRuntimeRecord seedAbsoluteTarget(String packageName,
                                                    ViewportTargetSpec targetSpec,
                                                    String requestedMode,
                                                    boolean systemHooksEnabled,
                                                    Configuration config,
                                                    DisplayMetrics metrics) {
        if (packageName == null || packageName.isBlank()
                || config == null
                || metrics == null
                || targetSpec == null) {
            return null;
        }
        if (!targetSpec.isEnabled() || !targetSpec.isAbsoluteDp()) {
            return null;
        }
        String mode = EffectiveModeResolver.resolveViewportMode(
                requestedMode,
                systemHooksEnabled);
        if (ViewportApplyMode.OFF.equals(mode)) {
            return null;
        }
        int sourceWidthDp = config.screenWidthDp;
        int sourceHeightDp = config.screenHeightDp;
        int sourceSmallestWidthDp = config.smallestScreenWidthDp;
        int sourceWidthPx = metrics.widthPixels;
        int sourceHeightPx = metrics.heightPixels;
        if ((sourceWidthDp <= 0 || sourceHeightDp <= 0 || sourceSmallestWidthDp <= 0)
                && metrics.densityDpi > 0
                && sourceWidthPx > 0
                && sourceHeightPx > 0) {
            float density = DensityOverride.densityFromDpi(metrics.densityDpi);
            sourceWidthDp = Math.max(1, Math.round(sourceWidthPx / density));
            sourceHeightDp = Math.max(1, Math.round(sourceHeightPx / density));
            sourceSmallestWidthDp = Math.min(sourceWidthDp, sourceHeightDp);
        }
        VirtualDisplayOverride.Result virtualDisplay =
                VirtualDisplayPlan.deriveAbsoluteResultFromPhysicalPixels(
                        sourceWidthDp,
                        sourceHeightDp,
                        sourceSmallestWidthDp,
                        sourceWidthPx,
                        sourceHeightPx,
                        targetSpec.absoluteWidthDp());
        if (virtualDisplay == null) {
            return null;
        }
        ViewportOverride.Result viewportResult = new ViewportOverride.Result(
                virtualDisplay.widthDp,
                virtualDisplay.heightDp,
                virtualDisplay.smallestWidthDp,
                virtualDisplay.densityDpi);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.systemDisplayInfo(
                sourceWidthDp,
                sourceHeightDp,
                sourceSmallestWidthDp,
                metrics.densityDpi,
                sourceWidthPx,
                sourceHeightPx);
        ViewportRuntimeRecord record = VirtualDisplayState.publish(
                packageName,
                targetSpec,
                source,
                viewportResult,
                virtualDisplay,
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);
        DpisLog.i("DPIS_VIEWPORT app-process state seeded: package=" + packageName
                + ", targetWidthDp=" + targetSpec.absoluteWidthDp()
                + ", source=wDp=" + sourceWidthDp
                + ",hDp=" + sourceHeightDp
                + ",swDp=" + sourceSmallestWidthDp
                + ",dpi=" + metrics.densityDpi
                + ",wPx=" + sourceWidthPx
                + ",hPx=" + sourceHeightPx
                + ", result=wDp=" + virtualDisplay.widthDp
                + ",hDp=" + virtualDisplay.heightDp
                + ",swDp=" + virtualDisplay.smallestWidthDp
                + ",dpi=" + virtualDisplay.densityDpi
                + ",wPx=" + virtualDisplay.widthPx
                + ",hPx=" + virtualDisplay.heightPx);
        return record;
    }
}
