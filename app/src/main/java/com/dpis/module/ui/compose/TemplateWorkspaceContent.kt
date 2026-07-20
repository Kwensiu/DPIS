package com.dpis.module.ui.compose

import android.widget.Toast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.dpis.module.R
import com.dpis.module.templates.QuickTemplateStore
import com.dpis.module.templates.QuickTemplateTargetsPresentationController
import com.dpis.module.templates.TemplateConfigSummaryFormatter
import com.dpis.module.templates.TemplateEditorForm
import com.dpis.module.templates.TemplateWorkspacePresentation

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TemplateWorkspaceContent(
    state: TemplateWorkspacePresentation.State,
    padding: PaddingValues,
    onQueryChanged: (String) -> Unit,
    onEditorOpened: (quickTemplate: Boolean, templateId: String?) -> Unit = { _, _ -> },
    onEditorChanged: (TemplateEditorForm) -> Unit = {},
    onEditorClosed: () -> Unit = {}
) {
    var editorKind by rememberSaveable {
        mutableStateOf(editorKindFor(state.detailKind))
    }
    var editorTemplateId by rememberSaveable { mutableStateOf(state.detailTemplateId) }
    var targetsTemplateId by rememberSaveable {
        mutableStateOf(
            state.detailTemplateId.takeIf {
                state.detailKind == TemplateWorkspacePresentation.DetailKind.QUICK_TEMPLATE_TARGETS
            }
        )
    }
    var deleteConfirmationVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.detailKind, state.detailTemplateId) {
        when (state.detailKind) {
            TemplateWorkspacePresentation.DetailKind.GLOBAL_PREFILL -> {
                editorKind = EDITOR_GLOBAL
                editorTemplateId = null
                targetsTemplateId = null
            }
            TemplateWorkspacePresentation.DetailKind.QUICK_TEMPLATE -> {
                editorKind = EDITOR_QUICK
                editorTemplateId = state.detailTemplateId
                targetsTemplateId = null
            }
            TemplateWorkspacePresentation.DetailKind.QUICK_TEMPLATE_TARGETS -> {
                editorKind = null
                editorTemplateId = null
                targetsTemplateId = state.detailTemplateId
            }
            TemplateWorkspacePresentation.DetailKind.NONE -> Unit
        }
    }

    // Compose owns the visible editor on both portrait and landscape. Activity callbacks retain
    // only the route contract used by saved-instance and portrait target-Activity migration.
    fun openEditor(kind: String, templateId: String?) {
        editorKind = kind
        editorTemplateId = templateId
        targetsTemplateId = null
        onEditorOpened(kind == EDITOR_QUICK, templateId)
    }

    val selectedTemplate = state.templates.firstOrNull { it.id == editorTemplateId }
    val editorKey = "${editorKind.orEmpty()}:${editorTemplateId.orEmpty()}"
    val editorDraft = rememberTemplateEditorDraftState(editorKey) {
        when (editorKind) {
            EDITOR_GLOBAL -> TemplateEditorForm.global(state.globalPrefill)
            EDITOR_QUICK -> {
                val template = selectedTemplate
                TemplateEditorForm.quick(
                    template?.let {
                        QuickTemplateStore.QuickTemplate(
                            it.id, it.name, 0L, linkedSetOf(), it.configValue
                        )
                    },
                    ""
                )
            }
            else -> TemplateEditorForm.global(state.globalPrefill)
        }
    }

    fun notifyEditorChanged() {
        editorDraft.changed()
        onEditorChanged(editorDraft.form)
    }

    fun closeEditor() {
        if (editorKind == null) return
        editorKind = null
        editorTemplateId = null
        deleteConfirmationVisible = false
        onEditorClosed()
    }

    fun saveEditor() {
        val result = if (editorDraft.form.quickTemplate) {
            state.actions.saveQuickTemplate(editorDraft.form)
        } else {
            state.actions.saveGlobalPrefill(editorDraft.form)
        }
        if (result.success) {
            editorDraft.form.markSaved(result.templateId)
            editorTemplateId = editorDraft.form.templateId
            notifyEditorChanged()
            if (editorDraft.form.quickTemplate) {
                onEditorOpened(true, editorDraft.form.templateId)
            }
        }
    }

    val draftRevision = editorDraft.observe()
    val editorBody: @Composable () -> Unit = {
        TemplateEditorContent(
            form = editorDraft.form,
            draftRevision = draftRevision,
            onFormChanged = ::notifyEditorChanged,
            onSelectTypeface = {
                state.actions.selectTypeface(editorDraft.form, ::notifyEditorChanged)
            },
            onEditHookDomains = {
                state.actions.editHookDomains(editorDraft.form, ::notifyEditorChanged)
            },
            onReset = { editorDraft.form.reset(); notifyEditorChanged() },
            onDelete = if (editorDraft.form.quickTemplate && !editorDraft.form.newTemplate) {
                { deleteConfirmationVisible = true }
            } else null,
            onSave = ::saveEditor,
            showSheetBadge = false
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        val twoPane = maxWidth >= TemplateUiTokens.TwoPaneMinWidth
        val openTargets: (String) -> Unit = { templateId ->
            if (twoPane) {
                editorKind = null
                editorTemplateId = null
                targetsTemplateId = templateId
                state.actions.openEmbeddedTargets(templateId)
            } else {
                state.actions.selectTargets(templateId)
            }
        }

        if (twoPane) {
            Row(Modifier.fillMaxSize()) {
                TemplateWorkspaceListPane(
                    state = state,
                    padding = padding,
                    onQueryChanged = onQueryChanged,
                    onEditorOpened = ::openEditor,
                    onTargetsOpened = openTargets,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when {
                        targetsTemplateId != null -> EmbeddedQuickTemplateTargets(
                            templateId = targetsTemplateId.orEmpty(),
                            onClose = {
                                targetsTemplateId = null
                                onEditorClosed()
                            }
                        )
                        editorKind != null -> editorBody()
                        else -> TemplateDetailEmptyState()
                    }
                }
            }
        } else {
            TemplateWorkspaceListPane(
                state = state,
                padding = padding,
                onQueryChanged = onQueryChanged,
                onEditorOpened = ::openEditor,
                onTargetsOpened = openTargets
            )
            if (editorKind != null) {
                ModalBottomSheet(
                    onDismissRequest = ::closeEditor,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    dragHandle = {
                        TemplateSheetDragHandle(
                            showUnsaved = editorDraft.form.isDirty(),
                            draftRevision = draftRevision
                        )
                    }
                ) {
                    TemplateEditorContent(
                        form = editorDraft.form,
                        draftRevision = draftRevision,
                        onFormChanged = ::notifyEditorChanged,
                        onSelectTypeface = {
                            state.actions.selectTypeface(editorDraft.form, ::notifyEditorChanged)
                        },
                        onEditHookDomains = {
                            state.actions.editHookDomains(editorDraft.form, ::notifyEditorChanged)
                        },
                        onReset = { editorDraft.form.reset(); notifyEditorChanged() },
                        onDelete = if (
                            editorDraft.form.quickTemplate && !editorDraft.form.newTemplate
                        ) {
                            { deleteConfirmationVisible = true }
                        } else null,
                        onSave = ::saveEditor
                    )
                }
            }
        }
    }
    if (deleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { deleteConfirmationVisible = false },
            title = { Text(stringResource(R.string.quick_template_delete_title)) },
            text = {
                Text(stringResource(
                    R.string.quick_template_delete_message,
                    editorDraft.form.nameInput
                ))
            },
            confirmButton = {
                TextButton(onClick = {
                    deleteConfirmationVisible = false
                    val result = state.actions.deleteQuickTemplate(editorDraft.form.templateId)
                    if (result.success) closeEditor()
                }) { Text(stringResource(R.string.font_library_delete_action)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmationVisible = false }) {
                    Text(stringResource(R.string.dialog_cancel_button))
                }
            }
        )
    }
}

private fun editorKindFor(kind: TemplateWorkspacePresentation.DetailKind): String? = when (kind) {
    TemplateWorkspacePresentation.DetailKind.GLOBAL_PREFILL -> EDITOR_GLOBAL
    TemplateWorkspacePresentation.DetailKind.QUICK_TEMPLATE -> EDITOR_QUICK
    else -> null
}

@Composable
private fun TemplateWorkspaceListPane(
    state: TemplateWorkspacePresentation.State,
    padding: PaddingValues,
    onQueryChanged: (String) -> Unit,
    onEditorOpened: (String, String?) -> Unit,
    onTargetsOpened: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        Spacer(Modifier.height(TemplateUiTokens.SearchTopPadding))
        TemplateWorkspaceSearchCard(query = state.query, onQueryChanged = onQueryChanged)
        Spacer(Modifier.height(TemplateUiTokens.SearchBottomPadding))
        LazyColumn(
            contentPadding = PaddingValues(
                top = TemplateUiTokens.WorkspaceTopPadding,
                bottom = padding.calculateBottomPadding() +
                    TemplateUiTokens.WorkspaceBottomReserve
            ),
            verticalArrangement = Arrangement.spacedBy(TemplateUiTokens.ListGap),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = TemplateUiTokens.WorkspaceHorizontalPadding)
        ) {
            if (!state.searching) {
                item {
                    GlobalPrefillCard(state, rememberDpisConfirmAction {
                        onEditorOpened(EDITOR_GLOBAL, null)
                    })
                }
                item {
                    TemplateHeader(state, rememberDpisConfirmAction {
                        onEditorOpened(EDITOR_QUICK, null)
                    })
                }
            }
            if (state.templates.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        Text(
                            stringResource(
                                if (state.searching) R.string.quick_template_search_empty
                                else R.string.template_workspace_quick_templates_empty
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                top = TemplateUiTokens.EmptyStateTopGap,
                                bottom = TemplateUiTokens.EmptyStatePadding,
                                end = TemplateUiTokens.EmptyStatePadding
                            )
                        )
                    }
                }
            } else {
                items(state.templates.size, key = { state.templates[it].id }) { index ->
                    val template = state.templates[index]
                    TemplateCard(
                        template = template,
                        actions = state.actions,
                        onEdit = rememberDpisConfirmAction {
                            onEditorOpened(EDITOR_QUICK, template.id)
                        },
                        onTargets = rememberDpisConfirmAction {
                            onTargetsOpened(template.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmbeddedQuickTemplateTargets(
    templateId: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val controller = remember(templateId) {
        QuickTemplateTargetsPresentationController(context.applicationContext)
    }
    var targetState by remember(templateId) {
        mutableStateOf<QuickTemplateTargetsPresentationController.State?>(null)
    }

    DisposableEffect(controller, templateId) {
        val listener = QuickTemplateTargetsPresentationController.Listener { next ->
            targetState = next
        }
        controller.addListener(listener)
        controller.load(templateId)
        onDispose {
            controller.removeListener(listener)
            controller.dispose()
        }
    }

    LaunchedEffect(targetState?.missingTemplate) {
        if (targetState?.missingTemplate == true) {
            Toast.makeText(context, R.string.quick_template_target_missing, Toast.LENGTH_SHORT)
                .show()
            onClose()
        }
    }

    QuickTemplateTargetsContent(
        state = targetState,
        onBack = onClose,
        onQueryChanged = controller::setQuery,
        onFiltersChanged = controller::setFilters,
        onSelectionChanged = controller::toggleSelection,
        onSave = {
            val result = controller.save()
            Toast.makeText(context, result.messageResId, Toast.LENGTH_SHORT).show()
            if (result.success) onClose()
        },
        onIconVisible = controller::onIconVisible
    )
}

@Composable
private fun TemplateDetailEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_template_24),
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.template_detail_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.template_detail_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun TemplateDetailEmptyStatePreview() {
    DpisTheme(darkTheme = false, dynamicColor = false) {
        TemplateDetailEmptyState()
    }
}

@Composable
private fun TemplateWorkspaceSearchCard(
    query: String,
    onQueryChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TemplateUiTokens.WorkspaceHorizontalPadding)
            .height(TemplateUiTokens.SearchCardHeight),
        shape = TemplateUiTokens.SearchCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search_24),
                contentDescription = null,
                modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChanged("") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_24),
                        contentDescription = stringResource(R.string.search_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GlobalPrefillCard(state: TemplateWorkspacePresentation.State, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TemplateUiTokens.GlobalCardShape,
        border = BorderStroke(
            TemplateUiTokens.CardBorderWidth,
            MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(TemplateUiTokens.CardPadding)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.template_workspace_global_prefill_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(R.string.template_workspace_global_prefill_subtitle),
                        modifier = Modifier.padding(top = TemplateUiTokens.TextSpacingTop),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                 IconButton(onClick = onEdit) {
                     Icon(
                         painter = painterResource(R.drawable.ic_chevron_right_24),
                         contentDescription = stringResource(
                             R.string.template_workspace_action_edit_global_prefill
                         ),
                         modifier = Modifier.size(20.dp)
                     )
                }
            }
            SummaryPills(
                state.globalPrefillSummaryParts,
                state.globalPrefillTypefaceStatus
            )
        }
    }
}

@Composable
private fun TemplateHeader(
    state: TemplateWorkspacePresentation.State,
    onCreate: () -> Unit
) {
    val sort = rememberDpisConfirmAction(state.actions::sortTemplates)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                start = TemplateUiTokens.SectionTitleInset,
                top = TemplateUiTokens.SectionTopGap,
                end = TemplateUiTokens.SectionActionInset
            ),
        horizontalArrangement = Arrangement.spacedBy(TemplateUiTokens.HeaderActionSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.template_workspace_quick_templates_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(TemplateUiTokens.HeaderActionSpacing)) {
            TemplateActionIconButton(
                iconRes = R.drawable.ic_sort_24,
                contentDescription = stringResource(R.string.quick_template_sort_action),
                enabled = state.templates.isNotEmpty(),
                onClick = sort,
                visualSize = TemplateUiTokens.HeaderActionVisualSize
            )
            TemplateActionIconButton(
                iconRes = R.drawable.ic_add_24,
                contentDescription = stringResource(R.string.quick_template_create_action),
                onClick = onCreate,
                visualSize = TemplateUiTokens.HeaderActionVisualSize
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: TemplateWorkspacePresentation.Template,
    actions: TemplateWorkspacePresentation.Actions,
    onEdit: () -> Unit,
    onTargets: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TemplateUiTokens.TemplateCardShape,
        border = BorderStroke(
            TemplateUiTokens.CardBorderWidth,
            MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(TemplateUiTokens.CardPadding)) {
            Text(
                template.name,
                style = MaterialTheme.typography.titleMedium
            )
            SummaryPills(template.summaryParts, template.typefaceStatus)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = TemplateUiTokens.CardActionsTopGap),
                horizontalArrangement = Arrangement.spacedBy(TemplateUiTokens.ActionSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TemplateActionIconButton(
                    iconRes = R.drawable.ic_checklist_rtl_24,
                    contentDescription = stringResource(R.string.template_workspace_action_select_apps),
                    onClick = onTargets,
                    visualSize = TemplateUiTokens.CardActionVisualSize,
                    style = TemplateActionButtonStyle.Plain
                )
                TemplateActionIconButton(
                    iconRes = R.drawable.ic_edit_24,
                    contentDescription = stringResource(R.string.template_workspace_action_edit_template),
                    onClick = onEdit,
                    visualSize = TemplateUiTokens.CardActionVisualSize,
                    style = TemplateActionButtonStyle.Plain
                )
                Spacer(Modifier.weight(1f))
                TemplateApplyAction(
                    onClick = rememberDpisConfirmAction { actions.applyTemplate(template.id) }
                )
            }
        }
    }
}

@Composable
private fun TemplateApplyAction(onClick: () -> Unit) {
    TemplateActionIconButton(
        iconRes = R.drawable.ic_done_all_24,
        contentDescription = stringResource(R.string.template_workspace_action_apply),
        onClick = onClick,
        visualSize = TemplateUiTokens.ApplyActionVisualSize,
        style = TemplateActionButtonStyle.Primary
    )
}

private const val EDITOR_GLOBAL = "global"
private const val EDITOR_QUICK = "quick"

@Composable
private fun SummaryPills(
    parts: List<String>,
    typefaceStatus: TemplateConfigSummaryFormatter.TypefaceStatus
) {
    if (parts.isEmpty() && !typefaceStatus.missing) {
        EmptySummary()
        return
    }
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TemplateUiTokens.SummaryTopGap),
        horizontalArrangement = Arrangement.spacedBy(TemplateUiTokens.SummaryHorizontalGap),
        verticalArrangement = Arrangement.spacedBy(TemplateUiTokens.SummaryVerticalGap)
    ) {
        parts.forEachIndexed { index, part ->
            Surface(
                modifier = Modifier.heightIn(min = TemplateUiTokens.SummaryMinHeight),
                shape = TemplateUiTokens.SummaryShape,
                color = if (index == 0) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (index == 0) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ) {
                Text(
                    part,
                    modifier = Modifier.padding(
                        horizontal = TemplateUiTokens.SummaryHorizontalPadding,
                        vertical = TemplateUiTokens.SummaryVerticalPadding
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        if (typefaceStatus.missing) {
            SummaryPill(
                text = stringResource(
                    R.string.template_workspace_missing_font,
                    typefaceStatus.typefaceId.orEmpty()
                ),
                containerColor = colorResource(R.color.dpis_warn_container),
                contentColor = colorResource(R.color.dpis_on_warn_container)
            )
        }
    }
}

@Composable
private fun SummaryPill(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = Modifier.heightIn(min = TemplateUiTokens.SummaryMinHeight),
        shape = TemplateUiTokens.SummaryShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text,
            modifier = Modifier.padding(
                horizontal = TemplateUiTokens.SummaryHorizontalPadding,
                vertical = TemplateUiTokens.SummaryVerticalPadding
            ),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptySummary() {
    val shape = TemplateUiTokens.EmptySummaryShape
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TemplateUiTokens.EmptySummaryTopGap)
            .heightIn(min = TemplateUiTokens.EmptySummaryMinHeight),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .templateDashedBorder(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    cornerRadius = 16.dp
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.template_workspace_summary_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TemplateActionIconButton(
    iconRes: Int,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    visualSize: androidx.compose.ui.unit.Dp,
    style: TemplateActionButtonStyle = TemplateActionButtonStyle.TonalOutlined
) {
    val shape = TemplateUiTokens.CircularActionShape
    val containerColor = when (style) {
        TemplateActionButtonStyle.TonalOutlined -> MaterialTheme.colorScheme.surfaceContainerHigh
        TemplateActionButtonStyle.Plain -> Color.Transparent
        TemplateActionButtonStyle.Primary -> MaterialTheme.colorScheme.primary
    }
    val contentColor = when (style) {
        TemplateActionButtonStyle.Primary -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderStroke = when (style) {
        TemplateActionButtonStyle.TonalOutlined -> BorderStroke(
            TemplateUiTokens.CardBorderWidth,
            MaterialTheme.colorScheme.outlineVariant
        )
        TemplateActionButtonStyle.Plain,
        TemplateActionButtonStyle.Primary -> null
    }
    var buttonModifier = Modifier
        .size(visualSize)
        .clip(shape)
    if (style != TemplateActionButtonStyle.Plain) {
        buttonModifier = buttonModifier.background(containerColor)
    }
    if (borderStroke != null) {
        buttonModifier = buttonModifier.border(borderStroke, shape)
    }
    Box(
        modifier = buttonModifier
            .alpha(if (enabled) 1f else TemplateUiTokens.DisabledActionAlpha)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = contentColor
        )
    }
}

private enum class TemplateActionButtonStyle {
    TonalOutlined,
    Plain,
    Primary
}

private fun Modifier.templateDashedBorder(
    color: Color,
    cornerRadius: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 1.dp
): Modifier = drawWithCache {
    val stroke = strokeWidth.toPx()
    val radius = cornerRadius.toPx()
    val effect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
    onDrawBehind {
        drawRoundRect(
            color = color,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = stroke, pathEffect = effect)
        )
    }
}
