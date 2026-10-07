package com.dpis.module.quickconfig

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

object ForegroundAppResolver {
    private const val LOOKBACK_MS = 30_000L
    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"

    @JvmStatic
    fun hasUsageAccess(context: Context?): Boolean {
        if (context == null) return false
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        return try {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            ) == AppOpsManager.MODE_ALLOWED
        } catch (_: RuntimeException) {
            false
        }
    }

    @JvmStatic
    fun resolve(context: Context?): String? {
        if (context == null || !hasUsageAccess(context)) return null
        val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val events = try {
            val now = System.currentTimeMillis()
            usageStats.queryEvents(now - LOOKBACK_MS, now)
        } catch (_: RuntimeException) {
            return null
        } ?: return null

        var packageName: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                packageName = event.packageName
            }
        }
        return packageName.takeIf { isUsablePackage(context, it) }
    }

    private fun isUsablePackage(context: Context, packageName: String?): Boolean =
        !packageName.isNullOrBlank()
            && packageName != context.packageName
            && packageName != SYSTEM_UI_PACKAGE
}
