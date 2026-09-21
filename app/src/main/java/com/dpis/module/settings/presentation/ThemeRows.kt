package com.dpis.module.settings.presentation

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.settings.PageSettingsStore
import com.dpis.module.settings.ThemeModeStore
import com.dpis.module.ui.dialog.DialogColumn
import com.dpis.module.ui.dialog.DialogDoneButton
import com.dpis.module.ui.dialog.ModalDialog
import com.dpis.module.ui.presentation.design.ColorSchemeFactory
import com.dpis.module.ui.presentation.design.ReorderableDragFeedback
import com.dpis.module.ui.presentation.design.ThemeSwatchPreviewCache
import com.dpis.module.ui.presentation.design.dpisClickable
import com.dpis.module.ui.presentation.design.dpisLongPress
import com.dpis.module.ui.presentation.design.rememberClickAction
import com.dpis.module.ui.presentation.design.rememberClickValueAction
import com.dpis.module.ui.presentation.design.rememberLongPressAction
import com.dpis.module.ui.presentation.editor.DpisSwitch
import com.dpis.module.ui.presentation.editor.HorizontalScrollWithEdgeFade
import com.dpis.module.ui.presentation.workspace.PageChromeTokens
import com.dpis.module.ui.presentation.workspace.PageSectionLabel
import com.dpis.module.ui.presentation.workspace.dpisSegmentedShapes
import com.dpis.module.ui.presentation.workspace.rememberSegmentedPressedShape
import com.dpis.module.ui.presentation.workspace.segmentedRowColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt

internal val themeModeOptions = listOf(
    ThemeModeStore.LIGHT to R.string.settings_theme_mode_light,
    ThemeModeStore.DARK to R.string.settings_theme_mode_dark,
    ThemeModeStore.FOLLOW_SYSTEM to R.string.settings_theme_mode_follow_system,
)

internal val paletteStyleOptions = listOf(
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

internal val colorSpecificationOptions = listOf(
    ThemeModeStore.SPEC_2021 to R.string.settings_theme_color_spec_2021,
    ThemeModeStore.SPEC_2025 to R.string.settings_theme_color_spec_2025,
)

private val startupPageOptions = listOf(
    "APP" to R.string.workspace_app,
    "TEMPLATE" to R.string.workspace_template,
    PageSettingsStore.HOME to R.string.workspace_home,
    "TOOLS" to R.string.workspace_tools,
    "SETTINGS" to R.string.workspace_settings,
)

internal fun startupPageLabel(value: String): Int =
    startupPageOptions.firstOrNull { it.first == value }?.second
        ?: R.string.workspace_home

@Composable
internal fun themeModeLabel(mode: String): String = stringResource(
    when (mode) {
        ThemeModeStore.LIGHT -> R.string.settings_theme_mode_light
        ThemeModeStore.DARK -> R.string.settings_theme_mode_dark
        else -> R.string.settings_theme_mode_follow_system
    },
)

@Composable
internal fun paletteStyleLabel(value: String): String = stringResource(
    paletteStyleOptions.firstOrNull { it.first == value }?.second
        ?: R.string.settings_theme_palette_style_tonal_spot,
)

@Composable
internal fun colorSpecificationLabel(paletteStyle: String, value: String): String = stringResource(
    colorSpecificationOptions.firstOrNull {
        it.first == if (ColorSchemeFactory.supports2025Specification(paletteStyle)) {
            value
        } else {
            ThemeModeStore.SPEC_2021
        }
    }?.second ?: R.string.settings_theme_color_spec_2025,
)

@Composable
internal fun ThemeChoiceMenuAnchor(
    expanded: Boolean,
    options: List<Pair<String, Int>>,
    selected: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
    anchor: @Composable () -> Unit,
) {
    SettingsChoiceMenu(
        expanded = expanded,
        options = options.map { (value, labelRes) ->
            SettingsChoiceOption(value, stringResource(labelRes))
        },
        selected = selected,
        onDismiss = onDismiss,
        onSelected = onSelected,
        content = anchor,
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun ThemeSettingsSection(
    title: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = PageChromeTokens.SectionBlockGap)) {
        PageSectionLabel(stringResource(title))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            content = content,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun ThemeSettingsEntry(
    icon: Int,
    title: Int,
    summary: Int,
    value: String,
    index: Int,
    total: Int,
    onClick: () -> Unit,
) {
    ThemeSegmentedSurfaceRow(
        onClick = onClick,
        index = index,
        total = total,
        leadingContent = {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

/** Theme rows use Material's segmented item so edge shapes animate with press interactions. */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun ThemeSegmentedSurfaceRow(
    onClick: () -> Unit,
    index: Int,
    total: Int,
    leadingContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    SegmentedListItem(
        modifier = Modifier.fillMaxWidth(),
        onClick = rememberClickAction(onClick),
        shapes = dpisSegmentedShapes(index, total),
        colors = segmentedRowColors(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = leadingContent,
        content = content,
        supportingContent = supportingContent,
        trailingContent = trailingContent,
    )
}

@Composable
internal fun PageNavigationDialog(
    selectedStartupPage: String,
    onStartupPageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var hidden by remember { mutableStateOf(PageSettingsStore.getHiddenWorkspaces(context)) }
    val order = remember {
        mutableStateListOf(*PageSettingsStore.getWorkspaceOrder(context).toTypedArray())
    }
    ModalDialog(onDismissRequest = onDismiss) {
        DialogColumn(
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.settings_page_default_startup),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.settings_page_startup_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            },
            actions = {
                DialogDoneButton(onClick = onDismiss)
            },
        ) {
            val lazyListState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                val fromIndex = order.indexOf(from.key)
                val toIndex = order.indexOf(to.key)
                if (fromIndex >= 0 && toIndex >= 0) {
                    val moved = order.removeAt(fromIndex)
                    order.add(toIndex, moved)
                    PageSettingsStore.setWorkspaceOrder(context, order)
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(order, key = { it }) { page ->
                    val label = startupPageLabel(page)
                    val isHidden = page in hidden
                    val isSettings = page == "SETTINGS"
                    val selectStartupPage = rememberLongPressAction { onStartupPageSelected(page) }
                    ReorderableItem(reorderableState, key = page) { isDragging ->
                        ReorderableDragFeedback(isDragging)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .alpha(if (isHidden) 0.48f else 1f)
                                .dpisClickable(onClick = {
                                    if (isSettings) {
                                        Toast.makeText(
                                            context,
                                            R.string.settings_page_settings_cannot_hide,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } else {
                                        PageSettingsStore.setWorkspaceVisible(
                                            context,
                                            page,
                                            isHidden
                                        )
                                        hidden = PageSettingsStore.getHiddenWorkspaces(context)
                                    }
                                })
                                .background(MaterialTheme.colorScheme.surfaceBright)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_drag_indicator_24),
                                stringResource(R.string.quick_template_sort_drag_handle),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.longPressDraggableHandle(),
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .dpisLongPress(onLongPress = selectStartupPage),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        stringResource(label),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    if (selectedStartupPage == page) {
                                        Text(
                                            stringResource(R.string.settings_page_startup_badge),
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun ThemePredictiveBackRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    total: Int,
) {
    val changePredictiveBack = rememberClickValueAction(onCheckedChange)
    ThemeSegmentedSurfaceRow(
        onClick = { changePredictiveBack(!checked) },
        index = index,
        total = total,
        leadingContent = {
            Icon(
                painterResource(R.drawable.ic_transition_push_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        content = {
            Text(
                stringResource(R.string.settings_page_predictive_back),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Text(
                stringResource(R.string.settings_page_predictive_back_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        trailingContent = {
            DpisSwitch(checked = checked, onCheckedChange = changePredictiveBack)
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun ThemeDynamicColorRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    total: Int,
) {
    val changeDynamicColor = rememberClickValueAction(onCheckedChange)
    ThemeSegmentedSurfaceRow(
        onClick = { changeDynamicColor(!checked) },
        index = index,
        total = total,
        leadingContent = {
            Icon(
                painterResource(R.drawable.ic_palette_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            DpisSwitch(checked = checked, onCheckedChange = changeDynamicColor)
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun ThemeStaticOptionRow(
    icon: Int,
    title: Int,
    value: String,
    index: Int,
    total: Int,
    onClick: () -> Unit,
) {
    ThemeSegmentedSurfaceRow(
        onClick = onClick,
        index = index,
        total = total,
        leadingContent = {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
internal fun ThemeColorRow(
    selectedColor: String,
    paletteStyle: String,
    colorSpecification: String,
    onColorSelected: (String) -> Unit,
    index: Int,
    total: Int,
) {
    var previewRevision by remember { mutableIntStateOf(0) }
    LaunchedEffect(paletteStyle, colorSpecification) {
        withContext(Dispatchers.Default) {
            ThemeSwatchPreviewCache.prefetch(paletteStyle, colorSpecification)
        }
        previewRevision++
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = dpisSegmentedShapes(index, total).shape,
        color = MaterialTheme.colorScheme.surfaceBright,
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
                HorizontalScrollWithEdgeFade(
                    owningSurfaceColor = MaterialTheme.colorScheme.surfaceBright,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    themeColorOptions.forEach { option ->
                        GeneratedThemeSwatch(
                            option = option,
                            paletteStyle = paletteStyle,
                            colorSpecification = colorSpecification,
                            previewRevision = previewRevision,
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
    previewRevision: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val swatchDividerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val seedColor = ColorSchemeFactory.seedColor(option.id)
    val scheme = remember(option.id, paletteStyle, colorSpecification, previewRevision) {
        ThemeSwatchPreviewCache.peek(option.id, paletteStyle, colorSpecification)
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
            .dpisClickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Canvas(Modifier
            .fillMaxSize()
            .clip(CircleShape)) {
            val gap = 1.dp.toPx()
            val leftWidth = size.width / 2f
            val rightHeight = size.height / 2f
            val secondary = scheme?.secondaryContainer ?: seedColor
            val tertiary = scheme?.tertiaryContainer ?: seedColor
            drawCircle(swatchDividerColor)
            // Keep the selected color visible instead of showing only its lighter
            // generated container tone.
            drawRect(
                seedColor,
                size = size.copy(width = leftWidth - gap / 2f),
            )
            drawRect(
                secondary,
                topLeft = Offset(leftWidth + gap / 2f, 0f),
                size = size.copy(
                    width = leftWidth - gap / 2f,
                    height = rightHeight - gap / 2f,
                ),
            )
            drawRect(
                tertiary,
                topLeft = Offset(
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

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun ThemeInterfaceScaleRow(
    pendingScale: Float,
    onPendingScaleChanged: (Float) -> Unit,
    onScaleChanged: (Int) -> Unit,
    onClick: () -> Unit,
    index: Int,
    total: Int,
) {
    val cardInteractionSource = remember { MutableInteractionSource() }
    val shape = rememberSegmentedPressedShape(
        shapes = dpisSegmentedShapes(index, total),
        interactionSource = cardInteractionSource,
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .indication(cardInteractionSource, ripple()),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceBright,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // The slider owns its drag/tap feedback. Keeping the dialog action on the header
            // avoids a slider gesture also invoking the card's confirmation click.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .dpisClickable(
                        onClick = onClick,
                        interactionSource = cardInteractionSource,
                        indication = null,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_aspect_ratio_24),
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
                    stringResource(
                        R.string.settings_interface_scale_value,
                        pendingScale.roundToInt()
                    ),
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
    val sliderState = rememberSliderState(
        value = coercedValue,
        steps = 0,
        trackRange = valueRange,
    )
    sliderState.value = coercedValue
    val interactionSource = remember { MutableInteractionSource() }
    val latestGestureValue = remember { mutableFloatStateOf(coercedValue) }
    val view = LocalView.current
    var lastFeedbackPercent by remember(value) {
        mutableIntStateOf(AppUiScaleManager.normalizeSliderPercent(coercedValue))
    }
    Slider(
        state = sliderState,
        onValueChange = { changedValue ->
            val normalizedValue = AppUiScaleManager.normalizeSliderPercent(changedValue)
            val normalizedFloat = normalizedValue.toFloat()
            sliderState.value = normalizedFloat
            latestGestureValue.floatValue = normalizedFloat
            if (normalizedValue != lastFeedbackPercent) {
                lastFeedbackPercent = normalizedValue
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            onValueChange(normalizedFloat)
        },
        onValueChangeFinished = { onValueChangeFinished(latestGestureValue.floatValue) },
        interactionSource = interactionSource,
        thumb = {
            SliderDefaults.Thumb(interactionSource = interactionSource, isVertical = false)
        },
        track = { currentSliderState ->
            SliderDefaults.Track(sliderState = currentSliderState)
        },
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    )
}
