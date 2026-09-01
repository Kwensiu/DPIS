package com.dpis.module.settings

import android.content.Context

/** Persists page navigation and home presentation preferences independently of theme settings. */
object PageSettingsStore {
    const val HOME = "HOME"
    private const val NAME = "dpis_page"
    private const val SHOW_EDIT = "show_home_edit_button"
    private const val START_PAGE = "default_startup_page"
    private const val ORDER = "workspace_order"
    private const val HIDDEN = "workspace_hidden"
    private val validPages = setOf("APP", HOME, "TEMPLATE", "TOOLS", "SETTINGS")

    @JvmStatic
    fun isHomeEditButtonVisible(context: Context): Boolean = context
        .getSharedPreferences(NAME, 0).getBoolean(SHOW_EDIT, true)

    @JvmStatic
    fun setHomeEditButtonVisible(context: Context, value: Boolean) {
        context.getSharedPreferences(NAME, 0).edit().putBoolean(SHOW_EDIT, value).apply()
    }

    @JvmStatic
    fun getDefaultStartupPage(context: Context): String = context
        .getSharedPreferences(NAME, 0).getString(START_PAGE, HOME)
        ?.uppercase()?.takeIf(validPages::contains) ?: HOME

    @JvmStatic
    fun setDefaultStartupPage(context: Context, value: String) {
        require(value in validPages)
        context.getSharedPreferences(NAME, 0).edit().putString(START_PAGE, value).apply()
    }
    @JvmStatic fun getWorkspaceOrder(context: Context): List<String> = context.getSharedPreferences(NAME, 0)
        .getString(ORDER, null)?.split(',')?.filter(validPages::contains)?.distinct().orEmpty()
        .let { stored -> (stored + listOf("APP", "TEMPLATE", HOME, "TOOLS", "SETTINGS")).distinct() }
    @JvmStatic
    fun setWorkspaceOrder(context: Context, order: List<String>) {
        context.getSharedPreferences(NAME, 0).edit()
            .putString(ORDER, order.filter(validPages::contains).distinct().joinToString(","))
            .apply()
    }

    @JvmStatic
    fun getHiddenWorkspaces(context: Context): Set<String> = context
        .getSharedPreferences(NAME, 0).getStringSet(HIDDEN, emptySet()).orEmpty()
        .filter(validPages::contains).toSet()

    @JvmStatic
    fun setWorkspaceVisible(context: Context, page: String, visible: Boolean) {
        if (page == "SETTINGS") return
        val hidden = getHiddenWorkspaces(context).toMutableSet()
        if (visible) hidden.remove(page) else hidden.add(page)
        context.getSharedPreferences(NAME, 0).edit().putStringSet(HIDDEN, hidden).apply()
    }
}
