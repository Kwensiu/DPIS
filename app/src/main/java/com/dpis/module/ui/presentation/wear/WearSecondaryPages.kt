package com.dpis.module.ui.compose

import com.dpis.module.ui.dialog.ModalDialog

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.dpis.module.R
import com.dpis.module.about.OpenSourceLicenseActivity
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.settings.ThemeModeStore
import com.dpis.module.ui.presentation.WearMaterialTheme
import com.dpis.module.ui.presentation.WearWorkspaceList

@Composable
internal fun WearThemeSettingsContent(
    mode: String,
    dynamicColorEnabled: Boolean,
    themeColor: String,
    paletteStyle: String,
    colorSpecification: String,
    interfaceScalePercent: Int,
    onModeSelected: (String) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onThemeColorSelected: (String) -> Unit,
    onPaletteStyleSelected: (String) -> Unit,
    onColorSpecificationSelected: (String) -> Unit,
    onInterfaceScaleChanged: (Int) -> Unit,
) {
    var showScaleDialog by remember { mutableStateOf(false) }
    var pendingScale by remember(interfaceScalePercent) {
        mutableFloatStateOf(interfaceScalePercent.toFloat())
    }
    val supports2025 = ColorSchemeFactory.supports2025Specification(paletteStyle)
    val modeLabel = stringResource(R.string.settings_theme_mode_label)
    val modeValue = stringResource(themeModeLabel(mode))
    val themeColorLabel = stringResource(R.string.settings_theme_color_label)
    val themeColorValue = stringResource(themeColorLabel(themeColor))
    val paletteLabel = stringResource(R.string.settings_theme_palette_style_label)
    val paletteValue = stringResource(paletteStyleLabel(paletteStyle))
    val colorSpecificationLabel = stringResource(R.string.settings_theme_color_spec_label)
    val colorSpecificationValue = stringResource(
        colorSpecificationLabel(paletteStyle, colorSpecification)
    )
    val interfaceScaleLabel = stringResource(R.string.settings_interface_scale_label)
    val interfaceScaleValue = stringResource(
        R.string.settings_interface_scale_value,
        interfaceScalePercent,
    )
    WearWorkspaceList(title = R.string.settings_theme_settings_title) {
        wearButton(
            key = "mode",
            label = modeLabel,
            secondaryLabel = modeValue,
            icon = R.drawable.ic_routine_24,
            onClick = { onModeSelected(nextThemeMode(mode)) },
        )
        wearSwitch(
            key = "dynamic",
            label = R.string.settings_theme_dynamic_color_label,
            checked = dynamicColorEnabled,
            enabled = true,
            onChanged = onDynamicColorChanged,
        )
        if (!dynamicColorEnabled) {
            wearButton(
                key = "theme-color",
                label = themeColorLabel,
                secondaryLabel = themeColorValue,
                icon = R.drawable.ic_pie_chart_24,
                onClick = { onThemeColorSelected(nextThemeColor(themeColor)) },
            )
        }
        wearButton(
            key = "palette",
            label = paletteLabel,
            secondaryLabel = paletteValue,
            icon = R.drawable.ic_style_24,
            onClick = { onPaletteStyleSelected(nextPaletteStyle(paletteStyle)) },
        )
        wearButton(
            key = "spec",
            label = colorSpecificationLabel,
            secondaryLabel = colorSpecificationValue,
            icon = R.drawable.ic_design_services_24,
            enabled = supports2025,
            onClick = {
                onColorSpecificationSelected(nextColorSpecification(colorSpecification))
            },
        )
        wearButton(
            key = "scale",
            label = interfaceScaleLabel,
            secondaryLabel = interfaceScaleValue,
            icon = R.drawable.ic_fit_width_24,
            onClick = { showScaleDialog = true },
        )
    }
    if (showScaleDialog) {
        ModalDialog(onDismissRequest = { showScaleDialog = false }) {
            InterfaceScaleDialogContent(
                initialPercent = pendingScale.toInt(),
                minimumPercent = AppUiScaleManager.MIN_SCALE_PERCENT,
                maximumPercent = AppUiScaleManager.MAX_SCALE_PERCENT,
                onCancel = { showScaleDialog = false },
                onSave = {
                    pendingScale = it.toFloat()
                    showScaleDialog = false
                    onInterfaceScaleChanged(it)
                },
            )
        }
    }
}

@Composable
internal fun WearAboutContent(
    versionText: String,
    showDebugUpdateEntry: Boolean,
    onCheckUpdates: () -> Unit,
    onShowDebugUpdate: () -> Unit,
    onOpenSource: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    val updateLabel = stringResource(R.string.about_link_update_title)
    val sourceLabel = stringResource(R.string.about_link_source_title)
    val debugUpdateLabel = stringResource(R.string.about_link_update_dialog_debug_only_title)
    val feedbackLabel = stringResource(R.string.about_link_feedback_title)
    val licensesLabel = stringResource(R.string.open_source_license)
    WearWorkspaceList(title = R.string.about_title) {
        wearInfoCard(
            key = "about-summary",
            title = appName,
            secondaryLabel = versionText,
            icon = R.drawable.ic_info_24,
        )
        wearButton(
            key = "check-updates",
            label = updateLabel,
            icon = R.drawable.ic_refresh_24,
            onClick = onCheckUpdates,
        )
        wearButton(
            key = "source",
            label = sourceLabel,
            icon = R.drawable.ic_code_24,
            onClick = onOpenSource,
        )
        if (showDebugUpdateEntry) {
            wearButton(
                key = "debug-update",
                label = debugUpdateLabel,
                icon = R.drawable.ic_refresh_24,
                onClick = onShowDebugUpdate,
            )
        }
        wearButton(
            key = "feedback",
            label = feedbackLabel,
            icon = R.drawable.ic_adjust_24,
            onClick = onOpenFeedback,
        )
        wearButton(
            key = "licenses",
            label = licensesLabel,
            icon = R.drawable.ic_license_24,
            onClick = onOpenLicenses,
        )
    }
}

@Composable
internal fun WearFontLibraryContent(
    presentation: FontLibraryPresentation,
    onImportFont: () -> Unit,
    onExportArchive: () -> Unit,
    onImportArchive: () -> Unit,
    onFontSelected: (String) -> Unit,
) {
    val importFontLabel = stringResource(R.string.font_library_import_action)
    val importArchiveLabel = stringResource(R.string.font_library_import_archive_action)
    val exportArchiveLabel = stringResource(R.string.font_library_export_archive_action)
    val emptyLabel = stringResource(R.string.font_library_empty)
    WearWorkspaceList(title = R.string.font_library_page_title) {
        wearButton(
            key = "import-font",
            label = importFontLabel,
            icon = R.drawable.ic_upload_file_24,
            onClick = onImportFont,
        )
        wearButton(
            key = "import-archive",
            label = importArchiveLabel,
            icon = R.drawable.ic_upload_file_24,
            onClick = onImportArchive,
        )
        wearButton(
            key = "export-archive",
            label = exportArchiveLabel,
            icon = R.drawable.ic_save_24dp,
            onClick = onExportArchive,
        )
        if (presentation.items.isEmpty()) {
            wearInfoCard(
                key = "empty",
                title = emptyLabel,
                icon = R.drawable.ic_info_24,
            )
        } else {
            presentation.items.forEach { item ->
                wearButton(
                    key = item.id,
                    label = item.title,
                    secondaryLabel = if (item.inUse) item.subtitle else null,
                    icon = if (item.inUse) R.drawable.ic_check_24 else R.drawable.ic_upload_file_24,
                    onClick = { onFontSelected(item.id) },
                )
            }
        }
    }
}

@Composable
internal fun WearOpenSourceLicenseContent(
    items: List<OpenSourceLicenseActivity.LicenseItem>,
    onItemSelected: (OpenSourceLicenseActivity.LicenseItem) -> Unit,
) {
    WearWorkspaceList(title = R.string.open_source_license) {
        items.forEach { item ->
            wearButton(
                key = "${item.name}\u0000${item.website}",
                label = item.name,
                secondaryLabel = item.summary,
                icon = R.drawable.ic_license_24,
                onClick = { onItemSelected(item) },
            )
        }
    }
}

@Composable
internal fun WearExperimentalSettingsContent() {
    WearMaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(LocalWearWorkspaceContentPadding.current),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.settings_experimental_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val wearThemeModeOptions = listOf(
    ThemeModeStore.FOLLOW_SYSTEM to R.string.settings_theme_mode_follow_system,
    ThemeModeStore.LIGHT to R.string.settings_theme_mode_light,
    ThemeModeStore.DARK to R.string.settings_theme_mode_dark,
)

private val wearPaletteStyleOptions = listOf(
    ThemeModeStore.STYLE_TONAL_SPOT to R.string.settings_theme_palette_style_tonal_spot,
    ThemeModeStore.STYLE_NEUTRAL to R.string.settings_theme_palette_style_neutral,
    ThemeModeStore.STYLE_VIBRANT to R.string.settings_theme_palette_style_vibrant,
    ThemeModeStore.STYLE_EXPRESSIVE to R.string.settings_theme_palette_style_expressive,
    ThemeModeStore.STYLE_RAINBOW to R.string.settings_theme_palette_style_rainbow,
    ThemeModeStore.STYLE_FRUIT_SALAD to R.string.settings_theme_palette_style_fruit_salad,
    ThemeModeStore.STYLE_MONOCHROME to R.string.settings_theme_palette_style_monochrome,
    ThemeModeStore.STYLE_FIDELITY to R.string.settings_theme_palette_style_fidelity,
    ThemeModeStore.STYLE_CONTENT to R.string.settings_theme_palette_style_content,
)

private val wearColorSpecificationOptions = listOf(
    ThemeModeStore.SPEC_2021 to R.string.settings_theme_color_spec_2021,
    ThemeModeStore.SPEC_2025 to R.string.settings_theme_color_spec_2025,
)

private val wearThemeColorOptions = listOf(
    ThemeModeStore.COLOR_PURPLE to R.string.settings_theme_color_name_purple,
    ThemeModeStore.COLOR_PINK to R.string.settings_theme_color_name_pink,
    ThemeModeStore.COLOR_RED to R.string.settings_theme_color_name_red,
    ThemeModeStore.COLOR_ORANGE to R.string.settings_theme_color_name_orange,
    ThemeModeStore.COLOR_AMBER to R.string.settings_theme_color_name_amber,
    ThemeModeStore.COLOR_YELLOW to R.string.settings_theme_color_name_yellow,
    ThemeModeStore.COLOR_LIME to R.string.settings_theme_color_name_lime,
    ThemeModeStore.COLOR_GREEN to R.string.settings_theme_color_name_green,
    ThemeModeStore.COLOR_CYAN to R.string.settings_theme_color_name_cyan,
    ThemeModeStore.COLOR_TEAL to R.string.settings_theme_color_name_teal,
    ThemeModeStore.COLOR_LIGHT_BLUE to R.string.settings_theme_color_name_light_blue,
    ThemeModeStore.COLOR_BLUE to R.string.settings_theme_color_name_blue,
    ThemeModeStore.COLOR_INDIGO to R.string.settings_theme_color_name_indigo,
    ThemeModeStore.COLOR_DEEP_PURPLE to R.string.settings_theme_color_name_deep_purple,
    ThemeModeStore.COLOR_BLUE_GREY to R.string.settings_theme_color_name_blue_grey,
    ThemeModeStore.COLOR_BROWN to R.string.settings_theme_color_name_brown,
    ThemeModeStore.COLOR_GREY to R.string.settings_theme_color_name_grey,
)

private fun nextThemeMode(current: String): String =
    nextOptionValue(wearThemeModeOptions, current)

@StringRes
private fun themeModeLabel(mode: String): Int =
    optionLabel(wearThemeModeOptions, mode)

private fun nextPaletteStyle(current: String): String =
    nextOptionValue(wearPaletteStyleOptions, current)

@StringRes
private fun paletteStyleLabel(value: String): Int =
    optionLabel(wearPaletteStyleOptions, value)

private fun nextColorSpecification(current: String): String =
    nextOptionValue(wearColorSpecificationOptions, current)

@StringRes
private fun colorSpecificationLabel(paletteStyle: String, value: String): Int {
    val effectiveValue = if (ColorSchemeFactory.supports2025Specification(paletteStyle)) {
        value
    } else {
        ThemeModeStore.SPEC_2021
    }
    return optionLabel(wearColorSpecificationOptions, effectiveValue)
}

private fun nextThemeColor(current: String): String =
    nextOptionValue(wearThemeColorOptions, current)

@StringRes
private fun themeColorLabel(value: String): Int =
    optionLabel(wearThemeColorOptions, value)

private fun nextOptionValue(options: List<Pair<String, Int>>, current: String): String {
    val index = options.indexOfFirst { it.first == current }
    return options[((if (index >= 0) index else 0) + 1) % options.size].first
}

@StringRes
private fun optionLabel(options: List<Pair<String, Int>>, value: String): Int =
    options.firstOrNull { it.first == value }?.second ?: options.first().second
