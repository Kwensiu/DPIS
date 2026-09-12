package com.dpis.module.about.presentation

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import com.dpis.module.R
import com.dpis.module.about.OpenSourceLicenseItem
import com.dpis.module.ui.WatchUiMode
import com.dpis.module.ui.compose.WearOpenSourceLicenseContent
import com.dpis.module.ui.compose.setFeatureContent
import com.dpis.module.ui.compose.ComposeDesignSystem
import com.dpis.module.ui.compose.SecondaryPageContentTokens
import com.dpis.module.ui.compose.SecondaryPageScaffold
import com.dpis.module.ui.compose.dpisSegmentedShapes
import com.dpis.module.ui.compose.edgeToEdgeContentBottomPadding
import com.dpis.module.ui.compose.rememberClickAction

@Composable
fun OpenSourceLicenseContent(
    items: List<OpenSourceLicenseItem>,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedItem by remember { mutableStateOf<OpenSourceLicenseItem?>(null) }
    selectedItem?.let { item ->
        LicenseDetailDialog(
            item = item,
            onOpenUrl = onOpenUrl,
            onDismiss = { selectedItem = null },
        )
    }
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
                top = contentPadding.calculateTopPadding() + SecondaryPageContentTokens.TitleToContentGap,
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
                val select = rememberClickAction { selectedItem = item }
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
    item: OpenSourceLicenseItem,
    index: Int,
    total: Int,
    onClick: () -> Unit
) {
    SegmentedListItem(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shapes = dpisSegmentedShapes(index, total),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        supportingContent = { Text(item.summary) },
        content = { Text(item.name) }
    )
}

@Preview(showBackground = true)
@Composable
private fun OpenSourceLicenseContentPreview() {
    ComposeDesignSystem(darkTheme = false) {
        OpenSourceLicenseContent(
            items = listOf(
                OpenSourceLicenseItem(
                    "DPIS",
                    "GPL-3.0-or-later",
                    "License detail",
                    "https://github.com/Kwensiu/DPIS"
                )
            ),
            onBack = {},
            onOpenUrl = {}
        )
    }
}

fun ComponentActivity.installOpenSourceLicenses(
    items: List<OpenSourceLicenseItem>,
    onOpenUrl: (String) -> Unit,
) {
    setFeatureContent {
        if (WatchUiMode.shouldUseCompactUi(this@installOpenSourceLicenses)) {
            WearOpenSourceLicenseContent(
                items = items,
                onOpenUrl = onOpenUrl,
            )
        } else {
            OpenSourceLicenseContent(
                items = items,
                onBack = ::finish,
                onOpenUrl = onOpenUrl,
            )
        }
    }
}
