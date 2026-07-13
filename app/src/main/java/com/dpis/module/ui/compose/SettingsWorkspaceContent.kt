package com.dpis.module.ui.compose

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.SettingsUiState
import com.dpis.module.settings.AppUiScaleManager
import kotlin.math.roundToInt

/** Compose rendering only; all settings workflows execute through Java-owned actions. */
@Composable
fun SettingsWorkspaceContent(
    state: SettingsUiState?,
    padding: PaddingValues,
    onHooksChanged: (Boolean) -> Unit,
    onSafeModeChanged: (Boolean) -> Unit,
    onGlobalLogChanged: (Boolean) -> Unit,
    onLauncherHiddenChanged: (Boolean) -> Unit,
    onInterfaceScaleChanged: (Int) -> Unit,
    onInterfaceScaleDetails: () -> Unit,
    onFontDebug: () -> Unit,
    onFontLibrary: () -> Unit,
    onExperimental: () -> Unit,
    onLanguage: () -> Unit,
    onBackup: () -> Unit,
    onClearCache: () -> Unit,
    onAbout: () -> Unit,
    onDonate: () -> Unit
) {
    var pendingScale by remember(state?.interfaceScalePercent) {
        mutableFloatStateOf((state?.interfaceScalePercent ?: 100).toFloat())
    }
    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.system_settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { topBarPadding ->
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = topBarPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSection(R.string.system_settings_section_general) {
                SettingsSwitchRow(
                    R.drawable.ic_android_24,
                    R.string.system_hooks_enabled_label,
                    R.string.system_hooks_enabled_hint,
                    state?.systemHooksEnabled == true,
                    state?.storeAvailable == true,
                    index = 0, total = 6,
                    onHooksChanged
                )
                SettingsSwitchRow(
                    R.drawable.ic_shield_24,
                    R.string.system_safe_mode_label,
                    R.string.system_safe_mode_hint,
                    state?.safeModeEnabled == true,
                    state?.storeAvailable == true,
                    index = 1, total = 6,
                    onSafeModeChanged
                )
                SettingsSwitchRow(
                    R.drawable.ic_view_kanban_24,
                    R.string.global_log_enabled_label,
                    R.string.global_log_enabled_hint,
                    state?.globalLogEnabled == true,
                    state?.storeAvailable == true,
                    index = 2, total = 6,
                    onGlobalLogChanged
                )
                SettingsEntry(
                    R.drawable.ic_bug_report_24,
                    R.string.font_debug_overlay_label,
                    R.string.font_debug_entry_hint,
                    state?.storeAvailable == true,
                    index = 3, total = 6,
                    onFontDebug
                )
                SettingsEntry(
                    R.drawable.ic_upload_file_24,
                    R.string.settings_font_library_label,
                    R.string.settings_font_library_hint,
                    state?.storeAvailable == true,
                    index = 4, total = 6,
                    onFontLibrary
                )
                SettingsEntry(
                    R.drawable.ic_experiment_24,
                    R.string.settings_experimental_title,
                    R.string.settings_experimental_hint,
                    state?.storeAvailable == true,
                    index = 5, total = 6,
                    onExperimental
                )
            }
        }
        item {
            SettingsSection(R.string.settings_section_theme) {
                SettingsEntry(
                    R.drawable.ic_language_24,
                    R.string.settings_language_label,
                    state?.languageLabel ?: stringResource(R.string.settings_language_follow_system),
                    enabled = state?.storeAvailable == true,
                    index = 0, total = 2,
                    onLanguage
                )
                SettingsScaleRow(
                    R.drawable.ic_fit_width_24,
                    pendingScale = pendingScale,
                    enabled = state?.storeAvailable == true,
                    index = 1, total = 2,
                    onPendingScaleChanged = { pendingScale = it },
                    onScaleChanged = onInterfaceScaleChanged,
                    onDetails = onInterfaceScaleDetails
                )
            }
        }
        item {
            SettingsSection(R.string.settings_section_other) {
                SettingsEntry(
                    R.drawable.ic_upload_file_24,
                    R.string.settings_config_backup_label,
                    R.string.settings_config_backup_hint,
                    state?.storeAvailable == true,
                    index = 0, total = 3,
                    onBackup
                )
                SettingsEntry(
                    R.drawable.ic_mop_24,
                    R.string.settings_clear_cache_label,
                    state?.cacheUsage ?: stringResource(R.string.settings_clear_cache_size, "0 B"),
                    enabled = state?.storeAvailable == true && state.cacheClearInProgress != true,
                    index = 1, total = 3,
                    onClearCache
                )
                SettingsSwitchRow(
                    R.drawable.ic_hide_image_24,
                    R.string.settings_hide_launcher_icon_label,
                    R.string.settings_hide_launcher_icon_hint,
                    state?.launcherIconHidden == true,
                    state?.storeAvailable == true,
                    index = 2, total = 3,
                    onLauncherHiddenChanged
                )
            }
        }
        item {
            SettingsSection(R.string.settings_section_about) {
                SettingsEntry(
                    R.drawable.ic_info_24,
                    R.string.settings_about_label,
                    R.string.settings_about_hint,
                    enabled = true,
                    index = 0, total = 2,
                    onAbout
                )
                SettingsEntry(
                    R.drawable.ic_volunteer_24,
                    R.string.settings_donate_label,
                    R.string.settings_donate_hint,
                    enabled = true,
                    index = 1, total = 2,
                    onDonate
                )
            }
        }
    }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SettingsSection(title: Int, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.foundation.layout.Column {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp)
        )
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            content = content
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SettingsSwitchRow(
    @androidx.annotation.DrawableRes iconRes: Int,
    title: Int,
    summary: Int,
    checked: Boolean,
    enabled: Boolean,
    index: Int,
    total: Int,
    onChanged: (Boolean) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val hapticChanged = { value: Boolean ->
        hapticFeedback.performDpisConfirm()
        onChanged(value)
    }
    SegmentedListItem(
        onClick = { hapticChanged(!checked) },
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(index, total),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { Icon(painterResource(iconRes), contentDescription = null) },
        content = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(summary)) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = hapticChanged,
                enabled = enabled
            )
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SettingsScaleRow(
    @androidx.annotation.DrawableRes iconRes: Int,
    pendingScale: Float,
    enabled: Boolean,
    index: Int,
    total: Int,
    onPendingScaleChanged: (Float) -> Unit,
    onScaleChanged: (Int) -> Unit,
    onDetails: () -> Unit
) {
    val hapticDetails = rememberDpisConfirmAction(onDetails)
    // A SegmentedListItem reserves the trailing slot across every content line. The scale
    // control must instead span the full card width, as a separate control beneath its label.
    // Surface owns the entire-card ripple and clips it to the segmented card shape.
    Surface(
        onClick = hapticDetails,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        shape = ListItemDefaults.segmentedShapes(index, total).shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_interface_scale_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        stringResource(R.string.settings_interface_scale_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    stringResource(R.string.settings_interface_scale_value, pendingScale.roundToInt()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            CenteredScaleSlider(
                value = pendingScale,
                onValueChange = onPendingScaleChanged,
                onValueChangeFinished = { onScaleChanged(it.roundToInt()) },
                enabled = enabled,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun CenteredScaleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
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
            // Store the event value before publishing UI state, avoiding a stale Compose
            // snapshot when a track tap changes and completes the gesture in one frame.
            latestGestureValue.floatValue = normalizedValue.toFloat()
            if (normalizedValue != lastFeedbackPercent) {
                lastFeedbackPercent = normalizedValue
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            onValueChange(normalizedValue.toFloat())
        },
        onValueChangeFinished = { onValueChangeFinished(latestGestureValue.floatValue) },
        valueRange = valueRange,
        // A continuous slider keeps Material 3's two endpoint stop indicators while
        // suppressing the intermediate tick marks that read as a dotted track.
        steps = 0,
        enabled = enabled,
        interactionSource = interactionSource,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                isVertical = false
            )
        },
        track = { sliderState -> SliderDefaults.Track(sliderState = sliderState) },
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SettingsEntry(
    @androidx.annotation.DrawableRes iconRes: Int,
    title: Int,
    summary: String,
    enabled: Boolean,
    index: Int,
    total: Int,
    onClick: () -> Unit
) {
    val hapticClick = rememberDpisConfirmAction(onClick)
    SegmentedListItem(
        onClick = hapticClick,
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(index, total),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { Icon(painterResource(iconRes), contentDescription = null) },
        content = { Text(stringResource(title)) },
        supportingContent = { Text(summary) },
        trailingContent = {
            Icon(
                painterResource(R.drawable.ic_chevron_right_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun SettingsEntry(
    @androidx.annotation.DrawableRes iconRes: Int,
    title: Int,
    summary: Int,
    enabled: Boolean,
    index: Int,
    total: Int,
    onClick: () -> Unit
) = SettingsEntry(iconRes, title, stringResource(summary), enabled, index, total, onClick)
