package com.dpis.module.settings

import com.dpis.module.SettingsUiState

/** Settings presentation capabilities exposed to the main workspace shell. */
interface SettingsActions {
    fun state(): SettingsUiState
    fun setHooks(enabled: Boolean)
    fun setSafeMode(enabled: Boolean)
    fun setGlobalLog(enabled: Boolean)
    fun openLogs()
    fun setLauncherHidden(hidden: Boolean)
    fun openFontDebug()
    fun openFontLibrary()
    fun openExperimental()
    fun openTheme()
    fun setLanguage(tag: String)
    fun openLanguage()
    fun openBackup()
    fun clearCache()
    fun openAbout()
    fun openDonate()
}
