package com.dpis.module.ui.compose

import android.graphics.Typeface
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.dpis.module.ui.dialog.ConfirmAlertDialog

class FontLibraryUiItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val inUse: Boolean,
    val previewTypeface: Typeface?
)

sealed class FontLibraryDialog {
    data class Name(
        val uri: Uri,
        val sourceName: String,
        val mimeType: String?,
        val initial: String,
    ) : FontLibraryDialog()
    data class Large(
        val uri: Uri,
        val sourceName: String,
        val mimeType: String?,
        val displayName: String,
        val sizeMiB: Long,
    ) : FontLibraryDialog()
    data class Repair(val missingCount: Int) : FontLibraryDialog()
}

class FontLibraryPresentation {
    var items: List<FontLibraryUiItem> by mutableStateOf(emptyList())
        private set
    var dialog: FontLibraryDialog? by mutableStateOf(null)
        private set

    fun show(items: List<FontLibraryUiItem>) {
        this.items = items
    }

    fun show(dialog: FontLibraryDialog) {
        this.dialog = dialog
    }

    fun dismiss() {
        dialog = null
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

sealed class FontDetailDialog {
    data class Rename(val initial: String) : FontDetailDialog()
    data object Fallback : FontDetailDialog()
    data class Delete(val title: String, val message: String, val confirm: String) : FontDetailDialog()
    data class Restore(val packageName: String, val label: String) : FontDetailDialog()
}

class FontDetailPresentation {
    var state: FontDetailUiState? by mutableStateOf(null)
        private set
    var dialog: FontDetailDialog? by mutableStateOf(null)
        private set

    fun show(state: FontDetailUiState) {
        this.state = state
    }

    fun show(dialog: FontDetailDialog) {
        this.dialog = dialog
    }

    fun dismiss() {
        dialog = null
    }
}

@Composable
fun FontLibraryContent(
    presentation: FontLibraryPresentation,
    onBack: () -> Unit,
    onImportFont: () -> Unit,
    onExportArchive: () -> Unit,
    onImportArchive: () -> Unit,
    onFontSelected: (String) -> Unit,
    onNameSubmit: (String) -> Unit,
    onLargeConfirm: () -> Unit,
    onRepairConfirm: () -> Unit,
) {
    var archiveMenuExpanded by remember { mutableStateOf(false) }
    FontLibraryDialogHost(
        presentation = presentation,
        onNameSubmit = onNameSubmit,
        onLargeConfirm = onLargeConfirm,
        onRepairConfirm = onRepairConfirm,
    )
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
                OverflowMenu(
                    expanded = archiveMenuExpanded,
                    onDismiss = { archiveMenuExpanded = false },
                ) {
                    OverflowMenuItem(
                        textRes = R.string.font_library_export_archive_action,
                        onClick = {
                            archiveMenuExpanded = false
                            onExportArchive()
                        },
                    )
                    OverflowMenuItem(
                        textRes = R.string.font_library_import_archive_action,
                        onClick = {
                            archiveMenuExpanded = false
                            onImportArchive()
                        },
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = rememberClickAction(onImportFont),
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
                    FontLibraryCard(item, rememberClickAction { onFontSelected(item.id) })
                }
            }
        }
    }
}

@Composable
private fun FontLibraryCard(item: FontLibraryUiItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().dpisClickable(onClick = onClick),
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
    onRemoveReference: (String) -> Unit,
    onRenameSubmit: (String) -> Boolean,
    onFallbackRetry: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onRestoreConfirm: () -> Unit,
) {
    val state = presentation.state ?: return
    var menuExpanded by remember { mutableStateOf(false) }
    FontDetailDialogHost(
        presentation = presentation,
        onRenameSubmit = onRenameSubmit,
        onFallbackRetry = onFallbackRetry,
        onDeleteConfirm = onDeleteConfirm,
        onRestoreConfirm = onRestoreConfirm,
    )
    SecondaryPageScaffold(
        onBack = onBack,
        titleRes = R.string.font_library_detail_page_title,
        actions = {
            Box(modifier = Modifier.padding(end = 16.dp)) {
                DpisToolbarIconButton(
                    iconRes = R.drawable.ic_more_vert_24,
                    descriptionRes = R.string.font_library_detail_menu_action,
                    onClick = { menuExpanded = true },
                )
                OverflowMenu(
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false },
                ) {
                    if (state.publicationFailed) {
                        OverflowMenuItem(
                            textRes = R.string.font_library_publication_retry_action,
                            onClick = {
                                menuExpanded = false
                                onRetryPublication()
                            },
                        )
                    }
                    OverflowMenuItem(
                        textRes = R.string.font_library_rename_action,
                        onClick = {
                            menuExpanded = false
                            onRename()
                        },
                    )
                    OverflowMenuItem(
                        textRes = R.string.font_library_delete_action,
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        },
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + SecondaryPageContentTokens.TitleToContentGap,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = edgeToEdgeContentBottomPadding(24.dp),
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { FontDetailCard(state) }
            item { FontReferenceSection(state.references, onRemoveReference) }
        }
    }
}

@Composable
private fun FontDetailCard(state: FontDetailUiState) {
    val publication = stringResource(
        if (state.isPublished) R.string.font_library_public_badge
        else R.string.font_library_private_badge,
    )
    val status = if (state.inUse) {
        stringResource(R.string.font_library_used_badge) + " · " + publication
    } else {
        publication
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                state.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                state.sourceFileName,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                status,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.previewTypeface?.let { typeface ->
                Spacer(Modifier.height(12.dp))
                Text(
                    "AaBbCc 你好世界 123",
                    fontFamily = FontFamily(typeface),
                    fontSize = 26.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "The quick brown fox jumps over the lazy dog",
                    modifier = Modifier.padding(top = 6.dp),
                    fontFamily = FontFamily(typeface),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun FontReferenceSection(
    references: List<FontReferenceUiItem>,
    onRemoveReference: (String) -> Unit,
) {
    Column {
        FontSectionTitle(R.string.font_library_used_by_title)
        if (references.isEmpty()) {
            Text(
                stringResource(R.string.font_library_unused),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(
                Modifier.padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                references.forEachIndexed { index, reference ->
                    val restore = rememberClickAction {
                        onRemoveReference(reference.packageName)
                    }
                    SegmentedListItem(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = restore,
                        shapes = dpisSegmentedShapes(index, references.size),
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        supportingContent = { Text(reference.packageName) },
                        content = { Text(reference.label) },
                    )
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
@OptIn(ExperimentalMaterial3Api::class)
private fun DpisToolbarIconButton(iconRes: Int, descriptionRes: Int, onClick: () -> Unit) {
    val description = stringResource(descriptionRes)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Below,
        ),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = rememberClickAction(onClick)) {
            Icon(painterResource(iconRes), contentDescription = description)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun OverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismiss,
        popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
            MenuAnchorPosition.Below,
        ),
    ) {
        OverflowMenuGroup(content = content)
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun OverflowMenuGroup(
    index: Int = 0,
    count: Int = 1,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenuGroup(
        shapes = MenuDefaults.groupShape(index, count),
        content = content,
    )
}

@Composable
private fun OverflowMenuItem(
    textRes: Int,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(textRes)) },
        onClick = rememberClickAction(onClick),
    )
}

@Preview(showBackground = true)
@Composable
private fun FontLibraryContentPreview() {
    val presentation = remember {
        FontLibraryPresentation().apply {
            show(listOf(FontLibraryUiItem("font", "Noto Sans SC", "NotoSansSC.ttf", true, null)))
        }
    }
    ComposeDesignSystem(darkTheme = false) {
        FontLibraryContent(presentation, {}, {}, {}, {}, {}, {}, {}, {})
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
    ComposeDesignSystem(darkTheme = false) {
        FontDetailContent(presentation, {}, {}, {}, {}, {}, { true }, {}, {}, {})
    }
}

@Composable
internal fun FontLibraryDialogHost(
    presentation: FontLibraryPresentation,
    onNameSubmit: (String) -> Unit,
    onLargeConfirm: () -> Unit,
    onRepairConfirm: () -> Unit,
) {
    when (val dialog = presentation.dialog) {
        is FontLibraryDialog.Name -> TextInputDialog(
            title = stringResource(R.string.font_library_name_title),
            hint = stringResource(R.string.font_library_name_hint),
            initialValue = dialog.initial,
            onDismiss = presentation::dismiss,
            onSubmit = onNameSubmit,
        )
        is FontLibraryDialog.Large -> ConfirmAlertDialog(
            onDismissRequest = presentation::dismiss,
            title = stringResource(R.string.font_library_large_import_title),
            message = stringResource(R.string.font_library_large_import_message, dialog.sizeMiB),
            cancelLabel = stringResource(R.string.dialog_process_action_confirm_negative),
            confirmLabel = stringResource(R.string.font_library_large_import_continue),
            onConfirm = onLargeConfirm,
        )
        is FontLibraryDialog.Repair -> ConfirmAlertDialog(
            onDismissRequest = presentation::dismiss,
            title = stringResource(R.string.font_library_publication_repair_title),
            message = stringResource(
                R.string.font_library_publication_repair_message,
                dialog.missingCount,
            ),
            cancelLabel = stringResource(R.string.dialog_process_action_confirm_negative),
            confirmLabel = stringResource(R.string.font_library_publication_retry_action),
            onConfirm = onRepairConfirm,
        )
        null -> Unit
    }
}

@Composable
internal fun FontDetailDialogHost(
    presentation: FontDetailPresentation,
    onRenameSubmit: (String) -> Boolean,
    onFallbackRetry: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onRestoreConfirm: () -> Unit,
) {
    when (val dialog = presentation.dialog) {
        is FontDetailDialog.Rename -> TextInputDialog(
            title = stringResource(R.string.font_library_name_title),
            hint = stringResource(R.string.font_library_name_hint),
            initialValue = dialog.initial,
            onDismiss = presentation::dismiss,
            onSubmit = { name ->
                if (onRenameSubmit(name)) presentation.dismiss()
            },
        )
        FontDetailDialog.Fallback -> ConfirmAlertDialog(
            onDismissRequest = presentation::dismiss,
            title = stringResource(R.string.font_library_fallback_dialog_title),
            message = stringResource(R.string.font_library_fallback_dialog_message),
            cancelLabel = stringResource(R.string.dialog_close_button),
            confirmLabel = stringResource(R.string.font_library_publication_retry_action),
            onConfirm = onFallbackRetry,
        )
        is FontDetailDialog.Delete -> ConfirmAlertDialog(
            onDismissRequest = presentation::dismiss,
            title = dialog.title,
            message = dialog.message,
            cancelLabel = stringResource(R.string.dialog_process_action_confirm_negative),
            confirmLabel = dialog.confirm,
            onConfirm = onDeleteConfirm,
        )
        is FontDetailDialog.Restore -> ConfirmAlertDialog(
            onDismissRequest = presentation::dismiss,
            title = stringResource(R.string.font_library_restore_app_font_title),
            message = stringResource(R.string.font_library_restore_app_font_message, dialog.label),
            cancelLabel = stringResource(R.string.dialog_process_action_confirm_negative),
            confirmLabel = stringResource(R.string.font_library_restore_default_action),
            onConfirm = onRestoreConfirm,
        )
        null -> Unit
    }
}
