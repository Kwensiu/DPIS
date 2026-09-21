package com.dpis.module.settings.presentation

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.settings.PageSettingsStore
import com.dpis.module.settings.ThemeModeStore
import com.dpis.module.ui.WatchUiMode
import com.dpis.module.ui.presentation.design.ComposeDesignSystem
import com.dpis.module.ui.presentation.design.findActivity
import com.dpis.module.ui.presentation.wear.WearThemeSettingsContent

@Composable
fun ThemeSettingsRoute(
    onBack: () -> Unit,
    compactUi: Boolean,
    onAppearanceChanged: () -> Unit,
    onInterfaceScaleChanged: () -> Unit,
    onPredictiveBackChanged: () -> Unit,
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(ThemeModeStore.getMode(context)) }
    var dynamicColorEnabled by remember {
        mutableStateOf(ThemeModeStore.isDynamicColorEnabled(context))
    }
    var themeColor by remember { mutableStateOf(ThemeModeStore.getThemeColor(context)) }
    var paletteStyle by remember { mutableStateOf(ThemeModeStore.getPaletteStyle(context)) }
    var colorSpecification by remember {
        mutableStateOf(ThemeModeStore.getColorSpecification(context))
    }
    var showHomeEditButton by remember {
        mutableStateOf(PageSettingsStore.isHomeEditButtonVisible(context))
    }
    var defaultStartupPage by remember {
        mutableStateOf(PageSettingsStore.getDefaultStartupPage(context))
    }
    var predictiveBackEnabled by remember {
        mutableStateOf(PageSettingsStore.isPredictiveBackEnabled(context))
    }
    val persistAppearance: () -> Unit = {
        (context.findActivity() as? LocalizedActivity)?.markAppearanceAppliedInPlace()
        onAppearanceChanged()
    }
    val applyMode: (String) -> Unit = { selectedMode ->
        ThemeModeStore.setMode(context, selectedMode)
        mode = selectedMode
        persistAppearance()
    }
    val applyDynamicColor: (Boolean) -> Unit = { enabled ->
        ThemeModeStore.setDynamicColorEnabled(context, enabled)
        dynamicColorEnabled = enabled
        persistAppearance()
    }
    val applyColor: (String) -> Unit = { color ->
        ThemeModeStore.setThemeColor(context, color)
        themeColor = color
        persistAppearance()
    }
    val applyPalette: (String) -> Unit = { style ->
        ThemeModeStore.setPaletteStyle(context, style)
        paletteStyle = style
        persistAppearance()
    }
    val applySpec: (String) -> Unit = { specification ->
        ThemeModeStore.setColorSpecification(context, specification)
        colorSpecification = specification
        persistAppearance()
    }
    val applyScale: (Int) -> Unit = { percent ->
        val store = interfaceScaleStore(context)
        val normalized = AppUiScaleManager.normalizeScalePercent(percent)
        if (normalized != store.percent || !store.hasExplicitPercent) {
            if (store.setPercent(normalized)) {
                (context.findActivity() as? LocalizedActivity)
                    ?.markInterfaceScaleAppliedInPlace()
                onInterfaceScaleChanged()
            }
        }
    }
    val scalePercent = interfaceScaleStore(context).percent
    if (compactUi) {
        WearThemeSettingsContent(
            mode = mode,
            dynamicColorEnabled = dynamicColorEnabled,
            themeColor = themeColor,
            paletteStyle = paletteStyle,
            colorSpecification = colorSpecification,
            interfaceScalePercent = scalePercent,
            onModeSelected = applyMode,
            onDynamicColorChanged = applyDynamicColor,
            onThemeColorSelected = applyColor,
            onPaletteStyleSelected = applyPalette,
            onColorSpecificationSelected = applySpec,
            onInterfaceScaleChanged = applyScale,
        )
    } else {
        ThemeSettingsContent(
            mode = mode,
            dynamicColorEnabled = dynamicColorEnabled,
            themeColor = themeColor,
            paletteStyle = paletteStyle,
            colorSpecification = colorSpecification,
            interfaceScalePercent = scalePercent,
            onModeSelected = applyMode,
            onDynamicColorChanged = applyDynamicColor,
            onThemeColorSelected = applyColor,
            onPaletteStyleSelected = applyPalette,
            onColorSpecificationSelected = applySpec,
            onInterfaceScaleChanged = applyScale,
            onShowHomeEditButtonChanged = { value ->
                PageSettingsStore.setHomeEditButtonVisible(context, value)
                showHomeEditButton = value
            },
            onDefaultStartupPageSelected = { value ->
                PageSettingsStore.setDefaultStartupPage(context, value)
                defaultStartupPage = value
            },
            onPredictiveBackChanged = { value ->
                if (value != predictiveBackEnabled) {
                    PageSettingsStore.setPredictiveBackEnabled(context, value)
                    predictiveBackEnabled = value
                    (context.findActivity() as? LocalizedActivity)
                        ?.applyPredictiveBackPreferenceInPlace()
                    onPredictiveBackChanged()
                }
            },
            showHomeEditButton = showHomeEditButton,
            defaultStartupPage = defaultStartupPage,
            predictiveBackEnabled = predictiveBackEnabled,
            onBack = onBack,
        )
    }
}

fun ComponentActivity.installThemeSettings() {
    setContent {
        var appearanceEpoch by remember { mutableIntStateOf(0) }
        appearanceEpoch
        val mode = ThemeModeStore.getMode(this@installThemeSettings)
        ComposeDesignSystem(
            darkTheme = ThemeModeStore.resolveDarkTheme(mode, isSystemInDarkTheme()),
            dynamicColor = ThemeModeStore.isDynamicColorEnabled(this@installThemeSettings),
            themeColor = ThemeModeStore.getThemeColor(this@installThemeSettings),
            paletteStyle = ThemeModeStore.getPaletteStyle(this@installThemeSettings),
            colorSpecification = ThemeModeStore.getColorSpecification(this@installThemeSettings),
        ) {
            ThemeSettingsRoute(
                onBack = ::finish,
                compactUi = WatchUiMode.shouldUseCompactUi(this@installThemeSettings),
                onAppearanceChanged = { appearanceEpoch++ },
                onInterfaceScaleChanged = { recreate() },
                onPredictiveBackChanged = { },
            )
        }
    }
}
