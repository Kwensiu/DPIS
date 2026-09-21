package com.dpis.module.about.presentation

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.dpis.module.R
import com.dpis.module.about.OpenSourceLicenseItem
import com.dpis.module.ui.WatchUiMode
import com.dpis.module.ui.presentation.design.ComposeDesignSystem
import com.dpis.module.ui.presentation.design.rememberClickAction
import com.dpis.module.ui.presentation.design.setFeatureContent
import com.dpis.module.ui.presentation.wear.WearOpenSourceLicenseContent
import com.dpis.module.ui.presentation.workspace.SecondaryPageScaffold
import com.dpis.module.ui.presentation.workspace.dpisSegmentedShapes
import com.dpis.module.ui.presentation.workspace.segmentedRowColors
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries

@Composable
fun OpenSourceLicenseContent(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val libraries by produceLibraries(R.raw.aboutlibraries)
    val items = remember(libraries) {
        resolvePhoneLicenseItems(context, libraries)
    }
    OpenSourceLicenseContent(
        items = items,
        onBack = onBack,
        onOpenUrl = onOpenUrl,
        modifier = modifier,
    )
}

@Composable
fun OpenSourceLicenseContent(
    items: List<OpenSourceLicenseItem>,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
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
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        items(
            count = items.size,
            key = { index -> "${items[index].name}\u0000${items[index].website}" },
        ) { index ->
            val item = items[index]
            val select = rememberClickAction { selectedItem = item }
            LicenseEntry(
                item = item,
                index = index,
                total = items.size,
                onClick = select,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LicenseEntry(
    item: OpenSourceLicenseItem,
    index: Int,
    total: Int,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shapes = dpisSegmentedShapes(index, total),
        colors = segmentedRowColors(),
        supportingContent = { Text(item.summary) },
        content = { Text(item.name) },
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
                    "https://github.com/Kwensiu/DPIS",
                ),
            ),
            onBack = {},
            onOpenUrl = {},
        )
    }
}

fun ComponentActivity.installOpenSourceLicenses(onOpenUrl: (String) -> Unit) {
    val wearItems = if (WatchUiMode.shouldUseCompactUi(this)) {
        OpenSourceLicenseItems.load(this)
    } else {
        emptyList()
    }
    setFeatureContent {
        if (WatchUiMode.shouldUseCompactUi(this@installOpenSourceLicenses)) {
            WearOpenSourceLicenseContent(
                items = wearItems,
                onOpenUrl = onOpenUrl,
            )
        } else {
            OpenSourceLicenseContent(
                onBack = ::finish,
                onOpenUrl = onOpenUrl,
            )
        }
    }
}

internal fun resolvePhoneLicenseItems(
    context: Context,
    libraries: Libs?,
): List<OpenSourceLicenseItem> {
    val project = OpenSourceLicenseItems.projectItem(context)
    if (libraries == null) {
        return listOf(project)
    }
    val mapped = libraries.libraries
        .map { OpenSourceLicenseItems.fromLibrary(it, context) }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    return if (mapped.isEmpty()) {
        listOf(
            project,
            OpenSourceLicenseItems.emptyItem(
                context,
                context.getString(R.string.open_source_license_empty),
            ),
        )
    } else {
        listOf(project) + mapped
    }
}
