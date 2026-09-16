package com.dpis.module.settings.presentation

import android.app.Activity
import android.content.Intent
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.settings.SettingsUiState
import com.dpis.module.settings.SettingsActions
import com.dpis.module.ui.SecondaryDestination
import com.dpis.module.ui.presentation.SecondaryNavigation

/**
 * Java-facing adapter that owns the settings controller's Activity session while MainActivity
 * remains the platform shell.
 */
class SettingsWorkspaceSession(
    private val activity: LocalizedActivity,
    private val onComposeStateChanged: Runnable,
    private val secondaryNavigation: SecondaryNavigation,
    private val onConfigurationChanged: Runnable,
) : SettingsActions {
    companion object {
        /** Stable Java entry point that avoids exposing Kotlin function types or internal classes. */
        @JvmStatic
        fun create(
            activity: Activity,
            onComposeStateChanged: Runnable,
            secondaryNavigation: SecondaryNavigation,
            onConfigurationChanged: Runnable,
        ): SettingsWorkspaceSession {
            return SettingsWorkspaceSession(
                activity as LocalizedActivity,
                onComposeStateChanged,
                secondaryNavigation,
                onConfigurationChanged,
            )
        }
    }

    private var controller: SystemServerSettingsPageController? = null
    private var composePresentationStarted = false

    fun ensureComposeController(): SystemServerSettingsPageController {
        val current = controller ?: SystemServerSettingsPageController(
            activity,
            onConfigurationChanged,
        ).also {
            controller = it
        }
        if (!composePresentationStarted) {
            current.startComposePresentation { onComposeStateChanged.run() }
            composePresentationStarted = true
        }
        return current
    }

    override fun state(): SettingsUiState = ensureComposeController().presentationState()

    override fun setHooks(enabled: Boolean) {
        ensureComposeController().setHooksEnabledFromPresentation(enabled)
    }

    override fun setSafeMode(enabled: Boolean) {
        ensureComposeController().setSafeModeFromPresentation(enabled)
    }

    override fun setGlobalLog(enabled: Boolean) {
        ensureComposeController().setGlobalLogFromPresentation(enabled)
    }

    override fun setHomeActivationDetection(enabled: Boolean) {
        ensureComposeController().setHomeActivationDetectionFromPresentation(enabled)
    }

    override fun openLogs() {
        secondaryNavigation.open(SecondaryDestination.Logs)
    }

    override fun setLauncherHidden(hidden: Boolean) {
        ensureComposeController().setLauncherHiddenFromPresentation(hidden)
    }

    override fun openFontDebug() {
        ensureComposeController().showFontDebugFromPresentation()
    }

    override fun openFontLibrary() {
        secondaryNavigation.open(SecondaryDestination.FontLibrary)
    }

    override fun openExperimental() {
        secondaryNavigation.open(SecondaryDestination.Experimental)
    }

    override fun openTheme() {
        secondaryNavigation.open(SecondaryDestination.Theme)
    }

    override fun setLanguage(tag: String) {
        ensureComposeController().setLanguageFromPresentation(tag)
    }

    override fun openLanguage() {
        ensureComposeController().showLanguageFromPresentation()
    }

    override fun openBackup() {
        ensureComposeController().showConfigBackupFromPresentation()
    }

    override fun confirmImport() {
        ensureComposeController().confirmImportFromPresentation()
    }

    override fun dismissImport() {
        ensureComposeController().dismissImportFromPresentation()
    }

    override fun clearCache() {
        ensureComposeController().clearCacheFromPresentation()
    }

    override fun openAbout() {
        secondaryNavigation.open(SecondaryDestination.About)
    }

    override fun openDonate() {
        secondaryNavigation.open(SecondaryDestination.Donate)
    }

    fun onStart() {
        controller?.onStart()
    }

    fun onResume() {
        controller?.onResume()
    }

    fun onStop() {
        controller?.onStop()
    }

    fun onServiceStateChanged() {
        controller?.onServiceStateChanged()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        controller?.onActivityResult(requestCode, resultCode, data)
    }

    fun onDestroy() {
        controller?.stopComposePresentation()
        controller = null
        composePresentationStarted = false
    }
}
