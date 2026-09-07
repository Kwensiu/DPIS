package com.dpis.module

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dpis.module.about.AboutActivity
import com.dpis.module.backup.ConfigBackupCoordinator
import com.dpis.module.fonts.FontDebugDataDiagnostics
import com.dpis.module.fonts.FontDebugDataDiagnostics.NoDataReason
import com.dpis.module.fonts.FontDebugOverlayService
import com.dpis.module.fonts.FontDebugStatsSchema
import com.dpis.module.fonts.FontDebugStatsStore
import com.dpis.module.fonts.FontLibraryActivity
import com.dpis.module.home.DonateActivity
import com.dpis.module.runtime.RuntimeConfigDelivery
import com.dpis.module.runtime.RuntimeDebugPropertySyncer
import com.dpis.module.settings.AppLocaleManager
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.settings.ExperimentalSettingsActivity
import com.dpis.module.settings.InterfaceScaleStore
import com.dpis.module.settings.LauncherIconVisibilityStore
import com.dpis.module.settings.SafeCacheCleaner
import com.dpis.module.settings.SystemFrameworkScope
import com.dpis.module.settings.SystemHookState
import com.dpis.module.settings.SystemHooksToggleController
import com.dpis.module.settings.SystemHooksToggleController.ScopeGateway
import com.dpis.module.settings.ThemeSettingsActivity
import com.dpis.module.templates.QuickTemplateStore
import com.dpis.module.ui.compose.FontDebugComposeSheet
import com.dpis.module.ui.compose.FontDebugComposeSheet.show
import com.dpis.module.ui.compose.LanguageDialogOption
import com.dpis.module.ui.compose.SettingsComposeDialogs
import com.dpis.module.ui.compose.SettingsComposeDialogs.showBackupActions
import com.dpis.module.ui.compose.SettingsComposeDialogs.showInterfaceScale
import com.dpis.module.ui.dialog.ConfirmDialog.show
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textview.MaterialTextView
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.function.Consumer
import kotlin.concurrent.Volatile

/** Java-facing settings workflow controller shared by the legacy and Compose presentations. */
class SystemServerSettingsPageController(
    private val activity: LocalizedActivity,
    private val root: View?
) : DpisApplication.ServiceStateListener {
    private val launcherIconVisibilityStore: LauncherIconVisibilityStore
    private val interfaceScaleStore: InterfaceScaleStore
    private val presentationController: SettingsPresentationController
    private var store: DpisConfigStore? = null
    private var hooksEnabledSwitch: MaterialSwitch? = null
    private var safeModeSwitch: MaterialSwitch? = null
    private var globalLogSwitch: MaterialSwitch? = null
    private var hideLauncherIconSwitch: MaterialSwitch? = null
    private var primarySwitchCard: View? = null
    private var languageEntryRow: View? = null
    private var clearCacheEntryRow: View? = null
    private var fontDebugEntryRow: View? = null
    private var experimentalSettingsEntryRow: View? = null
    private var fontLibraryEntryRow: View? = null
    private var backupConfigEntryRow: View? = null
    private var interfaceScaleRow: View? = null
    private var interfaceScaleSlider: Slider? = null
    private var interfaceScaleValueView: MaterialTextView? = null
    private var lastInterfaceScaleFeedbackPercent = AppUiScaleManager.DEFAULT_SCALE_PERCENT
    private var suppressInterfaceScaleSliderChange = false

    @Volatile
    private var clearCacheInProgress = false
    private var lastCacheUsage = "0 B"
    private var statsPreferences: SharedPreferences? = null
    private var selectedMode = FontDebugStatsStore.MODE_CHAIN
    private var selectedWindow = FontDebugStatsStore.WINDOW_ALL

    private var fontDebugDialog: FontDebugComposeSheet.Handle? = null
    private var hooksToggleController: SystemHooksToggleController? = null
    private var composePresentationListener: SettingsPresentationController.Listener? = null

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
                    if (enabled) {
                        onSafeModeChanged(null, true)
                    } else {
                        showDisableSafeModeConfirmationDialog()
                    }
                }

                override fun setGlobalLogEnabled(enabled: Boolean) {
                    onGlobalLogChanged(null, enabled)
                }

                override fun setLauncherIconHidden(hidden: Boolean) {
                    if (hidden) {
                        showHideLauncherIconConfirmationDialog()
                    } else {
                        onHideLauncherIconChanged(null, false)
                    }
                }

                override fun refresh() {
                    if (root == null) {
                        refreshComposeStoreState()
                    } else {
                        refreshStoreState(false)
                    }
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
            launcherIconVisibilityStore.isHidden(), interfaceScaleStore.getPercent(),
            clearCacheInProgress, lastCacheUsage,
            getString(AppLocaleManager.selectedLabelResId(activity))
        )
    }

    private fun publishPresentationState() {
        presentationController.publishState()
    }

    fun bind() {
        applyInsets()

        primarySwitchCard = findViewById(R.id.settings_primary_switch_card)
        primarySwitchCard!!.visibility = View.GONE
        hooksEnabledSwitch = bindSwitchRow(
            R.id.row_system_hooks,
            R.drawable.ic_android_24,
            R.string.system_hooks_enabled_label,
            R.string.system_hooks_enabled_hint
        )
        applySystemHooksRowVisibility()
        safeModeSwitch = bindSwitchRow(
            R.id.row_safe_mode,
            R.drawable.ic_shield_24,
            R.string.system_safe_mode_label,
            R.string.system_safe_mode_hint
        )
        globalLogSwitch = bindSwitchRow(
            R.id.row_global_log,
            R.drawable.ic_view_kanban_24,
            R.string.global_log_enabled_label,
            R.string.global_log_enabled_hint
        )
        fontDebugEntryRow = bindEntryRow(
            R.id.row_font_debug_overlay,
            R.drawable.ic_bug_report_24,
            R.string.font_debug_overlay_label,
            R.string.font_debug_entry_hint
        ) { anchor: View? -> this.showFontDebugDialog(anchor) }
        experimentalSettingsEntryRow = bindEntryRow(
            R.id.row_experimental_settings,
            R.drawable.ic_experiment_24,
            R.string.settings_experimental_title,
            R.string.settings_experimental_hint
        ) { v: View? ->
            startActivity(
                Intent(
                    activity,
                    ExperimentalSettingsActivity::class.java
                )
            )
        }
        fontLibraryEntryRow = bindEntryRow(
            R.id.row_font_library,
            R.drawable.ic_upload_file_24,
            R.string.settings_font_library_label,
            R.string.settings_font_library_hint
        ) { v: View? ->
            startActivity(
                Intent(
                    activity,
                    FontLibraryActivity::class.java
                )
            )
        }
        bindInterfaceScaleRow()
        backupConfigEntryRow = bindEntryRow(
            R.id.row_config_backup,
            R.drawable.ic_upload_file_24,
            R.string.settings_config_backup_label,
            R.string.settings_config_backup_hint
        ) { anchor: View? -> this.showConfigBackupDialog(anchor) }
        bindLanguageRow()
        clearCacheEntryRow = bindEntryRow(
            R.id.row_clear_cache,
            R.drawable.ic_mop_24,
            R.string.settings_clear_cache_label,
            R.string.settings_clear_cache_size
        ) { anchor: View? -> this.clearCache(anchor) }
        setCacheEntrySubtitle("0 B")
        updateCacheEntrySubtitle()
        bindEntryRow(
            R.id.row_about,
            R.drawable.ic_info_24,
            R.string.settings_about_label,
            R.string.settings_about_hint
        ) { v: View? ->
            startActivity(
                Intent(
                    activity,
                    AboutActivity::class.java
                )
            )
        }
        bindEntryRow(
            R.id.row_donate,
            R.drawable.ic_volunteer_24,
            R.string.settings_donate_label,
            R.string.settings_donate_hint
        ) { v: View? -> startActivity(DonateActivity.createIntent(activity)) }
        hideLauncherIconSwitch = bindSwitchRow(
            R.id.row_hide_launcher_icon,
            R.drawable.ic_hide_image_24,
            R.string.settings_hide_launcher_icon_label,
            R.string.settings_hide_launcher_icon_hint
        )

        statsPreferences = FontDebugStatsStore.getPreferences(activity)
        hooksEnabledSwitch!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            this.onHooksEnabledChanged(
                buttonView,
                isChecked
            )
        }
        safeModeSwitch!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            this.onSafeModeChanged(
                buttonView,
                isChecked
            )
        }
        globalLogSwitch!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            this.onGlobalLogChanged(
                buttonView,
                isChecked
            )
        }
        hideLauncherIconSwitch!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            this.onHideLauncherIconChanged(
                buttonView,
                isChecked
            )
        }
        refreshStoreState(true)
        publishPresentationState()
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
        onHooksEnabledChanged(null, enabled)
    }

    fun setSafeModeFromPresentation(enabled: Boolean) {
        if (enabled) onSafeModeChanged(null, true) else showDisableSafeModeConfirmationDialog()
    }

    fun setGlobalLogFromPresentation(enabled: Boolean) {
        onGlobalLogChanged(null, enabled)
    }

    fun setLauncherHiddenFromPresentation(hidden: Boolean) {
        if (hidden) showHideLauncherIconConfirmationDialog()
        else onHideLauncherIconChanged(null, false)
    }

    fun showFontDebugFromPresentation() {
        showFontDebugDialog(null)
    }

    fun showExperimentalSettingsFromPresentation() {
        startActivity(Intent(activity, ExperimentalSettingsActivity::class.java))
    }

    fun showThemeSettingsFromPresentation() {
        startActivity(Intent(activity, ThemeSettingsActivity::class.java))
    }

    fun showFontLibraryFromPresentation() {
        startActivity(Intent(activity, FontLibraryActivity::class.java))
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

    fun clearCacheFromPresentation() {
        clearCache(null)
    }

    fun showAboutFromPresentation() {
        startActivity(Intent(activity, AboutActivity::class.java))
    }

    fun showDonateFromPresentation() {
        startActivity(DonateActivity.createIntent(activity))
    }

    fun onStart() {
        DpisApplication.addServiceStateListener(this, true)
        statsHandler.post(statsRefreshRunnable)
    }

    fun onResume() {
        syncHooksSwitchWithScope()
        syncLauncherIconSwitch()
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
            if (root == null) {
                refreshComposeStoreState()
            } else {
                refreshStoreState(false)
            }
            publishPresentationState()
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null || data.data == null) {
            return
        }
        val uri = data.data
        if (requestCode == REQUEST_EXPORT_CONFIG_BACKUP) {
            exportConfigBackup(uri)
            publishPresentationState()
            return
        }
        if (requestCode == REQUEST_IMPORT_CONFIG_BACKUP) {
            showImportBackupConfirmDialog(uri)
            publishPresentationState()
            return
        }
    }

    private fun applyInsets() {
        val toolbar = findViewById<View?>(R.id.settings_toolbar)
        if (root == null || toolbar == null) {
            return
        }
        val baseRootPaddingLeft = root.paddingLeft
        val baseRootPaddingRight = root.paddingRight
        val baseTopPadding = toolbar.paddingTop
        val baseToolbarPaddingLeft = toolbar.paddingLeft
        val baseToolbarPaddingRight = toolbar.paddingRight
        ViewCompat.setOnApplyWindowInsetsListener(
            root
        ) { view: View?, insets: WindowInsetsCompat? ->
            val safeDrawing = insets!!.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view!!.setPadding(
                baseRootPaddingLeft + safeDrawing.left, view.paddingTop,
                baseRootPaddingRight + safeDrawing.right, view.paddingBottom
            )
            toolbar.setPadding(
                baseToolbarPaddingLeft, baseTopPadding + safeDrawing.top,
                baseToolbarPaddingRight, toolbar.paddingBottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun <T : View?> findViewById(id: Int): T? {
        if (id == android.R.id.content) {
            return activity.findViewById<T?>(id)
        }
        return root!!.findViewById<T?>(id)
    }

    private val resources: Resources
        get() = activity.resources

    private fun getString(resId: Int): String {
        return activity.getString(resId)
    }

    private fun getString(resId: Int, vararg formatArgs: Any?): String {
        return activity.getString(resId, *formatArgs)
    }

    private fun <T> getSystemService(serviceClass: Class<T?>): T? {
        return activity.getSystemService<T?>(serviceClass)
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

    private fun startActivityForResult(intent: Intent, requestCode: Int) {
        activity.startActivityForResult(intent, requestCode)
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

    private fun dp(value: Int): Int {
        return Math.round(value * resources.displayMetrics.density)
    }

    private fun bindSwitchRow(
        rowId: Int,
        iconRes: Int,
        titleRes: Int,
        subtitleRes: Int
    ): MaterialSwitch {
        val row = findViewById<View>(rowId)!!
        val iconView = row.findViewById<ImageView>(R.id.setting_icon)
        val titleView = row.findViewById<MaterialTextView>(R.id.setting_title)
        val subtitleView = row.findViewById<MaterialTextView>(R.id.setting_subtitle)
        val switchView = row.findViewById<MaterialSwitch>(R.id.setting_switch)

        iconView.setImageResource(iconRes)
        titleView.setText(titleRes)
        subtitleView.setText(subtitleRes)
        row.setOnClickListener { v: View? ->
            if (switchView.isEnabled) {
                switchView.toggle()
            }
        }
        return switchView
    }

    private fun bindEntryRow(
        rowId: Int,
        iconRes: Int,
        titleRes: Int,
        subtitleRes: Int,
        clickListener: View.OnClickListener?
    ): View {
        val row = findViewById<View>(rowId)!!
        val iconView = row.findViewById<ImageView>(R.id.setting_icon)
        val titleView = row.findViewById<MaterialTextView>(R.id.setting_title)
        val subtitleView = row.findViewById<MaterialTextView>(R.id.setting_subtitle)
        iconView.setImageResource(iconRes)
        titleView.setText(titleRes)
        subtitleView.setText(subtitleRes)
        row.setOnClickListener(clickListener)
        return row
    }

    private fun bindLanguageRow() {
        languageEntryRow = findViewById(R.id.row_language)
        val iconView = languageEntryRow!!.findViewById<ImageView>(R.id.setting_icon)
        val titleView = languageEntryRow!!.findViewById<MaterialTextView>(R.id.setting_title)
        iconView.setImageResource(R.drawable.ic_language_24)
        titleView.setText(R.string.settings_language_label)
        updateLanguageEntrySubtitle()
        languageEntryRow!!.setOnClickListener { anchor: View? ->
            this.showLanguageDialog(
                anchor
            )
        }
    }

    private fun bindInterfaceScaleRow() {
        interfaceScaleRow = findViewById<View>(R.id.row_interface_scale)
        val iconView = interfaceScaleRow!!.findViewById<ImageView>(R.id.setting_icon)
        val titleView = interfaceScaleRow!!.findViewById<MaterialTextView>(R.id.setting_title)
        val subtitleView = interfaceScaleRow!!.findViewById<MaterialTextView>(R.id.setting_subtitle)
        interfaceScaleValueView =
            interfaceScaleRow!!.findViewById(R.id.setting_value)
        interfaceScaleSlider = interfaceScaleRow!!.findViewById(R.id.setting_slider)

        iconView.setImageResource(R.drawable.ic_fit_width_24)
        titleView.setText(R.string.settings_interface_scale_label)
        subtitleView.setText(R.string.settings_interface_scale_hint)
        interfaceScaleSlider!!.valueFrom = AppUiScaleManager.MIN_SCALE_PERCENT.toFloat()
        interfaceScaleSlider!!.valueTo = AppUiScaleManager.MAX_SCALE_PERCENT.toFloat()
        interfaceScaleSlider!!.stepSize = 10f
        interfaceScaleRow!!.setOnClickListener { v: View? -> showInterfaceScaleDialog() }
        interfaceScaleSlider!!.addOnChangeListener { slider: Slider?, value: Float, fromUser: Boolean ->
            if (fromUser && !suppressInterfaceScaleSliderChange) {
                val percent = normalizeInterfaceScaleSliderPercent(Math.round(value))
                updateInterfaceScaleValue(percent)
                performInterfaceScaleStepFeedback(percent)
            }
        }
        interfaceScaleSlider!!.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                val percent = AppUiScaleManager.normalizeScalePercent(Math.round(slider.value))
                lastInterfaceScaleFeedbackPercent = percent
                updateInterfaceScaleValue(percent)
            }

            override fun onStopTrackingTouch(slider: Slider) {
                saveInterfaceScalePercent(Math.round(slider.value))
            }
        })
        setInterfaceScalePercentSilently(AppUiScaleManager.getEffectiveScalePercent(activity))
    }

    private fun setInterfaceScalePercentSilently(percent: Int) {
        if (interfaceScaleSlider == null) {
            return
        }
        val normalized = AppUiScaleManager.normalizeScalePercent(percent)
        val sliderPercent = nearestInterfaceScaleSliderPercent(normalized)
        suppressInterfaceScaleSliderChange = true
        interfaceScaleSlider!!.value = sliderPercent.toFloat()
        suppressInterfaceScaleSliderChange = false
        lastInterfaceScaleFeedbackPercent = sliderPercent
        updateInterfaceScaleValue(normalized)
    }

    private fun updateInterfaceScaleValue(percent: Int) {
        if (interfaceScaleValueView != null) {
            interfaceScaleValueView!!.text = getString(
                R.string.settings_interface_scale_value,
                AppUiScaleManager.normalizeScalePercent(percent)
            )
        }
    }

    private fun saveInterfaceScalePercent(percent: Int) {
        val normalized = AppUiScaleManager.normalizeScalePercent(percent)
        if (normalized == interfaceScaleStore.getPercent()
            && interfaceScaleStore.hasExplicitPercent()
        ) {
            setInterfaceScalePercentSilently(normalized)
            return
        }
        if (!interfaceScaleStore.setPercent(normalized)) {
            setInterfaceScalePercentSilently(interfaceScaleStore.getPercent())
            showToast(R.string.system_settings_save_failed)
            return
        }
        setInterfaceScalePercentSilently(normalized)
        publishPresentationState()
        recreate()
    }

    private fun showInterfaceScaleDialog() {
        showInterfaceScale(
            activity,
            AppUiScaleManager.getEffectiveScalePercent(activity),
            AppUiScaleManager.MIN_SCALE_PERCENT,
            AppUiScaleManager.MAX_SCALE_PERCENT
        ) { percent: Int -> this.saveInterfaceScalePercent(percent) }
    }

    private fun performInterfaceScaleStepFeedback(percent: Int) {
        if (percent == lastInterfaceScaleFeedbackPercent || interfaceScaleSlider == null) {
            return
        }
        lastInterfaceScaleFeedbackPercent = percent
        interfaceScaleSlider!!.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun normalizeInterfaceScaleSliderPercent(percent: Int): Int {
        return nearestInterfaceScaleSliderPercent(
            AppUiScaleManager.normalizeScalePercent(percent)
        )
    }

    private fun nearestInterfaceScaleSliderPercent(percent: Int): Int {
        val normalized = AppUiScaleManager.normalizeScalePercent(percent)
        val min = AppUiScaleManager.MIN_SCALE_PERCENT
        val rounded = Math.round((normalized - min) / 10f) * 10 + min
        return AppUiScaleManager.normalizeScalePercent(rounded)
    }

    private fun showLanguageDialog(anchor: View?) {
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
        updateLanguageEntrySubtitle()
        if (selectedTag != previousTag) {
            recreate()
        }
    }

    private fun updateLanguageEntrySubtitle() {
        if (languageEntryRow == null) {
            return
        }
        val subtitleView = languageEntryRow!!.findViewById<MaterialTextView>(R.id.setting_subtitle)
        subtitleView.setText(AppLocaleManager.selectedLabelResId(activity))
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

    private fun clearCache(anchor: View?) {
        if (clearCacheInProgress) {
            return
        }
        clearCacheInProgress = true
        setRowEnabled(clearCacheEntryRow, false)
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
                    setRowEnabled(clearCacheEntryRow, true)
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
        if (clearCacheEntryRow == null) {
            return
        }
        val subtitleView =
            clearCacheEntryRow!!.findViewById<MaterialTextView>(R.id.setting_subtitle)
        subtitleView.text = getString(R.string.settings_clear_cache_size, usage)
    }

    private fun showConfigBackupDialog(anchor: View?) {
        if (store == null) {
            showToast(R.string.status_save_requires_init)
            return
        }
        showBackupActions(
            activity,
            { this.launchExportBackupPicker() },
            { this.launchImportBackupPicker() })
    }

    private fun launchExportBackupPicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/json")
            .putExtra(Intent.EXTRA_TITLE, buildBackupFileName())
        try {
            startActivityForResult(intent, REQUEST_EXPORT_CONFIG_BACKUP)
        } catch (error: ActivityNotFoundException) {
            showToast(R.string.config_backup_picker_failed)
        }
    }

    private fun launchImportBackupPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
            .putExtra(
                Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/json",
                    "text/plain"
                )
            )
        try {
            startActivityForResult(intent, REQUEST_IMPORT_CONFIG_BACKUP)
        } catch (error: ActivityNotFoundException) {
            showToast(R.string.config_backup_picker_failed)
        }
    }

    private fun showImportBackupConfirmDialog(uri: Uri?) {
        show(
            activity,
            getString(R.string.config_backup_import_confirm_title),
            getString(R.string.config_backup_import_confirm_message),
            {
                importConfigBackup(uri)
                publishPresentationState()
            },
            { this.publishPresentationState() })
    }

    private fun exportConfigBackup(uri: Uri?) {
        val localStore = store
        if (localStore == null) {
            showToast(R.string.status_save_requires_init)
            return
        }
        Thread({
            val result = ConfigBackupCoordinator(
                activity.contentResolver, localStore, QuickTemplateStore(activity)
            )
                .export(uri)
            runOnUiThread {
                if (result.isSuccess()) {
                    showToast(R.string.config_backup_export_success)
                    publishPresentationState()
                    return@runOnUiThread
                }
                showToast(R.string.config_backup_export_failed)
                publishPresentationState()
            }
        }, "dpis-config-backup-export").start()
    }

    private fun importConfigBackup(uri: Uri?) {
        val localStore = store
        if (localStore == null) {
            showToast(R.string.status_save_requires_init)
            return
        }
        Thread({
            val result = ConfigBackupCoordinator(
                activity.contentResolver, localStore, QuickTemplateStore(activity)
            )
                .restore(uri)
            runOnUiThread {
                if (!result.isSuccess()) {
                    showToast(
                        if (result.code == ConfigBackupCoordinator.Code.INVALID_FILE)
                            R.string.config_backup_import_invalid
                        else
                            R.string.config_backup_import_failed
                    )
                    publishPresentationState()
                    return@runOnUiThread
                }
                showToast(R.string.config_backup_import_success)
                publishPresentationState()
                relaunchDpisTask()
            }
        }, "dpis-config-backup-import").start()
    }

    private fun relaunchDpisTask() {
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
        val intent = Intent(activity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finishAffinity()
    }

    private fun applyRestoredStoreState() {
        if (store == null) {
            return
        }
        selectedMode = store!!.fontDebugSelectedMode
        selectedWindow = store!!.fontDebugSelectedWindow

        setCheckedSilently(
            safeModeSwitch,
            store!!.isSystemServerSafeModeEnabled()
        ) { buttonView: CompoundButton?, isChecked: Boolean ->
            this.onSafeModeChanged(
                buttonView,
                isChecked
            )
        }
        setCheckedSilently(
            globalLogSwitch,
            store!!.isGlobalLogEnabled()
        ) { buttonView: CompoundButton?, isChecked: Boolean ->
            this.onGlobalLogChanged(
                buttonView,
                isChecked
            )
        }
        DpisLog.setLoggingEnabled(store!!.isGlobalLogEnabled())
        setInterfaceScalePercentSilently(AppUiScaleManager.getEffectiveScalePercent(activity))

        applyLauncherIconVisibilityFromStore()
        syncHooksSwitchWithScope()

        if (store!!.isFontDebugOverlayEnabled && canDrawOverlays()) {
            startFontDebugOverlayService()
        } else if (!store!!.isFontDebugOverlayEnabled) {
            stopService(Intent(activity, FontDebugOverlayService::class.java))
        }
        updateDialogButtons()
        refreshStatsPanel()
    }

    private fun refreshStoreState(showInitToast: Boolean) {
        store = DpisApplication.getConfigStore()
        if (store == null) {
            applyUnavailableStoreState(showInitToast)
            return
        }
        applyAvailableStoreState()
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

    private fun applyAvailableStoreState() {
        hooksEnabledSwitch!!.isEnabled = true
        safeModeSwitch!!.isEnabled = true
        globalLogSwitch!!.isEnabled = true
        hideLauncherIconSwitch!!.isEnabled = true
        interfaceScaleSlider!!.isEnabled = true
        setRowEnabled(fontDebugEntryRow, true)
        setRowEnabled(experimentalSettingsEntryRow, true)
        setRowEnabled(fontLibraryEntryRow, true)
        setRowEnabled(backupConfigEntryRow, true)
        setRowEnabled(interfaceScaleRow, true)
        hooksToggleController = SystemHooksToggleController(
            store,
            ActivitySystemScopeGateway(),
            ActivitySystemHooksToggleView()
        ) { this.publishPresentationState() }
        applyRestoredStoreState()
        setPrimarySwitchRowsVisible(true)
    }

    private fun applyUnavailableStoreState(showInitToast: Boolean) {
        hooksToggleController = null
        setPrimarySwitchRowsVisible(true)
        hooksEnabledSwitch!!.isEnabled = false
        safeModeSwitch!!.isEnabled = false
        globalLogSwitch!!.isEnabled = false
        hideLauncherIconSwitch!!.isEnabled = false
        interfaceScaleSlider!!.isEnabled = false
        setRowEnabled(fontDebugEntryRow, false)
        setRowEnabled(experimentalSettingsEntryRow, false)
        setRowEnabled(fontLibraryEntryRow, false)
        setRowEnabled(backupConfigEntryRow, false)
        setRowEnabled(interfaceScaleRow, false)
        setRowEnabled(languageEntryRow, false)
        if (showInitToast) {
            showToast(R.string.status_save_requires_init)
        }
    }

    private fun setPrimarySwitchRowsVisible(visible: Boolean) {
        if (primarySwitchCard == null) {
            return
        }
        primarySwitchCard!!.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun applyLauncherIconVisibilityFromStore() {
        if (hideLauncherIconSwitch == null) {
            return
        }
        val storedHidden = launcherIconVisibilityStore.isHidden()
        val actualHidden = resolveLauncherIconHiddenState(storedHidden)
        if (actualHidden != storedHidden) {
            launcherIconVisibilityStore.isHidden = actualHidden
        }
        setCheckedSilently(
            hideLauncherIconSwitch,
            actualHidden
        ) { buttonView: CompoundButton?, isChecked: Boolean ->
            this.onHideLauncherIconChanged(
                buttonView,
                isChecked
            )
        }
    }

    private fun buildBackupFileName(): String {
        return String.format(
            Locale.US,
            $$"dpis-backup-%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS.json",
            Date()
        )
    }

    private fun showFontDebugDialog(anchor: View?) {
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

    private fun onHooksEnabledChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (!BuildConfig.DEBUG) {
            return
        }
        if (hooksToggleController == null) {
            return
        }
        hooksToggleController!!.onUserToggle(isChecked)
        publishPresentationState()
    }

    private fun applySystemHooksRowVisibility() {
        val row = findViewById<View?>(R.id.row_system_hooks) ?: return
        row.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
    }

    private fun onSafeModeChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (store == null) {
            return
        }
        if (!isChecked) {
            showDisableSafeModeConfirmationDialog()
            return
        }
        if (!store!!.setSystemServerSafeModeEnabled(true)) {
            setCheckedSilently(
                safeModeSwitch,
                false
            ) { buttonView: CompoundButton?, isChecked: Boolean ->
                this.onSafeModeChanged(
                    buttonView,
                    isChecked
                )
            }
            showToast(R.string.system_settings_save_failed)
            return
        }
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
        publishPresentationState()
    }

    private fun showDisableSafeModeConfirmationDialog() {
        show(
            activity,
            activity.getString(R.string.system_safe_mode_disable_confirm_title),
            activity.getString(R.string.system_safe_mode_disable_confirm_message),
            {
                if (!store!!.setSystemServerSafeModeEnabled(false)) {
                    setCheckedSilently(
                        safeModeSwitch,
                        true
                    ) { buttonView: CompoundButton?, isChecked: Boolean ->
                        this.onSafeModeChanged(
                            buttonView,
                            isChecked
                        )
                    }
                    showToast(R.string.system_settings_save_failed)
                    publishPresentationState()
                    return@show
                }
                RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
                publishPresentationState()
            },
            {
                setCheckedSilently(
                    safeModeSwitch,
                    true
                ) { buttonView: CompoundButton?, isChecked: Boolean ->
                    this.onSafeModeChanged(
                        buttonView,
                        isChecked
                    )
                }
                publishPresentationState()
            })
    }

    private fun onGlobalLogChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (store == null) {
            return
        }
        if (!store!!.setGlobalLogEnabled(isChecked)) {
            setCheckedSilently(
                globalLogSwitch,
                !isChecked
            ) { buttonView: CompoundButton?, isChecked: Boolean ->
                this.onGlobalLogChanged(
                    buttonView,
                    isChecked
                )
            }
            showToast(R.string.system_settings_save_failed)
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

    private fun onHideLauncherIconChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (isChecked) {
            showHideLauncherIconConfirmationDialog()
            return
        }
        if (!persistLauncherIconState(false)) {
            setCheckedSilently(
                hideLauncherIconSwitch,
                true
            ) { buttonView: CompoundButton?, isChecked: Boolean ->
                this.onHideLauncherIconChanged(
                    buttonView,
                    isChecked
                )
            }
        }
        publishPresentationState()
    }

    private fun showHideLauncherIconConfirmationDialog() {
        show(
            activity,
            activity.getString(R.string.settings_hide_launcher_icon_confirm_title),
            activity.getString(R.string.settings_hide_launcher_icon_confirm_message),
            {
                if (!persistLauncherIconState(true)) {
                    setCheckedSilently(
                        hideLauncherIconSwitch, false
                    ) { buttonView: CompoundButton?, isChecked: Boolean ->
                        this.onHideLauncherIconChanged(
                            buttonView,
                            isChecked
                        )
                    }
                }
                publishPresentationState()
            },
            {
                setCheckedSilently(
                    hideLauncherIconSwitch, false
                ) { buttonView: CompoundButton?, isChecked: Boolean ->
                    this.onHideLauncherIconChanged(
                        buttonView,
                        isChecked
                    )
                }
                publishPresentationState()
            })
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

    private fun setCheckedSilently(
        switchView: CompoundButton?,
        checked: Boolean,
        listener: CompoundButton.OnCheckedChangeListener?
    ) {
        if (switchView == null) {
            return
        }
        switchView.setOnCheckedChangeListener(null)
        switchView.isChecked = checked
        switchView.setOnCheckedChangeListener(listener)
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

    private fun syncLauncherIconSwitch() {
        if (hideLauncherIconSwitch == null) {
            return
        }
        val storedHidden = launcherIconVisibilityStore.isHidden()
        val hidden = resolveLauncherIconHiddenState(storedHidden)
        if (hidden != storedHidden) {
            launcherIconVisibilityStore.isHidden = hidden
        }
        setCheckedSilently(
            hideLauncherIconSwitch,
            hidden
        ) { buttonView: CompoundButton?, isChecked: Boolean ->
            this.onHideLauncherIconChanged(
                buttonView,
                isChecked
            )
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
        override fun render(state: SystemHookState) {
            if (hooksEnabledSwitch == null) {
                return
            }
            setCheckedSilently(
                hooksEnabledSwitch, state.switchChecked
            ) { buttonView: CompoundButton?, isChecked: Boolean ->
                this@SystemServerSettingsPageController.onHooksEnabledChanged(
                    buttonView,
                    isChecked
                )
            }
            hooksEnabledSwitch!!.isEnabled = state.switchEnabled
        }

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
        private const val REQUEST_EXPORT_CONFIG_BACKUP = 1001
        private const val REQUEST_IMPORT_CONFIG_BACKUP = 1002
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

        private fun setRowEnabled(row: View?, enabled: Boolean) {
            if (row == null) {
                return
            }
            row.isEnabled = enabled
            row.alpha = if (enabled) 1f else 0.5f
        }
    }
}
