package com.dpis.module.templates.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.templates.TemplateConfigSummaryFormatter
import com.dpis.module.templates.TemplateWorkspacePresentation
import com.dpis.module.ui.compose.EdgeOcclusionFadeDirection
import com.dpis.module.ui.compose.EdgeOcclusionFadeTokens
import com.dpis.module.ui.compose.PageScrollPositionStore
import com.dpis.module.ui.compose.WorkspaceSearchCard
import com.dpis.module.ui.compose.clearTextInputFocusOnPointerDown
import com.dpis.module.ui.compose.edgeOcclusionFade
import com.dpis.module.ui.compose.rememberClickAction
import com.dpis.module.ui.compose.dpisClickable
import com.dpis.module.ui.compose.rememberRestorableLazyListState

private const val EDITOR_GLOBAL = "global"
private const val EDITOR_QUICK = "quick"

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TemplateWorkspaceListPane(
    state: TemplateWorkspacePresentation.State,
    padding: PaddingValues,
    onQueryChanged: (String) -> Unit,
    onEditorOpened: (String, String?) -> Unit,
    onSortRequested: () -> Unit,
    onTargetsOpened: (String) -> Unit,
    scrollStore: PageScrollPositionStore,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val topSafePadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(top = topSafePadding)
    ) {
        val searchDividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
        WorkspaceSearchCard(
            query = state.query,
            onQueryChanged = onQueryChanged,
            hintRes = R.string.template_search_hint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TemplateUiTokens.WorkspaceHorizontalPadding)
                .padding(
                    top = TemplateUiTokens.SearchTopPadding,
                    bottom = TemplateUiTokens.SearchBottomPadding
                )
                .height(TemplateUiTokens.SearchCardHeight),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(searchDividerColor),
        )
        val listState =
            rememberRestorableLazyListState(
                key = "templates",
                store = scrollStore,
                enabled = !state.searching,
            )
        val listScrolled by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            }
        }
        Box(Modifier
            .weight(1f)
            .fillMaxWidth()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = TemplateUiTokens.WorkspaceHorizontalPadding,
                    top = TemplateUiTokens.WorkspaceTopPadding,
                    end = TemplateUiTokens.WorkspaceHorizontalPadding,
                    bottom = padding.calculateBottomPadding() + TemplateUiTokens.WorkspaceBottomReserve
                ),
                verticalArrangement = Arrangement.spacedBy(TemplateUiTokens.ListGap),
                modifier = Modifier
                    .fillMaxSize()
                    .clearTextInputFocusOnPointerDown(focusManager)
            ) {
                if (!state.searching) {
                    item {
                        GlobalPrefillCard(state,
                            rememberClickAction {
                                onEditorOpened(EDITOR_GLOBAL, null)
                            })
                    }
                    item {
                        TemplateHeader(
                            state = state,
                            onSort = onSortRequested,
                            onCreate = rememberClickAction {
                                onEditorOpened(EDITOR_QUICK, null)
                            },
                        )
                    }
                }
                if (state.templates.isEmpty()) {
                    item {
                        val searching = state.searching
                        Box(
                            modifier = if (searching) {
                                Modifier.fillMaxWidth()
                            } else {
                                Modifier
                                    .fillParentMaxWidth()
                                    .fillParentMaxHeight(TemplateUiTokens.EMPTY_STATE_VIEWPORT_FRACTION)
                                    .padding(bottom = TemplateUiTokens.EmptyStateBottomBias)
                            },
                            contentAlignment = if (searching) Alignment.CenterStart else Alignment.Center
                        ) {
                            Text(
                                stringResource(
                                    if (searching) R.string.quick_template_search_empty
                                    else R.string.template_workspace_quick_templates_empty
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = if (searching) {
                                    Modifier.padding(
                                        top = TemplateUiTokens.EmptyStateTopGap,
                                        bottom = TemplateUiTokens.EmptyStatePadding,
                                        end = TemplateUiTokens.EmptyStatePadding
                                    )
                                } else Modifier
                            )
                        }
                    }
                } else {
                    items(state.templates.size, key = { state.templates[it].id }) { index ->
                        val template = state.templates[index]
                        TemplateCard(
                            template = template,
                            actions = state.actions,
                            onEdit = rememberClickAction {
                                onEditorOpened(EDITOR_QUICK, template.id)
                            },
                            onTargets = rememberClickAction {
                                onTargetsOpened(template.id)
                            }
                        )
                    }
                }
            }
            if (listScrolled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(EdgeOcclusionFadeTokens.Height)
                        .edgeOcclusionFade(
                            visibility = 1f,
                            direction = EdgeOcclusionFadeDirection.TOP_TO_BOTTOM,
                        )
                )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
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
                IconButton(onClick = rememberClickAction(onEdit)) {
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
    onSort: () -> Unit,
    onCreate: () -> Unit
) {
    val sort = rememberClickAction(onSort)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
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
                    onClick = rememberClickAction {
                        actions.applyTemplate(
                            template.id
                        )
                    }
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
                    overflow = TextOverflow.Ellipsis
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
            overflow = TextOverflow.Ellipsis
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
        color = MaterialTheme.colorScheme.surfaceBright
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
    visualSize: Dp,
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
            .alpha(if (enabled) 1f else TemplateUiTokens.DISABLED_ACTION_ALPHA)
            .dpisClickable(
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
    cornerRadius: Dp,
    strokeWidth: Dp = 1.dp
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
