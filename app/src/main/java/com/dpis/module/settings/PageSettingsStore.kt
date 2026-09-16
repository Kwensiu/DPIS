package com.dpis.module.settings

import android.content.SharedPreferences

/** Persists page navigation and home presentation preferences independently of theme settings. */
object PageSettingsStore {
    const val HOME = "HOME"
    internal const val PREFS_NAME = "dpis_page"
    private const val SHOW_EDIT = "show_home_edit_button"
    private const val START_PAGE = "default_startup_page"
    private const val PREDICTIVE_BACK = "predictive_back_enabled"
    private const val HOME_ACTIVATION_DETECTION = "home_activation_detection_enabled"
    private const val ORDER = "workspace_order"
    private const val HIDDEN = "workspace_hidden"
    private val validPages = setOf("APP", HOME, "TEMPLATE", "TOOLS", "SETTINGS")

    fun isHomeEditButtonVisible(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(SHOW_EDIT, true)

    fun setHomeEditButtonVisible(prefs: SharedPreferences, value: Boolean) {
        prefs.edit().putBoolean(SHOW_EDIT, value).apply()
    }

    fun getDefaultStartupPage(prefs: SharedPreferences): String = prefs
        .getString(START_PAGE, HOME)
        ?.uppercase()
        ?.takeIf(validPages::contains)
        ?: HOME

    fun setDefaultStartupPage(prefs: SharedPreferences, value: String) {
        require(value in validPages)
        prefs.edit().putString(START_PAGE, value).apply()
    }

    fun isPredictiveBackEnabled(prefs: SharedPreferences): Boolean =
        resolvePredictiveBackEnabled(storedFlag(prefs, PREDICTIVE_BACK))

    fun setPredictiveBackEnabled(prefs: SharedPreferences, value: Boolean) {
        prefs.edit().putBoolean(PREDICTIVE_BACK, value).commit()
    }

    @JvmStatic
    fun resolvePredictiveBackEnabled(stored: Boolean?): Boolean = stored ?: true

    fun isHomeActivationDetectionEnabled(prefs: SharedPreferences): Boolean =
        resolveHomeActivationDetectionEnabled(storedFlag(prefs, HOME_ACTIVATION_DETECTION))

    fun setHomeActivationDetectionEnabled(prefs: SharedPreferences, value: Boolean) {
        prefs.edit().putBoolean(HOME_ACTIVATION_DETECTION, value).apply()
    }

    @JvmStatic
    fun resolveHomeActivationDetectionEnabled(stored: Boolean?): Boolean = stored ?: true

    fun getWorkspaceOrder(prefs: SharedPreferences): List<String> = prefs.getString(ORDER, null)
        ?.split(',')
        ?.filter(validPages::contains)
        ?.distinct()
        .orEmpty()
        .let { stored -> (stored + listOf("APP", "TEMPLATE", HOME, "TOOLS", "SETTINGS")).distinct() }

    fun setWorkspaceOrder(prefs: SharedPreferences, order: List<String>) {
        prefs.edit()
            .putString(ORDER, order.filter(validPages::contains).distinct().joinToString(","))
            .apply()
    }

    fun getHiddenWorkspaces(prefs: SharedPreferences): Set<String> = prefs
        .getStringSet(HIDDEN, emptySet())
        .orEmpty()
        .filter(validPages::contains)
        .toSet()

    fun setWorkspaceVisible(prefs: SharedPreferences, page: String, visible: Boolean) {
        if (page == "SETTINGS") return
        val hidden = getHiddenWorkspaces(prefs).toMutableSet()
        if (visible) hidden.remove(page) else hidden.add(page)
        prefs.edit().putStringSet(HIDDEN, hidden).apply()
    }

    private fun storedFlag(prefs: SharedPreferences, key: String): Boolean? =
        if (prefs.contains(key)) prefs.getBoolean(key, true) else null
}
