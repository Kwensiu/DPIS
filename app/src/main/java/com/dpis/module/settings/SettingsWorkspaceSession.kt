package com.dpis.module.settings

import android.app.Activity
import android.content.Intent
import android.view.View
import com.dpis.module.LocalizedActivity
import com.dpis.module.SettingsUiState
import com.dpis.module.SystemServerSettingsPageController

/**
 * Java-facing adapter that owns the settings controller's Activity session while MainActivity
 * remains the platform shell. Legacy View and Compose presentations share one workflow
 * controller, but only one root is active for a given Activity instance.
 */
class SettingsWorkspaceSession(
    private val activity: LocalizedActivity,
    private val onComposeStateChanged: Runnable,
    private val onOpenLogs: Runnable,
) : SettingsActions {
    companion object {
        /** Stable Java entry point that avoids exposing Kotlin function types or internal classes. */
        @JvmStatic
        fun create(
            activity: Activity,
            onComposeStateChanged: Runnable,
            onOpenLogs: Runnable,
        ): SettingsWorkspaceSession {
            return SettingsWorkspaceSession(
                activity as LocalizedActivity,
                onComposeStateChanged,
                onOpenLogs,
            )
        }
    }

    private var controller: SystemServerSettingsPageController? = null
    private var composePresentationStarted = false

    fun bindLegacy(root: View) {
        if (controller != null) return
        controller = SystemServerSettingsPageController(activity, root).also { it.bind() }
    }

    fun ensureComposeController(): SystemServerSettingsPageController {
        val current = controller ?: SystemServerSettingsPageController(activity, null).also {
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

    override fun openLogs() {
        onOpenLogs.run()
    }

    override fun setLauncherHidden(hidden: Boolean) {
        ensureComposeController().setLauncherHiddenFromPresentation(hidden)
    }

    override fun openFontDebug() {
        ensureComposeController().showFontDebugFromPresentation()
    }

    override fun openFontLibrary() {
        ensureComposeController().showFontLibraryFromPresentation()
    }

    override fun openExperimental() {
        ensureComposeController().showExperimentalSettingsFromPresentation()
    }

    override fun openTheme() {
        ensureComposeController().showThemeSettingsFromPresentation()
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

    override fun clearCache() {
        ensureComposeController().clearCacheFromPresentation()
    }

    override fun openAbout() {
        ensureComposeController().showAboutFromPresentation()
    }

    override fun openDonate() {
        ensureComposeController().showDonateFromPresentation()
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
