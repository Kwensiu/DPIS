package com.dpis.module.settings

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import com.dpis.module.settings.AppLocaleManager
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.settings.ThemeModeStore
import com.dpis.module.ui.WatchUiMode
import com.dpis.module.ui.presentation.wrapInterfaceScaleContext
import com.dpis.module.ui.compose.applyComposeWindowBackground

/** Applies app-level locale, interface scale, and theme changes across activity boundaries. */
abstract class LocalizedActivity : ComponentActivity() {
    private var activeLanguageTag = AppLocaleManager.TAG_FOLLOW_SYSTEM
    private var activeInterfaceScalePercent = AppUiScaleManager.DEFAULT_SCALE_PERCENT
    private var activePredictiveBackEnabled = true
    private var activeAppearance = ThemeModeStore.AppearancePreferences(
        mode = ThemeModeStore.FOLLOW_SYSTEM,
        dynamicColorEnabled = true,
        themeColor = ThemeModeStore.DEFAULT_STATIC_THEME_COLOR,
        paletteStyle = ThemeModeStore.STYLE_TONAL_SPOT,
        colorSpecification = ThemeModeStore.SPEC_2025,
    )
    private val legacySystemBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (isTaskRoot) {
                onUnhandledTaskRootBack()
            } else {
                finish()
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(wrapInterfaceScaleContext(AppLocaleManager.wrap(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activeLanguageTag = AppLocaleManager.getLanguageTag(this)
        activeInterfaceScalePercent = currentInterfaceScalePercent()
        activeAppearance = ThemeModeStore.getAppearance(this)
        super.onCreate(savedInstanceState)
        // Keep every user-visible Activity on the same edge-to-edge window contract.
        // Individual content surfaces remain responsible for their own safe-area insets.
        enableEdgeToEdge()
        applyComposeWindowBackground()
        // Keep the edge-to-edge root at a stable size while the IME animates. The Compose shell
        // owns the single whole-screen pan, so adjustResize must not resize the root underneath
        // it and snap the content back before the close animation can finish.
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        onBackPressedDispatcher.addCallback(this, legacySystemBack)
        applyPredictiveBackPreference()
    }

    override fun onResume() {
        super.onResume()
        val currentLanguageTag = AppLocaleManager.getLanguageTag(this)
        val currentInterfaceScalePercent = currentInterfaceScalePercent()
        val currentAppearance = ThemeModeStore.getAppearance(this)
        val currentPredictiveBackEnabled = PageSettingsStore.isPredictiveBackEnabled(this)
        if (currentPredictiveBackEnabled != activePredictiveBackEnabled) {
            applyPredictiveBackPreference()
        }
        if (currentAppearance != activeAppearance) {
            activeAppearance = currentAppearance
            if (!handleAppearanceChangeOnResume()) {
                recreate()
                return
            }
        }
        if (currentLanguageTag != activeLanguageTag ||
            currentInterfaceScalePercent != activeInterfaceScalePercent
        ) {
            activeLanguageTag = currentLanguageTag
            activeInterfaceScalePercent = currentInterfaceScalePercent
            recreate()
        }
    }

    /**
     * Called when appearance changed in another Activity. Return true if this
     * screen applied the new colors without recreating.
     */
    protected open fun handleAppearanceChangeOnResume(): Boolean = false

    /** Predictive-off back on a task root. Secondary Activities finish. */
    protected open fun onUnhandledTaskRootBack() {
        finish()
    }

    /**
     * Records an appearance change that this Activity has already rendered through Compose.
     *
     * Theme settings update the active color scheme in place. Without this acknowledgement,
     * returning to the page later would make {@link #onResume()} recreate the Activity for the
     * same preference change and unnecessarily dismiss any restored transient UI.
     */
    fun markAppearanceAppliedInPlace() {
        activeAppearance = ThemeModeStore.getAppearance(this)
    }

    fun markLocaleAppliedInPlace() {
        activeLanguageTag = AppLocaleManager.getLanguageTag(this)
    }

    fun markInterfaceScaleAppliedInPlace() {
        activeInterfaceScalePercent = currentInterfaceScalePercent()
    }

    fun applyPredictiveBackPreferenceInPlace() {
        applyPredictiveBackPreference()
    }

    private fun currentInterfaceScalePercent(): Int {
        val store = InterfaceScaleStore(this)
        return AppUiScaleManager.effectiveScalePercent(
            store,
            WatchUiMode.shouldUseCompactUi(this),
        )
    }

    private fun applyPredictiveBackPreference() {
        activePredictiveBackEnabled = PageSettingsStore.isPredictiveBackEnabled(this)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            legacySystemBack.isEnabled = false
            return
        }
        // When predictive back is off, consume the unhandled system back so the platform
        // does not play back-to-home / cross-activity previews.
        legacySystemBack.isEnabled = !activePredictiveBackEnabled
    }
}
