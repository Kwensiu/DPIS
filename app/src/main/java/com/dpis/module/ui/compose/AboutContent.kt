package com.dpis.module.ui.compose

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dpis.module.R

@Composable
fun AboutContent(
    versionText: String,
    showDebugUpdateEntry: Boolean,
    onBack: () -> Unit,
    onCheckUpdates: () -> Unit,
    onShowDebugUpdate: () -> Unit,
    onOpenSource: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier
) {
    SecondaryPageScaffold(
        modifier = modifier.fillMaxSize(),
        titleRes = R.string.about_title,
        onBack = onBack,
    ) { contentPadding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                top = contentPadding.calculateTopPadding() + SecondaryPageContentTokens.TitleToContentGap,
                end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = edgeToEdgeContentBottomPadding(24.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceBright
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            versionText,
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.module_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                ) {
                    val total = if (showDebugUpdateEntry) 5 else 4
                    AboutEntry(
                        R.drawable.ic_refresh_24,
                        R.string.about_link_update_title,
                        R.string.about_link_update_desc,
                        index = 0,
                        total = total,
                        onCheckUpdates
                    )
                    AboutEntry(
                        R.drawable.ic_code_24,
                        R.string.about_link_source_title,
                        R.string.about_link_source_desc,
                        index = 1,
                        total = total,
                        onOpenSource
                    )
                    if (showDebugUpdateEntry) {
                        AboutEntry(
                            R.drawable.ic_refresh_24,
                            R.string.about_link_update_dialog_debug_only_title,
                            R.string.about_link_update_dialog_debug_only_desc,
                            index = 2,
                            total = total,
                            onShowDebugUpdate
                        )
                    }
                    AboutEntry(
                        R.drawable.ic_adjust_24,
                        R.string.about_link_feedback_title,
                        R.string.about_link_feedback_desc,
                        index = total - 2,
                        total = total,
                        onOpenFeedback
                    )
                    AboutEntry(
                        R.drawable.ic_license_24,
                        R.string.open_source_license,
                        R.string.open_source_license_settings_description,
                        index = total - 1,
                        total = total,
                        onOpenLicenses
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun AboutEntry(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes descriptionRes: Int,
    index: Int,
    total: Int,
    onClick: () -> Unit
) {
    val confirm = rememberConfirmAction(onClick)
    SegmentedListItem(
        onClick = confirm,
        // The leading icon follows the full row, not the first text baseline. A wrapped
        // supporting line may increase row height without pulling the icon upward.
        verticalAlignment = Alignment.CenterVertically,
        shapes = dpisSegmentedShapes(index, total),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            contentColor = MaterialTheme.colorScheme.onSurface,
            leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        supportingContent = { Text(stringResource(descriptionRes)) },
        leadingContent = { Icon(painterResource(iconRes), contentDescription = null) },
        trailingContent = {
            Icon(
                painterResource(R.drawable.ic_chevron_right_24),
                contentDescription = null
            )
        },
        content = { Text(stringResource(titleRes)) }
    )
}

@Preview(showBackground = true)
@Composable
private fun AboutContentPreview() {
    DpisTheme(darkTheme = false) {
        AboutContent(
            versionText = "Version: 1.15.0 (11500)",
            showDebugUpdateEntry = true,
            onBack = {},
            onCheckUpdates = {},
            onShowDebugUpdate = {},
            onOpenSource = {},
            onOpenFeedback = {},
            onOpenLicenses = {}
        )
    }
}
