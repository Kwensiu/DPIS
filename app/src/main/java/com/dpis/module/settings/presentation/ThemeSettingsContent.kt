package com.dpis.module.settings.presentation

import android.os.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.dpis.module.R
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.settings.PageSettingsStore
import com.dpis.module.ui.dialog.ModalDialog
import com.dpis.module.ui.presentation.design.ColorSchemeFactory
import com.dpis.module.ui.presentation.design.rememberClickValueAction
import com.dpis.module.ui.presentation.editor.DpisSwitch
import com.dpis.module.ui.presentation.editor.rememberTextInputFocusBoundary
import com.dpis.module.ui.presentation.workspace.AnimatedConditionalItem
import com.dpis.module.ui.presentation.workspace.SecondaryPageScaffold
import kotlin.math.roundToInt

/** The first focused page in Theme settings; further appearance controls can join this list. */
@Composable
fun ThemeSettingsContent(
    mode: String,
    dynamicColorEnabled: Boolean,
    themeColor: String,
    paletteStyle: String,
    colorSpecification: String,
    interfaceScalePercent: Int,
    showHomeEditButton: Boolean = true,
    defaultStartupPage: String = PageSettingsStore.HOME,
    predictiveBackEnabled: Boolean = true,
    onModeSelected: (String) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onThemeColorSelected: (String) -> Unit,
    onPaletteStyleSelected: (String) -> Unit,
    onColorSpecificationSelected: (String) -> Unit,
    onInterfaceScaleChanged: (Int) -> Unit,
    onShowHomeEditButtonChanged: (Boolean) -> Unit = {},
    onDefaultStartupPageSelected: (String) -> Unit = {},
    onPredictiveBackChanged: (Boolean) -> Unit = {},
    onBack: () -> Unit,
) {
    var showModeDialog by rememberSaveable { mutableStateOf(false) }
    var showPaletteDialog by rememberSaveable { mutableStateOf(false) }
    var showSpecificationDialog by rememberSaveable { mutableStateOf(false) }
    var showScaleDialog by rememberSaveable { mutableStateOf(false) }
    var showStartupPageDialog by rememberSaveable { mutableStateOf(false) }
    var pendingScale by remember(interfaceScalePercent) {
        mutableFloatStateOf(interfaceScalePercent.toFloat())
    }
    SecondaryPageScaffold(
        titleRes = R.string.settings_theme_settings_title,
        onBack = onBack,
    ) {
        item {
            ThemeSettingsSection(R.string.settings_theme_section_appearance) {
                ThemeChoiceMenuAnchor(
                    expanded = showModeDialog,
                    options = themeModeOptions,
                    selected = mode,
                    onDismiss = { showModeDialog = false },
                    onSelected = {
                        onModeSelected(it)
                        showModeDialog = false
                    },
                ) {
                    ThemeSettingsEntry(
                        icon = R.drawable.ic_routine_24,
                        title = R.string.settings_theme_mode_label,
                        summary = R.string.settings_theme_mode_hint,
                        value = themeModeLabel(mode),
                        index = 0,
                        total = if (dynamicColorEnabled) 5 else 6,
                        onClick = { showModeDialog = true },
                    )
                }
                ThemeDynamicColorRow(
                    checked = dynamicColorEnabled,
                    onCheckedChange = onDynamicColorChanged,
                    index = 1,
                    total = if (dynamicColorEnabled) 5 else 6,
                )
                AnimatedConditionalItem(visible = !dynamicColorEnabled) {
                    ThemeColorRow(
                        selectedColor = themeColor,
                        paletteStyle = paletteStyle,
                        colorSpecification = colorSpecification,
                        onColorSelected = onThemeColorSelected,
                        index = 2,
                        total = 6,
                    )
                }
                ThemeChoiceMenuAnchor(
                    expanded = showPaletteDialog,
                    options = paletteStyleOptions,
                    selected = paletteStyle,
                    onDismiss = { showPaletteDialog = false },
                    onSelected = {
                        onPaletteStyleSelected(it)
                        showPaletteDialog = false
                    },
                ) {
                    ThemeStaticOptionRow(
                        icon = R.drawable.ic_style_24,
                        title = R.string.settings_theme_palette_style_label,
                        value = paletteStyleLabel(paletteStyle),
                        index = if (dynamicColorEnabled) 2 else 3,
                        total = if (dynamicColorEnabled) 5 else 6,
                        onClick = { showPaletteDialog = true },
                    )
                }
                ThemeChoiceMenuAnchor(
                    expanded = showSpecificationDialog,
                    options = if (ColorSchemeFactory.supports2025Specification(paletteStyle)) {
                        colorSpecificationOptions
                    } else {
                        colorSpecificationOptions.take(1)
                    },
                    selected = colorSpecification,
                    onDismiss = { showSpecificationDialog = false },
                    onSelected = {
                        onColorSpecificationSelected(it)
                        showSpecificationDialog = false
                    },
                ) {
                    ThemeStaticOptionRow(
                        icon = R.drawable.ic_design_services_24,
                        title = R.string.settings_theme_color_spec_label,
                        value = colorSpecificationLabel(paletteStyle, colorSpecification),
                        index = if (dynamicColorEnabled) 3 else 4,
                        total = if (dynamicColorEnabled) 5 else 6,
                        onClick = { showSpecificationDialog = true },
                    )
                }
                ThemeInterfaceScaleRow(
                    pendingScale = pendingScale,
                    onPendingScaleChanged = { pendingScale = it },
                    onScaleChanged = onInterfaceScaleChanged,
                    onClick = { showScaleDialog = true },
                    index = if (dynamicColorEnabled) 4 else 5,
                    total = if (dynamicColorEnabled) 5 else 6,
                )
            }
        }
        item {
            ThemeSettingsSection(R.string.settings_theme_section_page) {
                val predictiveBackAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                val pageItemCount = if (predictiveBackAvailable) 3 else 2
                if (predictiveBackAvailable) {
                    ThemePredictiveBackRow(
                        checked = predictiveBackEnabled,
                        onCheckedChange = onPredictiveBackChanged,
                        index = 0,
                        total = pageItemCount,
                    )
                }
                val changeHomeEditButton = rememberClickValueAction(onShowHomeEditButtonChanged)
                ThemeSegmentedSurfaceRow(
                    onClick = { changeHomeEditButton(!showHomeEditButton) },
                    index = if (predictiveBackAvailable) 1 else 0,
                    total = pageItemCount,
                    leadingContent = { Icon(painterResource(R.drawable.ic_edit_24), null) },
                    content = {
                        Text(
                            stringResource(R.string.settings_page_home_edit_button),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    trailingContent = {
                        DpisSwitch(
                            checked = showHomeEditButton,
                            onCheckedChange = changeHomeEditButton,
                        )
                    },
                )
                ThemeSegmentedSurfaceRow(
                    onClick = { showStartupPageDialog = true },
                    index = if (predictiveBackAvailable) 2 else 1,
                    total = pageItemCount,
                    leadingContent = { Icon(painterResource(R.drawable.ic_home_24), null) },
                    content = {
                        Text(
                            stringResource(R.string.settings_page_default_startup),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingContent = {
                        Text(
                            stringResource(R.string.settings_page_default_startup_hint),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    trailingContent = {
                        Text(
                            stringResource(startupPageLabel(defaultStartupPage)),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }
        }
    }
    if (showScaleDialog) {
        val scaleInputBoundary = rememberTextInputFocusBoundary()
        ModalDialog(
            onDismissRequest = { showScaleDialog = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
            imeFocusBoundary = scaleInputBoundary,
        ) {
            InterfaceScaleDialogContent(
                initialPercent = pendingScale.roundToInt(),
                minimumPercent = AppUiScaleManager.MIN_SCALE_PERCENT,
                maximumPercent = AppUiScaleManager.MAX_SCALE_PERCENT,
                inputFocusBoundary = scaleInputBoundary,
                onCancel = { showScaleDialog = false },
                onSave = {
                    showScaleDialog = false
                    pendingScale = it.toFloat()
                    onInterfaceScaleChanged(it)
                },
            )
        }
    }
    if (showStartupPageDialog) {
        PageNavigationDialog(
            selectedStartupPage = defaultStartupPage,
            onStartupPageSelected = onDefaultStartupPageSelected,
            onDismiss = { showStartupPageDialog = false },
        )
    }
}
