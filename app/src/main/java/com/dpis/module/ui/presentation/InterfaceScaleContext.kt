package com.dpis.module.ui.presentation

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.settings.InterfaceScaleStore
import com.dpis.module.ui.WatchUiMode

fun wrapInterfaceScaleContext(context: Context): Context {
    val unscaledDensityDpi = unscaledDensityDpi(context)
    if (unscaledDensityDpi <= 0) {
        return context
    }
    val store = InterfaceScaleStore(context)
    val target = AppUiScaleManager.targetDensityDpi(
        unscaledDensityDpi,
        AppUiScaleManager.effectiveScalePercent(
            store,
            WatchUiMode.shouldUseCompactUi(context),
        ),
    )
    val configuration = context.resources.configuration
    val currentDensityDpi = if (configuration.densityDpi > 0) {
        configuration.densityDpi
    } else {
        context.resources.displayMetrics.densityDpi
    }
    if (currentDensityDpi == target) {
        return context
    }
    val next = Configuration(configuration)
    next.densityDpi = target
    return context.createConfigurationContext(next)
}

private fun unscaledDensityDpi(context: Context): Int {
    val app = context.applicationContext
    if (app != null) {
        val dpi = app.resources.displayMetrics.densityDpi
        if (dpi > 0) {
            return dpi
        }
    }
    val systemDpi = Resources.getSystem().displayMetrics.densityDpi
    if (systemDpi > 0) {
        return systemDpi
    }
    val configuration = context.resources.configuration
    if (configuration.densityDpi > 0) {
        return configuration.densityDpi
    }
    return context.resources.displayMetrics.densityDpi
}
