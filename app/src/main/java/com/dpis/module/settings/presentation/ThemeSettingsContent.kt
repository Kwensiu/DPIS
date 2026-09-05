package com.dpis.module.ui.compose

import com.dpis.module.ui.dialog.ModalDialog
import com.dpis.module.ui.dialog.DialogColumn
import com.dpis.module.ui.dialog.DialogDoneButton

import android.widget.Toast
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ripple
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.dpis.module.R
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.settings.ThemeModeStore
import com.dpis.module.settings.PageSettingsStore
import kotlin.math.roundToInt
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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
    onModeSelected: (String) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onThemeColorSelected: (String) -> Unit,
    onPaletteStyleSelected: (String) -> Unit,
    onColorSpecificationSelected: (String) -> Unit,
    onInterfaceScaleChanged: (Int) -> Unit,
    onShowHomeEditButtonChanged: (Boolean) -> Unit = {},
    onDefaultStartupPageSelected: (String) -> Unit = {},
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
    val layoutDirection = LocalLayoutDirection.current
    SecondaryPageScaffold(
        titleRes = R.string.settings_theme_settings_title,
        onBack = onBack,
    ) { topBarPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = topBarPadding.calculateStartPadding(layoutDirection) + 16.dp,
                top = topBarPadding.calculateTopPadding() + SecondaryPageContentTokens.TitleToContentGap,
                end = topBarPadding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = 24.dp,
            ),
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
                    ThemeSegmentedSurfaceRow(
                        onClick = { onShowHomeEditButtonChanged(!showHomeEditButton) },
                        index = 0, total = 2,
                        leadingContent = { Icon(painterResource(R.drawable.ic_edit_24), null) },
                        content = { Text(stringResource(R.string.settings_page_home_edit_button), style = MaterialTheme.typography.titleMedium) },
                        trailingContent = { Switch(checked = showHomeEditButton, onCheckedChange = onShowHomeEditButtonChanged) },
                    )
                    ThemeSegmentedSurfaceRow(
                        onClick = { showStartupPageDialog = true },
                        index = 1, total = 2,
                        leadingContent = { Icon(painterResource(R.drawable.ic_home_24), null) },
                        content = { Text(stringResource(R.string.settings_page_default_startup), style = MaterialTheme.typography.titleMedium) },
                        supportingContent = { Text(stringResource(R.string.settings_page_default_startup_hint), style = MaterialTheme.typography.bodyMedium) },
                        trailingContent = { Text(stringResource(startupPageLabel(defaultStartupPage)), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) },
                    )
                }
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

private val startupPageOptions = listOf(
    "APP" to R.string.workspace_app,
    "TEMPLATE" to R.string.workspace_template,
    PageSettingsStore.HOME to R.string.workspace_home,
    "TOOLS" to R.string.workspace_tools,
    "SETTINGS" to R.string.workspace_settings,
)

private fun startupPageLabel(value: String): Int = startupPageOptions.firstOrNull { it.first == value }?.second
    ?: R.string.workspace_home

@Composable
private fun PageNavigationDialog(
    selectedStartupPage: String,
    onStartupPageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var hidden by remember { mutableStateOf(PageSettingsStore.getHiddenWorkspaces(context)) }
    val order = remember { mutableStateListOf(*PageSettingsStore.getWorkspaceOrder(context).toTypedArray()) }
    ModalDialog(onDismissRequest = onDismiss) {
        DialogColumn(title = { Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.settings_page_default_startup), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(stringResource(R.string.settings_page_startup_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        } }, actions = {
            DialogDoneButton(onClick = onDismiss)
        }) {
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
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp), state = lazyListState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                        Toast.makeText(context, R.string.settings_page_settings_cannot_hide, Toast.LENGTH_SHORT).show()
                                    } else {
                                        PageSettingsStore.setWorkspaceVisible(context, page, isHidden)
                                        hidden = PageSettingsStore.getHiddenWorkspaces(context)
                                    }
                                })
                            .background(MaterialTheme.colorScheme.surfaceBright)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(R.drawable.ic_drag_indicator_24), stringResource(R.string.quick_template_sort_drag_handle), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.longPressDraggableHandle())
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
private fun ThemeDynamicColorRow(
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
            Switch(checked = checked, onCheckedChange = changeDynamicColor)
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
                    edgeColor = MaterialTheme.colorScheme.surfaceBright,
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
    val seedColor = ColorSchemeFactory.seedColor(option.id)
    val scheme = remember(option, paletteStyle, colorSpecification) {
        ColorSchemeFactory.create(
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
            .dpisClickable(onClick = onClick)
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
        it.first == if (ColorSchemeFactory.supports2025Specification(paletteStyle)) {
            value
        } else {
            ThemeModeStore.SPEC_2021
        }
    }?.second ?: R.string.settings_theme_color_spec_2025,
)

@Composable
private fun ThemeChoiceMenuAnchor(
    expanded: Boolean,
    options: List<Pair<String, Int>>,
    selected: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
    anchor: @Composable () -> Unit,
) {
    val selectOption = rememberClickValueAction(onSelected)
    Box(Modifier.fillMaxWidth()) {
        anchor()
        // Anchor below the setting's trailing edge. Starting at the row's top edge lets the
        // opening tap land on the first item after the popup is composed.
        Box(Modifier.align(Alignment.BottomEnd)) {
            DropdownMenuPopup(
                expanded = expanded,
                onDismissRequest = onDismiss,
                popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                    MenuAnchorPosition.Below,
                ),
                modifier = Modifier
                    // Let the widest option define the menu. A fixed minimum made short
                    // choices look detached from their setting on compact displays.
                    .widthIn(max = 360.dp)
                    .heightIn(max = 360.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .heightIn(max = 360.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = MenuDefaults.TonalElevation,
                    shadowElevation = MenuDefaults.ShadowElevation,
                ) {
                    // Keep the vertical inset inside the scroll container. The stock menu
                    // applies it outside the scroller, producing an extra dark band and a
                    // second clipping edge before items reach the rounded menu boundary.
                    Column(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                    ) {
                        options.forEach { (value, labelRes) ->
                            val isSelected = selected == value
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(labelRes),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                },
                                onClick = { selectOption(value) },
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(MaterialTheme.shapes.large)
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            androidx.compose.ui.graphics.Color.Transparent
                                        },
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val themeModeOptions = listOf(
    ThemeModeStore.LIGHT to R.string.settings_theme_mode_light,
    ThemeModeStore.DARK to R.string.settings_theme_mode_dark,
    ThemeModeStore.FOLLOW_SYSTEM to R.string.settings_theme_mode_follow_system,
)

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
    Column(modifier = Modifier.padding(bottom = SecondaryPageContentTokens.SectionLabelToFirstItemGap * 2)) {
        PageSectionLabel(
            stringResource(title),
            modifier = Modifier.padding(
                start = SecondaryPageContentTokens.SectionLabelHorizontalInset,
            ),
        )
        Spacer(Modifier.height(SecondaryPageContentTokens.SectionLabelToFirstItemGap))
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
private fun ThemeSegmentedSurfaceRow(
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
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            contentColor = MaterialTheme.colorScheme.onSurface,
            leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            trailingContentColor = MaterialTheme.colorScheme.primary,
        ),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = leadingContent,
        content = content,
        supportingContent = supportingContent,
        trailingContent = trailingContent,
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
                modifier = Modifier.fillMaxWidth().dpisClickable(
                    onClick = onClick,
                    interactionSource = cardInteractionSource,
                    indication = null,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
