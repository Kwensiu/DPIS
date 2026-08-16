package com.dpis.module.runtime

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager

object ModuleRuntimeReloadAdvisor {
    private const val PREFS_NAME = "dpis.module_runtime"
    private const val KEY_SHOWN_UPDATE_TIME = "shown_update_time"
    private const val RUNTIME_LOAD_TOLERANCE_MS = 2_000L

    @JvmStatic
    fun shouldShowReloadAdvice(context: Context?): Boolean {
        val lastUpdateTime = getLastUpdateTime(context)
        val systemServerLoadedAt = ModuleRuntimeStateReporter.getSystemServerLoadedAt()
        if (!isSystemServerRuntimeOlderThanInstall(lastUpdateTime, systemServerLoadedAt)) {
            return false
        }
        return getPreferences(context).getLong(KEY_SHOWN_UPDATE_TIME, 0L) != lastUpdateTime
    }

    @JvmStatic
    fun markReloadAdviceShown(context: Context?): Boolean {
        val lastUpdateTime = getLastUpdateTime(context)
        if (lastUpdateTime <= 0L) {
            return false
        }
        // The notice can be followed immediately by a configuration recreation.
        // Commit so the next Activity instance observes that it was already shown.
        return getPreferences(context)
            .edit()
            .putLong(KEY_SHOWN_UPDATE_TIME, lastUpdateTime)
            .commit()
    }

    @JvmStatic
    fun isSystemServerRuntimeOlderThanInstall(
        lastUpdateTime: Long,
        systemServerLoadedAt: Long,
    ): Boolean = lastUpdateTime > 0L &&
        systemServerLoadedAt > 0L &&
        lastUpdateTime > systemServerLoadedAt + RUNTIME_LOAD_TOLERANCE_MS

    private fun getLastUpdateTime(context: Context?): Long {
        val appContext = context ?: return 0L
        return try {
            appContext.packageManager
                .getPackageInfo(appContext.packageName, 0)
                .lastUpdateTime
        } catch (_: PackageManager.NameNotFoundException) {
            0L
        } catch (_: RuntimeException) {
            0L
        }
    }

    private fun getPreferences(context: Context?): SharedPreferences =
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?: error("A context is required after resolving the install timestamp")
}
