package com.dpis.module;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

final class ModuleRuntimeReloadAdvisor {
    private static final String PREFS_NAME = "dpis.module_runtime";
    private static final String KEY_ACKED_UPDATE_TIME = "acked_update_time";
    private static final long RUNTIME_LOAD_TOLERANCE_MS = 2_000L;

    private ModuleRuntimeReloadAdvisor() {
    }

    static boolean shouldShowReloadAdvice(Context context) {
        long lastUpdateTime = getLastUpdateTime(context);
        long systemServerLoadedAt = ModuleRuntimeStateReporter.getSystemServerLoadedAt();
        if (!isSystemServerRuntimeOlderThanInstall(lastUpdateTime, systemServerLoadedAt)) {
            return false;
        }
        SharedPreferences preferences = getPreferences(context);
        return preferences.getLong(KEY_ACKED_UPDATE_TIME, 0L) != lastUpdateTime;
    }

    static void markReloadAdviceAcknowledged(Context context) {
        long lastUpdateTime = getLastUpdateTime(context);
        if (lastUpdateTime <= 0L) {
            return;
        }
        getPreferences(context).edit()
                .putLong(KEY_ACKED_UPDATE_TIME, lastUpdateTime)
                .apply();
    }

    static boolean isSystemServerRuntimeOlderThanInstall(long lastUpdateTime,
                                                         long systemServerLoadedAt) {
        if (lastUpdateTime <= 0L || systemServerLoadedAt <= 0L) {
            return false;
        }
        return lastUpdateTime > systemServerLoadedAt + RUNTIME_LOAD_TOLERANCE_MS;
    }

    private static long getLastUpdateTime(Context context) {
        if (context == null) {
            return 0L;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            return 0L;
        }
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
