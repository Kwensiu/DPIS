package com.dpis.module.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.applist.AppListFilterState

/** Main-catalogue filter surface. Values commit immediately through the shared UI state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppFilterSheet(
    filterState: AppListFilterState,
    onFilterChanged: (AppListFilterState) -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppFilterSheetTokens.HorizontalPadding)
                .padding(vertical = AppFilterSheetTokens.VerticalPadding),
            verticalArrangement = Arrangement.spacedBy(AppFilterSheetTokens.SwitchGap)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(
                            width = AppFilterSheetTokens.VisualLineWidth,
                            height = AppFilterSheetTokens.VisualLineHeight
                        )
                        .background(
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(AppFilterSheetTokens.VisualLineHeight)
                        )
                )
            }
            Text(
                stringResource(R.string.filter_sheet_title),
                modifier = Modifier.padding(top = AppFilterSheetTokens.TitleGap),
                style = MaterialTheme.typography.titleLarge
            )
            AppFilterSwitch(
                label = R.string.filter_show_system_apps,
                checked = filterState.showSystemApps(),
                modifier = Modifier.padding(top = AppFilterSheetTokens.FirstSwitchGap),
                onCheckedChange = {
                    onFilterChanged(
                        AppListFilterState(
                            it,
                            filterState.injectedOnly(),
                            filterState.widthConfiguredOnly(),
                            filterState.fontConfiguredOnly()
                        )
                    )
                }
            )
            AppFilterSwitch(
                label = R.string.filter_injected_only,
                checked = filterState.injectedOnly(),
                onCheckedChange = {
                    onFilterChanged(
                        AppListFilterState(
                            filterState.showSystemApps(),
                            it,
                            filterState.widthConfiguredOnly(),
                            filterState.fontConfiguredOnly()
                        )
                    )
                }
            )
            AppFilterSwitch(
                label = R.string.filter_width_only,
                checked = filterState.widthConfiguredOnly(),
                onCheckedChange = {
                    onFilterChanged(
                        AppListFilterState(
                            filterState.showSystemApps(),
                            filterState.injectedOnly(),
                            it,
                            filterState.fontConfiguredOnly()
                        )
                    )
                }
            )
            AppFilterSwitch(
                label = R.string.filter_font_only,
                checked = filterState.fontConfiguredOnly(),
                onCheckedChange = {
                    onFilterChanged(
                        AppListFilterState(
                            filterState.showSystemApps(),
                            filterState.injectedOnly(),
                            filterState.widthConfiguredOnly(),
                            it
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun AppFilterSwitch(
    @androidx.annotation.StringRes label: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private object AppFilterSheetTokens {
    val HorizontalPadding = 24.dp
    val VerticalPadding = 20.dp
    val VisualLineWidth = 36.dp
    val VisualLineHeight = 4.dp
    val TitleGap = 16.dp
    val FirstSwitchGap = 12.dp
    val SwitchGap = 8.dp
}
