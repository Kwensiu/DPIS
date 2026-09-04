package com.dpis.module.templates.presentation

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dpis.module.ConfigEditorDestination
import com.dpis.module.R
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.templates.QuickTemplateSortContent
import com.dpis.module.templates.QuickTemplateStore
import com.dpis.module.templates.QuickTemplateTargetsPresentationController
import com.dpis.module.templates.TemplateEditorForm
import com.dpis.module.templates.TemplateWorkspacePresentation
import com.dpis.module.ui.compose.PageScrollPositionStore
import com.dpis.module.ui.compose.rememberClickAction
import com.dpis.module.ui.compose.WorkspaceTwoPaneMinWidth
import com.dpis.module.ui.compose.AppTypefacePickerPage
import com.dpis.module.ui.compose.ComposeDesignSystem
import com.dpis.module.ui.compose.HookChainEditorPage
import com.dpis.module.ui.dialog.ConfirmAlertDialog
import com.dpis.module.ui.dialog.ModalDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TemplateWorkspaceContent(
    state: TemplateWorkspacePresentation.State,
    padding: PaddingValues,
    onQueryChanged: (String) -> Unit,
    onEditorOpened: (quickTemplate: Boolean, templateId: String?) -> Unit = { _, _ -> },
    onEditorChanged: (TemplateEditorForm) -> Unit = {},
    onEditorDestinationChanged: (ConfigEditorDestination) -> Unit = {},
    onEditorClosed: () -> Unit = {},
    editorOverlayHost: TemplateEditorOverlayHost? = null,
    scrollStore: PageScrollPositionStore,
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
    var targetSessionDirty by rememberSaveable { mutableStateOf(false) }
    var pendingTargetTemplateId by rememberSaveable { mutableStateOf<String?>(null) }
    var targetSwitchDialogVisible by rememberSaveable { mutableStateOf(false) }
    var targetSaveRequest by rememberSaveable { mutableIntStateOf(0) }
    var deleteConfirmationVisible by rememberSaveable { mutableStateOf(false) }
    var sortDialogVisible by rememberSaveable { mutableStateOf(false) }
    val editorDestination = state.editorDestination
    val topSafePadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
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
        onEditorDestinationChanged(ConfigEditorDestination.MAIN)
        onEditorOpened(kind == EDITOR_QUICK, templateId)
    }

    val selectedTemplate = state.templates.firstOrNull { it.id == editorTemplateId }
    val editorKey = "${editorKind.orEmpty()}:${editorTemplateId.orEmpty()}"
    val editorDraft =
        rememberTemplateEditorDraftState(editorKey) {
            when (editorKind) {
                EDITOR_GLOBAL -> TemplateEditorForm.global(state.globalPrefill).also {
                    it.applyDraft(state.globalPrefillDraft)
                }

                EDITOR_QUICK -> {
                    TemplateEditorForm.quick(
                        selectedTemplate?.let {
                            QuickTemplateStore.QuickTemplate(
                                it.id, it.name, 0L, linkedSetOf(), it.configValue
                            )
                        },
                        ""
                    ).also { it.applyDraft(state.quickTemplateDraft) }
                }

                else -> TemplateEditorForm.global(state.globalPrefill).also {
                    it.applyDraft(state.globalPrefillDraft)
                }
            }
        }

    fun notifyEditorChanged() {
        editorDraft.changed()
        onEditorChanged(editorDraft.form)
    }

    fun finishEditorClose() {
        if (editorKind == null) return
        editorKind = null
        editorTemplateId = null
        deleteConfirmationVisible = false
        onEditorClosed()
    }

    fun closeEditor() {
        if (editorKind == null) return
        finishEditorClose()
    }

    fun saveEditor() {
        val createdNewTemplate = editorDraft.form.quickTemplate && editorDraft.form.newTemplate
        val result = if (editorDraft.form.quickTemplate) {
            state.actions.saveQuickTemplate(editorDraft.form)
        } else {
            state.actions.saveGlobalPrefill(editorDraft.form)
        }
        if (result.success) {
            editorDraft.form.markSaved(result.templateId)
            editorTemplateId = editorDraft.form.templateId
            notifyEditorChanged()
            if (createdNewTemplate) {
                closeEditor()
                return
            }
            if (editorDraft.form.quickTemplate) {
                onEditorOpened(true, editorDraft.form.templateId)
            }
        }
    }

    val draftRevision = editorDraft.observe()
    @Composable
    fun hookChainPage(
        bottomPadding: Dp,
        modifier: Modifier = Modifier,
        onBack: (() -> Unit)? = null,
    ) {
        HookChainEditorPage(
            destination = editorDestination,
            rawDomains = editorDraft.form.fontHookDomainsRaw,
            fontDomainsResetRequested = editorDraft.form.fontHookDomainsRaw == null,
            automaticDomains = FontHookDomainRegistry.automaticCustomizableDomains(),
            fontDomainsEditable = editorDraft.form.fontMode ==
                    FontApplyMode.FIELD_REWRITE,
            viewportApplyMode = editorDraft.form.viewportApplyMode,
            onHookChainChanged = { raw, reset, mode, _ ->
                editorDraft.form.fontHookDomainsRaw = if (reset) null else raw
                editorDraft.form.viewportApplyMode = mode
                notifyEditorChanged()
            },
            onDestinationChanged = onEditorDestinationChanged,
            onBack = onBack ?: { onEditorDestinationChanged(editorDestination.backDestination()) },
            modifier = modifier,
            bottomPadding = bottomPadding
        )
    }

    @Composable
    fun typefacePage(
        modifier: Modifier = Modifier,
        onBack: (() -> Unit)? = null,
    ) {
        AppTypefacePickerPage(
            selectedTypefaceId = editorDraft.form.selectedTypefaceId,
            onTypefaceSelected = {
                editorDraft.form.selectedTypefaceId = it
                notifyEditorChanged()
            },
            onBack = onBack ?: {
                onEditorDestinationChanged(editorDestination.backDestination())
            },
            modifier = modifier
        )
    }

    val editorBody: @Composable () -> Unit = {
        TemplateEditorSurface(
            form = editorDraft.form,
            draftRevision = draftRevision,
            topSafePadding = topSafePadding,
            bottomSafePadding = padding.calculateBottomPadding(),
            onFormChanged = ::notifyEditorChanged,
            onSelectTypeface = {
                onEditorDestinationChanged(ConfigEditorDestination.TYPEFACE)
            },
            onEditHookDomains = {
                onEditorDestinationChanged(ConfigEditorDestination.HOOK_CHAIN_INTERFACE)
            },
            onReset = { editorDraft.form.reset(); notifyEditorChanged() },
            onDelete = if (editorDraft.form.quickTemplate && !editorDraft.form.newTemplate) {
                { deleteConfirmationVisible = true }
            } else null,
            onSave = ::saveEditor,
            destination = editorDestination,
            hookContent = {
                hookChainPage(
                    padding.calculateBottomPadding(),
                    Modifier.padding(top = topSafePadding)
                )
            },
            typefaceContent = {
                typefacePage(Modifier.padding(top = topSafePadding))
            }
        )
    }
    val editorSheetBody: @Composable ColumnScope.((Dp) -> Unit, Boolean, () -> Unit) -> Unit = {
            onContentBottomMeasured, _, onReturnFromChild ->
        TemplateEditorSurface(
            form = editorDraft.form,
            draftRevision = draftRevision,
            onFormChanged = ::notifyEditorChanged,
            onSelectTypeface = {
                onEditorDestinationChanged(ConfigEditorDestination.TYPEFACE)
            },
            onEditHookDomains = {
                onEditorDestinationChanged(ConfigEditorDestination.HOOK_CHAIN_INTERFACE)
            },
            onReset = { editorDraft.form.reset(); notifyEditorChanged() },
            onDelete = if (editorDraft.form.quickTemplate && !editorDraft.form.newTemplate) {
                { deleteConfirmationVisible = true }
            } else null,
            onSave = ::saveEditor,
            onContentBottomMeasured = onContentBottomMeasured,
            // Portrait already exposes this state in the fixed sheet chrome. Keeping a second
            // badge in the wrapped title changes its measured width and can destabilize the
            // collapsed editor layout for long localized titles.
            showInlineUnsavedBadge = false,
            animateDestinationSize = false,
            clipDestinationContent = false,
            destination = editorDestination,
            hookContent = {
                hookChainPage(
                    padding.calculateBottomPadding(),
                    onBack = onReturnFromChild,
                )
            },
            typefaceContent = {
                typefacePage(onBack = onReturnFromChild)
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Keep the landscape list/detail contract independent of user interface scaling. The
        // reported Dp width shrinks with density, but the physical landscape window remains split.
        val twoPane = isLandscape
        val openTargets: (String) -> Unit = { templateId ->
            if (twoPane) {
                if (targetsTemplateId != null && targetsTemplateId != templateId && targetSessionDirty) {
                    pendingTargetTemplateId = templateId
                    targetSwitchDialogVisible = true
                } else {
                    editorKind = null
                    editorTemplateId = null
                    targetsTemplateId = templateId
                    state.actions.openEmbeddedTargets(templateId)
                }
            } else {
                state.actions.selectTargets(templateId)
            }
        }

        if (twoPane) {
            Row(
                Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            ) {
                TemplateWorkspaceListPane(
                    state = state,
                    padding = padding,
                    onQueryChanged = onQueryChanged,
                    onEditorOpened = ::openEditor,
                    onSortRequested = { sortDialogVisible = true },
                    onTargetsOpened = openTargets,
                    scrollStore = scrollStore,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.Transparent)
                ) {
                    when {
                        targetsTemplateId != null -> Box(
                            Modifier.padding(bottom = padding.calculateBottomPadding())
                        ) {
                            EmbeddedQuickTemplateTargets(
                                templateId = targetsTemplateId.orEmpty(),
                                onClose = {
                                    targetSessionDirty = false
                                    val next = pendingTargetTemplateId
                                    pendingTargetTemplateId = null
                                    targetsTemplateId = next
                                    if (next != null) {
                                        state.actions.openEmbeddedTargets(next)
                                    } else {
                                        onEditorClosed()
                                    }
                                },
                                onUnsavedChanged = { targetSessionDirty = it },
                                saveRequest = targetSaveRequest
                            )
                        }

                        editorKind != null -> editorBody()
                        else -> TemplateDetailEmptyState(
                            modifier = Modifier.padding(top = topSafePadding)
                        )
                    }
                }
            }
        } else {
            TemplateWorkspaceListPane(
                state = state,
                padding = padding,
                onQueryChanged = onQueryChanged,
                onEditorOpened = ::openEditor,
                onSortRequested = { sortDialogVisible = true },
                onTargetsOpened = openTargets,
                scrollStore = scrollStore,
                modifier = Modifier
            )
        }
    }
    val templateOverlayContent: TemplateEditorOverlay? = remember(
        editorKind,
        editorTemplateId,
        editorDestination,
        draftRevision,
        isLandscape,
    ) {
        if (!isLandscape && editorKind != null) {
            {
                TemplateEditorSheet(
                    destination = editorDestination,
                    onDismissRequest = ::closeEditor,
                    onReturnToMain = {
                        onEditorDestinationChanged(editorDestination.backDestination())
                    },
                    topChrome = {
                        TemplateEditorSheetChrome(showUnsaved = editorDraft.form.isDirty)
                    },
                    content = editorSheetBody,
                )
            }
        } else {
            null
        }
    }
    SideEffect {
        editorOverlayHost?.content?.value = templateOverlayContent
    }
    DisposableEffect(editorOverlayHost) {
        onDispose {
            editorOverlayHost?.clear(templateOverlayContent)
        }
    }
    if (sortDialogVisible) {
        ModalDialog(onDismissRequest = { sortDialogVisible = false }) {
            QuickTemplateSortContent(
                initialItems = state.sortItems,
                onOrderChanged = state.actions::reorderTemplates,
                onDone = { sortDialogVisible = false },
            )
        }
    }
    if (deleteConfirmationVisible) {
        ConfirmAlertDialog(
            onDismissRequest = { deleteConfirmationVisible = false },
            title = stringResource(R.string.quick_template_delete_title),
            message = stringResource(
                R.string.quick_template_delete_message,
                editorDraft.form.nameInput
            ),
            cancelLabel = stringResource(R.string.dialog_cancel_button),
            confirmLabel = stringResource(R.string.font_library_delete_action),
            onConfirm = {
                deleteConfirmationVisible = false
                val result =
                    state.actions.deleteQuickTemplate(editorDraft.form.templateId.orEmpty())
                if (result.success) closeEditor()
            }
        )
    }
    if (targetSwitchDialogVisible) {
        ModalDialog(onDismissRequest = { targetSwitchDialogVisible = false }) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    stringResource(R.string.quick_template_targets_unsaved_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    stringResource(R.string.quick_template_targets_unsaved_message),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = rememberClickAction {
                        val next = pendingTargetTemplateId
                        pendingTargetTemplateId = null
                        targetSwitchDialogVisible = false
                        targetSessionDirty = false
                        targetsTemplateId = next
                        if (next != null) state.actions.openEmbeddedTargets(next)
                    }) {
                        Text(stringResource(R.string.quick_template_targets_discard_changes))
                    }
                    TextButton(onClick = rememberClickAction {
                        targetSwitchDialogVisible = false
                        targetSaveRequest++
                    }) {
                        Text(stringResource(R.string.quick_template_targets_save_and_switch))
                    }
                }
            }
        }
    }
}

private fun editorKindFor(kind: TemplateWorkspacePresentation.DetailKind): String? = when (kind) {
    TemplateWorkspacePresentation.DetailKind.GLOBAL_PREFILL -> EDITOR_GLOBAL
    TemplateWorkspacePresentation.DetailKind.QUICK_TEMPLATE -> EDITOR_QUICK
    else -> null
}

private const val EDITOR_GLOBAL = TemplateEditorKinds.GLOBAL
private const val EDITOR_QUICK = TemplateEditorKinds.QUICK

@Composable
private fun EmbeddedQuickTemplateTargets(
    templateId: String,
    onClose: () -> Unit,
    onUnsavedChanged: (Boolean) -> Unit,
    saveRequest: Int,
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
    LaunchedEffect(targetState?.hasUnsavedChanges) {
        onUnsavedChanged(targetState?.hasUnsavedChanges == true)
    }
    LaunchedEffect(saveRequest) {
        if (saveRequest <= 0) return@LaunchedEffect
        val result = controller.save()
        Toast.makeText(context, result.messageResId, Toast.LENGTH_SHORT).show()
        if (result.success) onClose()
    }

    QuickTemplateTargetsContent(
        state = targetState,
        onBack = onClose,
        onQueryChanged = controller::setQuery,
        onFiltersChanged = controller::setFilters,
        onSelectionChanged = controller::toggleSelection,
        onSaveAndExit = {
            val result = controller.save()
            Toast.makeText(context, result.messageResId, Toast.LENGTH_SHORT).show()
            if (result.success) onClose()
            result.success
        },
        showBackButton = false,
    )
}

@Composable
private fun TemplateDetailEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_template_24),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.template_detail_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.template_detail_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun TemplateDetailEmptyStatePreview() {
    ComposeDesignSystem(
        darkTheme = false,
        dynamicColor = false
    ) {
        TemplateDetailEmptyState()
    }
}
