package com.dpis.module.applist;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Process;

public final class ForegroundPackageResolver {
    private static final long LOOKBACK_MS = 30_000L;
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";

    private ForegroundPackageResolver() {
    }

    public static boolean hasUsageAccess(Context context) {
        if (context == null) {
            return false;
        }
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) {
            return false;
        }
        try {
            return appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.getPackageName()) == AppOpsManager.MODE_ALLOWED;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static String resolve(Context context) {
        if (context == null || !hasUsageAccess(context)) {
            return null;
        }
        UsageStatsManager usageStats = (UsageStatsManager) context.getSystemService(
                Context.USAGE_STATS_SERVICE);
        if (usageStats == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        UsageEvents events;
        try {
            events = usageStats.queryEvents(now - LOOKBACK_MS, now);
        } catch (RuntimeException exception) {
            return null;
        }
        if (events == null) {
            return null;
        }
        String packageName = null;
        UsageEvents.Event event = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                packageName = event.getPackageName();
            }
        }
        return isUsablePackage(context, packageName) ? packageName : null;
    }

    private static boolean isUsablePackage(Context context, String packageName) {
        return packageName != null
                && !packageName.isBlank()
                && !packageName.equals(context.getPackageName())
                && !SYSTEM_UI_PACKAGE.equals(packageName);
    }
}
