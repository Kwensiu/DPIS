package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.BuildConfig
import com.dpis.module.applist.AppListFilterState

/** App catalogue filters. Visual grouping mirrors the template target picker, state remains local. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppFilterSheet(
    filterState: AppListFilterState,
    onFilterChanged: (AppListFilterState) -> Unit,
    onDismissRequest: () -> Unit
) {
    FilterSheetScaffold(
        onDismissRequest = onDismissRequest,
        title = {
            Text(stringResource(R.string.app_filter_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.offset(x = FilterSheetUiTokens.HeaderTitleOffset))
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
                FeedbackFilterChip(selected = filterState.allAppsSelected(), onClick = { onFilterChanged(filterState.withAllApps()) }, label = { Text(stringResource(R.string.app_filter_all)) })
                FeedbackFilterChip(selected = !filterState.allAppsSelected() && filterState.systemAppsSelected(), onClick = { onFilterChanged(filterState.withAppTypes(filterState.userAppsSelected(), !filterState.systemAppsSelected())) }, label = { Text(stringResource(R.string.app_filter_system)) }, leadingIcon = if (!filterState.allAppsSelected() && filterState.systemAppsSelected()) { { SelectedChipIcon() } } else null)
                FeedbackFilterChip(selected = !filterState.allAppsSelected() && filterState.userAppsSelected(), onClick = { onFilterChanged(filterState.withAppTypes(!filterState.userAppsSelected(), filterState.systemAppsSelected())) }, label = { Text(stringResource(R.string.app_filter_user)) }, leadingIcon = if (!filterState.allAppsSelected() && filterState.userAppsSelected()) { { SelectedChipIcon() } } else null)
            }
            AppFilterLabel(R.string.app_filter_configuration)
            ConfigurationChipRow {
                FeedbackFilterChip(selected = filterState.allConfigurationSelected(), onClick = { onFilterChanged(filterState.withConfiguration(false, false, false, false, false, false)) }, label = { Text(stringResource(R.string.app_filter_all)) })
                if (BuildConfig.FLAVOR != "legacy") {
                    FeedbackFilterChip(selected = filterState.injectedOnly(), onClick = { onFilterChanged(filterState.withConfiguration(!filterState.injectedOnly(), filterState.disabledOnly(), filterState.widthConfiguredOnly(), filterState.fontConfiguredOnly(), filterState.typefaceConfiguredOnly(), filterState.hookConfiguredOnly())) }, label = { Text(stringResource(R.string.app_filter_scoped)) }, leadingIcon = if (filterState.injectedOnly()) { { SelectedChipIcon() } } else null)
                }
                FeedbackFilterChip(selected = filterState.disabledOnly(), onClick = { onFilterChanged(filterState.withConfiguration(filterState.injectedOnly(), !filterState.disabledOnly(), filterState.widthConfiguredOnly(), filterState.fontConfiguredOnly(), filterState.typefaceConfiguredOnly(), filterState.hookConfiguredOnly())) }, label = { Text(stringResource(R.string.app_filter_disabled)) }, leadingIcon = if (filterState.disabledOnly()) { { SelectedChipIcon() } } else null)
            }
            ConfigurationChipRow { FeedbackFilterChip(selected = filterState.widthConfiguredOnly(), onClick = { onFilterChanged(filterState.withConfiguration(filterState.injectedOnly(), filterState.disabledOnly(), !filterState.widthConfiguredOnly(), filterState.fontConfiguredOnly(), filterState.typefaceConfiguredOnly(), filterState.hookConfiguredOnly())) }, label = { Text(stringResource(R.string.app_filter_viewport)) }, leadingIcon = if (filterState.widthConfiguredOnly()) { { SelectedChipIcon() } } else null); FeedbackFilterChip(selected = filterState.fontConfiguredOnly(), onClick = { onFilterChanged(filterState.withConfiguration(filterState.injectedOnly(), filterState.disabledOnly(), filterState.widthConfiguredOnly(), !filterState.fontConfiguredOnly(), filterState.typefaceConfiguredOnly(), filterState.hookConfiguredOnly())) }, label = { Text(stringResource(R.string.app_filter_font_scale)) }, leadingIcon = if (filterState.fontConfiguredOnly()) { { SelectedChipIcon() } } else null) }
            ConfigurationChipRow { FeedbackFilterChip(selected = filterState.typefaceConfiguredOnly(), onClick = { onFilterChanged(filterState.withConfiguration(filterState.injectedOnly(), filterState.disabledOnly(), filterState.widthConfiguredOnly(), filterState.fontConfiguredOnly(), !filterState.typefaceConfiguredOnly(), filterState.hookConfiguredOnly())) }, label = { Text(stringResource(R.string.app_filter_custom_font)) }, leadingIcon = if (filterState.typefaceConfiguredOnly()) { { SelectedChipIcon() } } else null); FeedbackFilterChip(selected = filterState.hookConfiguredOnly(), onClick = { onFilterChanged(filterState.withConfiguration(filterState.injectedOnly(), filterState.disabledOnly(), filterState.widthConfiguredOnly(), filterState.fontConfiguredOnly(), filterState.typefaceConfiguredOnly(), !filterState.hookConfiguredOnly())) }, label = { Text(stringResource(R.string.app_filter_custom_hook)) }, leadingIcon = if (filterState.hookConfiguredOnly()) { { SelectedChipIcon() } } else null) }
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
    HorizontalScrollWithEdgeFade(
        edgeWidth = EdgeFadeTokens.Width,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
@Composable private fun AppTypeChip(state: AppListFilterState, type: AppListFilterState.AppType, label: Int, onChanged: (AppListFilterState) -> Unit) = FeedbackFilterChip(selected = state.appType() == type, onClick = { onChanged(state.withAppType(type)) }, label = { Text(stringResource(label)) })
@Composable private fun BooleanChip(selected: Boolean, label: Int, onClick: () -> Unit) = FeedbackFilterChip(selected = selected, onClick = onClick, label = { Text(stringResource(label)) })
@Composable private fun SortChip(state: AppListFilterState, sort: AppListFilterState.SortOrder, label: Int, onChanged: (AppListFilterState) -> Unit) = FeedbackFilterChip(selected = state.sortOrder() == sort, onClick = { onChanged(state.withSortOrder(sort)) }, label = { Text(stringResource(label)) })
@Composable private fun SelectedChipIcon() { Icon(painterResource(R.drawable.ic_check_24), contentDescription = null, modifier = Modifier.size(18.dp)) }
