package com.dpis.module.templates.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dpis.module.ConfigEditorDestination
import com.dpis.module.R
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation
import com.dpis.module.templates.TemplateEditorForm
import com.dpis.module.ui.compose.AppConfigSheetUiTokens
import com.dpis.module.ui.compose.AppIdentityMarqueeText
import com.dpis.module.ui.compose.LocalSpacing
import com.dpis.module.ui.compose.ConfigEditorAnimatedContent
import com.dpis.module.ui.compose.CompactEditorTextField
import com.dpis.module.ui.compose.EditorClearButton
import com.dpis.module.ui.compose.EditorTypefaceHookRow
import com.dpis.module.ui.compose.EditorValueModeRow
import com.dpis.module.ui.compose.edgeToEdgeContentBottomPadding
import com.dpis.module.ui.compose.rememberClickAction
import com.dpis.module.ui.compose.FeedbackButton
import com.dpis.module.ui.compose.FeedbackOutlinedButton
import com.dpis.module.ui.compose.dpisClickable
import com.dpis.module.ui.compose.rememberEditorControlHeight
import com.dpis.module.ui.compose.clearTextInputFocusOutside
import com.dpis.module.ui.compose.rememberTextInputFocusBoundary
import com.dpis.module.ui.compose.LocalTextInputFocusBoundary
import com.dpis.module.ui.compose.reportTextInputFocusBounds
import com.dpis.module.viewport.ViewportTargetType

/** Template editor content for the landscape detail pane. */
@Composable
fun TemplateEditorSurface(
    form: TemplateEditorForm,
    draftRevision: Int = 0,
    topSafePadding: Dp = 0.dp,
    bottomSafePadding: Dp = 0.dp,
    onFormChanged: () -> Unit,
    onSelectTypeface: () -> Unit,
    onEditHookDomains: () -> Unit,
    onReset: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: () -> Unit,
    onContentBottomMeasured: ((Dp) -> Unit)? = null,
    showInlineUnsavedBadge: Boolean = true,
    animateDestinationSize: Boolean = true,
    clipDestinationContent: Boolean = true,
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
            onContentBottomMeasured = onContentBottomMeasured,
            showInlineUnsavedBadge = showInlineUnsavedBadge,
            extraTopPadding = topSafePadding,
            extraBottomPadding = bottomSafePadding,
        )
    }
    val editor: @Composable () -> Unit = {
        if (hookContent == null && typefaceContent == null) {
            mainEditor()
        } else {
            ConfigEditorAnimatedContent(
                destination = destination,
                animateSize = animateDestinationSize,
                clipContentToAnimatedBounds = clipDestinationContent,
                mainContent = mainEditor,
                hookContent = hookContent ?: mainEditor,
                typefaceContent = typefaceContent
            )
        }
    }

    editor()
}

/**
 * Shared editor body for template editing.
 *
 * The caller owns [form]'s cross-surface lifetime. Every text mutation is written through to the
 * same draft before requesting recomposition, so a form never depends on View widget state.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun TemplateEditorContent(
    form: TemplateEditorForm,
    onFormChanged: () -> Unit,
    onSelectTypeface: () -> Unit,
    onEditHookDomains: () -> Unit,
    onReset: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    draftRevision: Int = 0,
    onContentBottomMeasured: ((Dp) -> Unit)? = null,
    showInlineUnsavedBadge: Boolean = true,
    extraTopPadding: Dp = 0.dp,
    extraBottomPadding: Dp = 0.dp
) {
    // The revision is intentionally part of the parameter list. The form is a mutable Java draft
    // and its stable object identity must not allow Compose to skip the updated editor subtree.
    // Invalid values retain their field outline and save-disabled state, but never insert a
    // transient validation row. Changing the form's measured height while the sheet is moving
    // makes its partial anchor unstable.
    val nameHasError = !form.isNameValid()
    val viewportHasError = !form.isViewportInputValid()
    val fontHasError = !form.isFontInputValid()
    val context = LocalContext.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val inputFocusBoundary = LocalTextInputFocusBoundary.current ?: rememberTextInputFocusBoundary()
    val hookDomainsButtonText = FontHookDomainPresentation
        .forAutomaticDomainsRaw(form.fontHookDomainsRaw)
        .buttonText(context)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(AppConfigSheetUiTokens.ContentPadding)
            .padding(top = extraTopPadding)
            .padding(bottom = edgeToEdgeContentBottomPadding(
                extraBottomPadding
            )
            )
    ) {
        TemplateEditorHeader(
            form = form,
            onReset = onReset,
            onDelete = onDelete,
            showInlineUnsavedBadge = showInlineUnsavedBadge,
        )

        if (form.quickTemplate) {
            Spacer(
                Modifier.height(
                    AppConfigSheetUiTokens.HeaderToFirstInputGap))
            Column(Modifier.fillMaxWidth()) {
                CompactEditorTextField(
                    value = form.nameInput,
                    onValueChange = { form.nameInput = it; onFormChanged() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .reportTextInputFocusBounds(inputFocusBoundary, "template-name"),
                    label = stringResource(R.string.quick_template_name_hint),
                    isError = nameHasError,
                    trailingIcon = if (form.nameInput.isNotEmpty()) {
                        {
                            IconButton(onClick = rememberClickAction {
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
        EditorValueModeRow(
            input = { modifier, onFocusChanged ->
                CompactEditorTextField(
                    value = form.viewportInput,
                    onValueChange = {
                        form.viewportInput = it
                        form.updateActiveViewportDraft()
                        onFormChanged()
                    },
                    // The mode track is the row's 48dp alignment anchor. DecorationBox's label
                    // can extend outside that outline, so move only the input surface down.
                    modifier = modifier
                        .reportTextInputFocusBounds(inputFocusBoundary, "template-viewport"),
                    label = stringResource(
                        if (ViewportTargetType.ABSOLUTE_DP == form.viewportMode) {
                            R.string.dialog_viewport_hint_absolute
                        } else {
                            R.string.dialog_viewport_hint_scale
                        }
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = viewportHasError,
                    onFocusChanged = onFocusChanged,
                    trailingIcon = {
                        EditorClearButton(visible = form.viewportInput.isNotEmpty(), onClear = {
                            form.viewportInput = ""
                            form.updateActiveViewportDraft()
                            onFormChanged()
                        })
                    },
                )
            },
            first = stringResource(R.string.dialog_viewport_mode_system),
            second = stringResource(R.string.dialog_viewport_mode_compat),
            firstSelected = form.viewportMode == ViewportTargetType.RELATIVE_SCALE,
            onFirst = { form.switchViewportMode(ViewportTargetType.RELATIVE_SCALE); onFormChanged() },
            onSecond = { form.switchViewportMode(ViewportTargetType.ABSOLUTE_DP); onFormChanged() },
            labelStyle = MaterialTheme.typography.labelSmall,
        )

        Spacer(Modifier.height(AppConfigSheetUiTokens.InputRowLayoutGap))
        EditorValueModeRow(
            input = { modifier, onFocusChanged ->
                CompactEditorTextField(
                    value = form.fontInput,
                    onValueChange = { form.fontInput = it; onFormChanged() },
                    modifier = modifier
                        .reportTextInputFocusBounds(inputFocusBoundary, "template-font"),
                    label = stringResource(R.string.dialog_font_scale_hint),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = fontHasError,
                    onFocusChanged = onFocusChanged,
                    trailingIcon = {
                        EditorClearButton(visible = form.fontInput.isNotEmpty(), onClear = {
                            form.fontInput = ""
                            onFormChanged()
                        })
                    },
                )
            },
            first = stringResource(R.string.dialog_font_mode_system),
            second = stringResource(R.string.dialog_font_mode_compat),
            firstSelected = form.fontMode == FontApplyMode.SYSTEM_EMULATION,
            onFirst = { form.fontMode = FontApplyMode.SYSTEM_EMULATION; onFormChanged() },
            onSecond = { form.fontMode = FontApplyMode.FIELD_REWRITE; onFormChanged() },
            labelStyle = MaterialTheme.typography.labelMedium,
        )

        Spacer(Modifier.height(AppConfigSheetUiTokens.ControlGroupGap))
        EditorTypefaceHookRow(
            primary = { modifier ->
                FeedbackOutlinedButton(
                    onClick = onSelectTypeface,
                    modifier = modifier.height(rememberEditorControlHeight()),
                    shape = AppConfigSheetUiTokens.FieldAndActionShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = LocalSpacing.current.none, vertical = LocalSpacing.current.none)
                ) {
                    AppIdentityMarqueeText(
                        text = stringResource(
                            R.string.dialog_typeface_selector_value,
                            form.selectedTypefaceId
                                ?: stringResource(R.string.dialog_typeface_default)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelLarge,
                        centerWhenStatic = true,
                        textHorizontalInset = LocalSpacing.current.lg,
                        edgeFadeColor = MaterialTheme.colorScheme.surfaceContainer,
                    )
                }
            },
            secondary = { modifier ->
                FeedbackOutlinedButton(
                    onClick = onEditHookDomains,
                    modifier = modifier.height(rememberEditorControlHeight()),
                    shape = AppConfigSheetUiTokens.FieldAndActionShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = LocalSpacing.current.none, vertical = LocalSpacing.current.none)
                ) {
                    AppIdentityMarqueeText(
                        text = hookDomainsButtonText,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelLarge,
                        centerWhenStatic = true,
                        textHorizontalInset = LocalSpacing.current.md,
                        edgeFadeColor = MaterialTheme.colorScheme.surfaceContainer,
                    )
                }
            }
        )

        Spacer(Modifier.height(AppConfigSheetUiTokens.ControlGroupGap))
        FeedbackButton(
            onClick = onSave,
            enabled = form.isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(rememberEditorControlHeight())
                .onGloballyPositioned { coordinates ->
                    onContentBottomMeasured?.invoke(with(density) {
                        (coordinates.positionInParent().y + coordinates.size.height).toDp()
                    })
                },
            shape = AppConfigSheetUiTokens.ActionShape,
            contentPadding = PaddingValues(horizontal = LocalSpacing.current.lg, vertical = LocalSpacing.current.none)
        ) {
            Text(stringResource(R.string.status_save_button))
        }
    }
}

@Composable
private fun TemplateEditorHeader(
    form: TemplateEditorForm,
    onReset: () -> Unit,
    onDelete: (() -> Unit)?,
    showInlineUnsavedBadge: Boolean,
) {
    val resetAction = rememberClickAction(onReset)
    val deleteAction = onDelete?.let {
        rememberClickAction(
            it
        )
    }
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
            // Long localized titles/subtitles must be allowed to wrap. The old fixed row
            // Keep localized titles readable when they wrap to multiple lines.
            .heightIn(min = AppConfigSheetUiTokens.FieldRowHeight),
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
                if (showInlineUnsavedBadge) {
                    Spacer(Modifier.width(8.dp))
                    UnsavedBadge(visible = form.isDirty)
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
    containerColor: Color,
    borderColor: Color,
    iconTint: Color
) {
    Box(
        modifier = Modifier
            .size(TemplateUiTokens.HeaderActionVisualSize)
            .clip(CircleShape)
            .background(containerColor)
            .border(BorderStroke(1.dp, borderColor), CircleShape)
            .dpisClickable(role = Role.Button, onClick = onClick),
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
private fun UnsavedBadge(visible: Boolean) {
    Surface(
        modifier = Modifier
            .heightIn(min = TemplateUiTokens.UnsavedBadgeMinHeight)
            .offset(y = (-2).dp)
            .alpha(if (visible) 1f else 0f),
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
