package com.dpis.module.settings

import android.content.Context

/** Stores the user-selected application appearance independently from system night mode. */
object ThemeModeStore {
    data class AppearancePreferences(
        val mode: String,
        val dynamicColorEnabled: Boolean,
        val themeColor: String,
        val paletteStyle: String,
        val colorSpecification: String,
    )
    const val FOLLOW_SYSTEM = "follow_system"
    const val LIGHT = "light"
    const val DARK = "dark"

    private const val PREFERENCES_NAME = "dpis_theme"
    private const val KEY_MODE = "mode"
    private const val KEY_DYNAMIC_COLOR_ENABLED = "dynamic_color_enabled"
    private const val KEY_THEME_COLOR = "theme_color"
    private const val KEY_PALETTE_STYLE = "palette_style"
    private const val KEY_COLOR_SPECIFICATION = "color_specification"

    const val COLOR_PURPLE = "purple"
    const val COLOR_PINK = "pink"
    const val COLOR_RED = "red"
    const val COLOR_ORANGE = "orange"
    const val COLOR_AMBER = "amber"
    const val COLOR_YELLOW = "yellow"
    const val COLOR_LIME = "lime"
    const val COLOR_GREEN = "green"
    const val COLOR_CYAN = "cyan"
    const val COLOR_TEAL = "teal"
    const val COLOR_LIGHT_BLUE = "light_blue"
    const val COLOR_BLUE = "blue"
    const val COLOR_INDIGO = "indigo"
    const val COLOR_DEEP_PURPLE = "deep_purple"
    const val COLOR_BLUE_GREY = "blue_grey"
    const val COLOR_BROWN = "brown"
    const val COLOR_GREY = "grey"
    const val DEFAULT_STATIC_THEME_COLOR = COLOR_PURPLE
    const val STYLE_TONAL_SPOT = "tonal_spot"
    const val STYLE_NEUTRAL = "neutral"
    const val STYLE_VIBRANT = "vibrant"
    const val STYLE_EXPRESSIVE = "expressive"
    const val STYLE_RAINBOW = "rainbow"
    const val STYLE_FRUIT_SALAD = "fruit_salad"
    const val STYLE_MONOCHROME = "monochrome"
    const val STYLE_FIDELITY = "fidelity"
    const val STYLE_CONTENT = "content"
    const val SPEC_2021 = "2021"
    const val SPEC_2025 = "2025"

    @JvmStatic
    fun getMode(context: Context): String = context
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getString(KEY_MODE, FOLLOW_SYSTEM)
        .takeIf { it == FOLLOW_SYSTEM || it == LIGHT || it == DARK }
        ?: FOLLOW_SYSTEM

    @JvmStatic
    fun setMode(context: Context, mode: String) {
        require(mode == FOLLOW_SYSTEM || mode == LIGHT || mode == DARK) {
            "Unsupported theme mode: $mode"
        }
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode)
            .commit()
    }

    /** Dynamic color remains the default until the user explicitly chooses the DPIS palette. */
    @JvmStatic
    fun isDynamicColorEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val storedValue = if (preferences.contains(KEY_DYNAMIC_COLOR_ENABLED)) {
            preferences.getBoolean(KEY_DYNAMIC_COLOR_ENABLED, true)
        } else {
            null
        }
        return resolveDynamicColorEnabled(storedValue)
    }

    @JvmStatic
    fun resolveDynamicColorEnabled(storedValue: Boolean?): Boolean = storedValue ?: true

    @JvmStatic
    fun setDynamicColorEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DYNAMIC_COLOR_ENABLED, enabled)
            .commit()
    }

    @JvmStatic
    fun getThemeColor(context: Context): String = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getString(KEY_THEME_COLOR, DEFAULT_STATIC_THEME_COLOR)
        .takeIf { it in supportedThemeColors() }
        ?: DEFAULT_STATIC_THEME_COLOR

    @JvmStatic
    fun setThemeColor(context: Context, color: String) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_THEME_COLOR, color).commit()
    }

    @JvmStatic
    fun getPaletteStyle(context: Context): String = (
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PALETTE_STYLE, STYLE_TONAL_SPOT) ?: STYLE_TONAL_SPOT
        ).let { if (it == "clock" || it == "clock_vibrant") STYLE_TONAL_SPOT else it }

    @JvmStatic
    fun setPaletteStyle(context: Context, style: String) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_PALETTE_STYLE, style).commit()
    }

    @JvmStatic
    fun getColorSpecification(context: Context): String = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getString(KEY_COLOR_SPECIFICATION, SPEC_2025)
        .let { if (it == "expressive") SPEC_2025 else if (it == "material_3") SPEC_2021 else it }
        .takeIf { it == SPEC_2021 || it == SPEC_2025 }
        ?: SPEC_2025

    @JvmStatic
    fun setColorSpecification(context: Context, specification: String) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_COLOR_SPECIFICATION, specification).commit()
    }

    @JvmStatic
    fun getAppearance(context: Context): AppearancePreferences = AppearancePreferences(
        mode = getMode(context),
        dynamicColorEnabled = isDynamicColorEnabled(context),
        themeColor = getThemeColor(context),
        paletteStyle = getPaletteStyle(context),
        colorSpecification = getColorSpecification(context),
    )

    @JvmStatic
    fun isDarkTheme(context: Context, systemDarkTheme: Boolean): Boolean =
        resolveDarkTheme(getMode(context), systemDarkTheme)

    @JvmStatic
    fun resolveDarkTheme(mode: String, systemDarkTheme: Boolean): Boolean = when (mode) {
        LIGHT -> false
        DARK -> true
        else -> systemDarkTheme
    }

    @JvmStatic
    fun supportedThemeColors(): Set<String> = setOf(
        COLOR_PURPLE,
        COLOR_PINK,
        COLOR_RED,
        COLOR_ORANGE,
        COLOR_AMBER,
        COLOR_YELLOW,
        COLOR_LIME,
        COLOR_GREEN,
        COLOR_CYAN,
        COLOR_TEAL,
        COLOR_LIGHT_BLUE,
        COLOR_BLUE,
        COLOR_INDIGO,
        COLOR_DEEP_PURPLE,
        COLOR_BLUE_GREY,
        COLOR_BROWN,
        COLOR_GREY,
    )
}
