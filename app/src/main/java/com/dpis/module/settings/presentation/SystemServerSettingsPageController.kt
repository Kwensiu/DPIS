package com.dpis.module.settings.presentation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import com.dpis.module.BuildConfig
import com.dpis.module.DpisApplication
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.MainActivity
import com.dpis.module.R
import com.dpis.module.settings.SettingsPresentationController
import com.dpis.module.settings.SettingsUiState

import com.dpis.module.backup.presentation.ConfigBackupHost
import com.dpis.module.fonts.FontDebugDataDiagnostics
import com.dpis.module.fonts.FontDebugDataDiagnostics.NoDataReason
import com.dpis.module.fonts.FontDebugOverlayService
import com.dpis.module.fonts.FontDebugStatsSchema
import com.dpis.module.fonts.FontDebugStatsStore

import com.dpis.module.runtime.delivery.RuntimeConfigDelivery
import com.dpis.module.runtime.delivery.RuntimeDebugPropertySyncer
import com.dpis.module.settings.AppLocaleManager

import com.dpis.module.settings.InterfaceScaleStore
import com.dpis.module.settings.LauncherIconVisibilityStore
import com.dpis.module.settings.PageSettingsStore
import com.dpis.module.settings.SafeCacheCleaner
import com.dpis.module.hooks.SystemFrameworkScope
import com.dpis.module.hooks.SystemHookState
import com.dpis.module.settings.SystemHooksToggleController
import com.dpis.module.settings.SystemHooksToggleController.ScopeGateway

import com.dpis.module.settings.presentation.SettingsComposeDialogs.showBackupActions
import com.dpis.module.ui.compose.FontDebugComposeSheet
import com.dpis.module.ui.compose.FontDebugComposeSheet.show
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.function.Consumer
import kotlin.concurrent.Volatile

/** Java-facing settings workflow controller used by the Compose presentation. */
class SystemServerSettingsPageController(
    private val activity: LocalizedActivity,
    private val onConfigurationChanged: Runnable? = null,
) : DpisApplication.ServiceStateListener {
    private val launcherIconVisibilityStore: LauncherIconVisibilityStore
    private val interfaceScaleStore: InterfaceScaleStore
    private val presentationController: SettingsPresentationController
    private var store: DpisConfigStore? = null

    @Volatile
    private var clearCacheInProgress = false
    private var lastCacheUsage = "0 B"
    private var statsPreferences: SharedPreferences? = null
    private var selectedMode = FontDebugStatsStore.MODE_CHAIN
    private var selectedWindow = FontDebugStatsStore.WINDOW_ALL

    private var fontDebugDialog: FontDebugComposeSheet.Handle? = null
    private var hooksToggleController: SystemHooksToggleController? = null
    private var composePresentationListener: SettingsPresentationController.Listener? = null
    private val backupHost = ConfigBackupHost(
        activity,
        object : ConfigBackupHost.Port {
            override fun configStore(): DpisConfigStore? = store
            override fun isComposeSurface(): Boolean = true
            override fun showToast(messageResId: Int) {
                this@SystemServerSettingsPageController.showToast(messageResId)
            }
            override fun publishPresentationState() {
                this@SystemServerSettingsPageController.publishPresentationState()
            }
            override fun onRestoreSucceeded() {
                relaunchDpisTask()
            }
            override fun runOnUiThread(action: Runnable) {
                this@SystemServerSettingsPageController.runOnUiThread(action)
            }
        },
    )

    private val statsHandler = Handler(Looper.getMainLooper())
    private val statsRefreshRunnable: Runnable = object : Runnable {
        override fun run() {
            refreshStatsPanel()
            statsHandler.postDelayed(this, STATS_REFRESH_INTERVAL_MS)
        }
    }

    init {
        this.launcherIconVisibilityStore = LauncherIconVisibilityStore(activity)
        this.interfaceScaleStore = InterfaceScaleStore(activity)
        this.presentationController = SettingsPresentationController(
            object : SettingsPresentationController.Port {
                override fun snapshot(): SettingsUiState {
                    return presentationState()
                }

                override fun setSafeModeEnabled(enabled: Boolean) {
                    persistSafeMode(enabled)
                }

                override fun setGlobalLogEnabled(enabled: Boolean) {
                    onGlobalLogChanged(null, enabled)
                }

                override fun setLauncherIconHidden(hidden: Boolean) {
                    persistLauncherHidden(hidden)
                }

                override fun refresh() {
                    refreshComposeStoreState()
                    publishPresentationState()
                }
            })
    }

    fun presentationState(): SettingsUiState {
        val available = store != null
        return SettingsUiState(
            available,
            available && store!!.isSystemServerHooksEnabled(),
            available && store!!.isSystemServerSafeModeEnabled(),
            available && store!!.isGlobalLogEnabled(),
            PageSettingsStore.isHomeActivationDetectionEnabled(activity),
            launcherIconVisibilityStore.isHidden(), interfaceScaleStore.percent,
            clearCacheInProgress, lastCacheUsage,
            getString(AppLocaleManager.selectedLabelResId(activity)),
            backupHost.pendingImportUri,
        )
    }

    private fun publishPresentationState() {
        presentationController.publishState()
    }

    /** Initializes the same Java-owned workflows when Settings is Compose-native.  */
    fun startComposePresentation(onStateChanged: () -> Unit) {
        refreshComposeStoreState()
        statsPreferences = FontDebugStatsStore.getPreferences(activity)
        updateCacheEntrySubtitle()
        composePresentationListener = SettingsPresentationController.Listener {
            onStateChanged()
        }
        presentationController.addListener(composePresentationListener)
        publishPresentationState()
    }

    fun stopComposePresentation() {
        composePresentationListener?.let(presentationController::removeListener)
        composePresentationListener = null
    }

    fun setHooksEnabledFromPresentation(enabled: Boolean) {
        onHooksEnabledChanged(enabled)
    }

    fun setSafeModeFromPresentation(enabled: Boolean) {
        persistSafeMode(enabled)
    }

    fun setGlobalLogFromPresentation(enabled: Boolean) {
        onGlobalLogChanged(null, enabled)
    }

    fun setHomeActivationDetectionFromPresentation(enabled: Boolean) {
        PageSettingsStore.setHomeActivationDetectionEnabled(activity, enabled)
        publishPresentationState()
    }

    fun setLauncherHiddenFromPresentation(hidden: Boolean) {
        persistLauncherHidden(hidden)
    }

    fun showFontDebugFromPresentation() {
        showFontDebugDialog(null)
    }

    fun showLanguageFromPresentation() {
        showLanguageDialog(null)
    }

    fun setLanguageFromPresentation(selectedTag: String) {
        applyLanguageSelection(selectedTag)
    }

    fun showConfigBackupFromPresentation() {
        showConfigBackupDialog(null)
    }

    fun confirmImportFromPresentation() {
        backupHost.confirmPendingImport()
    }

    fun dismissImportFromPresentation() {
        backupHost.dismissPendingImport()
    }

    fun clearCacheFromPresentation() {
        clearCache(null)
    }

    fun onStart() {
        DpisApplication.addServiceStateListener(this, true)
        statsHandler.post(statsRefreshRunnable)
    }

    fun onResume() {
        syncHooksSwitchWithScope()
        syncLauncherIconHiddenState()
        if (store != null && store!!.isFontDebugOverlayEnabled && canDrawOverlays()) {
            startFontDebugOverlayService()
        }
        publishPresentationState()
    }

    fun onStop() {
        DpisApplication.removeServiceStateListener(this)
        statsHandler.removeCallbacks(statsRefreshRunnable)
        dismissFontDebugDialog()
    }

    override fun onServiceStateChanged() {
        runOnUiThread {
            refreshComposeStoreState()
            publishPresentationState()
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        backupHost.onActivityResult(requestCode, resultCode, data)
    }

    private fun getString(resId: Int): String {
        return activity.getString(resId)
    }

    private fun getString(resId: Int, vararg formatArgs: Any?): String {
        return activity.getString(resId, *formatArgs)
    }

    private val applicationContext: Context?
        get() = activity.applicationContext

    private val packageName: String?
        get() = activity.packageName

    private val packageManager: PackageManager?
        get() = activity.packageManager

    private fun startActivity(intent: Intent?) {
        activity.startActivity(intent)
    }

    private fun startService(intent: Intent?) {
        activity.startService(intent)
    }

    private fun stopService(intent: Intent?) {
        activity.stopService(intent)
    }

    private fun runOnUiThread(action: Runnable?) {
        activity.runOnUiThread(action)
    }

    private val isFinishing: Boolean
        get() = activity.isFinishing

    private val isDestroyed: Boolean
        get() = activity.isDestroyed

    private fun recreate() {
        activity.recreate()
    }

    private fun finishAffinity() {
        activity.finishAffinity()
    }

    private fun showLanguageDialog(anchor: Any?) {
        val languageOptions = AppLocaleManager.supportedLanguages()
        val options = languageOptions.map { option ->
            LanguageDialogOption(option.tag, getString(option.labelResId))
        }
        SettingsComposeDialogs.showLanguage(
            activity,
            options,
            AppLocaleManager.getLanguageTag(activity),
            Consumer { selectedTag: String? -> this.applyLanguageSelection(selectedTag!!) })
    }

    private fun applyLanguageSelection(selectedTag: String) {
        val previousTag = AppLocaleManager.getLanguageTag(activity)
        if (!AppLocaleManager.setLanguageTag(activity, selectedTag)) {
            showToast(R.string.system_settings_save_failed)
            return
        }
        if (selectedTag != previousTag) {
            if (onConfigurationChanged != null) {
                onConfigurationChanged.run()
            } else {
                recreate()
            }
        }
    }

    private fun updateCacheEntrySubtitle() {
        val appContext = this.applicationContext
        Thread({
            val usage = SafeCacheCleaner.formatCacheUsage(appContext)
            runOnUiThread {
                if (!this.isFinishing && !this.isDestroyed && !clearCacheInProgress) {
                    setCacheEntrySubtitle(usage)
                    publishPresentationState()
                }
            }
        }, "dpis-cache-size").start()
    }

    private fun clearCache(anchor: Any?) {
        if (clearCacheInProgress) {
            return
        }
        clearCacheInProgress = true
        setCacheEntrySubtitle(getString(R.string.settings_clear_cache_cleaning))
        publishPresentationState()
        val appContext = this.applicationContext
        Thread({
            val startedAt = System.currentTimeMillis()
            var legacyCacheStillNeedsManualDelete = false
            var failed = false
            try {
                SafeCacheCleaner.clearAll(appContext)
                legacyCacheStillNeedsManualDelete = SafeCacheCleaner.hasLegacyPublicFontDebugCache()
            } catch (exception: RuntimeException) {
                failed = true
                DpisLog.e("clear cache failed", exception)
            } finally {
                sleepUntilMinDisabledElapsed(startedAt)
                val finalLegacyCacheStillNeedsManualDelete = legacyCacheStillNeedsManualDelete
                val finalFailed = failed
                runOnUiThread {
                    if (this.isFinishing || this.isDestroyed) {
                        return@runOnUiThread
                    }
                    clearCacheInProgress = false
                    updateCacheEntrySubtitle()
                    publishPresentationState()
                    if (finalLegacyCacheStillNeedsManualDelete) {
                        showToast(R.string.settings_clear_cache_legacy_public_file_blocked)
                        return@runOnUiThread
                    }
                    showToast(
                        if (finalFailed)
                            R.string.system_settings_save_failed
                        else
                            R.string.settings_clear_cache_done
                    )
                }
            }
        }, "dpis-clear-cache").start()
    }

    private fun setCacheEntrySubtitle(usage: String?) {
        lastCacheUsage = usage ?: ""
    }

    private fun showConfigBackupDialog(anchor: Any?) {
        if (store == null) {
            showToast(R.string.status_save_requires_init)
            return
        }
        showBackupActions(
            activity,
            { backupHost.launchExportPicker() },
            { backupHost.launchImportPicker() },
        )
    }

    private fun relaunchDpisTask() {
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
        val intent = Intent(activity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finishAffinity()
    }

    private fun refreshComposeStoreState() {
        store = DpisApplication.getConfigStore()
        if (store == null) {
            hooksToggleController = null
            return
        }
        selectedMode = store!!.fontDebugSelectedMode
        selectedWindow = store!!.fontDebugSelectedWindow
        hooksToggleController = SystemHooksToggleController(
            store,
            ActivitySystemScopeGateway(),
            ActivitySystemHooksToggleView()
        ) { this.publishPresentationState() }
    }

    private fun showFontDebugDialog(anchor: Any?) {
        if (store == null) {
            return
        }
        dismissFontDebugDialog()
        fontDebugDialog = show(
            activity,
            {
                selectedMode = if (selectedMode == FontDebugStatsStore.MODE_CHAIN)
                    FontDebugStatsStore.MODE_CHAIN_VIEW
                else
                    FontDebugStatsStore.MODE_CHAIN
                store!!.setFontDebugSelectedMode(selectedMode)
                refreshStatsPanel()
                publishPresentationState()
            }, {
                selectedWindow = when (selectedWindow) {
                    FontDebugStatsStore.WINDOW_5S -> {
                        FontDebugStatsStore.WINDOW_30S
                    }
                    FontDebugStatsStore.WINDOW_30S -> {
                        FontDebugStatsStore.WINDOW_ALL
                    }
                    else -> {
                        FontDebugStatsStore.WINDOW_5S
                    }
                }
                store!!.setFontDebugSelectedWindow(selectedWindow)
                refreshStatsPanel()
                publishPresentationState()
            }, {
                val currentEnabled = store!!.isFontDebugOverlayEnabled
                val requestedEnabled = !currentEnabled
                if (requestedEnabled && !canDrawOverlays()) {
                    requestOverlayPermission()
                    showToast(R.string.font_debug_overlay_permission_needed)
                    updateDialogButtons()
                    publishPresentationState()
                    return@show
                }
                if (!store!!.setFontDebugOverlayEnabled(requestedEnabled)) {
                    showToast(R.string.system_settings_save_failed)
                    updateDialogButtons()
                    publishPresentationState()
                    return@show
                }
                RuntimeDebugPropertySyncer.publishAsync(
                    store!!.isGlobalLogEnabled(),
                    requestedEnabled
                )
                if (requestedEnabled) {
                    startFontDebugOverlayService()
                } else {
                    stopService(Intent(activity, FontDebugOverlayService::class.java))
                }
                updateDialogButtons()
                publishPresentationState()
            }, {
                clearDebugStatsData()
                refreshStatsPanel()
                showToast(R.string.font_debug_clear_done)
                publishPresentationState()
            }, {
                fontDebugDialog = null
                publishPresentationState()
            })
        refreshStatsPanel()
    }

    private fun dismissFontDebugDialog() {
        if (fontDebugDialog != null) {
            fontDebugDialog!!.dismiss()
        }
    }

    private fun refreshStatsPanel() {
        val handle = fontDebugDialog
        if (statsPreferences == null || handle == null) {
            return
        }
        val key = FontDebugStatsSchema.statsKeyFor(selectedMode, selectedWindow)
        val statsText = statsPreferences!!.getString(key, null)
        val updatedAt = statsPreferences!!.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L)
        val eventTotal = statsPreferences!!.getInt(FontDebugStatsStore.KEY_EVENT_TOTAL, 0)

        val contentText: String?
        if (statsText == null || statsText.trim { it <= ' ' }.isEmpty()) {
            val reason = FontDebugDataDiagnostics.resolveNoDataReason(
                store,
                statsPreferences
            )
            contentText = if (reason == NoDataReason.NONE) {
                getString(R.string.font_debug_not_updated)
            } else {
                getString(
                    R.string.font_debug_no_data_with_reason,
                    reasonTitleText(reason),
                    reasonHintText(reason)
                )
            }
        } else {
            contentText = statsText
        }
        var updatedText = getString(R.string.font_debug_not_updated)
        if (updatedAt > 0L) {
            val format = DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.getDefault())
            updatedText = getString(
                R.string.font_debug_last_updated,
                format.format(Date(updatedAt)), eventTotal
            )
        }
        val windowLabelRes = when (selectedWindow) {
            FontDebugStatsStore.WINDOW_5S -> R.string.font_debug_window_button_5s
            FontDebugStatsStore.WINDOW_30S -> R.string.font_debug_window_button_30s
            else -> R.string.font_debug_window_button_all
        }
        val overlayEnabled = store!!.isFontDebugOverlayEnabled
        handle.update(
            getString(
                if (selectedMode == FontDebugStatsStore.MODE_CHAIN)
                    R.string.font_debug_mode_button_chain
                else
                    R.string.font_debug_mode_button_chain_view
            ),
            getString(windowLabelRes), updatedText, contentText,
            getString(
                if (overlayEnabled)
                    R.string.font_debug_overlay_disable_button
                else
                    R.string.font_debug_overlay_enable_button
            ),
            overlayEnabled
        )
    }

    private fun reasonTitleText(reason: NoDataReason): String {
        return when (reason) {
            NoDataReason.SCOPE_MISSING -> getString(R.string.font_debug_reason_scope_missing)
            NoDataReason.NOT_INJECTED -> getString(R.string.font_debug_reason_not_injected)
            NoDataReason.NO_EVENTS -> getString(R.string.font_debug_reason_no_events)
            else -> getString(R.string.font_debug_not_updated)
        }
    }

    private fun reasonHintText(reason: NoDataReason): String {
        return when (reason) {
            NoDataReason.SCOPE_MISSING -> getString(R.string.font_debug_reason_scope_missing_hint)
            NoDataReason.NOT_INJECTED -> getString(R.string.font_debug_reason_not_injected_hint)
            NoDataReason.NO_EVENTS -> getString(R.string.font_debug_reason_no_events_hint)
            else -> getString(R.string.font_debug_not_updated)
        }
    }

    private fun clearDebugStatsData() {
        FontDebugStatsStore.clearStats(statsPreferences)
    }

    private fun updateDialogButtons() {
        refreshStatsPanel()
    }

    private fun onHooksEnabledChanged(isChecked: Boolean) {
        if (!BuildConfig.DEBUG) {
            return
        }
        if (hooksToggleController == null) {
            return
        }
        hooksToggleController!!.onUserToggle(isChecked)
        publishPresentationState()
    }

    private fun persistSafeMode(enabled: Boolean) {
        if (store == null) {
            return
        }
        if (!store!!.setSystemServerSafeModeEnabled(enabled)) {
            showToast(R.string.system_settings_save_failed)
            publishPresentationState()
            return
        }
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
        publishPresentationState()
    }

    private fun persistLauncherHidden(hidden: Boolean) {
        persistLauncherIconState(hidden)
        publishPresentationState()
    }

    private fun onGlobalLogChanged(buttonView: Any?, isChecked: Boolean) {
        if (store == null) {
            return
        }
        if (!store!!.setGlobalLogEnabled(isChecked)) {
            showToast(R.string.system_settings_save_failed)
            publishPresentationState()
            return
        }
        DpisLog.setLoggingEnabled(isChecked)
        RuntimeDebugPropertySyncer.publishAsync(
            isChecked,
            store!!.isFontDebugOverlayEnabled
        )
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
        publishPresentationState()
    }

    private fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(activity)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + this.packageName)
        )
        startActivity(intent)
        publishPresentationState()
    }

    private fun startFontDebugOverlayService() {
        val serviceIntent = Intent(activity, FontDebugOverlayService::class.java)
        startService(serviceIntent)
    }

    private fun showToast(messageResId: Int) {
        showToast(getString(messageResId))
    }

    private fun showToast(messageResId: Int, vararg formatArgs: Any?) {
        showToast(getString(messageResId, *formatArgs))
    }

    private fun showToast(message: CharSequence?) {
        if (this.isFinishing || this.isDestroyed) {
            return
        }
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    private fun syncHooksSwitchWithScope() {
        if (!BuildConfig.DEBUG) {
            return
        }
        if (hooksToggleController == null) {
            return
        }
        hooksToggleController!!.syncFromStore()
    }

    private fun syncLauncherIconHiddenState() {
        val storedHidden = launcherIconVisibilityStore.isHidden()
        val hidden = resolveLauncherIconHiddenState(storedHidden)
        if (hidden != storedHidden) {
            launcherIconVisibilityStore.isHidden = hidden
        }
    }

    private fun persistLauncherIconState(hidden: Boolean): Boolean {
        if (!setLauncherAliasHidden(hidden)) {
            showToast(R.string.settings_hide_launcher_icon_apply_failed)
            return false
        }
        if (launcherIconVisibilityStore.setHidden(hidden)) {
            return true
        }
        setLauncherAliasHidden(!hidden)
        showToast(R.string.system_settings_save_failed)
        return false
    }

    private fun setLauncherAliasHidden(hidden: Boolean): Boolean {
        try {
            val state = if (hidden)
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            else
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            this.packageManager!!.setComponentEnabledSetting(
                this.launcherAliasComponentName,
                state,
                PackageManager.DONT_KILL_APP
            )
            return true
        } catch (error: RuntimeException) {
            return false
        }
    }

    private fun resolveLauncherIconHiddenState(fallback: Boolean): Boolean {
        val state: Int
        try {
            state =
                this.packageManager!!.getComponentEnabledSetting(this.launcherAliasComponentName)
        } catch (error: RuntimeException) {
            return fallback
        }
        if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
            return true
        }
        if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            return false
        }
        return fallback
    }

    private val launcherAliasComponentName: ComponentName
        get() = ComponentName(activity, MainActivity::class.java.name + "Launcher")

    private inner class ActivitySystemHooksToggleView : SystemHooksToggleController.View {
        override fun render(state: SystemHookState) = Unit

        override fun showInitRequired() {
            showToast(R.string.status_save_requires_init)
        }

        override fun showSaveFailed() {
            showToast(R.string.system_settings_save_failed)
        }

        override fun showScopeRequired() {
            showToast(R.string.system_hooks_scope_required)
        }
    }

    private class ActivitySystemScopeGateway : ScopeGateway {
        override fun isServiceAvailable(): Boolean {
            return DpisApplication.xposedService != null
        }

        override fun hasSystemScopeSelected(): Boolean {
            val service = DpisApplication.xposedService ?: return false
            try {
                val scope = service.scope
                return SystemFrameworkScope.containsSystemScope(scope)
            } catch (error: RuntimeException) {
                return false
            }
        }
    }

    companion object {
        private const val STATS_REFRESH_INTERVAL_MS = 500L
        private const val CLEAR_CACHE_MIN_DISABLED_MS = 300L

        private fun sleepUntilMinDisabledElapsed(startedAt: Long) {
            val elapsed = System.currentTimeMillis() - startedAt
            val remaining: Long = CLEAR_CACHE_MIN_DISABLED_MS - elapsed
            if (remaining <= 0L) {
                return
            }
            try {
                Thread.sleep(remaining)
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }
}
