package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.about.OpenSourceLicenseActivity

@Composable
fun OpenSourceLicenseContent(
    items: List<OpenSourceLicenseActivity.LicenseItem>,
    onBack: () -> Unit,
    onItemSelected: (OpenSourceLicenseActivity.LicenseItem) -> Unit,
    modifier: Modifier = Modifier
) {
    SecondaryPageScaffold(
        modifier = modifier.fillMaxSize(),
        titleRes = R.string.open_source_license,
        onBack = onBack,
    ) { contentPadding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = edgeToEdgeContentBottomPadding(24.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            items(
                count = items.size,
                key = { index -> "${items[index].name}\u0000${items[index].website}" }
            ) { index ->
                val item = items[index]
                val select = rememberDpisConfirmAction { onItemSelected(item) }
                LicenseEntry(
                    item = item,
                    index = index,
                    total = items.size,
                    onClick = select
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LicenseEntry(
    item: OpenSourceLicenseActivity.LicenseItem,
    index: Int,
    total: Int,
    onClick: () -> Unit
) {
    SegmentedListItem(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shapes = dpisSegmentedShapes(index, total),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        supportingContent = { Text(item.summary) },
        content = { Text(item.name) }
    )
}

@Preview(showBackground = true)
@Composable
private fun OpenSourceLicenseContentPreview() {
    DpisTheme(darkTheme = false) {
        OpenSourceLicenseContent(
            items = listOf(
                OpenSourceLicenseActivity.LicenseItem(
                    "DPIS",
                    "GPL-3.0-or-later",
                    "License detail",
                    "https://github.com/Kwensiu/DPIS"
                )
            ),
            onBack = {},
            onItemSelected = {}
        )
    }
}
