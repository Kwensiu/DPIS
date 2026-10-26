package com.dpis.module.onboarding.presentation

import androidx.compose.runtime.Composable
import com.dpis.module.settings.ThemeModeStore

@Composable
internal fun OnboardingConfigurationPage(
    configuration: OnboardingConfigurationPresenter?,
    request: OnboardingGate.Request,
) {
    val configurationState = configuration?.state
    val appearanceState = configuration?.appearanceState
    ConfigurationStep(
        configurationState = configurationState,
        onImport = { configuration?.launchImportPicker() },
        onRequestScope = { configuration?.requestRestoreScope() },
        mode = appearanceState?.mode ?: ThemeModeStore.FOLLOW_SYSTEM,
        dynamicColorEnabled = appearanceState?.dynamicColorEnabled ?: false,
        themeColor = appearanceState?.themeColor
            ?: ThemeModeStore.DEFAULT_STATIC_THEME_COLOR,
        paletteStyle = appearanceState?.paletteStyle ?: ThemeModeStore.STYLE_TONAL_SPOT,
        colorSpecification = appearanceState?.colorSpecification
            ?: ThemeModeStore.SPEC_2025,
        interfaceScalePercent = appearanceState?.interfaceScalePercent ?: 100,
        onModeSelected = {
            configuration?.selectMode(it)
            request.onAppearanceChanged()
        },
        onDynamicColorChanged = {
            configuration?.setDynamicColorEnabled(it)
            request.onAppearanceChanged()
        },
        onThemeColorSelected = {
            configuration?.selectThemeColor(it)
            request.onAppearanceChanged()
        },
        onPaletteStyleSelected = {
            configuration?.selectPaletteStyle(it)
            request.onAppearanceChanged()
        },
        onColorSpecificationSelected = {
            configuration?.selectColorSpecification(it)
            request.onAppearanceChanged()
        },
        onInterfaceScaleChanged = {
            if (configuration?.setInterfaceScalePercent(it) == true) {
                request.onConfigurationChanged()
            }
        },
    )
}
