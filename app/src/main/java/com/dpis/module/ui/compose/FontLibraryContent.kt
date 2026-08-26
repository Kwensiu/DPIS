package com.dpis.module.ui.compose

import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpis.module.R

class FontLibraryUiItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val inUse: Boolean,
    val previewTypeface: Typeface?
)

class FontLibraryPresentation {
    var items: List<FontLibraryUiItem> by mutableStateOf(emptyList())
        private set

    fun show(items: List<FontLibraryUiItem>) {
        this.items = items
    }
}

class FontReferenceUiItem(
    val packageName: String,
    val label: String
)

class FontDetailUiState(
    val title: String,
    val sourceFileName: String,
    val inUse: Boolean,
    val isPublished: Boolean,
    val publicationFailed: Boolean,
    val previewTypeface: Typeface?,
    val references: List<FontReferenceUiItem>
)

class FontDetailPresentation {
    var state: FontDetailUiState? by mutableStateOf(null)
        private set

    fun show(state: FontDetailUiState) {
        this.state = state
    }
}

@Composable
fun FontLibraryContent(
    presentation: FontLibraryPresentation,
    onBack: () -> Unit,
    onImportFont: () -> Unit,
    onExportArchive: () -> Unit,
    onImportArchive: () -> Unit,
    onFontSelected: (String) -> Unit
) {
    var archiveMenuExpanded by remember { mutableStateOf(false) }
    SecondaryPageScaffold(
        onBack = onBack,
        titleRes = R.string.font_library_page_title,
        actions = {
            Box(modifier = Modifier.padding(end = 16.dp)) {
                DpisToolbarIconButton(
                    iconRes = R.drawable.ic_more_vert_24,
                    descriptionRes = R.string.font_library_archive_menu_action,
                    onClick = { archiveMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = archiveMenuExpanded,
                    onDismissRequest = { archiveMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.font_library_export_archive_action)) },
                        onClick = {
                            archiveMenuExpanded = false
                            onExportArchive()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.font_library_import_archive_action)) },
                        onClick = {
                            archiveMenuExpanded = false
                            onImportArchive()
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = rememberDpisConfirmAction(onImportFont),
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(
                    painterResource(R.drawable.ic_upload_file_24),
                    contentDescription = stringResource(R.string.font_library_import_action)
                )
            }
        }
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val items = presentation.items
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.font_library_empty),
                    modifier = Modifier.padding(32.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                    top = padding.calculateTopPadding() + SecondaryPageContentTokens.TitleToContentGap,
                    end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                    bottom = edgeToEdgeContentBottomPadding(88.dp)
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    FontLibraryCard(item, rememberDpisConfirmAction { onFontSelected(item.id) })
                }
            }
        }
    }
}

@Composable
private fun FontLibraryCard(item: FontLibraryUiItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.inUse) {
                    DpisStatusBadge(
                        text = stringResource(R.string.font_library_used_badge),
                        primary = true
                    )
                }
            }
            Text(
                item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.previewTypeface?.let { typeface ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "AaBbCc 你好世界 123",
                    fontFamily = FontFamily(typeface),
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FontDetailContent(
    presentation: FontDetailPresentation,
    onBack: () -> Unit,
    onRetryPublication: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onRemoveReference: (String) -> Unit
) {
    val state = presentation.state ?: return
    SecondaryPageScaffold(
        onBack = onBack,
        titleRes = R.string.font_library_detail_page_title,
        actions = {
            if (state.publicationFailed) {
                DpisToolbarIconButton(
                    R.drawable.ic_build_24,
                    R.string.font_library_publication_retry_action,
                    onRetryPublication
                )
                Spacer(Modifier.width(8.dp))
            }
            DpisToolbarIconButton(
                R.drawable.ic_edit_24,
                R.string.font_library_rename_action,
                onRename
            )
            Spacer(Modifier.width(8.dp))
            DpisToolbarIconButton(
                R.drawable.ic_delete_24,
                R.string.font_library_delete_action,
                onDelete
            )
        }
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + SecondaryPageContentTokens.TitleToContentGap,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = edgeToEdgeContentBottomPadding(24.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { FontDetailHeader(state) }
            state.previewTypeface?.let { typeface ->
                item { FontPreviewSection(typeface) }
            }
            item {
                FontReferenceSection(state.references, onRemoveReference)
            }
        }
    }
}

@Composable
private fun FontDetailHeader(state: FontDetailUiState) {
    Column {
        Text(state.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            state.sourceFileName,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.inUse) {
                DpisStatusBadge(stringResource(R.string.font_library_used_badge), primary = true)
            }
            DpisStatusBadge(
                if (state.isPublished) stringResource(R.string.font_library_public_badge)
                else stringResource(R.string.font_library_private_badge),
                primary = state.isPublished
            )
        }
    }
}

@Composable
private fun FontPreviewSection(typeface: Typeface) {
    Column {
        FontSectionTitle(R.string.font_library_preview_title)
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "AaBbCc 你好世界 123",
                    fontFamily = FontFamily(typeface),
                    fontSize = 26.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "The quick brown fox jumps over the lazy dog",
                    modifier = Modifier.padding(top = 8.dp),
                    fontFamily = FontFamily(typeface),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FontReferenceSection(
    references: List<FontReferenceUiItem>,
    onRemoveReference: (String) -> Unit
) {
    Column {
        FontSectionTitle(R.string.font_library_active_apps_title)
        if (references.isEmpty()) {
            Text(
                stringResource(R.string.font_library_unused),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                Modifier.padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                references.forEachIndexed { index, reference ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = dpisSegmentedShapes(index, references.size).shape,
                        color = MaterialTheme.colorScheme.surfaceBright
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(reference.label, fontWeight = FontWeight.Bold)
                                Text(
                                    reference.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            AssistChip(
                                onClick = rememberDpisConfirmAction {
                                    onRemoveReference(reference.packageName)
                                },
                                label = { Text(stringResource(R.string.font_library_remove_app_action)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FontSectionTitle(textRes: Int) {
    Text(stringResource(textRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun DpisStatusBadge(text: String, primary: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (primary) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (primary) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun DpisToolbarIconButton(iconRes: Int, descriptionRes: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(36.dp).clip(CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = rememberDpisConfirmAction(onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painterResource(iconRes),
                contentDescription = stringResource(descriptionRes),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FontLibraryContentPreview() {
    val presentation = remember {
        FontLibraryPresentation().apply {
            show(listOf(FontLibraryUiItem("font", "Noto Sans SC", "NotoSansSC.ttf", true, null)))
        }
    }
    DpisTheme(darkTheme = false) {
        FontLibraryContent(presentation, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun FontDetailContentPreview() {
    val presentation = remember {
        FontDetailPresentation().apply {
            show(
                FontDetailUiState(
                    "Noto Sans SC",
                    "NotoSansSC.ttf",
                    true,
                    false,
                    false,
                    null,
                    listOf(FontReferenceUiItem("com.example.app", "Example"))
                )
            )
        }
    }
    DpisTheme(darkTheme = false) {
        FontDetailContent(presentation, {}, {}, {}, {}, {})
    }
}
