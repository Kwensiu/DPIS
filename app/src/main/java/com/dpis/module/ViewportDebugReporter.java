package com.dpis.module;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import java.lang.reflect.Method;

final class ViewportDebugReporter {
    private static volatile String lastSummary;

    private ViewportDebugReporter() {
    }

    static void report(DpiConfigStore store,
                       String packageName,
                       String viewportMode,
                       int sourceWidthDp,
                       int sourceHeightDp,
                       int sourceDensityDpi,
                       ViewportOverride.Result result,
                       VirtualDisplayOverride.Result sharedResult,
                       boolean configurationApplied) {
        if (result == null || packageName == null || packageName.isEmpty()) {
            return;
        }
        String modeText = ViewportApplyMode.FIELD_REWRITE.equals(viewportMode) ? "替换" : "伪装";
        int targetWidthPx = sharedResult != null ? sharedResult.widthPx : -1;
        int targetHeightPx = sharedResult != null ? sharedResult.heightPx : -1;
        String summary = "视口 " + packageName
                + " | " + modeText
                + " | dp " + sourceWidthDp + "x" + sourceHeightDp + " -> "
                + result.widthDp + "x" + result.heightDp
                + " | dpi " + sourceDensityDpi + " -> " + result.densityDpi
                + " | px " + targetWidthPx + "x" + targetHeightPx
                + " | cfg=" + (configurationApplied ? "on" : "off");
        String previous = lastSummary;
        if (summary.equals(previous)) {
            return;
        }
        Context context = resolveContext();
        if (context == null) {
            return;
        }
        Intent intent = new Intent(FontDebugStatsStore.ACTION_STATS_UPDATE);
        intent.setPackage(context.getPackageName());
        intent.putExtra(FontDebugStatsStore.EXTRA_VIEWPORT_DEBUG_SUMMARY, summary);
        try {
            context.sendBroadcast(intent);
            lastSummary = summary;
        } catch (Throwable ignored) {
        }
    }

    private static Context resolveContext() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentApplication = activityThread.getDeclaredMethod("currentApplication");
            Object app = currentApplication.invoke(null);
            if (app instanceof Application application) {
                return application;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
