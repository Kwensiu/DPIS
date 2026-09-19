package com.dpis.module.applist.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dpis.module.BuildConfig
import com.dpis.module.R
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.ui.presentation.design.FilterSheetResetButton
import com.dpis.module.ui.presentation.design.FilterSheetScaffold
import com.dpis.module.ui.presentation.design.FilterSheetScrollChipRow
import com.dpis.module.ui.presentation.design.FilterSheetUiTokens
import com.dpis.module.ui.presentation.editor.FeedbackFilterChip

/** App catalogue filters. Visual grouping mirrors the template target picker, state remains local. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppFilterSheet(
    filterState: AppListFilterState,
    onFilterChanged: (AppListFilterState) -> Unit,
    onDismissRequest: () -> Unit,
) {
    FilterSheetScaffold(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.app_filter_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = FilterSheetUiTokens.HeaderTitleOffset),
            )
        },
        trailingContent = {
            FeedbackFilterChip(shape = FilterSheetUiTokens.PillShape, selected = filterState.reverseOrder(), onClick = { onFilterChanged(filterState.withReverseOrder(!filterState.reverseOrder())) }, label = { Text(stringResource(R.string.app_filter_reverse)) })
            Spacer(Modifier.width(FilterSheetUiTokens.HeaderActionSpacing))
            FilterSheetResetButton(
                onClick = { onFilterChanged(AppListFilterState.defaultState()) },
                contentDescription = stringResource(R.string.app_filter_reset),
            )
        },
    ) {
        AppFilterLabel(R.string.app_filter_type)
            ChipRow {
                FeedbackFilterChip(
                    selected = filterState.allAppsSelected(),
                    onClick = { onFilterChanged(filterState.selectAllAppTypes()) },
                    label = { Text(stringResource(R.string.app_filter_all)) })
                FeedbackFilterChip(
                    selected = !filterState.allAppsSelected() && filterState.systemAppsSelected(),
                    onClick = { onFilterChanged(filterState.toggleAppType(AppListFilterState.AppType.SYSTEM)) },
                    label = { Text(stringResource(R.string.app_filter_system)) },
                    leadingIcon = if (!filterState.allAppsSelected() && filterState.systemAppsSelected()) {
                        { SelectedChipIcon() }
                    } else null)
                FeedbackFilterChip(
                    selected = !filterState.allAppsSelected() && filterState.userAppsSelected(),
                    onClick = { onFilterChanged(filterState.toggleAppType(AppListFilterState.AppType.USER)) },
                    label = { Text(stringResource(R.string.app_filter_user)) },
                    leadingIcon = if (!filterState.allAppsSelected() && filterState.userAppsSelected()) {
                        { SelectedChipIcon() }
                    } else null)
            }
            AppFilterLabel(R.string.app_filter_configuration)
            ConfigurationChipRow {
                FeedbackFilterChip(
                    selected = filterState.allConfigurationSelected(),
                    onClick = { onFilterChanged(filterState.clearConfigurationFilters()) },
                    label = { Text(stringResource(R.string.app_filter_all)) })
                if (BuildConfig.FLAVOR != "legacy") {
                    FeedbackFilterChip(
                        selected = filterState.injectedOnly(),
                        onClick = {
                            toggleConfiguration(
                                filterState,
                                AppListFilterState.ConfigurationFilter.INJECTED,
                                onFilterChanged
                            )
                        },
                        label = { Text(stringResource(R.string.app_filter_scoped)) },
                        leadingIcon = if (filterState.injectedOnly()) {
                            { SelectedChipIcon() }
                        } else null)
                }
                FeedbackFilterChip(
                    selected = filterState.disabledOnly(),
                    onClick = {
                        toggleConfiguration(
                            filterState,
                            AppListFilterState.ConfigurationFilter.DISABLED,
                            onFilterChanged
                        )
                    },
                    label = { Text(stringResource(R.string.app_filter_disabled)) },
                    leadingIcon = if (filterState.disabledOnly()) {
                        { SelectedChipIcon() }
                    } else null)
            }
        ConfigurationChipRow {
            FeedbackFilterChip(
                selected = filterState.widthConfiguredOnly(),
                onClick = {
                    toggleConfiguration(
                        filterState,
                        AppListFilterState.ConfigurationFilter.VIEWPORT,
                        onFilterChanged
                    )
                },
                label = { Text(stringResource(R.string.app_filter_viewport)) },
                leadingIcon = if (filterState.widthConfiguredOnly()) {
                    { SelectedChipIcon() }
                } else null); FeedbackFilterChip(
            selected = filterState.fontConfiguredOnly(),
            onClick = {
                toggleConfiguration(
                    filterState,
                    AppListFilterState.ConfigurationFilter.FONT,
                    onFilterChanged
                )
            },
            label = { Text(stringResource(R.string.app_filter_font_scale)) },
            leadingIcon = if (filterState.fontConfiguredOnly()) {
                { SelectedChipIcon() }
            } else null)
        }
        ConfigurationChipRow {
            FeedbackFilterChip(
                selected = filterState.typefaceConfiguredOnly(),
                onClick = {
                    toggleConfiguration(
                        filterState,
                        AppListFilterState.ConfigurationFilter.TYPEFACE,
                        onFilterChanged
                    )
                },
                label = { Text(stringResource(R.string.app_filter_custom_font)) },
                leadingIcon = if (filterState.typefaceConfiguredOnly()) {
                    { SelectedChipIcon() }
                } else null); FeedbackFilterChip(
            selected = filterState.hookConfiguredOnly(),
            onClick = {
                toggleConfiguration(
                    filterState,
                    AppListFilterState.ConfigurationFilter.HOOK,
                    onFilterChanged
                )
            },
            label = { Text(stringResource(R.string.app_filter_custom_hook)) },
            leadingIcon = if (filterState.hookConfiguredOnly()) {
                { SelectedChipIcon() }
            } else null)
        }
            AppFilterLabel(R.string.app_filter_sort)
            ChipRow {
                SortChip(filterState, AppListFilterState.SortOrder.NAME, R.string.app_filter_sort_name, onFilterChanged)
                SortChip(filterState, AppListFilterState.SortOrder.UPDATED, R.string.app_filter_sort_updated, onFilterChanged)
                SortChip(filterState, AppListFilterState.SortOrder.INSTALLED, R.string.app_filter_sort_installed, onFilterChanged)
            }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable private fun AppFilterLabel(@androidx.annotation.StringRes label: Int) = Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
@Composable private fun ChipRow(content: @Composable FlowRowScope.() -> Unit) = FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
@Composable
private fun ConfigurationChipRow(content: @Composable RowScope.() -> Unit) =
    FilterSheetScrollChipRow(content = content)
private fun toggleConfiguration(
    state: AppListFilterState,
    filter: AppListFilterState.ConfigurationFilter,
    onChanged: (AppListFilterState) -> Unit,
) = onChanged(state.toggleConfiguration(filter))

@Composable private fun SortChip(state: AppListFilterState, sort: AppListFilterState.SortOrder, label: Int, onChanged: (AppListFilterState) -> Unit) = FeedbackFilterChip(selected = state.sortOrder() == sort, onClick = { onChanged(state.withSortOrder(sort)) }, label = { Text(stringResource(label)) })
@Composable private fun SelectedChipIcon() { Icon(painterResource(R.drawable.ic_check_24), contentDescription = null, modifier = Modifier.size(18.dp)) }
