package com.dpis.module.ui.compose

import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dpis.module.appconfig.EditorPresentation
import com.dpis.module.ConfigEditorDestination
import com.dpis.module.R
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.applist.AppStatusFormatter
import com.dpis.module.appconfig.AppConfigInputValidation
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetType
import com.dpis.module.ui.compose.FeedbackButton
import com.dpis.module.ui.compose.FeedbackOutlinedButton

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AppConfigEditorContent(
    state: EditorPresentation.State,
    contentPadding: PaddingValues = AppConfigSheetUiTokens.ContentPadding,
    onAdvancedAnchorMeasured: ((androidx.compose.ui.unit.Dp) -> Unit)? = null,
    showInlineUnsavedBadge: Boolean = true,
    extraTopPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val draft = state.draft
    val appIcon = rememberInstalledAppIcon(state.item.packageName, state.item.icon)
    val focusManager = LocalFocusManager.current
    val inputFocusBoundary = LocalTextInputFocusBoundary.current ?: rememberTextInputFocusBoundary()
    val density = LocalDensity.current
    val completeInput = { focusManager.clearFocus(force = true) }
    val viewportTargetSpec = AppConfigInputValidation.parseViewportTargetSpec(
        draft.viewportInputFor(draft.viewportMode),
        draft.viewportMode
    )
    val fontScalePercent = AppConfigInputValidation.parseFontScalePercentOrNull(draft.fontInput)
    val effectiveViewportApplyMode = if (viewportTargetSpec.isEnabled()) {
        draft.viewportApplyMode
    } else {
        ViewportApplyMode.OFF
    }
    val effectiveFontMode = if (fontScalePercent != null) draft.fontMode else FontApplyMode.OFF
    val appSpecificConfigActive = viewportTargetSpec.isEnabled()
        || fontScalePercent != null
        || !draft.selectedTypefaceId.isNullOrEmpty()
    val context = androidx.compose.ui.platform.LocalContext.current
    val resources = context.resources
    val statusLabel = AppStatusFormatter.formatCompact(
        resources,
        AppStatusFormatter.StatusInput(
            state.item.inScope,
            state.item.scopeKnown,
            state.item.installed,
            viewportTargetSpec,
            effectiveViewportApplyMode,
            fontScalePercent,
            effectiveFontMode,
            draft.selectedTypefaceId,
            draft.dpisEnabled,
            appSpecificConfigActive,
            null
        )
    )
    val warnViewport = state.item.scopeKnown && AppStatusFormatter.shouldWarnViewportEmulation(
        viewportTargetSpec,
        effectiveViewportApplyMode,
        state.systemHooksEnabled,
        draft.dpisEnabled
    )
    val warnFont = state.item.scopeKnown && AppStatusFormatter.shouldWarnFontEmulation(
        fontScalePercent,
        effectiveFontMode,
        state.systemHooksEnabled,
        draft.dpisEnabled
    )
    val styledStatus = AppStatusFormatter.applyConfigSegmentsWarnStyle(
        statusLabel,
        MaterialTheme.colorScheme.error.toArgb(),
        warnViewport,
        warnFont
    )
    val statusText = buildAnnotatedString {
        append(styledStatus)
        if (styledStatus is Spanned) {
            styledStatus.getSpans(0, styledStatus.length, ForegroundColorSpan::class.java).forEach { span ->
                addStyle(
                    SpanStyle(color = Color(span.foregroundColor)),
                    styledStatus.getSpanStart(span),
                    styledStatus.getSpanEnd(span)
                )
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(top = extraTopPadding)
            .padding(bottom = edgeToEdgeContentBottomPadding(0.dp))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(AppConfigSheetUiTokens.AppIconSize)
                        .clip(AppConfigSheetUiTokens.AppIconShape)
                        // The rounded surface is only needed while the real app icon is absent.
                        .then(
                            if (appIcon == null) {
                                Modifier.background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    appIcon?.let { icon ->
                        AndroidView(
                            factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                            update = { it.setImageDrawable(icon) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f)
                        .padding(start = AppConfigSheetUiTokens.AppIconGap),
                    verticalArrangement = Arrangement.Center
                ) {
                    AppIdentityMarqueeText(state.item.label,
                        modifier = Modifier.fillMaxWidth(),
                        // Trim only the boundary facing the package name. The normal title line
                        // height remains intact, so this does not make the glyphs feel cramped.
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.LastLineBottom,
                                mode = LineHeightStyle.Mode.Fixed
                            )
                        ))
                    AppIdentityMarqueeText(state.item.packageName, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.FirstLineTop,
                            mode = LineHeightStyle.Mode.Fixed
                        )
                    ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (state.versionName.isBlank()) "-" else state.versionName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (showInlineUnsavedBadge) {
                            // Keep the badge in the measurement tree while it is hidden. The
                            // first edit then changes only alpha, so the sheet anchor and window
                            // pan do not jump when dirty transitions from false to true.
                            Surface(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .alpha(if (state.dirty) 1f else 0f),
                                shape = AppConfigSheetUiTokens.UnsavedBadgeShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    stringResource(R.string.sheet_unsaved_badge),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(AppConfigSheetUiTokens.HeaderToFirstInputGap))
        EditorValueModeRow(
            input = { modifier, onFocusChanged ->
                CompactEditorTextField(
                    value = draft.viewportInputFor(draft.viewportMode),
                    onValueChange = state.actions::updateViewportInput,
                    modifier = modifier
                        .reportTextInputFocusBounds(inputFocusBoundary, "viewport"),
                    isError = !state.viewportInputValid,
                    label = stringResource(
                        if (state.usesAbsoluteViewport()) {
                            R.string.dialog_viewport_hint_absolute
                        } else {
                            R.string.dialog_viewport_hint_scale
                        }
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    onFocusChanged = onFocusChanged,
                    trailingIcon = if (draft.viewportInputFor(draft.viewportMode).isNotEmpty()) {
                        { EditorClearButton { state.actions.updateViewportInput("") } }
                    } else null
                )
            },
            first = stringResource(R.string.dialog_viewport_mode_system),
            second = stringResource(R.string.dialog_viewport_mode_compat),
            firstSelected = !state.usesAbsoluteViewport(),
            onFirst = {
                completeInput()
                state.actions.changeViewportMode(ViewportTargetType.RELATIVE_SCALE)
            },
            onSecond = {
                completeInput()
                state.actions.changeViewportMode(ViewportTargetType.ABSOLUTE_DP)
            },
            labelStyle = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.height(AppConfigSheetUiTokens.InputRowLayoutGap))
        EditorValueModeRow(
            input = { modifier, onFocusChanged ->
                CompactEditorTextField(
                    value = draft.fontInput,
                    onValueChange = state.actions::updateFontInput,
                    modifier = modifier
                        .reportTextInputFocusBounds(inputFocusBoundary, "font"),
                    isError = !state.fontInputValid,
                    label = stringResource(R.string.dialog_font_scale_hint),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onFocusChanged = onFocusChanged,
                    trailingIcon = if (draft.fontInput.isNotEmpty()) {
                        { EditorClearButton { state.actions.updateFontInput("") } }
                    } else null
                )
            },
            first = stringResource(R.string.dialog_font_mode_system),
            second = stringResource(R.string.dialog_font_mode_compat),
            firstSelected = state.usesSystemFontMode(),
            onFirst = {
                completeInput()
                state.actions.changeFontMode(FontApplyMode.SYSTEM_EMULATION)
            },
            onSecond = {
                completeInput()
                state.actions.changeFontMode(FontApplyMode.FIELD_REWRITE)
            },
            labelStyle = MaterialTheme.typography.labelSmall,
        )
        if (state.showsWechatDpi()) {
            DisposableEffect(inputFocusBoundary) {
                onDispose { inputFocusBoundary.removeInput("wechat") }
            }
            Spacer(Modifier.height(AppConfigSheetUiTokens.InputRowLayoutGap))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppConfigSheetUiTokens.FieldTopInset + rememberEditorControlHeight()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                // Keep the app-specific field on the same bottom edge as the standard
                // value/mode rows. The shared row height reserves the floating-label inset,
                // so the visible gaps above and below this row remain consistent.
                verticalAlignment = Alignment.Bottom
            ) {
                CompactEditorTextField(
                    value = draft.wechatDpiInput,
                    onValueChange = state.actions::updateWechatDpiInput,
                    modifier = Modifier
                        .weight(1f)
                        .reportTextInputFocusBounds(inputFocusBoundary, "wechat"),
                    isError = !state.wechatDpiInputValid,
                    label = stringResource(R.string.dialog_wechat_dpi_hint),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = if (!draft.wechatDpiInput.isNullOrEmpty()) {
                        { EditorClearButton { state.actions.updateWechatDpiInput("") } }
                    } else null
                )
                Box(
                    modifier = Modifier
                        .size(rememberEditorControlHeight())
                        .clip(AppConfigSheetUiTokens.ActionShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant,
                            AppConfigSheetUiTokens.ActionShape)
                        .dpisClickable(role = Role.Button, onClick = {
                            completeInput()
                            state.actions.showWechatDpiHelp()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_question_mark_24),
                        contentDescription = stringResource(R.string.dialog_wechat_dpi_help_button),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        Spacer(Modifier.height(AppConfigSheetUiTokens.ControlGroupGap))
        EditorTypefaceHookRow(
            primary = { modifier ->
                FeedbackOutlinedButton(
                    onClick = {
                        completeInput()
                        state.actions.navigate(ConfigEditorDestination.TYPEFACE)
                    },
                    modifier = modifier.height(rememberEditorControlHeight()),
                    shape = AppConfigSheetUiTokens.FieldAndActionShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = LocalSpacing.current.none, vertical = LocalSpacing.current.none)
                ) {
                    AppIdentityMarqueeText(
                        text = state.typefaceSelectorText,
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
                    onClick = {
                        completeInput()
                        state.actions.navigate(ConfigEditorDestination.HOOK_CHAIN_INTERFACE)
                    },
                    modifier = modifier.height(rememberEditorControlHeight()),
                    shape = AppConfigSheetUiTokens.FieldAndActionShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = LocalSpacing.current.none, vertical = LocalSpacing.current.none)
                ) {
                    AppIdentityMarqueeText(
                        text = state.hookChainText,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppConfigSheetUiTokens.ProcessActionGap)
        ) {
            FeedbackButton(
                onClick = {
                    completeInput()
                    state.actions.stopProcess()
                },
                modifier = Modifier.weight(1f).height(rememberEditorControlHeight()),
                shape = AppConfigSheetUiTokens.ActionShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.dpis_stop_container),
                    contentColor = colorResource(R.color.dpis_on_stop_container)
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(stringResource(R.string.dialog_stop_button), style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center, maxLines = 1)
            }
            FeedbackButton(
                onClick = {
                    completeInput()
                    state.actions.restartProcess()
                },
                modifier = Modifier.weight(1f).height(rememberEditorControlHeight()),
                shape = AppConfigSheetUiTokens.ActionShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.dpis_warn_container),
                    contentColor = colorResource(R.color.dpis_on_warn_container)
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(stringResource(R.string.dialog_restart_button), style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center, maxLines = 1)
            }
            FeedbackButton(
                onClick = {
                    completeInput()
                    state.actions.startProcess()
                },
                modifier = Modifier.weight(1f).height(rememberEditorControlHeight()),
                shape = AppConfigSheetUiTokens.ActionShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.dpis_success_container),
                    contentColor = colorResource(R.color.dpis_on_success_container)
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(stringResource(R.string.dialog_start_button), style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center, maxLines = 1)
            }
        }
        Spacer(Modifier.height(AppConfigSheetUiTokens.ControlGroupGap))
        FeedbackButton(
            onClick = {
                completeInput()
                state.actions.save()
            },
            modifier = Modifier.fillMaxWidth().height(rememberEditorControlHeight()),
            enabled = state.saveEnabled,
            colors = ButtonDefaults.buttonColors(),
            shape = AppConfigSheetUiTokens.ActionShape
        ) {
            Text(stringResource(if (state.saveFeedbackVisible) {
                R.string.status_save_success_inline
            } else {
                R.string.status_save_button
            }), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(AppConfigSheetUiTokens.SaveToAdvancedDividerGap))
        HorizontalDivider(
            modifier = Modifier.onGloballyPositioned { coordinates ->
                onAdvancedAnchorMeasured?.invoke(with(density) {
                    coordinates.positionInParent().y.toDp()
                })
            },
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(AppConfigSheetUiTokens.AdvancedTitleTopGap))
        Text(
            stringResource(R.string.dialog_advanced_section_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(AppConfigSheetUiTokens.AdvancedRowTopGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppConfigSheetUiTokens.ProcessActionGap)
        ) {
            if (state.isScopeSelected) {
                FeedbackButton(
                    onClick = {
                        completeInput()
                        state.actions.toggleScope()
                    },
                    enabled = state.item.scopeKnown,
                    modifier = Modifier.weight(1f).height(rememberEditorControlHeight()),
                    shape = AppConfigSheetUiTokens.ActionShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) { Text(stringResource(R.string.dialog_scope_in_scope), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            } else {
                FeedbackOutlinedButton(
                    onClick = {
                        completeInput()
                        state.actions.toggleScope()
                    },
                    enabled = state.item.scopeKnown,
                    modifier = Modifier.weight(1f).height(rememberEditorControlHeight()),
                    shape = AppConfigSheetUiTokens.ActionShape
                ) { Text(stringResource(R.string.dialog_scope_apply), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            if (state.isDpisEnabled) {
                FeedbackOutlinedButton(
                    onClick = {
                        completeInput()
                        state.actions.toggleDpisEnabled()
                    },
                    modifier = Modifier.weight(1f).height(rememberEditorControlHeight()),
                    shape = AppConfigSheetUiTokens.ActionShape,
                ) {
                    Text(stringResource(R.string.dialog_config_disable), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                FeedbackButton(
                    onClick = {
                        completeInput()
                        state.actions.toggleDpisEnabled()
                    },
                    modifier = Modifier.weight(1f).height(rememberEditorControlHeight()),
                    shape = AppConfigSheetUiTokens.ActionShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(stringResource(R.string.dialog_config_disabled), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(AppConfigSheetUiTokens.DisableActionTopGap))
            FeedbackOutlinedButton(
            onClick = {
                completeInput()
                state.actions.reset()
            },
            modifier = Modifier.fillMaxWidth().height(rememberEditorControlHeight()),
            shape = AppConfigSheetUiTokens.ActionShape,
            contentPadding = PaddingValues(horizontal = LocalSpacing.current.lg, vertical = LocalSpacing.current.none)
        ) {
            Text(stringResource(R.string.dialog_disable_button), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(4.dp))
    }
}
