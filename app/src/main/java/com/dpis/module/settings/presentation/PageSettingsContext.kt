package com.dpis.module.settings.presentation

import android.content.Context
import com.dpis.module.settings.PageSettingsStore

private fun pagePrefs(context: Context) =
    context.getSharedPreferences(PageSettingsStore.PREFS_NAME, 0)

fun PageSettingsStore.isHomeEditButtonVisible(context: Context): Boolean =
    isHomeEditButtonVisible(pagePrefs(context))

fun PageSettingsStore.setHomeEditButtonVisible(context: Context, value: Boolean) {
    setHomeEditButtonVisible(pagePrefs(context), value)
}

fun PageSettingsStore.getDefaultStartupPage(context: Context): String =
    getDefaultStartupPage(pagePrefs(context))

fun PageSettingsStore.setDefaultStartupPage(context: Context, value: String) {
    setDefaultStartupPage(pagePrefs(context), value)
}

fun PageSettingsStore.isPredictiveBackEnabled(context: Context): Boolean =
    isPredictiveBackEnabled(pagePrefs(context))

fun PageSettingsStore.setPredictiveBackEnabled(context: Context, value: Boolean) {
    setPredictiveBackEnabled(pagePrefs(context), value)
}

fun PageSettingsStore.isHomeActivationDetectionEnabled(context: Context): Boolean =
    isHomeActivationDetectionEnabled(pagePrefs(context))

fun PageSettingsStore.setHomeActivationDetectionEnabled(context: Context, value: Boolean) {
    setHomeActivationDetectionEnabled(pagePrefs(context), value)
}

fun PageSettingsStore.getWorkspaceOrder(context: Context): List<String> =
    getWorkspaceOrder(pagePrefs(context))

fun PageSettingsStore.setWorkspaceOrder(context: Context, order: List<String>) {
    setWorkspaceOrder(pagePrefs(context), order)
}

fun PageSettingsStore.getHiddenWorkspaces(context: Context): Set<String> =
    getHiddenWorkspaces(pagePrefs(context))

fun PageSettingsStore.setWorkspaceVisible(context: Context, page: String, visible: Boolean) {
    setWorkspaceVisible(pagePrefs(context), page, visible)
}
