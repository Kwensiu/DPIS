package com.dpis.module.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dpis.module.R
import com.dpis.module.ConfigEditorDestination
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation
import com.dpis.module.templates.TemplateEditorForm
import com.dpis.module.viewport.ViewportTargetType

enum class TemplateEditorSurfaceKind {
    PORTRAIT_SHEET,
    LANDSCAPE_DETAIL
}

/**
 * Shared editor surface for the portrait sheet and landscape detail pane.
 *
 * The field body and callbacks are identical. Only the outer presentation contract differs:
 * portrait owns a bottom sheet chrome/inset, while landscape reserves the status-bar gap inline.
 */
@Composable
fun TemplateEditorSurface(
    form: TemplateEditorForm,
    surface: TemplateEditorSurfaceKind,
    draftRevision: Int = 0,
    topSafePadding: androidx.compose.ui.unit.Dp = 0.dp,
    bottomSafePadding: androidx.compose.ui.unit.Dp = 0.dp,
    onFormChanged: () -> Unit,
    onSelectTypeface: () -> Unit,
    onEditHookDomains: () -> Unit,
    onReset: () -> Unit,
    onDismissRequest: () -> Unit = {},
    onDelete: (() -> Unit)?,
    onSave: () -> Unit,
    destination: ConfigEditorDestination = ConfigEditorDestination.MAIN,
    hookContent: (@Composable () -> Unit)? = null,
    typefaceContent: (@Composable () -> Unit)? = null
) {
    val mainEditor: @Composable () -> Unit = {
        TemplateEditorContent(
            form = form,
            draftRevision = draftRevision,
            onFormChanged = onFormChanged,
            onSelectTypeface = onSelectTypeface,
            onEditHookDomains = onEditHookDomains,
            onReset = onReset,
            onDelete = onDelete,
            onSave = onSave,
            showSheetBadge = surface == TemplateEditorSurfaceKind.PORTRAIT_SHEET,
            extraTopPadding = if (surface == TemplateEditorSurfaceKind.LANDSCAPE_DETAIL) {
                topSafePadding
            } else {
                0.dp
            },
            extraBottomPadding = if (surface == TemplateEditorSurfaceKind.LANDSCAPE_DETAIL) {
                bottomSafePadding
            } else {
                0.dp
            }
        )
    }
    val editor: @Composable () -> Unit = {
        if (hookContent == null && typefaceContent == null) {
            mainEditor()
        } else {
            ConfigEditorAnimatedContent(
                destination = destination,
                clipContentToAnimatedBounds =
                    surface == TemplateEditorSurfaceKind.LANDSCAPE_DETAIL,
                mainContent = mainEditor,
                hookContent = hookContent ?: mainEditor,
                typefaceContent = typefaceContent
            )
        }
    }

    if (surface == TemplateEditorSurfaceKind.PORTRAIT_SHEET) {
        DpisEditorBottomSheet(
            onDismissRequest = onDismissRequest,
            topChrome = { DpisSheetVisualChrome(showUnsaved = form.isDirty()) },
            // The editor body owns the shared bottom reserve. Do not add a second navigation inset.
            contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) }
        ) {
            editor()
        }
    } else {
        editor()
    }
}

/**
 * Shared editor body for the portrait sheet and the landscape detail pane.
 *
 * The caller owns [form]'s cross-surface lifetime. Every text mutation is written through to the
 * same draft before requesting recomposition, so a form never depends on View widget state.
 */
@Composable
fun TemplateEditorContent(
    form: TemplateEditorForm,
    draftRevision: Int = 0,
    onFormChanged: () -> Unit,
    onSelectTypeface: () -> Unit,
    onEditHookDomains: () -> Unit,
    onReset: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    showSheetBadge: Boolean = true,
    extraTopPadding: androidx.compose.ui.unit.Dp = 0.dp,
    extraBottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    // The revision is intentionally passed as a parameter. The form is a mutable Java draft and
    // its stable object identity must not allow Compose to skip the updated editor subtree.
    @Suppress("UNUSED_VARIABLE")
    val observedDraftRevision = draftRevision
    val inputErrorMessage = stringResource(R.string.status_save_invalid)
    val nameErrorMessage = stringResource(R.string.quick_template_name_required)
    val nameError = if (form.isNameValid()) null else nameErrorMessage
    val viewportError = if (form.isViewportInputValid()) null else inputErrorMessage
    val fontError = if (form.isFontInputValid()) null else inputErrorMessage
    val hookDomainsButtonText = FontHookDomainPresentation
        .forRecommendedTemplateRaw(form.fontHookDomainsRaw)
        .buttonText(LocalContext.current)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(AppConfigSheetUiTokens.ContentPadding)
            .padding(top = extraTopPadding)
            .padding(bottom = edgeToEdgeContentBottomPadding(extraBottomPadding))
    ) {
        TemplateEditorSheetHeader(
            form = form,
            showInlineBadge = !showSheetBadge,
            onReset = onReset,
            onDelete = onDelete
        )

        if (form.quickTemplate) {
            Spacer(Modifier.height(AppConfigSheetUiTokens.HeaderToFirstInputGap))
            Column(Modifier.fillMaxWidth()) {
                DpisCompactEditorTextField(
                    value = form.nameInput,
                    onValueChange = { form.nameInput = it; onFormChanged() },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.quick_template_name_hint),
                    isError = nameError != null,
                    trailingIcon = if (form.nameInput.isNotEmpty()) {
                        {
                            IconButton(onClick = {
                                form.nameInput = ""
                                onFormChanged()
                            }) {
                                Icon(
                                    painterResource(R.drawable.ic_close_24),
                                    stringResource(R.string.search_clear)
                                )
                            }
                        }
                    } else null,
                )
                nameError?.let { TemplateEditorErrorMessage(it) }
            }
        }

        Spacer(
            Modifier.height(
                if (form.quickTemplate) {
                    TemplateUiTokens.EditorNameToFirstInputGap
                } else {
                    AppConfigSheetUiTokens.HeaderToFirstInputGap
                }
            )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppConfigSheetUiTokens.FieldRowHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = AppConfigSheetUiTokens.FieldTopInset)
            ) {
                DpisCompactEditorTextField(
                    value = form.viewportInput,
                    onValueChange = {
                        form.viewportInput = it
                        form.updateActiveViewportDraft()
                        onFormChanged()
                    },
                    // The mode track is the row's 48dp alignment anchor. DecorationBox's label
                    // can extend outside that outline, so move only the input surface down.
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(
                        if (ViewportTargetType.ABSOLUTE_DP == form.viewportMode) {
                            R.string.dialog_viewport_hint_absolute
                        } else {
                            R.string.dialog_viewport_hint_scale
                        }
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = viewportError != null,
                    trailingIcon = if (form.viewportInput.isNotEmpty()) {
                        {
                            DpisEditorClearButton {
                                form.viewportInput = ""
                                form.updateActiveViewportDraft()
                                onFormChanged()
                            }
                        }
                    } else null,
                )
                viewportError?.let { TemplateEditorErrorMessage(it) }
            }
            DpisModeSelector(
                selectedFirst = form.viewportMode == ViewportTargetType.RELATIVE_SCALE,
                firstLabel = stringResource(R.string.dialog_viewport_mode_system),
                secondLabel = stringResource(R.string.dialog_viewport_mode_compat),
                onFirstSelected = { form.switchViewportMode(ViewportTargetType.RELATIVE_SCALE); onFormChanged() },
                onSecondSelected = { form.switchViewportMode(ViewportTargetType.ABSOLUTE_DP); onFormChanged() },
                modifier = Modifier.padding(top = AppConfigSheetUiTokens.FieldTopInset),
                labelStyle = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(Modifier.height(AppConfigSheetUiTokens.InputRowLayoutGap))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppConfigSheetUiTokens.FieldRowHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = AppConfigSheetUiTokens.FieldTopInset)
            ) {
                DpisCompactEditorTextField(
                    value = form.fontInput,
                    onValueChange = { form.fontInput = it; onFormChanged() },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.dialog_font_scale_hint),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = fontError != null,
                    trailingIcon = if (form.fontInput.isNotEmpty()) {
                        {
                            DpisEditorClearButton {
                                form.fontInput = ""
                                onFormChanged()
                            }
                        }
                    } else null,
                )
                fontError?.let { TemplateEditorErrorMessage(it) }
            }
            DpisModeSelector(
                selectedFirst = form.fontMode == FontApplyMode.SYSTEM_EMULATION,
                firstLabel = stringResource(R.string.dialog_font_mode_system),
                secondLabel = stringResource(R.string.dialog_font_mode_compat),
                onFirstSelected = { form.fontMode = FontApplyMode.SYSTEM_EMULATION; onFormChanged() },
                onSecondSelected = { form.fontMode = FontApplyMode.FIELD_REWRITE; onFormChanged() },
                modifier = Modifier.padding(top = AppConfigSheetUiTokens.FieldTopInset),
                labelStyle = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(Modifier.height(AppConfigSheetUiTokens.ControlGroupGap))
        DpisEditorTypefaceHookRow(
            primary = {
                OutlinedButton(
                    onClick = onSelectTypeface,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = AppConfigSheetUiTokens.FieldAndActionShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text(
                        stringResource(
                            R.string.dialog_typeface_selector_value,
                            form.selectedTypefaceId
                                ?: stringResource(R.string.dialog_typeface_default)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            secondary = {
                OutlinedButton(
                    onClick = onEditHookDomains,
                    modifier = Modifier.width(AppConfigSheetUiTokens.SecondaryControlWidth)
                        .heightIn(min = AppConfigSheetUiTokens.ActionHeight),
                    shape = AppConfigSheetUiTokens.FieldAndActionShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(
                        hookDomainsButtonText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )

        Spacer(Modifier.height(AppConfigSheetUiTokens.ControlGroupGap))
        Button(
            onClick = onSave,
            enabled = form.isValid(),
            modifier = Modifier
                .fillMaxWidth()
                .height(AppConfigSheetUiTokens.ActionHeight),
            shape = AppConfigSheetUiTokens.ActionShape,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
            Text(stringResource(R.string.status_save_button))
        }
    }
}

/** Error text sits outside the fixed outline so it can wrap without clipping the input. */
@Composable
private fun TemplateEditorErrorMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun TemplateEditorSheetHeader(
    form: TemplateEditorForm,
    showInlineBadge: Boolean,
    onReset: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val resetAction = rememberDpisConfirmAction(onReset)
    val deleteAction = onDelete?.let { rememberDpisConfirmAction(it) }
    val titleRes = when {
        !form.quickTemplate -> R.string.template_workspace_global_prefill_title
        form.newTemplate -> R.string.quick_template_edit_page_title_new
        else -> R.string.quick_template_edit_page_title_edit
    }
    val subtitleRes = if (form.quickTemplate) {
        R.string.quick_template_edit_sheet_subtitle
    } else {
        R.string.template_workspace_global_prefill_subtitle
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppConfigSheetUiTokens.FieldRowHeight),
        horizontalArrangement = Arrangement.spacedBy(TemplateUiTokens.HeaderActionSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (showInlineBadge && form.isDirty()) {
                    Spacer(Modifier.width(8.dp))
                    UnsavedBadge()
                }
            }
            Text(
                stringResource(subtitleRes),
                modifier = Modifier.padding(top = 1.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        EditorHeaderIconButton(
            iconRes = R.drawable.ic_reset_settings_24,
            contentDescription = stringResource(R.string.template_workspace_action_reset),
            onClick = resetAction,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (deleteAction != null) {
            EditorHeaderIconButton(
                iconRes = R.drawable.ic_delete_24,
                contentDescription = stringResource(R.string.font_library_delete_action),
                onClick = deleteAction,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                borderColor = MaterialTheme.colorScheme.error,
                iconTint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun EditorHeaderIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .size(TemplateUiTokens.HeaderActionVisualSize)
            .clip(CircleShape)
            .background(containerColor)
            .border(BorderStroke(1.dp, borderColor), CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = iconTint
        )
    }
}

@Composable
private fun UnsavedBadge() {
    Surface(
        modifier = Modifier
            .heightIn(min = TemplateUiTokens.UnsavedBadgeMinHeight)
            .offset(y = (-2).dp),
        shape = TemplateUiTokens.UnsavedBadgeShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Text(
            stringResource(R.string.sheet_unsaved_badge),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
