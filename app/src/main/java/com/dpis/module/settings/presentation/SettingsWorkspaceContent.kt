package com.dpis.module.ui.compose

import com.dpis.module.ui.dialog.ModalDialog

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.SettingsUiState
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.settings.AppLocaleManager
import com.dpis.module.settings.TranslationContributorCatalog
import kotlin.math.roundToInt

/** Compose rendering only; all settings workflows execute through Java-owned actions. */
@Composable
fun SettingsWorkspaceContent(
    state: SettingsUiState?,
    padding: PaddingValues,
    onHooksChanged: (Boolean) -> Unit,
    onSafeModeChanged: (Boolean) -> Unit,
    onGlobalLogChanged: (Boolean) -> Unit,
    onOpenLogs: () -> Unit,
    onLauncherHiddenChanged: (Boolean) -> Unit,
    onFontDebug: () -> Unit,
    onFontLibrary: () -> Unit,
    onExperimental: () -> Unit,
    onThemeSettings: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onBackup: () -> Unit,
    onClearCache: () -> Unit,
    onAbout: () -> Unit,
    onDonate: () -> Unit,
    scrollStore: PageScrollPositionStore,
) {
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val systemContext = context.applicationContext
    val languageOptions = AppLocaleManager.supportedLanguages().map {
        val label = if (it.tag == AppLocaleManager.TAG_FOLLOW_SYSTEM) {
            // The activity context is wrapped in the selected app language; the
            // application context keeps the device language for this option.
            systemContext.getString(it.labelResId)
        } else {
            stringResource(it.labelResId)
        }
        LanguageDialogOption(it.tag, label)
    }
    val translationContributors = TranslationContributorCatalog.forLanguage(
        AppLocaleManager.getLanguageTag(context)
    )
    val generalItemCount = if (state?.globalLogEnabled == true) 6 else 5
    val listState = rememberRestorableLazyListState("settings", scrollStore)
    PageScaffold(
        pageBar = PageBarBehavior.Collapsing,
        onBack = null,
        titleRes = R.string.system_settings_title,
        scrollStore = scrollStore,
        scrollKey = "settings",
    ) { pagePadding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(
                start = pagePadding.calculateStartPadding(layoutDirection) + 16.dp,
                top = pagePadding.calculateTopPadding() + SecondaryPageContentTokens.TitleToContentGap,
                end = pagePadding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = pagePadding.calculateBottomPadding() + LocalSpacing.current.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsGroup(R.string.system_settings_section_general) {
                SettingsSwitchRow(
                    R.drawable.ic_android_24,
                    R.string.system_hooks_enabled_label,
                    R.string.system_hooks_enabled_hint,
                    state?.systemHooksEnabled == true,
                    state?.storeAvailable == true,
                    index = 0, total = generalItemCount,
                    onHooksChanged
                )
                SettingsSwitchRow(
                    R.drawable.ic_shield_24,
                    R.string.system_safe_mode_label,
                    R.string.system_safe_mode_hint,
                    state?.safeModeEnabled == true,
                    state?.storeAvailable == true,
                    index = 1, total = generalItemCount,
                    onSafeModeChanged
                )
                SettingsSwitchRow(
                    R.drawable.ic_view_kanban_24,
                    R.string.global_log_enabled_label,
                    R.string.global_log_enabled_hint,
                    state?.globalLogEnabled == true,
                    state?.storeAvailable == true,
                    index = 2, total = generalItemCount,
                    onGlobalLogChanged
                )
                AnimatedConditionalItem(visible = state?.globalLogEnabled == true) {
                    SettingsEntry(
                        R.drawable.ic_overview_24,
                        R.string.tools_log_title,
                        R.string.tools_log_subtitle,
                        state?.storeAvailable == true,
                        index = 3, total = generalItemCount,
                        onOpenLogs
                    )
                }
                SettingsEntry(
                    R.drawable.ic_upload_file_24,
                    R.string.settings_font_library_label,
                    R.string.settings_font_library_hint,
                    state?.storeAvailable == true,
                    index = if (state?.globalLogEnabled == true) 4 else 3,
                    total = generalItemCount,
                    onFontLibrary
                )
                SettingsEntry(
                    R.drawable.ic_experiment_24,
                    R.string.settings_experimental_title,
                    R.string.settings_experimental_hint,
                    state?.storeAvailable == true,
                    index = if (state?.globalLogEnabled == true) 5 else 4,
                    total = generalItemCount,
                    onExperimental
                )
            }
        }
        item {
            SettingsGroup(R.string.settings_section_theme) {
                val themeItemCount = 2 + translationContributors.size
                SettingsEntry(
                    R.drawable.ic_format_paint_24,
                    R.string.settings_theme_settings_title,
                    R.string.settings_theme_settings_hint,
                    enabled = true,
                    index = 0, total = themeItemCount,
                    onThemeSettings
                )
                SettingsEntry(
                    R.drawable.ic_language_24,
                    R.string.settings_language_label,
                    state?.languageLabel ?: stringResource(R.string.settings_language_follow_system),
                    enabled = state?.storeAvailable == true,
                    index = 1, total = themeItemCount,
                    { showLanguageDialog = true }
                )
                translationContributors.forEachIndexed { index, contributor ->
                    SettingsEntry(
                        contributor.iconRes,
                        contributor.labelRes,
                        contributor.nameRes,
                        enabled = true,
                        index = index + 2, total = themeItemCount,
                        onClick = {},
                        showTrailingIcon = false,
                    )
                }
            }
        }
        item {
            SettingsGroup(R.string.settings_section_other) {
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
            SettingsGroup(R.string.settings_section_about) {
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
    if (showLanguageDialog) {
        ModalDialog(onDismissRequest = { showLanguageDialog = false }) {
            LanguageDialogContent(
                options = languageOptions,
                selectedTag = AppLocaleManager.getLanguageTag(context),
                onDone = { showLanguageDialog = false },
                onSelected = { selectedTag ->
                    // Dismiss before the locale change recreates the host activity.
                    showLanguageDialog = false
                    onLanguageSelected(selectedTag)
                },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SettingsGroup(title: Int, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.foundation.layout.Column {
        PageSectionLabel(
            stringResource(title),
            modifier = Modifier.padding(
                start = SecondaryPageContentTokens.SectionLabelHorizontalInset,
                top = 8.dp,
            ),
        )
        Spacer(Modifier.height(SecondaryPageContentTokens.SectionLabelToFirstItemGap))
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
    val hapticChanged = rememberClickValueAction(onChanged)
    SegmentedListItem(
        onClick = { hapticChanged(!checked) },
        enabled = enabled,
        shapes = dpisSegmentedShapes(index, total),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            // Disabled settings remain on the same surface; only their content
            // should receive Material's disabled emphasis.
            disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
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
private fun SettingsEntry(
    @androidx.annotation.DrawableRes iconRes: Int,
    title: Int,
    summary: String,
    enabled: Boolean,
    index: Int,
    total: Int,
    onClick: () -> Unit,
    showTrailingIcon: Boolean = true,
) {
    val hapticClick = rememberClickAction(onClick)
    SegmentedListItem(
        onClick = hapticClick,
        enabled = enabled,
        shapes = dpisSegmentedShapes(index, total),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            // Keep unavailable actions from falling back to the darker default
            // disabled container while preserving disabled content treatment.
            disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
            contentColor = MaterialTheme.colorScheme.onSurface,
            leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { Icon(painterResource(iconRes), contentDescription = null) },
        content = { Text(stringResource(title)) },
        supportingContent = { Text(summary) },
        trailingContent = if (showTrailingIcon) {
            {
                Icon(
                    painterResource(R.drawable.ic_chevron_right_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null
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
    onClick: () -> Unit,
    showTrailingIcon: Boolean = true,
) = SettingsEntry(
    iconRes,
    title,
    stringResource(summary),
    enabled,
    index,
    total,
    onClick,
    showTrailingIcon,
)
