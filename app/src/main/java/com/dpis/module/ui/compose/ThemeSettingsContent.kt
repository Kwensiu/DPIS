package com.dpis.module.ui.compose

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.settings.ThemeModeStore
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
    onModeSelected: (String) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onThemeColorSelected: (String) -> Unit,
    onPaletteStyleSelected: (String) -> Unit,
    onColorSpecificationSelected: (String) -> Unit,
    onInterfaceScaleChanged: (Int) -> Unit,
    onBack: () -> Unit,
) {
    var showModeDialog by rememberSaveable { mutableStateOf(false) }
    var showPaletteDialog by rememberSaveable { mutableStateOf(false) }
    var showSpecificationDialog by rememberSaveable { mutableStateOf(false) }
    var showScaleDialog by rememberSaveable { mutableStateOf(false) }
    var pendingScale by remember(interfaceScalePercent) {
        mutableFloatStateOf(interfaceScalePercent.toFloat())
    }
    val layoutDirection = LocalLayoutDirection.current
    SecondaryPageScaffold(
        titleRes = R.string.settings_theme_settings_title,
        onBack = onBack,
    ) { topBarPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = topBarPadding.calculateStartPadding(layoutDirection) + 16.dp,
                top = topBarPadding.calculateTopPadding() + 8.dp,
                end = topBarPadding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = 24.dp,
            ),
        ) {
            item {
            ThemeSettingsSection(R.string.settings_theme_section_appearance) {
                    ThemeSettingsEntry(
                        icon = R.drawable.ic_routine_24,
                        title = R.string.settings_theme_mode_label,
                        summary = R.string.settings_theme_mode_hint,
                        value = themeModeLabel(mode),
                        index = 0,
                        total = if (dynamicColorEnabled) 5 else 6,
                        onClick = { showModeDialog = true },
                    )
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
                    ThemeStaticOptionRow(
                        icon = R.drawable.ic_style_24,
                        title = R.string.settings_theme_palette_style_label,
                        value = paletteStyleLabel(paletteStyle),
                        index = if (dynamicColorEnabled) 2 else 3,
                        total = if (dynamicColorEnabled) 5 else 6,
                        onClick = { showPaletteDialog = true },
                    )
                    ThemeStaticOptionRow(
                        icon = R.drawable.ic_design_services_24,
                        title = R.string.settings_theme_color_spec_label,
                        value = colorSpecificationLabel(paletteStyle, colorSpecification),
                        index = if (dynamicColorEnabled) 3 else 4,
                        total = if (dynamicColorEnabled) 5 else 6,
                        onClick = { showSpecificationDialog = true },
                    )
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
        }
    }
    if (showModeDialog) {
        ThemeModeDialog(
            selectedMode = mode,
            onSelected = {
                onModeSelected(it)
            },
            onDismiss = { showModeDialog = false },
        )
    }
    if (showPaletteDialog) {
        ThemeChoiceDialog(
            title = stringResource(R.string.settings_theme_palette_style_label),
            selected = paletteStyle,
            options = paletteStyleOptions,
            onSelected = onPaletteStyleSelected,
            onDismiss = { showPaletteDialog = false },
        )
    }
    if (showSpecificationDialog) {
        ThemeChoiceDialog(
            title = stringResource(R.string.settings_theme_color_spec_label),
            selected = colorSpecification,
            options = if (DpisColorSchemeFactory.supports2025Specification(paletteStyle)) {
                colorSpecificationOptions
            } else {
                colorSpecificationOptions.take(1)
            },
            onSelected = onColorSpecificationSelected,
            onDismiss = { showSpecificationDialog = false },
        )
    }
    if (showScaleDialog) {
        DpisModalDialog(onDismissRequest = { showScaleDialog = false }) {
            InterfaceScaleDialogContent(
                initialPercent = pendingScale.roundToInt(),
                minimumPercent = AppUiScaleManager.MIN_SCALE_PERCENT,
                maximumPercent = AppUiScaleManager.MAX_SCALE_PERCENT,
                onCancel = { showScaleDialog = false },
                onSave = {
                    showScaleDialog = false
                    pendingScale = it.toFloat()
                    onInterfaceScaleChanged(it)
                },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ThemeDynamicColorRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    total: Int,
) {
    SegmentedListItem(
        onClick = { onCheckedChange(!checked) },
        shapes = dpisSegmentedShapes(index, total),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        leadingContent = {
            Icon(painterResource(R.drawable.ic_palette_24), contentDescription = null)
        },
        content = {
            Text(
                stringResource(R.string.settings_theme_dynamic_color_label),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Text(
                stringResource(R.string.settings_theme_dynamic_color_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ThemeStaticOptionRow(
    icon: Int,
    title: Int,
    value: String,
    index: Int,
    total: Int,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = dpisSegmentedShapes(index, total),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        leadingContent = {
            Icon(painterResource(icon), contentDescription = null)
        },
        content = {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        },
        trailingContent = {
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        },
    )
}

private data class ThemeColorOption(val id: String)

private val themeColorOptions = listOf(
    ThemeColorOption(ThemeModeStore.DEFAULT_STATIC_THEME_COLOR),
    ThemeColorOption(ThemeModeStore.COLOR_PINK),
    ThemeColorOption(ThemeModeStore.COLOR_RED),
    ThemeColorOption(ThemeModeStore.COLOR_ORANGE),
    ThemeColorOption(ThemeModeStore.COLOR_AMBER),
    ThemeColorOption(ThemeModeStore.COLOR_YELLOW),
    ThemeColorOption(ThemeModeStore.COLOR_LIME),
    ThemeColorOption(ThemeModeStore.COLOR_GREEN),
    ThemeColorOption(ThemeModeStore.COLOR_CYAN),
    ThemeColorOption(ThemeModeStore.COLOR_TEAL),
    ThemeColorOption(ThemeModeStore.COLOR_LIGHT_BLUE),
    ThemeColorOption(ThemeModeStore.COLOR_BLUE),
    ThemeColorOption(ThemeModeStore.COLOR_INDIGO),
    ThemeColorOption(ThemeModeStore.COLOR_DEEP_PURPLE),
    ThemeColorOption(ThemeModeStore.COLOR_BLUE_GREY),
    ThemeColorOption(ThemeModeStore.COLOR_BROWN),
    ThemeColorOption(ThemeModeStore.COLOR_GREY),
)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ThemeColorRow(
    selectedColor: String,
    paletteStyle: String,
    colorSpecification: String,
    onColorSelected: (String) -> Unit,
    index: Int,
    total: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = dpisSegmentedShapes(index, total).shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_pie_chart_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.settings_theme_color_label),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(top = 8.dp),
            ) {
                DpisHorizontalScrollWithEdgeFade(
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    themeColorOptions.forEach { option ->
                        GeneratedThemeSwatch(
                            option = option,
                            paletteStyle = paletteStyle,
                            colorSpecification = colorSpecification,
                            selected = selectedColor == option.id,
                            onClick = { onColorSelected(option.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneratedThemeSwatch(
    option: ThemeColorOption,
    paletteStyle: String,
    colorSpecification: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val swatchDividerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val seedColor = DpisColorSchemeFactory.seedColor(option.id)
    val scheme = remember(option, paletteStyle, colorSpecification) {
        DpisColorSchemeFactory.create(
            seedColor = seedColor,
            darkTheme = false,
            paletteStyle = paletteStyle,
            requestedSpecification = colorSpecification,
        )
    }
    Box(
        modifier = Modifier
            .size(52.dp)
            .then(
                if (selected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp),
                ) else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Canvas(Modifier.fillMaxSize().clip(CircleShape)) {
            val gap = 1.dp.toPx()
            val leftWidth = size.width / 2f
            val rightHeight = size.height / 2f
            drawCircle(swatchDividerColor)
            // Keep the selected color visible instead of showing only its lighter
            // generated container tone.
            drawRect(
                seedColor,
                size = size.copy(width = leftWidth - gap / 2f),
            )
            drawRect(
                scheme.secondaryContainer,
                topLeft = androidx.compose.ui.geometry.Offset(leftWidth + gap / 2f, 0f),
                size = size.copy(
                    width = leftWidth - gap / 2f,
                    height = rightHeight - gap / 2f,
                ),
            )
            drawRect(
                scheme.tertiaryContainer,
                topLeft = androidx.compose.ui.geometry.Offset(
                    leftWidth + gap / 2f,
                    rightHeight + gap / 2f,
                ),
                size = size.copy(
                    width = leftWidth - gap / 2f,
                    height = rightHeight - gap / 2f,
                ),
            )
        }
    }
}

private val paletteStyleOptions = listOf(
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

private val colorSpecificationOptions = listOf(
    ThemeModeStore.SPEC_2021 to R.string.settings_theme_color_spec_2021,
    ThemeModeStore.SPEC_2025 to R.string.settings_theme_color_spec_2025,
)

@Composable
private fun paletteStyleLabel(value: String): String = stringResource(
    paletteStyleOptions.firstOrNull { it.first == value }?.second
        ?: R.string.settings_theme_palette_style_tonal_spot,
)

@Composable
private fun colorSpecificationLabel(paletteStyle: String, value: String): String = stringResource(
    colorSpecificationOptions.firstOrNull {
        it.first == if (DpisColorSchemeFactory.supports2025Specification(paletteStyle)) {
            value
        } else {
            ThemeModeStore.SPEC_2021
        }
    }?.second ?: R.string.settings_theme_color_spec_2025,
)

@Composable
private fun ThemeChoiceDialog(
    title: String,
    selected: String,
    options: List<Pair<String, Int>>,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    DpisModalDialog(onDismissRequest = onDismiss) {
        DialogColumn {
            DialogTitle(title)
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(options) { option ->
                    ThemeChoiceRow(
                        label = stringResource(option.second),
                        selected = selected == option.first,
                        onClick = { onSelected(option.first) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dialog_typeface_done_action))
            }
        }
    }
}

@Composable
private fun ThemeModeDialog(
    selectedMode: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        ThemeModeStore.LIGHT to R.string.settings_theme_mode_light,
        ThemeModeStore.DARK to R.string.settings_theme_mode_dark,
        ThemeModeStore.FOLLOW_SYSTEM to
            R.string.settings_theme_mode_follow_system,
    )
    DpisModalDialog(onDismissRequest = onDismiss) {
        DialogColumn {
            DialogTitle(stringResource(R.string.settings_theme_mode_label))
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(options) { (value, label) ->
                    ThemeChoiceRow(
                        label = stringResource(label),
                        selected = selectedMode == value,
                        onClick = { onSelected(value) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dialog_typeface_done_action))
            }
        }
    }
}

@Composable
private fun ThemeChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).clickable(onClick = onClick),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun themeModeLabel(mode: String): String = stringResource(
    when (mode) {
        ThemeModeStore.LIGHT -> R.string.settings_theme_mode_light
        ThemeModeStore.DARK -> R.string.settings_theme_mode_dark
        else -> R.string.settings_theme_mode_follow_system
    },
)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ThemeSettingsSection(
    title: Int,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            content = content,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ThemeSettingsEntry(
    icon: Int,
    title: Int,
    summary: Int,
    value: String,
    index: Int,
    total: Int,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = rememberDpisConfirmAction(onClick),
        shapes = dpisSegmentedShapes(index, total),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            Icon(painterResource(icon), contentDescription = null)
        },
        content = {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = {
            Text(stringResource(summary), style = MaterialTheme.typography.bodyMedium)
        },
        trailingContent = {
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ThemeInterfaceScaleRow(
    pendingScale: Float,
    onPendingScaleChanged: (Float) -> Unit,
    onScaleChanged: (Int) -> Unit,
    onClick: () -> Unit,
    index: Int,
    total: Int,
) {
    val shape = dpisSegmentedShapes(index, total).shape
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_fit_width_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_interface_scale_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                Text(
                    stringResource(R.string.settings_interface_scale_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.settings_interface_scale_value, pendingScale.roundToInt()),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            ThemeScaleSlider(
                value = pendingScale,
                onValueChange = onPendingScaleChanged,
                onValueChangeFinished = { onScaleChanged(it.roundToInt()) },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ThemeScaleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val valueRange = AppUiScaleManager.MIN_SCALE_PERCENT.toFloat()..
        AppUiScaleManager.MAX_SCALE_PERCENT.toFloat()
    val coercedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val interactionSource = remember { MutableInteractionSource() }
    val latestGestureValue = remember { mutableFloatStateOf(coercedValue) }
    val view = LocalView.current
    var lastFeedbackPercent by remember(value) {
        mutableIntStateOf(AppUiScaleManager.normalizeSliderPercent(coercedValue))
    }
    Slider(
        value = coercedValue,
        onValueChange = { changedValue ->
            val normalizedValue = AppUiScaleManager.normalizeSliderPercent(changedValue)
            latestGestureValue.floatValue = normalizedValue.toFloat()
            if (normalizedValue != lastFeedbackPercent) {
                lastFeedbackPercent = normalizedValue
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            onValueChange(normalizedValue.toFloat())
        },
        onValueChangeFinished = { onValueChangeFinished(latestGestureValue.floatValue) },
        valueRange = valueRange,
        steps = 0,
        interactionSource = interactionSource,
        thumb = {
            SliderDefaults.Thumb(interactionSource = interactionSource, isVertical = false)
        },
        track = { sliderState -> SliderDefaults.Track(sliderState = sliderState) },
        modifier = modifier.fillMaxWidth().height(48.dp),
    )
}
